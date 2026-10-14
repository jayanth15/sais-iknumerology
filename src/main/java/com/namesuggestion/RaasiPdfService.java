package com.namesuggestion;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * One-page Raasi / Natchathiram report matching the Customer PDF letterhead,
 * typography, metadata rows and bordered-table format.
 * All information comes from the same BirthResult as the on-screen /raasi form.
 */
@Service
public class RaasiPdfService {
    private static final PDFont NORMAL = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final float W = PDRectangle.A4.getWidth();
    private static final float LEFT = 50f;
    private static final float RIGHT = 50f;
    private static final float TABLE_WIDTH = W - LEFT - RIGHT;
    private static final Color BRAND = new Color(20, 64, 69);    // Customer PDF heading teal
    private static final Color GOLD = new Color(191, 150, 77);
    private static final Color BODY = Color.BLACK;
    private static final Color HEADER_BG = new Color(191, 191, 191); // Customer name table grey
    private static final Color BORDER = new Color(70, 70, 70);
    private static final Color SELECTED_BG = new Color(227, 245, 232);
    private static final Color SELECTED_INK = new Color(16, 100, 58);
    private static final Color MUTED = new Color(94, 94, 94);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    public byte[] render(RaasiCalculatorService.BirthResult r) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDImageXObject logo = CustomerPdfBranding.loadLogo(document);
            try (PDPageContentStream out = new PDPageContentStream(document, page)) {
                // The exact shared logo, gold divider and right-side contacts as Customer PDF.
                CustomerPdfBranding.drawHeader(out, logo, true);
                centered(out, "Raasi / Natchathiram Report", 679f, BOLD, 17f, BRAND);

                // Match Customer PDF's label/value positions (78 / 211), 18pt rows.
                detail(out, "Date of Birth:", r.dateOfBirth().format(DATE), 646f);
                detail(out, "Time of Birth:", r.timeOfBirth().format(TIME) + " IST", 628f);
                detail(out, "Place of Birth:", r.placeOfBirth(), 610f);
                detail(out, "Raasi:", r.raasi(), 592f);
                drawTamil(out, document, "raasi-" + slug(r.raasi()) + ".png", 364f, 588f, 13f);
                detail(out, "Natchathiram:", r.natchathiram(), 574f);
                drawTamil(out, document, "nakshatra-" + slug(r.natchathiram()) + ".png", 364f, 570f, 13f);
                detail(out, "Pada:", r.pada() + " / 4", 556f);
                detail(out, "VITHI EN:", String.valueOf(r.vithiEn()), 532f);
                detail(out, "MATHI EN:", String.valueOf(r.mathiEn()), 514f);
                detail(out, "FRIENDLY EN:", r.friendlyEn().isBlank() ? "Not specified" : r.friendlyEn(), 496f);

                text(out, "Vithi: " + r.vithiCalculation(), 78f, 478f, NORMAL, 8.6f, MUTED);
                text(out, "Mathi: " + r.mathiCalculation(), 78f, 465f, NORMAL, 8.6f, MUTED);

                text(out, "Vithi En / Friendly En", LEFT, 449f, BOLD, 10.4f, BRAND);
                float friendlyTop = 439f;
                tableHeader(out, friendlyTop, new float[]{130, 155, TABLE_WIDTH - 285},
                        new String[]{"Vithi En", "Friendly En", "Selection"});
                List<String> friendly = List.of("4", "7", "9", "1", "9", "9", "2", "5, 6", "");
                float fy = friendlyTop - 22f;
                for (int i = 1; i <= 9; i++) {
                    boolean match = i == r.vithiEn();
                    String note = match ? "SELECTED VITHI EN" : "";
                    tableRow(out, fy, 17f, new float[]{130, 155, TABLE_WIDTH - 285},
                            new String[]{String.valueOf(i), friendly.get(i - 1), note}, match);
                    fy -= 17f;
                }

                text(out, "Nakshatra Pada Naming Letters - " + r.natchathiram(), LEFT, 250f,
                        BOLD, 10.4f, BRAND);
                float padaTop = 239f;
                float[] widths = {94f, 159f, 88f, TABLE_WIDTH - 341f};
                tableHeader(out, padaTop, widths,
                        new String[]{"Pada", "English Letters", "Tamil", "Selection"});
                float py = padaTop - 22f;
                for (NakshatraPadaLetters.PadaLetter entry : r.padaLetters()) {
                    boolean current = entry.pada() == r.pada();
                    tableRow(out, py, 22f, widths,
                            new String[]{"Pada " + entry.pada(), entry.english(), "", current ? "YOUR PADA" : ""}, current);
                    // Tamil is a pre-shaped, packaged glyph image to avoid font-embedding issues.
                    drawTamil(out, document, "pada-" + slug(r.natchathiram()) + "-" + entry.pada() + ".png",
                            LEFT + widths[0] + widths[1] + 12f, py - 17f, 15f);
                    py -= 22f;
                }

                if (r.padaLettersUnverified()) {
                    text(out, "Mapping warning: this nakshatra's letters were marked UNVERIFIED in the provided workbook.",
                            LEFT, 111f, NORMAL, 7.1f, MUTED);
                } else {
                    text(out, "Naming-letter variants are taken from the provided Nakshatra/Pada workbook.",
                            LEFT, 111f, NORMAL, 7.1f, MUTED);
                }
                if (r.nearPadaBoundary()) {
                    text(out, "Boundary warning: confirm the recorded birth time with a high-precision Lahiri ephemeris.",
                            LEFT, 99f, NORMAL, 7.1f, MUTED);
                } else {
                    text(out, "Method: approximate geocentric Moon calculation with Lahiri ayanamsa.",
                            LEFT, 99f, NORMAL, 7.1f, MUTED);
                }
                line(out, LEFT, 83f, W - RIGHT, 83f, GOLD, 0.7f);
                text(out, "Generated by IK's Numerology  |  iksnumerology@gmail.com",
                        LEFT, 68f, NORMAL, 8f, BRAND);
                text(out, "Traditional calculation; near-boundary results require professional ephemeris verification.",
                        LEFT, 54f, NORMAL, 7.5f, MUTED);
            }
            document.save(bytes);
            return bytes.toByteArray();
        }
    }

    private static void detail(PDPageContentStream out, String label, String value, float y) throws IOException {
        text(out, label, 78f, y, BOLD, 10f, BODY);
        text(out, fit(value, NORMAL, 10f, 310f), 211f, y, NORMAL, 10f, BODY);
    }

    private static void tableHeader(PDPageContentStream out, float top, float[] widths,
                                    String[] columns) throws IOException {
        float x = LEFT;
        for (int i = 0; i < widths.length; i++) {
            box(out, x, top - 22f, widths[i], 22f, HEADER_BG);
            String label = columns[i];
            float sw = BOLD.getStringWidth(label) * 10f / 1000f;
            text(out, label, x + (widths[i] - sw) / 2f, top - 15f, BOLD, 10f, BODY);
            x += widths[i];
        }
    }

    private static void tableRow(PDPageContentStream out, float top, float height, float[] widths,
                                 String[] values, boolean selected) throws IOException {
        float x = LEFT;
        for (int i = 0; i < widths.length; i++) {
            box(out, x, top - height, widths[i], height, selected ? SELECTED_BG : Color.WHITE);
            PDFont font = selected ? BOLD : NORMAL;
            float fontSize = i == widths.length - 1 ? 8f : 9.5f;
            String value = fit(values[i], font, fontSize, widths[i] - 14f);
            text(out, value, x + 7f, top - (height + fontSize) / 2f + 3f,
                    font, fontSize, selected ? SELECTED_INK : BODY);
            x += widths[i];
        }
        if (selected) {
            out.setNonStrokingColor(SELECTED_INK);
            out.addRect(LEFT, top - height + 0.5f, 3.5f, height - 1f);
            out.fill();
        }
    }

    private static void box(PDPageContentStream out, float x, float y, float w, float h,
                            Color background) throws IOException {
        out.setNonStrokingColor(background);
        out.addRect(x, y, w, h);
        out.fill();
        out.setLineWidth(0.5f);
        out.setStrokingColor(BORDER);
        out.addRect(x, y, w, h);
        out.stroke();
    }

    private static void line(PDPageContentStream out, float x1, float y1, float x2, float y2,
                             Color color, float width) throws IOException {
        out.setStrokingColor(color);
        out.setLineWidth(width);
        out.moveTo(x1, y1);
        out.lineTo(x2, y2);
        out.stroke();
    }

    private static void centered(PDPageContentStream out, String value, float y, PDFont font,
                                 float size, Color color) throws IOException {
        float width = font.getStringWidth(value) * size / 1000f;
        text(out, value, (W - width) / 2f, y, font, size, color);
    }

    private static void drawTamil(PDPageContentStream out, PDDocument document,
                                  String fileName, float x, float y, float displayHeight) throws IOException {
        String path = "/pdf/tamil/" + fileName;
        try (InputStream input = RaasiPdfService.class.getResourceAsStream(path)) {
            if (input == null) throw new IOException("Missing packaged Tamil label: " + path);
            PDImageXObject image = PDImageXObject.createFromByteArray(document, input.readAllBytes(), fileName);
            float displayWidth = displayHeight * image.getWidth() / image.getHeight();
            out.drawImage(image, x, y, displayWidth, displayHeight);
        }
    }

    private static String slug(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }

    private static void text(PDPageContentStream out, String value, float x, float y,
                             PDFont font, float size, Color color) throws IOException {
        out.beginText();
        out.setFont(font, size);
        out.setNonStrokingColor(color);
        out.newLineAtOffset(x, y);
        out.showText(pdfSafe(value));
        out.endText();
    }

    private static String fit(String value, PDFont font, float size, float maxWidth) throws IOException {
        String string = pdfSafe(value);
        if (font.getStringWidth(string) / 1000f * size <= maxWidth) return string;
        while (!string.isEmpty() && font.getStringWidth(string + "...") / 1000f * size > maxWidth) {
            string = string.substring(0, string.length() - 1);
        }
        return string + "...";
    }

    private static String pdfSafe(String value) {
        if (value == null) return "";
        return value.replace("→", " -> ").replace('–', '-').replace('—', '-')
                .replaceAll("[^\\x20-\\x7E]", "?");
    }
}
