package com.crm_bancaire.common.importexport.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Parser pour fichiers Excel (.xlsx)
 */
@Slf4j
@Component
public class ExcelParser implements FileParser {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public List<Map<String, String>> parse(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream()) {
            return parse(is);
        }
    }

    @Override
    public List<Map<String, String>> parse(InputStream inputStream) throws Exception {
        List<Map<String, String>> result = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            // Ligne 1 = Headers
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Fichier vide - aucun header trouvé");
            }

            List<String> headers = extractHeaders(headerRow);

            // Lignes 2+ = Data
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                Map<String, String> rowData = new HashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    String value = getCellValueAsString(cell);
                    rowData.put(headers.get(j), value);
                }

                result.add(rowData);
            }
        }

        log.info("Parsed {} rows from Excel file", result.size());
        return result;
    }

    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".xlsx");
    }

    /**
     * Extrait les headers de la première ligne.
     */
    private List<String> extractHeaders(Row headerRow) {
        List<String> headers = new ArrayList<>();
        for (Cell cell : headerRow) {
            String header = getCellValueAsString(cell);
            if (header != null && !header.trim().isEmpty()) {
                // Enlever les "*" (marqueur champ obligatoire)
                header = header.replace("*", "").trim();
                headers.add(header);
            }
        }
        return headers;
    }

    /**
     * Convertit une cellule en String.
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return DATE_FORMAT.format(cell.getDateCellValue());
                } else {
                    double value = cell.getNumericCellValue();
                    // Si c'est un entier, pas de décimales
                    if (value == Math.floor(value)) {
                        return String.valueOf((long) value);
                    }
                    return String.valueOf(value);
                }

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }

            case BLANK:
                return null;

            default:
                return null;
        }
    }

    /**
     * Vérifie si une ligne est vide.
     */
    private boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValueAsString(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}
