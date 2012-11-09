/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.Context;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.ConfigurationService;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;
import org.junit.*;
import static org.junit.Assert.*;

/**
 * Tests for {@link DataCiteIdentifierProvider}.
 * 
 * @author Mark H. Wood
 */
public class DataCiteIdentifierProviderTest
        extends AbstractUnitTest
{
    /** Name of the reserved EZID test authority */
    private static final String TEST_SHOULDER = "10.5072/FK2";

    private static ServiceManager sm = null;

    private static ConfigurationService config = null;

    private static Community community;

    private static Collection collection;

    /** The most recently created test Item's ID */
    private static int itemID;

    public DataCiteIdentifierProviderTest()
    {
    }

    private static void dumpMetadata(Item eyetem)
    {
        DCValue[] metadata = eyetem.getMetadata("dc", Item.ANY, Item.ANY, Item.ANY);
        for (DCValue metadatum : metadata)
            System.out.printf("Metadata:  %s.%s.%s(%s) = %s\n",
                    metadatum.schema,
                    metadatum.element,
                    metadatum.qualifier,
                    metadatum.language,
                    metadatum.value);
    }

    /**
     * Create a fresh Item, installed in the repository.
     * 
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException 
     */
    private Item newItem(Context ctx)
            throws SQLException, AuthorizeException, IOException
    {
        ctx.turnOffAuthorisationSystem();
        ctx.setCurrentUser(eperson);

        WorkspaceItem wsItem = WorkspaceItem.create(ctx, collection, false);

        WorkflowItem wfItem = WorkflowManager.start(ctx, wsItem);
        WorkflowManager.advance(ctx, wfItem, ctx.getCurrentUser());

        Item item = wfItem.getItem();
        item.addMetadata("dc", "contributor", "author", null, "Author, A. N.");
        item.addMetadata("dc", "title", null, null, "A Test Object");
        item.addMetadata("dc", "publisher", null, null, "DSpace Test Harness");
        item.update();
        itemID = item.getID();

        ctx.commit();
        ctx.restoreAuthSystemState();

        return item;
    }

    @BeforeClass
    public static void setUpClass()
            throws Exception
    {
        Context ctx = new Context();
        ctx.turnOffAuthorisationSystem();

        ctx.setCurrentUser(eperson);

        // Create an environment for our test objects to live in.
        community = Community.create(null, ctx);
        community.setMetadata("name", "A Test Community");
        community.update();

        collection = community.createCollection();
        collection.setMetadata("name", "A Test Collection");
        collection.update();

        ctx.complete();

        // Find the usual kernel services
        sm = kernelImpl.getServiceManager();

        config = kernelImpl.getConfigurationService();

        // Configure the service under test.
        config.setProperty(DataCiteIdentifierProvider.CFG_SHOULDER, TEST_SHOULDER);
        config.setProperty(DataCiteIdentifierProvider.CFG_USER, "apitest");
        config.setProperty(DataCiteIdentifierProvider.CFG_PASSWORD, "apitest");

        // Don't try to send mail.
        config.setProperty("mail.server.disabled", "true");
    }

    @AfterClass
    public static void tearDownClass()
            throws Exception
    {
        /*
        System.out.print("Tearing down\n\n");
        Context ctx = new Context();
        dumpMetadata(Item.find(ctx, itemID));
        */
    }
    
    @Before
    public void setUp()
    {
        context.setCurrentUser(eperson);
        context.turnOffAuthorisationSystem();
    }
    
    @After
    public void tearDown()
    {
        context.restoreAuthSystemState();
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

        String identifier = "doi:" + TEST_SHOULDER;
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
        System.out.println("register 2");

        List<DataCiteIdentifierProvider> instance
                = (List<DataCiteIdentifierProvider>)
                sm.getServicesByType(DataCiteIdentifierProvider.class);

        DSpaceObject dso = newItem(context);

        String result = instance.register(context, dso);
        assertTrue("Didn't get a DOI back", result.startsWith("doi:" + TEST_SHOULDER));
        System.out.println(" got identifier:  " + result);
    }

    /**
     * Test of register method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testRegister_3args()
            throws SQLException, AuthorizeException, IOException
    {
        System.out.println("register 3");

        List<DataCiteIdentifierProvider> instances
                = (List<DataCiteIdentifierProvider>)
                sm.getServicesByType(DataCiteIdentifierProvider.class);

        DSpaceObject object = newItem(context);

        String identifier = UUID.randomUUID().toString();

        instances.get(0).register(context, object, identifier);
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

        DSpaceObject dso = newItem(context);
        String identifier = UUID.randomUUID().toString();
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
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");

        DataCiteIdentifierProvider instance = new DataCiteIdentifierProvider();

        DSpaceObject dso = newItem(context);
        String result = instance.mint(context, dso);
        assertEquals(expResult, result);
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

        String identifier = UUID.randomUUID().toString();
        DSpaceObject expResult = newItem(context);
        instance.register(context, expResult, identifier);

        String[] attributes = null;
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
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");

        DataCiteIdentifierProvider instance = new DataCiteIdentifierProvider();

        String identifier = UUID.randomUUID().toString();
        DSpaceObject object = newItem(context);
        instance.register(context, object, identifier);

        String result = instance.lookup(context, object);
        assertEquals(expResult, result);
    }

    /**
     * Test of delete method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testDelete_Context_DSpaceObject()
            throws Exception
    {
        System.out.println("delete 2");

        DataCiteIdentifierProvider instance = new DataCiteIdentifierProvider();

        DSpaceObject dso = newItem(context);

        // Ensure that it has multiple DOIs (ooo, bad boy!)
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        instance.reserve(context, dso, id1);
        instance.reserve(context, dso, id2);

        // Test deletion
        try {
            instance.delete(context, dso);
        } catch (IdentifierException e) {
            // Creation of the Item registers a "public" identifier, which can't be deleted.
            assertEquals("Unexpected exception", "1 identifiers could not be deleted.", e.getMessage());
        }

        // See if those identifiers were really deleted.
        ItemIterator found;
        found = Item.findByMetadataField(context,
                DataCiteIdentifierProvider.MD_SCHEMA,
                DataCiteIdentifierProvider.DOI_ELEMENT,
                DataCiteIdentifierProvider.DOI_QUALIFIER, id1);
        assertFalse("A test identifier is still present", found.hasNext());

        found = Item.findByMetadataField(context,
                DataCiteIdentifierProvider.MD_SCHEMA,
                DataCiteIdentifierProvider.DOI_ELEMENT,
                DataCiteIdentifierProvider.DOI_QUALIFIER, id2);
        assertFalse("A test identifier is still present", found.hasNext());
    }

    /**
     * Test of delete method, of class DataCiteIdentifierProvider.
     */
    @Test()
    public void testDelete_3args()
            throws Exception
    {
        System.out.println("delete 3");

        DataCiteIdentifierProvider instance = new DataCiteIdentifierProvider();

        DSpaceObject dso = newItem(context);
        String identifier = UUID.randomUUID().toString();

        // Set a known identifier on the object
        instance.reserve(context, dso, identifier);

        // Test deletion
        instance.delete(context, dso, identifier);

        // See if it is gone
        ItemIterator found = Item.findByMetadataField(context,
                DataCiteIdentifierProvider.MD_SCHEMA,
                DataCiteIdentifierProvider.DOI_ELEMENT,
                DataCiteIdentifierProvider.DOI_QUALIFIER, identifier);
        assertFalse("Test identifier is still present", found.hasNext());
    }
}
