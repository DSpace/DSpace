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

    /**
     * Recognises the DOI forms published by CrossRef.
     * <p>
     * Two constructs in the third alternative were rewritten because they let a crafted query
     * drive matching into cubic time (CodeQL <code>java/polynomial-redos</code>).
     * <code>\d+X?(\d+)\d+</code> became the equivalent but unambiguous
     * <code>(?:\d{3,}|\d+X\d{2,})</code>: with the optional <code>X</code> absent it is three
     * adjacent unbounded digit runs, and with it present two. The unescaped dots in
     * <code>\d+.\d+.\w+</code> became literal dots, which is what the version segment of a
     * legacy SICI DOI (for example <code>3.0.CO;2</code>) always meant.
     */
    private static final Pattern PATTERN = Pattern.compile("10.\\d{4,9}/[-._;()/:A-Z0-9]+" +
                                                               "|10.1002/[^\\s]+" +
                                                               "|10.\\d{4}/\\d+-(?:\\d{3,}|\\d+X\\d{2,})" +
                                                               "<[\\d\\w]+:[\\d\\w]*>\\d+\\.\\d+\\.\\w+;\\d" +
                                                               "|10.1021/\\w\\w\\d++" +
                                                               "|10.1207/[\\w\\d]+\\&\\d+_\\d+",
                                                           Pattern.CASE_INSENSITIVE);


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

        // return only the normalized DOI, e.g. 10.1038/s41561-018-0221-6
        return value.trim();
    }

    private static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return value;
        }
    }

}
