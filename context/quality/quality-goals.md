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

The backend suite covers process invocation, registry authentication/properties/client behavior, image-reference parsing, update parsing, local-account and machine-token services, authorization-filter/method-security behavior, webhook-signature verification, and a minimal update-service scenario. Dedicated tests are not present for secret encryption/lifecycle, deployment configuration validation, the Gitea webhook controller contract, deployment start/stop/reconciliation, or general API controller contracts. Frontend services, guard, routing, theme handling, and pages are covered by test-first Vitest tests (COD-010).

**Rationale:** Independent reconstruction and change review require evidence that behavior works across component boundaries, not only that individual utilities compile.

**Constraints:** Backend tests in the required `make test` suite must not depend on public registries or a locally running Docker daemon; integration tests that need external services must manage their own fixtures or remain outside the default gate. Spring-context tests should use only the application configuration needed for the behavior under test. Frontend production build is available. Pull requests and image builds must pass the repository's `make test` suite for the exact ref being built. The manual release workflow must pass the same suite before changing versions or creating a release tag, and image publication must wait for the applicable test gate. No numerical availability, latency, or recovery promise is defined.

**Dependencies:** QUA-001, QUA-002, QUA-003, COD-002, SEC-006

**Verification expectation:** Pull-request and image-build workflows run `make test` against the same resolved ref that is built; the image job depends on successful tests. The manual publish job runs `make test` before committing version changes or creating a tag. The default suite passes without public registry access or a local Docker daemon. Existing test-coverage gaps listed above remain explicit limitations.

### Authentication and authorization behavior is covered by focused tests

**ID: QUA-005**

Authentication changes must include focused backend tests for local login, OIDC issuer authentication with and without the optional required group, OIDC administrator access to token management, anonymous access denial, administrator authorization, login rate limiting, API-token permission/mandatory expiry/revocation, webhook `webhooks:trigger` authorization, and local-account CLI lifecycle operations. Frontend verification covers readable identity display, logout success/failure states, responsive login presentation, token creation dialog and immediate token table presentation, and a successful Angular production build.

**Rationale:** Authentication is a security boundary around privileged deployment and secret operations; compilation alone cannot demonstrate correct access policy.

**Constraints:** External OIDC tests must use a deterministic local test provider or mocked protocol responses and must not depend on a live identity service. Test credentials are non-production and must not be committed as reusable secrets.

**Dependencies:** REQ-014, REQ-015, REQ-016, REQ-017, SEC-004, SEC-005, SEC-007, SEC-008, SEC-009, SEC-010

**Verification expectation:** The focused backend and frontend checks cover both allowed and denied cases; failures must not be accepted as a passing authentication verification.

### Operator documentation matches supported runtime behavior

**ID: QUA-006**

README setup, configuration, security, and usage guidance must describe capabilities and defaults that match the supported implementation. Changes affecting those areas require a documentation consistency review as part of verification.

**Rationale:** Operators rely on the README to configure and secure a self-hosted deployment; stale guidance can undermine otherwise correct implementation and security controls.

**Constraints:** Documentation review complements executable tests and does not replace behavior tests for security-sensitive changes. The README should not promise behavior that depends on unverified deployment-specific configuration.

**Dependencies:** COD-006, COD-004, ADR-001, ADR-002, SEC-005

**Verification expectation:** Review confirms documented environment variables, defaults, CLI commands, API authentication, and deployment examples correspond to the implementation; test commands or example snippets syntactically where practical.

### A manual test environment runs the current code with authentication

**ID: QUA-007**

**Status:** Proposed.

**Basis:** Maintainer request for a professional, repeatable manual test setup that runs the current code locally; QUA-004; QUA-005.

A maintainer can start the backend and the web dashboard from the working tree with one documented command and sign in with a non-production test account, without preparing a database, encryption key, or operator account by hand. The environment exercises the same authentication and authorization behavior as production (REQ-014, SEC-004), is clearly distinguishable from a production deployment, and never touches real deployments, operator data, or production configuration.

**Rationale:** Manual testing of authentication-protected workflows is otherwise slow and error-prone, and unrepeatable setups weaken release evidence.

**Constraints:** The environment runs as host processes of the working tree and uses the toolchain and Docker/Compose the project already requires. It reports a clear message when a prerequisite is missing. Test credentials follow SEC-012.

**Dependencies:** QUA-004, QUA-005, QUA-006, SEC-012, REQ-014, COD-007

**Verification expectation:** From a clean checkout, the documented start command yields a reachable dashboard where the test account can sign in and anonymous API access is denied.

### Manual test scenarios and data reset are repeatable

**ID: QUA-008**

**Status:** Proposed.

**Basis:** Maintainer request for quick test scenarios and data wiping; QUA-001; QUA-004.

The manual test environment provides named, self-contained scenarios (for example a deployments configuration with several sample deployments) that need no external Git hosting or credentials, and loads one with a single documented command. The environment always has a scenario available: starting it without a loaded scenario loads the default one. A single documented command resets the environment to an empty state, removing all state the environment created (database, secrets, tokens, cloned configuration and deployments, scenario data) and leaving committed files and anything outside the environment untouched. Resetting and reloading a scenario always yields the same starting state.

**Rationale:** A deterministic starting state is needed to reproduce defects and to judge lifecycle, secret, token, and webhook behavior.

**Constraints:** Reset affects only the test environment's own state and refuses to run while the environment is running. Scenarios contain no real credentials or secrets.

**Dependencies:** QUA-007, QUA-001, SEC-012, COD-007

**Verification expectation:** After reset the dashboard shows no deployments, secrets, or tokens and the test account is recreated on next start; after loading a scenario its documented deployments appear.
