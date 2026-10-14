# Suggestions page: Birth Time + Customer PDF Raasi/Natchathiram

## New feature

- Suggestions page (`/`): birth-time selector with hour, minute, AM/PM and native `<input type="time">` fallback. Selects any minute. Birth date stays in its existing modern calendar.
- Input is interpreted in **IST (UTC+05:30)** for births in India; a birth outside India must not be entered as local time without conversion.
- Time of birth is now required on both Sarvam and Excel suggestion searches; submitted value uses `HH:mm` in 24-hour ISO time for backend accuracy.
- Customer PDF (`/export/customer-pdf`): new `Time of Birth`, `Raasi`, `Natchathiram` rows. Date `dd/MM/yyyy`, time `hh:mm a` with an `IST` suffix (e.g., `03/07/1992`, `01:49 PM IST`).
- Raasi & nakshatra values come from the same `RaasiCalculatorService` as `/raasi`; Sarvam does not calculate these fields and no additional Sarvam request is made. The calculation is based on geocentric Moon longitude with approximate Lahiri ayanamsa, and is best cross-checked with a high-precision ephemeris near boundaries.
- If a legacy DOB is outside the Raasi calculator's supported range (1950–today), the Customer PDF reports `Unavailable (outside supported date range)` instead of inventing a result. Regular name suggestions still work with earlier DOBs.
- User PDF, Excel export columns, Raasi page, numerology and name filters remain unchanged.

## Example customer PDF details

```text
Surname:          Saikiran
Gender:           Male
Date of Birth:    03/07/1992
Time of Birth:    01:49 PM IST
Raasi:            Kadagam
Natchathiram:     Ayilyam
```

## Deploy

Commit source updates and push to the GitHub branch connected to Render. The repository Dockerfile builds the application from source. After successful deployment, hard-refresh the Suggestions page so the browser loads the newly updated HTML/CSS/JS.

## Validation in this environment

Java 17+ `javac` smoke checks passed for SearchForm time validation and the three prior birth-date examples; AM/PM formatting checked for 13:49, 00:01 and 12:00. Full Maven/Spring Boot test suite, PDFBox rendering, and live Render deployment not executed here because Maven / required Spring Boot and PDFBox 3 dependencies are not installed in this environment.
