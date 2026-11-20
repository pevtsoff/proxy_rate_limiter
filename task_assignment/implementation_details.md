1. Use Java 21+, Spring Boot, Maven and Docker/Docker Compose to implement the application. Carefully read `./task.txt` and every requirement before starting implementation.
2. Follow the exact directory structure described in `task.txt`.
3. Put Docker-related files under `src/main/docker`.
4. Use Spring Boot, Spring MVC, Spring Data JPA, Hibernate, PostgreSQL, Spring Data Redis (Lettuce), Bucket4j for distributed rate limiting, Flyway for database migrations, Jakarta Validation, Spring Security where required, and Jmix or Vaadin for the Admin UI.
5. Follow Java and Spring Boot best practices, SOLID principles, constructor-based dependency injection, proper transaction management and clean separation of concerns.
6. Use virtual threads where appropriate; avoid unnecessary reactive programming.
7. Carefully implement all rate-limiting algorithms and requirements specified in `task.txt`, ensuring Redis-backed distributed rate limiting and correct concurrency handling.
8. Create `.env` and `.env.example` files and use environment variables for Docker Compose configuration.
9. Write `run.sh` and `test.sh` to build, run and test the application using Docker Compose.
10. Use JUnit 5, Mockito, Spring Boot Test and Testcontainers for unit and integration testing, including PostgreSQL, Redis and concurrent rate-limiting scenarios.
11. Ensure the Admin UI supports the required CRUD operations, entity relationships, filtering and pagination, with appropriate authentication and authorization.

## What we need to build:
1. Test Spring Boot container, which will be the target API for which we will use the proxy rate limiter.
2. Architectural Assumption N1: Since we don't know what/how the target API is built (Python, Java, FastAPI, Spring or whatever), we build not a framework-specific middleware, but rather a full-size proxy rate limiter. Write it down as the first architectural assumption.
3. We need to build a proxy rate limiter, which will limit access to the target REST API.
4. Target API and Proxy Rate Limiter should be placed each in a separate Docker Compose container.

## Target API Spec:
1. It should be a usual Spring Boot REST application, which will log ALL incoming requests and respond with some static response.
2. Let the target API have these types of API:

- `/orders` (GET, POST, PUT, PATCH) - GET and PUT should be idempotent, POST and PATCH - not.
- `/users` (GET, POST, PUT, PATCH) - GET and PUT should be idempotent, POST and PATCH - not.
- `/carts` (GET, POST, PUT, PATCH) - GET and PUT should be idempotent, POST and PATCH - not.

## Proxy Rate Limiter Spec:
1. Architectural Assumption N2: The target API HAS OpenAPI/Swagger documentation.
2. It should fetch ALL available endpoints and methods from the Target API using its OpenAPI specification and save them into the resources table (see below).

## Proxy API Limiter DB Schema Description

### Proxy API Limiter should have these tables at least:

#### policies
Should have a list of rate-limiting policies supporting the strategies available in the chosen Java rate-limiting implementation.

1. `rate_limiting_strategy` - string enum representing the supported rate-limiting strategies. Implement the required strategies programmatically and expose the supported values through the API. The default value should be `moving-window`.
2. `number_of_requests` - integer, number of allowed requests per time period.
3. `time_period` - enum, should be second, minute, hour, day, week.
4. `name` - string, unique name.

#### resources
Should be a list of target endpoints from the target API. The resources table should have these fields:

1. `io_type` - field which will have `read` and `write` enums.
2. `method` - field with all HTTP request methods available as enums. POST, PUT and PATCH endpoints should by default be marked as `write`, GET - as `read`.
3. `resource_path` - target resource REST path (without domain and port).
4. `resource_group_id` - foreign key to resource_groups table.

#### resource_groups
Should be a list of resource groups. Groups exist to deal with resource groups rather than individual resources.

#### consumers
Should be a list of user IDs:

1. `consumer_id`.
2. `consumer_group_id` (many consumers can belong to one consumer group).

Table constraint: A consumer cannot exist without a consumer group.

#### consumer_groups
Should have at least these fields:

1. `group_name` - string enum field with the following values (premium, regular, free, internal).

Table constraint: A consumer group should have at least one consumer.

#### consumer_groups_policies
This table shall keep relationships between resources, consumer_groups and policies.

Each consumer group should relate to ONE or MANY policies. It should cover cases like:

1. One consumer group has ONE same policy for ALL target resources.
2. One consumer group has a separate policy for each target resource.
3. One consumer group has a separate policy for each target resource group.

The table should have these fields at least:

1. `id` - integer.
2. `consumer_group_id` - foreign key to consumer_groups table.
3. `relationship_type` - string enum, can be `resource` / `resource_group`.
4. `policy_id` - foreign key to policies.
5. `resource_id` - foreign key to resources or resource_groups depending on `relationship_type`.

