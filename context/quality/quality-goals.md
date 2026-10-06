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

The repository currently has six backend test classes covering process invocation, registry authentication/properties/client behavior, image-reference parsing, and a minimal update-service scenario. Dedicated tests are not present for secret encryption/lifecycle, deployment configuration validation, webhook handling, deployment start/stop/reconciliation, controller contracts, or frontend components. The frontend defines a test script, but no frontend component tests are present in the reviewed source.

**Rationale:** Independent reconstruction and change review require evidence that behavior works across component boundaries, not only that individual utilities compile.

**Additional evidence:** Auto-update selection (REQ-014) is covered by `DeploymentUpdateServiceAutoUpdateTest`.

**Constraints:** There is no documented release gate requiring a particular test/build/security sequence. Backend test execution includes environment-dependent Docker and Spring-context tests; frontend production build is available. No numerical availability, latency, or recovery promise is defined.

**Dependencies:** QUA-001, QUA-002, QUA-003, COD-002, SEC-006

**Verification expectation:** Current evidence is the test suite and build commands run by maintainers; no automated enforcement of this release evidence is specified here.
