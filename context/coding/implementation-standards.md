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

### Angular frontend styling uses only Tailwind CSS

**ID: COD-005**

All frontend layout and presentation is expressed with Tailwind utility classes in templates. Component stylesheets and component-library themes (such as Angular Material) are not used. The single global stylesheet contains only the Tailwind import, the theme tokens derived from the project logo (blue, cyan, and orange-red accents), the dark-mode variant, and base element defaults. Angular CDK may be used for behavior and accessibility primitives such as dialogs and focus handling, never for visual styling.

**Rationale:** One styling mechanism keeps operator UI changes colocated with their markup, makes theming consistent, and prevents parallel CSS systems from accumulating.

**Constraints:** Responsive, dark-theme, focus, and state styling is part of every component. The ESLint configuration and production build must fail on component `styles`/`styleUrl` usage.

**Dependencies:** ADR-002, UX-005, UX-006, UX-008, COD-009

**Verification expectation:** Lint and the Angular production build pass; review confirms no component stylesheets or Material imports exist.

### Angular frontend structure is standalone, lazy, and linted

**ID: COD-009**

The frontend uses standalone components and a route table in which every feature area is loaded lazily as its own chunk. Access to feature routes is protected by a functional authentication guard that redirects unauthenticated operators to the login route. API access is confined to dedicated API services (COD-001). The frontend is linted with `angular-eslint`, and lint is part of `make test`.

**Rationale:** Small per-route chunks keep the initial load light, and automated rules keep the structure and styling conventions enforced rather than aspirational.

**Constraints:** The route structure is `/login`, `/` (dashboard), `/deployments`, `/updates`, `/secrets`, and `/settings/tokens`. Unknown paths redirect to the dashboard.

**Dependencies:** ADR-002, COD-001, COD-005, UX-007, QUA-004

**Verification expectation:** The production build output shows a separate chunk per feature route; route and guard tests pass; lint passes.

### Frontend features are developed test-first

**ID: COD-010**

Frontend behavior is specified by Vitest tests (Angular unit-test builder, jsdom) written before the implementation: API services, the authentication guard, routing, the theme service, and each page's states and operator interactions.

**Rationale:** The frontend previously had no tests, so its operator workflows were unprotected during change.

**Constraints:** Tests do not depend on a live backend or a locally installed browser; HTTP is mocked. Frontend dependencies track the latest stable releases compatible with the Angular version in use.

**Dependencies:** QUA-004, COD-009

**Verification expectation:** `npm test` runs without watch mode and passes, and each feature route has at least one test of its main states.

### README operator guidance stays aligned with the implementation

**ID: COD-006**

When a change affects user-visible behavior, supported configuration, CLI operations, API contracts, security controls, or deployment/setup instructions, update the README in the same change. Keep commands, variable names, defaults, examples, and security guidance consistent with the implemented behavior; remove superseded instructions rather than leaving conflicting alternatives.

**Rationale:** The README is the primary operator-facing setup and usage reference. Stale commands or configuration examples can cause failed deployments or unsafe security settings even when the implementation is correct.

**Constraints:** Document supported behavior and stable operational guidance, not source-code details. When the README intentionally describes a future capability or limitation, label it clearly rather than implying it is implemented.

**Dependencies:** COD-004, ADR-001, ADR-002, QUA-006

**Verification expectation:** For affected changes, review README examples and claims against the current runtime configuration, CLI, API, and security behavior; validate commands/examples where practical.

### The Makefile is the single entry point for local development and testing

**ID: COD-007**

**Status:** Proposed.

**Basis:** QUA-007, QUA-008, and the maintainer request for a professional manual test setup that is easy to start and use.

The repository root `Makefile` is the only supported entry point for local development and test workflows, and `make help` lists its commands. `make dev` starts the backend and the dashboard together, streams the output of both with a per-service prefix, and loads the default scenario when none is loaded. `make scenario SCENARIO=<name>` loads the named scenario from `dev/scenarios/<name>`. `make reset` erases the environment's state and refuses to run while the backend is running. `make test` runs the backend tests, the frontend lint, the frontend tests, and the frontend production build. Environment state lives only in `dev/.state`, which is excluded from version control and from build contexts. New local workflows are added as Makefile targets with a help description rather than as separate scripts.

**Rationale:** One discoverable, documented entry point keeps manual testing repeatable and avoids divergent ad-hoc scripts.

**Constraints:** The Makefile does not add production behavior. Test-only code and configuration live in test sources (SEC-012). Scenario content contains no real credentials. The toolchain version follows the project build configuration.

**Dependencies:** QUA-007, QUA-008, SEC-011, SEC-012, COD-006

**Verification expectation:** `make help` lists the commands above; running `make reset` then `make dev` yields a reachable dashboard with the default scenario loaded; the README section on the manual test environment matches these commands.

### Python is not used for repository tooling

**ID: COD-008**

**Status:** Proposed.

**Basis:** Maintainer decision that Python is not wanted in this repository or its working tooling.

Repository scripts, build and test tooling, and one-off maintenance or automation steps performed for the repository do not use Python. They use the project's existing toolchain (Make, shell utilities, Maven, npm) or the editing facilities of the development environment. The repository contains no Python source files.

**Rationale:** The project is a Spring Boot and Angular codebase; adding a further language runtime increases the toolchain contributors must install and maintain.

**Constraints:** This applies to files committed to the repository and to tooling that the repository documents or depends on. It does not restrict tools that third-party components bring along internally.

**Dependencies:** COD-007

**Verification expectation:** Review confirms no `.py` files are tracked and that documented commands and Makefile targets do not invoke a Python interpreter.
