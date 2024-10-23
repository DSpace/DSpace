/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * General static utilities for use with GND and LOBID API code:
 * Formatting URIs and identifiers, constructing exemplar objects for testing correct object parsing.
 * See {@link LobidGNDRestConnector}
 *
 * @author Kim Shepherd
 */
public class GNDUtils {

    /**
     * Initialise configuration service
     */
    private static final ConfigurationService configurationService =
            DSpaceServicesFactory.getInstance().getConfigurationService();
    /**
     * Initialise logger
     */
    private static final Logger log = LogManager.getLogger(GNDUtils.class);

    public GNDUtils() {}

    /**
     * Given a partial identifier, full identifier, or JSON request URL, return the official
     * LOBID object URI in https://lobid.org/gnd/{identifier}.json format, for retrieving the object
     * by identifier
     *
     * @param identifier Partial or full identifier
     * @return full GND URI
     */
    public static String formatObjectURI(String identifier) throws IllegalArgumentException {
        if (null == identifier) {
            throw new IllegalArgumentException("Null GND identifier supplied to formatURI()");
        }
        String partialIdentifier = null;
        // Firstly, try to extract the partial identifier
        try {
            partialIdentifier = extractIdentifier(identifier);
        } catch (IllegalArgumentException e) {
            // If we caught this exception, we will continue
            log.debug("Could not extract a partial identifier from non-null string, " +
                    "will simply prepend the URL prefix to the original value (" + identifier + ")");
            partialIdentifier = identifier;
        }

        // Prefix the identifier with the configured URL prefix and return
        String urlPrefix = configurationService.getProperty("gnd.api.url",
                "https://lobid.org/gnd/");
        return urlPrefix + partialIdentifier + ".json";
    }

    /**
     * Given a partial identifier, full identifier, or JSON request URL, return the official
     * GND URI in https://d-nb.info/gnd/{identifier} format
     *
     * @param identifier Partial or full identifier
     * @return full GND URI
     */
    public static String formatURI(String identifier) throws IllegalArgumentException {
        if (null == identifier) {
            throw new IllegalArgumentException("Null GND identifier supplied to formatURI()");
        }
        String partialIdentifier = null;
        // Firstly, try to extract the partial identifier
        try {
            partialIdentifier = extractIdentifier(identifier);
        } catch (IllegalArgumentException e) {
            // If we caught this exception, we will continue
            log.debug("Could not extract a partial identifier from non-null string, " +
                    "will simply prepend the URL prefix to the original value (" + identifier + ")");
            partialIdentifier = identifier;
        }

        // Prefix the identifier with the configured URL prefix and return
        String urlPrefix = configurationService.getProperty("gnd.uri.prefix",
                "https://d-nb.info/gnd/");
        return urlPrefix + partialIdentifier;
    }

    /**
     * Extract and return a GND identifier from a full URI or other format. This can then be used to construct
     * other URIs, URLs, paths, log messages, labels, and so on.
     *
     * @param identifier the input value from which to parse and extract a partial identifier
     *                   e.g. <a href="https://d-nb.info/gnd/4074335-4">https://d-nb.info/gnd/4074335-4</a>
     * @return partial identifier eg. 4074335-4
     */
    public static String extractIdentifier(String identifier) {
        // Throw a hard error if this parameter is null
        if (null == identifier) {
            throw new IllegalArgumentException("Null GND identifier supplied to formatIdentifer()");
        }
        // Partial/basic identifier: 4074335-4
        // GND URI: https://d-nb.info/gnd/4074335-4
        // API request URL: http://lobid.org/gnd/4074335-4.json

        // Read the regular expression from configuration, which we'll use to extract the identifier from either
        // of the expected paths above
        String regex = configurationService.getProperty("gnd.identifier.regex",
                "^https?://[^/]*/gnd/([^.]*).*$");
        // Attempt to match the identifier
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(identifier);
        if (m.matches()) {
            // Return the matched identifier portion, eg. 4074335-4
            return m.group(1);
        }
        // If we reached this line, we did not find a valid match and we should throw an error
        throw new IllegalArgumentException("Supplied string does not match regex. input="
                + identifier + ", regex=" + regex);
    }

}
