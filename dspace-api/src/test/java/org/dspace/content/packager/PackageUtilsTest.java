/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.packager;

import org.apache.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.MetadataSchema;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.junit.*;

import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

/**
 * @author Andrea Schweer schweer@waikato.ac.nz
 *         for the University of Waikato's Institutional Research Repositories
 */
public class PackageUtilsTest extends AbstractUnitTest
{
    private static final Logger log = Logger.getLogger(PackageUtilsTest.class);

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

    /** Handles for Test objects initialized in setUpClass() and used in various tests below **/
    private static String topCommunityHandle = null;
    private static String testCollectionHandle = null;

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

            log.info("setUpClass() - CREATE TEST HIERARCHY");
            // Create a hierachy of sub-Communities and Collections
            // which looks like this:
            //  "Top Community"
            //      - "Child Community"
            //          - "Grandchild Collection"
            //
            Community topCommunity = communityService.create(null, context);
            communityService.addMetadata(context, topCommunity, MetadataSchema.DC_SCHEMA, "title", null, null, "Top Community");
            communityService.update(context, topCommunity);
            topCommunityHandle = topCommunity.getHandle();

            Community child = communityService.createSubcommunity(context, topCommunity);
            communityService.addMetadata(context, child, MetadataSchema.DC_SCHEMA, "title", null, null, "Child Community");
            communityService.update(context, child);

            // Create our primary Test Collection
            Collection grandchildCol = collectionService.create(context, child);
            collectionService.addMetadata(context, grandchildCol, "dc", "title", null, null, "Grandchild Collection");
            collectionService.update(context, grandchildCol);
            testCollectionHandle = grandchildCol.getHandle();

            // Commit these changes to our DB
            context.restoreAuthSystemState();
            context.complete();
        }
        catch (AuthorizeException ex)
        {
            log.error("Authorization Error in setUpClass()", ex);
            fail("Authorization Error in setUpClass(): " + ex.getMessage());
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
                context.complete();
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
     * Pass through initialisation; turn off authorisation
     */
    @Before
    @Override
    public void init()
    {
        // call init() from AbstractUnitTest to initialize testing framework
        super.init();
        context.turnOffAuthorisationSystem();
    }

    @Test
    public void testCrosswalkGroupNameWithoutUnderscore() throws Exception
    {
        Collection testCollection = (Collection) handleService.resolveToObject(context, testCollectionHandle);
        Group originalFirstStepWorkflowGroup = testCollection.getWorkflowStep1();

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        Group testGroup = groupService.create(context);
        groupService.setName(testGroup, "TESTGROUP");
        testCollection.setWorkflowGroup(context, 1, testGroup);

        String exportName = PackageUtils.translateGroupNameForExport(context,
                testGroup.getName());
        assertEquals("Group name without underscore unchanged by translation for export", testGroup.getName(), exportName);

        String importName = PackageUtils.translateGroupNameForImport(context, exportName);
        assertEquals("Exported Group name without underscore unchanged by translation for import", exportName, importName);

        testCollection.setWorkflowGroup(context, 1, originalFirstStepWorkflowGroup);
    }

    @Test
    public void testCrosswalkGroupNameUnderscoresNoDSO() throws Exception
    {
        Collection testCollection = (Collection) handleService.resolveToObject(context, testCollectionHandle);
        Group originalFirstStepWorkflowGroup = testCollection.getWorkflowStep1();

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        Group testGroup = groupService.create(context);
        groupService.setName(testGroup, "TESTGROUP_ABC_TEST");
        testCollection.setWorkflowGroup(context, 1, testGroup);

        String exportName = PackageUtils.translateGroupNameForExport(context,
                testGroup.getName());
        assertEquals("Group name with underscores but no DSO unchanged by translation for export", testGroup.getName(), exportName);

        String importName = PackageUtils.translateGroupNameForImport(context, exportName);
        assertEquals("Exported Group name with underscores but no DSO unchanged by translation for import", exportName, importName);

        testCollection.setWorkflowGroup(context, 1, originalFirstStepWorkflowGroup);
    }

    @Test
    public void testCrosswalkGroupNameUnderscoresAndDSO() throws Exception
    {
        Collection testCollection = (Collection) handleService.resolveToObject(context, testCollectionHandle);
        Group originalFirstStepWorkflowGroup = testCollection.getWorkflowStep1();

        Group group = collectionService.createWorkflowGroup(context, testCollection, 1);

        String exportName = PackageUtils.translateGroupNameForExport(context,
                group.getName());
        assertNotEquals("Exported group name should differ from original", group.getName(), exportName);
        assertThat("Exported group name should contain '_hdl:' substring", exportName, containsString("_hdl:"));

        String importName = PackageUtils.translateGroupNameForImport(context, exportName);
        assertEquals("Exported Group name with dso unchanged by roundtrip translation for export/import", group.getName(), importName);

        testCollection.setWorkflowGroup(context, 1, originalFirstStepWorkflowGroup);
    }

    @After
    @Override
    public void destroy() {
        context.abort();
        context.restoreAuthSystemState();
        super.destroy();
    }

}
