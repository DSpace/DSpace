/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.eperson;

import java.sql.SQLException;
import java.util.ArrayList;
import org.apache.commons.codec.DecoderException;
import org.dspace.AbstractUnitTest;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.TableRow;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author mwood
 */
public class EPersonTest extends AbstractUnitTest
{
    private static TableRow row1;

    public EPersonTest()
    {
    }

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init()
    {
        super.init();

        // Build a TableRow for an EPerson to wrap
        final ArrayList<String> epersonColumns = new ArrayList<String>();
        epersonColumns.add("eperson_id");
        epersonColumns.add("password");
        epersonColumns.add("salt");
        epersonColumns.add("digest_algorithm");

        row1 = new TableRow("EPerson", epersonColumns);   
    }

    /**
     * Test of equals method, of class EPerson.
     */
/*
    @Test
    public void testEquals()
    {
        System.out.println("equals");
        Object obj = null;
        EPerson instance = null;
        boolean expResult = false;
        boolean result = instance.equals(obj);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of hashCode method, of class EPerson.
     */
/*
    @Test
    public void testHashCode()
    {
        System.out.println("hashCode");
        EPerson instance = null;
        int expResult = 0;
        int result = instance.hashCode();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of find method, of class EPerson.
     */
/*
    @Test
    public void testFind()
            throws Exception
    {
        System.out.println("find");
        Context context = null;
        int id = 0;
        EPerson expResult = null;
        EPerson result = EPerson.find(context, id);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of findByEmail method, of class EPerson.
     */
/*
    @Test
    public void testFindByEmail()
            throws Exception
    {
        System.out.println("findByEmail");
        Context context = null;
        String email = "";
        EPerson expResult = null;
        EPerson result = EPerson.findByEmail(context, email);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of findByNetid method, of class EPerson.
     */
/*
    @Test
    public void testFindByNetid()
            throws Exception
    {
        System.out.println("findByNetid");
        Context context = null;
        String netid = "";
        EPerson expResult = null;
        EPerson result = EPerson.findByNetid(context, netid);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of search method, of class EPerson.
     */
/*
    @Test
    public void testSearch_Context_String()
            throws Exception
    {
        System.out.println("search");
        Context context = null;
        String query = "";
        EPerson[] expResult = null;
        EPerson[] result = EPerson.search(context, query);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of search method, of class EPerson.
     */
/*
    @Test
    public void testSearch_4args()
            throws Exception
    {
        System.out.println("search");
        Context context = null;
        String query = "";
        int offset = 0;
        int limit = 0;
        EPerson[] expResult = null;
        EPerson[] result = EPerson.search(context, query, offset, limit);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of searchResultCount method, of class EPerson.
     */
/*
    @Test
    public void testSearchResultCount()
            throws Exception
    {
        System.out.println("searchResultCount");
        Context context = null;
        String query = "";
        int expResult = 0;
        int result = EPerson.searchResultCount(context, query);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of findAll method, of class EPerson.
     */
/*
    @Test
    public void testFindAll()
            throws Exception
    {
        System.out.println("findAll");
        Context context = null;
        int sortField = 0;
        EPerson[] expResult = null;
        EPerson[] result = EPerson.findAll(context, sortField);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of create method, of class EPerson.
     */
/*
    @Test
    public void testCreate()
            throws Exception
    {
        System.out.println("create");
        Context context = null;
        EPerson expResult = null;
        EPerson result = EPerson.create(context);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of delete method, of class EPerson.
     */
/*
    @Test
    public void testDelete()
            throws Exception
    {
        System.out.println("delete");
        EPerson instance = null;
        instance.delete();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getID method, of class EPerson.
     */
/*
    @Test
    public void testGetID()
    {
        System.out.println("getID");
        EPerson instance = null;
        int expResult = 0;
        int result = instance.getID();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getLanguage method, of class EPerson.
     */
/*
    @Test
    public void testGetLanguage()
    {
        System.out.println("getLanguage");
        EPerson instance = null;
        String expResult = "";
        String result = instance.getLanguage();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setLanguage method, of class EPerson.
     */
/*
    @Test
    public void testSetLanguage()
    {
        System.out.println("setLanguage");
        String language = "";
        EPerson instance = null;
        instance.setLanguage(language);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getHandle method, of class EPerson.
     */
/*
    @Test
    public void testGetHandle()
    {
        System.out.println("getHandle");
        EPerson instance = null;
        String expResult = "";
        String result = instance.getHandle();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getEmail method, of class EPerson.
     */
/*
    @Test
    public void testGetEmail()
    {
        System.out.println("getEmail");
        EPerson instance = null;
        String expResult = "";
        String result = instance.getEmail();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setEmail method, of class EPerson.
     */
/*
    @Test
    public void testSetEmail()
    {
        System.out.println("setEmail");
        String s = "";
        EPerson instance = null;
        instance.setEmail(s);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getNetid method, of class EPerson.
     */
/*
    @Test
    public void testGetNetid()
    {
        System.out.println("getNetid");
        EPerson instance = null;
        String expResult = "";
        String result = instance.getNetid();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setNetid method, of class EPerson.
     */
/*
    @Test
    public void testSetNetid()
    {
        System.out.println("setNetid");
        String s = "";
        EPerson instance = null;
        instance.setNetid(s);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getFullName method, of class EPerson.
     */
/*
    @Test
    public void testGetFullName()
    {
        System.out.println("getFullName");
        EPerson instance = null;
        String expResult = "";
        String result = instance.getFullName();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getFirstName method, of class EPerson.
     */
/*
    @Test
    public void testGetFirstName()
    {
        System.out.println("getFirstName");
        EPerson instance = null;
        String expResult = "";
        String result = instance.getFirstName();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setFirstName method, of class EPerson.
     */
/*
    @Test
    public void testSetFirstName()
    {
        System.out.println("setFirstName");
        String firstname = "";
        EPerson instance = null;
        instance.setFirstName(firstname);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getLastName method, of class EPerson.
     */
/*
    @Test
    public void testGetLastName()
    {
        System.out.println("getLastName");
        EPerson instance = null;
        String expResult = "";
        String result = instance.getLastName();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setLastName method, of class EPerson.
     */
/*
    @Test
    public void testSetLastName()
    {
        System.out.println("setLastName");
        String lastname = "";
        EPerson instance = null;
        instance.setLastName(lastname);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setCanLogIn method, of class EPerson.
     */
/*
    @Test
    public void testSetCanLogIn()
    {
        System.out.println("setCanLogIn");
        boolean login = false;
        EPerson instance = null;
        instance.setCanLogIn(login);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of canLogIn method, of class EPerson.
     */
/*
    @Test
    public void testCanLogIn()
    {
        System.out.println("canLogIn");
        EPerson instance = null;
        boolean expResult = false;
        boolean result = instance.canLogIn();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setRequireCertificate method, of class EPerson.
     */
/*
    @Test
    public void testSetRequireCertificate()
    {
        System.out.println("setRequireCertificate");
        boolean isrequired = false;
        EPerson instance = null;
        instance.setRequireCertificate(isrequired);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getRequireCertificate method, of class EPerson.
     */
/*
    @Test
    public void testGetRequireCertificate()
    {
        System.out.println("getRequireCertificate");
        EPerson instance = null;
        boolean expResult = false;
        boolean result = instance.getRequireCertificate();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setSelfRegistered method, of class EPerson.
     */
/*
    @Test
    public void testSetSelfRegistered()
    {
        System.out.println("setSelfRegistered");
        boolean sr = false;
        EPerson instance = null;
        instance.setSelfRegistered(sr);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getSelfRegistered method, of class EPerson.
     */
/*
    @Test
    public void testGetSelfRegistered()
    {
        System.out.println("getSelfRegistered");
        EPerson instance = null;
        boolean expResult = false;
        boolean result = instance.getSelfRegistered();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getMetadata method, of class EPerson.
     */
/*
    @Test
    public void testGetMetadata()
    {
        System.out.println("getMetadata");
        String field = "";
        EPerson instance = null;
        String expResult = "";
        String result = instance.getMetadata(field);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setMetadata method, of class EPerson.
     */
/*
    @Test
    public void testSetMetadata()
    {
        System.out.println("setMetadata");
        String field = "";
        String value = "";
        EPerson instance = null;
        instance.setMetadata(field, value);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setPassword method, of class EPerson.
     */
/*
    @Test
    public void testSetPassword()
    {
        System.out.println("setPassword");
        String s = "";
        EPerson instance = null;
        instance.setPassword(s);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setPasswordHash method, of class EPerson.
     */
/*
    @Test
    public void testSetPasswordHash()
    {
        System.out.println("setPasswordHash");
        PasswordHash password = null;
        EPerson instance = null;
        instance.setPasswordHash(password);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getPasswordHash method, of class EPerson.
     */
/*
    @Test
    public void testGetPasswordHash()
    {
        System.out.println("getPasswordHash");
        EPerson instance = null;
        PasswordHash expResult = null;
        PasswordHash result = instance.getPasswordHash();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of checkPassword method, of class EPerson.
     */
    @Test
    public void testCheckPassword()
            throws SQLException, DecoderException
    {
        final String attempt = "secret";
        EPerson instance = new EPerson(context, row1);

        // Test old unsalted MD5 hash
        final String hash = "5ebe2294ecd0e0f08eab7690d2a6ee69"; // MD5("secret");
        instance.setPasswordHash(new PasswordHash(null, null, hash));
        boolean result = instance.checkPassword(attempt);
        assertTrue("check string with matching MD5 hash", result);
        // It should have converted the password to the new hash
        assertEquals("should have upgraded algorithm",
                PasswordHash.getDefaultAlgorithm(),
                instance.getPasswordHash().getAlgorithm());
        assertTrue("upgraded hash should still match",
                instance.checkPassword(attempt));

        // TODO test a salted multiround hash
    }

    /**
     * Test of update method, of class EPerson.
     */
/*
    @Test
    public void testUpdate()
            throws Exception
    {
        System.out.println("update");
        EPerson instance = null;
        instance.update();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getType method, of class EPerson.
     */
    @Test
    public void testGetType()
            throws SQLException
    {
        System.out.println("getType");
        EPerson instance = new EPerson(context, row1);
        int expResult = Constants.EPERSON;
        int result = instance.getType();
        assertEquals("Should return Constants.EPERSON", expResult, result);
    }

    /**
     * Test of getDeleteConstraints method, of class EPerson.
     */
/*
    @Test
    public void testGetDeleteConstraints()
            throws Exception
    {
        System.out.println("getDeleteConstraints");
        EPerson instance = null;
        List expResult = null;
        List result = instance.getDeleteConstraints();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getName method, of class EPerson.
     */
/*
    @Test
    public void testGetName()
    {
        System.out.println("getName");
        EPerson instance = null;
        String expResult = "";
        String result = instance.getName();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/
}
