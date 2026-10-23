[![Build Docker Image](https://github.com/DerDavidBohl/dirigent-spring/actions/workflows/build.yml/badge.svg)](https://github.com/DerDavidBohl/dirigent-spring/actions/workflows/build.yml)

# <img src="https://raw.githubusercontent.com/DerDavidBohl/dirigent-spring/latest/assets/icon-background.svg" width="80px"></img> Dirigent
Tool to manage your docker compose deployments via git.

See the [changelog](CHANGELOG.md) for release notes and the [2.x to 3.0 upgrade guide](docs/upgrading-to-3.0.md) when upgrading from 2.x. Installations still on 1.x should first follow the [1.x to 2.0 upgrade guide](docs/upgrading-to-2.0.md).

## Table of Contents

- [Screenshots](#screenshots)
- [Why Dirigent?](#why-dirigent)
- [Setup](#setup)
  - [docker-compose](#docker-compose)
  - [docker CLI](#docker-cli)
  - [Environment Variables](#environment-variables)
  - [Authentication](#authentication)
  - [deployments.yml](#deploymentsyml)
  - [Volumes](#volumes)
  - [Step by Step (Gitea)](#step-by-step-gitea)
- [Tweaks](#tweaks)
- [API](#api)
  - [Gitea Webhook](#gitea-webhook)
  - [Deployments](#deployments)
    - [Start](#start)
      - [All Deployments](#all-deployments)
      - [Deployment by name](#deployment-by-name)
    - [Stop](#stop)
      - [Deployment by name](#deployment-by-name-1)
    - [State](#state)
- [Develop](#develop)
  - [Manual test environment](#manual-test-environment)
- [Roadmap](#roadmap)

## Screenshots

![image](/assets/screenshots/dashboard.png)

## Why Dirigent?

**TL;DR**: Dirigent brings **GitOps simplicity** to Docker Compose.

Dirigent simplifies **GitOps for Docker Compose deployments** by automating the management of your containers through Git. Here’s why you should use it:

### ✅ **Git-Centric Deployments**
- **Version-controlled infrastructure**: Track changes to your Docker Compose setups via Git, ensuring reproducibility and auditability.
- **Automated sync**: Push changes to your Git repo, and Dirigent handles deployment — no manual `docker compose` commands needed.

### 🔄 **Seamless Git Integration**
- **Webhook-driven updates**: Trigger deployments automatically when your repo changes
- **Centralized management**: Define all deployments in a single `deployments.yml` file for easy orchestration.

### 🚀 **Self-Hosting Made Simple**
- **Designed for homelabs & private clouds**: Perfect for developers who self-host with Docker and want Git-based control.
- **No Kubernetes complexity**: Lightweight alternative to full-blown GitOps tools like ArgoCD or Flux—just Docker Compose + Git.

### 🔧 **Extensible & Developer-Friendly**
- **REST API**: Integrate with existing tools (CI/CD, monitoring).
- **Gotify notifications**: Get alerts when deployments fail, and an hourly reminder while machine tokens are expired but not revoked.

### 🔒 Encrypted Secrets
- You can store encrypted Secrets that are injected as Environment Variable on execution

Ideal for Self-hosters managing multiple services (e.g., Nextcloud, Gitea, Vaultwarden).

## Setup

### docker-compose

```yml

volumes:
  config:
  deployments:
    name: dirigent_deployments
  data:

services:
  app:
    image: ghcr.io/derdavidbohl/dirigent-spring:3
    container_name: dirigent-app
    restart: unless-stopped
    environment:
      - DIRIGENT_DEPLOYMENTS_GIT_URL= # required
      - DIRIGENT_SECRETS_ENCRYPTION_KEY= # Has to be 16 Chars. Can be generated with `openssl rand -base64 12`
      - DIRIGENT_COMPOSE_COMMAND= # optional
      - DIRIGENT_GIT_AUTHTOKEN= # optional
      - DIRIGENT_START_ALL_ON_STARTUP= # optional
      - DIRIGENT_DEPLOYMENTS_SCHEDULE_ENABLED= # optional
      - DIRIGENT_DEPLOYMENTS_SCHEDULE_CRON= # optional
      - DIRIGENT_GOTIFY_BASEURL= # optional
      - DIRIGENT_GOTIFY_TOKEN= # optional
      - DIRIGENT_INSTANCENAME= # optional but recommended
      - DIRIGENT_UPDATES_DISABLED= # optional
      - DIRIGENT_AUTH_COOKIE_SECURE=true # use HTTPS in production
      # Optional OIDC login. Set ENABLED=true and provide client ID, secret, and HTTPS issuer URI.
      - DIRIGENT_OIDC_ENABLED=false
      - DIRIGENT_OIDC_CLIENT_ID=
      - DIRIGENT_OIDC_CLIENT_SECRET=
      - DIRIGENT_OIDC_ISSUER_URI=
      - DIRIGENT_OIDC_SCOPE=openid,profile,email
      - DIRIGENT_OIDC_REQUIRED_GROUP= # optional provider group gate
      - DIRIGENT_OIDC_GROUPS_CLAIM=groups # optional claim name override
      # Optional read/write federation. Repeat the indexed settings for each remote instance.
      - DIRIGENT_FEDERATION_INSTANCES_0_ID=office
      - DIRIGENT_FEDERATION_INSTANCES_0_BASE_URL=https://dirigent-office.example.com
      - DIRIGENT_FEDERATION_INSTANCES_0_TOKEN=
      - DIRIGENT_FEDERATION_INSTANCES_0_CERTIFICATE_SHA256= # optional pin for self-signed remote TLS
      - DIRIGENT_TLS_HOSTNAMES=dirigent.example.com # names/IPs clients use; include before first start
      # Optional BYOC; otherwise a self-signed certificate is generated and persisted in /app/data/tls.
      # - SERVER_SSL_KEY_STORE=file:/run/secrets/dirigent.p12
      # - SERVER_SSL_KEY_STORE_PASSWORD=
      # - SERVER_SSL_KEY_STORE_TYPE=PKCS12
      # Set DIRIGENT_TLS_ENABLED=false only when a trusted reverse proxy terminates TLS.
      # Optional private registry credentials. Repeat with index 1, 2, ... for more registries.
      - DIRIGENT_REGISTRIES_0_HOST= # registry hostname, e.g. gitea.example.com
      - DIRIGENT_REGISTRIES_0_USERNAME= # registry username
      - DIRIGENT_REGISTRIES_0_PASSWORD= # registry token or password
      - DIRIGENT_HOST_DEPLOYMENTS_DIR= # optional. Needed, if you mount your config volume to a diffrent place on you root system. Defaults do <docker-root>/volumes/dirigent_deployments/_data
    ports:
      - "8443:8443"
    volumes:
      - config:/app/config
      - deployments:/app/deployments
      - data:/app/data
      - /var/run/docker.sock:/var/run/docker.sock
```

### docker CLI

```bash
docker run -d \
  --name=dirigent \
  -p 8443:8443 \
  -e DIRIGENT_DEPLOYMENTS_GIT_URL= \
  -e DIRIGENT_SECRETS_ENCRYPTION_KEY= \
  -e DIRIGENT_COMPOSE_COMMAND= \
  -e DIRIGENT_GIT_AUTHTOKEN= \
  -e DIRIGENT_START_ALL_ON_STARTUP= \
  -e DIRIGENT_DEPLOYMENTS_SCHEDULE_ENABLED= \
  -e DIRIGENT_DEPLOYMENTS_SCHEDULE_CRON= \
  -e DIRIGENT_GOTIFY_BASEURL= \
  -e DIRIGENT_GOTIFY_TOKEN= \
  -e DIRIGENT_INSTANCENAME= \
  -e DIRIGENT_UPDATES_DISABLED= \
  -e DIRIGENT_AUTH_COOKIE_SECURE=true \
  -e DIRIGENT_OIDC_ENABLED=false \
  -e DIRIGENT_OIDC_CLIENT_ID= \
  -e DIRIGENT_OIDC_CLIENT_SECRET= \
  -e DIRIGENT_OIDC_ISSUER_URI= \
  -e DIRIGENT_OIDC_SCOPE=openid,profile,email \
  -e DIRIGENT_OIDC_REQUIRED_GROUP= \
  -e DIRIGENT_OIDC_GROUPS_CLAIM=groups \
  -e DIRIGENT_FEDERATION_INSTANCES_0_ID=office \
  -e DIRIGENT_FEDERATION_INSTANCES_0_BASE_URL=https://dirigent-office.example.com \
  -e DIRIGENT_FEDERATION_INSTANCES_0_TOKEN= \
  -e DIRIGENT_FEDERATION_INSTANCES_0_CERTIFICATE_SHA256= \
  -e DIRIGENT_TLS_HOSTNAMES=dirigent.example.com \
  -e SERVER_SSL_KEY_STORE_TYPE=PKCS12 \
  -p 8443:8443 \
  -e DIRIGENT_HOST_DEPLOYMENTS_DIR=/path/to/deployments \
  -v /path/to/config:/app/config \
  -v /path/to/deployments:/app/deployments \
  -v /path/to/data:/app/data \
  -v /var/run/docker.sock:/var/run/docker.sock \
  ghcr.io/derdavidbohl/dirigent-spring:3
```

### Environment Variables

| Variable                              | Description                                                                                           | Default          |
|---------------------------------------|-------------------------------------------------------------------------------------------------------|------------------|
| DIRIGENT_DEPLOYMENTS_GIT_URL          | URL to your deployments git repository                                                                |                  |
| DIRIGENT_SECRETS_ENCRYPTION_KEY       | Encryption Key to save encrypted secrets. Has to be 16 Chars. Can be generated with `openssl rand -base64 12` |          |
| DIRIGENT_COMPOSE_COMMAND              | Command to run your docker-compose files                                                              | `docker compose` |
| DIRIGENT_GIT_AUTHTOKEN                | Auth token with access to your repos                                                                  |                  |
| DIRIGENT_START_ALL_ON_STARTUP         | Start all deployments on startup                                                                      | `true`           |
| DIRIGENT_DEPLOYMENTS_SCHEDULE_ENABLED | Enable scheduled deployment reconciliation                                               | `true`           |
| DIRIGENT_DEPLOYMENTS_SCHEDULE_CRON    | Cron expression (second minute hour day(month) month day(week))                          | `0 */5 * * * *`  |
| DIRIGENT_GOTIFY_BASEURL               | Gotify Base URL for Notification, when deployments fail                                               |                  |
| DIRIGENT_GOTIFY_TOKEN                 | Gotify Token for Notification, when deployments fail                                                  |                  |
| DIRIGENT_INSTANCENAME                 | Name of your Instance (shown in Web UI and notification titles)                                                       |                  |
| DIRIGENT_UPDATES_DISABLED             | Disable Update Feature                                                                                |`false`           |
| DIRIGENT_AUTH_COOKIE_SECURE           | Set Secure on session and CSRF cookies; use HTTPS in production                                       | `true`           |
| DIRIGENT_OIDC_ENABLED                 | Enable OIDC login                                                                                    | `false`          |
| DIRIGENT_OIDC_CLIENT_ID               | OIDC client identifier (required when enabled)                                                       |                  |
| DIRIGENT_OIDC_CLIENT_SECRET           | OIDC client secret (required when enabled)                                                           |                  |
| DIRIGENT_OIDC_ISSUER_URI              | HTTPS issuer URL (required when enabled)                                                             |                  |
| DIRIGENT_OIDC_SCOPE                   | OIDC scopes requested                                                                                | `openid,profile,email` |
| DIRIGENT_OIDC_REQUIRED_GROUP          | Optional group required for OIDC access                                                              |                  |
| DIRIGENT_OIDC_GROUPS_CLAIM            | Claim containing provider group membership                                                           | `groups`         |
| DIRIGENT_FEDERATION_INSTANCES_0_ID    | Stable ID of the first remote Dirigent instance                                                     |                  |
| DIRIGENT_FEDERATION_INSTANCES_0_BASE_URL | HTTPS base URL of the first remote instance                                                        |                  |
| DIRIGENT_FEDERATION_INSTANCES_0_TOKEN | Expiring machine token for the first remote instance                                                 |                  |
| DIRIGENT_FEDERATION_INSTANCES_0_CERTIFICATE_SHA256 | Optional SHA-256 fingerprint pin for a self-signed remote certificate                     |                  |
| DIRIGENT_TLS_HOSTNAMES                | Comma-separated DNS names/IPs included in the generated server certificate                         | `localhost,127.0.0.1` |
| DIRIGENT_TLS_ENABLED                  | Enable native HTTPS; disable only behind a trusted TLS-terminating proxy                           | `true`           |
| DIRIGENT_TLS_ROTATION_ENABLED         | Automatically rotate the generated certificate before expiry                                       | `true`           |
| DIRIGENT_TLS_ROTATION_RENEW_BEFORE_DAYS | Renewal window before expiry, in days                                                            | `30`             |
| SERVER_SSL_KEY_STORE                  | Optional PKCS#12 keystore path for a bring-your-own certificate                                    | Generated       |
| SERVER_SSL_KEY_STORE_PASSWORD         | Password for the configured BYOC keystore                                                          |                  |
| SERVER_SSL_KEY_STORE_TYPE             | Keystore type for BYOC                                                                             | `PKCS12`         |
| DIRIGENT_HOST_DEPLOYMENTS_DIR             | Needed, if you mount your config volume to a diffrent place on you root system.                                                                                | Defaults do `<docker-root>/volumes/dirigent_deployments/_data`          |
| DIRIGENT_REGISTRIES_0_HOST             | Hostname of the first private Docker registry                                                          |                  |
| DIRIGENT_REGISTRIES_0_USERNAME         | Username for the first private Docker registry                                                        |                  |
| DIRIGENT_REGISTRIES_0_PASSWORD         | Token or password for the first private Docker registry                                               |                  |

Authentication settings include `DIRIGENT_AUTH_COOKIE_SECURE` for Secure session/CSRF cookies. Optional OIDC uses `DIRIGENT_OIDC_ENABLED`, `DIRIGENT_OIDC_CLIENT_ID`, `DIRIGENT_OIDC_CLIENT_SECRET`, `DIRIGENT_OIDC_ISSUER_URI`, and optional `DIRIGENT_OIDC_SCOPE` (default `openid,profile,email`). Set `DIRIGENT_OIDC_ENABLED=true` only after supplying the client ID, client secret, and HTTPS issuer URI. By default every successfully authenticated user of that issuer is accepted. To restrict access, set `DIRIGENT_OIDC_REQUIRED_GROUP` to a group emitted in the `DIRIGENT_OIDC_GROUPS_CLAIM` claim (default claim name `groups`). Configure the OIDC provider to include that claim in the ID token or UserInfo response. Dirigent discovers provider endpoints from the issuer URI.

#### Authentication

The dashboard and REST API require authentication. On a fresh database, create the first local administrator from an interactive terminal; the password is prompted without echo and must be 12 to 72 UTF-8 bytes:

```bash
docker exec -it dirigent-app java -jar /app/app.jar --spring.profiles.active=production --create-admin operator
```

There is no public local-account registration or default password. Additional local administrators can be created with the same command. Configure OIDC using the `DIRIGENT_OIDC_*` environment variables shown in the Compose example. The issuer URI must use HTTPS. Register `https://<dirigent-host>/login/oauth2/code/dirigent` as the provider callback URL. Any user successfully authenticated by that issuer can sign in and receives administrator access; there is no per-user OIDC CLI provisioning. To restrict access, configure `DIRIGENT_OIDC_REQUIRED_GROUP` and ensure the provider emits that group in the configured claim. Retain a local administrator for recovery.

Reset a local account password or delete a local account with these interactive CLI operations:

```bash
docker exec -it dirigent-app java -jar /app/app.jar --spring.profiles.active=production --reset-admin-password operator
docker exec -it dirigent-app java -jar /app/app.jar --spring.profiles.active=production --delete-local-account operator
```

Password reset prompts twice without echo. Deletion requires typing the account name and refuses to remove the final local administrator. These operations cannot change OIDC-only identities.

Use the dashboard's **Machine tokens** page (`/settings/tokens`) to create scoped, revocable tokens for automation. The page shows when each token was last used and hides revoked tokens unless you enable **Show revoked tokens**. For headless instances managed through configuration or federation, issue a token on the instance with the server-side CLI. This command uses the same database and token validation as dashboard creation and does not start HTTP serving or deployment synchronization:

```bash
docker exec -it dirigent-app java -jar /app/app.jar --spring.profiles.active=production \
  --create-machine-token federation-hub \
  --token-permissions deployments:read,deployments:write,updates:read,updates:write,secrets:read,secrets:write,system:read \
  --token-expires-at 2027-01-01T00:00:00Z
```

The name, comma-separated permissions, and expiry instant are required. Use only the permissions needed by the integration; for a Gitea webhook, use only `webhooks:trigger`. Expiry must be in the future and no more than 366 days away. The raw token is printed once to stdout. Capture it securely, do not redirect it to a persistent log or include the command in shared shell history, and store it in the hub's secret configuration. Send it as `Authorization: Bearer <token>`; never put it in a URL or commit it to a repository. The dashboard also displays the full token only once. Existing tokens without an expiry are rejected and must be replaced. Human browser sessions remain cookie-based and protected by CSRF tokens.

Failed local-login attempts are rate limited per source peer and normalized username. When running behind a reverse proxy, restrict direct application access and configure rate limiting at that trusted proxy too; Dirigent does not trust arbitrary forwarded-IP headers.

Set `DIRIGENT_AUTH_COOKIE_SECURE=true` when serving through HTTPS, which is the production default. For local HTTP development only, set it to `false`; do not expose an HTTP deployment to an untrusted network.

#### Private Docker registries

Dirigent supports different credentials for different Docker registries. Configure each registry with the same index:

```yaml
services:
  app:
    environment:
      - DIRIGENT_REGISTRIES_0_HOST=gitea.example.com
      - DIRIGENT_REGISTRIES_0_USERNAME=deploy-user
      - DIRIGENT_REGISTRIES_0_PASSWORD=your-gitea-token
      - DIRIGENT_REGISTRIES_1_HOST=ghcr.io
      - DIRIGENT_REGISTRIES_1_USERNAME=your-github-user
      - DIRIGENT_REGISTRIES_1_PASSWORD=your-ghcr-token
```

The registry host must match the hostname used in the image reference. For example:

```yaml
services:
  private-service:
    image: gitea.example.com/my-org/private-service:latest
```

Dirigent uses these credentials in two places:

- To authenticate when checking registry digests for available updates.
- To run `docker login` before `docker compose up`, including update-triggered deployments.

Registry entries are indexed from zero, so additional registries use `DIRIGENT_REGISTRIES_2_*`, `DIRIGENT_REGISTRIES_3_*`, and so on. Keep tokens in deployment secrets or an environment management system rather than committing them to this file. The password is passed to `docker login` through standard input and is not added to the command arguments.

### deployments.yml

The deployments.yml contains the list of repos you want to deploy. Every deployment needs a name and a source. You can optionally define an order, if one deployment depends on another deployment.  
  
Here is an example of a `deployments.yml`:

```yaml
deployments:
  - name: test1
    source: https://github.com/url/tomyrepo1.git
  - name: test2
    source: https://github.com//url/tomyrepo2.git
    order: 10
```

### Volumes

| Volume               | Description                            |
|----------------------|----------------------------------------|
| /app/config          | Config directory for Dirigent          |
| /app/deployments     | Deployments directory for Dirigent     |
| /app/data            | Data directory containing the database |
| /var/run/docker.sock | Docker socket for Dirigent             |

### Security considerations

- Access to the Docker socket grants broad control over the host's containers; run Dirigent only in an environment trusted with that authority.
- Dashboard and REST API routes require a local/OIDC administrator session or an authorized scoped service token. Use HTTPS and restrict network reachability as defense in depth.
- Federation is opt-in and uses indexed `DIRIGENT_FEDERATION_INSTANCES_<n>_ID`, `_BASE_URL`, and `_TOKEN` settings. Add one entry per remote instance; configure a stable ID, an HTTPS base URL, and a token created on that remote. The token needs only `deployments:read`, `deployments:write`, `updates:read`, `updates:write`, `secrets:read`, `secrets:write`, and `system:read`; do not grant `webhooks:trigger`. Keep tokens in a secret manager, rotate them before expiry, and use TLS certificates trusted by the hub. The hub can start/stop deployments, apply updates, and manage secrets on every configured remote, so compromise of the hub exposes those capabilities. Remote account, OIDC, and machine-token administration remain local to each instance.
- Gitea webhook requests require a machine token with only the `webhooks:trigger` permission where possible. Send it in `Authorization: Bearer <token>`, use HTTPS, set an expiry, and revoke it when the integration is removed. Restrict endpoint reachability as defense in depth.
- The Docker build excludes `backend/src/main/resources/application-local.properties` and local data. If an image was built before this exclusion, treat local credentials and encryption keys as disclosed and rotate them.
- Keep registry and Git credentials in an environment-management system and restrict access to the Dirigent host and its configuration.

### Step by Step (Gitea)

#### Setup Deployments Repo
1. Create a new repository in Gitea
2. Create a new file `deployments.yml` in the root of your repository with the following content:
    ```yaml
    deployments: []
    ```
3. Deploy the dirigent app as described above. Set the `DIRIGENT_DEPLOYMENTS_GIT_URL` to the URL of your repository. Dont forget to set the `DIRIGENT_GIT_AUTHTOKEN` if your repository is private.
4. Create a service token in Dirigent with the `webhooks:trigger` permission. In Gitea, configure the webhook URL as `https://<dirigent-host>/api/v1/gitea` and set the request Authorization header to `Bearer <token>`. Store the token in Gitea's secret/variable facility; do not commit it to the repository.

#### Add Deployments
1. Create a git repository for your deployment
2. Add a `docker-compose.yml` to your repository
3. Add a new entry to the `deployments.yml` in your deployments repository with the name of your deployment and the URL to your deployment repository. Optionally you can set an order, if your deployment depends on another deployment.
    ```yaml
    deployments:
      - name: test1
        source: https://url/toyourdeploymentrepo.git
        order: 10 # optional
    ```
4. Add a webhook in your deployment repository using the same endpoint and bearer token. One token may be shared by related repository webhooks; use a separate token when you need independent revocation.

#### Optional good practice:
Store all your repositories for one host in one gitea organization. This way you only have to set up one webhook at organization level.

## Tweaks

### Dirigent Host Deployments Dir

Sometimes, you want to mount files or directories from your repo inside your docker compose. To help you with that Dirigent always passes the environment variable `DIRIGENT_DEPLOYMENT_DIR` to its compose up commands.

You can use it inside your docker-compose.yml Files to reference files, you want to track in you repository.

```yaml
services:
  my-service:
    image: my-image
    volumes:
      - ${DIRIGENT_DEPLOYMENT_DIR}/config-dir:/var/lib/config-dir
```

> The value of `DIRIGENT_DEPLOYMENT_DIR` defaults to `<docker-root>/volumes/dirigent_deployments/_data`. If you mount a diffrent directory from your host as deployments dir, you have to set the `DIRIGENT_HOST_DEPLOYMENTS_DIR` variable as environment to your setup!

## API

### Gitea Webhook

`POST` to `https://<dirigent-host>/api/v1/gitea`

Gitea must send `Authorization: Bearer <token>` with a token granted the `webhooks:trigger` permission. Anonymous requests and tokens without that permission are rejected. Webhook token authentication does not prevent replay of a captured request; use HTTPS and restrict network reachability.

### Authentication

- `POST /api/v1/auth/login` accepts form fields `username` and `password` and establishes a browser session. Fetch `GET /api/v1/auth/csrf` first; send the returned cookie/header pair on state-changing browser requests.
- `GET /api/v1/auth/me` returns the current authenticated identity.
- `POST /api/v1/auth/logout` ends the browser session.
- `GET /api/v1/auth/options` reports whether OIDC is configured.
- `GET /api/v1/auth/tokens` lists token metadata. `POST /api/v1/auth/tokens` issues a token and returns the secret once. `DELETE /api/v1/auth/tokens/{id}` revokes it. These management endpoints require a human administrator session.
- Machine tokens use `Authorization: Bearer <token>` and are limited to their selected permissions. They do not create browser sessions.

### Native HTTPS and certificates

Dirigent serves HTTPS on port `8443` by default. On first startup it creates a self-signed certificate and private key under `/app/data/tls` and reuses them on later starts. Keep the `/app/data` volume persistent and accessible only to the Dirigent process. Set `DIRIGENT_TLS_HOSTNAMES` to the DNS names or IP addresses clients will use before the first startup; generated certificates also include `localhost` and `127.0.0.1`. Publish host port 8443 to container port 8443. Browsers do not automatically trust this certificate; install its public certificate in the browser/OS trust store or use a certificate issued by a trusted CA. Do not bypass hostname or certificate warnings for production use.

To use your own certificate, mount a PKCS#12 keystore read-only and configure `SERVER_SSL_KEY_STORE`, `SERVER_SSL_KEY_STORE_PASSWORD`, and `SERVER_SSL_KEY_STORE_TYPE=PKCS12`. To keep TLS at a trusted reverse proxy, set `DIRIGENT_TLS_ENABLED=false`, publish/map the backend's HTTP port (8080 by default), and do not expose it directly to untrusted networks. Existing reverse-proxy installations must set this explicitly before upgrading.

#### Existing reverse-proxy installations

Before upgrading an installation that currently proxies to Dirigent over HTTP:

1. Add `DIRIGENT_TLS_ENABLED=false` to the Dirigent service environment.
2. Keep the proxy's upstream on HTTP port `8080`; do not publish that backend port to an untrusted network.
3. Recreate Dirigent and verify the dashboard and API through the proxy before removing the old container.

Alternatively, keep native TLS enabled and update the proxy upstream to HTTPS port `8443`, configuring it to trust Dirigent's certificate and verify its hostname.

For a generated self-signed remote, obtain its leaf-certificate fingerprint through trusted local access to that host:

```bash
docker exec dirigent-app sh -c 'keytool -exportcert -alias dirigent -keystore /app/data/tls/dirigent.p12 -storepass:file /app/data/tls/keystore.password' | sha256sum
```

Set the resulting 64-character hex value as `DIRIGENT_FEDERATION_INSTANCES_<n>_CERTIFICATE_SHA256` on the hub. This trusts only that leaf certificate; the URL hostname must still match its SAN, and the certificate must be valid. When the certificate is regenerated or replaced, securely update the pin. Do not use a trust-all setting.

Generated certificates rotate automatically: Dirigent checks at startup and every 12 hours (`DIRIGENT_TLS_ROTATION_CHECK_INTERVAL`, ISO-8601 duration) and replaces the certificate when fewer than `DIRIGENT_TLS_ROTATION_RENEW_BEFORE_DAYS` (default 30) remain, without a restart. The new SHA-256 fingerprint is logged at WARN level; securely update every hub pin, since pinned peers fail closed after rotation. Set `DIRIGENT_TLS_ROTATION_ENABLED=false` to disable. Bring-your-own keystores are never rotated automatically. To force rotation, run `docker exec -it dirigent-app java -jar /app/app.jar --spring.profiles.active=production --rotate-tls-certificate`. It replaces the generated certificate immediately, prints the new SHA-256 fingerprint, and refuses when a bring-your-own keystore is configured; a running server loads the new certificate without a restart (with automatic rotation enabled). For multiple installations, a CA-issued BYOC certificate avoids individual leaf-pin updates.

### Federation

Federation is configured on the hub with indexed `DIRIGENT_FEDERATION_INSTANCES_<n>_ID`, `DIRIGENT_FEDERATION_INSTANCES_<n>_BASE_URL`, and `DIRIGENT_FEDERATION_INSTANCES_<n>_TOKEN` environment variables. The base URL must use HTTPS. The token is created on the remote instance and should include only `deployments:read`, `deployments:write`, `updates:read`, `updates:write`, `secrets:read`, `secrets:write`, and `system:read`; do not grant `webhooks:trigger`.

The hub exposes instance status at `GET /api/v1/federation/instances` and proxies only the existing deployment, update, secret, and system-information workflows under `/api/v1/federation/instances/{id}/...`. Unknown resources are rejected; remote account and token administration are not federated. Remote writes are audited without request bodies or credentials. A remote authentication failure is reported as a gateway error and does not end the hub browser session.

### Deployments

#### Start

**Parameters**

| Parameter       | Description                                          |
|-----------------|------------------------------------------------------|
| `forceRecreate=true` | forces recreation of the targeted deployment(s) |

##### All Deployments:

`POST` to `/api/v1/deployments/all/start`

##### Deployment by name:

`POST` to `/api/v1/deployments/{name}/start`

#### Stop

##### Deployment by name:

`POST` to `/api/v1/deployments/{name}/stop`

#### State

`GET` to `/api/v1/deployment-states`

### Secrets

#### Model

```json
{
  "key": "string",
  "value": "string",
  "environmentVariable": "string",
  "deployments": ["string"]
}
```

#### Get Secrets

`GET` to `/api/v1/secrets`

#### Save Secret

`PUT` to `/api/v1/secrets/{key}`

#### Delete Secret

`DELETE` to `/api/v1/secrets/{iey}`

## Develop

### Manual test environment

Runs the current working tree locally with authentication, isolated from real data. Requires Java, Maven, Node/npm, `make`, Git, and Docker with Compose (for deployments).

| Command         | Purpose                                                                              |
|-----------------|--------------------------------------------------------------------------------------|
| `make dev`      | Start backend (`127.0.0.1:8080`) and dashboard (`http://localhost:4200`); loads the `basic` scenario if none is loaded |
| `make scenario` | Load a test scenario (default `basic`, see `dev/scenarios`); then restart `make dev` |
| `make reset`    | Erase all local test data; refuses to run while the backend is running               |
| `make test`     | Run backend tests, frontend lint and tests, and the frontend production build                                |

- Sign in with `admin` / `admin`. This account exists only in this environment (`dev` profile, test sources) and is never part of the production image.
- All state (database, secrets, tokens, cloned configuration and deployments) lives in `dev/.state` and is git-ignored. `make reset` deletes it; containers already started by a scenario are not removed.
- Scenarios are local Git repositories built from `dev/scenarios/<name>`: `config/deployments.yml` plus one folder per deployment under `repos/`. Add a folder there to create a new scenario, then run `make scenario SCENARIO=<name>`.
- Backend tests that need Docker fail when Docker is not running.
- Requires JDK 25 (see `java.version` in `backend/pom.xml`).

Fresh start: stop `make dev`, then `make reset dev`.

## Roadmap

The roadmap separates planned work from delivered capabilities.

### Planned

- [ ] Automate image update application (currently operator-triggered) - Work in Progress
- [ ] Retain and expose completed image-update outcomes
- [ ] Expand automated test coverage for deployment lifecycle and API integration gaps
- [ ] Reduce the initial frontend bundle size
- [ ] Add cleanups (i. e. `docker system prune` or `docker volume prune`)
- [ ] Add backup and restore support
- [ ] Add a single repo mode
- [ ] Improve path handling for deployment-related files in repositories
- [ ] Validate deployment configuration and recover safely from invalid changes
- [ ] Improve secret encryption and add key rotation
- [ ] Improve certificate rotation experience
- [ ] Improve notifications
- [ ] Show more information about instances, including their version
- [x] Identify the affected instance clearly in notifications
- [ ] Support additional notification destinations
- [ ] Allow unused features to be disabled to reduce resource use

### Achieved

- [x] Image-update discovery and operator-triggered application
- [x] Automated test suite and release gate
- [x] Gotify deployment, update, and expired-token notifications
- [x] Native HTTPS with generated or operator-supplied certificates
- [x] Multiple Dirigent hosts (via federation)
- [x] Authentication (local accounts, OIDC, and scoped machine tokens)
