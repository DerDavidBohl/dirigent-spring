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

### Gitea webhook requests require a dedicated machine-token permission

**ID: SEC-004**

Gitea webhook requests are accepted only when authenticated with a valid, unexpired, unrevoked machine token granted the dedicated `webhooks:trigger` permission. The webhook does not require a payload HMAC signature. HTTPS and network restriction remain defense in depth.

**Rationale:** A forged webhook can trigger privileged deployment operations. A dedicated, revocable, least-privilege machine token gives the integration a distinct identity and avoids coupling webhook access to a human credential or broader API token.

**Constraints:** The token must be sent in the `Authorization: Bearer` header, kept confidential, and issued without unrelated permissions where possible. Missing, invalid, expired, revoked, or incorrectly scoped tokens are rejected before request processing. Operators should restrict endpoint reachability and use HTTPS. Token authentication does not prevent replay of a previously captured valid request; event-type validation and deduplication remain outside this control.

**Dependencies:** ADR-001, REQ-001, SEC-003

**Verification expectation:** Tests verify rejection of anonymous and incorrectly scoped callers and acceptance of a valid `webhooks:trigger` token. Deployment guidance explains bearer-token configuration, secure storage, expiry/revocation, HTTPS, and network restriction.

### Dashboard and REST API require application authentication

**ID: SEC-005**

All dashboard and REST API access requires an authenticated administrator session or an authorized machine token. Unauthenticated access is denied by default. Network restriction remains defense in depth and does not replace application authorization.

**Rationale:** The API can trigger privileged deployment operations and manage sensitive configuration, so network location alone is not a sufficient identity or authorization boundary.

**Constraints:** Browser sessions use secure cookie attributes and CSRF protection for state-changing requests. Authorization is deny-by-default. Operator access remains highly privileged because the supported human role can control deployments and secrets.

**Dependencies:** ADR-001, ADR-002, SEC-003, CLR-002

**Verification expectation:** Security tests verify anonymous denial, authenticated access, authorization enforcement, CSRF protection, and safe session invalidation. Operator guidance recommends TLS and network restriction as additional controls.

### Sensitive values and credentials are excluded from diagnostics

**ID: SEC-006**

The current diagnostics do not guarantee redaction of secrets and credentials. Process command strings and process output are written to debug logs; Git operations can pass authenticated repository URLs to the process runner; secret-key validation includes the configured key in its exception message. Deployment and update diagnostics may also include process stderr or exception text.

**Rationale:** Diagnostics are often retained and shared more broadly than the secret stores they describe; they must not become an alternate credential disclosure channel.

**Constraints:** These paths can disclose credentials when debug/error logging is enabled or exceptions are retained. Operators should treat logs and diagnostic output as sensitive. No systematic redaction layer is currently specified or guaranteed.

**Dependencies:** SEC-001, SEC-002, SEC-005, QUA-002

**Verification expectation:** Existing tests do not verify log/exception redaction; such checks would be needed before claiming that sensitive diagnostic data is excluded.

### Local and OIDC identities use separate authorization controls

**ID: SEC-007**

Local accounts use a one-way password hash suitable for password storage and are created only by an operator through the server-side CLI. OIDC identities are authorized after successful authentication by the configured HTTPS issuer without per-user provisioning. Operators may optionally require membership in a configured provider group claim; when set, users missing that group are denied. There is no public local-account signup.

**Rationale:** Local credentials and federated identity have different lifecycle controls. Trusting the issuer avoids duplicate per-user provisioning; an optional group restriction narrows the trusted identity-provider population when required.

**Constraints:** OIDC client secrets, group claims, and local credentials are sensitive. The issuer must use HTTPS. With no group configured, all authenticated users of that issuer receive administrator access. Local passwords must never be logged or supplied as CLI arguments. The deployment must retain a documented local-account recovery path.

**Dependencies:** REQ-014, REQ-016, ADR-007

**Verification expectation:** Tests establish password-hash storage, acceptance of authenticated OIDC users without per-user provisioning, optional group enforcement, and that credentials do not appear in logs or command arguments.

### OIDC discovery requires a secure issuer transport

**ID: SEC-009**

OIDC issuer metadata and authentication endpoints must use HTTPS. The configured issuer must be validated as an absolute HTTPS URL before provider discovery; plaintext HTTP issuers are rejected.

**Rationale:** OIDC metadata, authorization codes, and tokens cross a security boundary and must not be exposed to network interception or modification.

**Constraints:** No HTTP exception is provided for local or private identity providers. Operators must make their test provider available over HTTPS when exercising OIDC.

