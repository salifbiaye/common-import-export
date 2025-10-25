package com.crm_bancaire.common.importexport.annotation;

import com.crm_bancaire.common.importexport.enums.ExportFormat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marque un service comme exportable, générant automatiquement
 * l'endpoint GET /{entity}/export.
 *
 * Usage:
 * <pre>
 * {@code
 * @Service
 * @Exportable(
 *     entity = "User",
 *     fields = {"firstName", "lastName", "email", "telephone"},
 *     filename = "users-export",
 *     columnStyles = {
 *         "firstName: width=20",
 *         "email: width=30",
 *         "isActive: color=GREEN|RED"
 *     }
 * )
 * public class UserService {
 *     // Vos méthodes normales - rien à changer!
 * }
 * }
 * </pre>
 *
 * Génère automatiquement:
 * - GET /api/users/export?format=xlsx
 * - Supporte filtrage et pagination via query params
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Exportable {

    /**
     * Nom de l'entité (utilisé pour les URLs et messages).
     * Ex: "User" → /api/users/export
     */
    String entity();

    /**
     * Champs à inclure dans l'export (dans l'ordre).
     * Supporte les champs imbriqués avec notation point: "address.city"
     *
     * Ex: {"firstName", "lastName", "email", "personalIdentity.telephone"}
     */
    String[] fields();

    /**
     * Nom du fichier exporté (sans extension).
     * Ex: "users-export" → users-export-2024-10-24.xlsx
     *
     * Par défaut: {entity}-export
     */
    String filename() default "";

    /**
     * Format par défaut si non spécifié dans la requête.
     * Par défaut: XLSX
     */
    ExportFormat defaultFormat() default ExportFormat.XLSX;

    /**
     * Configuration des styles des colonnes.
     *
     * Syntaxe simple:
     * <pre>
     * columnStyles = {
     *     "firstName: width=20",
     *     "email: width=30, bold=true",
     *     "balance: width=15, align=RIGHT, format=#,##0.00, color=GREEN|RED",
     *     "status: color=GREEN|ORANGE|RED, mapping=ACTIVE:GREEN,PENDING:ORANGE,CLOSED:RED"
     * }
     * </pre>
     *
     * Propriétés disponibles:
     * - width: Largeur colonne (nombre)
     * - color: Couleur texte (RED, GREEN, BLUE, etc.)
     * - bg: Couleur fond (LIGHT_GREEN, LIGHT_RED, etc.)
     * - bold: Texte gras (true/false)
     * - align: Alignement (LEFT, CENTER, RIGHT)
     * - format: Format nombre/date (#,##0.00, dd/MM/yyyy, etc.)
     * - mapping: Mapping valeur→couleur (KEY:COLOR,...)
     */
    String[] columnStyles() default {};

    /**
     * Méthode du service à appeler pour récupérer les données.
     * Par défaut: "findAll" → Cherche findAll(), getAll(), etc.
     */
    String findMethod() default "findAll";
}
