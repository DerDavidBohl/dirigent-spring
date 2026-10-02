# Operational requirements

### GitOps deployment management must be repository-driven

**ID: REQ-001**

Dirigent manages Docker Compose deployments from a repository-backed deployment configuration, allowing operators to define and update deployments in version control rather than by ad hoc host commands.

**Rationale:** The project’s purpose is to simplify deployment coordination for self-hosted infrastructure by changing infrastructure state through Git-driven configuration.

**Constraints:** Repository definitions are read from YAML with `deployments` entries containing `name`, `source`, `order`, and optional `ref`. The implementation does not validate required/non-empty fields or duplicate names. An omitted numeric `order` is zero; a null `ref` is passed as `HEAD`. Configuration read/parse failures are propagated; no last-known-good fallback is provided. Host-specific actions are still required for Docker runtime access and local secrets.

**Dependencies:** ADR-001, ADR-002

**Verification expectation:** The operator can configure deployment sources and observe deployment actions through the system without direct shell-based orchestration.

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

**Constraints:** Webhook use is optional. The receiver accepts a JSON body containing `ref` and a repository object with `clone_url`; it does not check the event-type header or an application signature. A clone URL equal to the configured deployment-list repository URL requests full reconciliation. Other clone URLs publish a source/ref start request; the orchestration service acts only on configured source/ref matches. Requests rely on externally managed network restriction as described by SEC-004.

**Dependencies:** REQ-001, ADR-001, ADR-004, SEC-004

**Verification expectation:** Current behavior can be checked by posting a compatible body whose clone URL matches the configured deployment-list repository or a configured deployment source/ref. The receiver does not validate event type, and there is no dedicated webhook controller test.

**Additional constraints:** The receiver does not distinguish push events from other request types. Repeated requests publish repeated events; there is no explicit deduplication or idempotency key.

### Startup and scheduled synchronization are operator-configurable

**ID: REQ-005**

Operators can configure whether managed deployments are synchronized at application startup and on a recurring schedule.

**Rationale:** Self-hosted operators need a way to reconcile deployment state after restart and at a cadence appropriate to their environment.

**Constraints:** Startup synchronization is controlled by `dirigent.start.all.on.startup` (default true in the packaged properties). Recurring synchronization is controlled by `dirigent.delpoyments.schedule.enabled` and `dirigent.delpoyments.schedule.cron` (both spelled `delpoyments`; defaults true and every five minutes in packaged properties). These settings are separate from `startAllOnStartup` in the parsed YAML configuration, which is not the property used by the scheduler.

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
