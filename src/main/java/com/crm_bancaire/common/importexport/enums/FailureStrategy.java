package com.crm_bancaire.common.importexport.enums;

/**
 * Stratégie à suivre en cas d'erreur lors de l'import.
 */
public enum FailureStrategy {

    /**
     * Arrête l'import dès la première erreur rencontrée.
     * Toutes les données importées sont rollback (transaction).
     */
    FAIL_FAST,

    /**
     * Skip les lignes en erreur et continue l'import.
     * Seules les lignes valides sont sauvegardées.
     */
    SKIP_ERRORS,

    /**
     * Collecte toutes les erreurs sans arrêter l'import.
     * Aucune donnée n'est sauvegardée - mode validation seulement.
     */
    COLLECT_ALL
}
