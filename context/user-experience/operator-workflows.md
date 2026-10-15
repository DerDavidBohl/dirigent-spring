# Operator workflows

### Operators must have a clear deployment view

**ID: UX-001**

The deployments page lists configured deployments with source, state, and message. Operators can search by deployment name, filter by state, sort columns, and open source links. Search, filter, and sort are kept in the URL so views are linkable and survive reload. On narrow viewports the table becomes stacked cards.

**Rationale:** Self-hosted operational tools depend on readable and actionable views so administrators can quickly assess and act on infrastructure state.

**Constraints:** The interface must remain focused on operational clarity and not become a general-purpose configuration editor.

**Dependencies:** REQ-002, ADR-002

**Verification expectation:** Tests confirm the list fields and search/filter/sort controls. The page polls deployment data every two seconds, pauses while the browser tab is hidden, and shows explicit loading, error, and last-refreshed states.

### Operators must be able to trigger lifecycle actions explicitly

**ID: UX-002**

The deployments page provides Start and Stop actions. Start opens a dialog where the operator confirms and may request force recreation. Stop opens a confirmation dialog and invokes the API only after the operator confirms. Deployment state is refreshed by periodic polling rather than a visible manual refresh action. The dashboard offers no lifecycle actions.

**Rationale:** Infrastructure workflows depend on the operator making intentional decisions rather than implicit side effects.

**Constraints:** The start dialog exposes the force-recreate choice. Both dialogs can be cancelled with a cancel button or the Escape key without sending a request.

**Dependencies:** REQ-002, ADR-003

**Verification expectation:** Tests verify the start dialog, the stop confirmation, and that cancelling sends no request.

### Operators can manage deployment secrets without seeing stored values

**ID: UX-003**

Operators can create, update, associate, and remove deployment secrets. The interface identifies each secret and its deployment/environment-variable associations without displaying the stored secret value; entering a replacement value is an explicit action.

**Rationale:** Operators need to manage runtime configuration while reducing accidental disclosure during routine review.

**Constraints:** Secret list responses do not include stored values. The editor displays a password field labelled “Value” with a hint that a blank value keeps the stored value. Restarting associated deployments is an optional checkbox. Deletion requires a second confirmation click in the same dialog.

**Dependencies:** REQ-003, SEC-001, QUA-002

**Verification expectation:** Tests confirm list values are hidden, a password field is used, the retain-versus-replace hint is shown, and the restart/delete controls behave as described.

### Operators receive deployment and image-update notifications when configured

**ID: UX-004**

When a Gotify base URL and token are configured, notices are sent for deployment state changes, discovered image updates, successful image updates, and failed image updates. Without both values, sending is a no-op.

**Rationale:** Notifications let operators learn about operational changes without continuously watching the dashboard.

**Constraints:** The implementation has one Gotify destination configured by base URL and token. The four event categories are fixed in the service; operators cannot select destinations or event types. Retry behavior is not explicitly configured or guaranteed.

**Dependencies:** REQ-002, QUA-001

**Verification expectation:** Current notification behavior can be reviewed through the configured Gotify request. No notification integration test verifies delivery, filtering, or retries.

### Dashboard states and consequential actions are accessible and explicit

**ID: UX-005**

The dashboard, deployments, and updates pages poll every two seconds and show explicit loading, error, and last-refreshed states. Status changes are announced to assistive technology through live regions, and state is never conveyed by colour alone. The “Check for updates” label changes while the check request is pending; that request acknowledges asynchronous work rather than its completion. Update rows expose an `isRunning` flag to disable the row action while an update is active.

**Rationale:** Clear system feedback and reversible decision points help operators distinguish current state from stale or incomplete information.

**Constraints:** Start/force recreation and Stop use dialogs, secret deletion uses a second click, and secret-triggered restart is a checkbox. The target is WCAG 2.2 AA: visible focus, a skip link, labelled controls, and sufficient contrast in both themes. No formal accessibility audit is recorded.

**Dependencies:** UX-001, UX-002, UX-003, UX-007, UX-008, REQ-002, REQ-006

**Verification expectation:** Tests cover the loading, error, and populated states and the dialog flows; manual review checks keyboard use and contrast in both themes.

