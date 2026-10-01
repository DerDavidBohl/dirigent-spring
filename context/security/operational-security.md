# Operational security

### Secret values must be protected at rest

**ID: SEC-001**

The system protects sensitive deployment values using encryption before persistence, while ensuring the decrypted value is only available in the runtime path required by the deployment process.

**Rationale:** Deployment secrets are high-value operational data and must not be stored in plain text where the database or backup material could expose them.

**Constraints:** The repository cannot guarantee the security of host-level key material beyond the operator’s environment; secret protection depends on a trusted runtime environment and key management process.

**Dependencies:** REQ-003, ADR-001

**Verification expectation:** Security review confirms encryption is used for persistence and that secrets remain excluded from normal UI or plaintext storage paths.

### Registry authentication must be scoped to the configured host and credentials

**ID: SEC-002**

Credentials for private container registries are handled only for explicitly configured registry hosts and are used for the specific operations requiring registry access.

**Rationale:** The project supports private registry images, and registry credentials must not be over-broadened beyond the configured host or used as a general-purpose secret leak.

**Constraints:** Registry credentials are a trusted operator input. The system must not aggregate credentials from arbitrary or untrusted image sources.

**Dependencies:** REQ-001, ADR-001, ADR-003

**Verification expectation:** Security review confirms credential usage is limited to the configured registry host and runtime login operations.

### Docker socket access imposes a privileged trust boundary

**ID: SEC-003**

The system assumes a trusted host environment for Docker socket access and treats deployment execution as privileged operator-controlled behavior.

**Rationale:** Container orchestration requires direct access to Docker runtime control, which creates a privileged trust boundary that cannot be fully isolated from the host operator.

**Constraints:** The system must not imply that untrusted repository content is safe to execute with unrestricted host access; the trust boundary is explicit and operator-managed.

**Dependencies:** ADR-001, REQ-001

**Verification expectation:** Operational guidance and security review must make clear that host privileges and Docker socket access are required for deployment execution.
