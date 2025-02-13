# Dirigent

Tool to manage your docker compose deployments via git.

## Table of Contents

- [Setup](#setup)
  - [docker-compose](#docker-compose)
  - [docker CLI](#docker-cli)
  - [Environment Variables](#environment-variables)
  - [deployments.yml](#deploymentsyml)
  - [Volumes](#volumes)
  - [Step by Step (Gitea)](#step-by-step-gitea)
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


## Setup

### docker-compose

```yml
services:
  app:
    image: ghcr.io/derdavidbohl/dirigent-spring:latest
    container_name: dirigent-app
    restart: unless-stopped
    environment:
      - DIRIGENT_DEPLOYMENTS_GIT_URL= # required
      - DIRIGENT_COMPOSE_COMMAND= # optional
      - DIRIGENT_GIT_AUTHTOKEN= # optional
      - DIRIGENT_START_ALL_ON_STARTUP= # optional
      - DIRIGENT_DEPLOYMENTS_SCHEDULE_ENABLED= # optional
      - DIRIGENT_DEPLOYMENTS_SCHEDULE_CRON= # optional
      - DIRIGENT_GOTIFY_BASEURL= # optional
      - DIRIGENT_GOTIFY_TOKEN= # optional
    ports:
      - 8080:8080
    volumes:
      - /path/to/config:/app/config
      - /path/to/deployments:/app/deployments
      - /path/to/data:/app/data
      - /var/run/docker.sock:/var/run/docker.sock
```

### docker CLI

```bash
docker run -d \
  --name=dirigent \
  -e DIRIGENT_DEPLOYMENTS_GIT_URL= \
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
  -v /path/to/config:/app/config \
  -v /path/to/deployments:/app/deployments \
  -v /path/to/data:/app/data \
  -v /var/run/docker.sock:/var/run/docker.sock \
  ghcr.io/derdavidbohl/dirigent-spring:latest
```

### Environment Variables

| Variable                              | Description                                                                                           | Default          |
|---------------------------------------|-------------------------------------------------------------------------------------------------------|------------------|
| DIRIGENT_DEPLOYMENTS_GIT_URL          | URL to your deployments git repository                                                                |                  |
| DIRIGENT_COMPOSE_COMMAND              | Command to run your docker-compose files                                                              | `docker compose` |
| DIRIGENT_GIT_AUTHTOKEN                | Auth token with access to your repos                                                                  |                  |
| DIRIGENT_START_ALL_ON_STARTUP         | Start all deployments on startup                                                                      | `true`           |
| DIRIGENT_DEPLOYMENTS_SCHEDULE_ENABLED | enable scheduled start of all deployments                                                             | `true`           |
| DIRIGENT_DEPLOYMENTS_SCHEDULE_CRON    | cron expression for scheduled start of all deployments (second minute hour day(month) month day(week) | `* */5 * * * *`  |
| DIRIGENT_GOTIFY_BASEURL               | Gotify Base URL for Notification, when deployments fail                                               |                  |
| DIRIGENT_GOTIFY_TOKEN                 | Gotify Token for Notification, when deployments fail                                                  |                  |

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

### Step by Step (Gitea)

#### Setup Deployments Repo
1. Create a new repository in Gitea
2. Create a new file `deployments.yml` in the root of your repository with the following content:
    ```yaml
    deployments: []
    ```
3. Deploy the dirigent app as described above. Set the `DIRIGENT_DEPLOYMENTS_GIT_URL` to the URL of your repository. Dont forget to set the `DIRIGENT_GIT_AUTHTOKEN` if your repository is private.
4. Optional: Create a new webhook in your repository. Set the URL to `http://<dirigent-host-and-port>/api/v1/gitea`

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
4. Optional: Add a new webhook in your deployment repository. Set the URL to `http://<dirigent-host-and-port>/api/v1/gitea`

#### Optional good practice:
Store all your repositories for one host in one gitea organization. This way you only have to set up one webhook at organization level.

## API

### Gitea Webhook

`POST` to `http://<dirigent-host-and-port>/api/v1/gitea`

### Deployments

#### Start

**Parameters**

| Parameter       | Description                                          |
|-----------------|------------------------------------------------------|
| `force=true`    | forces Recreation and Run of targeted deployment(s)  |
| `forceRun=true` | only forces run of targeted deployment(s)            |
| `forceRecreate` | only forces recreation of the targeted deployment(s) |

##### All Deployments:

`POST` to `/api/v1/deployments/all/start`

##### Deployment by name:

`POST` to `/api/v1/deployments/{name}/start`

#### Stop

##### Deployment by name:

`POST` to `/api/v1/deployments/{name}/stop`

#### State

`GET` to `/api/v1/deployment-states`

## Develop

### Setup for local Tests

1. copy `src/test/resources/application-local.properties.template` to `src/test/resources/application-local.properties`
2. fill in your test repository url and auth token
3. Done ;)
