# Operator workflows

### Operators must have a clear deployment view

**ID: UX-001**

The dashboard lists configured deployments with source, order, state, and message. Operators can search by deployment name, filter by state, sort table columns, and open source links.

**Rationale:** Self-hosted operational tools depend on readable and actionable views so administrators can quickly assess and act on infrastructure state.

**Constraints:** The interface must remain focused on operational clarity and not become a general-purpose configuration editor.

**Dependencies:** REQ-002, ADR-002

**Verification expectation:** Manual review confirms the deployment list fields and search/filter/sort controls are present. The page polls deployment data every two seconds; it does not display a last-refresh timestamp or explicit stale/error state.

### Operators must be able to trigger lifecycle actions explicitly

**ID: UX-002**

The deployments table provides Start and Stop actions. Start opens a dialog where the operator confirms and may request force recreation. Stop invokes the API directly without a confirmation dialog. Deployment state is refreshed by periodic polling rather than a visible manual refresh action.

**Rationale:** Infrastructure workflows depend on the operator making intentional decisions rather than implicit side effects.

**Constraints:** The start dialog exposes the force-recreate choice. Stop is immediate from the menu; no confirmation or cancel step is presented before the request.

**Dependencies:** REQ-002, ADR-003

**Verification expectation:** Manual review or tests can verify the start dialog and direct stop action. There are currently no frontend component tests for these interactions.

### Operators can manage deployment secrets without seeing stored values

**ID: UX-003**

Operators can create, update, associate, and remove deployment secrets. The interface identifies each secret and its deployment/environment-variable associations without displaying the stored secret value; entering a replacement value is an explicit action.

**Rationale:** Operators need to manage runtime configuration while reducing accidental disclosure during routine review.

**Constraints:** Secret list responses do not include stored values. The editor displays a password field labelled “Value”, but does not explain that a blank/missing value preserves an existing value. Restarting associated deployments is an optional checkbox. Deletion requires a second confirmation click in the same dialog.

**Dependencies:** REQ-003, SEC-001, QUA-002

**Verification expectation:** Manual review confirms list values are hidden, a password field is used, and the restart/delete controls behave as described. The current UI does not clearly communicate retain-versus-replace semantics; there are no component tests for secret editing.

### Operators receive deployment and image-update notifications when configured

**ID: UX-004**

When a Gotify base URL and token are configured, notices are sent for deployment state changes, discovered image updates, successful image updates, and failed image updates. Without both values, sending is a no-op.

**Rationale:** Notifications let operators learn about operational changes without continuously watching the dashboard.

**Constraints:** The implementation has one Gotify destination configured by base URL and token. The four event categories are fixed in the service; operators cannot select destinations or event types. Retry behavior is not explicitly configured or guaranteed.

**Dependencies:** REQ-002, QUA-001

**Verification expectation:** Current notification behavior can be reviewed through the configured Gotify request. No notification integration test verifies delivery, filtering, or retries.

### Dashboard states and consequential actions are accessible and explicit

**ID: UX-005**

The deployment dashboard shows state and message and polls every two seconds. The updates page polls update rows every two seconds and shows a spinner while its list request is in progress. The “Check for updates” label changes while the check request is pending; that request acknowledges asynchronous work rather than its completion. Update rows expose an `isRunning` flag to disable the row action while an update is active.

**Rationale:** Clear system feedback and reversible decision points help operators distinguish current state from stale or incomplete information.

**Constraints:** Start/force recreation uses a dialog, Stop has no confirmation, secret deletion uses a second click, and secret-triggered restart is a checkbox. The deployment view has no visible loading/stale/error state or last-refresh timestamp. The current codebase has no demonstrated WCAG 2.2 AA conformance; no formal accessibility audit is recorded.

**Dependencies:** UX-001, UX-002, UX-003, REQ-002, REQ-006

**Verification expectation:** Manual inspection describes the visible states and action confirmations. Automated frontend workflow/accessibility tests are not currently present.
