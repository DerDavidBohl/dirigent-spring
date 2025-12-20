
FROM node:alpine AS frontend-build
WORKDIR /app
COPY frontend .
RUN rm -rf package-lock.json
RUN npm cache clean --force
RUN npm install
RUN npm run build

# Use Maven image to build the application
FROM maven:3.9.11-eclipse-temurin-25-alpine AS backend-build
WORKDIR /app
COPY backend/pom.xml .
COPY backend/src ./src
COPY --from=frontend-build /app/dist/browser ./src/main/resources/static
RUN mvn clean package -DskipTests

# Use OpenJDK image to run the application
FROM eclipse-temurin:25-alpine

# Install Docker, git, and tini (init system to reap zombies)
RUN apk add docker docker-compose git tini

# Finish
WORKDIR /app
COPY --from=backend-build /app/target/*.jar app.jar
EXPOSE 8080

# Use tini as init to reap zombie processes
ENTRYPOINT ["/sbin/tini", "--", "java", "-jar", "app.jar", "--spring.profiles.active=production"]
