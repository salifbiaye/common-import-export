package com.crm_bancaire.common.importexport.parser;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Parser pour fichiers CSV
 */
@Slf4j
@Component
public class CsvParser implements FileParser {

    @Override
    public List<Map<String, String>> parse(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream()) {
            return parse(is);
        }
    }

    @Override
    public List<Map<String, String>> parse(InputStream inputStream) throws Exception {
        List<Map<String, String>> result = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            List<String[]> allRows = reader.readAll();

            if (allRows.isEmpty()) {
                throw new IllegalArgumentException("Fichier CSV vide");
            }

            // Ligne 1 = Headers
            String[] headers = allRows.get(0);
            List<String> cleanHeaders = cleanHeaders(headers);

            // Lignes 2+ = Data
            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);

                // Skip lignes vides
                if (isRowEmpty(row)) {
                    continue;
                }

                Map<String, String> rowData = new HashMap<>();
                for (int j = 0; j < Math.min(row.length, cleanHeaders.size()); j++) {
                    String value = row[j] != null ? row[j].trim() : null;
                    rowData.put(cleanHeaders.get(j), value);
                }

                result.add(rowData);
            }

        } catch (CsvException e) {
            throw new IllegalArgumentException("Erreur lors du parsing CSV: " + e.getMessage(), e);
        }

        log.info("Parsed {} rows from CSV file", result.size());
        return result;
    }

    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".csv");
    }

    /**
     * Nettoie les headers (enlève les * et trim).
     */
    private List<String> cleanHeaders(String[] headers) {
        List<String> result = new ArrayList<>();
        for (String header : headers) {
            if (header != null && !header.trim().isEmpty()) {
                String clean = header.replace("*", "").trim();
                result.add(clean);
            }
        }
        return result;
    }

    /**
     * Vérifie si une ligne est vide.
     */
    private boolean isRowEmpty(String[] row) {
        for (String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