### Operators authenticate and manage integration access explicitly

**ID: UX-006**

Operators can sign in with a local account or the configured OIDC provider without a separate per-user provisioning step. The signed-in header should show a human-readable provider username when one is supplied, falling back through display name and email before using the provider subject. If an optional OIDC group restriction is configured, only members of that provider group can sign in. There is no public local-account registration. Operators manage local account recovery through the server-side CLI and machine tokens through the dashboard using tables and dialog workflows consistent with the rest of the application. The token expiry is chosen with a calendar date input; the token expires at 00:00 UTC on that date, which the form states, and the token table shows expiry dates in UTC. A newly created machine token is presented prominently upon creation so the operator can copy it before dismissal, while the table immediately reflects the new token metadata. Operators can revoke tokens and end their browser session. Signing out ends the browser session and returns to the login view; failures are communicated without falsely implying the session ended.

**Rationale:** Authentication must fit both standalone self-hosting and existing identity-provider setups while making privileged access and one-time credentials understandable.

**Constraints:** All human accounts are administrators initially. Local account creation, password reset, and deletion are operator-controlled through server-side CLI operations; the final local administrator cannot be deleted. OIDC authorization follows the configured issuer and optional group restriction without per-user CLI provisioning. Gitea is configured with a machine token carrying the dedicated `webhooks:trigger` permission; that token is separate from interactive login and should not carry unrelated API permissions.

**Dependencies:** REQ-014, REQ-015, REQ-016, REQ-017, SEC-005, SEC-007, SEC-008, ADR-007

**Verification expectation:** Manual or automated checks cover local and OIDC login without per-user provisioning, readable OIDC display-name selection, optional group acceptance/rejection, successful and failed logout behavior, local account recovery, token creation with a calendar-date expiry sent as 00:00 UTC of the chosen date, revocation, and one-time token presentation without exposing credentials in list views. The login form remains aligned and usable on desktop and narrow viewports.

### The start page is a read-only operational dashboard

**ID: UX-007**

After sign-in, operators land on a dashboard at `/`. It shows, from top to bottom: a tile for each deployment state that currently has one or more deployments, each showing its count and linking to the deployments page filtered by that state; a "Needs attention" panel listing failed deployments with their latest messages; a "Pending updates" panel listing available image updates with a "Check for updates" button; and the instance name and Git source, with the Git source presented as an external link. The dashboard offers no start, stop, or other lifecycle actions. Each panel shows its own loading, error, and last-refreshed state.

**Rationale:** Operators need an at-a-glance health overview before drilling into a specific deployment, without risking accidental actions.

**Constraints:** The dashboard uses only existing API data. The update check acknowledges asynchronous work (UX-005). Navigation between the dashboard, deployments, updates, secrets, and tokens is a persistent sidebar on desktop that collapses to a top bar with a drawer on narrow viewports; the header holds the signed-in identity, the theme toggle, and sign out.

**Dependencies:** UX-001, UX-005, UX-006, REQ-002, ADR-002, COD-009

**Verification expectation:** Tests confirm only nonzero state counts are shown, their filter links are correct, the Git source is linked, and the attention list, pending updates, absence of lifecycle actions, and per-panel loading and error states are correct.

### The interface follows the logo palette and supports light and dark themes

**ID: UX-008**

The visual design is dense and operational and uses the logo colours: blue (`#4A90E2`, `#357ABD`) as the primary accent, cyan (`#00BFFF`) as the secondary accent, and orange-red (`#F1502F`) for emphasis and failure. A light and a dark theme are provided. The initial theme follows the system preference, and a header toggle switches it and remembers the choice. Deployment states are shown as badges combining colour, icon, and text, in four tones: running, transitional (starting, stopping, updated), stopped (stopped, removed, unknown), and failed.

**Rationale:** A consistent, accessible visual language lets operators read state quickly in any lighting and ties the interface to the product identity.

**Constraints:** Contrast meets WCAG 2.2 AA in both themes (UX-005). Colour is never the only state signal.

**Dependencies:** UX-005, REQ-002, COD-005

**Verification expectation:** Tests confirm theme initialisation, toggling, and persistence; manual review confirms contrast in both themes.
