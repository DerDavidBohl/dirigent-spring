# Operational requirements

### GitOps deployment management must be repository-driven

**ID: REQ-001**

Dirigent manages Docker Compose deployments from a repository-backed deployment configuration, allowing operators to define and update deployments in version control rather than by ad hoc host commands.

**Rationale:** The project’s purpose is to simplify deployment coordination for self-hosted infrastructure by changing infrastructure state through Git-driven configuration.

**Constraints:** Repository definitions are read from YAML with `deployments` entries containing `name`, `source`, `order`, and optional `ref`. The implementation does not validate required/non-empty fields or duplicate names. An omitted numeric `order` is zero; a null `ref` is passed as `HEAD`. Configuration read/parse failures are propagated; no last-known-good fallback is provided. Host-specific actions are still required for Docker runtime access and local secrets.

**Dependencies:** ADR-001, ADR-002

**Verification expectation:** The operator can configure deployment sources and observe deployment actions through the system without direct shell-based orchestration.

### Operators can serve Dirigent over native HTTPS

**ID: REQ-021**

The embedded server serves HTTPS by default. On first startup, Dirigent generates a self-signed certificate and private key, stores them persistently, and reuses them on subsequent starts; operators can instead configure their own PKCS#12 certificate and key. An advertised hostname can be configured so generated certificates identify the address operators use. Federation callers can trust a remote certificate through the JVM trust store or an explicitly configured SHA-256 certificate fingerprint.

**Rationale:** HTTPS protects browser sessions, machine tokens, and federated secret operations without requiring certificate-generation expertise or a reverse proxy.

**Constraints:** Generated certificate and private key material must survive application restarts and be stored with restrictive filesystem permissions. The default self-signed certificate is not automatically trusted by browsers or remote clients; callers must explicitly trust its SHA-256 fingerprint or use the ordinary JVM trust store for a CA-issued certificate. BYOC configuration takes precedence over generated material. Operators may explicitly disable native TLS when a trusted reverse proxy provides TLS. A configured fingerprint is exact leaf-certificate trust and must not bypass hostname verification, certificate validity dates, or HTTPS-only URLs. Certificate renewal requires updating the pin when the leaf certificate changes. Generated self-signed certificates are rotated automatically before they expire, without operator action and without a restart; the new SHA-256 fingerprint is logged so operators can update pins. Operator-supplied certificates are never rotated automatically. Automatic rotation may be disabled by configuration. An operator with server access can also rotate the generated certificate immediately through a server-side CLI command that prints the new fingerprint, refuses when a BYOC keystore is configured, does not start HTTP serving, and is picked up by a running server.

**Dependencies:** ADR-009, ADR-010, SEC-005, SEC-014, SEC-016

**Verification expectation:** Tests verify first-start creation and persistent reuse, BYOC precedence, default HTTPS, explicit reverse-proxy HTTP mode, client trust limited to the configured certificate fingerprint, and enforced hostname and validity checks. Tests also verify that a generated certificate inside its renewal window is replaced (with the new fingerprint logged), one outside it is kept, BYOC is untouched, and the running server picks up the new certificate. CLI tests verify forced rotation regardless of remaining validity, fingerprint output, and BYOC refusal.

### Operators can provision machine tokens without browser access

**ID: REQ-022**

An operator with server access can issue a machine token through the server-side CLI for an instance that is managed through configuration or federation rather than its dashboard. The command requires an explicit token name, one or more supported permissions, and an expiry instant. It uses the instance's persistent application database, applies the same issuance rules as dashboard token creation, and prints the token value only once after successful creation.

**Rationale:** Headless and federation-target instances need a way to provision their own least-privilege integration credentials without enabling routine browser administration.

**Constraints:** The token must follow REQ-015 and SEC-008, including mandatory expiry within 366 days, supported explicit permissions, verifier-only persistence, and one-time token disclosure. The command does not start HTTP serving or deployment synchronization. It must not print the token in application logs or accept the token value as input.

**Dependencies:** REQ-015, SEC-008, SEC-014, UX-006, QUA-005

**Verification expectation:** CLI tests verify required arguments, permission and expiry validation, token persistence through the shared issuance service, one-time token output, and that CLI mode exits without starting HTTP serving or deployment synchronization.

### Deployment lifecycle control must cover start, stop, and state visibility

**ID: REQ-002**

The system must expose a clear lifecycle for managed deployments, including starting, stopping, and reporting current deployment state and messages to operators.

