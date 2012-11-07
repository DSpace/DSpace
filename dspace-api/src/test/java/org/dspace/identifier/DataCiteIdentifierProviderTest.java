/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier;

import java.util.List;
import java.util.UUID;
import org.dspace.AbstractUnitTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.ConfigurationService;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author mwood
 */
public class DataCiteIdentifierProviderTest
        extends AbstractUnitTest
{
    private static final String TEST_SHOULDER = "doi:10.5072/FK2";

    private static ServiceManager sm = null;

    private static ConfigurationService config = null;

    private static Item item = null;

    public DataCiteIdentifierProviderTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
            throws Exception
    {
        // Create an object to work with
        Context ctx = new Context();
        ctx.turnOffAuthorisationSystem();
        Community community = Community.create(null, ctx);
        Collection collection = community.createCollection();
        WorkspaceItem wsItem = WorkspaceItem.create(ctx, collection, false);
        item = wsItem.getItem();
        ctx.complete();

        // Find the usual kernel services
        sm = kernelImpl.getServiceManager();

        config = kernelImpl.getConfigurationService();

        // Configure the service under test
        config.setProperty("identifier.doi.ezid.shoulder", TEST_SHOULDER);
        config.setProperty("identifier.doi.ezid.user", "apitest");
        config.setProperty("identifier.doi.ezid.password", "apitest");
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

        DataCiteIdentifierProvider instance
                = (DataCiteIdentifierProvider)
                sm.getServicesByType(DataCiteIdentifierProvider.class).get(0);

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

        DataCiteIdentifierProvider instance
                = (DataCiteIdentifierProvider)
                sm.getServicesByType(DataCiteIdentifierProvider.class).get(0);

        String identifier = TEST_SHOULDER;
        boolean result = instance.supports(identifier);
        assertTrue(identifier + " should be supported", result);
    }

    /**
     * Test of register method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testRegister_Context_DSpaceObject()
            throws Exception
    {
        System.out.println("register");

        DataCiteIdentifierProvider instance
                = (DataCiteIdentifierProvider)
                sm.getServicesByType(DataCiteIdentifierProvider.class).get(0);

        DSpaceObject dso = item;

        String result = instance.register(context, dso);
        assertTrue("Didn't get a DOI back", result.startsWith("doi:10.5072/"));
        System.out.println(" got identifier:  " + result);
    }

    /**
     * Test of register method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testRegister_3args()
    {
        System.out.println("register");

        DataCiteIdentifierProvider instance
                = (DataCiteIdentifierProvider)
                sm.getServicesByType(DataCiteIdentifierProvider.class).get(0);

        DSpaceObject object = item;

        String identifier = TEST_SHOULDER + UUID.randomUUID();

        instance.register(context, object, identifier);
    }

    /**
     * Test of reserve method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testReserve()
            throws Exception
    {
        System.out.println("reserve");
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");

        DataCiteIdentifierProvider instance
                = (DataCiteIdentifierProvider)
                sm.getServicesByType(DataCiteIdentifierProvider.class).get(0);

        DSpaceObject dso = item;
        String identifier = "";
        instance.reserve(context, dso, identifier);
    }

    /**
     * Test of mint method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testMint()
            throws Exception
    {
        System.out.println("mint");

        DataCiteIdentifierProvider instance
                = (DataCiteIdentifierProvider)
                sm.getServicesByType(DataCiteIdentifierProvider.class).get(0);

        DSpaceObject dso = item;
        String result = instance.mint(context, dso);
        assertNotNull("Null returned", result);
    }

    /**
     * Test of resolve method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testResolve()
            throws Exception
    {
        System.out.println("resolve");
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");

        DataCiteIdentifierProvider instance
                = (DataCiteIdentifierProvider)
                sm.getServicesByType(DataCiteIdentifierProvider.class).get(0);

        String identifier = "";
        String[] attributes = null;
        DSpaceObject expResult = null;
        DSpaceObject result = instance.resolve(context, identifier, attributes);
        assertEquals(expResult, result);
    }

    /**
     * Test of lookup method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testLookup()
            throws Exception
    {
        System.out.println("lookup");

        DataCiteIdentifierProvider instance
                = (DataCiteIdentifierProvider)
                sm.getServicesByType(DataCiteIdentifierProvider.class).get(0);

        DSpaceObject object = item;
        String result = instance.lookup(context, object);
        assertNotNull("Null returned", result);
    }

    /**
     * Test of delete method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testDelete_Context_DSpaceObject()
            throws Exception
    {
        System.out.println("delete");
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");

        DataCiteIdentifierProvider instance
                = (DataCiteIdentifierProvider)
                sm.getServicesByType(DataCiteIdentifierProvider.class).get(0);

        DSpaceObject dso = item;
        instance.delete(context, dso);
    }

    /**
     * Test of delete method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testDelete_3args()
            throws Exception
    {
        System.out.println("delete");
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");

        DataCiteIdentifierProvider instance
                = (DataCiteIdentifierProvider)
                sm.getServicesByType(DataCiteIdentifierProvider.class).get(0);

        DSpaceObject dso = item;
        String identifier = "";
        instance.delete(context, dso, identifier);
    }
}
