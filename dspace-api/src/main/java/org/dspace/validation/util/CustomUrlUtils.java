/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation.util;

import java.util.regex.Pattern;

/**
 * Utility class for Custom URL validation and processing.
 * Provides shared logic for validating custom URL format to ensure
 * URLs contain only Latin characters, preventing UI rendering issues
 * with non-Latin scripts (Cyrillic, Arabic, Chinese, etc.).
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public final class CustomUrlUtils {

    /**
     * Pattern that defines valid custom URL format.
     * A valid custom URL must contain only:
     * - Latin letters (a-z, A-Z)
     * - Digits (0-9)
     * - Hyphens (-)
     * - Underscores (_)
     * - Dots (.)
     */
    private static final Pattern URL_PATH_PATTERN = Pattern.compile("^[.a-zA-Z0-9-_]+$");

    private CustomUrlUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Validates if the given custom URL contains only allowed characters.
     * The URL must contain only Latin letters (a-z, A-Z), digits (0-9),
     * hyphens (-), underscores (_), and dots (.).
     * <p>
     * This validation prevents URLs with Cyrillic, Arabic, Chinese, or other
     * non-Latin scripts from being accepted, as they cause UI rendering issues.
     * </p>
     *
     * @param customUrl the custom URL to validate
     * @return true if the URL is valid (contains only allowed characters), false otherwise
     */
    public static boolean isValidCustomUrl(String customUrl) {
        if (customUrl == null) {
            return false;
        }
        return URL_PATH_PATTERN.matcher(customUrl).matches();
    }

    /**
     * Checks if the given custom URL contains invalid characters.
     * This is the inverse of {@link #isValidCustomUrl(String)}.
     *
     * @param customUrl the custom URL to check
     * @return true if the URL contains invalid characters, false otherwise
     */
    public static boolean hasInvalidCharacters(String customUrl) {
        return !isValidCustomUrl(customUrl);
    }
}
