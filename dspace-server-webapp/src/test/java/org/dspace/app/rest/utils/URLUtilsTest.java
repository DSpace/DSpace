/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author mwood
 */
public class URLUtilsTest {

    public URLUtilsTest() {
    }

    /**
     * Test of decode method, of class URLUtils.
     */
    @Ignore
    @Test
    public void testDecode() {
    }

    /**
     * Test of encode method, of class URLUtils.
     */
    @Ignore
    @Test
    public void testEncode() {
    }

    /**
     * Test of urlIsPrefixOf method, of class URLUtils.
     */
    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("UnusedAssignment")
    public void testUrlIsPrefixOf() {
        boolean isPrefix;

        isPrefix = URLUtils.urlIsPrefixOf("http://example.com/path", "http://example.com/path");
        assertTrue("Should match if all is equal", isPrefix);
        isPrefix = URLUtils.urlIsPrefixOf("http://example.com:80/test", "http://example.com:80/test/1");
        assertTrue("Should match if pattern path is longer", isPrefix);
        isPrefix = URLUtils.urlIsPrefixOf("http://example.com:80/test", "http://example.com/test");
        assertTrue("Should match if missing port matches default", isPrefix);

        isPrefix = URLUtils.urlIsPrefixOf("http://example.com/path", "https://example.com/path");
        assertFalse("Should not match if protocols don't match", isPrefix);
        isPrefix = URLUtils.urlIsPrefixOf("http://example.com/", "http://oops.example.com/");
        assertFalse("Should not match if hosts don't match", isPrefix);
        isPrefix = URLUtils.urlIsPrefixOf("http://example.com:80/", "http://example.com:8080/");
        assertFalse("Should not match if ports don't match", isPrefix);
        isPrefix = URLUtils.urlIsPrefixOf("http://example.com/path1/a", "http://example.com/path2/a");
        assertFalse("Should not match if paths don't match", isPrefix);

        isPrefix = URLUtils.urlIsPrefixOf("http://example.com/path", "http://example.com/path/");
        assertTrue("Should match with, without trailing slash", isPrefix);
        isPrefix = URLUtils.urlIsPrefixOf("http://example.com/path1", "http://example.com/path2");
        assertFalse("Should not match if paths don't match", isPrefix);
        isPrefix = URLUtils.urlIsPrefixOf("http://example.com/path", "http://example.com/path2/sub");
        assertFalse("Should not match if interior path elements don't match", isPrefix);

        // Check if a malformed URL raises an exception
        isPrefix = URLUtils.urlIsPrefixOf(null, "http://example.com/");
    }
}
