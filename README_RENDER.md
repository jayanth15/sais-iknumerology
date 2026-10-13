# Render 429 fix — IK's Numerology

**Observed deployment failure:** Maven Central returned `HTTP 429 Too Many Requests` while resolving `org.springframework.boot:spring-boot-starter-parent:3.5.6`, at the `dependency:go-offline` step.

**Fix:** use `maven-settings.xml` with the Google Cloud Asia Maven Central mirror and build once using `mvn -s /build/maven-settings.xml -B -ntp -U -DskipTests package`. Nothing in the application logic or POM versions is changed.

## Minimal update to your existing GitHub repository

Copy just these **two files** from this package to your repository root, replacing the existing Dockerfile:

- `Dockerfile`
- `maven-settings.xml` (new file)

Then push:

```powershell
git add Dockerfile maven-settings.xml
git commit -m "Fix Maven Central 429 on Render Docker build"
git push origin main
```

Render auto-deploys the new commit. Dockerfile builds the JAR during deployment; no prebuilt JAR is needed. The runtime still uses Java 17; the API key is injected as the `SARVAM_API_KEY` runtime environment variable in Render.

If Google mirror itself is inaccessible from your infrastructure, replace its URL in `maven-settings.xml` with your company's Maven repository manager/cache URL and retry.

**Validation performed:** Maven settings XML parsed successfully, required Docker COPY paths exist, and ZIP includes full Java source. **Not performed:** live Docker build, Maven dependency download or a new Render deploy.
