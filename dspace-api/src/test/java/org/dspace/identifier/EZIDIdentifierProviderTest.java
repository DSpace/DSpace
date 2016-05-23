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
import java.util.List;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.identifier.ezid.DateToYear;
import org.dspace.identifier.ezid.Transform;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.junit.*;

import static org.junit.Assert.*;

/**
 *
 * @author mwood
 */
public class EZIDIdentifierProviderTest
        extends AbstractUnitTest
{
    /** Name of the reserved EZID test authority. */
    private static final String TEST_SHOULDER = "10.5072/FK2";

    /** A sensible metadata crosswalk. */
    private static final Map<String, String> aCrosswalk = new HashMap<>();
    static {
        aCrosswalk.put("datacite.creator", "dc.contributor.author");
        aCrosswalk.put("datacite.title", "dc.title");
        aCrosswalk.put("datacite.publisher", "dc.publisher");
        aCrosswalk.put("datacite.publicationyear", "dc.date.issued");
    }
    /** A sensible set of metadata transforms. */
    private static final Map<String, Transform> crosswalkTransforms = new HashMap();
    static {
        crosswalkTransforms.put("datacite.publicationyear", new DateToYear());
    }

    private static ConfigurationService config = null;

    private static Community community;

    private static Collection collection;

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();


    /** The most recently created test Item's ID */
    private static Item item;

    public EZIDIdentifierProviderTest()
    {
    }

    private void dumpMetadata(Item eyetem)
    {
        List<MetadataValue> metadata = itemService.getMetadata(eyetem, "dc", Item.ANY, Item.ANY, Item.ANY);
        for (MetadataValue metadatum : metadata)
            System.out.printf("Metadata:  %s.%s.%s(%s) = %s\n",
                    metadatum.getMetadataField().getMetadataSchema().getName(),
                    metadatum.getMetadataField().getElement(),
                    metadatum.getMetadataField().getQualifier(),
                    metadatum.getLanguage(),
                    metadatum.getValue());
    }

    /**
     * Create a fresh Item, installed in the repository.
     *
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     * @throws IOException if IO error
     */
    private Item newItem(Context ctx)
            throws SQLException, AuthorizeException, IOException, WorkflowException {
        ctx.turnOffAuthorisationSystem();
       
         //Install a fresh item
        context.turnOffAuthorisationSystem();

        WorkspaceItem wsItem = workspaceItemService.create(context, collection, false);

        WorkflowItem wfItem = WorkflowServiceFactory.getInstance().getWorkflowService().start(context, wsItem);

        item = wfItem.getItem();

        itemService.addMetadata(context, item, "dc", "contributor", "author", null, "Author, A. N.");
        itemService.addMetadata(context, item, "dc", "title", null, null, "A Test Object");
        itemService.addMetadata(context, item, "dc", "publisher", null, null, "DSpace Test Harness");

        itemService.update(context, item);

        // Commit work, clean up
        ctx.restoreAuthSystemState();

        return item;
    }

    @BeforeClass
    public static void setUpClass()
            throws Exception
    {
        // Find the configuration service
        config = DSpaceServicesFactory.getInstance().getConfigurationService();

        // Configure the service under test.
        config.setProperty(EZIDIdentifierProvider.CFG_SHOULDER, TEST_SHOULDER);
        config.setProperty(EZIDIdentifierProvider.CFG_USER, "apitest");
        config.setProperty(EZIDIdentifierProvider.CFG_PASSWORD, "apitest");

        // Don't try to send mail.
        config.setProperty("mail.server.disabled", "true");
        EZIDIdentifierProvider instance = new EZIDIdentifierProvider();
        instance.setConfigurationService(config);
        instance.setCrosswalk(aCrosswalk);
        instance.setCrosswalkTransform(crosswalkTransforms);
        instance.setItemService(ContentServiceFactory.getInstance().getItemService());
        DSpaceServicesFactory.getInstance().getServiceManager().registerServiceNoAutowire(EZIDIdentifierProvider.class.getName(), instance);
        assertNotNull(DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(EZIDIdentifierProvider.class.getName(), EZIDIdentifierProvider.class));
    }

    @AfterClass
    public static void tearDownClass()
            throws Exception
    {
        DSpaceServicesFactory.getInstance().getServiceManager().unregisterService(EZIDIdentifierProvider.class.getName());
        System.out.print("Tearing down\n\n");
    }

    @Before
    public void setUp()
            throws Exception
    {
        context.turnOffAuthorisationSystem();

        // Create an environment for our test objects to live in.
        community = communityService.create(community, context);
        communityService.setMetadata(context, community, "name", "A Test Community");
        communityService.update(context, community);

        collection = collectionService.create(context, community);
        collectionService.setMetadata(context, collection, "name", "A Test Collection");
        collectionService.update(context, collection);
    }

    @After
    public void tearDown()
            throws SQLException
    {
        context.restoreAuthSystemState();

        dumpMetadata(item);
    }

    /**
     * Test of supports method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testSupports_Class()
    {
        System.out.println("supports Class");

        EZIDIdentifierProvider instance = DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(EZIDIdentifierProvider.class.getName(), EZIDIdentifierProvider.class);

        Class<? extends Identifier> identifier = DOI.class;
        boolean result = instance.supports(identifier);
        assertTrue("DOI is supported", result);
    }

    /**
     * Test of supports method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testSupports_String()
    {
        System.out.println("supports String");

        EZIDIdentifierProvider instance = DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(EZIDIdentifierProvider.class.getName(), EZIDIdentifierProvider.class);

        String identifier = "doi:" + TEST_SHOULDER;
        boolean result = instance.supports(identifier);
        assertTrue(identifier + " is supported", result);
    }

    /**
     * Test of register method, of class EZIDIdentifierProvider.
     */
    /*
    @Test
    public void testRegister_Context_DSpaceObject()
            throws Exception
    {
        System.out.println("register Context, DSpaceObject");

        EZIDIdentifierProvider instance = new EZIDIdentifierProvider();

        DSpaceObject dso = newItem(context);

        String result = instance.register(context, dso);
        assertTrue("Didn't get a DOI back", result.startsWith("doi:" + TEST_SHOULDER));
        System.out.println(" got identifier:  " + result);
    }
    */

    /**
     * Test of register method, of class DataCiteIdentifierProvider.
     */
    /*
    @Test
    public void testRegister_3args()
            throws SQLException, AuthorizeException, IOException
    {
        System.out.println("register 3");

        EZIDIdentifierProvider instance = new EZIDIdentifierProvider();

        DSpaceObject object = newItem(context);

        String identifier = UUID.randomUUID().toString();

        instance.register(context, object, identifier);
    }
    */

    /**
     * Test of reserve method, of class DataCiteIdentifierProvider.
     */
    /*
    @Test
    public void testReserve()
            throws Exception
    {
        System.out.println("reserve");

        EZIDIdentifierProvider instance = new EZIDIdentifierProvider();

        DSpaceObject dso = newItem(context);
        String identifier = UUID.randomUUID().toString();
        instance.reserve(context, dso, identifier);
    }
    */

    /**
     * Test of mint method, of class DataCiteIdentifierProvider.
     */
    /*
    @Test
    public void testMint()
            throws Exception
    {
        System.out.println("mint");

        EZIDIdentifierProvider instance = new EZIDIdentifierProvider();

        DSpaceObject dso = newItem(context);
        String result = instance.mint(context, dso);
        assertNotNull("Non-null returned", result);
    }
    */

    /**
     * Test of resolve method, of class DataCiteIdentifierProvider.
     */
    /*
    @Test
    public void testResolve()
            throws Exception
    {
        System.out.println("resolve");

        EZIDIdentifierProvider instance = new EZIDIdentifierProvider();

        String identifier = UUID.randomUUID().toString();
        DSpaceObject expResult = newItem(context);
        instance.register(context, expResult, identifier);

        String[] attributes = null;
        DSpaceObject result = instance.resolve(context, identifier, attributes);
        assertEquals(expResult, result);
    }
    */

    /**
     * Test of lookup method, of class DataCiteIdentifierProvider.
     */
    /*
    @Test
    public void testLookup()
            throws Exception
    {
        System.out.println("lookup");

        EZIDIdentifierProvider instance = new EZIDIdentifierProvider();

        String identifier = UUID.randomUUID().toString();
        DSpaceObject object = newItem(context);
        instance.register(context, object, identifier);

        String result = instance.lookup(context, object);
        assertNotNull("Null returned", result);
    }
    */

    /**
     * Test of delete method, of class DataCiteIdentifierProvider.
     */
    /*
    @Test
    public void testDelete_Context_DSpaceObject()
            throws Exception
    {
        System.out.println("delete 2");

        EZIDIdentifierProvider instance = new EZIDIdentifierProvider();

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
                EZIDIdentifierProvider.MD_SCHEMA,
                EZIDIdentifierProvider.DOI_ELEMENT,
                EZIDIdentifierProvider.DOI_QUALIFIER, id1);
        assertFalse("A test identifier is still present", found.hasNext());

        found = Item.findByMetadataField(context,
                EZIDIdentifierProvider.MD_SCHEMA,
                EZIDIdentifierProvider.DOI_ELEMENT,
                EZIDIdentifierProvider.DOI_QUALIFIER, id2);
        assertFalse("A test identifier is still present", found.hasNext());
    }
    */

    /**
     * Test of delete method, of class EZIDIdentifierProvider.
     */
    /*
    @Test
    public void testDelete_3args()
            throws Exception
    {
        System.out.println("delete 3");

        EZIDIdentifierProvider instance = new EZIDIdentifierProvider();

        DSpaceObject dso = newItem(context);
        String identifier = UUID.randomUUID().toString();

        // Set a known identifier on the object
        instance.reserve(context, dso, identifier);

        // Test deletion
        instance.delete(context, dso, identifier);

        // See if it is gone
        ItemIterator found = Item.findByMetadataField(context,
                EZIDIdentifierProvider.MD_SCHEMA,
                EZIDIdentifierProvider.DOI_ELEMENT,
                EZIDIdentifierProvider.DOI_QUALIFIER, identifier);
        assertFalse("Test identifier is still present", found.hasNext());
    }
    */

    /**
     * Test of crosswalkMetadata method, of class EZIDIdentifierProvider.
     * @throws Exception if error
     */
    @Test
    public void testCrosswalkMetadata()
            throws Exception
    {
        System.out.println("crosswalkMetadata");

        // Set up the instance to be tested
        EZIDIdentifierProvider instance = DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(EZIDIdentifierProvider.class.getName(), EZIDIdentifierProvider.class);
//        instance.setConfigurationService(config);
//        instance.setCrosswalk(aCrosswalk);
//        instance.setCrosswalkTransform(crosswalkTransforms);

        // Let's have a fresh Item to work with
        DSpaceObject dso = newItem(context);
        String handle = dso.getHandle();

        // Test!
        Map<String, String> metadata = instance.crosswalkMetadata(context, dso);

        // Evaluate
        String target = (String) metadata.get("_target");
        assertEquals("Generates correct _target metadatum",
                config.getProperty("dspace.url") + "/handle/" + handle,
                target);
        assertTrue("Has title", metadata.containsKey("datacite.title"));
        assertTrue("Has publication year", metadata.containsKey("datacite.publicationyear"));
        assertTrue("Has publisher", metadata.containsKey("datacite.publisher"));
        assertTrue("Has creator", metadata.containsKey("datacite.creator"));

        // Dump out the generated metadata for inspection
        System.out.println("Results:");
        for (Entry metadatum : metadata.entrySet())
        {
            System.out.printf("  %s : %s\n", metadatum.getKey(), metadatum.getValue());
        }
    }
}
