# syntax=docker/dockerfile:1.7
# Stub target API dev image. Build context: repository root.
#
# The source (target-api/pom.xml, target-api/src/main/) is bind-mounted in by
# docker-compose.yml, not copied in here, and is compiled on every container start via
# `mvn spring-boot:run`. There is nothing to COPY and nothing baked into the image: only the
# JDK/Maven toolchain and curl (for the healthcheck) live in the image itself.
FROM maven:3.9-eclipse-temurin-21
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /workspace
ENTRYPOINT ["mvn", "-B", "-Dmaven.test.skip=true", \
    "org.springframework.boot:spring-boot-maven-plugin:run"]
