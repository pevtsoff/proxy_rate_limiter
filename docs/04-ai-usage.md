# AI usage

## Tool and tasks

OpenAI Codex was used to inspect the assignment, scaffold the FastAPI proxy, database models, Docker Compose stack, documentation and initial automated test files.

## Significant prompts

1. “read carefully task_assignment/task.txt and implementation_details.txt and do the task. Observe which files are already exist and fill them in appropriately. Verify your code corresponds to the task and implementation details. Ask questions if something unclear”
2. “why there are so few tests? have you red implementation_details.txt?” This prompted review of the existing tests and additional async integration coverage for policy CRUD, assignment CRUD and conflict validation, plus rollback-isolated database and Redis counter tests.

## Review and decisions

The generated work was reviewed against both assignment documents. The proxy shape, separate target container, Redis shared counters, PostgreSQL schema, Admin UI, seed/start/test scripts, docs, and test categories were accepted. Fixed-window counters were used for atomic simplicity; the configurable strategy label is currently metadata and the documentation calls out this limitation. The local bearer token interpretation is an assumption, not production authentication. No third-party AI generated code was imported.
