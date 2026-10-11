# IK's Numerology — Docker (Java 17)

Includes the application executable JAR, the modern date picker and the branded customer PDF.

**Requirements:** Docker Desktop / Docker Engine, and a valid **Sarvam AI API key**. Docker downloads an Eclipse Temurin **Java 17 JRE** image the first time you build. No JDK or Maven installation is required on your PC.

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

Note: `docker compose up` requires `SARVAM_API_KEY` in your local environment. It deliberately refuses to start if missing.

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
