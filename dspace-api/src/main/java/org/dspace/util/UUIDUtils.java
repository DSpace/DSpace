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
