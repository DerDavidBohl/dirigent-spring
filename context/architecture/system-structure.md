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

Each deployment is described by a repository-backed source and optional order metadata so deployment dependencies can be resolved when operations are triggered.

**Rationale:** Automation in self-hosted environments benefits from deterministic ordering and clear dependency boundaries.

**Constraints:** Dependency order is a coordination aid, not an implicit trust model; it must not override explicit operator intent.

**Dependencies:** REQ-001, REQ-002

**Verification expectation:** Deployment operations reflect the configured order when multiple deployments are started or updated together.
