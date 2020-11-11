/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.crossref;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;


/**
 * Utility class that provides methods to check if a given string is a DOI and exists on CrossRef services
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class CrossRefDoiCheck {

    private static final List<String> DOI_PREFIXES = Arrays.asList("http://dx.doi.org/", "https://dx.doi.org/");

    private static final Pattern PATTERN = Pattern.compile("10.\\d{4,9}/[-._;()/:A-Z0-9]+" +
                                                               "|10.1002/[^\\s]+" +
                                                               "|10.\\d{4}/\\d+-\\d+X?(\\d+)" +
                                                               "\\d+<[\\d\\w]+:[\\d\\w]*>\\d+.\\d+.\\w+;\\d" +
                                                               "|10.1021/\\w\\w\\d++" +
                                                               "|10.1207/[\\w\\d]+\\&\\d+_\\d+",
                                                           Pattern.CASE_INSENSITIVE);

    private final WebTarget webTarget;

    public CrossRefDoiCheck(final WebTarget webTarget) {
        this.webTarget = webTarget;
    }

    public boolean validExistingDoi(final String query) {
        final boolean doi = isDoi(query);
        if (!doi) {
            return true;
        }
        return existingDoi(query);
    }

    public boolean isDoi(final String value) {

        Matcher m = PATTERN.matcher(purgeDoiValue(value));
        return m.matches();
    }

    private boolean existingDoi(final String value) {
        final Response head = webTarget.path(purgeDoiValue(value)).request().head();
        return head.getStatus() == 200;
    }


    private String purgeDoiValue(final String query) {
        String value = query.replaceAll(",", "");
        for (final String prefix : DOI_PREFIXES) {
            value = value.replaceAll(prefix, "");
        }
        return value;
    }

}
