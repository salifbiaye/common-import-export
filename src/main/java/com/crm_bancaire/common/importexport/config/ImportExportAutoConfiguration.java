package com.crm_bancaire.common.importexport.config;

import com.crm_bancaire.common.importexport.parser.CsvParser;
import com.crm_bancaire.common.importexport.parser.ExcelParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration pour common-import-export.
 *
 * Active automatiquement les parsers et scanne les composants.
 */
@Slf4j
@AutoConfiguration
@ComponentScan(basePackages = "com.crm_bancaire.common.importexport")
public class ImportExportAutoConfiguration {

    public ImportExportAutoConfiguration() {
        log.info("🚀 common-import-export v1.0.0 activated!");
    }

    @Bean
    public ExcelParser excelParser() {
        return new ExcelParser();
    }

    @Bean
    public CsvParser csvParser() {
        return new CsvParser();
    }
}
