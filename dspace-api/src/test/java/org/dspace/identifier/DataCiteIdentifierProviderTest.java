/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.identifier;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.TableRow;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author mwood
 */
public class DataCiteIdentifierProviderTest
        extends AbstractUnitTest
{
    private static Item item = null;

    public DataCiteIdentifierProviderTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
            throws Exception
    {
        Context ctx = new Context();
        ctx.turnOffAuthorisationSystem();
        Community community = Community.create(null, ctx);
        Collection collection = community.createCollection();
        WorkspaceItem wsItem = WorkspaceItem.create(ctx, collection, false);
        item = wsItem.getItem();
        ctx.complete();
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
     * Test of supports method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testSupports_Class()
    {
        System.out.println("supports");
        DataCiteIdentifierProvider instance = new DataCiteIdentifierProvider();

        Class<? extends Identifier> identifier = DOI.class;
        boolean result = instance.supports(identifier);
        assertTrue("DOI should be supported", result);
    }

    /**
     * Test of supports method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testSupports_String()
    {
        System.out.println("supports");
        DataCiteIdentifierProvider instance = new DataCiteIdentifierProvider();

        String identifier = "";
        boolean result = instance.supports(identifier);
        assertTrue("blah should be supported", result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of register method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testRegister_Context_DSpaceObject()
            throws Exception
    {
        System.out.println("register");
        DataCiteIdentifierProvider instance = new DataCiteIdentifierProvider();

        DSpaceObject dso = item;
        String expResult = "";
        String result = instance.register(context, dso);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of register method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testRegister_3args()
    {
        System.out.println("register");
        DataCiteIdentifierProvider instance = new DataCiteIdentifierProvider();

        DSpaceObject object = item;
        String identifier = "";
        instance.register(context, object, identifier);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of reserve method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testReserve()
            throws Exception
    {
        System.out.println("reserve");
        DataCiteIdentifierProvider instance = new DataCiteIdentifierProvider();

        DSpaceObject dso = item;
        String identifier = "";
        instance.reserve(context, dso, identifier);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of mint method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testMint()
            throws Exception
    {
        System.out.println("mint");
        DataCiteIdentifierProvider instance = new DataCiteIdentifierProvider();

        DSpaceObject dso = item;
        String expResult = "";
        String result = instance.mint(context, dso);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of resolve method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testResolve()
            throws Exception
    {
        System.out.println("resolve");
        DataCiteIdentifierProvider instance = new DataCiteIdentifierProvider();

        String identifier = "";
        String[] attributes = null;
        DSpaceObject expResult = null;
        DSpaceObject result = instance.resolve(context, identifier, attributes);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of lookup method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testLookup()
            throws Exception
    {
        System.out.println("lookup");
        DataCiteIdentifierProvider instance = new DataCiteIdentifierProvider();

        DSpaceObject object = item;
        String expResult = "";
        String result = instance.lookup(context, object);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of delete method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testDelete_Context_DSpaceObject()
            throws Exception
    {
        System.out.println("delete");
        DataCiteIdentifierProvider instance = new DataCiteIdentifierProvider();

        DSpaceObject dso = item;
        instance.delete(context, dso);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of delete method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testDelete_3args()
            throws Exception
    {
        System.out.println("delete");
        DataCiteIdentifierProvider instance = new DataCiteIdentifierProvider();

        DSpaceObject dso = item;
        String identifier = "";
        instance.delete(context, dso, identifier);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
}
