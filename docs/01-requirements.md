# Requirements and assumptions

## Assumptions

1. As required by implementation detail N1, the limiter is a standalone reverse proxy and can protect a target service regardless of its implementation language.
2. As required by N2, the target publishes OpenAPI at `/openapi.json`; managers trigger discovery explicitly.
3. A bearer token's opaque token value is the consumer ID. Tokens are not cryptographically validated in this exercise.
4. Policies apply to an HTTP method and normalized path. Exact-resource rules take precedence over resource-group rules; absent a rule, a configurable default applies.
5. Counters use Redis fixed windows for atomicity across proxy instances. The policy strategy is retained as configuration; a production implementation should map every supported strategy to its corresponding algorithm.

## Essential requirements

Policy/resource/consumer-group management, target discovery, transparent request forwarding, shared rate enforcement, JSON errors, default limits, local Docker Compose startup, tests and documentation.

## Ambiguities

The prompt does not define identity verification, path parameter matching, precedence when multiple assignments match, counter semantics at boundaries, or how group membership changes are authorized. This implementation treats incoming bearer text as identity and synchronizes exact OpenAPI paths.

## Product questions

Confirm identity provider and token claims; whether quotas span methods or path templates; strategy and precedence rules; policy update semantics for active windows; admin authentication; and expected handling of upstream timeouts/streaming.

## Out of scope

Production authentication, TLS termination, high availability deployment, sophisticated distributed cache invalidation, streaming uploads/responses, and migration/version management beyond local schema creation.
