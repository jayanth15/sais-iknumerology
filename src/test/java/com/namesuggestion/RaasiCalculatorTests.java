package com.namesuggestion;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.api.Test;

class RaasiCalculatorTests {
    private final RaasiCalculatorService calc = new RaasiCalculatorService();

    @Test void july1992AyilyamFourthPada() {
        var r = calc.calculate(LocalDate.of(1992,7,3), LocalTime.of(13,49), "Kanchipuram");
        assertEquals("Kadagam", r.raasi());
        assertEquals("Ayilyam", r.natchathiram());
        assertEquals(4, r.pada());
        assertEquals(4, r.vithiEn());
        assertEquals(3, r.mathiEn());
        assertEquals("0 + 3 + 0 + 7 + 1 + 9 + 9 + 2 = 31 → 3 + 1 = 4", r.vithiCalculation());
        assertEquals("0 + 3 = 3", r.mathiCalculation());
        assertEquals("Do", r.namingSyllable());
        assertEquals(117.423485, r.moonLongitudeDegrees(), 0.02); // independent Swiss Lahiri reference
    }

    @Test void june1998AnushamSecondPada() {
        var r = calc.calculate(LocalDate.of(1998,6,9), LocalTime.of(1,10), "Kanchipuram");
        assertEquals("Viruchigam", r.raasi());
        assertEquals("Anusham", r.natchathiram());
        assertEquals(2, r.pada());
        assertEquals(6, r.vithiEn()); // 0+9+0+6+1+9+9+8 = 42 -> 6
        assertEquals(9, r.mathiEn());
        assertEquals("Ni", r.namingSyllable());
        assertEquals(218.273362, r.moonLongitudeDegrees(), 0.02);
    }

    @Test void april1961VisakamThirdPadaNearBoundary() {
        var r = calc.calculate(LocalDate.of(1961,4,4), LocalTime.of(10,17), "Chennai");
        assertEquals("Thulam", r.raasi());
        assertEquals("Visakam", r.natchathiram());
        assertEquals(3, r.pada());
        assertEquals(7, r.vithiEn()); // 0+4+0+4+1+9+6+1 = 25 -> 7
        assertEquals(4, r.mathiEn());
        assertTrue(r.nearPadaBoundary());
        assertEquals(206.680191, r.moonLongitudeDegrees(), 0.02);
    }

    @Test void invalidInputRejected() {
        assertThrows(IllegalArgumentException.class, () -> calc.calculate(LocalDate.of(1910,1,1), LocalTime.NOON,"Chennai"));
        assertThrows(IllegalArgumentException.class, () -> calc.calculate(LocalDate.of(1998,6,9), LocalTime.NOON,"  "));
        assertThrows(IllegalArgumentException.class, () -> calc.calculate(LocalDate.of(1998,6,9), LocalTime.NOON,"<script>"));
    }

    @Test void sameTimeSameTimezoneGivesSameMoonSign() {
        var a=calc.calculate(LocalDate.of(1998,6,9), LocalTime.of(1,10),"Chennai");
        var b=calc.calculate(LocalDate.of(1998,6,9), LocalTime.of(1,10),"Kanchipuram");
        assertEquals(a.raasi(), b.raasi());
        assertEquals(a.natchathiram(), b.natchathiram());
        assertEquals(a.pada(), b.pada());
        assertEquals(a.moonLongitudeDegrees(), b.moonLongitudeDegrees());
        assertEquals(a.vithiEn(), b.vithiEn());
        assertEquals(a.mathiEn(), b.mathiEn());
    }

    @Test void doubleDigitDayIsReducedToSingleDigit() {
        var numbers = BirthDateNumerology.calculate(LocalDate.of(1992, 7, 29));
        assertEquals(2, numbers.mathiEn());
        assertEquals("2 + 9 = 11 → 1 + 1 = 2", numbers.mathiCalculation());
        assertEquals(3, numbers.vithiEn()); // 2+9+0+7+1+9+9+2 = 39 -> 12 -> 3
        assertEquals("2 + 9 + 0 + 7 + 1 + 9 + 9 + 2 = 39 → 3 + 9 = 12 → 1 + 2 = 3", numbers.vithiCalculation());
    }

    @Test void dateOnlyNumbersIndependentOfTimeAndPlace() {
        var a = calc.calculate(LocalDate.of(1992, 7, 3), LocalTime.MIDNIGHT, "Chennai");
        var b = calc.calculate(LocalDate.of(1992, 7, 3), LocalTime.NOON, "Kanchipuram");
        assertEquals(a.vithiEn(), b.vithiEn());
        assertEquals(a.mathiEn(), b.mathiEn());
        assertEquals(a.vithiCalculation(), b.vithiCalculation());
    }
}
