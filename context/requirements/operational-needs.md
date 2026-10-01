# Operational requirements

### GitOps deployment management must be repository-driven

**ID: REQ-001**

Dirigent manages Docker Compose deployments from a repository-backed deployment configuration, allowing operators to define and update deployments in version control rather than by ad hoc host commands.

**Rationale:** The project’s purpose is to simplify deployment coordination for self-hosted infrastructure by changing infrastructure state through Git-driven configuration.

**Constraints:** Repository definitions must remain explicit and reviewable; host-specific actions are still required for Docker runtime access and local secrets.

**Dependencies:** ADR-001, ADR-002

**Verification expectation:** The operator can configure deployment sources and observe deployment actions through the system without direct shell-based orchestration.

### Deployment lifecycle control must cover start, stop, and state visibility

**ID: REQ-002**

The system must expose a clear lifecycle for managed deployments, including starting, stopping, and reporting current deployment state and messages to operators.

**Rationale:** A deployment manager must provide enough operational transparency for users to understand what is active, what failed, and what action is pending.

**Constraints:** State reporting must distinguish between unknown, running, stopped, and failed or degraded states when the system has sufficient evidence.

**Dependencies:** ADR-003, UX-001

**Verification expectation:** Acceptance testing confirms the UI and API expose the deployment state and allow explicit lifecycle actions.

### Secret handling must protect environment-sensitive values

**ID: REQ-003**

Sensitive values used by deployments, including secret-bearing environment variables, must be stored and managed in a way that avoids disclosure in plain view while supporting runtime injection into deployment containers.

**Rationale:** Infrastructure automation is exposed to credential and configuration risk; secret handling is central to safe self-hosted operation.

**Constraints:** Secret encryption must not depend on a public code repository or a plaintext secret file placed in the deployment source.

**Dependencies:** SEC-001, SEC-002

**Verification expectation:** Security review confirms secret values are protected at rest and only provided to the runtime process when needed.
