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

/**
 *  Class is copied from the LINDAT/CLARIAH-CZ (This class is taken from UFAL-clarin.
 *  <a href="https://github.com/ufal/clarin-dspace/blob/clarin
 *  /dspace-oai/src/main/java/cz/cuni/mff/ufal/utils/LangUtil.java">...</a>) and modified by
 *
 *  @author Marian Berger (dspace at dataquest.sk)
 */
public class LangUtil {

    private LangUtil() {}
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger
            .getLogger(LangUtil.class);

    static final HashMap<String, Lang> idToLang;

    static {
        idToLang = new HashMap<>();
        final InputStream langCodesInputStream = LangUtil.class.getClassLoader()
                .getResourceAsStream("iso-639-3.tab");
        if (langCodesInputStream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(langCodesInputStream,
                    StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Lang lang = new Lang(line);
                    idToLang.put(lang.getId(), lang);
                    if (lang.getPart2B() != null) {
                        idToLang.put(lang.getPart2B(), lang);
                    }
                }
            } catch (IOException e) {
                log.error(e);
            }
        }
    }

    public static String getShortestId(String id) {
        Lang lang = idToLang.get(id);
        if (lang != null) {
            if (lang.getPart1() != null) {
                return lang.getPart1();
            } else {
                return lang.getId();
            }
        }
        return id;
    }

    public static void main(String[] args) {
        System.out.println(getShortestId("eng"));
        System.out.println(getShortestId("deu"));
        System.out.println(getShortestId("ger"));
        System.out.println(getShortestId("wtf"));
    }

    private static class Lang {
        private final String id;
        private final String part2B;
        //private final String part2T;
        private final String part1;
        /*private final String scope;
        private final String languageType;
        private final String refName;
        private final String comment;*/

        public Lang(String line) {
            String[] parts = line.split("\t", 8);
            id = parts[0];
            part2B = parts[1].isEmpty() ? null : parts[1];
            //part2T = parts[2];
            part1 = parts[3].isEmpty() ? null : parts[3];
        }

        public String getId() {
            return id;
        }

        public String getPart1() {
            return part1;
        }

        public String getPart2B() {
            return part2B;
        }
    }
}