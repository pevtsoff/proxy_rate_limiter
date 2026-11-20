-- Demo / test data (CUC1.7). Idempotent: safe to run repeatedly.
-- Used by ./seed.sh against the running stack and by the integration tests (@Sql) as their
-- data fixture (CUC1.8). seed.sh runs it with psql --single-transaction because the
-- "group has at least one consumer" constraint trigger is checked at COMMIT.
-- Resource groups and resources as published by the target API's OpenAPI document.
INSERT INTO resource_groups (name)
VALUES ('orders'), ('users'), ('carts')
ON CONFLICT (name) DO NOTHING;

INSERT INTO resources (method, resource_path, io_type, resource_group_id)
SELECT r.method, r.resource_path, r.io_type, g.id
FROM (VALUES ('GET', '/orders', 'read', 'orders'),
             ('POST', '/orders', 'write', 'orders'),
             ('GET', '/orders/{id}', 'read', 'orders'),
             ('PUT', '/orders/{id}', 'write', 'orders'),
             ('PATCH', '/orders/{id}', 'write', 'orders'),
             ('GET', '/users', 'read', 'users'),
             ('POST', '/users', 'write', 'users'),
             ('GET', '/users/{id}', 'read', 'users'),
             ('PUT', '/users/{id}', 'write', 'users'),
             ('PATCH', '/users/{id}', 'write', 'users'),
             ('GET', '/carts', 'read', 'carts'),
             ('POST', '/carts', 'write', 'carts'),
             ('GET', '/carts/{id}', 'read', 'carts'),
             ('PUT', '/carts/{id}', 'write', 'carts'),
             ('PATCH', '/carts/{id}', 'write', 'carts'))
         AS r(method, resource_path, io_type, group_name)
         JOIN resource_groups g ON g.name = r.group_name
ON CONFLICT (method, resource_path) DO NOTHING;

-- Policies (the default policy is created by Flyway migration V2).
INSERT INTO policies (name, rate_limiting_strategy, number_of_requests, time_period)
VALUES ('premium-500-per-minute', 'moving-window', 500, 'minute'),
       ('regular-100-per-minute', 'moving-window', 100, 'minute'),
       ('free-10-per-minute', 'fixed-window', 10, 'minute'),
       ('free-writes-3-per-minute', 'token-bucket', 3, 'minute'),
       ('internal-10000-per-minute', 'token-bucket', 10000, 'minute')
ON CONFLICT (name) DO NOTHING;

-- Consumer groups with their consumers (a group must never be empty - the constraint trigger
-- is checked at COMMIT, so both inserts live in this one transaction).
INSERT INTO consumer_groups (group_name)
VALUES ('premium'), ('regular'), ('free'), ('internal')
ON CONFLICT (group_name) DO NOTHING;

INSERT INTO consumers (consumer_id, consumer_group_id)
SELECT c.consumer_id, g.id
FROM (VALUES ('premium-user-1', 'premium'),
             ('premium-user-2', 'premium'),
             ('regular-user-1', 'regular'),
             ('regular-user-2', 'regular'),
             ('free-user-1', 'free'),
             ('free-user-2', 'free'),
             ('internal-service-1', 'internal'))
         AS c(consumer_id, group_name)
         JOIN consumer_groups g ON g.group_name = c.group_name
ON CONFLICT (consumer_id) DO NOTHING;

-- Assignments covering the three relationship patterns of the task:
--   premium  : ONE policy per resource group (same policy for every resource group)
--   regular  : a separate policy for each resource group
--   free     : a separate policy for individual resources (writes) + default elsewhere
--   internal : one policy for every target resource, assigned resource by resource
INSERT INTO consumer_groups_policies (consumer_group_id, relationship_type, policy_id,
                                      resource_group_id)
SELECT cg.id, 'resource_group', p.id, rg.id
FROM consumer_groups cg
         JOIN policies p ON p.name = CASE cg.group_name
                                         WHEN 'premium' THEN 'premium-500-per-minute'
                                         ELSE 'regular-100-per-minute' END
         CROSS JOIN resource_groups rg
WHERE cg.group_name IN ('premium', 'regular')
  AND rg.name IN ('orders', 'users', 'carts')
ON CONFLICT DO NOTHING;

INSERT INTO consumer_groups_policies (consumer_group_id, relationship_type, policy_id,
                                      resource_id)
SELECT cg.id, 'resource', p.id, r.id
FROM consumer_groups cg
         JOIN resources r ON r.io_type = 'write'
         JOIN policies p ON p.name = 'free-writes-3-per-minute'
WHERE cg.group_name = 'free'
ON CONFLICT DO NOTHING;

INSERT INTO consumer_groups_policies (consumer_group_id, relationship_type, policy_id,
                                      resource_id)
SELECT cg.id, 'resource', p.id, r.id
FROM consumer_groups cg
         CROSS JOIN resources r
         JOIN policies p ON p.name = 'free-10-per-minute'
WHERE cg.group_name = 'free'
  AND r.io_type = 'read'
  AND r.resource_path LIKE '/orders%'
ON CONFLICT DO NOTHING;

INSERT INTO consumer_groups_policies (consumer_group_id, relationship_type, policy_id,
                                      resource_id)
SELECT cg.id, 'resource', p.id, r.id
FROM consumer_groups cg
         CROSS JOIN resources r
         JOIN policies p ON p.name = 'internal-10000-per-minute'
WHERE cg.group_name = 'internal'
ON CONFLICT DO NOTHING;
