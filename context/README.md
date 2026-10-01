# Repository Context

This directory records the normative specifications for the Dirigent project. It captures the intended product behavior, architecture, security model, user experience expectations, quality goals, and coding standards used to guide changes in the repository.

## Applicable context areas

- [Requirements](requirements/README.md)
- [Architecture](architecture/README.md)
- [Security](security/README.md)
- [User Experience](user-experience/README.md)
- [Quality](quality/README.md)
- [Coding](coding/README.md)

## Scope

The repository is a self-hosted GitOps helper for Docker Compose deployments. It supports repository-driven deployment management, secret handling, registry authentication, update checks, and a lightweight web dashboard for operators.

## Navigation

- Start with the requirement records to understand stakeholder goals.
- Follow the architecture records for the system decomposition and operational model.
- Use the security and quality records to evaluate changes with risk and reliability in mind.
- Apply the coding records for implementation conventions and maintainability standards.

## Open assumptions and clarifications

- The repository is intended for self-hosted deployment management rather than multi-tenant SaaS operation.
- Ownership and formal approval workflows are not currently defined in the repository; this onboarding reflects the observed product intent and documented operational needs.
- Deployment behavior assumes a trusted operator is managing the host and Docker socket access.
