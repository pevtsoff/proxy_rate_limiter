-- Explicit cleanup for tests that commit (non-@Transactional): restores the post-migration
-- state (only the default policy remains). TRUNCATE does not fire the row-level triggers.
TRUNCATE consumer_groups_policies, consumers, consumer_groups, resources, resource_groups
    RESTART IDENTITY CASCADE;
DELETE FROM policies WHERE is_default = FALSE;
