package com.crm_bancaire.common.importexport.service;

import com.crm_bancaire.common.importexport.annotation.Importable;
import com.crm_bancaire.common.importexport.enums.ExportFormat;
import com.crm_bancaire.common.importexport.mapper.ImportMapper;
import com.crm_bancaire.common.importexport.parser.ExcelParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Service pour générer les templates d'import Excel/CSV.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {

    private final ExcelParser excelParser;

    /**
     * Génère un template d'import (Excel ou CSV).
     *
     * @param mapper ImportMapper avec colonnes et exemple
     * @param annotation Annotation @Importable
     * @param format Format (XLSX ou CSV)
     * @return Bytes du template
     */
    public byte[] generateTemplate(
            ImportMapper<?> mapper,
            Importable annotation,
            ExportFormat format
    ) throws Exception {
        log.info("Generating {} template for entity '{}'", format, annotation.entity());

        if (format == ExportFormat.XLSX) {
            return generateExcelTemplate(mapper);
        } else {
            return generateCsvTemplate(mapper);
        }
    }

    /**
     * Génère template Excel avec dropdowns.
     */
    private byte[] generateExcelTemplate(ImportMapper<?> mapper) throws Exception {
        // 1. Construire headers
        List<String> headers = buildHeaders(mapper);

        // 2. Construire exemple
        Map<String, String> exampleData = buildExampleData(mapper);

        // 3. Récupérer dropdowns
        Map<String, List<String>> dropdownOptions = mapper.getDropdownOptions();

        // 4. Générer workbook avec ExcelParser
        Workbook workbook = excelParser.generateTemplate(headers, exampleData, dropdownOptions);

        // 5. Écrire en ByteArray
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        log.info("Generated Excel template with {} columns and {} dropdowns",
            headers.size(), dropdownOptions.size());

        return out.toByteArray();
    }

    /**
     * Génère template CSV (simple, pas de dropdowns).
     */
    private byte[] generateCsvTemplate(ImportMapper<?> mapper) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(out);

        // 1. Headers
        List<String> headers = mapper.getRequiredColumns();
        headers.addAll(mapper.getOptionalColumns());

        writer.write(String.join(",", headers) + "\n");

        // 2. Exemple
        Object example = mapper.getExampleRow();
        List<String> values = new ArrayList<>();

        for (String header : headers) {
            Object value = getFieldValue(example, header);
            values.add(value != null ? value.toString() : "");
        }

        writer.write(String.join(",", values) + "\n");

        writer.flush();
        writer.close();

        log.info("Generated CSV template with {} columns", headers.size());

        return out.toByteArray();
    }

    /**
     * Construit la liste des headers avec * pour colonnes requises.
     */
    private List<String> buildHeaders(ImportMapper<?> mapper) {
        List<String> headers = new ArrayList<>();

        // Required columns avec *
        for (String col : mapper.getRequiredColumns()) {
            headers.add(col + " *");
        }

        // Optional columns sans *
        headers.addAll(mapper.getOptionalColumns());

        return headers;
    }

    /**
     * Construit la Map exemple pour ExcelParser.
     */
    private Map<String, String> buildExampleData(ImportMapper<?> mapper) {
        Map<String, String> exampleData = new LinkedHashMap<>();

        Object example = mapper.getExampleRow();
        List<String> allColumns = new ArrayList<>(mapper.getRequiredColumns());
        allColumns.addAll(mapper.getOptionalColumns());

        for (String column : allColumns) {
            Object value = getFieldValue(example, column);
            exampleData.put(column, value != null ? value.toString() : "");
        }

        return exampleData;
    }

    /**
     * Récupère la valeur d'un champ via reflection.
     */
    private Object getFieldValue(Object object, String fieldName) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        } catch (Exception e) {
            log.warn("Could not get field '{}' from {}", fieldName, object.getClass().getSimpleName());
            return null;
        }
    }
}
