# Nakshatra Pada naming letters — IK's Numerology

## What changed

The `/raasi` result screen now shows a **Nakshatra Pada Naming Letters** section
immediately **under Vithi En / Friendly En**, based on the user-supplied Excel file
`Nakshatra_Strict_Pada_English_Tamil_Mapping.xlsx Edited.xlsx`.

For the calculated nakshatra, a four-row table shows the **Tamil and English
starting letters for padas 1–4**. The calculated pada is bold/highlighted and
marked **✓ Your Pada**. A result card repeats the selected Tamil and English
letters. No new form input is required; selection is derived from the existing
birth-date/time calculation. Existing Raasi, numerology, and Sarvam features
remain unchanged.

The application carries all **27 nakshatra × 4 pada = 108 mappings** as a small
UTF-8 TSV resource bundled into the executable Spring Boot JAR:

`src/main/resources/nakshatra/strict_pada_letters.tsv`

Read-only loader: `NakshatraPadaLetters.java`. Only the letters from the supplied
workbook are used, including all `A/B` transliteration alternatives; they are
**not** silently substituted with a generic online phonetic chart. The existing
`namingSyllable` JSON result is now the **English letters from this workbook**
for the selected pada; new JSON result fields include `padaLetters`,
`padaLetterSourceStatus`, and `padaLettersUnverified`.

**Important source-data issue:** The Excel workbook marks **Poosam** as
`UNVERIFIED`; its notes say the original Tamil image did not provide a confirmed
alignment for all four padas. The app deliberately reproduces the workbook
cells unchanged and shows an explicit warning for that nakshatra. The mapping
is a traditional naming reference, not a guarantee that the phonetic spelling
or birth-time astronomical position has been professionally verified.

## Sample

DOB `03/07/1992`, time `13:49`, place `Kanchipuram`:

- Ayilyam (ஆயில்யம்), **Pada 4: டோ / Do** (highlighted)
- Pada 1: டி / De/Di
- Pada 2: டு / Du
- Pada 3: டெ / Dae/De
- Pada 4: டோ / Do

## Build and deployment

Use the same Java 17, Maven, Docker and Render instructions as in
`README_RENDER.md`. The file is read from the classpath, so no Excel upload,
Sarvam key, extra API request, or separate runtime data file is required.
Run `mvn test` before deploying when Maven is available.
