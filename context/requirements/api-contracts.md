# Public API contracts

These records define the current versioned JSON API resource surface used by the operator dashboard and external callers. Command operations return an empty success response when the request is accepted; execution outcomes are reported through deployment/update state. Authentication and network trust are specified in the security context.

### Deployment resource and lifecycle commands

**ID: REQ-008**

`GET /api/v1/deployments` returns a JSON array of deployment objects with `name`, `source`, `order`, `state`, and `message`. A deployment without an observed state is represented as `UNKNOWN` with an empty message. `POST /api/v1/deployments/{name}/start` accepts the optional boolean query parameter `forceRecreate`, defaulting to false. `POST /api/v1/deployments/{name}/stop` requests a stop. `POST /api/v1/deployments/all/start` requests all configured deployments to start and accepts the same optional `forceRecreate` parameter. Command responses have no body; their HTTP success acknowledges the request, not successful completion. Clients observe progress and outcomes by polling `GET /api/v1/deployments` and reading the corresponding state and message. The API does not issue operation IDs.

**Rationale:** Operators and integrations need a stable means to inspect configured deployments and explicitly request lifecycle actions.

**Dependencies:** REQ-001, REQ-002, UX-001, UX-002, ADR-001

**Verification expectation:** Contract tests verify methods, paths, query defaults, JSON fields, empty command responses, and deployment-list state defaults.

### Deployment-state resource

**ID: REQ-009**

`GET /api/v1/deployment-states` returns a JSON array of state objects with `name`, `state`, and nullable `message`. State values are `UNKNOWN`, `RUNNING`, `STOPPED`, `FAILED`, `UPDATED`, `REMOVED`, `STARTING`, and `STOPPING`, with meanings specified in REQ-002. An empty collection is returned when no deployment states have been recorded.

**Dependencies:** REQ-002, UX-001, ADR-001

**Verification expectation:** Contract tests verify the response fields, allowed enum values, nullable message, and empty-state response.

### Secret resource

**ID: REQ-010**

`GET /api/v1/secrets` returns a JSON array with `key`, `environmentVariable`, `value`, and `deployments`; list responses always set `value` to null. `PUT /api/v1/secrets/{key}` accepts those fields as a JSON object and an optional `restartDeployments` boolean query parameter, default false. The path key identifies the secret and takes precedence over a different `key` field in the body. `DELETE /api/v1/secrets/{key}` accepts the same optional query parameter. PUT and DELETE return empty success responses. Deleting a nonexistent key is idempotent and returns the same success response as deleting an existing key. Secret value update semantics and association policy are specified in REQ-003.

**Dependencies:** REQ-003, SEC-001, SEC-005, UX-003

**Verification expectation:** Contract tests verify redacted list values, request field shape, defaults, idempotent deletion, and that no response reveals stored secret values.

### Image-update resource and commands

**ID: REQ-011**

`GET /api/v1/deployment-updates` returns a JSON array of update objects with `deploymentName`, `service`, `image`, and `isRunning`. `POST /api/v1/deployment-updates/run` accepts one update object with those fields and requests applying the selected service update. `POST /api/v1/deployment-updates/check` requests an update scan and has no request body. Command responses have no body; their HTTP success acknowledges the request, not completion. Clients poll `GET /api/v1/deployment-updates`; `isRunning` indicates that an update is in progress. No operation ID or durable update-operation history is exposed, and clients cannot reliably distinguish a completed success from a completed failure through this resource. Apply failures are reported through logs and, when configured, notifications. Image discovery/application behavior is specified in REQ-006.

**Dependencies:** REQ-006, UX-004, UX-005, ADR-001

**Verification expectation:** Contract tests verify response and request fields, paths, methods, no-body check command, empty acknowledgment responses, update progress polling, and that update-apply failures do not create a durable API result.

### System-information resource

**ID: REQ-012**

