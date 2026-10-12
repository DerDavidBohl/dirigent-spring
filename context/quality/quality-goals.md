# Quality goals

### Deployment orchestration must be reliable enough for operator trust

**ID: QUA-001**

The system should behave predictably when handling deployment lifecycle actions, registry checks, and configuration refreshes so operators can rely on the dashboard and backend as the source of operational truth.

**Rationale:** Deployment automation depends on correctness and predictable handling; unclear or flaky behavior undermines self-hosted operations.

**Constraints:** Reliability is constrained by Docker, host configuration, repositories, and registries. Registry login failures are caught and logged as warnings, and registry digest lookup failures are logged and skipped. A nonzero Compose `up` result marks a deployment failed; a nonzero Compose `down` result is not checked and is followed by a `STOPPED` state. Update application does not inspect the returned Compose exit code before publishing its success event and cleaning up the update row.

**Dependencies:** REQ-001, REQ-002, ADR-001

**Verification expectation:** Existing tests cover registry authentication/properties, registry-client behavior, image-reference parsing, and a basic process invocation. They do not cover the full deployment lifecycle, stop nonzero outcomes, configuration fallback, or update-apply exit-code handling.

### Secret and registry handling must minimize accidental exposure

**ID: QUA-002**

Secret-list API responses omit stored values, and registry login supplies the password through stdin. Other paths currently expose sensitive data risks: process commands and output are logged, authenticated Git URLs may appear in command logs, and invalid secret-key configuration is included in an exception message.

**Rationale:** Secret exposure would undermine the trust model for a deployment orchestration tool and compromise the infrastructure it manages.

**Constraints:** Complete prevention depends on secure host practices and runtime configuration; the project can only define the intended handling boundaries.

**Dependencies:** REQ-003, SEC-001, SEC-002

**Verification expectation:** Secret-list responses currently return null values. No tests verify end-to-end diagnostic redaction or cryptographic properties.

### Optional integrations must not become hidden prerequisites

**ID: QUA-003**

Failure or absence of a Gotify destination does not prevent deployment lifecycle operations. Notification send attempts are guarded by a try/catch and failures are logged. A private `@Retryable` method with no explicit retry parameters is invoked internally; effective retry behavior is not verified. Destination and event type are fixed rather than operator-selectable.

**Rationale:** Operators must be able to distinguish a failed deployment from an unavailable auxiliary notification service.

**Constraints:** Quantitative availability, latency, and recovery targets are deferred until supported by operational measurements and a defined workload; see the [clarification register](../clarifications.md) for the revisit condition.

**Dependencies:** REQ-002, UX-004, ADR-001

**Verification expectation:** Code inspection shows notification exceptions are caught and logged. There are no notification tests establishing retry count/backoff or configurable filtering.

### Release evidence covers each supported operator workflow

**ID: QUA-004**

The backend suite covers process invocation, registry authentication/properties/client behavior, image-reference parsing, update parsing, local-account and machine-token services, authorization-filter/method-security behavior, webhook-signature verification, and a minimal update-service scenario. Dedicated tests are not present for secret encryption/lifecycle, deployment configuration validation, the Gitea webhook controller contract, deployment start/stop/reconciliation, general API controller contracts, or frontend components. The frontend defines a test script, but no frontend component tests are present in the reviewed source.

**Rationale:** Independent reconstruction and change review require evidence that behavior works across component boundaries, not only that individual utilities compile.

**Constraints:** There is no documented release gate requiring a particular test/build/security sequence. Backend test execution includes environment-dependent Docker and Spring-context tests; frontend production build is available. No numerical availability, latency, or recovery promise is defined.

**Dependencies:** QUA-001, QUA-002, QUA-003, COD-002, SEC-006

**Verification expectation:** Current evidence is the test suite and build commands run by maintainers; no automated enforcement of this release evidence is specified here.

### Authentication and authorization behavior is covered by focused tests

**ID: QUA-005**

Authentication changes must include focused backend tests for local login, OIDC issuer authentication with and without the optional required group, anonymous access denial, administrator authorization, login rate limiting, API-token permission/mandatory expiry/revocation, webhook `webhooks:trigger` authorization, and local-account CLI lifecycle operations. The Angular production build and tests for login/token interaction must pass for frontend changes.

**Rationale:** Authentication is a security boundary around privileged deployment and secret operations; compilation alone cannot demonstrate correct access policy.

**Constraints:** External OIDC tests must use a deterministic local test provider or mocked protocol responses and must not depend on a live identity service. Test credentials are non-production and must not be committed as reusable secrets.

**Dependencies:** REQ-014, REQ-015, REQ-016, REQ-017, SEC-004, SEC-005, SEC-007, SEC-008, SEC-009, SEC-010

**Verification expectation:** The focused backend and frontend checks cover both allowed and denied cases; failures must not be accepted as a passing authentication verification.
