# Runtime Java version matches pom.xml (Java 17).
# Build from this directory: docker build -t iks-numerology:java17 .
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Run as an unprivileged account. /app/data stores the embedded H2 database.
RUN groupadd --system --gid 10001 numerology \
    && useradd --system --uid 10001 --gid numerology --home-dir /app --shell /usr/sbin/nologin numerology \
    && mkdir -p /app/data \
    && chown -R numerology:numerology /app

COPY --chown=numerology:numerology name-suggestion.jar /app/name-suggestion.jar
COPY --chown=numerology:numerology docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod 755 /app/docker-entrypoint.sh

# Spring Boot defaults to 127.0.0.1 outside Docker: bind all container interfaces.
# API KEY intentionally NOT included in the image. Supply SARVAM_API_KEY at startup when using Sarvam AI.
# Excel-only mode starts without a key.
ENV BIND_ADDRESS=0.0.0.0 \
    PORT=8080 \
    APP_DATA=/app/data

EXPOSE 8080
VOLUME ["/app/data"]
USER numerology

ENTRYPOINT ["/app/docker-entrypoint.sh"]
