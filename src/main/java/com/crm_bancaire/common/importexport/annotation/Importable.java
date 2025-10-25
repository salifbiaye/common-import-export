package com.crm_bancaire.common.importexport.annotation;

import com.crm_bancaire.common.importexport.enums.FailureStrategy;
import com.crm_bancaire.common.importexport.mapper.ImportMapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marque un service comme importable, générant automatiquement
 * l'endpoint POST /{entity}/import.
 *
 * Usage:
 * <pre>
 * {@code
 * @Service
 * @Importable(
 *     entity = "User",
 *     mapper = UserImportMapper.class,
 *     failureStrategy = FailureStrategy.SKIP_ERRORS,
 *     maxRows = 5000
 * )
 * public class UserService {
 *     // Vos méthodes normales - rien à changer!
 * }
 * }
 * </pre>
 *
 * Génère automatiquement:
 * - POST /api/users/import (multipart/form-data)
 * - GET /api/users/import/template?format=xlsx
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Importable {

    /**
     * Nom de l'entité (utilisé pour les URLs et messages).
     * Ex: "User" → /api/users/import
     */
    String entity();

    /**
     * Classe du mapper qui convertit les lignes Excel/CSV en entités.
     * Doit implémenter ImportMapper&lt;T&gt;.
     */
    Class<? extends ImportMapper<?>> mapper();

    /**
     * Stratégie en cas d'erreur lors de l'import.
     * Par défaut: SKIP_ERRORS (continue avec les lignes valides)
     */
    FailureStrategy failureStrategy() default FailureStrategy.SKIP_ERRORS;

    /**
     * Nombre maximum de lignes autorisées dans le fichier.
     * Par défaut: 5000 lignes (pour éviter les timeouts)
     */
    int maxRows() default 5000;

    /**
     * Taille des batchs pour l'insertion en base.
     * Par défaut: 100 (bon compromis performance/mémoire)
     */
    int batchSize() default 100;

    /**
     * Méthode du service à appeler pour sauvegarder les entités.
     * Par défaut: "save" → Cherche save(T), save(List&lt;T&gt;), saveAll(List&lt;T&gt;)
     */
    String saveMethod() default "save";
}
