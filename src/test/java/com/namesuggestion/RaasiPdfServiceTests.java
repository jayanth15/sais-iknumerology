package com.namesuggestion;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

class RaasiPdfServiceTests {
    private final RaasiCalculatorService calculator = new RaasiCalculatorService();
    private final RaasiPdfService pdfService = new RaasiPdfService();

    @Test void pdfContainsBirthDetailsNumbersAndSelectedNamingLetters() throws Exception {
        var result = calculator.calculate(LocalDate.of(1992, 7, 3), LocalTime.of(13, 49), "Kanchipuram");
        byte[] pdf = pdfService.render(result);
        assertTrue(pdf.length > 4000);
        assertEquals("%PDF-", new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertEquals(1, document.getNumberOfPages());
            String extracted = new PDFTextStripper().getText(document);
            assertTrue(extracted.contains("Raasi / Natchathiram Report"));
            assertTrue(extracted.contains("CONTACT"));
            assertTrue(extracted.contains("iksnumerology@gmail.com"));
            assertTrue(extracted.contains("03/07/1992"));
            assertTrue(extracted.contains("01:49 PM IST"));
            assertTrue(extracted.contains("Kanchipuram"));
            assertTrue(extracted.contains("Kadagam"));
            assertTrue(extracted.contains("Ayilyam"));
            assertTrue(extracted.contains("VITHI EN"));
            assertTrue(extracted.contains("MATHI EN"));
            assertTrue(extracted.contains("FRIENDLY EN"));
            assertTrue(extracted.contains("SELECTED VITHI EN"));
            assertTrue(extracted.contains("YOUR PADA"));
            assertTrue(extracted.contains("De/Di"));
            assertTrue(extracted.contains("Du"));
            assertTrue(extracted.contains("Dae/De"));
            assertTrue(extracted.contains("Do"));
        }
    }

    @Test void warnsWhenNearPadaBoundary() throws Exception {
        var result = calculator.calculate(LocalDate.of(1961, 4, 4), LocalTime.of(10, 17), "Chennai");
        try (PDDocument document = Loader.loadPDF(pdfService.render(result))) {
            assertTrue(new PDFTextStripper().getText(document).contains("Boundary warning"));
        }
    }
}
