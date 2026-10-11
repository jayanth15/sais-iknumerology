
# ==========================================
# STAGE 1: BUILD SPRING BOOT APPLICATION
# ==========================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy Maven configuration and download dependencies
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy source code
COPY src ./src

# Compile and package application
RUN mvn -B clean package -DskipTests

# Copy generated executable JAR to a fixed filename
RUN set -eux; \
    JAR_FILE=$(find target -maxdepth 1 -type f \
        -name "*.jar" \
        ! -name "original-*" \
        ! -name "*-sources.jar" \
        ! -name "*-javadoc.jar" \
        ! -name "*-plain.jar" | head -n 1); \
    test -n "$JAR_FILE"; \
    cp "$JAR_FILE" /build/name-suggestion.jar

# ==========================================
# STAGE 2: JAVA 17 RUNTIME IMAGE
# ==========================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Create non-root user and H2 database directory
RUN groupadd --system --gid 10001 numerology \
    && useradd --system \
       --uid 10001 \
       --gid numerology \
       --home-dir /app \
       --shell /usr/sbin/nologin numerology \
    && mkdir -p /app/data \
    && chown -R numerology:numerology /app

# Copy JAR generated during Maven build
COPY --from=builder \
    --chown=numerology:numerology \
    /build/name-suggestion.jar \
    /app/name-suggestion.jar

# Application configuration
ENV BIND_ADDRESS=0.0.0.0 \
    PORT=8080 \
    APP_DATA=/app/data \
    SERVER_ADDRESS=0.0.0.0 \
    SERVER_PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8"

# Expose Spring Boot port
EXPOSE 8080

# Persistent H2 database storage
VOLUME ["/app/data"]

# Run as non-root user
USER numerology

# Start Spring Boot application
ENTRYPOINT ["java", "-jar", "/app/name-suggestion.jar"]
