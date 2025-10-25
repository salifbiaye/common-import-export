package com.crm_bancaire.common.importexport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Représente une erreur survenue lors de l'import d'une ligne.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportError {

    /**
     * Numéro de la ligne en erreur (commence à 2 car ligne 1 = headers)
     */
    private int row;

    /**
     * Nom du champ en erreur (optionnel)
     */
    private String field;

    /**
     * Valeur qui a causé l'erreur (optionnel)
     */
    private String value;

    /**
     * Message d'erreur explicite
     */
    private String message;

    /**
     * Constructor simplifié pour message seulement
     */
    public ImportError(int row, String message) {
        this.row = row;
        this.message = message;
    }
}
