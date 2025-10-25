package com.crm_bancaire.common.importexport.enums;

/**
 * Formats de fichier supportés pour l'export.
 */
public enum ExportFormat {

    /**
     * Excel (.xlsx) - Format moderne avec styles
     */
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),

    /**
     * CSV - Format texte simple
     */
    CSV("text/csv", ".csv");

    private final String contentType;
    private final String extension;

    ExportFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String getContentType() {
        return contentType;
    }

    public String getExtension() {
        return extension;
    }

    /**
     * Parse format from string (case insensitive).
     *
     * @param format Format string ("xlsx", "csv", etc.)
     * @return ExportFormat or XLSX by default
     */
    public static ExportFormat fromString(String format) {
        if (format == null) {
            return XLSX;
        }

        try {
            return valueOf(format.toUpperCase());
        } catch (IllegalArgumentException e) {
            return XLSX;
        }
    }
}
