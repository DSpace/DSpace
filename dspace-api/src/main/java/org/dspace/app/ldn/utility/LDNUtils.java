/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.utility;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Some linked data notification utilities.
 */
public class LDNUtils {

    private final static Pattern UUID_REGEX_PATTERN = Pattern.compile(
            "\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}");

    private final static String SIMPLE_PROTOCOL_REGEX = "^(http[s]?://www\\.|http[s]?://|www\\.)";

    /**
     * 
     */
    private LDNUtils() {

    }

    /**
     * Whether the URL contains an UUID. Used to determine item id from item URL.
     *
     * @param url item URL
     * @return boolean true if URL has UUID, false otherwise
     */
    public static boolean hasUUIDInURL(String url) {
        Matcher matcher = UUID_REGEX_PATTERN.matcher(url);

        return matcher.find();
    }

    /**
     * Extract UUID from URL.
     *
     * @param url item URL
     * @return UUID item id
     */
    public static UUID getUUIDFromURL(String url) {
        Matcher matcher = UUID_REGEX_PATTERN.matcher(url);
        StringBuilder handle = new StringBuilder();
        if (matcher.find()) {
            handle.append(matcher.group(0));
        }
        return UUID.fromString(handle.toString());
    }

    /**
     * Remove http or https protocol from URL.
     *
     * @param url URL
     * @return String URL without protocol
     */
    public static String removedProtocol(String url) {
        return url.replaceFirst(SIMPLE_PROTOCOL_REGEX, EMPTY);
    }

    /**
     * Custom context resolver processing. Currently converting DOI URL to DOI id.
     *
     * @param value context ietf:cite-as
     * @return String ietf:cite-as identifier
     */
    public static String processContextResolverId(String value) {
        String resolverId = value;
        resolverId = resolverId.replace("https://doi.org/", "doi:");

        return resolverId;
    }

    /**
     * Clear the coarNotifyType from the source code.
     *
     * @param coarNotifyType coar Notify Type to sanitize
     * @return String just the notify type
     */
    public static String getNotifyType(String coarNotifyType) {
        String justNotifyType = coarNotifyType;
        justNotifyType = justNotifyType.substring(justNotifyType.lastIndexOf(":") + 1);
        justNotifyType = justNotifyType.replace("Action",  "");
        return justNotifyType;
    }

}