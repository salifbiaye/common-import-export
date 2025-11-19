package com.crm_bancaire.common.importexport.controller;

import com.crm_bancaire.common.importexport.config.ImportExportRegistry;
import com.crm_bancaire.common.importexport.dto.ImportResponse;
import com.crm_bancaire.common.importexport.enums.ExportFormat;
import com.crm_bancaire.common.importexport.service.ExportService;
import com.crm_bancaire.common.importexport.service.ImportService;
import com.crm_bancaire.common.importexport.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

/**
 * Controller REST pour import/export automatique.
 *
 * Génère automatiquement les endpoints pour tous les services annotés:
 * - POST /api/{entity}/import
 * - GET /api/{entity}/import/template
 * - GET /api/{entity}/export
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ImportExportController {

    private final ImportExportRegistry registry;
    private final ImportService importService;
    private final ExportService exportService;
    private final TemplateService templateService;

    /**
     * Upload et importe un fichier Excel/CSV.
     *
     * POST /api/{entity}/import
     */
    @PostMapping("/{entity}/import")
    public ResponseEntity<ImportResponse> importFile(
            @PathVariable String entity,
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Import request for entity '{}', file: {}", entity, file.getOriginalFilename());

        // Vérifier que l'entité est importable
        if (!registry.isImportable(entity)) {
            return ResponseEntity.notFound().build();
        }

        // Récupérer config
        ImportExportRegistry.ImportConfig config = registry.getImportConfig(entity);

        // Effectuer l'import
        ImportResponse response = importService.importFile(
            file,
            config.getService(),
            config.getMapper(),
            config.getAnnotation()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Télécharge un template d'import (Excel ou CSV).
     *
     * GET /api/{entity}/import/template?format=xlsx
     */
    @GetMapping("/{entity}/import/template")
    public ResponseEntity<byte[]> downloadTemplate(
            @PathVariable String entity,
            @RequestParam(defaultValue = "xlsx") String format
    ) {
        log.info("Template request for entity '{}', format: {}", entity, format);

        // Vérifier que l'entité est importable
        if (!registry.isImportable(entity)) {
            log.warn("Entity '{}' is not importable", entity);
            return ResponseEntity.notFound().build();
        }

        try {
            log.info("Retrieving import config for entity '{}'", entity);
            // Récupérer config
            ImportExportRegistry.ImportConfig config = registry.getImportConfig(entity);

            if (config == null) {
                log.error("Import config is null for entity '{}'", entity);
                return ResponseEntity.notFound().build();
            }

            log.info("Config retrieved: service={}, mapper={}",
                    config.getService().getClass().getSimpleName(),
                    config.getMapper().getClass().getSimpleName());

            // Générer template
            ExportFormat exportFormat = "csv".equalsIgnoreCase(format) ? ExportFormat.CSV : ExportFormat.XLSX;
            log.info("Calling templateService.generateTemplate() with format: {}", exportFormat);

            byte[] template = templateService.generateTemplate(
                    config.getMapper(),
                    config.getAnnotation(),
                    exportFormat
            );

            log.info("Template generated successfully, size: {} bytes", template.length);

            // Préparer réponse
            String filename = entity + "-import-template" + exportFormat.getExtension();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(exportFormat.getContentType()));
            headers.setContentDispositionFormData("attachment", filename);

            log.info("Sending response with filename: {}", filename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(template);

        } catch (Exception e) {
            log.error("Error generating template for entity '{}': {}", entity, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Exporte les données vers Excel ou CSV.
     *
     * GET /api/{entity}/export?format=xlsx&isActive=true
     */
    @GetMapping("/{entity}/export")
    public ResponseEntity<byte[]> export(
            @PathVariable String entity,
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam Map<String, String> queryParams
    ) {
        log.info("Export request for entity '{}', format: {}, params: {}", entity, format, queryParams);

        // Vérifier que l'entité est exportable
        if (!registry.isExportable(entity)) {
            return ResponseEntity.notFound().build();
        }

        try {
            // Récupérer config
            ImportExportRegistry.ExportConfig config = registry.getExportConfig(entity);

            // Effectuer l'export
            ExportFormat exportFormat = "csv".equalsIgnoreCase(format) ? ExportFormat.CSV : ExportFormat.XLSX;
            byte[] data = exportService.export(
                config.getService(),
                config.getAnnotation(),
                exportFormat,
                queryParams
            );

            // Préparer réponse
            String filename = config.getAnnotation().filename();
            if (filename.isEmpty()) {
                filename = entity + "-export";
            }
            filename = filename + "-" + LocalDate.now() + exportFormat.getExtension();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(exportFormat.getContentType()));
            headers.setContentDispositionFormData("attachment", filename);

            return ResponseEntity.ok()
                .headers(headers)
                .body(data);

        } catch (Exception e) {
            log.error("Error exporting entity '{}'", entity, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
