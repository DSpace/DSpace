package org.dspace.eperson;

import org.dspace.servicemanager.DSpaceKernelInit;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author mwood
 */
public class PasswordHashTest
{
    public PasswordHashTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
            throws Exception
    {
        // Make certain that a default DSpaceKernel is started.
        DSpaceKernelInit.getKernel(null).start();
    }

    @AfterClass
    public static void tearDownClass()
            throws Exception
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
    {
        PasswordHash h1, h3;

        // Test null inputs, as from NULL database columns (old EPerson using
        // unsalted hash, for example).
        h3 = new PasswordHash(null, (byte[])null, (byte[]) null);
        assertEquals("MD5", h3.getAlgorithm());
        assertNull(h3.getSalt());
        assertNull(h3.getHash());
        assertFalse(h3.matches(null));
        assertFalse(h3.matches("not null"));

        // Test single-argument constructor, which does the hashing.
        String password = "I've got a secret.";
        h1 = new PasswordHash(password);
        assertEquals("SHA-512", h1.getAlgorithm());
        assertFalse(h1.matches("random rubbish"));
        assertTrue(h1.matches(password));

        // Test 3-argument constructor with non-null data.
        h3 = new PasswordHash(h1.getAlgorithm(), h1.getSalt(), h1.getHash());
        assertTrue(h3.matches(password));
    }

    /**
     * Test of matches method, of class PasswordHash.
     */
    /*
    @Test
    public void testMatches()
    {
        System.out.println("matches");
        String secret = "";
        PasswordHash instance = null;
        boolean expResult = false;
        boolean result = instance.matches(secret);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    */

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
