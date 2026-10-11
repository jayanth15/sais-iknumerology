#!/bin/sh
set -eu

if [ -z "${SARVAM_API_KEY:-}" ]; then
    printf '%s\n' 'ERROR: Missing Sarvam API key.' 'Pass it when starting the container with: docker run -e SARVAM_API_KEY ...' >&2
    exit 1
fi

# Forward optional Spring Boot arguments while keeping the API key out of the command line.
exec java -jar /app/name-suggestion.jar "$@"
