package com.crm_bancaire.common.importexport.dto;

import com.crm_bancaire.common.importexport.enums.ExportFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Requête d'export avec format et filtres.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {

    /**
     * Format d'export (xlsx ou csv)
     */
    private ExportFormat format;

    /**
     * Filtres à appliquer (ex: isActive=true, role=CLIENT)
     * Mappés automatiquement depuis les query params
     */
    @Builder.Default
    private Map<String, Object> filters = new HashMap<>();

    /**
     * Page (pour pagination, optionnel)
     */
    private Integer page;

    /**
     * Taille de page (pour pagination, optionnel)
     */
    private Integer size;

    /**
     * Champ de tri (optionnel)
     */
    private String sortBy;

    /**
     * Direction du tri: asc ou desc (optionnel)
     */
    private String sortDir;
}
