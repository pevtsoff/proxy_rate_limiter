-- Database-level safety net for rules that a plain CHECK / FOREIGN KEY cannot express.
-- The application validates the same rules first and returns readable 409 responses; these
-- triggers guarantee integrity even for writes that bypass the application.

-- "A consumer group should have at least one consumer."
-- Deferred to COMMIT so that a group and its first consumers can be inserted in one transaction.
CREATE FUNCTION ensure_consumer_group_not_empty() RETURNS trigger AS
$$
DECLARE
    group_id BIGINT;
BEGIN
    IF TG_TABLE_NAME = 'consumer_groups' THEN
        group_id := NEW.id;
    ELSE
        group_id := OLD.consumer_group_id;
    END IF;
    IF EXISTS (SELECT 1 FROM consumer_groups WHERE id = group_id)
        AND NOT EXISTS (SELECT 1 FROM consumers WHERE consumer_group_id = group_id) THEN
        RAISE EXCEPTION 'Consumer group % must have at least one consumer', group_id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER consumer_groups_require_consumer
    AFTER INSERT ON consumer_groups
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION ensure_consumer_group_not_empty();

CREATE CONSTRAINT TRIGGER consumers_keep_group_non_empty
    AFTER DELETE OR UPDATE OF consumer_group_id ON consumers
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION ensure_consumer_group_not_empty();

-- Constraint 2: for one consumer group a resource cannot be assigned directly while its resource
-- group is assigned (and vice versa).
CREATE FUNCTION prevent_overlapping_assignment() RETURNS trigger AS
$$
BEGIN
    IF NEW.relationship_type = 'resource' AND EXISTS (
        SELECT 1
        FROM consumer_groups_policies a
                 JOIN resources r ON r.resource_group_id = a.resource_group_id
        WHERE a.consumer_group_id = NEW.consumer_group_id
          AND r.id = NEW.resource_id
          AND a.id IS DISTINCT FROM NEW.id) THEN
        RAISE EXCEPTION 'Resource % is covered by an assigned resource group of consumer group %',
            NEW.resource_id, NEW.consumer_group_id USING ERRCODE = 'check_violation';
    END IF;
    IF NEW.relationship_type = 'resource_group' AND EXISTS (
        SELECT 1
        FROM consumer_groups_policies a
                 JOIN resources r ON r.id = a.resource_id
        WHERE a.consumer_group_id = NEW.consumer_group_id
          AND r.resource_group_id = NEW.resource_group_id
          AND a.id IS DISTINCT FROM NEW.id) THEN
        RAISE EXCEPTION 'Resource group % contains a directly assigned resource of consumer group %',
            NEW.resource_group_id, NEW.consumer_group_id USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER consumer_groups_policies_no_overlap
    BEFORE INSERT OR UPDATE ON consumer_groups_policies
    FOR EACH ROW EXECUTE FUNCTION prevent_overlapping_assignment();

-- Moving a resource into a group can create the same overlap.
CREATE FUNCTION prevent_overlap_on_regroup() RETURNS trigger AS
$$
BEGIN
    IF NEW.resource_group_id IS NOT NULL AND EXISTS (
        SELECT 1
        FROM consumer_groups_policies direct
                 JOIN consumer_groups_policies grouped
                      ON grouped.consumer_group_id = direct.consumer_group_id
        WHERE direct.resource_id = NEW.id
          AND grouped.resource_group_id = NEW.resource_group_id) THEN
        RAISE EXCEPTION 'Resource % and its new resource group % are both assigned to one consumer group', NEW.id, NEW.resource_group_id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER resources_no_overlap_on_regroup
    BEFORE UPDATE OF resource_group_id ON resources
    FOR EACH ROW EXECUTE FUNCTION prevent_overlap_on_regroup();
