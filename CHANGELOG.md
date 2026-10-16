# Changelog

Notable changes to Dirigent are recorded here. See the [upgrade guide](docs/upgrading-to-2.0.md) before moving an existing installation to 2.0.

## [2.0.0]

### Breaking changes

- The dashboard and `/api/v1/**` routes now require an authenticated administrator session or an authorized machine token. Existing clients that relied on anonymous access must authenticate.
- Gitea webhook requests now require a machine token with the `webhooks:trigger` permission in the `Authorization: Bearer` header.
- Existing installations must provision a local administrator or configure OIDC before operators can use the dashboard. No default account is created.
- Existing machine tokens without an expiry are rejected; integrations must use replacement tokens with an expiry and only the permissions they need.

### Added

- Local administrator login, optional OpenID Connect login, and scoped, revocable machine tokens with a required expiry of no more than 366 days.
- CLI operations to create, reset, and delete local administrator accounts.