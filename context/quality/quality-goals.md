# Quality goals

### Deployment orchestration must be reliable enough for operator trust

**ID: QUA-001**

The system should behave predictably when handling deployment lifecycle actions, registry checks, and configuration refreshes so operators can rely on the dashboard and backend as the source of operational truth.

**Rationale:** Deployment automation depends on correctness and predictable handling; unclear or flaky behavior undermines self-hosted operations.

**Constraints:** Reliability is constrained by the external Docker environment, host configuration, and network access to registries. A configured registry that is unreachable or misconfigured must not block the start of a deployment that does not depend on it; such a registry login failure is reported as a warning rather than a start failure.

**Dependencies:** REQ-001, REQ-002, ADR-001

**Verification expectation:** Operational and automated tests validate lifecycle actions, state reporting, and the supported failure scenarios relevant to the system, including that an unreachable or misconfigured registry degrades to a warning instead of failing unrelated deployment starts.

### Secret and registry handling must minimize accidental exposure

**ID: QUA-002**

The system should minimize the chance of sensitive material being exposed through logs, UI responses, or raw configuration handling.

**Rationale:** Secret exposure would undermine the trust model for a deployment orchestration tool and compromise the infrastructure it manages.

**Constraints:** Complete prevention depends on secure host practices and runtime configuration; the project can only define the intended handling boundaries.

**Dependencies:** REQ-003, SEC-001, SEC-002

**Verification expectation:** Review and tests confirm that secret values are not rendered in standard responses and are bound to the expected runtime flow.
