/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.utility;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LDNUtils {

    private final static Pattern UUID_REGEX_PATTERN = Pattern.compile(
        "\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}"
    );

    private LDNUtils() {

    }

    public static UUID getUUIDFromURL(String url) {
        Matcher matcher = UUID_REGEX_PATTERN.matcher(url);
        StringBuilder handle = new StringBuilder();
        if (matcher.find()) {
            handle.append(matcher.group(0));
        }
        return UUID.fromString(handle.toString());
    }

    public static String removedProtocol(String url) {
        return url.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)", "");
    }

}
