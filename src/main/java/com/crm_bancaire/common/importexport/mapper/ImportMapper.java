package com.crm_bancaire.common.importexport.mapper;

import java.util.List;
import java.util.Map;

/**
 * Interface à implémenter pour mapper les lignes Excel/CSV vers des entités.
 *
 * @param <T> Type de l'entité
 *
 * Usage:
 * <pre>
 * {@code
 * @Component
 * public class UserImportMapper implements ImportMapper<User> {
 *
 *     @Override
 *     public User mapRow(Map<String, String> row, int rowNumber) {
 *         User user = new User();
 *         user.setFirstName(row.get("firstName"));
 *         user.setLastName(row.get("lastName"));
 *         user.setEmail(row.get("email"));
 *         return user;
 *     }
 *
 *     @Override
 *     public List<String> getRequiredColumns() {
 *         return List.of("firstName", "lastName", "email");
 *     }
 *
 *     @Override
 *     public User getExampleRow() {
 *         User example = new User();
 *         example.setFirstName("John");
 *         example.setLastName("Doe");
 *         example.setEmail("john@example.com");
 *         return example;
 *     }
 * }
 * }
 * </pre>
 */
public interface ImportMapper<T> {

    /**
     * Mappe une ligne du fichier Excel/CSV vers une entité.
     *
     * @param row Map contenant les valeurs des colonnes (key = nom colonne, value = valeur)
     * @param rowNumber Numéro de la ligne (commence à 2 car ligne 1 = headers)
     * @return L'entité mappée (non sauvegardée)
     * @throws Exception Si le mapping échoue (email invalide, données manquantes, etc.)
     */
    T mapRow(Map<String, String> row, int rowNumber) throws Exception;

    /**
     * Retourne la liste des colonnes obligatoires.
     * Utilisé pour valider que le fichier contient tous les headers nécessaires.
     *
     * @return Liste des noms de colonnes obligatoires
     */
    List<String> getRequiredColumns();

    /**
     * Retourne les colonnes optionnelles (pour le template).
     *
     * @return Liste des noms de colonnes optionnelles (peut être vide)
     */
    default List<String> getOptionalColumns() {
        return List.of();
    }

    /**
     * Retourne une entité exemple pour la génération du template.
     * Ligne 2 du template contiendra les valeurs de cet exemple.
     *
     * @return Entité exemple avec des valeurs réalistes
     */
    T getExampleRow();

    /**
     * Valide une entité après mapping (optionnel).
     * Par défaut, utilise Bean Validation (@NotNull, @Email, etc.)
     *
     * Vous pouvez override pour ajouter des validations custom.
     *
     * @param entity L'entité à valider
     * @param rowNumber Numéro de la ligne
     * @throws Exception Si la validation échoue
     */
    default void validate(T entity, int rowNumber) throws Exception {
        // Par défaut, pas de validation custom
        // La validation Bean sera faite automatiquement
    }
}
