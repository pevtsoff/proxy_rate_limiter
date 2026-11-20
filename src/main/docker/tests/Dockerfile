# syntax=docker/dockerfile:1.7
# Test runner: Maven + JDK in a container (no Java on the host). Integration tests start
# PostgreSQL and Redis with Testcontainers through the host's Docker socket.
# Build context: repository root.

FROM maven:3.9-eclipse-temurin-21
WORKDIR /workspace
COPY pom.xml .
COPY db db
COPY src src
COPY target-api target-api
CMD ["bash", "-c", "mvn -B -f target-api/pom.xml test && mvn -B verify"]