**Rationale:** A deployment manager must provide enough operational transparency for users to understand what is active, what failed, and what action is pending.

**Constraints:** The state vocabulary is `UNKNOWN`, `RUNNING`, `STOPPED`, `FAILED`, `UPDATED`, `REMOVED`, `STARTING`, and `STOPPING`. A configured deployment without a persisted state is presented as `UNKNOWN`. Start failures returned as non-zero Compose results set `FAILED` with stderr; start exceptions set `FAILED` with the exception message. `UPDATED` is published when the Git checkout changes, before the start attempt. A stop publishes `STOPPED` after the Compose command returns without checking its exit code. Removed deployments are marked `REMOVED` after cleanup completes. State messages may therefore describe the latest event rather than independently verified runtime health.

**Dependencies:** ADR-003, UX-001

**Verification expectation:** Acceptance testing confirms the UI and API expose the deployment state and allow explicit lifecycle actions.

### Secret handling must protect environment-sensitive values

**ID: REQ-003**

Sensitive values used by deployments, including secret-bearing environment variables, must be stored and managed in a way that avoids disclosure in plain view while supporting runtime injection into deployment containers.

**Rationale:** Infrastructure automation is exposed to credential and configuration risk; secret handling is central to safe self-hosted operation.

**Constraints:** Secrets are persisted as encrypted values using a configured 16-character key and the platform's default transformation for `AES`; the implementation does not configure an explicit mode, nonce, or authentication tag. On update, a missing or JSON-null value is passed as null and leaves the previously encrypted value unchanged; an empty string is encrypted as a value. A secret is identified by its path/key string. Deployment associations are stored as names and are not checked against the current deployment configuration. Deletion removes the secret and associations. Setting `restartDeployments=true` requests starts for associated deployments. Values are decrypted when preparing the deployment process environment. The current implementation does not provide key rotation or re-encryption migration.

**Dependencies:** SEC-001, SEC-002

**Verification expectation:** Security review confirms secret values are protected at rest and only provided to the runtime process when needed.

### Repository events can trigger the corresponding deployment

**ID: REQ-004**

When an operator configures a repository webhook, a change event for a managed deployment repository can trigger the corresponding deployment to synchronize and start. A change to the repository that defines the deployment list can trigger reconciliation of the managed deployment set.

**Rationale:** Repository-driven triggers reduce manual deployment steps while preserving version-controlled configuration as the source of changes.

**Constraints:** Webhook use is optional. The receiver accepts a JSON body containing `ref` and a repository object with `clone_url`; it requires a machine bearer token granted the dedicated `webhooks:trigger` permission before processing the body. A clone URL equal to the configured deployment-list repository URL requests full reconciliation. Other clone URLs publish a source/ref start request; the orchestration service acts only on configured source/ref matches. HTTPS and network restriction remain defense in depth. Payload HMAC signatures are not required.

**Dependencies:** REQ-001, ADR-001, ADR-004, SEC-004

**Verification expectation:** Tests verify rejection of anonymous and wrong-scope callers, acceptance of a `webhooks:trigger` token, deployment-list URL matching, source/ref matching, arbitrary event headers, repeated requests, malformed bodies, and empty acknowledgment responses.

**Additional constraints:** The receiver does not distinguish push events from other request types. Repeated requests publish repeated events; there is no explicit deduplication or idempotency key.

### Startup and scheduled synchronization are operator-configurable

**ID: REQ-005**

Operators can configure whether managed deployments are synchronized at application startup and on a recurring schedule.

**Rationale:** Self-hosted operators need a way to reconcile deployment state after restart and at a cadence appropriate to their environment.

**Constraints:** Startup synchronization is controlled by `dirigent.start.all.on.startup` (default true in the packaged properties). Recurring synchronization is controlled by `dirigent.deployments.schedule.enabled` and `dirigent.deployments.schedule.cron` (defaults true and every five minutes in packaged properties). These settings are separate from `startAllOnStartup` in the parsed YAML configuration, which is not the property used by the scheduler.

**Dependencies:** ADR-001, ADR-004, QUA-001

**Verification expectation:** Operators can enable or disable each trigger and observe reconciliation when it is enabled.

### Image updates are discovered and applied as separate operator actions

**ID: REQ-006**

The system checks images used by running Compose services for available updates, presents discovered updates to the operator, and applies an update only after an explicit operator action for the selected service.

