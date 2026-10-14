package com.namesuggestion;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class NakshatraPadaLettersTests {
    private final RaasiCalculatorService calculator = new RaasiCalculatorService();

    @Test void all27StarsAnd108PadasLoadedFromWorkbook() {
        for (int star = 0; star < 27; star++) {
            var mapping = NakshatraPadaLetters.forStarIndex(star);
            assertEquals(star, mapping.starIndex());
            assertEquals(4, mapping.letters().size());
            for (int pada = 1; pada <= 4; pada++) {
                var letter = mapping.forPada(pada);
                assertEquals(pada, letter.pada());
                assertFalse(letter.tamil().isBlank());
                assertFalse(letter.english().isBlank());
            }
        }
        assertThrows(IllegalArgumentException.class, () -> NakshatraPadaLetters.forStarIndex(27));
        assertThrows(IllegalArgumentException.class, () -> NakshatraPadaLetters.forStarIndex(-1));
    }

    @Test void july1992ShowsAyilyamPada4AndOtherThree() {
        var result = calculator.calculate(LocalDate.of(1992, 7, 3), LocalTime.of(13,49), "Kanchipuram");
        assertEquals("Ayilyam", result.natchathiram());
        assertEquals(4, result.pada());
        assertEquals("Do", result.namingSyllable());
        assertEquals("டி", result.padaLetters().get(0).tamil());
        assertEquals("De/Di", result.padaLetters().get(0).english());
        assertEquals("டு", result.padaLetters().get(1).tamil());
        assertEquals("Du", result.padaLetters().get(1).english());
        assertEquals("டெ", result.padaLetters().get(2).tamil());
        assertEquals("Dae/De", result.padaLetters().get(2).english());
        assertEquals("டோ", result.padaLetters().get(3).tamil());
        assertEquals("Do", result.padaLetters().get(3).english());
        assertFalse(result.padaLettersUnverified());
    }

    @Test void june1998AnushamSecondPadaHasNiAndTamilNi() {
        var result = calculator.calculate(LocalDate.of(1998,6,9), LocalTime.of(1,10), "Kanchipuram");
        assertEquals("Anusham", result.natchathiram());
        assertEquals(2, result.pada());
        assertEquals("நி", result.padaLetters().get(1).tamil());
        assertEquals("Ni", result.padaLetters().get(1).english());
    }

    @Test void visakamThirdPadaKeepsEveryEnglishVariant() {
        var result = calculator.calculate(LocalDate.of(1961,4,4), LocalTime.of(10,17), "Chennai");
        assertEquals("Visakam", result.natchathiram());
        assertEquals(3, result.pada());
        assertEquals("தே", result.padaLetters().get(2).tamil());
        assertEquals("The/Thae/De/Dhe", result.namingSyllable());
    }

    @Test void workbookPoosamRowFlaggedUnverified() {
        var result = NakshatraPadaLetters.forStarIndex(7);
        assertEquals("Poosam", result.sourceName());
        assertTrue(result.isUnverified());
        assertEquals("O/Ho", result.forPada(3).english());
        assertEquals("ஹே", result.forPada(3).tamil());
    }
}
