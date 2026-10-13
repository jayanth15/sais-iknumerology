# Maven test failure correction (13 October 2026)

The application supports **1–500 candidate names**. Two unit tests contained outdated expectations.

- `duplicateOnlyBatchesStopAndExcludePreviousNames` requested 10,000: now requests 500, still validating duplicate suppression and 11 API calls.
- `authenticationFailureIsActionableAndDoesNotExposeResponse` expected `rejected the key`: now checks the actual actionable message `rejected the API key`, and still verifies the secret never appears in the message.
- `invalidFiltersDoNotCallProvider` now checks 501 is invalid, while accepting 500 (with a blank API key to avoid network calls).

**No production Java code was changed.** This ZIP includes the executable JAR previously bundled in the application; Maven will regenerate it after you build.

## Build and test on Windows PowerShell

From the folder containing `pom.xml` and `Dockerfile`:

```powershell
mvn clean test
mvn clean package
Test-Path .\target\name-suggestion.jar
Copy-Item .\target\name-suggestion.jar .\name-suggestion.jar -Force
Test-Path .\name-suggestion.jar
docker build -t iks-numerology:java17 .
```

For GitHub-connected Render deployment, commit the corrected test, latest JAR and Docker files from your repo root and push. Example:

```powershell
git add src/test/java/com/namesuggestion/OnlineNameServiceTests.java
git add Dockerfile .dockerignore docker-entrypoint.sh
git add -f name-suggestion.jar
git commit -m "Fix 500-name tests and update Docker JAR"
git push origin main
```

Your Render service should auto-deploy on a push to `main`; make sure the JAR is in Render's configured Docker context (`.`).

**Verification in this environment:** all production source compiled with `javac --release 17`; a dedicated local Java smoke harness passed the exact test behaviors. Maven/JUnit suite was NOT run here because Maven and external package download access are unavailable. Please run `mvn clean test` locally.
