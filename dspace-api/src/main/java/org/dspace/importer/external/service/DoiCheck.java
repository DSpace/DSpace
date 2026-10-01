/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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

    private static final List<String> DOI_PREFIXES = Arrays.asList(
            "http://dx.doi.org/",
            "https://dx.doi.org/",
            "http://www-dx.doi.org/",
            "https://www-dx.doi.org/",
            "http://doi.org/",
            "https://doi.org/",
            "www.dx.doi.org/",
            "dx.doi.org/",
            "doi.org/",
            "doi:");

    private static final Pattern PATTERN = Pattern.compile("10.\\d{4,9}/[-._;()/:A-Z0-9]+" +
                                                               "|10.1002/[^\\s]+" +
                                                               "|10.\\d{4}/\\d+-\\d+X?(\\d+)" +
                                                               "\\d+<[\\d\\w]+:[\\d\\w]*>\\d+.\\d+.\\w+;\\d" +
                                                               "|10.1021/\\w\\w\\d++" +
                                                               "|10.1207/[\\w\\d]+\\&\\d+_\\d+",
                                                           Pattern.CASE_INSENSITIVE);

    private static final Pattern NON_DOI_PATH_SUFFIX = Pattern.compile(
            "(?i)/(pdf|epdf|fulltext|full|html|abstract|xml)$");

    private DoiCheck() {}

    // then run isDoi() against that normalized value
    public static boolean isDoi(final String value) {
        Matcher m = PATTERN.matcher(purgeDoiValue(value));
        return m.matches();
    }

    public static String purgeDoiValue(final String query) {
        // trim leading/trailing whitespace
        String value = query.trim();

        // URL-decode the DOI where needed, e.g. %2F → /
        value = urlDecode(value);

        // remove query-string fragments after the DOI, e.g. ?pdf=1, ?download=true
        int queryIdx = value.indexOf('?');
        if (queryIdx >= 0) {
            value = value.substring(0, queryIdx);
        }
        int fragmentIdx = value.indexOf('#');
        if (fragmentIdx >= 0) {
            value = value.substring(0, fragmentIdx);
        }

        // remove supported DOI resolver prefixes
        for (final String prefix : DOI_PREFIXES) {
            value = value.replaceAll("(?i)" + Pattern.quote(prefix), "");
        }

        // extract the DOI when it appears after a known URL/path prefix
        Matcher matcher = PATTERN.matcher(value);
        if (matcher.find()) {
            value = matcher.group();
        }

        // remove trailing punctuation copied from prose, e.g. ., ,
        value = value.replaceAll("[.,;:]+$", "");

        // remove non-DOI path suffixes after an otherwise valid DOI where applicable
        value = NON_DOI_PATH_SUFFIX.matcher(value).replaceAll("");

        // return only the normalized DOI, e.g. 10.1038/s41561-018-0221-6
        return value.trim();
    }

    private static String urlDecode(String value) {
        // decode repeatedly so double-encoding is handled, e.g. %252F → %2F → /
        String decoded = value;
        while (decoded.contains("%")) {
            decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8);
        }
        return decoded;
    }

}
