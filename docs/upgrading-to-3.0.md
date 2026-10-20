# Upgrading from 2.x to 3.0

Dirigent 3.0 enables native HTTPS by default, serves on port `8443`, and adds read/write federation. Existing reverse-proxy deployments must explicitly disable native TLS or change the proxy upstream. Review the [changelog](../CHANGELOG.md) for the release summary.

## Before upgrading

1. Back up the persistent `/app/data` and `/app/config` volumes. Keep the current image reference for rollback.
2. Keep `DIRIGENT_SECRETS_ENCRYPTION_KEY` unchanged; existing encrypted secrets depend on it.
3. Choose the TLS topology. For native TLS, identify the DNS names/IPs clients will use and set `DIRIGENT_TLS_HOSTNAMES` before the first 3.0 startup. Keep `/app/data` persistent so the generated certificate remains stable.
4. For an existing TLS-terminating reverse proxy, plan to set `DIRIGENT_TLS_ENABLED=false` and keep its HTTP upstream private on port `8080`, or configure the proxy to trust Dirigent's certificate and proxy to HTTPS port `8443`.
5. If enabling federation, create a dedicated expiring machine token on each remote with only the required `deployments:read/write`, `updates:read/write`, `secrets:read/write`, and `system:read` permissions. Do not grant `webhooks:trigger`. Record each remote HTTPS URL and, for a self-signed certificate, its SHA-256 fingerprint.

## Upgrade steps

1. Configure native HTTPS or the reverse-proxy compatibility mode selected above. For federation, add one indexed `DIRIGENT_FEDERATION_INSTANCES_<n>_ID`, `_BASE_URL`, and `_TOKEN` setting per remote; add `_CERTIFICATE_SHA256` when the remote uses a self-signed certificate.
2. Update the container image to `ghcr.io/derdavidbohl/dirigent-spring:3` and publish port `8443` when using native HTTPS. Keep the existing data, config, and deployments volumes.
3. Start the new image and wait for startup to complete. On first native-TLS startup, Dirigent generates and persists its certificate. The configured hostnames must match the addresses clients use.
4. Verify browser login and API access through the selected TLS topology. For each federated instance, confirm it appears as connected and test read access before trying deployment, update, or secret operations.
5. Verify existing deployments, secrets, webhooks, and integrations. Federation does not replace per-instance account, OIDC, or machine-token administration.

## Rollback

Stop Dirigent before changing images. For an installation that used native HTTPS, restore the previous 2.x image and configure the proxy to use its previous HTTP upstream. Do not remove the 3.0 TLS files from `/app/data`; 2.x ignores them. If rollback reports a database compatibility problem, stop the service and restore the pre-upgrade `/app/data` backup before restarting 2.x. Changes made after that backup will be lost.