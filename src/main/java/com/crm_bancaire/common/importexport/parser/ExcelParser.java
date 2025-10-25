package com.crm_bancaire.common.importexport.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDataValidationConstraint;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
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

    /**
     * Génère un template Excel avec headers, exemple et listes déroulantes.
     *
     * @param headers Liste des colonnes (avec * pour obligatoire)
     * @param exampleData Map avec valeur exemple pour chaque colonne
     * @param dropdownOptions Map avec options dropdown pour certaines colonnes
     * @return Workbook prêt à être téléchargé
     */
    public Workbook generateTemplate(
            List<String> headers,
            Map<String, String> exampleData,
            Map<String, List<String>> dropdownOptions
    ) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Import");

        // Style pour headers (bleu + gras + blanc)
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // Ligne 1: Headers
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);

            // Auto-size la colonne
            String columnName = headers.get(i).replace("*", "").trim();
            sheet.setColumnWidth(i, Math.max(3000, columnName.length() * 256 + 1000));
        }

        // Ligne 2: Exemple
        Row exampleRow = sheet.createRow(1);
        for (int i = 0; i < headers.size(); i++) {
            String columnName = headers.get(i).replace("*", "").trim();
            String value = exampleData.get(columnName);
            if (value != null) {
                Cell cell = exampleRow.createCell(i);
                cell.setCellValue(value);
            }
        }

        // Appliquer les listes déroulantes (data validation)
        if (dropdownOptions != null && !dropdownOptions.isEmpty()) {
            XSSFDataValidationHelper validationHelper = new XSSFDataValidationHelper(sheet);

            for (int i = 0; i < headers.size(); i++) {
                String columnName = headers.get(i).replace("*", "").trim();
                List<String> options = dropdownOptions.get(columnName);

                if (options != null && !options.isEmpty()) {
                    // Créer la liste de valeurs autorisées
                    String[] optionsArray = options.toArray(new String[0]);
                    XSSFDataValidationConstraint constraint =
                        (XSSFDataValidationConstraint) validationHelper.createExplicitListConstraint(optionsArray);

                    // Appliquer sur les lignes 2 à 501 (row index 1 à 500)
                    CellRangeAddressList addressList = new CellRangeAddressList(1, 500, i, i);
                    XSSFDataValidation validation = (XSSFDataValidation) validationHelper.createValidation(constraint, addressList);

                    // Configuration de la validation
                    validation.setShowErrorBox(true);
                    validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
                    validation.createErrorBox("Valeur invalide",
                        "Veuillez sélectionner une valeur dans la liste: " + String.join(", ", options));
                    validation.setSuppressDropDownArrow(false);

                    sheet.addValidationData(validation);

                    log.debug("Added dropdown for column {} with {} options", columnName, options.size());
                }
            }
        }

        // Figer la ligne de headers
        sheet.createFreezePane(0, 1);

        log.info("Generated Excel template with {} columns and {} dropdowns",
            headers.size(), dropdownOptions != null ? dropdownOptions.size() : 0);

        return workbook;
    }
}
