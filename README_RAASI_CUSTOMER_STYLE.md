# Raasi / Natchathiram PDF — Customer PDF layout

The **Download PDF** action at `/raasi` now produces a one-page A4 report styled to match the name-suggestions **Customer PDF**:

- Reuses the identical IK's Numerology logo, right-side contact block, gold rule, and colors via `CustomerPdfBranding.java`.
- Uses the Customer PDF's centered teal heading and aligned label/value rows (label x=78, value x=211).
- Uses the same grey table headings and black grid borders, with selected Vithi En and Pada rows additionally highlighted in green.
- Includes DOB in `dd/MM/yyyy`, birth time in `hh:mm AM/PM IST`, birthplace, raasi, natchathiram, pada, Vithi, Mathi, Friendly En, calculations, and all four English/Tamil naming-letter rows.
- Maintains warning text when the mapping is unverified or birth time is close to a pada boundary.
- Uses packaged pre-rendered Tamil label images (no end-user font requirements).
- Does not change PDF calculations, on-screen fields, or Sarvam usage.

## Changes
- `src/main/java/com/namesuggestion/CustomerPdfBranding.java`: extracted existing Customer PDF letterhead as reusable layout code.
- `src/main/java/com/namesuggestion/ExportService.java`: uses shared header without changing Customer PDF contents.
- `src/main/java/com/namesuggestion/RaasiPdfService.java`: uses the same header, metadata style, and bordered tables.

## Validate

```bash
mvn -q test
mvn -q -DskipTests package
```

Render deployment uses the existing Dockerfile and requires no additional secrets.
