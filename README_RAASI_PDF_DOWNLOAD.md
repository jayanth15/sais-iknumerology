# Raasi / Natchathiram downloadable PDF

## How it works

1. Log in and open `/raasi`.
2. Enter the date, birth time (including AM/PM) and Indian birthplace.
3. Click **Generate details**.
4. Click the **Download PDF** button above the result cards.

The download calls `POST /raasi/download` with the same date/time/place and the normal Spring Security CSRF token, recalculates the result server-side and sends `application/pdf` with a `Content-Disposition: attachment` header.

The one-page A4 report includes DOB in **dd/MM/yyyy**, birth time in **hh:mm AM/PM IST**, birthplace, Raasi, Natchathiram, Pada, Vithi En, Mathi En, Friendly En, the full nine-row friendly-number mapping with the user's row selected, and the four pada letter mappings (English and correctly rendered Tamil) with the user's pada highlighted. If Poosam's letter mapping is used, the report contains the source workbook's UNVERIFIED warning; near pada boundaries also receive a warning.

Tamil strings are supplied as pre-rendered transparent PNG labels under `src/main/resources/pdf/tamil/`. No font binaries are distributed, and the result does not depend on Render having a Tamil system font. The text portion is searchable in English; the Tamil letters are image-based.

All previous suggestions/customer PDF logic, Sarvam calls and the Raasi calculation service are unchanged. `RaasiPdfServiceTests` checks the actual PDF with PDFBox when running `mvn test`.

### Local checks

Run `mvn test` to run the existing suite and the new PDF tests. To build/deploy on Render, commit all changed Java, HTML and CSS files plus the `src/main/resources/pdf/tamil/` PNGs, then redeploy the service normally. The PDF button only appears after a successful generation. No extra environment variables or API credits are required.

## Customer PDF styling update

The Raasi PDF now reuses the exact Customer PDF letterhead/logo and contact section.
It adopts the Customer PDF's centered teal report heading, aligned bold-label rows,
gray table headers, and black grid borders. Selected Vithi and Pada rows remain green.
See `README_RAASI_CUSTOMER_STYLE.md` for details.
