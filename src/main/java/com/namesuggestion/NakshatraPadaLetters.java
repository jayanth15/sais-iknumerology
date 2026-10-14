package com.namesuggestion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Nakshatra/pada naming letters transcribed without normalization from the user's
 * "Nakshatra_Strict_Pada_English_Tamil_Mapping.xlsx Edited.xlsx" workbook.
 *
 * Row number maps to the same 27-star order used by RaasiCalculatorService;
 * transliteration variants (e.g. "The/Thae/De/Dhe") are deliberately preserved.
 * One workbook row (Poosam) is explicitly UNVERIFIED, so don't present it as
 * an astronomically verified phonetic mapping.
 */
public final class NakshatraPadaLetters {
    private static final String RESOURCE = "/nakshatra/strict_pada_letters.tsv";

    public record PadaLetter(int pada, String english, String tamil) { }
    public record Mapping(int starIndex, String sourceName, List<PadaLetter> letters,
                          String sourceStatus) {
        public Mapping {
            letters = List.copyOf(letters);
            if (letters.size() != 4) throw new IllegalArgumentException("Each nakshatra requires four padas");
        }
        public PadaLetter forPada(int pada) {
            if (pada < 1 || pada > 4) throw new IllegalArgumentException("Pada must be between 1 and 4");
            return letters.get(pada - 1);
        }
        public boolean isUnverified() { return "UNVERIFIED".equalsIgnoreCase(sourceStatus); }
    }

    private static final List<Mapping> MAPPINGS = load();

    private NakshatraPadaLetters() { }

    /** Zero-based index consistent with STAR_EN and STAR_TA in the calculator. */
    public static Mapping forStarIndex(int starIndex) {
        if (starIndex < 0 || starIndex >= MAPPINGS.size())
            throw new IllegalArgumentException("Nakshatra index outside 0-26: " + starIndex);
        return MAPPINGS.get(starIndex);
    }

    private static List<Mapping> load() {
        try (InputStream stream = NakshatraPadaLetters.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing packaged nakshatra mapping: " + RESOURCE);
            List<Mapping> values = new ArrayList<>(27);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] cells = line.split("\t", -1);
                    if (cells.length != 11) throw new IllegalStateException("Expected 11 fields in nakshatra mapping row");
                    int oneBased = Integer.parseInt(cells[0]);
                    if (oneBased != values.size() + 1) throw new IllegalStateException("Nakshatra mapping out of order: " + oneBased);
                    List<PadaLetter> four = new ArrayList<>(4);
                    for (int pada = 1; pada <= 4; pada++) {
                        int english = pada * 2;
                        int tamil = english + 1;
                        if (cells[english].isBlank() || cells[tamil].isBlank())
                            throw new IllegalStateException("Empty naming-letter cell for nakshatra " + oneBased + " pada " + pada);
                        four.add(new PadaLetter(pada, cells[english], cells[tamil]));
                    }
                    values.add(new Mapping(oneBased - 1, cells[1], four, cells[10]));
                }
            }
            if (values.size() != 27) throw new IllegalStateException("Expected 27 nakshatra rows; got " + values.size());
            return List.copyOf(values);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load supplied nakshatra mapping", ex);
        }
    }
}
