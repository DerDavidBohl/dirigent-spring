[![Build Docker Image](https://github.com/DerDavidBohl/dirigent-spring/actions/workflows/build.yml/badge.svg)](https://github.com/DerDavidBohl/dirigent-spring/actions/workflows/build.yml)

# <img src="https://raw.githubusercontent.com/DerDavidBohl/dirigent-spring/latest/assets/icon-background.svg" width="80px"></img> Dirigent
Tool to manage your docker compose deployments via git.

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
  - [Setup for local Tests](#setup-for-local-tests)
- [Roadmap](#roadmap)

## Screenshots

![image](https://github.com/user-attachments/assets/2e0ab3e1-d702-4cc2-9c05-055dc41136ff)

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
- **Gotify notifications**: Get alerts when deployments fail.

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
    image: ghcr.io/derdavidbohl/dirigent-spring:1
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
      # Optional group gate. Provider must emit the configured claim.
      - DIRIGENT_OIDC_REQUIRED_GROUP=
      - DIRIGENT_OIDC_GROUPS_CLAIM=groups
      # Optional private registry credentials. Repeat with index 1, 2, ... for more registries.
      - DIRIGENT_REGISTRIES_0_HOST= # registry hostname, e.g. gitea.example.com
      - DIRIGENT_REGISTRIES_0_USERNAME= # registry username
      - DIRIGENT_REGISTRIES_0_PASSWORD= # registry token or password
      - DIRIGENT_HOST_DEPLOYMENTS_DIR= # optional. Needed, if you mount your config volume to a diffrent place on you root system. Defaults do <docker-root>/volumes/dirigent_deployments/_data
    ports:
      - 8080:8080
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
  -e DIRIGENT_DEPLOYMENTS_GIT_URL= \
  -e DIRIGENT_SECRETS_ENCRYPTION_KEY= \ # Has to be 16 Chars. Can be generated with `openssl rand -base64 12`
  #optional
  -e DIRIGENT_COMPOSE_COMMAND= \
  #optional
  -e DIRIGENT_GIT_AUTHTOKEN= \
  #optional
  -e DIRIGENT_STARTALL_ON_STARTUP= \
  #optional
  -e DIRIGENT_DEPLOYMENTS_SCHEDULE_ENABLED= \
  #optional
  -e DIRIGENT_DEPLOYMENTS_SCHEDULE_CRON= \
  #optional
  -e DIRIGENT_GOTIFY_BASEURL= \
  #optional
  -e DIRIGENT_GOTIFY_TOKEN= \
  #optional but recommended
  -e DIRIGENT_INSTANCENAME= \
  #optional
  -e DIRIGENT_UPDATES_DISABLED= \

  # optional. Needed, if you mount your config volume to a diffrent place on you root system. Defaults do <docker-root>/volumes/dirigent_deployments/_data
  -e DIRIGENT_HOST_DEPLOYMENTS_DIR=/path/to/deployments \


  -v /path/to/config:/app/config \
  -v /path/to/deployments:/app/deployments \
  -v /path/to/data:/app/data \
  -v /var/run/docker.sock:/var/run/docker.sock \
  ghcr.io/derdavidbohl/dirigent-spring:1
```

### Environment Variables

| Variable                              | Description                                                                                           | Default          |
|---------------------------------------|-------------------------------------------------------------------------------------------------------|------------------|
| DIRIGENT_DEPLOYMENTS_GIT_URL          | URL to your deployments git repository                                                                |                  |
| DIRIGENT_SECRETS_ENCRYPTION_KEY       | Encryption Key to save encrypted secrets. Has to be 16 Chars. Can be generated with `openssl rand -base64 12` |          |
| DIRIGENT_COMPOSE_COMMAND              | Command to run your docker-compose files                                                              | `docker compose` |
| DIRIGENT_GIT_AUTHTOKEN                | Auth token with access to your repos                                                                  |                  |
| DIRIGENT_START_ALL_ON_STARTUP         | Start all deployments on startup                                                                      | `true`           |
| DIRIGENT_DEPLOYMENTS_SCHEDULE_ENABLED | enable scheduled start of all deployments                                                             | `true`           |
| DIRIGENT_DEPLOYMENTS_SCHEDULE_CRON    | cron expression for scheduled start of all deployments (second minute hour day(month) month day(week) | `* */5 * * * *`  |
| DIRIGENT_GOTIFY_BASEURL               | Gotify Base URL for Notification, when deployments fail                                               |                  |
| DIRIGENT_GOTIFY_TOKEN                 | Gotify Token for Notification, when deployments fail                                                  |                  |
| DIRIGENT_INSTANCENAME                 | Name of your Instance (will be shown in Web UI)                                                       |                  |
| DIRIGENT_UPDATES_DISABLED             | Disable Update Feature                                                                                |`false`           |
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

Use the dashboard's **Service tokens** tab to create scoped, revocable tokens for automation. The full token is displayed only once. Send it as `Authorization: Bearer <token>`; never put it in a URL or commit it to a repository. Every token must expire within 366 days; renew integrations by creating a replacement token before expiry. Existing tokens without an expiry are rejected and must be replaced. Human browser sessions remain cookie-based and protected by CSRF tokens.

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

### Setup for local Tests

1. copy `src/test/resources/application-local.properties.template` to `src/test/resources/application-local.properties`
2. fill in your test repository url and auth token
3. Done ;)

## Roadmap

Here is a roadmap with future Ideas

- [ ] Automate Updates for deployments - Work in Progress
- [ ] Add automated tests
- [ ] Add cleanups (i. e. `docker system prune` or `docker volume prune`)
- [ ] Add a single repo mode
- [ ] Improve pathhandling for deployment related files in repo
- [ ] Multiple Dirigent hosts
