/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * Utility class to read UUIDs
 */
public class UUIDUtils {

    public static UUID fromString(final String identifier) {
        UUID output = null;
        if(StringUtils.isNotBlank(identifier)) {
            try {
                output = UUID.fromString(identifier.trim());
            } catch(IllegalArgumentException e) {
                output = null;
            }
        }
        return output;
    }

    public static String toString(final UUID identifier) {
        return identifier == null ? null : identifier.toString();
    }
}
