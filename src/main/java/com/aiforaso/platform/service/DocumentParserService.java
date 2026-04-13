package com.aiforaso.platform.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

@Service
public class DocumentParserService {

    public String extractText(Path path, String sourceType) {
        try {
            return switch (sourceType.toUpperCase()) {
                case "TEXT" -> readTextFile(path);
                case "PDF" -> readPdf(path);
                case "EXCEL" -> readExcel(path);
                default -> buildFallback(path, sourceType);
            };
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to parse document: " + path, exception);
        }
    }

    private String readTextFile(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private String readPdf(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String readExcel(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            DataFormatter formatter = new DataFormatter();
            List<String> rows = new ArrayList<>();
            for (Sheet sheet : workbook) {
                rows.add("# Sheet: " + sheet.getSheetName());
                for (Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    for (Cell cell : row) {
                        String value = formatter.formatCellValue(cell);
                        if (!value.isBlank()) {
                            cells.add(value);
                        }
                    }
                    if (!cells.isEmpty()) {
                        rows.add(String.join(" | ", cells));
                    }
                }
            }
            return String.join("\n", rows);
        } catch (Exception exception) {
            throw new IOException("Failed to read spreadsheet content", exception);
        }
    }

    private String buildFallback(Path path, String sourceType) throws IOException {
        long size = Files.size(path);
        return "Imported " + sourceType + " file `" + path.getFileName() + "`, size=" + size
                + " bytes. Parser for this format is not implemented yet.";
    }
}
