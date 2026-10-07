/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link CustomUrlUtils}.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class CustomUrlUtilsTest {

    @Test
    public void testIsValidCustomUrl_ValidUrls() {
        // Valid URLs with only Latin characters, digits, hyphens, underscores, and dots
        assertTrue(CustomUrlUtils.isValidCustomUrl("valid-url"));
        assertTrue(CustomUrlUtils.isValidCustomUrl("valid_url"));
        assertTrue(CustomUrlUtils.isValidCustomUrl("valid.url"));
        assertTrue(CustomUrlUtils.isValidCustomUrl("ValidUrl123"));
        assertTrue(CustomUrlUtils.isValidCustomUrl("url-with-many-hyphens"));
        assertTrue(CustomUrlUtils.isValidCustomUrl("url_with_underscores"));
        assertTrue(CustomUrlUtils.isValidCustomUrl("url.with.dots"));
        assertTrue(CustomUrlUtils.isValidCustomUrl("mixed-url_123.test"));
        assertTrue(CustomUrlUtils.isValidCustomUrl("123-numeric-start"));
        assertTrue(CustomUrlUtils.isValidCustomUrl("a"));
        assertTrue(CustomUrlUtils.isValidCustomUrl("1"));
    }

    @Test
    public void testIsValidCustomUrl_InvalidUrls_NonLatinCharacters() {
        // Cyrillic characters
        assertFalse(CustomUrlUtils.isValidCustomUrl("тестовый-url"));
        assertFalse(CustomUrlUtils.isValidCustomUrl("url-тест"));
        assertFalse(CustomUrlUtils.isValidCustomUrl("Москва"));

        // Arabic characters
        assertFalse(CustomUrlUtils.isValidCustomUrl("اختبار-url"));
        assertFalse(CustomUrlUtils.isValidCustomUrl("url-اختبار"));

        // Chinese characters
        assertFalse(CustomUrlUtils.isValidCustomUrl("测试-url"));
        assertFalse(CustomUrlUtils.isValidCustomUrl("url-测试"));
        assertFalse(CustomUrlUtils.isValidCustomUrl("测试文章"));

        // Greek characters
        assertFalse(CustomUrlUtils.isValidCustomUrl("δοκιμή-url"));

        // Hebrew characters
        assertFalse(CustomUrlUtils.isValidCustomUrl("בדיקה-url"));
    }

    @Test
    public void testIsValidCustomUrl_InvalidUrls_SpecialCharacters() {
        // Special characters that are not allowed
        assertFalse(CustomUrlUtils.isValidCustomUrl("invalid?url"));
        assertFalse(CustomUrlUtils.isValidCustomUrl("invalid/url"));
        assertFalse(CustomUrlUtils.isValidCustomUrl("invalid&url"));
        assertFalse(CustomUrlUtils.isValidCustomUrl("invalid@url"));
        assertFalse(CustomUrlUtils.isValidCustomUrl("invalid#url"));
        assertFalse(CustomUrlUtils.isValidCustomUrl("invalid$url"));
        assertFalse(CustomUrlUtils.isValidCustomUrl("invalid%url"));
        assertFalse(CustomUrlUtils.isValidCustomUrl("invalid!url"));
        assertFalse(CustomUrlUtils.isValidCustomUrl("invalid*url"));
        assertFalse(CustomUrlUtils.isValidCustomUrl("invalid+url"));
        assertFalse(CustomUrlUtils.isValidCustomUrl("invalid=url"));
        assertFalse(CustomUrlUtils.isValidCustomUrl("invalid url")); // space
        assertFalse(CustomUrlUtils.isValidCustomUrl("invalid,url"));
        assertFalse(CustomUrlUtils.isValidCustomUrl("invalid;url"));
    }

    @Test
    public void testIsValidCustomUrl_InvalidUrls_Emoji() {
        assertFalse(CustomUrlUtils.isValidCustomUrl("url-🌍"));
        assertFalse(CustomUrlUtils.isValidCustomUrl("🌍🌡️"));
        assertFalse(CustomUrlUtils.isValidCustomUrl("test-😊-url"));
    }

    @Test
    public void testIsValidCustomUrl_EdgeCases() {
        // Null and empty
        assertFalse(CustomUrlUtils.isValidCustomUrl(null));
        assertFalse(CustomUrlUtils.isValidCustomUrl(""));
        assertFalse(CustomUrlUtils.isValidCustomUrl("   "));

        // Only special characters
        assertFalse(CustomUrlUtils.isValidCustomUrl("???"));
        assertFalse(CustomUrlUtils.isValidCustomUrl("$$$"));
    }

    @Test
    public void testHasInvalidCharacters_InverseOfIsValid() {
        // Valid URLs should NOT have invalid characters
        assertFalse(CustomUrlUtils.hasInvalidCharacters("valid-url"));
        assertFalse(CustomUrlUtils.hasInvalidCharacters("valid_url"));
        assertFalse(CustomUrlUtils.hasInvalidCharacters("valid.url"));

        // Invalid URLs SHOULD have invalid characters
        assertTrue(CustomUrlUtils.hasInvalidCharacters("invalid?url"));
        assertTrue(CustomUrlUtils.hasInvalidCharacters("тестовый"));
        assertTrue(CustomUrlUtils.hasInvalidCharacters("اختبار"));
        assertTrue(CustomUrlUtils.hasInvalidCharacters(null));
        assertTrue(CustomUrlUtils.hasInvalidCharacters(""));
    }

    @Test
    public void testIsValidCustomUrl_AccentedCharacters() {
        // Accented characters are NOT valid in the final URL
        // (they should be normalized first by CustomUrlService)
        assertFalse(CustomUrlUtils.isValidCustomUrl("café"));
        assertFalse(CustomUrlUtils.isValidCustomUrl("naïve"));
        assertFalse(CustomUrlUtils.isValidCustomUrl("résumé"));

        // But the normalized versions ARE valid
        assertTrue(CustomUrlUtils.isValidCustomUrl("cafe"));
        assertTrue(CustomUrlUtils.isValidCustomUrl("naive"));
        assertTrue(CustomUrlUtils.isValidCustomUrl("resume"));
    }
}
