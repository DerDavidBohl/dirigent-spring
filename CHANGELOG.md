# Changelog

Notable changes to Dirigent are recorded here. See the [2.x to 3.0 upgrade guide](docs/upgrading-to-3.0.md) when upgrading from 2.x, or the [1.x to 2.0 upgrade guide](docs/upgrading-to-2.0.md) first when upgrading from 1.x.

## [3.1.0]

### Added

- Generated TLS certificates are renewed automatically before expiry and can be rotated immediately with a server-side CLI command.
- Gotify notifications identify the affected Dirigent instance.

### Changed

- Routine deployment logging is quieter, while deployment starts are logged with their trigger.

### Fixed

- Reconciliation skips stale stopped states for deployments that have been removed.

## [3.0.0]

### Breaking changes

- Native HTTPS is enabled by default on port 8443. Existing reverse-proxy deployments must explicitly set `DIRIGENT_TLS_ENABLED=false` and keep the backend HTTP port private, or update the proxy to use HTTPS on port 8443.

### Added

- First-start persistent self-signed TLS certificate generation, configurable advertised hostnames, bring-your-own PKCS#12 certificates, and per-remote federation certificate fingerprint pinning.
- Read/write federation for deployment lifecycle, image updates, secrets, and system information across configured instances.
- Machine tokens show their last use, revoked tokens are hidden by default, and an hourly Gotify notice lists expired, unrevoked tokens.
- Unreachable federated instances now show the failure reason (timeout, connection, certificate, HTTP status such as a rejected token) in the Instances view, and the cause is logged.

## [2.0.0]

### Breaking changes

- The dashboard and `/api/v1/**` routes now require an authenticated administrator session or an authorized machine token. Existing clients that relied on anonymous access must authenticate.
- Gitea webhook requests now require a machine token with the `webhooks:trigger` permission in the `Authorization: Bearer` header.
- Existing installations must provision a local administrator or configure OIDC before operators can use the dashboard. No default account is created.
- Existing machine tokens without an expiry are rejected; integrations must use replacement tokens with an expiry and only the permissions they need.

### Added

- Local administrator login, optional OpenID Connect login, and scoped, revocable machine tokens with a required expiry of no more than 366 days.
- CLI operations to create, reset, and delete local administrator accounts.