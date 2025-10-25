package com.crm_bancaire.common.importexport.util;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Parse les configurations de style simplifiées.
 *
 * Syntaxe: "width=20, color=GREEN, bold=true"
 * Résultat: Map {width: "20", color: "GREEN", bold: "true"}
 */
@Slf4j
public class StyleParser {

    /**
     * Parse une configuration de style depuis un String.
     *
     * @param styleString String de config (ex: "width=20, color=GREEN|RED")
     * @return Map des propriétés
     */
    public static Map<String, String> parse(String styleString) {
        Map<String, String> result = new HashMap<>();

        if (styleString == null || styleString.trim().isEmpty()) {
            return result;
        }

        String[] parts = styleString.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.contains("=")) {
                String[] keyValue = trimmed.split("=", 2);
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                result.put(key, value);
            }
        }

        return result;
    }

    /**
     * Parse une ligne de columnStyles.
     *
     * Format: "columnName: width=20, color=GREEN"
     *
     * @param columnStyleLine Ligne de config
     * @return Map {columnName: Map{width: "20", color: "GREEN"}}
     */
    public static Map.Entry<String, Map<String, String>> parseColumnStyle(String columnStyleLine) {
        if (columnStyleLine == null || !columnStyleLine.contains(":")) {
            return null;
        }

        String[] parts = columnStyleLine.split(":", 2);
        String columnName = parts[0].trim();
        String styleString = parts.length > 1 ? parts[1].trim() : "";

        Map<String, String> style = parse(styleString);

        return Map.entry(columnName, style);
    }

    /**
     * Parse toutes les columnStyles d'une annotation.
     *
     * @param columnStyles Array de strings depuis @Exportable
     * @return Map {columnName: styleConfig}
     */
    public static Map<String, Map<String, String>> parseAllColumnStyles(String[] columnStyles) {
        Map<String, Map<String, String>> result = new HashMap<>();

        if (columnStyles == null) {
            return result;
        }

        for (String columnStyleLine : columnStyles) {
            var entry = parseColumnStyle(columnStyleLine);
            if (entry != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Helper: Get int value from style map.
     */
    public static int getInt(Map<String, String> style, String key, int defaultValue) {
        try {
            return Integer.parseInt(style.getOrDefault(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            log.warn("Invalid int value for key {}: {}", key, style.get(key));
            return defaultValue;
        }
    }

    /**
     * Helper: Get boolean value from style map.
     */
    public static boolean getBool(Map<String, String> style, String key, boolean defaultValue) {
        String value = style.get(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    /**
     * Helper: Get String value from style map.
     */
    public static String getString(Map<String, String> style, String key, String defaultValue) {
        return style.getOrDefault(key, defaultValue);
    }
}
