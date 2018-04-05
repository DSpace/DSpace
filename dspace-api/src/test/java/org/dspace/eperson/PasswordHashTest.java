/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.DecoderException;
import org.dspace.AbstractDSpaceTest;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author mwood
 */
public class PasswordHashTest extends AbstractDSpaceTest
{
    public PasswordHashTest()
    {
    }

    @Before
    public void setUp()
    {
    }
    
    @After
    public void tearDown()
    {
    }

    /**
     * Test the constructors.
     */
    @Test
    public void testConstructors()
            throws DecoderException
    {
        PasswordHash h1, h3;

        // Test null inputs, as from NULL database columns (old EPerson using
        // unsalted hash, for example).
        h3 = new PasswordHash(null, (byte[])null, (byte[])null);
        assertNull("Null algorithm", h3.getAlgorithm());
        assertNull("Null salt", h3.getSalt());
        assertNull("Null hash", h3.getHash());
        assertFalse("Match null string?", h3.matches(null));
        assertFalse("Match non-null string?", h3.matches("not null"));

        // Test 3-argument constructor with null string arguments
        h3 = new PasswordHash(null, (String)null, (String)null);
        assertNull("Null algorithm", h3.getAlgorithm());
        assertNull("Null salt", h3.getSalt());
        assertNull("Null hash", h3.getHash());
        assertFalse("Match null string?", h3.matches(null));
        assertFalse("Match non-null string?", h3.matches("not null"));

        // Test single-argument constructor, which does the hashing.
        String password = "I've got a secret.";
        h1 = new PasswordHash(password);
        assertEquals("SHA-512", h1.getAlgorithm());
        assertFalse("Match against a different string", h1.matches("random rubbish"));
        assertTrue("Match against the correct string", h1.matches(password));

        // Test 3-argument constructor with non-null data.
        h3 = new PasswordHash(h1.getAlgorithm(), h1.getSalt(), h1.getHash());
        assertTrue("Match a duplicate original made from getter values", h3.matches(password));
    }

    /**
     * Test of matches method, of class PasswordHash.
     */
    @Test
    public void testMatches()
            throws NoSuchAlgorithmException
    {
        System.out.println("matches");
        final String secret = "Clark Kent is Superman";

        // Test old 1-trip MD5 hash
        MessageDigest digest = MessageDigest.getInstance("MD5");
        PasswordHash hash = new PasswordHash(null, null, digest.digest(secret.getBytes()));
        boolean result = hash.matches(secret);
        assertTrue("Old unsalted 1-trip MD5 hash", result);

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
