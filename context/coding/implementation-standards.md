# Implementation standards

### Backend and frontend code must remain aligned with the same operational model

**ID: COD-001**

The backend and frontend should express a shared operational contract: backend services own privileged action execution, while the frontend presents the corresponding state and triggers the API-backed actions.

**Rationale:** Separation of responsibilities reduces ambiguity between presentation logic and system control logic.

**Constraints:** This standard does not permit the frontend to bypass backend validation or assume direct host access.

**Dependencies:** ADR-001, ADR-002, REQ-002

**Verification expectation:** Code review confirms that privileged operations are orchestrated through backend services and the UI does not duplicate privileged behavior.

### Deployment operations must be explicit and testable

**ID: COD-002**

Operational flows use Spring application events and asynchronous listeners for deployment commands and update operations. Existing tests concentrate on process and registry/update parsing utilities; core orchestration, secret lifecycle, webhook/controller, and frontend workflows do not currently have dedicated tests.

**Rationale:** Operational correctness depends on clear sequencing and predictable behavior in a system that interacts with Docker and external registries.

**Constraints:** API command handlers generally publish work and return before completion. There is no operation ID or shared async result abstraction. Database schema changes rely on ORM update behavior; no explicit migration framework is established in the current project context.

**Dependencies:** QUA-001, ADR-003

**Verification expectation:** Current regression tests are the backend tests under `backend/src/test`; no frontend component tests are present. Changes to untested flows need new focused tests before a claim of behavioral coverage is made.

### Code comments trace non-obvious behavior to normative records

**ID: COD-003**

Where a non-obvious implementation decision directly realizes or constrains a normative behavior, a concise code comment should reference the applicable context record ID. Comments must not replace clear code, tests, or the context specification, and should not assert conformance that the implementation has not demonstrated.

**Rationale:** Focused traceability helps reviewers find the intended contract without turning every declaration into repetitive annotation.

**Constraints:** Use only existing stable record IDs. When a behavior is unresolved, link to an open clarification rather than presenting an implementation choice as normative.

**Dependencies:** All applicable context records

**Verification expectation:** Review checks that comments are placed at the behavior they explain, refer to existing IDs, and do not duplicate or contradict the normative record.

### External inputs and compatibility changes have explicit contracts

**ID: COD-004**

Request-body deserialization and field validation currently use Spring/Jackson defaults; explicit bean-validation rules are not established. Only deployment-not-found has a controller-local `ProblemDetail` response; other errors use their controller/framework behavior. API v1 command methods generally acknowledge before asynchronous work completes. Persistence uses SQLite/JPA schema update behavior, not explicit versioned migrations.

**Rationale:** Boundary validation, explicit concurrency behavior, and upgrade guidance make independently maintained implementations predictable and operable.

**Constraints:** The published API routes/DTOs are the current external contract, but response error formats and exact invalid-input behavior are not uniform. Operator configuration names/defaults are documented in project deployment material; implementation property spelling and defaults remain authoritative for actual runtime behavior.

**Dependencies:** ADR-001, ADR-002, ADR-005, ADR-006, QUA-004

**Verification expectation:** Current controller contract and error behavior are partly covered by code inspection only; there are no dedicated controller contract tests, migration tests, or concurrency tests for orchestration.
