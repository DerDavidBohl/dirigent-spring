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

`POST /api/v1/gitea` accepts a JSON body with a `ref` string and a `repository` object containing `clone_url` and optional `ssh_url`. The endpoint does not inspect `X-Gitea-Event` or require a signature. If `clone_url` exactly matches the configured deployment-list Git URL, it requests full reconciliation; otherwise it publishes a source/ref start request, which only acts when a configured deployment matches. The endpoint has no response body on acknowledgment; clients observe deployment state through `GET /api/v1/deployments`. Each accepted request publishes a new event; no deduplication is performed. Network trust is described in SEC-004.

**Constraints:** The current payload contract does not require application-level signature fields.

**Dependencies:** REQ-004, ADR-004, SEC-004

**Verification expectation:** Contract tests should cover deployment-list URL matching, source/ref matching, arbitrary event headers, repeated requests, malformed bodies, and empty acknowledgment responses. Current tests do not cover this controller contract.