**Rationale:** Operators need visibility into newer images while retaining control over when running services are changed.

**Constraints:** Discovery scans Compose-labeled containers including stopped containers, attempts to resolve an image reference, obtains a registry manifest digest, and compares that string with the local Docker image ID; no explicit platform selection is performed. Update application runs Compose `up --pull always --force-recreate` for the selected service. The command's returned exit code is not checked by the update service. Update rows are removed after either success or caught failure; caught failures are logged and an update-failed event is published for optional notification. There is no durable API result for completed update application, and failed updates are not shown in the update list after cleanup. Registry login failures are logged as warnings and processing continues; registry digest lookup failures are logged and skipped.

**Dependencies:** REQ-001, REQ-002, SEC-002, UX-004, QUA-001

**Verification expectation:** Existing behavior can be checked by exercising container listing, registry lookup, row creation, selected-service Compose invocation, and update-row cleanup. Current tests primarily cover image-reference parsing and registry client/authentication behavior; they do not establish the full discovery/apply contract or operator-visible failure outcomes.

### Removed deployments are reconciled only after stop succeeds

**ID: REQ-007**

When a deployment is removed from the configuration, reconciliation invokes Compose `down`, then recursively deletes its managed checkout and publishes `REMOVED` if the process invocation returns normally.

**Rationale:** Reconciliation removes checkouts for deployments no longer present in configuration.

**Constraints:** The Compose process exit code is not checked before deleting the checkout. A thrown I/O/interruption error aborts the removal path; a non-zero exit result does not.

**Dependencies:** REQ-001, REQ-002, ADR-003, QUA-001

**Verification expectation:** Current behavior can be checked by verifying that command invocation precedes deletion and by comparing thrown process errors with non-zero process results; there is no dedicated removal-flow test coverage.

### Breaking releases must explain changes and upgrades

**ID: REQ-018**

For a release that changes an existing operator workflow or public integration contract incompatibly, release documentation must include a concise changelog entry identifying the breaking changes and a migration guide with ordered actions to upgrade and verify the installation.

**Rationale:** Operators and API consumers need to understand both what changed and what they must do to keep existing installations and integrations working.

**Constraints:** Upgrade guidance must distinguish existing installations from fresh setup and cover required account, configuration, API-client, and integration changes, along with any known data or rollback considerations. It must not imply compatibility or migration automation that the product does not provide.

**Dependencies:** REQ-004, REQ-013, REQ-014, REQ-015, SEC-005

**Verification expectation:** A breaking release has a changelog entry that explicitly marks incompatible behavior, and a linked migration guide provides a complete ordered checklist with a way to verify access and integrations after upgrade.

### The project roadmap distinguishes planned work from delivered capabilities

**ID: REQ-023**

The README roadmap must distinguish planned work from achieved capabilities and must not list delivered capabilities as though they were still outstanding. Where a roadmap goal is partially implemented, its wording should make the remaining gap clear. Achieved capabilities include operator authentication and scoped machine tokens (REQ-014, REQ-015), multi-host federation (REQ-019, REQ-020), native HTTPS (REQ-021), image-update discovery and operator-triggered application (REQ-006), configured Gotify notifications (UX-004), and the automated test/release gate (QUA-004).

The roadmap includes planned improvements to the certificate rotation experience and notifications. Their detailed behavioral scope remains to be specified before implementation, informed by SEC-016, UX-004, and QUA-003. Additional planned goals are to show more information about instances (including their version), make the affected instance clear in notifications, support additional notification destinations, and let operators disable unused features to reduce resource use. These goals are informed by UX-004, UX-009, and QUA-003; their detailed acceptance criteria and scope remain to be specified before implementation.

It also tracks reducing the initial frontend bundle size, retaining clear outcomes for image update operations, providing backup and restore support, improving secret encryption and key rotation, and validating deployment configuration with safe recovery from invalid changes. These goals are informed by CLR-013, REQ-011, ADR-005, SEC-001, REQ-001, and CLR-004; detailed acceptance criteria remain to be specified before implementation.

**Rationale:** Contributors and operators use the roadmap to understand project direction and should not be misled about existing capabilities or work that remains.

**Dependencies:** QUA-006, SEC-016, UX-004, UX-009, QUA-003, CLR-013, REQ-011, ADR-005, SEC-001, REQ-001, CLR-004

**Verification expectation:** Roadmap entries are checked against the current supported behavior and relevant requirements before publication.
