# NOTE: not verified in this environment (no Docker daemon available here).
# Multi-stage build: compiles the jar inside the image, so
# `docker compose up --build` works with no local Maven/JDK required.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
COPY maven-settings.xml .
# Cache dependencies in their own layer before copying sources.
RUN mvn -q -s maven-settings.xml -DskipTests dependency:go-offline || true
COPY src ./src
RUN mvn -q -s maven-settings.xml -DskipTests package

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app
COPY --from=build /build/target/ledgerprime-backend-1.0.0.jar app.jar

EXPOSE 8080

ENV POSTGRES_HOST=postgres \
    POSTGRES_PORT=5432 \
    POSTGRES_DB=expensetracker \
    POSTGRES_USER=postgres \
    POSTGRES_PASSWORD=postgres

ENTRYPOINT ["java", "-jar", "app.jar"]
