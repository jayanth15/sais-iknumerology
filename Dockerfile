# Two-stage build: Render builds the executable Spring Boot JAR from GitHub source.
# No prebuilt name-suggestion.jar needs to be committed to GitHub.

# Stage 1: build with Maven and JDK 17 (matches pom.xml Java 17).
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Resolve dependencies in a separately cached layer for faster future deploys.
COPY pom.xml ./
RUN mvn -B -ntp -DskipTests dependency:go-offline

# Maven builds name-suggestion.jar in /build/target.
COPY src ./src
# Deploy image creation skips tests; run `mvn clean verify` locally/CI.
RUN mvn -B -ntp -DskipTests clean package \
    && test -s /build/target/name-suggestion.jar

# Stage 2: slim Java 17 runtime. Maven and application source are not shipped.
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd --system --gid 10001 numerology \
    && useradd --system --uid 10001 --gid numerology \
         --home-dir /app --shell /usr/sbin/nologin numerology \
    && mkdir -p /app/data \
    && chown -R numerology:numerology /app

COPY --from=build --chown=numerology:numerology /build/target/name-suggestion.jar /app/name-suggestion.jar
COPY --chown=numerology:numerology docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod 755 /app/docker-entrypoint.sh

# The Sarvam key must be supplied as a runtime environment variable in Render.
# Excel-only mode can run without SARVAM_API_KEY.
ENV BIND_ADDRESS=0.0.0.0 \
    PORT=8080 \
    APP_DATA=/app/data
EXPOSE 8080
VOLUME ["/app/data"]
USER numerology
ENTRYPOINT ["/app/docker-entrypoint.sh"]
