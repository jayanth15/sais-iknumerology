package com.namesuggestion;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Reads user-provided names and meanings without calling or persisting anything to Sarvam. */
@Service
public class ExcelNameService {
    public record ExcelName(String name, String meaning) {}
    public record ReadResult(List<ExcelName> names, int skippedRows, int duplicateRows) {}
    private static final int MAX_FILE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_ROWS = 15000;
    private static final int MAX_MEANING_CHARS = 250;

    public ReadResult read(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("Choose an Excel file with Name and Meaning columns.");
        String filename = Objects.requireNonNullElse(file.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
        if (!(filename.endsWith(".xlsx") || filename.endsWith(".xls")))
            throw new IllegalArgumentException("Please upload a .xls or .xlsx Excel file.");
        if (file.getSize() > MAX_FILE_BYTES)
            throw new IllegalArgumentException("Excel file must be 5 MB or smaller.");
        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            if (workbook.getNumberOfSheets() == 0)
                throw new IllegalArgumentException("The Excel workbook has no sheets.");
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null)
                throw new IllegalArgumentException("The first row must have Name and Meaning column headings.");
            DataFormatter format = new DataFormatter(Locale.ENGLISH);
            int nameCol = -1, meaningCol = -1;
            for (Cell cell : header) {
                String value = stringValue(cell, format).replace("\uFEFF", "").trim();
                if (value.equalsIgnoreCase("Name")) nameCol = cell.getColumnIndex();
                if (value.equalsIgnoreCase("Meaning")) meaningCol = cell.getColumnIndex();
            }
            if (nameCol == -1 || meaningCol == -1 || nameCol == meaningCol)
                throw new IllegalArgumentException("Excel row 1 must contain columns named Name and Meaning (in any order).");
            if (sheet.getLastRowNum() - sheet.getFirstRowNum() > MAX_ROWS)
                throw new IllegalArgumentException("Excel file contains too many rows (maximum 15,000).");
            var names = new ArrayList<ExcelName>();
            var seen = new HashSet<String>();
            int skipped = 0, duplicates = 0;
            for (int i = header.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Cell rawName = row.getCell(nameCol, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                Cell rawMeaning = row.getCell(meaningCol, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                // Never run spreadsheet formulas or pull externally linked content into the report.
                if ((rawName != null && rawName.getCellType() == CellType.FORMULA) ||
                    (rawMeaning != null && rawMeaning.getCellType() == CellType.FORMULA)) {
                    skipped++; continue;
                }
                String name = stringValue(rawName, format).replaceAll("\\s+", " ").trim();
                String meaning = stringValue(rawMeaning, format).replaceAll("\\s+", " ").trim();
                if (name.isEmpty() && meaning.isEmpty()) continue;
                if (!name.matches("[A-Za-z][A-Za-z .'-]{0,79}") || meaning.isBlank() ||
                    meaning.length() > MAX_MEANING_CHARS) {
                    skipped++; continue;
                }
                if (!seen.add(name.toLowerCase(Locale.ROOT))) {duplicates++;continue;}
                names.add(new ExcelName(name, meaning));
            }
            if (names.isEmpty())
                throw new IllegalArgumentException("No valid Name/Meaning pairs found in the first Excel sheet. Use English-letter spellings and nonempty meanings.");
            return new ReadResult(List.copyOf(names), skipped, duplicates);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new IllegalArgumentException("Unable to read the Excel file. Please check that it is a valid .xls or .xlsx workbook.");
        }
    }

    private static String stringValue(Cell cell, DataFormatter formatter) {
        if (cell == null || cell.getCellType() == CellType.FORMULA) return "";
        return formatter.formatCellValue(cell).trim();
    }
}
