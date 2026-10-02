# Clarification register

This file tracks clarification questions and their resolution status. Resolved decisions are summarized here for traceability and specified normatively in their owning context records. Open items are not permission to infer behavior from the implementation; resolve them through the context workflow before relying on an answer.

### Lifecycle state meanings and transitions

**ID: CLR-001**

Resolved against the current code by REQ-002. The implementation exposes the listed state vocabulary and persists event-driven updates. State messages describe the latest event, and stop reports `STOPPED` after the process runner returns even when its exit code is nonzero. There is no comprehensive transition test suite.

**Status:** Resolved.

**Dependencies:** REQ-002

### Dashboard and REST API access control

**ID: CLR-002**

Resolved by SEC-005. Application-level authentication is not currently provided; operators must restrict network access to trusted users and systems. Future application authentication remains a suggested improvement, not an approved current or future delivery requirement.

**Status:** Resolved for the current access model.

**Dependencies:** SEC-005, ADR-001, ADR-002

### Quantitative service-quality targets

**ID: CLR-003**

Resolved by QUA-004. No numeric availability, latency, recovery-time, or scale commitment is required until operational measurements and a workload definition support setting one; the question should be revisited when those measurements exist.

**Status:** Resolved for the current release policy.

**Dependencies:** QUA-004

### Deployment configuration and reconciliation rules

**ID: CLR-004**

Resolved against the current code by REQ-001, REQ-004, REQ-007, ADR-003, ADR-005, and ADR-006. YAML entries are parsed without explicit required-field or uniqueness validation; `order` defaults to zero and a null `ref` is passed as `HEAD`. Read/parse failures propagate without a last-known-good fallback. Order groups run ascending and sequentially; handled deployment failures continue, while uncaught runtime exceptions may abort orchestration. Removed checkouts are deleted after the stop process returns, without checking its exit code.

**Status:** Resolved.

**Dependencies:** REQ-001, REQ-004, REQ-007, ADR-003, ADR-005, ADR-006


### Asynchronous API completion and error reporting

**ID: CLR-005**

Resolved against the current API by REQ-008 through REQ-013 and COD-004. Command endpoints acknowledge requests before asynchronous work necessarily completes. Clients poll existing deployment or update resources for progress; no operation IDs are used. Error envelopes remain endpoint/framework-specific rather than standardized. Caught update-apply failures are logged and published as events for optional notification; update rows are removed and no durable result is retained.

**Status:** Resolved.

**Dependencies:** ADR-001, ADR-002, ADR-004, REQ-002, REQ-004, CLR-002

Deployment lifecycle outcomes are exposed through state/message, subject to the limitations in REQ-002. Update progress uses `isRunning`, while final update-apply outcome is not queryable through the API. The webhook does not inspect an event-type header; any compatible body can publish an event. There is no explicit deduplication.

The current API schemas and paths are normative as listed in REQ-008 through REQ-013. Validation errors retain endpoint/framework-specific behavior; a shared error envelope is not part of the current contract.

### Secret lifecycle and replacement semantics

**ID: CLR-006**

Resolved against the current code by REQ-003, REQ-010, UX-003, and SEC-001. The key string identifies the record; missing or JSON-null values are not written on update, while empty strings are encrypted. Deletion removes the record and its associations. The configured 16-character key is used with the Java `AES` transformation without an explicit nonce/tag or rotation workflow. Secret listings omit values; the editor does not explain retain-versus-replace behavior.

**Status:** Resolved.

**Dependencies:** REQ-003, SEC-001, UX-003, QUA-002

Secret associations may refer to names that are not currently configured. The path key identifies the stored record; the request body's key field is not used by the controller.

### Image-update discovery and application rules

**ID: CLR-007**

Resolved against the current code by REQ-006 and REQ-011. Discovery queries Compose-labeled containers including stopped containers, resolves image references, and compares registry manifest digest strings with local Docker image IDs without explicit platform selection. Apply runs Compose `up --pull always --force-recreate`; its exit code is not checked. Update rows are deleted after the async attempt, so no durable result or rollback is available.

**Status:** Resolved.

**Dependencies:** REQ-001, REQ-002, SEC-002, UX-004, QUA-001


### Registry and notification integration tuning

**ID: CLR-008**

SEC-002, QUA-001, QUA-003, REQ-006, and SEC-006 describe current registry, notification, and diagnostic behavior.

**Status:** Resolved.

**Dependencies:** SEC-002, QUA-001, QUA-003, UX-004

One optional Gotify destination is configured by base URL and token; four event categories are fixed. The private internally invoked `@Retryable` method catches `Throwable`, so effective retries are not guaranteed. Registry login failures are caught and logged; digest lookup failures are logged and skipped. A nonzero start `up` result sets `FAILED`; update-apply exit codes are not checked.

### Operator workflows and accessibility expectations

**ID: CLR-009**

Resolved against the current UI by UX-001 through UX-005. The deployments page displays state/message and polls every two seconds; the updates page polls update rows and shows request/loading indicators. No last-refresh timestamp or explicit deployment stale/error state is shown. Start has a force-recreate dialog, Stop is direct, and secret deletion requires a second click. The editor does not explain value retention. WCAG 2.2 AA conformance has not been established.

**Status:** Resolved at the normative workflow level.

**Dependencies:** UX-001, UX-002, UX-003, UX-004, REQ-002


### Installation and operational configuration contract

**ID: CLR-010**

Resolved against the current architecture by ADR-005 and ADR-006. The service expects Docker access, SQLite persistence, configuration/deployment directories, and optional external integrations. No backup/restore, key rotation, or versioned database migration workflow is implemented in the described architecture. Reconciliation can be requested again but has no transactional or exactly-once guarantee.

**Status:** Resolved at the supported-topology level.

**Dependencies:** ADR-001, REQ-005, SEC-001, SEC-003, QUA-001


### Measurable quality and verification expectations

**ID: CLR-011**

Resolved against the current test setup by QUA-004. Backend tests cover process invocation and selected registry/image parsing/authentication paths. Dedicated secret, config-validation, webhook, lifecycle-controller, frontend, and accessibility tests are absent. No enforced release gate or numeric service target is established.

**Status:** Resolved for the current verification policy.

**Dependencies:** QUA-001, QUA-002, QUA-003, CLR-003


### Implementation and compatibility standards

**ID: CLR-012**

Resolved against the current code by COD-004. Request and YAML validation mostly rely on framework defaults; only deployment-not-found has an explicit controller-local ProblemDetail handler. Async operations return before completion and have no operation IDs. Persistence uses ORM schema update behavior without explicit migrations. Error formatting and validation behavior are not uniform.

**Status:** Resolved at the normative convention level.

**Dependencies:** COD-001, COD-002, COD-003, ADR-001, ADR-002, QUA-001