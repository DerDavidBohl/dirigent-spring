# Changelog

Notable changes to Dirigent are recorded here. See the [upgrade guide](docs/upgrading-to-2.0.md) before moving an existing installation to 2.0.

## [Unreleased]

### Breaking changes

- The dashboard and `/api/v1/**` routes now require an authenticated administrator session or an authorized machine token. Existing clients that relied on anonymous access must authenticate.
- Gitea webhook requests now require a machine token with the `webhooks:trigger` permission in the `Authorization: Bearer` header.
- Existing installations must provision a local administrator or configure OIDC before operators can use the dashboard. No default account is created.

### Added

- Local administrator login, optional OpenID Connect login, and scoped, expiring machine tokens for automation.
- CLI operations to create, reset, and delete local administrator accounts.