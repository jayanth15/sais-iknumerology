# IK's Numerology — Docker (Java 17)

Includes the executable JAR, modern date picker, branded customer PDF, Sarvam name generation and local Excel (.xls/.xlsx) upload mode.

**Requirements:** Docker Desktop / Docker Engine. A valid **Sarvam AI API key is optional** and is only needed for Sarvam mode. Docker downloads an Eclipse Temurin **Java 17 JRE** image the first time you build. No JDK or Maven installation is required on your PC.

## Windows PowerShell — fastest start

Open PowerShell **inside the `name-garden` folder** (the folder containing the `Dockerfile`).

```powershell
# Type your key when prompted. It is not written to the image.
$secret = Read-Host "Sarvam AI API key" -AsSecureString
$env:SARVAM_API_KEY = [System.Net.NetworkCredential]::new('', $secret).Password

# Build the Docker image with Java 17.
docker build -t iks-numerology:java17 .

# Start the web server and pass the key as a startup environment parameter.
docker run -d --name iks-numerology --restart unless-stopped -p 127.0.0.1:8080:8080 -v iks-numerology-data:/app/data -e SARVAM_API_KEY iks-numerology:java17

# The running container now has the key. Remove it from this terminal.
Remove-Item Env:SARVAM_API_KEY
```

Visit **http://localhost:8080** and register/sign in.

The `-e SARVAM_API_KEY` option forwards the environment variable from PowerShell to the container at startup. The key is not built into the Docker image or source code. For production, use a container secret manager instead of plain environment variables.

## macOS / Linux

```bash
cd name-garden
read -r -s -p 'Sarvam AI API key: ' SARVAM_API_KEY; echo
export SARVAM_API_KEY
docker build -t iks-numerology:java17 .
docker run -d --name iks-numerology --restart unless-stopped \
  -p 127.0.0.1:8080:8080 \
  -v iks-numerology-data:/app/data \
  -e SARVAM_API_KEY \
  iks-numerology:java17
unset SARVAM_API_KEY
```

## Docker Compose (alternative)

The included `compose.yaml` does the same thing:

```powershell
# Set SARVAM_API_KEY in this PowerShell session using the prompt above.
docker compose up --build -d
# After starting:
Remove-Item Env:SARVAM_API_KEY
```

Excel-only mode works even if `SARVAM_API_KEY` is unset. Docker Compose then starts without the key; Sarvam mode displays a setup message until the container is recreated with the key.

## Start without an API key (Excel-only mode)

```powershell
docker build -t iks-numerology:java17 .
docker run -d --name iks-numerology -p 127.0.0.1:8080:8080 -v iks-numerology-data:/app/data iks-numerology:java17
```

Or run `docker compose up -d --build` without defining `SARVAM_API_KEY`.
On the Suggestions page, select **Upload Excel**, choose your `.xls` or `.xlsx`
file (columns `Name`, `Meaning`), then use the usual filters and customer PDF.
The file is transferred to your local web server for processing; it is not
retained in the database, Docker image or persistent data volume. The server
may temporarily buffer multipart uploads. Use HTTPS if exposing the app remotely.

To enable Sarvam later, recreate the container with the key using the usual
Docker Run or Compose instructions above. Sarvam remains the default name source
in the app, but you can switch to Excel without an API key.

## Day-to-day commands

```bash
docker logs -f iks-numerology      # Follow server logs
docker stop iks-numerology         # Stop
docker start iks-numerology        # Restart with its original settings
docker rm -f iks-numerology        # Delete container (not its data volume)
```

**Persistent data:** User registration / H2 database files are written under `/app/data`, attached to the named volume `iks-numerology-data` (or a Compose-managed volume whose actual name is prefixed by the Compose project). Switching between Docker Run and Compose uses separate volumes unless explicitly configured. Do not delete the volume if you want to retain accounts.

**Default access:** The provided run examples map the site to `127.0.0.1:8080` on the host, so it is accessible only from the Docker host. Do not expose the login service to the public internet without a secure reverse proxy, HTTPS and an access/security review.

**Key changes:** To rotate the key for `docker run`, remove the container without deleting the data volume, then run the `docker run` command again with the new key. `docker start` retains the key passed when the original container was created. For Compose, set the new `SARVAM_API_KEY` and run `docker compose up -d --force-recreate`.

**Optional:** The entrypoint forwards arguments after the image name to Spring Boot, e.g. `--sarvam.max-batches=50`; do not pass the API key on the command line because it can show up in process listings.

## 500-name generation speed

The current build requests up to 50 names in parallel from Sarvam using 14
workers, with a default **28-second API time budget**. Valid responses (and
meanings) are written to `/app/data/names.mv.db` in the existing Docker
volume. The database stores **only actual Sarvam responses**: there is no
preloaded catalogue, no generated placeholders, and no guarantee a narrow
starting pattern contains 500 genuine names. Later identical searches can
load cached Sarvam data quickly without using further API calls.

Optional startup configuration:

```powershell
# Set these before docker compose up; the API key still comes separately.
$env:SARVAM_PARALLELISM = "14"
$env:SARVAM_MAX_SECONDS = "28"
$env:SARVAM_MAX_BATCHES = "36"
```

**Docker Compose:** The included compose.yaml passes these options if set.
For Docker Run, pass `-e SARVAM_PARALLELISM=14 -e SARVAM_MAX_SECONDS=28`.
Keep your existing named Docker volume during upgrades; cached names and
registered accounts will survive container replacement.

**Timing limitation:** The application will return partial results at its
configured deadline instead of fabricating 500 names. The 30-second target
is a response-budget design goal, not a Sarvam API service-level guarantee.
Actual provider speed/quota can prevent 500 unique names on the first request.
