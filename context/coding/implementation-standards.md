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

Operational actions involving deployment start, stop, update checks, or secret handling should be structured to support explicit flow control and unit or integration testing.

**Rationale:** Operational correctness depends on clear sequencing and predictable behavior in a system that interacts with Docker and external registries.

**Constraints:** Tests and implementation should not remove necessary safety checks or concurrency controls where they are part of the operational model.

**Dependencies:** QUA-001, ADR-003

**Verification expectation:** Automated tests cover the primary operation flows and failure paths relevant to deployment control.
