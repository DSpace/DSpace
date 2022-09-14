/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/* Created for LINDAT/CLARIAH-CZ (UFAL) */
package org.dspace.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class is taken from UFAL-clarin.
 * https://github.com/ufal/clarin-dspace/blob
 * /si-master-origin/dspace-api/src/main/java/cz/cuni/mff/ufal/IsoLangCodes.java
 */
public class IsoLangCodes {

    public static final String LANG_CODES_FILE = "lang_codes.txt";

    /**
     * Language codes in LANG_CODES_FILE are expected in format Language:code.
     * Therefore separator is ":".
     */
    public static final String LANG_CODE_SEPARATOR = ":";
    /**
     * Language codes in LANG_CODES_FILE are expected in format Language:code.
     * Therefore there must be 2 parts after separating by ":".
     */
    private static final int EXPECTED_PARTS_OF_ISO_LANG_CODE = 2;

    /**
     * Class that provides language codes from file LANG_CODES_FILE
     */
    private IsoLangCodes() {
    }

    /** log4j logger */
    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger
            .getLogger(IsoLangCodes.class);

    private static Map<String, String> isoLanguagesMap = null;

    static {
        getLangMap();
    }

    /**
     * @return map with language codes and languages. If called for the first time, builds the map.
     */
    private static Map<String, String> getLangMap() {
        if (isoLanguagesMap == null) {
            synchronized (IsoLangCodes.class) {
                isoLanguagesMap = buildMap();
            }
        }
        return isoLanguagesMap;
    }

    /**
     * Builds language code map from file LANG_CODES_FILE
     *
     *
     * @return map with language codes and languages
     */
    private static Map<String, String> buildMap() {
        Map<String, String> map = new HashMap<String, String>();
        final InputStream langCodesInputStream = Thread.currentThread()
                .getContextClassLoader().getResourceAsStream(LANG_CODES_FILE);
        if (!Objects.nonNull(langCodesInputStream)) {
            return map;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(langCodesInputStream,
                StandardCharsets.UTF_8))) {
            String line;
            boolean loading = false;
            while ((line = reader.readLine()) != null) {
                if (!loading) {
                    if (line.equals("==start==")) {
                        loading = true;
                    }
                } else {
                    String[] splitted = line.split(LANG_CODE_SEPARATOR);
                    if (!(splitted.length == EXPECTED_PARTS_OF_ISO_LANG_CODE)) {
                        log.warn("Bad string: " + line + " in " + LANG_CODES_FILE);
                        map.put("", "");
                    } else {
                        map.put(splitted[1], splitted[0]);
                    }
                }
            }
        } catch (IOException e) {
            log.error(e);
        }

        return map;
    }

    /**
     * @param langCode language code
     * @return Language for given code
     */
    public static String getLangForCode(String langCode) {
        return getLangMap().get(langCode);
    }

}
