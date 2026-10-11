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
