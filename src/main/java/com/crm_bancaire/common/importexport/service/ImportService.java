package com.crm_bancaire.common.importexport.service;

import com.crm_bancaire.common.importexport.annotation.Importable;
import com.crm_bancaire.common.importexport.dto.ImportError;
import com.crm_bancaire.common.importexport.dto.ImportResponse;
import com.crm_bancaire.common.importexport.enums.FailureStrategy;
import com.crm_bancaire.common.importexport.mapper.ImportMapper;
import com.crm_bancaire.common.importexport.parser.FileParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Service pour gérer l'import de fichiers Excel/CSV.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportService {

    private final List<FileParser> parsers;
    private final Validator validator;

    /**
     * Importe un fichier Excel/CSV vers une liste d'entités.
     *
     * @param file Fichier uploadé
     * @param targetService Service cible (avec @Importable)
     * @param mapper ImportMapper pour convertir les lignes
     * @param annotation Annotation @Importable
     * @return ImportResponse avec statistiques et erreurs
     */
    public ImportResponse importFile(
            MultipartFile file,
            Object targetService,
            ImportMapper<?> mapper,
            Importable annotation
    ) {
        Instant start = Instant.now();
        log.info("Starting import for entity '{}' from file '{}'", annotation.entity(), file.getOriginalFilename());

        try {
            // 1. Trouver le bon parser
            FileParser parser = findParser(file.getOriginalFilename());
            if (parser == null) {
                return ImportResponse.builder()
                    .success(false)
                    .message("Format de fichier non supporté: " + file.getOriginalFilename())
                    .build();
            }

            // 2. Parser le fichier
            List<Map<String, String>> rows = parser.parse(file);
            log.info("Parsed {} rows from file", rows.size());

            // 3. Valider le nombre de lignes
            if (rows.size() > annotation.maxRows()) {
                return ImportResponse.builder()
                    .success(false)
                    .totalRows(rows.size())
                    .message("Trop de lignes dans le fichier. Maximum autorisé: " + annotation.maxRows())
                    .build();
            }

            // 4. Valider les headers
            Set<String> fileHeaders = rows.isEmpty() ? Set.of() : rows.get(0).keySet();
            List<String> requiredColumns = mapper.getRequiredColumns();
            List<String> missingColumns = requiredColumns.stream()
                .filter(col -> !fileHeaders.contains(col))
                .toList();

            if (!missingColumns.isEmpty()) {
                return ImportResponse.builder()
                    .success(false)
                    .message("Colonnes manquantes: " + String.join(", ", missingColumns))
                    .build();
            }

            // 5. Mapper et valider
            List<Object> entities = new ArrayList<>();
            List<ImportError> errors = new ArrayList<>();
            FailureStrategy strategy = annotation.failureStrategy();

            for (int i = 0; i < rows.size(); i++) {
                int rowNumber = i + 2; // Ligne 1 = headers, donc data commence à 2
                Map<String, String> row = rows.get(i);

                try {
                    // Mapper la ligne
                    Object entity = mapper.mapRow(row, rowNumber);

                    // Validation custom du mapper
                    mapper.validate(entity, rowNumber);

                    // Bean Validation
                    Set<ConstraintViolation<Object>> violations = validator.validate(entity);
                    if (!violations.isEmpty()) {
                        ConstraintViolation<Object> first = violations.iterator().next();
                        throw new IllegalArgumentException(first.getMessage());
                    }

                    entities.add(entity);

                } catch (Exception e) {
                    ImportError error = ImportError.builder()
                        .row(rowNumber)
                        .message(e.getMessage())
                        .build();
                    errors.add(error);

                    log.warn("Error at row {}: {}", rowNumber, e.getMessage());

                    // Stratégie de gestion d'erreur
                    if (strategy == FailureStrategy.FAIL_FAST) {
                        return buildResponse(start, rows.size(), 0, errors, false,
                            "Import arrêté à la ligne " + rowNumber + ": " + e.getMessage());
                    }
                }
            }

            // 6. COLLECT_ALL: Ne rien sauvegarder, juste retourner les erreurs
            if (strategy == FailureStrategy.COLLECT_ALL) {
                String message = errors.isEmpty()
                    ? "Validation réussie. Aucune donnée n'a été sauvegardée (mode COLLECT_ALL)."
                    : errors.size() + " erreur(s) trouvée(s). Aucune donnée n'a été sauvegardée (mode COLLECT_ALL).";

                return buildResponse(start, rows.size(), 0, errors, errors.isEmpty(), message);
            }

            // 7. Sauvegarder les entités valides (SKIP_ERRORS ou FAIL_FAST)
            int savedCount = 0;
            if (!entities.isEmpty()) {
                savedCount = saveEntities(entities, targetService, annotation);
            }

            // 8. Construire la réponse
            boolean success = errors.isEmpty() || strategy == FailureStrategy.SKIP_ERRORS;
            String message = buildSuccessMessage(rows.size(), savedCount, errors.size(), strategy);

            return buildResponse(start, rows.size(), savedCount, errors, success, message);

        } catch (Exception e) {
            log.error("Import failed for entity '{}'", annotation.entity(), e);
            return ImportResponse.builder()
                .success(false)
                .message("Erreur lors de l'import: " + e.getMessage())
                .duration(Duration.between(start, Instant.now()).toString())
                .build();
        }
    }

    /**
     * Trouve le parser approprié pour le fichier.
     */
    private FileParser findParser(String filename) {
        return parsers.stream()
            .filter(p -> p.supports(filename))
            .findFirst()
            .orElse(null);
    }

    /**
     * Sauvegarde les entités via la méthode save du service cible.
     */
    private int saveEntities(List<Object> entities, Object targetService, Importable annotation) {
        try {
            String methodName = annotation.saveMethod();
            int batchSize = annotation.batchSize();

            // Chercher saveAll d'abord (plus performant)
            Method saveAllMethod = findMethod(targetService.getClass(), methodName + "All", List.class);
            if (saveAllMethod != null) {
                return saveInBatches(entities, targetService, saveAllMethod, batchSize);
            }

            // Fallback: méthode save unitaire
            Method saveMethod = findMethod(targetService.getClass(), methodName, Object.class);
            if (saveMethod == null) {
                log.error("No save method found: {} or {}All", methodName, methodName);
                return 0;
            }

            return saveOneByOne(entities, targetService, saveMethod);

        } catch (Exception e) {
            log.error("Error saving entities", e);
            return 0;
        }
    }

    /**
     * Sauvegarde par batches (saveAll).
     */
    private int saveInBatches(List<Object> entities, Object service, Method saveAllMethod, int batchSize) throws Exception {
        int saved = 0;
        for (int i = 0; i < entities.size(); i += batchSize) {
            int end = Math.min(i + batchSize, entities.size());
            List<Object> batch = entities.subList(i, end);
            saveAllMethod.invoke(service, batch);
            saved += batch.size();
            log.debug("Saved batch {}/{}: {} entities", (i / batchSize) + 1, (entities.size() / batchSize) + 1, batch.size());
        }
        return saved;
    }

    /**
     * Sauvegarde un par un (save).
     */
    private int saveOneByOne(List<Object> entities, Object service, Method saveMethod) throws Exception {
        for (Object entity : entities) {
            saveMethod.invoke(service, entity);
        }
        return entities.size();
    }

    /**
     * Cherche une méthode par nom et type de paramètre.
     */
    private Method findMethod(Class<?> clazz, String name, Class<?> paramType) {
        try {
            return clazz.getMethod(name, paramType);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Construit le message de succès.
     */
    private String buildSuccessMessage(int total, int saved, int errorCount, FailureStrategy strategy) {
        if (errorCount == 0) {
            return String.format("Import réussi: %d/%d lignes importées", saved, total);
        }

        if (strategy == FailureStrategy.SKIP_ERRORS) {
            return String.format("Import partiel: %d/%d lignes importées, %d erreur(s)", saved, total, errorCount);
        }

        return String.format("%d erreur(s) détectée(s)", errorCount);
    }

    /**
     * Construit la réponse finale.
     */
    private ImportResponse buildResponse(Instant start, int total, int saved, List<ImportError> errors, boolean success, String message) {
        return ImportResponse.builder()
            .success(success)
            .totalRows(total)
            .successCount(saved)
            .errorCount(errors.size())
            .errors(errors)
            .message(message)
            .duration(Duration.between(start, Instant.now()).toString())
            .build();
    }
}
