/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author mwood
 */
public class URLUtilsTest {

    public URLUtilsTest() {
    }

    /**
     * Test of decode method, of class URLUtils.
     */
    @Disabled
    @Test
    public void testDecode() {
    }

    /**
     * Test of encode method, of class URLUtils.
     */
    @Disabled
    @Test
    public void testEncode() {
    }

    /**
     * Test of urlIsPrefixOf method, of class URLUtils.
     */
    @Test
    @SuppressWarnings("UnusedAssignment")
    public void testUrlIsPrefixOf() {
        assertThrows(IllegalArgumentException.class, () -> {
            boolean isPrefix;

            isPrefix = URLUtils.urlIsPrefixOf("http://example.com/path", "http://example.com/path");
            assertTrue(isPrefix, "Should match if all is equal");
            isPrefix = URLUtils.urlIsPrefixOf("http://example.com:80/test", "http://example.com:80/test/1");
            assertTrue(isPrefix, "Should match if pattern path is longer");
            isPrefix = URLUtils.urlIsPrefixOf("http://example.com:80/test", "http://example.com/test");
            assertTrue(isPrefix, "Should match if missing port matches default");

            isPrefix = URLUtils.urlIsPrefixOf("http://example.com/path", "https://example.com/path");
            assertFalse(isPrefix, "Should not match if protocols don't match");
            isPrefix = URLUtils.urlIsPrefixOf("http://example.com/", "http://oops.example.com/");
            assertFalse(isPrefix, "Should not match if hosts don't match");
            isPrefix = URLUtils.urlIsPrefixOf("http://example.com:80/", "http://example.com:8080/");
            assertFalse(isPrefix, "Should not match if ports don't match");
            isPrefix = URLUtils.urlIsPrefixOf("http://example.com/path1/a", "http://example.com/path2/a");
            assertFalse(isPrefix, "Should not match if paths don't match");

            isPrefix = URLUtils.urlIsPrefixOf("http://example.com/path", "http://example.com/path/");
            assertTrue(isPrefix, "Should match with, without trailing slash");
            isPrefix = URLUtils.urlIsPrefixOf("http://example.com/path1", "http://example.com/path2");
            assertFalse(isPrefix, "Should not match if paths don't match");
            isPrefix = URLUtils.urlIsPrefixOf("http://example.com/path", "http://example.com/path2/sub");
            assertFalse(isPrefix, "Should not match if interior path elements don't match");

            // Check if a malformed URL raises an exception
            isPrefix = URLUtils.urlIsPrefixOf(null, "http://example.com/");
        });
    }
}