`GET /api/v1/system-information` returns a JSON object with `instanceName` and `gitUrl`, both represented as strings. The configured instance name defaults to `Unknown Instance`; an unavailable deployment-list Git URL is represented as an empty string.

**Dependencies:** REQ-001, UX-001, ADR-001

**Verification expectation:** Contract tests verify response field names, string types, and defaults when values are not configured.

### Gitea repository-event endpoint

**ID: REQ-013**

`POST /api/v1/gitea` accepts a JSON body with a `ref` string and a `repository` object containing `clone_url` and optional `ssh_url`. It requires an opaque machine bearer token with the dedicated `webhooks:trigger` permission before processing the request. If `clone_url` exactly matches the configured deployment-list Git URL, it requests full reconciliation; otherwise it publishes a source/ref start request, which only acts when a configured deployment matches. The endpoint has no response body on acknowledgment; clients observe deployment state through `GET /api/v1/deployments`. Each accepted request publishes a new event; no deduplication is performed. Network trust is described in SEC-004.

**Constraints:** The token is sent in `Authorization: Bearer <token>` and must be issued with only the dedicated `webhooks:trigger` permission where possible. No Gitea HMAC signature or shared webhook secret is required. Token expiry and revocation follow REQ-015.

**Dependencies:** REQ-004, ADR-004, SEC-004

**Verification expectation:** Contract tests should cover anonymous and wrong-scope denial, valid webhook-token acceptance, deployment-list URL matching, source/ref matching, arbitrary event headers, repeated requests, malformed bodies, and empty acknowledgment responses.

### Browser authentication and administrator accounts

**ID: REQ-014**

Dirigent supports browser authentication through either a configured OpenID Connect provider or a locally managed username and password. There is no public self-registration. Every identity successfully authenticated by the configured OIDC issuer receives administrator access without per-user CLI provisioning. Operators may optionally configure a required provider group and group-claim name; when configured, only authenticated identities whose claim contains that group receive access. Authenticated human accounts have administrator access. Browser sessions must be revocable by logging out and protected against cross-site request forgery for state-changing requests. When no local administrator has been provisioned, operators must be able to create one through the server-side CLI without supplying the password as a command-line argument.

**Rationale:** Dirigent controls privileged host deployments and secrets, so operator access must be explicit and manageable even when an external identity provider is unavailable.

**Constraints:** OIDC configuration is optional because local accounts provide standalone operation. OIDC is enabled explicitly and its client ID, client secret, issuer URI, optional scopes, optional required group, and group-claim name are supplied through Dirigent-prefixed configuration values. The issuer must use HTTPS. If no required group is configured, all successfully authenticated identities from the configured issuer are accepted. Local account creation is not available through public registration. Authentication failures must not reveal whether a local username exists.

**Dependencies:** SEC-005, SEC-007, ADR-007, UX-006

**Verification expectation:** Tests verify local login, configured OIDC login without per-user provisioning, optional group acceptance and rejection, logout/session invalidation, unauthenticated access denial, and CLI local-account creation with interactive secret input.

### Service API tokens

**ID: REQ-015**

An administrator can issue, list, and revoke machine API tokens. A token is displayed only once when created, must expire within 366 days, and is granted explicitly selected permissions. Token listings expose metadata and permissions but never the token value. Tokens authenticate API clients without creating browser sessions.

**Rationale:** Automation needs non-interactive access without sharing a human password or an administrator browser session.

**Constraints:** Tokens are revocable and scoped to supported API operations, including the dedicated `webhooks:trigger` permission. Every token must have an expiry no more than 366 days after creation; non-expiring tokens are not issued. Tokens do not authenticate interactive browser sessions. Webhook callers should not receive unrelated API permissions.

**Dependencies:** SEC-008, ADR-007, UX-006

**Verification expectation:** Tests verify one-time token disclosure, stored-token non-disclosure, permission enforcement, mandatory bounded expiry, expiration, and revocation.

