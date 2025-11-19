package com.crm_bancaire.common.importexport.config;

import com.crm_bancaire.common.importexport.annotation.Exportable;
import com.crm_bancaire.common.importexport.annotation.Importable;
import com.crm_bancaire.common.importexport.mapper.ImportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

/**
 * Scanner qui détecte automatiquement les services annotés avec @Importable/@Exportable
 * au démarrage et les enregistre dans le registry.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImportExportBeanPostProcessor implements BeanPostProcessor {

    private final ImportExportRegistry registry;
    private final ApplicationContext applicationContext;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // Récupérer la classe cible (pas le proxy!)
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);

        // Vérifier @Importable sur la classe cible
        Importable importable = AnnotationUtils.findAnnotation(targetClass, Importable.class);
        if (importable != null) {
            registerImportable(bean, importable, targetClass);
        }

        // Vérifier @Exportable sur la classe cible
        Exportable exportable = AnnotationUtils.findAnnotation(targetClass, Exportable.class);
        if (exportable != null) {
            registerExportable(bean, exportable, targetClass);
        }

        return bean;
    }

    /**
     * Enregistre un service @Importable.
     */
    private void registerImportable(Object service, Importable annotation, Class<?> targetClass) {
        try {
            String entity = annotation.entity();

            // Récupérer le mapper depuis le contexte Spring
            Class<? extends ImportMapper> mapperClass = annotation.mapper();
            ImportMapper<?> mapper = applicationContext.getBean(mapperClass);

            // Enregistrer dans le registry
            registry.registerImportable(entity, service, annotation, mapper);

            log.info("✅ @Importable registered: {} → POST /api/{}/import",
                targetClass.getSimpleName(), entity);

        } catch (Exception e) {
            log.error("❌ Failed to register @Importable for {}: {}",
                targetClass.getSimpleName(), e.getMessage(), e);
        }
    }

    /**
     * Enregistre un service @Exportable.
     */
    private void registerExportable(Object service, Exportable annotation, Class<?> targetClass) {
        try {
            String entity = annotation.entity();

            // Enregistrer dans le registry
            registry.registerExportable(entity, service, annotation);

            log.info("✅ @Exportable registered: {} → GET /api/{}/export",
                targetClass.getSimpleName(), entity);

        } catch (Exception e) {
            log.error("❌ Failed to register @Exportable for {}: {}",
                targetClass.getSimpleName(), e.getMessage(), e);
        }
    }
}