Table constraints:
1. One consumer group can have a policy either for `resource` OR for `resource_group`, NOT BOTH.
2. The same resource_id for some consumer group cannot belong to any other resource_groups for the same consumer group.

### Proxy API Limiter Use Cases

#### API Managers Use Cases

MUC1. API Manager can create main entities and assign rate-limiting policies to different resources.

As an API manager, I want to be able to:

1. Create a consumer group.
2. Create a consumer and assign a consumer group to it.
3. Create a rate-limiting policy.
4. Fetch the resource list from the target API OpenAPI documentation (via a dedicated endpoint).
5. Assign a consumer_group some rate-limiting policy for target API resources, one by one, or by resource group.

Acceptance Criteria:

1. When creating consumer_groups_policies rules, it should always check that new resource/resource_group relationships DON'T contradict existing assignments for the same consumer group. Existing resources should not belong to a resource_group from a newly added assignment and vice versa.
2. A consumer should always belong to some consumer group. Rate-limiting policies can only be assigned to consumer groups.

MUC2. Manager can perform CRUD operations over policy assignments.

As an API manager, I want to be able to update or delete rate-limiting assignments:

1. Get consumer_groups_policies.
2. Select consumer_group_id.
3. Update or delete an assignment for a particular resource or resource_group.

MUC3. Default Rate-Limiting Strategy.

As an API manager, I want to be sure that if no rate-limiting policy is applied, the default policy is used (it should be pre-created with sensible defaults) and cover ALL endpoints.

MUC4. CRUD for Rate-Limiting Policies.

As an API manager, I want to be able to CRUD rate-limiting policies via API.

MUC5. Redis Caching for API Consumer Group Policies.

As an API manager, I want the Proxy Rate Limiter to use an LRU cache for consumer_id and target REST API so that if the rate-limiting policies are not changed, it can rely on cached values.

Acceptance Criteria:

1. The LRU cache CAPACITY and TTL should be configured in `.env` file. Default CAPACITY=1000, TTL=3600.
2. There should be a cacheable service method which reads the proxy rate-limiting database to determine the applicable rate-limiting policy by `(consumer_id, rest_path)`. Use Spring Cache abstraction with an appropriate Caffeine cache configuration supporting maximum size, expiration and statistics.
3. If the admin performs any CRUD operation on the `policies` or `consumer_groups_policies` tables, the relevant cache entries should be invalidated using Spring Cache eviction mechanisms.

MUC6. Admin UI.

As an API manager, I want to have an Admin UI which will allow me to CRUD all the above tables and replicate their relationships.

Acceptance Criteria:

1. All models should be present in the Admin UI.
2. CRUD operations shall work the same way as the API.

#### Main Consumer Use Cases

As an API consumer, I want to be able to access the Proxy Rate Limiter API in the same way as the target API.

CUC1. Consumer gets a rate-limit error in case they have exhausted their rate limits.

When accessing the target API through the Proxy Rate Limiter, it should:

1. Check the request Bearer token and extract the consumer_id.
2. Extract the target endpoint path and HTTP method from the incoming request.
3. Fetch the appropriate consumer group for the consumer ID and find the consumer_group policy assignment in consumer_groups_policies for the matching REST resource.
4. Check if there is still capacity for this request.
5. Forward the request to the target API if capacity is available.
6. Return HTTP status 429 if the request cannot pass, with a meaningful JSON response including resource path and error.
7. Provide a `seed.sh` script to seed the database with test consumers, groups, policies and assignments.
8. Use the seed script as a basis for integration test data initialization.

### Create Automated Tests

1. Use JUnit 5 and Mockito to write the tests.
2. Under `src/test`, create subfolders for each Proxy API Limiter endpoint.
3. Create test fixtures using Spring Boot Test and Testcontainers to initialize consumers and consumer groups.
4. Ensure you cover all main CRUD tests for each endpoint and edge cases.
5. Use transactional rollback with Spring's `@Transactional` test support to roll back database changes after each test. For tests involving separate transactions or Testcontainers, explicitly manage database cleanup.
6. Create Locust tests for the target endpoints so that we can test the Proxy Rate Limiter under load.
7. Create both unit and integration tests. They should be placed in appropriate subfolders `src/test/unit` or `src/test/integration`.
8. Integration tests should have the above use-case names, such as `test_muc1_*`, `test_cuc1_*`.
9. Locust tests should be placed under `src/test/performance`.

### Acceptance Review

When finished, review `task.txt` and `implementation_details.txt` to confirm that everything has been implemented correctly.

### Proxy Limiter Details

1. All user-facing errors should be JSON and follow the same format:

`{"error": "You exceeded the request capacity for the current period", "resource": "orders/1", "https_status_code": 429}`

2. All exceptions should be logged in appropriate Spring exception handlers and filters.
3. Logs should include class name, method name, line number and full exception stack trace.