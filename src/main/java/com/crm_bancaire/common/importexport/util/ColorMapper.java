package com.crm_bancaire.common.importexport.util;

import org.apache.poi.ss.usermodel.IndexedColors;

import java.util.HashMap;
import java.util.Map;

/**
 * Mappe les noms de couleurs simples vers les codes POI.
 */
public class ColorMapper {

    private static final Map<String, IndexedColors> COLOR_MAP = new HashMap<>();

    static {
        // Couleurs basiques
        COLOR_MAP.put("RED", IndexedColors.RED);
        COLOR_MAP.put("GREEN", IndexedColors.GREEN);
        COLOR_MAP.put("BLUE", IndexedColors.BLUE);
        COLOR_MAP.put("YELLOW", IndexedColors.YELLOW);
        COLOR_MAP.put("ORANGE", IndexedColors.ORANGE);
        COLOR_MAP.put("PURPLE", IndexedColors.VIOLET);
        COLOR_MAP.put("GRAY", IndexedColors.GREY_50_PERCENT);
        COLOR_MAP.put("GREY", IndexedColors.GREY_50_PERCENT);
        COLOR_MAP.put("WHITE", IndexedColors.WHITE);
        COLOR_MAP.put("BLACK", IndexedColors.BLACK);

        // Couleurs claires (backgrounds)
        COLOR_MAP.put("LIGHT_GREEN", IndexedColors.LIGHT_GREEN);
        COLOR_MAP.put("LIGHT_RED", IndexedColors.ROSE);
        COLOR_MAP.put("LIGHT_BLUE", IndexedColors.LIGHT_BLUE);
        COLOR_MAP.put("LIGHT_YELLOW", IndexedColors.LIGHT_YELLOW);
        COLOR_MAP.put("LIGHT_ORANGE", IndexedColors.LIGHT_ORANGE);
        COLOR_MAP.put("LIGHT_GRAY", IndexedColors.GREY_25_PERCENT);
    }

    /**
     * Récupère la couleur POI depuis un nom simple.
     *
     * @param colorName Nom de la couleur (ex: "GREEN", "LIGHT_BLUE")
     * @return IndexedColors ou null si non trouvé
     */
    public static IndexedColors get(String colorName) {
        if (colorName == null) {
            return null;
        }
        return COLOR_MAP.get(colorName.toUpperCase());
    }

    /**
     * Récupère le short index de la couleur.
     *
     * @param colorName Nom de la couleur
     * @return Short index ou -1 si non trouvé
     */
    public static short getIndex(String colorName) {
        IndexedColors color = get(colorName);
        return color != null ? color.getIndex() : -1;
    }
}
