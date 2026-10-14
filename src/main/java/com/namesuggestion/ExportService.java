package com.namesuggestion;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.io.InputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class ExportService {
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float LEFT = 50f;
    private static final float RIGHT = 50f;
    private static final float TABLE_WIDTH = PAGE_WIDTH - LEFT - RIGHT;
    private static final float[] COLUMN_WIDTHS = {74f, 173f, TABLE_WIDTH - 247f};
    private static final float[] COLUMN_STARTS = {
            LEFT,
            LEFT + COLUMN_WIDTHS[0],
            LEFT + COLUMN_WIDTHS[0] + COLUMN_WIDTHS[1],
            PAGE_WIDTH - RIGHT
    };
    private static final PDFont REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final String CONTACT_PHONE = "+91 9944925273";
    private static final String CONTACT_PHONE_ALT = "+91 9944769370";
    private static final String CONTACT_EMAIL = "iksnumerology@gmail.com";
    private static final String CUSTOMER_LOGO_RESOURCE = "/pdf/iks-numerology-logo.png";
    private static final DateTimeFormatter BIRTH_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ENGLISH);
    private static final DateTimeFormatter BIRTH_TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private final RaasiCalculatorService raasiCalculator;

    public ExportService(RaasiCalculatorService raasiCalculator) {
        this.raasiCalculator = raasiCalculator;
    }

    private List<String[]> criteria(SearchForm form) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Language:", form.getLanguage()});
        if ("SARVAM".equals(form.getNameSource())) rows.add(new String[]{"Starting Pattern:", form.getStartLetter()});
        rows.add(new String[]{"Surname:", form.getSurname()});
        rows.add(new String[]{"Name Order:", form.isSurnameFirst()
                ? "Surname before Starting Pattern" : "Starting Pattern before Surname"});
        rows.add(new String[]{"Date of Birth:", form.getDateOfBirth()});
        rows.add(new String[]{"Gender:", form.getGender()});
        rows.add(new String[]{"Final Number:", form.getFinalNumber() == null
                ? "Any" : String.valueOf(form.getFinalNumber())});
        return rows;
    }

    /** Customer output: preserve local IST clock and use the same Moon calculator as /raasi. */
    private List<String[]> customerCriteria(SearchForm form) {
        LocalDate date = LocalDate.parse(form.getDateOfBirth());
        LocalTime time = LocalTime.parse(form.getTimeOfBirth());
        String raasi;
        String natchathiram;
        try {
            // The service computes geocentric Moon longitude; Indian birthplace does not
            // alter the Moon sign for the given local IST timestamp.
            var result = raasiCalculator.calculate(date, time, "India");
            raasi = result.raasi();
            natchathiram = result.natchathiram();
        } catch (IllegalArgumentException e) {
            // Existing suggestions can have DOBs before 1950, outside the Raasi
            // calculator's validated range. Never fabricate an astrology result.
            raasi = "Unavailable (outside supported date range)";
            natchathiram = "Unavailable (outside supported date range)";
        }
        return List.of(
                new String[]{"Surname:", form.getSurname()},
                new String[]{"Gender:", form.getGender()},
                new String[]{"Date of Birth:", date.format(BIRTH_DATE_FORMAT)},
                new String[]{"Time of Birth:", time.format(BIRTH_TIME_FORMAT) + " IST"},
                new String[]{"Raasi:", raasi},
                new String[]{"Natchathiram:", natchathiram}
        );
    }

    public byte[] excel(List<CatalogueService.Suggestion> suggestions, SearchForm form) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Name Suggestions");
            int rowNumber = 0;
            sheet.createRow(rowNumber++).createCell(0).setCellValue("Name Suggestions");
            for (String[] criterion : criteria(form)) {
                sheet.createRow(rowNumber++).createCell(0).setCellValue(criterion[0] + " " + criterion[1]);
            }
            rowNumber++;

            String[] headers = {"S.No.", "Name", "Meaning"};
            Row header = sheet.createRow(rowNumber++);
            CellStyle bold = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            font.setBold(true);
            bold.setFont(font);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(bold);
            }

            int serial = 1;
            for (CatalogueService.Suggestion suggestion : suggestions) {
                Row row = sheet.createRow(rowNumber++);
                row.createCell(0).setCellValue(serial++);
                row.createCell(1).setCellValue(suggestion.combinedName());
                row.createCell(2).setCellValue(suggestion.meaning());
            }
            sheet.setColumnWidth(0, 12 * 256);
            sheet.setColumnWidth(1, 35 * 256);
            sheet.setColumnWidth(2, 60 * 256);
            sheet.createFreezePane(0, header.getRowNum() + 1);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    public byte[] pdf(List<CatalogueService.Suggestion> suggestions, SearchForm form) throws IOException {
        return pdf(suggestions, form, false);
    }

    public byte[] customerPdf(List<CatalogueService.Suggestion> suggestions, SearchForm form) throws IOException {
        return pdf(suggestions, form, true);
    }

    private byte[] pdf(List<CatalogueService.Suggestion> suggestions, SearchForm form, boolean customer) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            // Load once per customer report and reuse the same embedded image on every page.
            // The internal/User PDF remains unchanged.
            PDImageXObject logo = customer ? CustomerPdfBranding.loadLogo(document) : null;
            int index = 0;
            int pageNumber = 0;
            while (index < suggestions.size()) {
                boolean firstPage = pageNumber == 0;
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    float rowTop;
                    float tableTop;
                    if (customer) {
                        tableTop = firstPage
                                ? drawCustomerFirstPageHeading(stream, logo, form)
                                : drawCustomerContinuationHeading(stream, logo);
                        rowTop = drawTableHeader(stream, tableTop);
                    } else if (firstPage) {
                        tableTop = drawFirstPageHeading(stream, form, false);
                        rowTop = drawTableHeader(stream, tableTop);
                    } else {
                        tableTop = PAGE_HEIGHT - 50f;
                        rowTop = tableTop;
                    }

                    int rowsOnPage = 0;
                    int pageLimit = firstPage ? 25 : 35;
                    while (index < suggestions.size() && rowsOnPage < pageLimit) {
                        CatalogueService.Suggestion suggestion = suggestions.get(index);
                        List<String> nameLines = wrap(suggestion.combinedName(), COLUMN_WIDTHS[1] - 12f, REGULAR, 10f);
                        List<String> meaningLines = wrap(suggestion.meaning(), COLUMN_WIDTHS[2] - 12f, REGULAR, 10f);
                        int lineCount = Math.max(nameLines.size(), meaningLines.size());
                        float rowHeight = Math.max(21f, lineCount * 12f + 8f);
                        if (rowTop - rowHeight < 48f) {
                            break;
                        }
                        drawTableRow(stream, index + 1, nameLines, meaningLines, rowTop, rowHeight);
                        rowTop -= rowHeight;
                        index++;
                        rowsOnPage++;
                    }
                    if (rowsOnPage == 0) {
                        throw new IOException("A name row could not fit on an A4 page.");
                    }
                    drawVerticalBorders(stream, tableTop, rowTop);
                }
                pageNumber++;
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    public byte[] calculationPdf(PyramidService.Calculation calculation) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new CalculationPdfLayout(document).render(calculation);
            document.save(output);
            return output.toByteArray();
        }
    }

    private static class CalculationPdfLayout {
        private static final float CARD_X = 36f;
        private static final float CARD_WIDTH = PAGE_WIDTH - CARD_X * 2;
        private static final float CARD_TOP = PAGE_HEIGHT - 36f;
        private static final float INNER_X = CARD_X + 26f;
        private static final float INNER_WIDTH = CARD_WIDTH - 52f;
        private static final float ROW_STEP = 25.5f;
        private static final float CELL = 18f;
        private static final float GAP = 6f;
        private final PDDocument document;
        CalculationPdfLayout(PDDocument document) { this.document = document; }

        void render(PyramidService.Calculation calculation) throws IOException {
            List<String> letterLines = letterValueLines(calculation);
            float firstRowOffset = 166f + Math.max(0, letterLines.size() - 1) * 11f;
            int maxRowsPerPage = Math.max(1, (int) ((CARD_TOP - firstRowOffset - 58f) / ROW_STEP));
            int start = 0;
            do {
                int end = Math.min(calculation.rows().size(), start + maxRowsPerPage);
                renderPage(calculation, start, end, letterLines, firstRowOffset);
                start = end;
            } while (start < calculation.rows().size());
        }

        private List<String> letterValueLines(PyramidService.Calculation calculation) throws IOException {
            String letters = ascii(calculation.combinedName()).replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
            List<Integer> values = calculation.rows().get(0);
            List<String> lines = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < Math.min(letters.length(), values.size()); i++) {
                String pair = letters.charAt(i) + "=" + values.get(i);
                String candidate = line.isEmpty() ? pair : line + "   " + pair;
                if (!line.isEmpty() && REGULAR.getStringWidth(candidate) * 8f / 1000f > INNER_WIDTH) {
                    lines.add(line.toString());
                    line.setLength(0);
                    line.append(pair);
                } else {
                    line.setLength(0);
                    line.append(candidate);
                }
            }
            if (!line.isEmpty()) lines.add(line.toString());
            return lines;
        }

        private void renderPage(PyramidService.Calculation calculation, int start, int end,
                                List<String> letterLines, float firstRowOffset) throws IOException {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.setNonStrokingColor(0.957f, 0.969f, 0.953f);
                stream.addRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);
                stream.fill();

                float rowTop = CARD_TOP - firstRowOffset;
                float lastRowBottom = rowTop - Math.max(0, end - start - 1) * ROW_STEP - CELL;
                float cardBottom = Math.max(40f, lastRowBottom - 26f);
                roundedRectangle(stream, CARD_X, cardBottom, CARD_WIDTH, CARD_TOP - cardBottom, 12f,
                        1f, 1f, 1f, 0.863f, 0.898f, 0.863f);

                write(stream, "FINAL NUMBER", INNER_X, CARD_TOP - 31f, BOLD, 9f, 0.392f, 0.486f, 0.443f);
                write(stream, String.valueOf(calculation.finalNumber()), INNER_X, CARD_TOP - 82f,
                        BOLD, 36f, 0.078f, 0.361f, 0.282f);

                String label = "Exact input: ";
                float nameFontSize = 11f;
                float exactWidth = (REGULAR.getStringWidth(label) + BOLD.getStringWidth(ascii(calculation.combinedName()))) * nameFontSize / 1000f;
                if (exactWidth > INNER_WIDTH) nameFontSize *= INNER_WIDTH / exactWidth;
                float labelWidth = REGULAR.getStringWidth(label) * nameFontSize / 1000f;
                write(stream, label, INNER_X, CARD_TOP - 111f, REGULAR, nameFontSize, 0.125f, 0.235f, 0.212f);
                write(stream, calculation.combinedName(), INNER_X + labelWidth, CARD_TOP - 111f,
                        BOLD, nameFontSize, 0.125f, 0.235f, 0.212f);

                write(stream, "LETTER VALUES", INNER_X, CARD_TOP - 131f, BOLD, 8f,
                        0.392f, 0.486f, 0.443f);
                for (int i = 0; i < letterLines.size(); i++) {
                    write(stream, letterLines.get(i), INNER_X, CARD_TOP - 145f - i * 11f, REGULAR, 8f,
                            0.125f, 0.235f, 0.212f);
                }

                for (int i = start; i < end; i++) {
                    drawCellRow(stream, calculation.rows().get(i), rowTop - (i - start) * ROW_STEP);
                }
            }
        }

        private void drawCellRow(PDPageContentStream stream, List<Integer> values, float top) throws IOException {
            float gap = GAP;
            float cell = CELL;
            float required = values.size() * cell + Math.max(0, values.size() - 1) * gap;
            if (required > INNER_WIDTH) {
                gap = Math.max(0.5f, Math.min(GAP, (INNER_WIDTH - values.size() * 4f) / Math.max(1, values.size() - 1)));
                cell = (INNER_WIDTH - Math.max(0, values.size() - 1) * gap) / values.size();
            }
            float total = values.size() * cell + Math.max(0, values.size() - 1) * gap;
            float x = CARD_X + (CARD_WIDTH - total) / 2f;
            float fontSize = Math.min(10f, Math.max(3.5f, cell * 0.55f));
            for (int value : values) {
                roundedRectangle(stream, x, top - CELL, cell, CELL, Math.min(3f, cell / 4f),
                        0.929f, 0.957f, 0.922f, 0.929f, 0.957f, 0.922f);
                String text = String.valueOf(value);
                float textWidth = REGULAR.getStringWidth(text) * fontSize / 1000f;
                write(stream, text, x + (cell - textWidth) / 2f, top - CELL + (CELL - fontSize) / 2f + 1.5f,
                        REGULAR, fontSize, 0.125f, 0.235f, 0.212f);
                x += cell + gap;
            }
        }

        private void write(PDPageContentStream stream, String text, float x, float y, PDFont font, float size,
                           float r, float g, float b) throws IOException {
            stream.beginText();
            stream.setNonStrokingColor(r, g, b);
            stream.setFont(font, size);
            stream.newLineAtOffset(x, y);
            stream.showText(ascii(text));
            stream.endText();
        }

        private void roundedRectangle(PDPageContentStream stream, float x, float y, float width, float height,
                                      float radius, float fillR, float fillG, float fillB,
                                      float lineR, float lineG, float lineB) throws IOException {
            float k = 0.55228475f * radius;
            stream.setNonStrokingColor(fillR, fillG, fillB);
            stream.setStrokingColor(lineR, lineG, lineB);
            stream.moveTo(x + radius, y);
            stream.lineTo(x + width - radius, y);
            stream.curveTo(x + width - radius + k, y, x + width, y + radius - k, x + width, y + radius);
            stream.lineTo(x + width, y + height - radius);
            stream.curveTo(x + width, y + height - radius + k, x + width - radius + k, y + height, x + width - radius, y + height);
            stream.lineTo(x + radius, y + height);
            stream.curveTo(x + radius - k, y + height, x, y + height - radius + k, x, y + height - radius);
            stream.lineTo(x, y + radius);
            stream.curveTo(x, y + radius - k, x + radius - k, y, x + radius, y);
            stream.closeAndFillAndStroke();
        }
    }

    private float drawCustomerFirstPageHeading(PDPageContentStream stream, PDImageXObject logo,
                                                SearchForm form) throws IOException {
        CustomerPdfBranding.drawHeader(stream, logo, true);
        String title = "Name Suggestions";
        float titleWidth = BOLD.getStringWidth(title) * 17f / 1000f;
        stream.setNonStrokingColor(0.08f, 0.25f, 0.27f);
        writeText(stream, title, (PAGE_WIDTH - titleWidth) / 2f, 679f, BOLD, 17f);
        stream.setNonStrokingColor(0f, 0f, 0f);
        float y = 646f;
        for (String[] criterion : customerCriteria(form)) {
            writeText(stream, criterion[0], 78f, y, BOLD, 10f);
            writeText(stream, criterion[1], 211f, y, REGULAR, 10f);
            y -= 18f;
        }
        return y - 6f; // Dynamic spacing: keep table close despite two new astrology rows.
    }

    private float drawCustomerContinuationHeading(PDPageContentStream stream,
                                                   PDImageXObject logo) throws IOException {
        CustomerPdfBranding.drawHeader(stream, logo, false);
        String title = "Name Suggestions (continued)";
        float titleWidth = BOLD.getStringWidth(title) * 12f / 1000f;
        stream.setNonStrokingColor(0.08f, 0.25f, 0.27f);
        writeText(stream, title, (PAGE_WIDTH - titleWidth) / 2f, 694f, BOLD, 12f);
        stream.setNonStrokingColor(0f, 0f, 0f);
        return 681f;
    }

    private float drawFirstPageHeading(PDPageContentStream stream, SearchForm form, boolean customer) throws IOException {
        String title = "Name Suggestions";
        float titleWidth = BOLD.getStringWidth(title) * 18f / 1000f;
        writeText(stream, title, (PAGE_WIDTH - titleWidth) / 2f, 768f, BOLD, 18f);

        List<String[]> shownCriteria = customer ? customerCriteria(form) : criteria(form);
        float y = 730f;
        for (String[] criterion : shownCriteria) {
            writeText(stream, criterion[0], 78f, y, BOLD, 10f);
            writeText(stream, criterion[1], 211f, y, REGULAR, 10f);
            y -= 18f;
        }
        return 730f - Math.max(0, shownCriteria.size() - 1) * 18f - 22f;
    }

    private float drawTableHeader(PDPageContentStream stream, float top) throws IOException {
        float height = 22f;
        stream.setNonStrokingColor(0.75f, 0.75f, 0.75f);
        stream.addRect(LEFT, top - height, TABLE_WIDTH, height);
        stream.fill();
        stream.setNonStrokingColor(0, 0, 0);
        stream.setStrokingColor(0, 0, 0);
        stream.setLineWidth(0.6f);
        stream.addRect(LEFT, top - height, TABLE_WIDTH, height);
        stream.stroke();
        for (int i = 1; i < COLUMN_STARTS.length - 1; i++) {
            stream.moveTo(COLUMN_STARTS[i], top);
            stream.lineTo(COLUMN_STARTS[i], top - height);
        }
        stream.stroke();

        String[] headers = {"S.No.", "Name", "Meaning"};
        for (int i = 0; i < headers.length; i++) {
            float textWidth = BOLD.getStringWidth(headers[i]) * 11f / 1000f;
            float x = COLUMN_STARTS[i] + (COLUMN_WIDTHS[i] - textWidth) / 2f;
            writeText(stream, headers[i], x, top - 15f, BOLD, 11f);
        }
        return top - height;
    }

    private void drawTableRow(PDPageContentStream stream, int serial, List<String> nameLines,
                              List<String> meaningLines, float top, float height) throws IOException {
        float bottom = top - height;
        stream.setStrokingColor(0, 0, 0);
        stream.setLineWidth(0.5f);
        stream.moveTo(LEFT, top);
        stream.lineTo(PAGE_WIDTH - RIGHT, top);
        stream.moveTo(LEFT, bottom);
        stream.lineTo(PAGE_WIDTH - RIGHT, bottom);
        stream.stroke();

        String number = String.valueOf(serial);
        float numberWidth = REGULAR.getStringWidth(number) * 10f / 1000f;
        writeText(stream, number, COLUMN_STARTS[0] + (COLUMN_WIDTHS[0] - numberWidth) / 2f,
                top - (height + 2f) / 2f, REGULAR, 10f);

        float lineBlockHeight = Math.max(nameLines.size(), meaningLines.size()) * 12f;
        float baseline = top - (height - lineBlockHeight) / 2f - 10f;
        drawLines(stream, nameLines, COLUMN_STARTS[1] + 6f, baseline, REGULAR, 10f);
        drawLines(stream, meaningLines, COLUMN_STARTS[2] + 6f, baseline, REGULAR, 10f);
    }

    private void drawVerticalBorders(PDPageContentStream stream, float top, float bottom) throws IOException {
        stream.setStrokingColor(0, 0, 0);
        stream.setLineWidth(0.5f);
        for (float x : COLUMN_STARTS) {
            stream.moveTo(x, top);
            stream.lineTo(x, bottom);
        }
        stream.stroke();
    }

    private void drawLines(PDPageContentStream stream, List<String> lines, float x, float baseline,
                           PDFont font, float fontSize) throws IOException {
        for (String line : lines) {
            writeText(stream, line, x, baseline, font, fontSize);
            baseline -= 12f;
        }
    }

    private void writeText(PDPageContentStream stream, String text, float x, float y,
                           PDFont font, float fontSize) throws IOException {
        stream.beginText();
        stream.setFont(font, fontSize);
        stream.newLineAtOffset(x, y);
        stream.showText(ascii(text));
        stream.endText();
    }

    private List<String> wrap(String value, float width, PDFont font, float fontSize) throws IOException {
        String text = ascii(value).strip();
        if (text.isEmpty()) {
            return List.of("");
        }
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.getStringWidth(candidate) * fontSize / 1000f <= width) {
                line.setLength(0);
                line.append(candidate);
            } else if (line.isEmpty()) {
                lines.addAll(splitLongWord(word, width, font, fontSize));
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private List<String> splitLongWord(String word, float width, PDFont font, float fontSize) throws IOException {
        List<String> parts = new ArrayList<>();
        StringBuilder part = new StringBuilder();
        for (char character : word.toCharArray()) {
            if (!part.isEmpty() && font.getStringWidth(part.toString() + character) * fontSize / 1000f > width) {
                parts.add(part.toString());
                part.setLength(0);
            }
            part.append(character);
        }
        if (!part.isEmpty()) {
            parts.add(part.toString());
        }
        return parts;
    }

    private static String ascii(String value) {
        return value.replaceAll("[^\\x20-\\x7E]", "?");
    }
}
