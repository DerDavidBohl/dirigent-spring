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

Operational flows use Spring application events and asynchronous listeners for deployment commands and update operations. The backend test suite includes focused coverage for process execution, registry and update parsing/authentication, local-account and machine-token services, method-level authorization, and webhook signature verification. Deployment lifecycle, secret lifecycle, the signed-webhook controller contract, and frontend component workflows still lack dedicated tests.

**Rationale:** Operational correctness depends on clear sequencing and predictable behavior in a system that interacts with Docker and external registries.

**Constraints:** API command handlers generally publish work and return before completion. There is no operation ID or shared async result abstraction. Database schema changes rely on ORM update behavior; no explicit migration framework is established in the current project context.

**Dependencies:** QUA-001, QUA-004, ADR-003

**Verification expectation:** Review the focused backend tests under `backend/src/test` when changing their covered behavior. Changes to the untested deployment, secret, webhook-controller, or frontend workflows need new focused tests before a claim of behavioral coverage is made.

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

### Angular component-specific styling uses Tailwind utilities

**ID: COD-005**

New and modified Angular component-specific layout and presentation should use Tailwind utility classes in templates rather than adding or extending component stylesheet rules. Shared base styles and narrowly scoped overrides for global or third-party components may remain in global stylesheets. When modifying an existing component stylesheet, migrate the touched component-specific rules to utilities while preserving established visual behavior and responsive states.

**Rationale:** A consistent utility-based styling convention keeps new operator UI changes colocated with their markup and avoids growing parallel component CSS patterns.

**Constraints:** This standard does not require a repository-wide rewrite of existing component styles. It does not permit removing responsive, theme, focus, or state styling without preserving the corresponding behavior.

**Dependencies:** ADR-002, UX-005, UX-006

**Verification expectation:** Review confirms new component presentation is expressed with Tailwind utilities, component-specific stylesheets are not introduced for new work, and the Angular production build passes.
