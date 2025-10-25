package com.crm_bancaire.common.importexport.parser;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Interface pour parser des fichiers (Excel, CSV, etc.)
 */
public interface FileParser {

    /**
     * Parse un fichier et retourne les données sous forme de liste de Maps.
     *
     * @param file Fichier à parser
     * @return Liste de Maps (key = nom colonne, value = valeur)
     * @throws Exception Si le parsing échoue
     */
    List<Map<String, String>> parse(MultipartFile file) throws Exception;

    /**
     * Parse depuis un InputStream.
     *
     * @param inputStream Stream du fichier
     * @return Liste de Maps
     * @throws Exception Si le parsing échoue
     */
    List<Map<String, String>> parse(InputStream inputStream) throws Exception;

    /**
     * Vérifie si ce parser supporte le fichier donné.
     *
     * @param filename Nom du fichier
     * @return true si supporté
     */
    boolean supports(String filename);
}