**Dependencies:** REQ-014, ADR-007

**Verification expectation:** Tests reject HTTP, relative, and malformed issuer values and accept a valid HTTPS issuer.

### Machine tokens are scoped and stored as hashes

**ID: SEC-008**

Machine tokens are generated from cryptographically secure random data, stored only as a non-reversible verifier, granted only the permissions selected at issuance, and revocable by an administrator. Expiring tokens are rejected after expiry. The complete token is disclosed only once at creation.

**Rationale:** Leaked automation credentials must not expose human passwords or grant broader API access than the integration requires.

**Constraints:** Token permissions must be enforced for every protected API operation, including the dedicated `webhooks:trigger` operation. Every token has a mandatory expiry within 366 days and is rejected after expiry or revocation. Token values and verifiers are sensitive and must not appear in logs or ordinary API responses.

**Dependencies:** REQ-015, ADR-007

**Verification expectation:** Tests verify high-entropy issuance, verifier-only persistence, one-time disclosure, permission checks, expiry, and revocation.

### Local login attempts are rate limited

**ID: SEC-010**

Repeated failed local login attempts from one client address are rate limited. The control must limit guessing without trusting caller-supplied forwarding headers unless they are supplied by a configured trusted proxy.

**Rationale:** Publicly reachable password login is vulnerable to automated password guessing and credential stuffing.

**Constraints:** Rate-limit state may be process-local for the supported single-instance deployment. Operators must apply corresponding controls at a trusted reverse proxy when multiple instances or distributed attack resistance is required. Authentication failures must continue to use generic responses that do not disclose account existence.

**Dependencies:** REQ-014, ADR-005, QUA-005

**Verification expectation:** Tests verify the configured failure threshold blocks subsequent login attempts for the defined window, successful login clears or expires the applicable state, and ordinary protected routes are unaffected.

### Local configuration secrets are excluded from build artifacts

**ID: SEC-011**

Local development configuration, credentials, encryption keys, databases, and other operator-local secret material must be excluded from container build contexts and distributable application artifacts. Production secrets are supplied through runtime secret/configuration mechanisms rather than copied into the image.

**Rationale:** A secret excluded from version control can still be disclosed if copied into a container image, build cache, or packaged JAR.

**Constraints:** Build exclusions must not remove safe configuration templates or required application resources. If a local secret has been included in an artifact, operators must treat it as disclosed and rotate it.

**Dependencies:** SEC-001, SEC-006, ADR-001, ADR-005

**Verification expectation:** Build review confirms local-only files are excluded, and an image/JAR inspection finds no local credentials or encryption keys.

### Test credentials exist only in the non-production test environment

**ID: SEC-012**

**Status:** Proposed.

**Basis:** QUA-007 and the maintainer request for a ready-to-use test account; SEC-007 and SEC-011 are the controls it must not weaken.

The manual test environment may create a fixed, well-known local administrator account and a throwaway encryption key so that testing needs no manual setup. This is the only exception to creating local accounts solely through the operator CLI (SEC-007). It applies only when an explicit test-environment mode is enabled, is never active in production configuration or the production image, and is reachable from the local machine only.

**Rationale:** Convenience credentials are acceptable for disposable local data only if they cannot be activated or reached in a real deployment.

**Constraints:** Test-environment configuration, databases, and tooling are excluded from distributable artifacts (SEC-011). Production startup never creates accounts automatically.

**Dependencies:** SEC-007, SEC-011, QUA-005, QUA-007

**Verification expectation:** Tests or review show that without the test-environment mode no account is created automatically, and that the production image and run configuration do not enable it.

### CI workflow inputs and publication permissions are constrained

**ID: SEC-013**

Workflow dispatch inputs and repository-dispatch payloads are untrusted data. They must not be interpreted as shell source, and values used as release versions or Git references must be validated against their allowed format before use. Workflow jobs receive only the permissions required for their task; package publication and branch updates are restricted to the jobs that perform those operations. Secrets and write-capable credentials are not exposed to pull-request code.

**Rationale:** CI workflows can hold repository write credentials, signing material, and package-publishing access. Unsafe input handling or unnecessarily broad permissions can turn a workflow trigger into credential exposure or unauthorized publication.

**Constraints:** Repository settings and branch protections remain part of the trust boundary. Pull-request validation must remain available without write-capable credentials. Release credentials are maintained as protected repository secrets and must be scoped to the repository operations they require.

**Dependencies:** SEC-006, SEC-011, QUA-004

