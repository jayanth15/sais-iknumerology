# IK's Numerology: Raasi, Natchathiram and Pada Calculator

This update is based on `IKs_Numerology_Render_429_Fix_Java17.zip` and retains its existing
Spring Boot 3.5.6 / Java 17 / Maven multi-stage Docker build, authentication, Sarvam generation,
name calculations, and PDF/Excel exports.

## New screen

After signing in, open `/raasi` from the navigation menu. Enter:

- **Date of birth:** Responsive calendar with direct month/year navigation, or type DD/MM/YYYY; valid from 1950 to the present. Submitted to Spring as YYYY-MM-DD.
- **Time of birth:** Large hour/minute dropdowns and AM/PM segmented control; minute precision, submitted to Spring as 24-hour HH:mm and interpreted as **India Standard Time**. Both fields have native fallbacks if JavaScript is unavailable.
- **Place of birth:** free-form Indian city/town, e.g. Kanchipuram or Chennai.

Click **Generate birth star** to see raasi/rasi (Tamil + English),
nakshatra/natchathiram (Tamil + English), pada/quarter, traditional name syllable,
lords, and calculated Moon and ayanamsa longitude. Close-to-boundary results
include a prominently displayed verification warning.

### New files

- `src/main/java/com/namesuggestion/RaasiCalculatorService.java`
- `src/main/java/com/namesuggestion/RaasiController.java`
- `src/main/resources/templates/raasi.html` — accessible upgraded date and time selectors
- `src/main/resources/static/birth-time-picker.js` — 12-hour selection to backend 24-hour time, with progressive enhancement
- `src/test/java/com/namesuggestion/RaasiCalculatorTests.java`

### Existing files modified

- `src/main/resources/templates/fragments.html` — adds nav link
- `src/main/resources/static/style.css` — responsive page styles
- `src/main/resources/static/date-picker.js` — supports the Raasi screen’s minimum year (1950) alongside the original name-search screen

## Method, precision, and limitations

Raasi and Nakshatra are determined from the **geocentric sidereal longitude of
Moon**; each of 12 raasis occupies 30°, each of the 27 nakshatras occupies
13°20', each pada occupies 3°20'. This is deterministic Java code. It does
**not** request birth details from Sarvam AI. Sarvam is an LLM, not a lunar
ephemeris, so using it for exact lunar positions would be unsafe.

The astronomical model uses Jean Meeus, *Astronomical Algorithms*, Chapter 47:
60 main geocentric lunar longitude periodic terms, mean Lahiri/Chitrapaksha
ayanamsa adjusted to sidereal ephemeris reference, and an approximate historical
Delta T. It is **not** the full Swiss or JPL ephemeris and can disagree within
some arcminutes or around a precise boundary. It emits a caution when the
computed longitude falls within 0.1° of a pada boundary. For any result near
a boundary, confirm the birth time and a professional ephemeris. Vakya
Panchangam and other ayanamsa conventions can differ.

**India only:** All input times are interpreted using Asia/Kolkata (UTC+05:30).
No third-party geocoder is used, and place is included for the report; a
geocentric Moon sign depends on the instant rather than the city within IST.
**Do not use** for non-Indian places/timezones. Dates before 1950 are deliberately
not supported because historical civil time-offset rules can vary.

## API

The authenticated endpoint `POST /api/raasi/calculate` consumes JSON:

```json
{"dateOfBirth":"1998-06-09","timeOfBirth":"01:10","placeOfBirth":"Kanchipuram"}
```

Returns a JSON `BirthResult` or HTTP 400 with `{ "error": "..." }`.
Session authentication and existing Spring Security CSRF rules apply to this
POST API. Browser HTML form `/raasi` automatically uses the existing CSRF setup.

## Verification examples

| Birth date | Birth time IST | Place | Raasi | Nakshatra | Pada |
|---|---|---|---|---|---|
| 1992-07-03 | 13:49 | Kanchipuram | Kadagam | Ayilyam | 4 |
| 1998-06-09 | 01:10 | Kanchipuram | Viruchigam | Anusham | 2 |
| 1961-04-04 | 10:17 | Chennai | Thulam | Visakam | 3 (boundary warning) |

The three examples were independently compared against a Python Swiss
Ephemeris Moshier geocentric Moon / Lahiri sidereal calculation. The Java
analytical longitude differences were below 0.002° for those samples. A
400-case regression spot check (1950–2025) showed **no differing pada** and
maximum Moon longitude difference **0.00253°** (~9 arcseconds) versus this
reference. This check is not proof of correctness for *every* possible birth
instant, especially exact transitions.

## Build and deploy

```bash
mvn test
mvn -s maven-settings.xml -DskipTests package
# or use the unchanged multi-stage Dockerfile
```

The Dockerfile builds `target/name-suggestion.jar` automatically. No prebuilt
JAR or new environmental variables are required. Keep `SARVAM_API_KEY` in Render
Environment; do not write the secret in the source. This update **does not
modify the live Render service**. To deploy, update your repo
`https://github.com/jayanth15/sais-iknumerology` and push to its `main` branch,
or merge the supplied `.patch` into your branch first.

No full Maven/Docker build was run in this isolated file-editing environment
because Maven and outbound dependency access were unavailable. The new
calculation service was compiled using `javac` with a minimal annotation stub
and exercised against the examples. Run the application's real Maven tests
on your PC/CI prior to deployment.

## Added: விதி எண் (Vithi En) and மதி எண் (Mathi En)

The `/raasi` result page includes two extra numerology cards beneath the
existing Raasi/Natchathiram/Pada cards. Both are computed in Java from the
**date of birth alone**; neither uses Sarvam or changes the astrological
lunar calculation.

- **Vithi En / விதி எண்:** add all eight DDMMYYYY digits, repeatedly sum the
  digits of any multi-digit total to reach a single digit (1–9).
- **Mathi En / மதி எண்:** add only the two DD digits and reduce to one digit.

Example for **03/07/1992**:

- Vithi: `0 + 3 + 0 + 7 + 1 + 9 + 9 + 2 = 31 → 3 + 1 = 4`
- Mathi: `0 + 3 = 3`

The results also include the full calculation steps. The existing authenticated
`POST /api/raasi/calculate` JSON response now contains `vithiEn`, `mathiEn`,
`vithiCalculation`, and `mathiCalculation` in addition to the old fields.
The inputs and endpoints are unchanged. Java tests cover these examples and
multi-digit reductions (e.g. 29 -> 2).

## Added: Vithi En → Friendly En reference table

The result page displays this table **directly below the Vithi En and Mathi En
cards**, and highlights the row matching the calculated Vithi En. This mapping
is provided by the user, rather than inferred from astronomical or numerological
libraries. The birth-date calculator and API remain unchanged.

| Vithi En | Friendly En |
|---|---|
| 1 | 4 |
| 2 | 7 |
| 3 | 9 |
| 4 | 1 |
| 5 | 9 |
| 6 | 9 |
| 7 | 2 |
| 8 | 5,6 |
| 9 | |

The empty Friendly En cell for Vithi En 9 is intentional. When a result has
Vithi En 9, the 9 row is still highlighted and the Friendly En cell stays empty.


## Nakshatra / Pada naming letters (user-provided mapping)

See `README_NAKSHATRA_PADA.md` for the four-pada table, selection highlighting, and the unverified Poosam row caveat.
