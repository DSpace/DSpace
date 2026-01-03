/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.DecoderException;
import org.dspace.AbstractDSpaceTest;
import org.dspace.core.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author mwood
 */
public class PasswordHashTest extends AbstractDSpaceTest {
    public PasswordHashTest() {
    }

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
    }

    /**
     * Test the constructors.
     */
    @Test
    public void testConstructors()
        throws DecoderException {
        PasswordHash h1;
        PasswordHash h3;

        // Test null inputs, as from NULL database columns (old EPerson using
        // unsalted hash, for example).
        h3 = new PasswordHash(null, (byte[]) null, (byte[]) null);
        assertNull(h3.getAlgorithm(), "Null algorithm");
        assertNull(h3.getSalt(), "Null salt");
        assertNull(h3.getHash(), "Null hash");
        assertFalse(h3.matches(null), "Match null string?");
        assertFalse(h3.matches("not null"), "Match non-null string?");

        // Test 3-argument constructor with null string arguments
        h3 = new PasswordHash(null, (String) null, (String) null);
        assertNull(h3.getAlgorithm(), "Null algorithm");
        assertNull(h3.getSalt(), "Null salt");
        assertNull(h3.getHash(), "Null hash");
        assertFalse(h3.matches(null), "Match null string?");
        assertFalse(h3.matches("not null"), "Match non-null string?");

        // Test single-argument constructor, which does the hashing.
        String password = "I've got a secret.";
        h1 = new PasswordHash(password);
        assertEquals("SHA-512", h1.getAlgorithm());
        assertFalse(h1.matches("random rubbish"), "Match against a different string");
        assertTrue(h1.matches(password), "Match against the correct string");

        // Test 3-argument constructor with non-null data.
        h3 = new PasswordHash(h1.getAlgorithm(), h1.getSalt(), h1.getHash());
        assertTrue(h3.matches(password), "Match a duplicate original made from getter values");
    }

    /**
     * Test of matches method, of class PasswordHash.
     */
    @Test
    public void testMatches()
        throws NoSuchAlgorithmException, UnsupportedEncodingException {
        System.out.println("matches");
        final String secret = "Clark Kent is Superman";

        // Test old 1-trip MD5 hash
        MessageDigest digest = MessageDigest.getInstance("MD5");
        PasswordHash hash = new PasswordHash(null, null, digest.digest(secret.getBytes(Constants.DEFAULT_ENCODING)));
        boolean result = hash.matches(secret);
        assertTrue(result, "Old unsalted 1-trip MD5 hash");

        // 3-argument form:  see constructor tests
    }

    /**
     * Test of getHash method, of class PasswordHash.
     */
    /*
    @Test
    public void testGetHash()
    {
        System.out.println("getHash");
        PasswordHash instance = null;
        byte[] expResult = null;
        byte[] result = instance.getHash();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    */

    /**
     * Test of getSalt method, of class PasswordHash.
     */
    /*
    @Test
    public void testGetSalt()
    {
        System.out.println("getSalt");
        PasswordHash instance = null;
        byte[] expResult = null;
        byte[] result = instance.getSalt();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    */

    /**
     * Test of getAlgorithm method, of class PasswordHash.
     */
    /*
    @Test
    public void testGetAlgorithm()
    {
        System.out.println("getAlgorithm");
        PasswordHash instance = null;
        String expResult = "";
        String result = instance.getAlgorithm();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    */
}
