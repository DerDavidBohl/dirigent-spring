# Upgrading from 1.x to 2.0

Dirigent 2.0 requires authentication for the dashboard and REST API. Existing anonymous browser sessions and API clients will no longer work. Gitea webhooks also need a machine token with the dedicated `webhooks:trigger` permission. Existing machine tokens without an expiry are rejected and must be replaced. Review the [changelog](../CHANGELOG.md) for the complete release summary.

## Before upgrading

1. Back up the persistent `/app/data` volume, which contains the SQLite database, and the `/app/config` volume. Keep the current image reference so you know which version you are running.
2. Preserve `DIRIGENT_SECRETS_ENCRYPTION_KEY`. Do not rotate it as part of this upgrade; existing encrypted deployment secrets depend on it.
3. Choose how operators will sign in. You can use a local administrator, configure an HTTPS OIDC issuer, or enable both. For OIDC, prepare the client ID, client secret, issuer URL, and callback URL `https://<dirigent-host>/login/oauth2/code/dirigent`.
4. Identify API clients and webhooks that currently make unauthenticated requests. Plan new machine tokens with only the permissions each integration needs.

## Upgrade steps

1. Change the container image from the 1.x major tag (for example, `ghcr.io/derdavidbohl/dirigent-spring:1`) to the 2.0 major tag, `ghcr.io/derdavidbohl/dirigent-spring:2`, or to the exact 2.0.x tag published for the release. Keep the existing data, config, and deployments volumes mounted at their current paths, and retain the other runtime settings, especially `DIRIGENT_SECRETS_ENCRYPTION_KEY`.
2. Start the new image and wait for application startup to complete. The existing database schema is updated automatically; no manual schema conversion is required.
3. If you will use local login, or want a local recovery account (recommended even when using OIDC), create an administrator through the running container's interactive CLI. Replace `operator` with the account name you want. The command prompts for a password without echoing it:

   ```bash
   docker exec -it dirigent-app java -jar /app/app.jar --spring.profiles.active=production --create-admin operator
   ```

   If you will use OIDC, configure `DIRIGENT_OIDC_ENABLED=true`, `DIRIGENT_OIDC_CLIENT_ID`, `DIRIGENT_OIDC_CLIENT_SECRET`, and `DIRIGENT_OIDC_ISSUER_URI`, then recreate the container. The issuer must use HTTPS. A local administrator is not required when OIDC is the only login method, but provides a recovery path if the identity provider is unavailable. See [Authentication setup](../README.md#authentication) for optional group restrictions and account recovery commands.
4. Sign in to the dashboard using the local account or configured OIDC provider. Confirm the deployments, secrets, and settings pages are accessible.
5. In **Machine tokens**, issue replacement tokens for automation, including any existing non-expiring tokens. Grant each token only the permissions its integration needs and set an expiry no more than 366 days away. Update API clients to send `Authorization: Bearer <token>`. The token value is shown once; store it in the integration's secret store, then revoke superseded tokens.
6. Update each Gitea webhook to send a token with the `webhooks:trigger` permission in its `Authorization` header. Verify a test webhook reaches the expected deployment and check its state in the dashboard.
7. Exercise each API integration against the operations it uses and confirm anonymous requests are rejected. Do not put tokens in URLs or source control.

## Rollback

Do not assume that a database used by 2.0 can be safely opened by 1.x. If rollback is necessary, stop Dirigent, restore the pre-upgrade `/app/data` backup, and then start the previous image with the original configuration and encryption key. Changes made after the backup will not be present in the restored database.