
# ==============================================
# STAGE 1: Build Spring Boot application
# ==============================================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Download dependencies first for better caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and compile
COPY src ./src

# Package the Spring Boot executable JAR
RUN mvn clean package -DskipTests -B && \
    find target -maxdepth 1 -name "*.jar" \
      ! -name "*.jar.original" \
      ! -name "*-sources.jar" \
      ! -name "*-javadoc.jar" \
      -exec cp {} /build/app.jar \;

# ==============================================
# STAGE 2: Lightweight Java 17 Runtime
# ==============================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Create non-root user and data directory
RUN groupadd --system --gid 10001 numerology \
    && useradd --system --uid 10001 \
       --gid numerology \
       --home-dir /app \
       --shell /usr/sbin/nologin numerology \
    && mkdir -p /app/data \
    && chown -R numerology:numerology /app

# Copy built JAR
COPY --from=build --chown=numerology:numerology \
    /build/app.jar /app/name-suggestion.jar

# Copy startup script
COPY --chown=numerology:numerology \
    docker-entrypoint.sh /app/docker-entrypoint.sh

RUN chmod 755 /app/docker-entrypoint.sh

# Render configuration
ENV BIND_ADDRESS=0.0.0.0 \
    PORT=8080 \
    APP_DATA=/app/data \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -XX:+UseSerialGC"

EXPOSE 8080

USER numerology

ENTRYPOINT ["/app/docker-entrypoint.sh"]