**Verification expectation:** Review confirms untrusted values are validated before use, are not interpolated into shell programs, and write permissions are limited to publishing or branch-synchronization jobs. Pull-request jobs have read-only repository permissions and no publication credentials.

### Federation credentials and outbound requests are constrained

**ID: SEC-014**

The federation hub treats remote instance URLs and machine tokens as sensitive operator-provided configuration. It accepts only absolute HTTPS URLs without embedded user information, query, or fragment; redirects are not followed. Tokens are used only for the explicitly allowlisted operations on that configured instance and must never be logged, returned to callers, or persisted in application data. Federation requests use bounded connection and response timeouts, and failures are isolated by instance.

**Rationale:** The hub holds credentials capable of reading operational metadata from multiple hosts, and unrestricted outbound URLs or token leakage could expose other services or instances.

**Constraints:** Remote instances must be reachable using the hub's normal TLS trust store; private-network HTTPS endpoints are allowed because the operator explicitly configures them. Tokens must include only the scopes needed by the supported workflows (`deployments:read/write`, `updates:read/write`, `secrets:read/write`, and `system:read`), must be expiring and revocable under SEC-008, and should be rotated by replacing runtime configuration. They must not include `webhooks:trigger`. The hub must not expose remote account, OIDC, or machine-token administration. Remote `401` and `403` responses must not be returned as hub authentication failures or terminate the hub browser session. The credential set grants substantial operational authority; compromise of the hub process or host can compromise every configured remote.

**Dependencies:** ADR-009, REQ-015, REQ-020, SEC-006, SEC-008

**Verification expectation:** Tests reject non-HTTPS or malformed URLs, ensure authorization headers are sent only to the configured origin and are not exposed in logs/responses, confirm finite timeouts and no redirect following, verify remote authorization failures do not invalidate the hub session, and verify one remote failure does not fail other operations or instance results.

### Federated secret values are transient and excluded from diagnostics

**ID: SEC-015**

Secret values handled through federation are forwarded only to the selected remote instance over HTTPS. The hub does not persist, cache, or include secret values in logs, audit records, error messages, or response metadata beyond the API response required by the operator workflow. Audit records for remote writes identify the authenticated hub operator, target instance, operation, timestamp, and outcome without recording request bodies or credentials.

**Rationale:** A central control plane increases the impact of a secret disclosure and must not become a second secret store or diagnostic leak path.

**Constraints:** The remote instance remains responsible for its existing secret encryption and lifecycle controls (SEC-001). This control does not eliminate the risk of hub process/host compromise or the existing diagnostic risks described by SEC-006.

**Dependencies:** ADR-009, REQ-020, SEC-001, SEC-006, SEC-014

**Verification expectation:** Tests verify secret request bodies are sent only to the selected remote, do not appear in hub persistence/logging/returned error text, and write audit metadata excludes bodies and bearer tokens.

### Native HTTPS and pinned self-signed certificates preserve peer identity

**ID: SEC-016**

Native HTTPS is enabled by default. On first startup, Dirigent creates and persists a self-signed certificate and private key; operators may supply a PKCS#12 keystore instead. A federation client may trust one remote leaf certificate by its SHA-256 fingerprint. HTTPS hostname verification and certificate validity-period checks remain enabled; a pin mismatch, expired certificate, or hostname mismatch fails closed. Without a pin, the JVM's ordinary certificate trust rules apply.

**Rationale:** Default direct TLS removes plaintext exposure without requiring a reverse proxy, while explicit pinning allows controlled self-signed certificates without accepting arbitrary certificates.

**Constraints:** Generated and supplied certificates must identify the DNS name or IP address used in the remote base URL. Generated private key material must be persisted with restrictive file permissions and must not be exposed in logs or application responses. Self-signed certificates are not automatically trusted by callers; explicit pinning or CA trust is required. Keystore files/passwords and certificate pins are sensitive operational configuration. Operators must renew certificates, update pins before expiry/rotation, and distribute trust changes through a secure channel. HTTP compatibility mode is only appropriate behind a trusted TLS-terminating proxy, except for the isolated local manual test environment, which must bind the backend to loopback. Do not provide a trust-all mode or disable endpoint identification.

**Dependencies:** ADR-010, REQ-021, SEC-005, SEC-014

**Verification expectation:** Tests verify safe first-start key generation and persistence, BYOC precedence, restrictive key-file permissions, rejection of unpinned/mismatched/expired/hostname-mismatched certificates, and successful use of a valid configured pin. Documentation explains hostname configuration, key protection, trust, and certificate rotation.
