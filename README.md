# Name Garden — Sarvam AI edition

Java 17+, Spring Boot and Thymeleaf. Sarvam replaces Wikidata entirely.
All suggestions come from Sarvam API completions. The application now persists
Sarvam-provided names and meanings in the existing H2 database and reuses them
for later searches with the same language, gender and prefix. No local prefilled
name catalogue or fabricated fallback names are used.

## Run on Windows (PowerShell)

Extract this ZIP, open PowerShell in the extracted name-garden folder, then:

```powershell
$env:SARVAM_API_KEY="YOUR_SARVAM_KEY"
java -jar name-suggestion.jar
```

Open http://localhost:8080 and create a local app account. Set the key on your
own computer; do not put it in source control or share it in chat.

Linux/macOS:

```sh
export SARVAM_API_KEY='YOUR_SARVAM_KEY'
java -jar name-suggestion.jar
```

IntelliJ: set SARVAM_API_KEY in Run Configuration > Environment variables.
AWS: inject the same environment variable at runtime, preferably from your secret
manager. The JAR contains no API key. An internet connection and an enabled Sarvam
chat-completion account with credits/quota are needed. Existing local H2 accounts
remain compatible. Keep your existing data folder when upgrading.

## IK's Numerology branding (customer PDF only)

The **Customer PDF** now embeds the IK's Numerology teal-and-gold logo in
its header, together with **+91 9944925273**, **+91 9944769370** and
**iksaikiran.kiran@gmail.com**. The branded header is repeated on every
customer PDF page, with a repeated column header on continuation pages.
The surname, gender, date-of-birth fields and names/meanings table remain.
The User PDF, XLSX and calculator PDF are unchanged. The bundled
`src/main/resources/pdf/iks-numerology-logo.png` is loaded directly
from the JAR - no external file or internet access is needed for PDF
branding. A sample customer PDF was rendered and visually reviewed.

## Date of birth picker

The Suggestions page includes a new, mobile-friendly, dependency-free calendar.
Choose a **month and year directly**, then select the day, or type
**DD/MM/YYYY** (including pasting 8 digits). The picker prevents future dates
and automatically submits the original **yyyy-MM-dd** value expected by the
Spring Boot application and customer PDF. Without JavaScript, the browser's
native date input remains available. No third-party CDN is needed.

## Generation and cost controls

- Uses POST https://api.sarvam.ai/v1/chat/completions with api-subscription-key.
- Default model: sarvam-105b. Sarvam-M is deprecated according to current docs.
- Prompts for established names genuinely used in the selected language community,
  selected gender, conventional English spellings and concise meanings.
- Validates any returned language/gender labels when present, ASCII spelling and
  prefix; deduplicates case-insensitively. Labels and meanings remain AI claims,
  not independently verified facts. The app asks the model not to invent names.
- The form requests 1–500 candidates BEFORE the pyramid-number filter. Default
  is 100. Requests above 500 are rejected by server-side validation.
- For searches of 100–500 candidates, up to 14 Sarvam requests run concurrently,
  each requesting up to 50 names. Independent prompts encourage different groups
  of established names, and responses are deduplicated case-insensitively.
- The default generation budget is 28 seconds (configured to return the request
  in approximately 30 seconds including server overhead); HTTP connection and
  read timeouts are 3 and 24 seconds respectively. The time budget and call count
  are hard stops, not guarantees of 500 matching names.
- All completed, valid Sarvam names and meanings are saved to H2, even when an
  individual response is truncated or more names than the current target arrive.
  The same language/gender/prefix can reuse saved names instantly. A shorter or
  different prefix may still need fresh API requests.
- An initial short search or cache miss can return fewer names on strict prefixes,
  account rate limits, API failures or duplicate responses. No names are invented
  to reach 500. Retrying can supplement the saved Sarvam names with fresh batches.
- One generation runs at a time per application instance to limit accidental
  overlapping calls. A second request gets a visible busy message.
- Only language, gender, prefix, batch size and names already generated during that
  search enter the prompt. Surname and DOB stay local. Sarvam does NOT calculate
  numerology; Java applies the original pyramid logic after generation.
- AI suggestions are labelled in UI and exported meanings. No fake source links.