### Server-side account provisioning command

**ID: REQ-016**

An operator with server access can invoke a CLI command to create a local administrator account. The command accepts the account name and obtains its password through a protected interactive prompt, not a command-line argument. It fails clearly if the account already exists and does not start the web server for this operation.

**Rationale:** Operators need a secure bootstrap and recovery path that does not depend on an already-authenticated dashboard or an available OIDC provider.

**Constraints:** The command requires access to the same persistent application database used by the running service. It must not create a default credential or print the entered password.

**Dependencies:** REQ-014, SEC-007, ADR-007

**Verification expectation:** CLI tests verify successful creation, duplicate rejection, password prompt handling, and that CLI mode exits without starting the HTTP server.

### Local administrator password reset and account deletion

**ID: REQ-017**

An operator with server access can reset a local administrator password through a CLI command that prompts for and confirms the new password without echoing or accepting it as a command-line argument. The CLI can delete a local account only after explicit interactive confirmation. Password reset and deletion apply only to local accounts, never OIDC-only identities. Deletion must not remove the final enabled local administrator account. These operations use the persistent application database and do not start the web server.

**Rationale:** Operators need a controlled recovery and deprovisioning path independent of browser login and external identity-provider availability.

**Constraints:** Passwords must meet the local password policy and are stored only as a password hash. Confirmation input must not be logged. OIDC identities are managed through their provisioning records rather than local password reset.

**Dependencies:** REQ-014, SEC-007, ADR-007

**Verification expectation:** Tests verify reset changes the password hash and enables login only with the replacement password, OIDC-only accounts cannot be reset/deleted through these commands, deletion requires confirmation, the last local administrator cannot be deleted, and CLI operations do not start HTTP serving.

### Operators can inspect configured Dirigent instances centrally

**ID: REQ-019**

When federation is configured, the dashboard presents the local instance and each configured remote instance with its instance name, connection status, and operational resources supported by the dashboard. Federation configuration is supplied at runtime and is not managed through the public API. Remote accounts, OIDC configuration, and machine-token administration remain local to each instance.

**Rationale:** Operators need a consolidated view of deployment health across independently hosted Dirigent instances while each instance retains control of its own host.

**Constraints:** Federation is optional. Remote operations are limited to the existing deployment lifecycle, image update, and secret APIs. A failure for one remote is visible for that remote and does not hide local data or successful remote results. Existing per-instance API contracts remain unchanged.

**Dependencies:** ADR-009, REQ-008, REQ-012, SEC-014, UX-009

**Verification expectation:** Tests verify local and multiple remote resources, per-remote connection failures, and relay of only the documented operations to the selected instance.

### Federation exposes a fixed set of remote API operations

**ID: REQ-020**

The federation API exposes explicitly routed, instance-scoped operations corresponding to the existing frontend workflows: read deployments, start/stop one deployment or start all; read updates, check for updates, and apply a selected update; read secrets and create/update/delete a secret; and read system information. Operations are routed only to the configured instance ID and corresponding fixed remote API path and method. Unknown instances, paths, and operations are rejected. Remote command responses preserve the remote API's acknowledgment semantics.

**Rationale:** A closed route set supports the current operational dashboard without creating an arbitrary HTTP proxy or granting access to remote identity administration.

**Constraints:** Human callers must be authenticated as hub administrators. Remote credentials are selected per instance and the remote Dirigent independently enforces the permission required for each operation. Secret request bodies are forwarded only to the selected HTTPS remote and must not be logged or stored by the hub. New remote API operations require an explicit contract and security review before being added.

**Dependencies:** ADR-009, REQ-008, REQ-010, REQ-011, REQ-012, SEC-005, SEC-014, SEC-015

**Verification expectation:** Controller/client tests verify allowed methods and paths, rejection of unknown instance/resource/operation, correct per-instance token use, preserved response status/body where applicable, and absence of remote auth/token-management routes.