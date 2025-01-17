# Dirigent

Tool to manage your docker compose deployments via git.

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
      - /var/run/docker.sock:/var/run/docker.sock
```

### docker CLI

```bash
docker run -d \
  --name=dirigent \
  -e DIRIGENT_DEPLOYMENTS_GIT_URL= \
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
  -v /var/run/docker.sock:/var/run/docker.sock \
  ghcr.io/derdavidbohl/dirigent-spring:latest
```

### Environment Variables

| Variable                              | Description                                                                                           | Default       |
|---------------------------------------|-------------------------------------------------------------------------------------------------------|---------------|
| DIRIGENT_DEPLOYMENTS_GIT_URL          | URL to your deployments git repository                                                                |               |
| DIRIGENT_GIT_AUTHTOKEN                | Auth token with access to your repos                                                                  |               |
| DIRIGENT_START_ALL_ON_STARTUP         | Start all deployments on startup                                                                      | true          |
| DIRIGENT_DEPLOYMENTS_SCHEDULE_ENABLED | enable scheduled start of all deployments                                                             | true          |
| DIRIGENT_DEPLOYMENTS_SCHEDULE_CRON    | cron expression for scheduled start of all deployments (second minute hour day(month) month day(week) | * */5 * * * * |
| DIRIGENT_GOTIFY_BASEURL               | Gotify Base URL for Notification, when deployments fail                                               |               |
| DIRIGENT_GOTIFY_TOKEN                 | Gotify Token for Notification, when deployments fail                                                  |               |

### deployments.yml

Example of a `deployments.yml`

```yaml
deployments:
  - name: test1
    source: https://github.com/url/tomyrepo1.git
    order: 20
  - name: test2
    source: https://github.com//url/tomyrepo2.git
    order: 10
```

### Volumes

| Volume               | Description                        |
|----------------------|------------------------------------|
| /app/config          | Config directory for Dirigent      |
| /app/deployments     | Deployments directory for Dirigent |
| /var/run/docker.sock | Docker socket for Dirigent         |

## API

### Gitea Webhook

1. Create a new webhook in your repository
2. Set the URL to `http://<dirigent-host-and-port>/api/v1/gitea`
3. Done ;)

### Deployments

#### Start All Deployments:

`POST` to `/api/v1/deployments/all/start`

#### Start Deployment by name:

`POST` to `/api/v1/deployments/{name}/start`

## Develop

### Setup for local Tests

1. copy `src/test/resources/application-local.properties.template` to `src/test/resources/application-local.properties`
2. fill in your test repository url and auth token
3. Done ;)
