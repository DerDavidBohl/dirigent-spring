# System structure

### Backend runtime model: Spring Boot service orchestrates deployment operations

**ID: ADR-001**

The system runtime is centered on a Spring Boot backend that reads deployment configuration, manages deployment state, triggers lifecycle actions, and interacts with Docker and external repositories.

**Rationale:** This matches a self-hosted deployment manager that exposes a controlled API and background runtime logic rather than a pure client-side tool.

**Constraints:** The backend must run with access to the Docker socket and deployment directory on the host, which makes host trust and privilege assumptions explicit.

**Dependencies:** REQ-001, REQ-002, SEC-001

**Verification expectation:** Architecture review confirms the backend owns orchestration responsibilities and external integrations remain within the defined operational boundary.

### Frontend presentation layer: Angular dashboard for operator oversight

**ID: ADR-002**

The frontend is a lightweight Angular interface that surfaces deployment state, secrets management, and operational actions for the operator.

**Rationale:** A web dashboard reduces the need for shell-based operations and supports a more controlled operational workflow for self-hosted administrators.

**Constraints:** The frontend is a presentation layer and must rely on the backend API for authoritative state and actions.

**Dependencies:** REQ-002, UX-001

**Verification expectation:** Acceptance review confirms the frontend presents a coherent operational view without performing privileged actions beyond the API contract.

### Deployment configuration is repository-based and ordered by dependency

**ID: ADR-003**

Each deployment is described by a repository-backed source and optional numeric order metadata. When multiple deployments are started together, lower order groups are processed before higher order groups; deployments sharing an order have no guaranteed relative completion order.

**Rationale:** Automation in self-hosted environments benefits from deterministic ordering and clear dependency boundaries.

**Constraints:** Lower numeric order groups are processed before higher groups. The current orchestration iterates deployments sequentially, including those within one order group. Start failures represented by a nonzero Compose result or caught I/O/interruption/registry-auth exception are converted to state events and iteration continues; uncaught runtime exceptions may abort the current orchestration. Equal order values do not express an explicit dependency.

**Dependencies:** REQ-001, REQ-002

**Verification expectation:** Deployment operations reflect the configured order when multiple deployments are started or updated together.

### Deployment triggers converge through the backend orchestration boundary

**ID: ADR-004**

Manual API actions, configured repository webhooks, startup synchronization, and scheduled synchronization enter the backend orchestration flow; the backend remains responsible for reconciling deployment sources and invoking runtime actions.

**Rationale:** Keeping trigger sources behind one orchestration boundary gives operators a consistent deployment lifecycle regardless of how synchronization begins.

**Constraints:** Trigger availability is configuration-dependent. Webhook caller trust is governed by SEC-004.

**Dependencies:** ADR-001, REQ-002, REQ-004, REQ-005

**Verification expectation:** Review confirms each supported trigger requests the backend deployment workflow rather than performing deployment actions in the presentation layer.

### The supported operating topology is one self-hosted instance and one Docker host

**ID: ADR-005**

The supported deployment topology is one Dirigent instance controlling one Docker host. The operator supplies Docker runtime access and durable application data. Multi-host orchestration is outside the supported architecture unless adopted by a later decision.

**Rationale:** A single-host boundary makes the trust, persistence, and operational responsibilities explicit for the self-hosted deployment manager.

**Constraints:** The Docker socket and application data must be available to the service. Application persistence uses SQLite with ORM schema update behavior. Deployment configuration is read from YAML without explicit schema validation; read/parse failures propagate, and no last-known-good configuration is retained. The application does not provide a backup/restore workflow or a secret-key rotation migration.

**Dependencies:** ADR-001, SEC-001, SEC-003, REQ-001

**Verification expectation:** Operational review confirms the supported topology and required durable assets are documented. Current automated tests do not demonstrate configuration validation, last-known-good recovery, backup/restore, or key rotation.

### Reconciliation is repeatable after interruption

**ID: ADR-006**

Deployment reconciliation can be requested again after an interrupted operation, but the system does not guarantee exactly-once execution, transactional recovery, or complete outcome reporting for every failure.

**Rationale:** Host, process, and network failures can interrupt orchestration; repeatable reconciliation is required for practical recovery.

**Constraints:** Repository events publish another asynchronous request each time; there is no deduplication. A later reconciliation re-reads configuration and re-invokes operations based on persisted state and checkout content. Some command failures are converted to state events; other runtime failures can escape the orchestration listener. Update-apply results are not retained in a durable operation history.

**Dependencies:** REQ-002, REQ-004, REQ-005, QUA-001

**Verification expectation:** Current retryability is established by repeated event/manual requests; no failure-injection suite demonstrates transactional recovery or duplicate-side-effect prevention.
