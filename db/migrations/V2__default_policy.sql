-- MUC3: the default policy covers every endpoint that has no explicit assignment.
-- Values come from .env through Flyway placeholders (see application.yml).
INSERT INTO policies (name, rate_limiting_strategy, number_of_requests, time_period, is_default)
VALUES ('${default_policy_name}', '${default_policy_strategy}', ${default_policy_requests},
        '${default_policy_period}', TRUE);
