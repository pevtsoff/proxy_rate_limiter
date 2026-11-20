-- Proxy Rate Limiter configuration schema.

CREATE TABLE policies
(
    id                     BIGSERIAL PRIMARY KEY,
    name                   VARCHAR(100) NOT NULL,
    rate_limiting_strategy VARCHAR(32)  NOT NULL DEFAULT 'moving-window',
    number_of_requests     INTEGER      NOT NULL,
    time_period            VARCHAR(16)  NOT NULL,
    is_default             BOOLEAN      NOT NULL DEFAULT FALSE,
    version                BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT policies_name_unique UNIQUE (name),
    CONSTRAINT policies_strategy_check
        CHECK (rate_limiting_strategy IN ('moving-window', 'fixed-window', 'token-bucket')),
    CONSTRAINT policies_requests_positive CHECK (number_of_requests > 0),
    CONSTRAINT policies_period_check
        CHECK (time_period IN ('second', 'minute', 'hour', 'day', 'week'))
);

-- At most one default policy.
CREATE UNIQUE INDEX policies_single_default ON policies (is_default) WHERE is_default;

CREATE TABLE resource_groups
(
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT resource_groups_name_unique UNIQUE (name)
);

CREATE TABLE resources
(
    id                BIGSERIAL PRIMARY KEY,
    method            VARCHAR(10)  NOT NULL,
    resource_path     VARCHAR(512) NOT NULL,
    io_type           VARCHAR(8)   NOT NULL,
    resource_group_id BIGINT REFERENCES resource_groups (id) ON DELETE SET NULL,
    CONSTRAINT resources_method_path_unique UNIQUE (method, resource_path),
    CONSTRAINT resources_method_check
        CHECK (method IN ('GET', 'HEAD', 'OPTIONS', 'TRACE', 'POST', 'PUT', 'PATCH', 'DELETE')),
    CONSTRAINT resources_io_type_check CHECK (io_type IN ('read', 'write'))
);

CREATE INDEX resources_resource_group_idx ON resources (resource_group_id);

CREATE TABLE consumer_groups
(
    id         BIGSERIAL PRIMARY KEY,
    group_name VARCHAR(16) NOT NULL,
    CONSTRAINT consumer_groups_name_unique UNIQUE (group_name),
    CONSTRAINT consumer_groups_name_check
        CHECK (group_name IN ('premium', 'regular', 'free', 'internal'))
);

-- A consumer cannot exist without a consumer group (NOT NULL foreign key).
CREATE TABLE consumers
(
    id                BIGSERIAL PRIMARY KEY,
    consumer_id       VARCHAR(128) NOT NULL,
    consumer_group_id BIGINT       NOT NULL REFERENCES consumer_groups (id) ON DELETE CASCADE,
    CONSTRAINT consumers_consumer_id_unique UNIQUE (consumer_id)
);

CREATE INDEX consumers_consumer_group_idx ON consumers (consumer_group_id);

-- relationship_type decides which target column is set. Two real foreign keys are used instead
-- of one polymorphic "resource_id" column so that PostgreSQL can enforce referential integrity;
-- the REST API still exposes a single "resource_id" field.
CREATE TABLE consumer_groups_policies
(
    id                BIGSERIAL PRIMARY KEY,
    consumer_group_id BIGINT      NOT NULL REFERENCES consumer_groups (id) ON DELETE CASCADE,
    relationship_type VARCHAR(16) NOT NULL,
    policy_id         BIGINT      NOT NULL REFERENCES policies (id) ON DELETE RESTRICT,
    resource_id       BIGINT REFERENCES resources (id) ON DELETE RESTRICT,
    resource_group_id BIGINT REFERENCES resource_groups (id) ON DELETE RESTRICT,
    CONSTRAINT cgp_relationship_type_check
        CHECK (relationship_type IN ('resource', 'resource_group')),
    -- Constraint 1: an assignment targets either a resource OR a resource group, never both.
    CONSTRAINT cgp_exactly_one_target CHECK (
        (relationship_type = 'resource' AND resource_id IS NOT NULL AND resource_group_id IS NULL)
            OR (relationship_type = 'resource_group' AND resource_group_id IS NOT NULL
            AND resource_id IS NULL))
);

CREATE UNIQUE INDEX cgp_unique_resource
    ON consumer_groups_policies (consumer_group_id, resource_id) WHERE resource_id IS NOT NULL;
CREATE UNIQUE INDEX cgp_unique_resource_group
    ON consumer_groups_policies (consumer_group_id, resource_group_id)
    WHERE resource_group_id IS NOT NULL;
CREATE INDEX cgp_policy_idx ON consumer_groups_policies (policy_id);
