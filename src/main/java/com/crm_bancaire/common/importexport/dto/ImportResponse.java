package com.crm_bancaire.common.importexport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Réponse de l'API d'import contenant le résultat et les erreurs détaillées.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResponse {

    /**
     * Indique si l'import s'est globalement bien passé
     */
    private boolean success;

    /**
     * Nombre total de lignes dans le fichier (hors header)
     */
    private int totalRows;

    /**
     * Nombre de lignes importées avec succès
     */
    private int successCount;

    /**
     * Nombre de lignes en erreur
     */
    private int errorCount;

    /**
     * Durée de l'import (ex: "2.3s")
     */
    private String duration;

    /**
     * Liste des erreurs détaillées (avec numéros de ligne)
     */
    @Builder.Default
    private List<ImportError> errors = new ArrayList<>();

    /**
     * Warnings (non bloquants)
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * Message global (optionnel)
     */
    private String message;
}
