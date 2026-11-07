# Implementation Details
1. Use docker and docker compose to raise the local stack: Python, Redis, Postgresql etc. Carefully read ./task.txt
and its every point, and implement it using the below instructions
2. The directory structure shall be as described in task.txt
3. Put docker files under src/main/docker folder
4. Use python, fastapi (async code), sqlalchemy (async code), async redis.py, uv package manager, slowapi (for fastAPI) as rate limiting package https://slowapi.readthedocs.io/en/latest/, pydantic settings
pytest-asyncio for testing
5. use asyncio where possible
6. read carefully https://slowapi.readthedocs.io/en/latest/
7. create .env file and use it for docker/docker compose runs
8. Write the run.sh/test.sh to run the application and tests under the docker compose
9. Use sqladmin to create Admin  UI. Adjust it for main

## What we need to build:
1. Test FastAPI container, which will be the  target API for which we will use  the proxy rate limiter
2. Architectural Assumption N1: since we dont know what/how the target API is built (python, java, FastAPI,spring or whatever)
we build not the python middleware, but rather a full size proxy rate limiter. Write it down as a first architectural assumption
3. We need to build a proxy rate limiter, which will limit the access to the target  REST API.
4. Target API and Proxy Rate limiter should be placed each in a separate docker compose container

## Target API Spec:
1. It should be a usual FastAPI application, which will log ALL the incoming  requests and respond some static response
2. Let the target API have these types of API:
/orders (GET, POST, PUT, PATCH) - GET and PUT should be idempotent, POST and PATCH - not
/users  (GET, POST, PUT, PATCH) - GET and PUT should be idempotent, POST and PATCH - not
/carts (GET, POST, PUT, PATCH) - GET and PUT should be idempotent, POST and PATCH - not

## Proxy Rate limiter spec:
1. Architectural Assumption N2: the target API HAS swagger docs
2. It should fetch ALL the available endpoints and methods from the TargetAPI using swagger
and save into resources table (see below)

 
## Proxy API limiter DB schema description
### Proxy API limiter should have these tables at least:

#### policies 
should have a list of rate limiting policies using the slowapi strategies.A policy should typically have these fields:

1. rate_limiting_strategy - all the string (lowered) values imported from slowapi strategies programmatically like
"""
import limits.strategies
supported_strategies = list(limits.strategies.STRATEGIES.keys())
"""
the default value should be "moving-window"

2. number_of_requests - integer, number of allowed  requests per time period
3. time_period: enum, shuld be second, minute,hour, day, week
4. name - string, unique name

#### resources 
should be a list of target endpoints from target API. The resources table should have these fields:
1. io_type -  field which will have "read" and "write" enums.
2. "method" -  field with all the HTTPS request types available as enums.
The POST, PUT and PATCH endpoints by default should be marked as "write", GET - as "read"
3. resource_path - target resource REST PATH (without domain and port)
4. resource_group_id - foreign key to resource_groups table

#### resource_groups
should be a list of resource groups. Groups exist to deal with resource groups rather than with individual resources

#### consumers 
should be a list of user ids 
1. consumer id
2. consumer_group_id (one-to-many relationship to consumer groups)

Table Constraints: Consumer cannot exist without a consumer group
#### consumer_groups
should have at least these fields:
1. group_name - string enum field with the following values (premium, regular, free, internal)
2. consumer_id - non-null

Table Constraints: Consumer group should have at least on consumer
#### consumer_groups_policies
this table shall keep relationships between resources, consumer_groups and policies. 
Each consumer group should relate to ONE or MANY policies. It should cover cases like:
1. One consumer group has ONE the same policy for ALL target resources
2. One consumer group has separate policy for each target resource
3. One consumer group has separate policy for each target resource group

the table should have these fields at least:
1.id - integer
2.consumer_group_id - foreign key to consumer_group table
3.relationship_type - string enum, can be "resource"/"resource_group"
4.policy_id - foreign key to policies.
5.resource_id - foreign key to resource or resource groups depending on relationship_type

Table constraints: 
1.one consumer group can have a policy either for "resource" OR for "resource_group", NOT BOTH!
2.The same resource_id for some consumer group cannot belong to any other resource_groups for the same consumer group 