Start with the default 100 candidates to check your key and output quality.
You can increase the request up to 500, but larger generations may take longer
and incur additional Sarvam API charges. The price
charged is controlled by Sarvam and your account plan; this application is no
longer key-free or guaranteed free. Cached repeats do not generate more API calls when the saved Sarvam results cover
the requested count. A cache miss may make paid requests. For reverse proxies, set
the request timeout above 30 seconds. Performance depends on actual Sarvam latency
and account rate limits; a cold 500-name request is not guaranteed in 30 seconds.

## Configuration

| Environment variable | Default | Purpose |
|---|---|---|
| SARVAM_API_KEY | empty | Server-side subscription key; blank produces a setup message |
| SARVAM_MODEL | sarvam-105b | V1-compatible chat model |
| SARVAM_MAX_BATCHES | 36 | Call limit per search, clamped to 1–100 |
| SARVAM_MAX_SECONDS | 28 | API generation time budget, clamped to 1–30 seconds |
| SARVAM_PARALLELISM | 14 | Concurrent Sarvam requests for large searches, clamped to 1–16 |
| PORT | 8080 | HTTP port |
| BIND_ADDRESS | 127.0.0.1 | Bind address; use 0.0.0.0 when hosting |
| APP_DATA | ./data | H2 account database directory |

All ten original language choices remain. Quality/coverage varies by language;
English spellings allow the existing A–Z calculation and portable PDF font.
Neutral means unisex. Native-script rendering/transliteration is not added.

## Existing functionality

Surname order, one-space separation, date-of-birth report field, 1–9 number filter,
standalone calculator, per-calculation pyramid PDF with each letter's mapped value,
local login, CSRF protection, PDF layout and XLSX export remain. PDF pagination
adapts to wrapped meanings. DOB does not affect calculation.

Mapping: 1=AIJQY, 2=BKR, 3=CGLS, 4=DMT, 5=EHNX, 6=UVW, 7=OZ, 8=FP.
Only letters contribute values; spaces and punctuation are ignored in the pyramid.
Adjacent letter values are added, sums 10–18 reduced by adding their digits, and
the process repeats until one value remains. The displayed given name and surname
still retain their separating space. This is not a simple digit-root sum.

## Build / portability

```sh
mvn clean verify
java -jar target/name-suggestion.jar
```

JDK 17+ and Maven are needed to build, with internet for initial dependencies.
The supplied fat JAR needs only Java 17+ to run on Windows/Linux/macOS. IntelliJ
must import pom.xml and use language level 17; do not use Java source 1.5.

Authenticated JSON API: POST /api/suggestions uses the existing fields plus
candidateCount (default 100; maximum 500). POST /api/calculate/final-number is unchanged.
Browser results remain in the session for PDF export. The results screen offers
User PDF (existing full details) and Customer PDF (surname, gender and date of
birth details); both include the generated names and meanings.

No authenticated live Sarvam call was made during development because no key was
provided. Mocked provider contract/error/batch tests cover the integration. Check
your account's model access with a small real search after setting the key.

Official references:
https://docs.sarvam.ai/api-reference/chat/chat-completions-v1
https://docs.sarvam.ai/api-reference/authentication

## Docker (Java 17, automatic startup)

A Dockerfile, Docker Compose configuration, startup script and detailed Windows/Linux instructions are included. **See [DOCKER_README.md](DOCKER_README.md)**. Pass your Sarvam key at runtime using `SARVAM_API_KEY` (never bake it into the image).

## Dokploy (Nixpacks build)

This repository builds as-is with Nixpacks (Java/Maven provider, JDK 17). The included `nixpacks.toml` pins the same runtime defaults as the Dockerfile (`BIND_ADDRESS=0.0.0.0`, `PORT=8080`, `APP_DATA=/app/data`) and starts `target/name-suggestion.jar`.

In Dokploy:

1. Set the application **Build Type** to **Nixpacks** (no base directory; the repo root is the app root).
2. Add environment variable `SARVAM_API_KEY` (never commit the key).
3. Set the application/domain port to **8080**.
4. Mount a volume at **/app/data** to keep the H2 user database across redeploys.

