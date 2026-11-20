# 04 - AI usage

## Tools

* **Claude (Anthropic), model Claude Opus 5.5**, used in the Claude app as an agent with access to
  the project folder. It read the task files, wrote the code, tests, scripts and these documents.
* The AI ran in a sandbox **without access to Maven Central and without a Docker daemon**, so it
  could not run the Maven build. It compiled the framework-free `domain` and `application`
  packages with a plain `javac` to verify them; everything else was written without a compiler
  and is verified by running `./test.sh` on the developer machine.

## What AI was used for

| Area | AI contribution |
|---|---|
| Requirements analysis | extracted use cases (MUC1-6, CUC1), listed ambiguities and assumptions (01) |
| Architecture | proposed the hexagonal package structure, ports, sequence and ER diagrams (02) |
| Code | all Java code of proxy and target API, Flyway migrations, seed SQL, Lua script, Vaadin views |
| Tests | unit tests, integration tests per endpoint, concurrency/two-instance tests, Locust file |
| Infrastructure | Dockerfiles, docker-compose, `run.sh`, `test.sh`, `seed.sh`, `.env.example` |
| Documentation | README and docs 01-04 |

## Significant prompts (in order)

1. The task prompt given to the agent (verbatim):

   > can you read please task_assignment/implementation_details.md and task.txt and implement
   > everything, following accurately SOLID principles and Hexagonal Architecture patterns. Use
   > only java/maven under docker /docker compose, NO localhost java is available and shall NOT
   > be installed

2. `task_assignment/implementation_details.md` (written by the developer beforehand) acted as the
   detailed specification: technology list (Spring Boot, JPA, Flyway, Lettuce, Bucket4j,
   Caffeine, Vaadin/Jmix, Testcontainers), the two architectural assumptions, the DB schema with
   its constraints, the use cases MUC1-MUC6 / CUC1 and the testing conventions.
3. `AGENTS.md` (coding standards: methods <= 30 lines, one public type per file, constructor
   injection, no inline fully-qualified names, 100-character lines, "functions as comments").

_Add later prompts (fix-ups after running `./test.sh`, review requests) here, in sequence._

## Generated artefacts and decisions taken by the AI

Accepted as generated (after review against the spec):

* framework-free core with `UnitOfWorkPort` and a caching decorator instead of annotations on
  services (D2);
* Lua sliding-window log for `moving-window` because Bucket4j lacks an exact sliding window (D3);
* policy fingerprint in the counter key (D4), fail-closed on Redis errors (D5);
* two foreign-key columns for the polymorphic assignment target (D6) and DB triggers as a second
  line of defence (D7);
* Redis pub/sub broadcast for cache eviction across instances (D8).

Alternatives the AI considered and rejected while designing (see 03 for the reasoning):

* a single polymorphic `resource_id` column exactly as written in the spec - rejected in favour of
  two real foreign keys while keeping the API field name;
* `@Transactional` / `@Cacheable` directly on application services - rejected to keep the core
  framework-free;
* plain string matchers in Spring Security - rejected for explicit Ant matchers because the
  additional Vaadin servlet makes string patterns ambiguous.

Changed after the first version was written:

* An overloaded `inTransaction(Runnable)` next to `inTransaction(Supplier)` was renamed to
  `runInTransaction` to avoid ambiguous lambda overload resolution.

## Incorrect or misleading output found and corrected

| Problem in generated output | How it was found | Fix |
|---|---|---|
| `seed.sql` started with `BEGIN; ... COMMIT;` - inside `@Sql` this would commit the test transaction and break rollback isolation | review of how `@Sql` joins the test transaction | transaction control moved to `psql --single-transaction` in `seed.sh` |
| PL/pgSQL `RAISE EXCEPTION` with a message split over two adjacent string literals | SQL review | single literal |
| A test expected a CHECK-constraint error, but the overlap trigger (BEFORE INSERT) would fire first for the chosen data | reasoning about trigger vs constraint order | test data changed to a non-overlapping consumer group |
| Validation errors reported Java field names (`numberOfRequests`) while the API is snake_case | test expectation mismatch during review | handler converts field names to snake_case |
| Inline fully-qualified names (`java.util.Optional`, `java.util.LinkedHashMap::new`) violating AGENTS.md | automated scan | replaced by imports |
| Token-bucket test with a per-minute policy could pass one extra request if the run is slow (greedy refill) | review of timing assumptions | test uses an hourly policy |
| Lines over 100 characters | automated scan | wrapped |

## Review guidance for the human developer

Because the AI could not build the project, the first `./test.sh` run is the real verification.
Library APIs that were used from memory and deserve attention if compilation fails:
`Bucket4jLettuce.casBasedBuilder(...)` (Bucket4j 8.14), Testcontainers 2.0 artefact coordinates,
Vaadin 24.8 component APIs, and `AntPathRequestMatcher` (deprecated in Spring Security 6.5).
