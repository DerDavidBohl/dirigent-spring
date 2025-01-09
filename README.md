# Dirigent

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
      - DIRIGENT_GIT_AUTHTOKEN=<Your Auth token with Access to your repos (only if needed)>
    ports:
      - 8080:8080
    volumes:
      - /path/to/config:/app/config
      - /path/to/deployments:/app/deployments
      - /var/run/docker.sock:/var/run/docker.sock

```
