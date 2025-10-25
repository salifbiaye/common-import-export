package com.crm_bancaire.common.importexport.service;

import com.crm_bancaire.common.importexport.annotation.Exportable;
import com.crm_bancaire.common.importexport.enums.ExportFormat;
import com.crm_bancaire.common.importexport.parser.CsvParser;
import com.crm_bancaire.common.importexport.parser.ExcelParser;
import com.crm_bancaire.common.importexport.util.ColorMapper;
import com.crm_bancaire.common.importexport.util.StyleParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service pour gérer l'export vers Excel/CSV.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Exporte des données vers Excel ou CSV.
     *
     * @param targetService Service cible (avec @Exportable)
     * @param annotation Annotation @Exportable
     * @param format Format d'export (XLSX ou CSV)
     * @param queryParams Paramètres de filtrage
     * @return Bytes du fichier généré
     */
    public byte[] export(
            Object targetService,
            Exportable annotation,
            ExportFormat format,
            Map<String, String> queryParams
    ) throws Exception {
        log.info("Starting export for entity '{}' in format {}", annotation.entity(), format);

        // 1. Récupérer les données
        List<Object> data = fetchData(targetService, annotation, queryParams);
        log.info("Fetched {} records for export", data.size());

        // 2. Générer le fichier selon le format
        if (format == ExportFormat.XLSX) {
            return exportToExcel(data, annotation);
        } else {
            return exportToCsv(data, annotation);
        }
    }

    /**
     * Récupère les données depuis le service cible.
     */
    private List<Object> fetchData(Object targetService, Exportable annotation, Map<String, String> queryParams) throws Exception {
        String methodName = annotation.findMethod();

        // Chercher méthode avec Pageable (préféré)
        Method method = findMethodWithPageable(targetService.getClass(), methodName);
        if (method != null) {
            return fetchWithPageable(targetService, method, queryParams);
        }

        // Fallback: méthode simple findAll()
        method = targetService.getClass().getMethod(methodName);
        Object result = method.invoke(targetService);

        if (result instanceof List) {
            return (List<Object>) result;
        }

        log.warn("findMethod '{}' returned unexpected type: {}", methodName, result.getClass());
        return List.of();
    }

    /**
     * Récupère données avec Pageable et filtres.
     */
    private List<Object> fetchWithPageable(Object service, Method method, Map<String, String> queryParams) throws Exception {
        // Construire les paramètres
        Object[] params = buildMethodParams(method, queryParams);

        Object result = method.invoke(service, params);

        if (result instanceof Page) {
            return ((Page<?>) result).getContent();
        } else if (result instanceof List) {
            return (List<Object>) result;
        }

        return List.of();
    }

    /**
     * Construit les paramètres pour la méthode (avec auto-mapping).
     */
    private Object[] buildMethodParams(Method method, Map<String, String> queryParams) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] params = new Object[paramTypes.length];

        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> type = paramTypes[i];

            // Pageable.unpaged() par défaut (export tout)
            if (type == Pageable.class) {
                Integer page = parseInt(queryParams.get("page"));
                Integer size = parseInt(queryParams.get("size"));

                if (page != null && size != null) {
                    params[i] = PageRequest.of(page, size);
                } else {
                    params[i] = Pageable.unpaged();  // TOUS les résultats!
                }
            }
            // String params
            else if (type == String.class) {
                String paramName = guessParamName(method, i);
                params[i] = queryParams.get(paramName);
            }
            // Boolean params
            else if (type == Boolean.class || type == boolean.class) {
                String paramName = guessParamName(method, i);
                String value = queryParams.get(paramName);
                params[i] = value != null ? Boolean.parseBoolean(value) : null;
            }
            // Map<String, Object> pour filtres génériques
            else if (type == Map.class) {
                params[i] = new HashMap<>(queryParams);
            }
        }

        return params;
    }

    /**
     * Devine le nom du paramètre (search, role, isActive, etc.).
     */
    private String guessParamName(Method method, int index) {
        // Simplification: on pourrait utiliser @Parameter annotations
        String[] commonNames = {"search", "role", "typeUser", "isActive", "status"};
        return index < commonNames.length ? commonNames[index] : "param" + index;
    }

    /**
     * Parse Integer depuis String.
     */
    private Integer parseInt(String value) {
        try {
            return value != null ? Integer.parseInt(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Cherche une méthode avec Pageable.
     */
    private Method findMethodWithPageable(Class<?> clazz, String name) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                for (Class<?> paramType : method.getParameterTypes()) {
                    if (paramType == Pageable.class) {
                        return method;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Exporte vers Excel avec styles.
     */
    private byte[] exportToExcel(List<Object> data, Exportable annotation) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Export");

        // Styles
        Map<String, CellStyle> styles = createStyles(workbook, annotation);

        // Headers (ligne 1)
        Row headerRow = sheet.createRow(0);
        String[] fields = annotation.fields();
        for (int i = 0; i < fields.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(fields[i]);
            cell.setCellStyle(styles.get("header"));
        }

        // Data (lignes 2+)
        for (int i = 0; i < data.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Object entity = data.get(i);

            for (int j = 0; j < fields.length; j++) {
                Cell cell = row.createCell(j);
                Object value = getFieldValue(entity, fields[j]);
                setCellValue(cell, value);

                // Lignes alternées
                if (i % 2 == 0) {
                    cell.setCellStyle(styles.get("even"));
                } else {
                    cell.setCellStyle(styles.get("odd"));
                }
            }
        }

        // Auto-size colonnes
        for (int i = 0; i < fields.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Figer headers
        sheet.createFreezePane(0, 1);

        // Écrire dans ByteArray
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        return out.toByteArray();
    }

    /**
     * Crée les styles pour Excel.
     */
    private Map<String, CellStyle> createStyles(Workbook workbook, Exportable annotation) {
        Map<String, CellStyle> styles = new HashMap<>();

        // Header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        styles.put("header", headerStyle);

        // Even row style
        CellStyle evenStyle = workbook.createCellStyle();
        evenStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        evenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("even", evenStyle);

        // Odd row style
        CellStyle oddStyle = workbook.createCellStyle();
        oddStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        oddStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("odd", oddStyle);

        return styles;
    }

    /**
     * Exporte vers CSV.
     */
    private byte[] exportToCsv(List<Object> data, Exportable annotation) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(out);

        String[] fields = annotation.fields();

        // Headers
        writer.write(String.join(",", fields) + "\n");

        // Data
        for (Object entity : data) {
            List<String> values = new ArrayList<>();
            for (String field : fields) {
                Object value = getFieldValue(entity, field);
                values.add(value != null ? value.toString() : "");
            }
            writer.write(String.join(",", values) + "\n");
        }

        writer.flush();
        writer.close();

        return out.toByteArray();
    }

    /**
     * Récupère la valeur d'un champ (support nested: "user.firstName").
     */
    private Object getFieldValue(Object object, String fieldPath) {
        try {
            if (fieldPath.contains(".")) {
                String[] parts = fieldPath.split("\\.", 2);
                Object nested = getFieldValue(object, parts[0]);
                return nested != null ? getFieldValue(nested, parts[1]) : null;
            }

            Field field = object.getClass().getDeclaredField(fieldPath);
            field.setAccessible(true);
            return field.get(object);
        } catch (Exception e) {
            log.warn("Could not get field '{}' from {}", fieldPath, object.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * Définit la valeur d'une cellule selon le type.
     */
    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof LocalDate) {
            cell.setCellValue(((LocalDate) value).format(DATE_FORMATTER));
        } else if (value instanceof LocalDateTime) {
            cell.setCellValue(((LocalDateTime) value).format(DATETIME_FORMATTER));
        } else {
            cell.setCellValue(value.toString());
        }
    }
}
