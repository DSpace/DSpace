/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa.v2;

/**
 * SHERPA v2 API query handling utility methods (static). Used by external data providers and SHERPA service.
 *
 * @author Kim Shepherd
 */
public final class SHERPAUtils {

    // Private constructor (since this is a Utility class)
    private SHERPAUtils() {}

    /**
     * Sanitise a SHERPA v2 API query for some special JSON characters to help with parsing at remote end
     * Strip all these characters: "'{};
     * The URI builder used in the provider and service classes will perform URL encoding. This string
     * is the raw query submitted to the provider or service.
     * @param query query string
     * @return safe query string
     */
    public static String sanitiseQuery(String query) {
        String safe = query;
        try {
            safe = query.replaceAll("['{}\";]", "");
        } catch (NullPointerException e) {
            safe = "";
        }
        return safe;
    }

}
