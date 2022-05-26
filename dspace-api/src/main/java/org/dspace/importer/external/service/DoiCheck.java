/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class that provides methods to check if a given string is a DOI
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class DoiCheck {

    private static final List<String> DOI_PREFIXES = Arrays.asList("http://dx.doi.org/", "https://dx.doi.org/");

    private static final Pattern PATTERN = Pattern.compile("10.\\d{4,9}/[-._;()/:A-Z0-9]+" +
                                                               "|10.1002/[^\\s]+" +
                                                               "|10.\\d{4}/\\d+-\\d+X?(\\d+)" +
                                                               "\\d+<[\\d\\w]+:[\\d\\w]*>\\d+.\\d+.\\w+;\\d" +
                                                               "|10.1021/\\w\\w\\d++" +
                                                               "|10.1207/[\\w\\d]+\\&\\d+_\\d+",
                                                           Pattern.CASE_INSENSITIVE);

    private DoiCheck() {}

    public static boolean isDoi(final String value) {
        Matcher m = PATTERN.matcher(purgeDoiValue(value));
        return m.matches();
    }

    public static String purgeDoiValue(final String query) {
        String value = query.replaceAll(",", "");
        for (final String prefix : DOI_PREFIXES) {
            value = value.replaceAll(prefix, "");
        }
        return value.trim();
    }

}