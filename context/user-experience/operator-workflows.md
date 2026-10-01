# Operator workflows

### Operators must have a clear deployment view

**ID: UX-001**

The operator should be able to see managed deployments, their current state, and relevant metadata through a single dashboard view without needing to inspect the raw host configuration.

**Rationale:** Self-hosted operational tools depend on readable and actionable views so administrators can quickly assess and act on infrastructure state.

**Constraints:** The interface must remain focused on operational clarity and not become a general-purpose configuration editor.

**Dependencies:** REQ-002, ADR-002

**Verification expectation:** Manual review of the user interface confirms that deployment status and lifecycle controls are visible and understandable.

### Operators must be able to trigger lifecycle actions explicitly

**ID: UX-002**

The user experience should provide direct, explicit actions for starting or stopping deployments and for refreshing deployment state when needed.

**Rationale:** Infrastructure workflows depend on the operator making intentional decisions rather than implicit side effects.

**Constraints:** Actions must not hide the difference between a deployment start and a forced recreation, when that option is available.

**Dependencies:** REQ-002, ADR-003

**Verification expectation:** Interaction testing confirms each lifecycle action is represented clearly and triggers the intended backend behavior.
