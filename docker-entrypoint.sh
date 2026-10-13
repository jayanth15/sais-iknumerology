#!/bin/sh
set -eu

if [ -z "${SARVAM_API_KEY:-}" ]; then
    printf '%s\n' 'INFO: SARVAM_API_KEY is not set. Excel upload mode is available; Sarvam AI requires the key at startup.' >&2
fi

# Forward optional Spring Boot arguments while keeping the API key out of the command line.
exec java -jar /app/name-suggestion.jar "$@"
