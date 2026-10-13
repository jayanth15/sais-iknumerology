# Sarvam edition verification — 8 October 2026

- Maven verify passed: 23 tests, zero failures/errors/skips, Java 17.
- Controlled HTTP tests verify Sarvam POST URL, authentication header, JSON mode,
  model, fresh requests, batching/exclusions, deduplication, prefix/language/gender
  validation, missing key, malformed/truncated JSON, rate limits and auth failures.
- Complete name records are recovered from a truncated Sarvam batch; generation
  continues with smaller calls and preserves recovered names.
- Batch size is capped at 25; missing provider language/gender metadata no longer
  causes valid candidates to be discarded. Explicit mismatched labels are rejected.
- Default generation window is 15 minutes and 250 API calls; varied follow-up
  prompts and ten no-progress calls help the search continue beyond short batches.
- Completed batches survive provider errors without automatic retries.
- Candidate-count field defaults to 100, allows 1–500 and rejects values above 500 in server-side validation before Sarvam is called.
- Original number mapping/parity, surname spacing, filters, login/CSRF and exports
  pass application tests. The results screen offers User PDF and Customer PDF only;
  the User PDF retains all search details, while the Customer PDF contains only
  surname, gender and date of birth in its criteria section. Both include name results.
- Customer PDF layout test confirms the names table starts close to the three detail rows. A visual check also confirms vertical borders cover the first rows.
- Pyramid rows use letters only; tests verify the displayed name separator does not
  insert a zero into the calculation in either name order.
- Calculator PDF endpoint is tested for the on-screen card, letter-to-number pairs,
  final result and every pyramid row.
- Standalone executable JAR started. Real local HTTP smoke verified registration,
  login, Sarvam form and actionable missing-key response using an in-memory test
  account database. No existing account database was changed by this smoke test.
- No authenticated live Sarvam request was made; no user API key was provided.
  Model output quality, billing, account permissions and real quotas are unverified.
- AI language labels and meanings are not independently validated. A requested count
  is a ceiling, not a guaranteed count, and number filtering can reduce it considerably.

Source + executable JAR included. Java 17+ required. No key or account database is
included. Native Windows/macOS and AWS deployment were not tested.

## Candidate limit extension — 11 October 2026

- Source and bundled Java 17 JAR updated to allow up to 500 names before number filtering; default remains 100.
- Updated rendered form verified through authenticated HTTP: `min="1"`, `max="500"`, `value="100"`.
- Java 17 compiled-class smoke test: 1, 100, 500 accepted; 0, 501 rejected.
- Authenticated HTTP test: 501 rejected by server before the Sarvam call.
- Authenticated HTTP test with a deliberately absent API key: 500 accepted through the server and reaches the generation layer without making any external call.
- New date-picker JavaScript remains accessible. Customer PDF / Docker API-key injection preserved.
- No live Sarvam generation was performed. This update did not rerun the historical full Maven test suite.

## 500-name parallel optimization — 11 October 2026

- Java source compiled using `javac --release 17 -parameters` against the Spring
  Boot dependency set in the existing executable JAR (class file major version 61).
- A **simulated** Sarvam adapter that provides distinct named rows in 450 ms
  per request generated 500 candidates in 0.9 seconds over 13 parallel calls;
  repeat search returned 500 from H2 cache in 16 ms with no new simulated calls.
  These figures are mocked workload results, NOT live Sarvam performance.
- H2 schema adds a cache keyed by language/gender/prefix/normalized name;
  it stores only name and meaning pairs returned by the Sarvam parser, never
  automatically invented catalogue entries.
- A patched application JAR passed a startup smoke test; Spring initialized
  its H2 schema without errors. Java 17-targeted bytecode was verified.
- Existing customer PDF, date picker, form default of 100, maximum 500,
  authentication, Docker runtime API-key injection and user data volume retained.
- No live Sarvam request was possible without the user's key, and Docker is not
  installed in the test environment. Therefore achieving 500 names in 30
  seconds with a real Sarvam account was NOT established. Strict prefixes,
  duplicate model output, unavailable established names and account throttling
  can all lower the final count. The API search stops near the 28-second budget.

## Excel name source extension (2026-10-12)

- Java 17 target compilation passed (`javac --release 17 -parameters`).
- Parsed **both** `.xls` (HSSF) and `.xlsx` (XSSF), preserving supplied meanings.
- Parsed an `.xlsx` sheet with 620 unique prefix-matching candidates and capped the pre-filter list at **500**.
- Confirmed that the existing Java pyramid calculation and final-number filter apply equally to Excel records.
- Rejected invalid Name/Meaning headings and unsupported filename extensions.
- Booted the executable Spring Boot JAR **without** `SARVAM_API_KEY` on localhost.
- Logged in through the real Spring Security form with CSRF protection, uploaded an Excel file and received matching names and meanings.
- Exported and examined the branded Customer PDF; both phone numbers and the email remained present.
- Verified User PDF and Excel export endpoints also returned their original content types.
- The workbook is processed in-memory for the search only; it is not inserted into the Sarvam cache.
- Full `docker build` was not run because Docker is unavailable in this execution environment.

## Excel starting-letter UI and customer PDF email update (2026-10-12)

- The Starting Letters input is disabled and not required when Excel is selected; it is restored and required when switching to Sarvam AI.
- Server-side validation does not require a starting-letter prefix for Excel; the Excel processing path ignores even a stale/manually submitted prefix.
- Confirmed an Excel upload without any startLetter field works using the actual logged-in Spring Boot site, and names with different first letters are retained.
- Sarvam AI still requires starting letters and its prefix-based filtering remains unchanged.
- Extracted text from the generated customer PDF and confirmed `iksnumerology@gmail.com` appears while the previous email address is absent. Both phone numbers and the logo remain unchanged.
- Java classes compiled with Java 17 bytecode target; updated JAR booted successfully without a Sarvam key.
- Full Docker image was not built in this environment; Dockerfile and compose settings remain unchanged.
