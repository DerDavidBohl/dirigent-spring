# Operational security

### Secret values must be protected at rest

**ID: SEC-001**

The system protects sensitive deployment values using encryption before persistence, while ensuring the decrypted value is only available in the runtime path required by the deployment process.

**Rationale:** Deployment secrets are high-value operational data and must not be stored in plain text where the database or backup material could expose them.

**Constraints:** Secret values are transformed with the Java `AES` cipher using a configured 16-character key. The cipher mode, nonce, and authentication tag are not specified by the application. The key is supplied through application configuration and is not stored in the secret database. No key-rotation/re-encryption workflow is provided. Treat encrypted database contents and the key as sensitive; possession of both permits recovery of stored values. The described protection does not establish authenticated encryption or tamper detection.

**Dependencies:** REQ-003, ADR-001

**Verification expectation:** Review confirms encrypted values are persisted and ordinary secret-list responses return null values. Tests do not currently establish confidentiality properties, authenticated encryption, nonce handling, or key rotation.

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

### Gitea webhook access is restricted by the trusted network boundary

**ID: SEC-004**

Gitea webhook requests are trusted only when the deployment environment restricts access to the webhook endpoint to trusted systems. Application-level signature verification is not required by the current operating model.

**Rationale:** Webhooks provide an automated repository-to-deployment trigger, while this deployment model places caller trust at the operator-managed network boundary.

**Constraints:** The application does not check a webhook event-type header or signature. The receiver trusts its network boundary; any caller able to reach it and provide a compatible body can cause a reconciliation/source-start request. Operators are responsible for restricting endpoint reachability.

**Dependencies:** ADR-001, REQ-001, SEC-003

**Verification expectation:** Deployment review confirms that network controls limit webhook access to trusted Gitea systems and that the operator-facing deployment guidance states this responsibility.

### Dashboard and REST API access is restricted by the operator's network boundary

**ID: SEC-005**

The current dashboard and REST API do not provide application-level authentication. Operators must restrict network access to trusted users and systems. Future application authentication is a potential improvement, not a current capability or guarantee.

**Rationale:** The current operating model has no application identity mechanism, while the API can trigger privileged deployment operations and manage sensitive configuration.

**Constraints:** Do not expose the dashboard or API to untrusted networks. Network controls are operator responsibilities and must not be mistaken for application authorization.

**Dependencies:** ADR-001, ADR-002, SEC-003, CLR-002

**Verification expectation:** Operator-facing deployment guidance states the current absence of application authentication and the requirement to restrict network reachability.

### Sensitive values and credentials are excluded from diagnostics

**ID: SEC-006**

The current diagnostics do not guarantee redaction of secrets and credentials. Process command strings and process output are written to debug logs; Git operations can pass authenticated repository URLs to the process runner; secret-key validation includes the configured key in its exception message. Deployment and update diagnostics may also include process stderr or exception text.

**Rationale:** Diagnostics are often retained and shared more broadly than the secret stores they describe; they must not become an alternate credential disclosure channel.

**Constraints:** These paths can disclose credentials when debug/error logging is enabled or exceptions are retained. Operators should treat logs and diagnostic output as sensitive. No systematic redaction layer is currently specified or guaranteed.

**Dependencies:** SEC-001, SEC-002, SEC-005, QUA-002

**Verification expectation:** Existing tests do not verify log/exception redaction; such checks would be needed before claiming that sensitive diagnostic data is excluded.
