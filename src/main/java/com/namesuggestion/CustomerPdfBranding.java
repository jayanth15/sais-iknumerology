package com.namesuggestion;

import java.io.IOException;
import java.io.InputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/** Shared IK's Numerology Customer PDF letterhead used by Customer and Raasi reports. */
final class CustomerPdfBranding {
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float LEFT = 50f;
    private static final float RIGHT = 50f;
    private static final PDFont BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final String LOGO = "/pdf/iks-numerology-logo.png";
    private static final String PHONE = "+91 9944925273";
    private static final String PHONE_ALT = "+91 9944769370";
    private static final String EMAIL = "iksnumerology@gmail.com";

    private CustomerPdfBranding() { }

    static PDImageXObject loadLogo(PDDocument document) throws IOException {
        try (InputStream input = CustomerPdfBranding.class.getResourceAsStream(LOGO)) {
            if (input == null) throw new IOException("Missing bundled IK's Numerology logo: " + LOGO);
            return PDImageXObject.createFromByteArray(document, input.readAllBytes(), "IK's Numerology");
        }
    }

    // Keep identical coordinates and colors to the original Customer PDF header.
    static void drawHeader(PDPageContentStream stream, PDImageXObject logo,
                           boolean firstPage) throws IOException {
        float width = firstPage ? 255f : 225f;
        float height = width * logo.getHeight() / logo.getWidth();
        float top = PAGE_HEIGHT - 30f;
        stream.drawImage(logo, LEFT, top - height, width, height);

        stream.setStrokingColor(0.75f, 0.59f, 0.30f);
        stream.setLineWidth(1.2f);
        stream.moveTo(315f, PAGE_HEIGHT - 107f);
        stream.lineTo(315f, PAGE_HEIGHT - 43f);
        stream.stroke();

        stream.setNonStrokingColor(0.69f, 0.50f, 0.20f);
        writeText(stream, "CONTACT", 329f, PAGE_HEIGHT - 44f, 8f);
        stream.setNonStrokingColor(0.08f, 0.25f, 0.27f);
        float labelX = 329f;
        float valueX = 372f;
        writeText(stream, "Phone:", labelX, PAGE_HEIGHT - 64f, 8.6f);
        writeText(stream, PHONE, valueX, PAGE_HEIGHT - 64f, 8.6f);
        writeText(stream, PHONE_ALT, valueX, PAGE_HEIGHT - 80f, 8.6f);
        writeText(stream, "Email:", labelX, PAGE_HEIGHT - 96f, 8.7f);
        writeText(stream, EMAIL, valueX, PAGE_HEIGHT - 96f, 8.4f);

        float separatorY = firstPage ? PAGE_HEIGHT - 130f : PAGE_HEIGHT - 124f;
        stream.setStrokingColor(0.75f, 0.59f, 0.30f);
        stream.setLineWidth(0.9f);
        stream.moveTo(LEFT, separatorY);
        stream.lineTo(PAGE_WIDTH - RIGHT, separatorY);
        stream.stroke();
        stream.setNonStrokingColor(0f, 0f, 0f);
    }

    private static void writeText(PDPageContentStream stream, String text, float x, float y, float size) throws IOException {
        stream.beginText();
        stream.setFont(BOLD, size);
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
    }
}