### Proxy API Limiter Use cases
#### API Managers Use Cases
"""
API Managers Use Cases
MUC1. API Manager can create main entities and assign rate limiting policies to
different resources
As an API manager, I want to be able to 
1.Create to a consumer group
2.Create a consumer and assign a consumer group to it 
3.Create a rate limiting policy
4.Fetch list resource list should from target API swagger docs (via special endpoint)
5.Assign a consumer_group  some rate limiting policy for  target API resources, one by one, 
or to consumer group

Acceptance Criteria
1.When creating consumer_groups_policies rules, it should always check that new resource/resource_groups relationships DONT contractict with existing assignments for the same consumer group. 
I.e. the existing resources should not belong to a resource_group from newly added assignment and vices versa
2.A consumer should always belong to some consumer group. The rate limiting policies can only be assigned to the consumer groups

MUC2. Manager can do CRUD operations over policy assignments
As an API manager, I want to be able to update or delete the rate limiting assignments
1. Get consumer_groups_policies
2. Select consumer_groups_id
3. Update or delete some assignment for a particular resource or resource_group

MUC3. Default Rate limiting strategy
As an API manager, I want to be sure, that if not any rate limiting policy is applied
then the default policy is used (it should be pre-created with wise defaults) and cover
ALL the  endpoints

MUC4. CRUD for Rate limiting policies
As an APIManager, I want to be able to RUD the rate limiting policy via API

MUC5. Redis caching for the API consumer groups policies
As an API Manager, I want the Proxy Rate limiter use LRU cache for the consumer_id and target REST API
so that if the rate limiting policies are not changed, it can fully rely on redis cached values

Acceptance Criteria:
1. The LRU cache CAPACITY and  TTL time should be configured in .env file. Default CAPACITY=1000, TTL=3600
2. There should be a lru cached function which reads proxy rate limiting database
to determine the applicable rate-limiting policy by (consumer_id, rest_path)
3. If the admin does any of CRUD operation for rate  policies  or consumer_groups_policies table - we should invalidate the cache

MUC6.AdminUI
As an API manager, I want to have Admin UI which will allow me to 
CRUD all the above tables and replicate their relationships

Acceptance Criteria:
1.All the models shouldbe present in the ADmin UI
2.The CRUD operations shall work the same way as API
"""

#### Main Consumer use cases
"""

As an API consumer, I want to be able to access the Proxy Rate limiter API the same way
CUC1.Consumer gets rate limit error in case it has exchausted the rate limits
I would access the target API. The Proxy Rate limiter should just 
1. Check the request bearer token, extract the consumer_id
2. Extract the target endpoint path and method like GET /orders/1 from the bearer token
3. fetch the appropriate consumer group for the consumer id and find the consumer_group policy assignment in
consumer_groups_policies table for marching REST resource
4. Check if there is still capacity for this request
5. Pass through the request if it can
6. Return HTTPS status 429 in case it cannot pass and a meningfull json response which will include resource path and error
7. fill in seed.sh file so that we can seed the database with test consumers, groups, policies and assignments. 
8. Use this file as a seed script for integration tests 
"""


### Create Automated tests
1. Use pytest to write the tests
2. Under src/test, create subfolders for each Proxy API limiter endpoint
3. Create test fixtures that would seed the database with existing consumers/consumer groups
4. Ensure you cover all the main CRUD tests for each endpoint and the edge cases
5. Use transaction roll-back db test fixture for async sqlchemy 2.0 to roll-back the test results after each test.
6. Create locust tests for the target endpoint so that we can test the Proxy Rate limit at work
7. Create both unit and integration tests. They should be placed in appropriate subfolders src/test/unit or src/test/integration
8. integration tests should be having above use case names like test_muc1_***, test_cuc2_***
9. locust tests should be placed under src/test/performance tests folder


### Acceptance Review
When finish, review the task.txt and implementation_details.txt to confirm you have done everything right

### Proxy limiter Details
1. All the user facing errors should be in JSON and in the same format like
{"error": "You exceeded the request capacity for the current period", "resource": "orders/1", "https_status_code": 429}
2. All the exceptions should be logged in appropriate middleware 
3. The logs should include module name, function name, line NO, exception traceback



