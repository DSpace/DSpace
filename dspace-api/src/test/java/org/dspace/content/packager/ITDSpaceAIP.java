/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.packager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import mockit.NonStrictExpectations;
import org.apache.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.*;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.dspace.core.*;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.WorkflowException;
import org.junit.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.rules.TemporaryFolder;

/**
 * Basic integration testing for the AIP Backup and Restore feature
 * https://wiki.duraspace.org/display/DSDOC5x/AIP+Backup+and+Restore
 * 
 * @author Tim Donohue
 */
public class ITDSpaceAIP extends AbstractUnitTest
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(ITDSpaceAIP.class);
    
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();
    protected ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    protected ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    /** InfoMap multiple value separator (see saveObjectInfo() and assertObject* methods) **/
    private static final String valueseparator = "::";
    
    /** Handles for Test objects initialized in setUpClass() and used in various tests below **/
    private static String topCommunityHandle = null;
    private static String testCollectionHandle = null;
    private static String testItemHandle = null;
    private static String testMappedItemHandle = null;
    private static String submitterEmail = "aip-test@dspace.org";
    private Context context;
    
    /** Create a global temporary upload folder which will be cleaned up automatically by JUnit.
        NOTE: As a ClassRule, this temp folder is shared by ALL tests below. **/
    @ClassRule
    public static final TemporaryFolder uploadTempFolder = new TemporaryFolder();

    /** Create another temporary folder for AIPs. As a Rule, this one is *recreated* for each
        test, in order to ensure each test is standalone with respect to AIPs. **/
    @Rule
    public final TemporaryFolder aipTempFolder = new TemporaryFolder();
    
    /**
     * This method will be run during class initialization. It will initialize
     * shared resources required for all the tests. It is only run once.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @BeforeClass
    public static void setUpClass()
    {
        try
        {
            Context context = new Context();
            // Create a dummy Community hierarchy to test with
            // Turn off authorization temporarily to create some test objects.
            context.turnOffAuthorisationSystem();

            CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
            CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
            ItemService itemService = ContentServiceFactory.getInstance().getItemService();
            BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
            WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
            InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();

            log.info("setUpClass() - CREATE TEST HIERARCHY");
            // Create a hierachy of sub-Communities and Collections and Items, 
            // which looks like this:
            //  "Top Community"
            //      - "Child Community"
            //          - "Grandchild Community"
            //              - "GreatGrandchild Collection"
            //                  - "GreatGrandchild Collection Item #1"
            //                  - "GreatGrandchild Collection Item #2"
            //                  - "Mapped Item" (mapped collection)
            //          - "Grandchild Collection"
            //              - "Grandchild Collection Item #1"
            //              - "Mapped Item" (owning collection)
            //
            Community topCommunity = communityService.create(null, context);
            communityService.addMetadata(context, topCommunity, MetadataSchema.DC_SCHEMA, "title", null, null, "Top Community");
            communityService.update(context, topCommunity);
            topCommunityHandle = topCommunity.getHandle();
            
            Community child = communityService.createSubcommunity(context, topCommunity);
            communityService.addMetadata(context, child, MetadataSchema.DC_SCHEMA, "title", null, null, "Child Community");
            communityService.update(context, child);
            
            Community grandchild = communityService.createSubcommunity(context, child);
            communityService.addMetadata(context, grandchild, MetadataSchema.DC_SCHEMA, "title", null, null, "Grandchild Community");
            communityService.update(context, grandchild);

            // Create our primary Test Collection
            Collection grandchildCol = collectionService.create(context, child);
            collectionService.addMetadata(context, grandchildCol, "dc", "title", null, null, "Grandchild Collection");
            collectionService.update(context, grandchildCol);
            testCollectionHandle = grandchildCol.getHandle();

            // Create an additional Test Collection
            Collection greatgrandchildCol = collectionService.create(context, grandchild);
            collectionService.addMetadata(context, greatgrandchildCol, "dc", "title", null, null, "GreatGrandchild Collection");
            collectionService.update(context, greatgrandchildCol);

            EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
            EPerson submitter = ePersonService.create(context);
            submitter.setEmail(submitterEmail);
            ePersonService.update(context, submitter);
            context.setCurrentUser(submitter);

            // Create our primary Test Item
            WorkspaceItem wsItem = workspaceItemService.create(context, grandchildCol, false);
            Item item = installItemService.installItem(context, wsItem);
            itemService.addMetadata(context, item, "dc", "title", null, null, "Grandchild Collection Item #1");
            // For our primary test item, create a Bitstream in the ORIGINAL bundle
            File f = new File(testProps.get("test.bitstream").toString());
            Bitstream b = itemService.createSingleBitstream(context, new FileInputStream(f), item);
            b.setName(context, "Test Bitstream");
            bitstreamService.update(context, b);
            itemService.update(context, item);
            testItemHandle = item.getHandle();

            // Create a Mapped Test Item (mapped to multiple collections
            WorkspaceItem wsItem2 = workspaceItemService.create(context, grandchildCol, false);
            Item item2 = installItemService.installItem(context, wsItem2);
            itemService.addMetadata(context, item2, "dc", "title", null, null, "Mapped Item");
            itemService.update(context, item2);
            collectionService.addItem(context, greatgrandchildCol, item2);
            testMappedItemHandle = item2.getHandle();

            WorkspaceItem wsItem3 = workspaceItemService.create(context, greatgrandchildCol, false);
            Item item3 = installItemService.installItem(context, wsItem3);
            itemService.addMetadata(context, item3, "dc", "title", null, null, "GreatGrandchild Collection Item #1");
            itemService.update(context, item3);

            WorkspaceItem wsItem4 = workspaceItemService.create(context, greatgrandchildCol, false);
            Item item4 = installItemService.installItem(context, wsItem4);
            itemService.addMetadata(context, item4, "dc", "title", null, null, "GreatGrandchild Collection Item #2");
            itemService.update(context, item4);

            // Commit these changes to our DB
            context.restoreAuthSystemState();
            context.complete();
        }
        catch (AuthorizeException ex)
        {
            log.error("Authorization Error in setUpClass()", ex);
            fail("Authorization Error in setUpClass(): " + ex.getMessage());
        }
        catch (IOException ex)
        {
            log.error("IO Error in setUpClass()", ex);
            fail("IO Error in setUpClass(): " + ex.getMessage());
        }
        catch (SQLException ex)
        {
            log.error("SQL Error in setUpClass()", ex);
            fail("SQL Error in setUpClass(): " + ex.getMessage());
        }
    }
    
    /**
     * This method will be run once at the very end
     */
    @AfterClass
    public static void tearDownClass()
    {
        try
        {
            Context context = new Context();
            CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
            HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
            Community topCommunity = (Community) handleService.resolveToObject(context, topCommunityHandle);

            // Delete top level test community and test hierarchy under it
            if(topCommunity!=null)
            {
                log.info("tearDownClass() - DESTROY TEST HIERARCHY");
                context.turnOffAuthorisationSystem();
                communityService.delete(context, topCommunity);
                context.restoreAuthSystemState();
                context.commit();
            }

            // Delete the Eperson created to submit test items
            EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
            EPerson eperson = ePersonService.findByEmail(context, submitterEmail);
            if(eperson!=null)
            {
                log.info("tearDownClass() - DESTROY TEST EPERSON");
                context.turnOffAuthorisationSystem();
                ePersonService.delete(context, eperson);
                context.restoreAuthSystemState();
                context.commit();
            }

            if(context.isValid())
                context.abort();
        }
        catch (Exception ex)
        {
            log.error("Error in tearDownClass()", ex);
        }
    }
    
    /**
     * Create an initial set of AIPs for the test content generated in setUpClass() above.
     */
    @Before
    @Override
    public void init()
    {
        // call init() from AbstractUnitTest to initialize testing framework
        super.init();

        // Override default value of configured temp directory to point at our
        // JUnit TemporaryFolder. This ensures Crosswalk classes like RoleCrosswalk
        // store their temp files in a place where JUnit can clean them up automatically.
        new NonStrictExpectations(configService.getClass()) {{
            configService.getProperty("upload.temp.dir"); result = uploadTempFolder.getRoot().getAbsolutePath();
        }};
        
        try
        {
            context = new Context();
            context.setCurrentUser(EPersonServiceFactory.getInstance().getEPersonService().findByEmail(context, submitterEmail));
        }
        catch (SQLException ex)
        {
            log.error("SQL Error in init()", ex);
            fail("SQL Error in init(): " + ex.getMessage());
        }
    }

    @After
    @Override
    public void destroy() {
        context.abort();
        super.destroy();
    }

    /**
     * Test restoration from AIP of entire Community Hierarchy
     */
    @Test
    public void testRestoreCommunityHierarchy() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Full Admin permissions. Since we are working with an object
            // hierarchy you really need full admin rights
            authorizeService.isAdmin((Context) any); result = true;
        }};

        log.info("testRestoreCommunityHierarchy() - BEGIN");

        // Locate the top level community (from our test data)
        Community topCommunity = (Community) handleService.resolveToObject(context, topCommunityHandle);

        // Get parent object, so that we can restore to same parent later
        DSpaceObject parent = communityService.getParentObject(context, topCommunity);

        // Save basic info about top community (and children) to an infoMap
        HashMap<String,String> infoMap = new HashMap<String,String>();
        saveObjectInfo(topCommunity, infoMap);

        // Export community & child AIPs
        log.info("testRestoreCommunityHierarchy() - CREATE AIPs");
        File aipFile = createAIP(topCommunity, null, true);

        // Delete everything from parent community on down
        log.info("testRestoreCommunityHierarchy() - DELETE Community Hierarchy");
        communityService.delete(context, topCommunity);

        // Assert all objects in infoMap no longer exist in DSpace
        assertObjectsNotExist(infoMap);

        // Restore this Community (recursively) from AIPs
        log.info("testRestoreCommunityHierarchy() - RESTORE Community Hierarchy");
        // Ensure "skipIfParentMissing" flag is set to true. 
        // As noted in the documentation, this is often needed for larger, hierarchical
        // restores when you have Mapped Items (which we do in our test data)
        PackageParameters pkgParams = new PackageParameters();
        pkgParams.addProperty("skipIfParentMissing", "true");
        restoreFromAIP(parent, aipFile, pkgParams, true);

        // Assert all objects in infoMap now exist again!
        assertObjectsExist(infoMap);
        
        // SPECIAL CASE: Test Item Mapping restoration was successful
        // In our community, we have one Item which should be in two Collections
        Item mappedItem = (Item) handleService.resolveToObject(context, testMappedItemHandle);
        assertEquals("testRestoreCommunityHierarchy() - Mapped Item's Collection mappings restored", 2, mappedItem.getCollections().size());
        
        log.info("testRestoreCommunityHierarchy() - END");
    }
    
    /**
     * Test restoration from AIP of an access restricted Community
     */
    @Test
    public void testRestoreRestrictedCommunity() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Full Admin permissions. Since we are working with an object
            // hierarchy  (Items/Bundles/Bitstreams) you need full admin rights
            authorizeService.isAdmin((Context) any); result = true;
        }};

        log.info("testRestoreRestrictedCommunity() - BEGIN");

        // Locate the top-level Community (as a parent)
        Community parent = (Community) handleService.resolveToObject(context, topCommunityHandle);

        // Create a brand new (empty) Community to test with
        Community community = communityService.createSubcommunity(context, parent);
        communityService.addMetadata(context, community, "dc", "title", null, null, "Restricted Community");
        communityService.update(context, community);
        String communityHandle = community.getHandle();

        // Create a new Group to access restrict to
        Group group = groupService.create(context);
        groupService.setName(group, "Special Users");
        groupService.update(context, group);

        // Create a custom resource policy for this community
        List<ResourcePolicy> policies = new ArrayList<>();
        ResourcePolicy policy = resourcePolicyService.create(context);
        policy.setRpName("Special Read Only");
        policy.setGroup(group);
        policy.setAction(Constants.READ);
        policies.add(policy);

        // Replace default community policies with this new one
        authorizeService.removeAllPolicies(context, community);
        authorizeService.addPolicies(context, policies, community);

        // Export collection AIP
        log.info("testRestoreRestrictedCommunity() - CREATE Community AIP");
        File aipFile = createAIP(community, null, false);

        // Now, delete that Collection
        log.info("testRestoreRestrictedCommunity() - DELETE Community");
        communityService.removeSubcommunity(context, parent, community);

        // Assert the deleted collection no longer exists
        DSpaceObject obj = handleService.resolveToObject(context, communityHandle);
        assertThat("testRestoreRestrictedCommunity() Community " + communityHandle + " doesn't exist", obj, nullValue());

        // Restore Collection from AIP (non-recursive)
        log.info("testRestoreRestrictedCommunity() - RESTORE Community");
        restoreFromAIP(parent, aipFile, null, false);

        // Assert the deleted Collection is RESTORED
        DSpaceObject objRestored = handleService.resolveToObject(context, communityHandle);
        assertThat("testRestoreRestrictedCommunity() Community " + communityHandle + " exists", objRestored, notNullValue());

        // Assert the number of restored policies is equal
        List<ResourcePolicy> policiesRestored = authorizeService.getPolicies(context, objRestored);
        assertEquals("testRestoreRestrictedCommunity() restored policy count equal", policies.size(), policiesRestored.size());

        // Assert the restored policy has same name, group and permission settings
        ResourcePolicy restoredPolicy = policiesRestored.get(0);
        assertEquals("testRestoreRestrictedCommunity() restored policy group successfully", policy.getGroup().getName(), restoredPolicy.getGroup().getName());
        assertEquals("testRestoreRestrictedCommunity() restored policy action successfully", policy.getAction(), restoredPolicy.getAction());
        assertEquals("testRestoreRestrictedCommunity() restored policy name successfully", policy.getRpName(), restoredPolicy.getRpName());

        log.info("testRestoreRestrictedCommunity() - END");
    }
    
    /**
     * Test replacement from AIP of entire Community Hierarchy
     */
    @Test
    public void testReplaceCommunityHierarchy() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Full Admin permissions. Since we are working with an object
            // hierarchy you really need full admin rights
            authorizeService.isAdmin((Context) any); result = true;
        }};

        log.info("testReplaceCommunityHierarchy() - BEGIN");

        // Locate the top level community (from our test data)
        Community topCommunity = (Community) handleService.resolveToObject(context, topCommunityHandle);

        // Get the count of collections under our Community or any Sub-Communities
        int numberOfCollections = communityService.getAllCollections(context, topCommunity).size();
        
        // Export community & child AIPs
        log.info("testReplaceCommunityHierarchy() - CREATE AIPs");
        File aipFile = createAIP(topCommunity, null, true);
        
        // Get some basic info about Collection to be deleted
        // In this scenario, we'll delete the test "Grandchild Collection" 
        // (which is initialized as being under the Top Community)
        String deletedCollectionHandle = testCollectionHandle;
        Collection collectionToDelete = (Collection) handleService.resolveToObject(context, deletedCollectionHandle);
        Community parent = (Community) collectionService.getParentObject(context, collectionToDelete);
         
        // How many items are in this Collection we are about to delete?
        int numberOfItems = itemService.countItems(context, collectionToDelete);
        // Get an Item that should be deleted when we delete this Collection
        // (NOTE: This item is initialized to be a member of the deleted Collection)
        String deletedItemHandle = testItemHandle;
        
        // Now, delete that one collection
        log.info("testReplaceCommunityHierarchy() - DELETE Collection");
        communityService.removeCollection(context, parent, collectionToDelete);

        // Assert the deleted collection no longer exists
        DSpaceObject obj = handleService.resolveToObject(context, deletedCollectionHandle);
        assertThat("testReplaceCommunityHierarchy() collection " + deletedCollectionHandle + " doesn't exist", obj, nullValue());
        
        // Assert the child item no longer exists
        DSpaceObject obj2 = handleService.resolveToObject(context, deletedItemHandle);
        assertThat("testReplaceCommunityHierarchy() item " + deletedItemHandle + " doesn't exist", obj2, nullValue());

        // Replace Community (and all child objects, recursively) from AIPs
        log.info("testReplaceCommunityHierarchy() - REPLACE Community Hierarchy");
        // Ensure "skipIfParentMissing" flag is set to true. 
        // As noted in the documentation, this is often needed for larger, hierarchical
        // replacements when you have Mapped Items (which we do in our test data)
        PackageParameters pkgParams = new PackageParameters();
        pkgParams.addProperty("skipIfParentMissing", "true");
        replaceFromAIP(topCommunity, aipFile, pkgParams, true);

        // Assert the deleted collection is RESTORED
        DSpaceObject objRestored = handleService.resolveToObject(context, deletedCollectionHandle);
        assertThat("testReplaceCommunityHierarchy() collection " + deletedCollectionHandle + " exists", objRestored, notNullValue());
        
        // Assert the deleted item is also RESTORED
        DSpaceObject obj2Restored = handleService.resolveToObject(context, deletedItemHandle);
        assertThat("testReplaceCommunityHierarchy() item " + deletedItemHandle + " exists", obj2Restored, notNullValue());

        // Assert the Collection count and Item count are same as before
        assertEquals("testReplaceCommunityHierarchy() collection count", numberOfCollections, communityService.getAllCollections(context, topCommunity).size());
        assertEquals("testReplaceCommunityHierarchy() item count", numberOfItems, itemService.countItems(context, ((Collection) objRestored)));
    
        log.info("testReplaceCommunityHierarchy() - END");
    }
    
    /**
     * Test replacement from AIP of JUST a Community object
     */
    @Test
    public void testReplaceCommunityOnly() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Community WRITE perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.WRITE,true); result = null;
        }};
        
        log.info("testReplaceCommunityOnly() - BEGIN");

        // Locate the top level community (from our test data)
        Community topCommunity = (Community) handleService.resolveToObject(context, topCommunityHandle);

        // Get its current name / title
        String oldName = topCommunity.getName();
        
        // Export only community AIP
        log.info("testReplaceCommunityOnly() - CREATE Community AIP");
        File aipFile = createAIP(topCommunity, null, false);
        
        // Change the Community name
        String newName = "This is NOT my Community name!";
        communityService.clearMetadata(context, topCommunity, MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);
        communityService.addMetadata(context, topCommunity, MetadataSchema.DC_SCHEMA, "title", null, null, newName);

        // Ensure name is changed
        assertEquals("testReplaceCommunityOnly() new name", topCommunity.getName(), newName);
        
        // Now, replace our Community from AIP (non-recursive)
        replaceFromAIP(topCommunity, aipFile, null, false);

        // Check if name reverted to previous value
        assertEquals("testReplaceCommunityOnly() old name", topCommunity.getName(), oldName);
    }
    
    /**
     * Test restoration from AIP of entire Collection Hierarchy
     */
    @Test
    public void testRestoreCollectionHierarchy() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Full Admin permissions. Since we are working with an object
            // hierarchy you really need full admin rights
            authorizeService.isAdmin((Context) any); result = true;
        }};

        log.info("testRestoreCollectionHierarchy() - BEGIN");

        // Locate the collection (from our test data)
        Collection testCollection = (Collection) handleService.resolveToObject(context, testCollectionHandle);
        
        // Get parent object, so that we can restore to same parent later
        Community parent = (Community) collectionService.getParentObject(context, testCollection);

        // Save basic info about collection (and children) to an infoMap
        HashMap<String,String> infoMap = new HashMap<String,String>();
        saveObjectInfo(testCollection, infoMap);

        // Export collection & child AIPs
        log.info("testRestoreCollectionHierarchy() - CREATE AIPs");
        File aipFile = createAIP(testCollection, null, true);

        // Delete everything from collection on down
        log.info("testRestoreCollectionHierarchy() - DELETE Collection Hierarchy");
        communityService.removeCollection(context, parent, testCollection);

        // Assert all objects in infoMap no longer exist in DSpace
        assertObjectsNotExist(infoMap);

        // Restore this Collection (recursively) from AIPs
        log.info("testRestoreCollectionHierarchy() - RESTORE Collection Hierarchy");
        restoreFromAIP(parent, aipFile, null, true);

        // Assert all objects in infoMap now exist again!
        assertObjectsExist(infoMap);
        
        log.info("testRestoreCollectionHierarchy() - END");
    }
    
    /**
     * Test restoration from AIP of an access restricted Collection
     */
    @Test
    public void testRestoreRestrictedCollection() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Full Admin permissions. Since we are working with an object
            // hierarchy  (Items/Bundles/Bitstreams) you need full admin rights
            authorizeService.isAdmin((Context) any); result = true;
        }};

        log.info("testRestoreRestrictedCollection() - BEGIN");

        // Locate the top-level Community (as a parent)
        Community parent = (Community) handleService.resolveToObject(context, topCommunityHandle);

        // Create a brand new (empty) Collection to test with
        Collection collection = collectionService.create(context, parent);
        collectionService.addMetadata(context, collection, "dc", "title", null, null, "Restricted Collection");
        collectionService.update(context, collection);
        String collectionHandle = collection.getHandle();

        // Create a new Group to access restrict to
        Group group = groupService.create(context);
        groupService.setName(group, "Special Users");
        groupService.update(context, group);

        // Create a custom resource policy for this Collection
        List<ResourcePolicy> policies = new ArrayList<>();
        ResourcePolicy policy = resourcePolicyService.create(context);
        policy.setRpName("Special Read Only");
        policy.setGroup(group);
        policy.setAction(Constants.READ);
        policies.add(policy);

        // Replace default Collection policies with this new one
        authorizeService.removeAllPolicies(context, collection);
        authorizeService.addPolicies(context, policies, collection);

        // Export collection AIP
        log.info("testRestoreRestrictedCollection() - CREATE Collection AIP");
        File aipFile = createAIP(collection, null, false);

        // Now, delete that Collection
        log.info("testRestoreRestrictedCollection() - DELETE Collection");
        communityService.removeCollection(context, parent, collection);

        // Assert the deleted collection no longer exists
        DSpaceObject obj = handleService.resolveToObject(context, collectionHandle);
        assertThat("testRestoreRestrictedCollection() Collection " + collectionHandle + " doesn't exist", obj, nullValue());

        // Restore Collection from AIP (non-recursive)
        log.info("testRestoreRestrictedCollection() - RESTORE Collection");
        restoreFromAIP(parent, aipFile, null, false);

        // Assert the deleted Collection is RESTORED
        DSpaceObject objRestored = handleService.resolveToObject(context, collectionHandle);
        assertThat("testRestoreRestrictedCollection() Collection " + collectionHandle + " exists", objRestored, notNullValue());

        // Assert the number of restored policies is equal
        List<ResourcePolicy> policiesRestored = authorizeService.getPolicies(context, objRestored);
        assertEquals("testRestoreRestrictedCollection() restored policy count equal", policies.size(), policiesRestored.size());

        // Assert the restored policy has same name, group and permission settings
        ResourcePolicy restoredPolicy = policiesRestored.get(0);
        assertEquals("testRestoreRestrictedCollection() restored policy group successfully", policy.getGroup().getName(), restoredPolicy.getGroup().getName());
        assertEquals("testRestoreRestrictedCollection() restored policy action successfully", policy.getAction(), restoredPolicy.getAction());
        assertEquals("testRestoreRestrictedCollection() restored policy name successfully", policy.getRpName(), restoredPolicy.getRpName());

        log.info("testRestoreRestrictedCollection() - END");
    }
    
    /**
     * Test replacement from AIP of entire Collection (with Items)
     */
    @Test
    public void testReplaceCollectionHierarchy() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Full Admin permissions. Since we are working with an object
            // hierarchy you really need full admin rights
            authorizeService.isAdmin((Context) any); result = true;
        }};

        log.info("testReplaceCollectionHierarchy() - BEGIN");

        // Locate the collection (from our test data)
        Collection testCollection = (Collection) handleService.resolveToObject(context, testCollectionHandle);
        
        // How many items are in this Collection?
        int numberOfItems = itemService.countItems(context, testCollection);
        
        // Export collection & child AIPs
        log.info("testReplaceCollectionHierarchy() - CREATE AIPs");
        File aipFile = createAIP(testCollection, null, true);
        
        // Get some basic info about Item to be deleted
        // In this scenario, we'll delete the test "Grandchild Collection Item #1" 
        // (which is initialized as being an Item within this Collection)
        String deletedItemHandle = testItemHandle;
        Item itemToDelete = (Item) handleService.resolveToObject(context, deletedItemHandle);
        Collection parent = (Collection) itemService.getParentObject(context, itemToDelete);
         
        // Now, delete that one item
        log.info("testReplaceCollectionHierarchy() - DELETE Item");
        collectionService.removeItem(context, parent, itemToDelete);

        // Assert the deleted item no longer exists
        DSpaceObject obj = handleService.resolveToObject(context, deletedItemHandle);
        assertThat("testReplaceCollectionHierarchy() item " + deletedItemHandle + " doesn't exist", obj, nullValue());
        
        // Assert the item count is one less
        assertEquals("testReplaceCollectionHierarchy() updated item count for collection " + testCollectionHandle, numberOfItems - 1, itemService.countItems(context, testCollection));

        // Replace Collection (and all child objects, recursively) from AIPs
        log.info("testReplaceCollectionHierarchy() - REPLACE Collection Hierarchy");
        replaceFromAIP(testCollection, aipFile, null, true);

        // Assert the deleted item is RESTORED
        DSpaceObject objRestored = handleService.resolveToObject(context, deletedItemHandle);
        assertThat("testReplaceCollectionHierarchy() item " + deletedItemHandle + " exists", objRestored, notNullValue());
        
        // Assert the Item count is same as before
        assertEquals("testReplaceCollectionHierarchy() restored item count for collection " + testCollectionHandle, numberOfItems, itemService.countItems(context, testCollection));

        log.info("testReplaceCollectionHierarchy() - END");
    }
    
    
    /**
     * Test replacement from AIP of JUST a Collection object
     */
    @Test
    public void testReplaceCollectionOnly() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};
        
        log.info("testReplaceCollectionOnly() - BEGIN");

        // Locate the collection (from our test data)
        Collection testCollection = (Collection) handleService.resolveToObject(context, testCollectionHandle);
        
        // Get its current name / title
        String oldName = testCollection.getName();
        
        // Export only collection AIP
        log.info("testReplaceCollectionOnly() - CREATE Collection AIP");
        File aipFile = createAIP(testCollection, null, false);
        
        // Change the Collection name
        String newName = "This is NOT my Collection name!";
        collectionService.clearMetadata(context, testCollection, MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);
        collectionService.addMetadata(context, testCollection, MetadataSchema.DC_SCHEMA, "title", null, null, newName);

        // Ensure name is changed
        assertEquals("testReplaceCollectionOnly() new name", testCollection.getName(), newName);
        
        // Now, replace our Collection from AIP (non-recursive)
        replaceFromAIP(testCollection, aipFile, null, false);

        // Check if name reverted to previous value
        assertEquals("testReplaceCollectionOnly() old name", testCollection.getName(), oldName);
    }
    
    
    /**
     * Test restoration from AIP of an Item
     */
    @Test
    public void testRestoreItem() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Full Admin permissions. Since we are working with an object
            // hierarchy  (Items/Bundles/Bitstreams) you need full admin rights
            authorizeService.isAdmin((Context) any); result = true;
        }};

        log.info("testRestoreItem() - BEGIN");

        // Locate the item (from our test data)
        Item testItem = (Item) handleService.resolveToObject(context, testItemHandle);
        
        // Get information about the Item's Bitstreams
        // (There should be one bitstream initialized above)
        int bitstreamCount = 0;
        String bitstreamName = null;
        String bitstreamCheckSum = null;
        List<Bundle> bundles = itemService.getBundles(testItem, Constants.CONTENT_BUNDLE_NAME);
        if(bundles.size()>0)
        {
            List<Bitstream> bitstreams = bundles.get(0).getBitstreams();
            bitstreamCount = bitstreams.size();
            if(bitstreamCount>0)
            {
                bitstreamName = bitstreams.get(0).getName();
                bitstreamCheckSum = bitstreams.get(0).getChecksum();
            }
        }

        // We need a test bitstream to work with!
        if(bitstreamCount<=0)
            fail("No test bitstream found for Item in testRestoreItem()!");

        // Export item AIP
        log.info("testRestoreItem() - CREATE Item AIP");
        File aipFile = createAIP(testItem, null, false);
        
        // Get parent, so we can restore under the same parent
        Collection parent = (Collection) itemService.getParentObject(context, testItem);
        
        // Now, delete that item
        log.info("testRestoreItem() - DELETE Item");
        collectionService.removeItem(context, parent, testItem);

        // Assert the deleted item no longer exists
        DSpaceObject obj = handleService.resolveToObject(context, testItemHandle);
        assertThat("testRestoreItem() item " + testItemHandle + " doesn't exist", obj, nullValue());
        
        // Restore Item from AIP (non-recursive)
        log.info("testRestoreItem() - RESTORE Item");
        restoreFromAIP(parent, aipFile, null, false);

        // Assert the deleted item is RESTORED
        DSpaceObject objRestored = handleService.resolveToObject(context, testItemHandle);
        assertThat("testRestoreItem() item " + testItemHandle + " exists", objRestored, notNullValue());
        
        // Assert Bitstream exists again & is associated with restored item
        List<Bundle> restoredBund = itemService.getBundles(((Item) objRestored), Constants.CONTENT_BUNDLE_NAME);
        Bitstream restoredBitstream = bundleService.getBitstreamByName(restoredBund.get(0), bitstreamName);
        assertThat("testRestoreItem() bitstream exists", restoredBitstream, notNullValue());
        assertEquals("testRestoreItem() bitstream checksum", restoredBitstream.getChecksum(), bitstreamCheckSum);

        log.info("testRestoreItem() - END");
    }
    
    /**
     * Test restoration from AIP of an access restricted Item
     */
    @Test
    public void testRestoreRestrictedItem() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Full Admin permissions. Since we are working with an object
            // hierarchy  (Items/Bundles/Bitstreams) you need full admin rights
            authorizeService.isAdmin((Context) any); result = true;
        }};

        log.info("testRestoreRestrictedItem() - BEGIN");

        // Locate the test Collection (as a parent)
        Collection parent = (Collection) handleService.resolveToObject(context, testCollectionHandle);

        // Create a brand new Item to test with (since we will be changing policies)
        WorkspaceItem wsItem = workspaceItemService.create(context, parent, false);
        Item item = installItemService.installItem(context, wsItem);
        itemService.addMetadata(context, item, "dc", "title", null, null, "Test Restricted Item");
        // Create a test Bitstream in the ORIGINAL bundle
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream b = itemService.createSingleBitstream(context, new FileInputStream(f), item);
        b.setName(context, "Test Bitstream");
        bitstreamService.update(context, b);
        itemService.update(context, item);

        // Create a custom resource policy for this Item
        List<ResourcePolicy> policies = new ArrayList<>();
        ResourcePolicy admin_policy = resourcePolicyService.create(context);
        admin_policy.setRpName("Admin Read-Only");
        Group adminGroup = groupService.findByName(context, Group.ADMIN);
        admin_policy.setGroup(adminGroup);
        admin_policy.setAction(Constants.READ);
        policies.add(admin_policy);
        itemService.replaceAllItemPolicies(context, item, policies);

        // Export item AIP
        log.info("testRestoreRestrictedItem() - CREATE Item AIP");
        File aipFile = createAIP(item, null, false);

        // Get item handle, so we can check that it is later restored properly
        String itemHandle = item.getHandle();

        // Now, delete that item
        log.info("testRestoreRestrictedItem() - DELETE Item");
        collectionService.removeItem(context, parent, item);

        // Assert the deleted item no longer exists
        DSpaceObject obj = handleService.resolveToObject(context, itemHandle);
        assertThat("testRestoreRestrictedItem() item " + itemHandle + " doesn't exist", obj, nullValue());

        // Restore Item from AIP (non-recursive)
        log.info("testRestoreRestrictedItem() - RESTORE Item");
        restoreFromAIP(parent, aipFile, null, false);

        // Assert the deleted item is RESTORED
        DSpaceObject objRestored = handleService.resolveToObject(context, itemHandle);
        assertThat("testRestoreRestrictedItem() item " + itemHandle + " exists", objRestored, notNullValue());

        // Assert the number of restored policies is equal
        List<ResourcePolicy> policiesRestored = authorizeService.getPolicies(context, objRestored);
        assertEquals("testRestoreRestrictedItem() restored policy count equal", policies.size(), policiesRestored.size());

        // Assert the restored policy has same name, group and permission settings
        ResourcePolicy restoredPolicy = policiesRestored.get(0);
        assertEquals("testRestoreRestrictedItem() restored policy group successfully", admin_policy.getGroup().getName(), restoredPolicy.getGroup().getName());
        assertEquals("testRestoreRestrictedItem() restored policy action successfully", admin_policy.getAction(), restoredPolicy.getAction());
        assertEquals("testRestoreRestrictedItem() restored policy name successfully", admin_policy.getRpName(), restoredPolicy.getRpName());

        log.info("testRestoreRestrictedItem() - END");
    }

    /**
     * Test restoration from AIP of an Item that has no access policies associated with it.
     */
    @Test
    public void testRestoreItemNoPolicies() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Full Admin permissions. Since we are working with an object
            // hierarchy  (Items/Bundles/Bitstreams) you need full admin rights
            authorizeService.isAdmin((Context) any); result = true;
        }};

        log.info("testRestoreItemNoPolicies() - BEGIN");

        // Locate the test Collection (as a parent)
        Collection parent = (Collection) handleService.resolveToObject(context, testCollectionHandle);

        // Create a brand new Item to test with (since we will be changing policies)
        WorkspaceItem wsItem = workspaceItemService.create(context, parent, false);
        Item item = installItemService.installItem(context, wsItem);
        itemService.addMetadata(context, item, "dc", "title", null, null, "Test No Policies Item");
        // Create a test Bitstream in the ORIGINAL bundle
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream b = itemService.createSingleBitstream(context, new FileInputStream(f), item);
        b.setName(context, "Test Bitstream");
        bitstreamService.update(context, b);
        itemService.update(context, item);

        // Remove all existing policies from the Item
        authorizeService.removeAllPolicies(context, item);

        // Export item AIP
        log.info("testRestoreItemNoPolicies() - CREATE Item AIP");
        File aipFile = createAIP(item, null, false);

        // Get item handle, so we can check that it is later restored properly
        String itemHandle = item.getHandle();

        // Now, delete that item
        log.info("testRestoreItemNoPolicies() - DELETE Item");
        collectionService.removeItem(context, parent, item);

        // Assert the deleted item no longer exists
        DSpaceObject obj = handleService.resolveToObject(context, itemHandle);
        assertThat("testRestoreItemNoPolicies() item " + itemHandle + " doesn't exist", obj, nullValue());

        // Restore Item from AIP (non-recursive)
        log.info("testRestoreItemNoPolicies() - RESTORE Item");
        restoreFromAIP(parent, aipFile, null, false);

        // Assert the deleted item is RESTORED
        DSpaceObject objRestored = handleService.resolveToObject(context, itemHandle);
        assertThat("testRestoreItemNoPolicies() item " + itemHandle + " exists", objRestored, notNullValue());

        // Assert the restored item also has ZERO policies
        List<ResourcePolicy> policiesRestored = authorizeService.getPolicies(context, objRestored);
        assertEquals("testRestoreItemNoPolicies() restored policy count is zero", 0, policiesRestored.size());

        log.info("testRestoreItemNoPolicies() - END");
    }

    /**
     * Test replacement from AIP of an Item object
     */
    @Test
    public void testReplaceItem() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Full Admin permissions. Since we are working with an object
            // hierarchy (Items/Bundles/Bitstreams) you need full admin rights
            authorizeService.isAdmin((Context) any); result = true;
        }};
        
        log.info("testReplaceItem() - BEGIN");

        // Locate the item (from our test data)
        Item testItem = (Item) handleService.resolveToObject(context, testItemHandle);
        
        // Get its current name / title
        String oldName = testItem.getName();
        
        // Export item AIP
        log.info("testReplaceItem() - CREATE Item AIP");
        File aipFile = createAIP(testItem, null, false);
        
        // Change the Item name
        String newName = "This is NOT my Item name!";
        itemService.clearMetadata(context, testItem, MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);
        itemService.addMetadata(context, testItem, MetadataSchema.DC_SCHEMA, "title", null, null, newName);

        // Ensure name is changed
        assertEquals("testReplaceItem() new name", testItem.getName(), newName);
        
        // Now, replace our Item from AIP (non-recursive)
        replaceFromAIP(testItem, aipFile, null, false);

        // Check if name reverted to previous value
        assertEquals("testReplaceItem() old name", testItem.getName(), oldName);
    }
    
    /**
     * Test restoration from AIP of an Item that is mapped to multiple Collections.
     * This tests restoring the mapped Item FROM its own AIP
     */
    @Test
    public void testRestoreMappedItem() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Full Admin permissions. Since we are working with an object
            // hierarchy  (Items/Bundles/Bitstreams) you need full admin rights
            authorizeService.isAdmin((Context) any); result = true;
            authorizeService.authorizeAction(context, (Collection) any, Constants.REMOVE); result = null;
            authorizeService.authorizeAction(context, (Collection) any, Constants.ADD); result = null;
        }};

        log.info("testRestoreMappedItem() - BEGIN");

        // Get a reference to our test mapped Item
        Item item = (Item) handleService.resolveToObject(context, testMappedItemHandle);
        // Get owning Collection
        Collection owner = item.getOwningCollection();
        
        // Assert that it is in multiple collections
        List<Collection> mappedCollections = item.getCollections();
        assertEquals("testRestoreMappedItem() item " + testMappedItemHandle + " is mapped to multiple collections", 2, mappedCollections.size());
        
        // Export mapped item AIP
        log.info("testRestoreMappedItem() - CREATE Mapped Item AIP");
        File aipFile = createAIP(item, null, false);
        
        // Now, delete that item (must be removed from BOTH collections to delete it)
        log.info("testRestoreMappedItem() - DELETE Item");
        itemService.delete(context, item);

        // Assert the deleted item no longer exists
        DSpaceObject obj = handleService.resolveToObject(context, testMappedItemHandle);
        assertThat("testRestoreMappedItem() item " + testMappedItemHandle + " doesn't exist", obj, nullValue());
        
        // Restore Item from AIP (non-recursive) into its original parent collection
        log.info("testRestoreMappedItem() - RESTORE Item");
        restoreFromAIP(owner, aipFile, null, false);
        // Commit these changes to our DB

        // Assert the deleted item is RESTORED
        Item itemRestored = (Item) handleService.resolveToObject(context, testMappedItemHandle);
        assertThat("testRestoreMappedItem() item " + testMappedItemHandle + " exists", itemRestored, notNullValue());
        
        // Test that this restored Item exists in multiple Collections
        List<Collection> restoredMappings = itemRestored.getCollections();
        assertEquals("testRestoreMappedItem() collection count", 2, restoredMappings.size());
        
        log.info("testRestoreMappedItem() - END");
    }
    
    /**
     * Create AIP(s) based on a given DSpaceObject. This is a simple utility method
     * to avoid having to rewrite this code into several tests.
     * @param dso DSpaceObject to create AIP(s) for
     * @param pkParams any special PackageParameters to pass (if any)
     * @param recursive whether to recursively create AIPs or just a single AIP
     * @return exported root AIP file
     */
    private File createAIP(DSpaceObject dso, PackageParameters pkgParams, boolean recursive)
            throws PackageException, CrosswalkException, AuthorizeException, SQLException, IOException
    {
        // Get a reference to the configured "AIP" package disseminator
        PackageDisseminator dip = (PackageDisseminator) pluginService
                    .getNamedPlugin(PackageDisseminator.class, "AIP");
        if (dip == null)
        {
            fail("Could not find a disseminator for type 'AIP'");
            return null;
        }
        else
        {
            // Export file (this is placed in JUnit's temporary folder, so that it can be cleaned up after tests complete)
            File exportAIPFile = new File(aipTempFolder.getRoot().getAbsolutePath() + File.separator + PackageUtils.getPackageName(dso, "zip"));

            // If unspecified, set default PackageParameters
            if (pkgParams==null)
                pkgParams = new PackageParameters();

            // Actually disseminate the object(s) to AIPs
            if(recursive)
                dip.disseminateAll(context, dso, pkgParams, exportAIPFile);
            else
                dip.disseminate(context, dso, pkgParams, exportAIPFile);

            return exportAIPFile;
        }
    }
    
    /**
     * Restore DSpaceObject(s) from AIP(s). This is a simple utility method
     * to avoid having to rewrite this code into several tests.
     * @param parent The DSpaceObject which will be the parent object of the newly restored object(s)
     * @param aipFile AIP file to start restoration from
     * @param pkParams any special PackageParameters to pass (if any)
     * @param recursive whether to recursively restore AIPs or just a single AIP
     */
    private void restoreFromAIP(DSpaceObject parent, File aipFile, PackageParameters pkgParams, boolean recursive)
            throws PackageException, CrosswalkException, AuthorizeException, SQLException, IOException, WorkflowException {
        // Get a reference to the configured "AIP" package ingestor
        PackageIngester sip = (PackageIngester) pluginService
                    .getNamedPlugin(PackageIngester.class, "AIP");
        if(sip == null)
        {
            fail("Could not find a ingestor for type 'AIP'");
        }
        else
        {
            if(!aipFile.exists())
            {
                fail("AIP Package File does NOT exist: " + aipFile.getAbsolutePath());
            }

            // If unspecified, set default PackageParameters
            if(pkgParams==null)
                pkgParams = new PackageParameters();

            // Ensure restore mode is enabled
            pkgParams.setRestoreModeEnabled(true);

            // Actually ingest the object(s) from AIPs
            if(recursive)
                sip.ingestAll(context, parent, aipFile, pkgParams, null);
            else
                sip.ingest(context, parent, aipFile, pkgParams, null);
        }
    }
    
    /**
     * Replace DSpaceObject(s) from AIP(s). This is a simple utility method
     * to avoid having to rewrite this code into several tests.
     * @param dso The DSpaceObject to be replaced from AIP
     * @param aipFile AIP file to start replacement from
     * @param pkParams any special PackageParameters to pass (if any)
     * @param recursive whether to recursively restore AIPs or just a single AIP
     */
    private void replaceFromAIP(DSpaceObject dso, File aipFile, PackageParameters pkgParams, boolean recursive)
            throws PackageException, CrosswalkException, AuthorizeException, SQLException, IOException, WorkflowException {
        // Get a reference to the configured "AIP" package ingestor
        PackageIngester sip = (PackageIngester) pluginService
                    .getNamedPlugin(PackageIngester.class, "AIP");
        if (sip == null)
        {
            fail("Could not find a ingestor for type 'AIP'");
        }
        else
        {
            if(!aipFile.exists())
            {
                fail("AIP Package File does NOT exist: " + aipFile.getAbsolutePath());
            }

            // If unspecified, set default PackageParameters
            if (pkgParams==null)
                pkgParams = new PackageParameters();

            // Ensure restore mode is enabled
            pkgParams.setRestoreModeEnabled(true);

            // Actually replace the object(s) from AIPs
            if(recursive)
                sip.replaceAll(context, dso, aipFile, pkgParams);
            else
                sip.replace(context, dso, aipFile, pkgParams);
        }
    }
    
    /**
     * Save Object hierarchy info to the given HashMap. This utility method can
     * be used in conjunction with "assertObjectsExist" and "assertObjectsNotExist"
     * methods below, in order to assert whether a restoration succeeded or not.
     * <P>
     * In HashMap, Key is the object handle, and Value is "[type-text]::[title]".
     * @param dso DSpaceObject
     * @param infoMap HashMap
     * @throws SQLException if database error
     */
    private void saveObjectInfo(DSpaceObject dso, HashMap<String,String> infoMap)
            throws SQLException
    {
        // We need the HashMap to be non-null
        if(infoMap==null)
            return;
        
        if(dso instanceof Community)
        {
            // Save this Community's info to the infoMap
            Community community = (Community) dso;
            infoMap.put(community.getHandle(), communityService.getTypeText(community) + valueseparator + community.getName());
            
            // Recursively call method for each SubCommunity
            List<Community> subCommunities = community.getSubcommunities();
            for(Community c : subCommunities)
            {
                saveObjectInfo(c, infoMap);
            }
            
            // Recursively call method for each Collection
            List<Collection> collections = community.getCollections();
            for(Collection c : collections)
            {
                saveObjectInfo(c, infoMap);
            }
        }
        else if(dso instanceof Collection)
        {
            // Save this Collection's info to the infoMap
            Collection collection = (Collection) dso;
            infoMap.put(collection.getHandle(), collectionService.getTypeText(collection) + valueseparator + collection.getName());
            
            // Recursively call method for each Item in Collection
            Iterator<Item> items = itemService.findByCollection(context, collection);
            while(items.hasNext())
            {
                Item i = items.next();
                saveObjectInfo(i, infoMap);
            }
        }
        else if(dso instanceof Item)
        {
            // Save this Item's info to the infoMap
            Item item = (Item) dso;
            infoMap.put(item.getHandle(), itemService.getTypeText(item) + valueseparator + item.getName());
        }
    }
    
    /**
     * Assert the objects listed in a HashMap all exist in DSpace and have 
     * properties equal to HashMap value(s).
     * <P>
     * In HashMap, Key is the object handle, and Value is "[type-text]::[title]".
     * @param infoMap HashMap of objects to check for
     * @throws SQLException if database error
     */
    private void assertObjectsExist(HashMap<String,String> infoMap)
            throws SQLException
    {
        if(infoMap==null || infoMap.isEmpty())
            fail("Cannot assert against an empty infoMap");
        
        // Loop through everything in infoMap, and ensure it all exists
        for(String key : infoMap.keySet())
        {
            // The Key is the Handle, so make sure this object exists
            DSpaceObject obj = handleService.resolveToObject(context, key);
            assertThat("assertObjectsExist object " + key + " (info=" + infoMap.get(key) + ") exists", obj, notNullValue());
            
            // Get the typeText & name of this object from the values
            String info = infoMap.get(key);
            String[] values = info.split(valueseparator);
            String typeText = values[0];
            String name = values[1];
            
            // Also assert type and name are correct
            assertEquals("assertObjectsExist object " + key + " type", ContentServiceFactory.getInstance().getDSpaceObjectService(obj).getTypeText(obj), typeText);
            assertEquals("assertObjectsExist object " + key + " name", obj.getName(), name);
        }
        
    }
    
    /**
     * Assert the objects listed in a HashMap do NOT exist in DSpace.
     * @param infoMap HashMap of objects to check for
     * @throws SQLException if database error
     */
    public void assertObjectsNotExist(HashMap<String,String> infoMap)
            throws SQLException
    {
        if(infoMap==null || infoMap.isEmpty())
            fail("Cannot assert against an empty infoMap");
        
        // Loop through everything in infoMap, and ensure it all exists
        for(String key : infoMap.keySet())
        {
            // The key is the Handle, so make sure this object does NOT exist
            DSpaceObject obj = handleService.resolveToObject(context, key);
            assertThat("assertObjectsNotExist object " + key + " (info=" + infoMap.get(key) + ") doesn't exist", obj, nullValue());
        }
    }
}
