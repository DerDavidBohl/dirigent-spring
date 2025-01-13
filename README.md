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
      - DIRIGENT_DEPLOYMENTS_GIT_URL=<Your Deployments Repo>
      - DIRIGENT_GIT_AUTHTOKEN=<Your Auth token with Access to your repos - only if needed> # optional
      - DIRIGENT_START_ALL_ON_STARTUP=<Start All Deployments On Startup> # optional Default true
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
  -e DIRIGENT_DEPLOYMENTS_GIT_URL=<Your Deployments Repo> \
  -e DIRIGENT_GIT_AUTHTOKEN=<Your Auth token with Access to your repos - only if needed> \
  -e DIRIGENT_STARTALL_ON_STARTUP=<Start All Deployments On Startup - only if needed> \
  -v /path/to/config:/app/config \
  -v /path/to/deployments:/app/deployments \
  -v /var/run/docker.sock:/var/run/docker.sock \
  ghcr.io/derdavidbohl/dirigent-spring:latest
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| DIRIGENT_DEPLOYMENTS_GIT_URL | URL to your deployments git repository | |
| DIRIGENT_GIT_AUTHTOKEN | Auth token with access to your repos | |
| DIRIGENT_START_ALL_ON_STARTUP | Start all deployments on startup | true |

### Volumes

| Volume | Description |
|--------|-------------|
| /app/config | Config directory for Dirigent |
| /app/deployments | Deployments directory for Dirigent |
| /var/run/docker.sock | Docker socket for Dirigent |


## Develop

### Setup for local Tests

1. copy `src/test/resources/application-local.properties.template` to `src/test/resources/application-local.properties`
2. fill in your test repository url and auth token
3. Done ;)
