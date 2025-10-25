package com.crm_bancaire.common.importexport.config;

import com.crm_bancaire.common.importexport.annotation.Exportable;
import com.crm_bancaire.common.importexport.annotation.Importable;
import com.crm_bancaire.common.importexport.mapper.ImportMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registre global pour stocker la configuration des services @Importable et @Exportable.
 */
@Component
@Slf4j
public class ImportExportRegistry {

    private final Map<String, ImportConfig> importConfigs = new ConcurrentHashMap<>();
    private final Map<String, ExportConfig> exportConfigs = new ConcurrentHashMap<>();

    /**
     * Enregistre un service avec @Importable.
     */
    public void registerImportable(String entity, Object service, Importable annotation, ImportMapper<?> mapper) {
        ImportConfig config = new ImportConfig();
        config.setEntity(entity);
        config.setService(service);
        config.setAnnotation(annotation);
        config.setMapper(mapper);

        importConfigs.put(entity, config);
        log.info("Registered @Importable for entity: {}", entity);
    }

    /**
     * Enregistre un service avec @Exportable.
     */
    public void registerExportable(String entity, Object service, Exportable annotation) {
        ExportConfig config = new ExportConfig();
        config.setEntity(entity);
        config.setService(service);
        config.setAnnotation(annotation);

        exportConfigs.put(entity, config);
        log.info("Registered @Exportable for entity: {}", entity);
    }

    /**
     * Récupère la config d'import pour une entité.
     */
    public ImportConfig getImportConfig(String entity) {
        return importConfigs.get(entity);
    }

    /**
     * Récupère la config d'export pour une entité.
     */
    public ExportConfig getExportConfig(String entity) {
        return exportConfigs.get(entity);
    }

    /**
     * Vérifie si une entité est importable.
     */
    public boolean isImportable(String entity) {
        return importConfigs.containsKey(entity);
    }

    /**
     * Vérifie si une entité est exportable.
     */
    public boolean isExportable(String entity) {
        return exportConfigs.containsKey(entity);
    }

    /**
     * Configuration pour @Importable.
     */
    @Data
    public static class ImportConfig {
        private String entity;
        private Object service;
        private Importable annotation;
        private ImportMapper<?> mapper;
    }

    /**
     * Configuration pour @Exportable.
     */
    @Data
    public static class ExportConfig {
        private String entity;
        private Object service;
        private Exportable annotation;
    }
}
