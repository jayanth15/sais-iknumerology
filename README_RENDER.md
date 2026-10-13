# IK's Numerology — Render source-build deployment (Java 17)

This package uses a **multi-stage Dockerfile**. Render builds `target/name-suggestion.jar`
from `pom.xml` and `src/` using Maven, then copies the JAR into a Java 17 runtime
image. Do **not** upload or commit `name-suggestion.jar` to GitHub.

## GitHub repository layout

Put these files directly at the *root* of `jayanth15/sais-iknumerology`
(the same folder that contains the current Dockerfile):

```
Dockerfile
.dockerignore
docker-entrypoint.sh
pom.xml
src/main/java/...
src/main/resources/...
src/test/java/...
compose.yaml
```

If your existing repository already includes `pom.xml`, `src/`, and
`docker-entrypoint.sh`, replacing **Dockerfile** and **.dockerignore** is enough.
Otherwise copy the complete source package into the repository root.

## Render settings

- Runtime: Docker
- Branch: main
- Root Directory: blank (repository root)
- Dockerfile Path: `./Dockerfile`
- Docker Build Context: `.`
- Environment variable `SARVAM_API_KEY`: supply your real key via Render Dashboard
  **Environment** only if using Sarvam AI. Do not write it into the Dockerfile or GitHub.
- `PORT`: Render supplies its runtime PORT; the entrypoint and app configuration use it.
- Excel-only mode does not require a Sarvam key.

Commit and push the Dockerfile / source changes to GitHub. Since automatic
production deploys are enabled for this service, the push triggers the build.
Avoid manually redeploying an unchanged commit to fix a missing file.

## Verify locally (optional)

```
docker build --progress=plain -t iks-numerology:java17 .
```

You can also run Java tests independently:

```
mvn clean verify
```

The Docker build deliberately uses `-DskipTests` because Maven unit tests should
run in local development/CI and not prevent a release if they require a test
fixture. Production Java sources still compile during image build.

## Local Docker Compose

Windows PowerShell (Sarvam mode):

```
$env:SARVAM_API_KEY = Read-Host "SARVAM_API_KEY"
docker compose up -d --build
```

Excel-only mode:

```
docker compose up -d --build
```

The local Docker Compose project persists the H2 database to a named volume.
**Render:** the free service filesystem is ephemeral. Add persistent storage on
an eligible paid plan, or migrate H2 to a persistent database to retain user
accounts and cached names across replacement deployments.

## If a new error appears

- Missing `pom.xml` or `src/`: ensure these exist in the Docker build context.
- Maven compiler error: inspect the next failed build log for source errors.
- Startup error after successful Docker build: inspect application logs and
  Render environment variables.
