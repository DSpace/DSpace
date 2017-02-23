/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.databene.contiperf.Required;
import org.databene.contiperf.PerfTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.dspace.AbstractIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * This is an integration test to ensure collections and communities interact properly.
 *
 * The code below is attached as an example. Performance checks by ContiPerf
 * can be applied at method level or at class level. This shows the syntax
 * for class-level checks.
 * @PerfTest(invocations = 1000, threads = 20)
 * @Required(max = 1200, average = 250)
 *
 * @author pvillega
 * @author tdonohue
 */
public class ITCommunityCollection extends AbstractIntegrationTest
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(ITCommunityCollection.class);

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

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
    }

    /**
     * This method will be run after every test as per @After. It will
     * clean resources initialized by the @Before methods.
     *
     * Other methods can be annotated with @After here or in subclasses
     * but no execution order is guaranteed
     */
    @After
    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * Tests the creation of a community collection tree
     */
    @Test
    @PerfTest(invocations = 25, threads = 1)
    @Required(percentile95 = 1200, average = 700, throughput = 1)
    public void testCreateTree() throws SQLException, AuthorizeException
    {
        //we create the structure
        context.turnOffAuthorisationSystem();
        Community parent = communityService.create(null, context);
        Community child1 = communityService.create(parent, context);
        
        Collection col1 = collectionService.create(context, child1);
        Collection col2 = collectionService.create(context, child1);
        
        context.restoreAuthSystemState();

        //verify it works as expected
        assertThat("testCreateTree 0", parent.getParentCommunities().size(), is(0));
        assertThat("testCreateTree 1", child1.getParentCommunities().get(0), equalTo(parent));
        assertThat("testCreateTree 2", (Community) collectionService.getParentObject(context, col1), equalTo(child1));
        assertThat("testCreateTree 3", (Community) collectionService.getParentObject(context, col2), equalTo(child1));
    }
    
    /**
     * Tests the creation of items in a community/collection tree
     */
    @Test
    @PerfTest(invocations = 25, threads = 1)
    @Required(percentile95 = 1200, average = 700, throughput = 1)
    public void testCreateItems() throws SQLException, AuthorizeException
    {
        //we create the structure
        context.turnOffAuthorisationSystem();
        Community parent = communityService.create(null, context);
        Community child1 = communityService.create(parent, context);
        
        Collection col1 = collectionService.create(context, child1);
        Collection col2 = collectionService.create(context, child1);
        
        Item item1 = installItemService.installItem(context, workspaceItemService.create(context, col1, false));
        Item item2 = installItemService.installItem(context, workspaceItemService.create(context, col2, false));

        context.restoreAuthSystemState();

        //verify it works as expected
        assertThat("testCreateItems 0", (Collection) itemService.getParentObject(context, item1), equalTo(col1));
        assertThat("testCreateItems 1", (Collection) itemService.getParentObject(context, item2), equalTo(col2));
    }

     /**
      * Tests that count items works as expected
      * NOTE: Counts are currently expensive (take a while)
      */
    @Test
    @PerfTest(invocations = 10, threads = 1)
    @Required(percentile95 = 2000, average= 1800)
    public void testCountItems() throws SQLException, AuthorizeException, IOException {
        int items_per_collection = 2;

        //we create the structure
        context.turnOffAuthorisationSystem();
        Community parentCom = communityService.create(null, context);
        Community childCom = communityService.create(parentCom, context);

        Collection col1 = collectionService.create(context, childCom);
        Collection col2 = collectionService.create(context, childCom);

        // Add same number of items to each collection
        for(int count = 0; count < items_per_collection; count++)
        {
            Item item1 = installItemService.installItem(context, workspaceItemService.create(context, col1, false));
            Item item2 = installItemService.installItem(context, workspaceItemService.create(context, col2, false));
        }
        
        // Finally, let's throw in a small wrench and add a mapped item
        // Add it to collection 1
        Item item3 = installItemService.installItem(context, workspaceItemService.create(context, col1, false));
        // Map it into collection 2
        collectionService.addItem(context, col2, item3);
        
        // Our total number of items should be
        int totalitems = items_per_collection*2 + 1;
        // Our collection counts should be
        int collTotalItems = items_per_collection + 1;

        context.restoreAuthSystemState();

        //verify it works as expected
        assertThat("testCountItems 0", itemService.countItems(context, col1), equalTo(collTotalItems));
        assertThat("testCountItems 1", itemService.countItems(context, col2), equalTo(collTotalItems));
        assertThat("testCountItems 2", itemService.countItems(context, childCom), equalTo(totalitems));
        assertThat("testCountItems 3", itemService.countItems(context, parentCom), equalTo(totalitems));
    }

     /**
      * Tests that ensure Community Admin deletion permissions are being properly
      * inherited to all objects in the Community hierarchy.
      */
    @Test
    public void testCommunityAdminDeletions() throws SQLException, AuthorizeException, IOException
    {
        //Turn off auth while we create the EPerson and structure
        context.turnOffAuthorisationSystem();

        // Create our Community Admin
        EPerson commAdmin = ePersonService.create(context);
        commAdmin.setEmail("comm-admin@dspace.org");
        ePersonService.update(context, commAdmin);

        // Create our Top-Level Community and add the user as an Administrator of that community
        Community parentCom = communityService.create(null, context);

        Group adminGroup = communityService.createAdministrators(context, parentCom);
        groupService.addMember(context, adminGroup, commAdmin);
        groupService.update(context, adminGroup);

        // Create a hierachy of sub-Communities and Collections and Items.
        Community child = communityService.createSubcommunity(context, parentCom);
        Community child2 = communityService.createSubcommunity(context, parentCom);
        Community grandchild = communityService.createSubcommunity(context, child);
        Collection childCol = collectionService.create(context, child);
        Collection grandchildCol = collectionService.create(context, grandchild);
        // Create two separate items
        WorkspaceItem wsItem = workspaceItemService.create(context, childCol, false);
        Item item = installItemService.installItem(context, wsItem);
        wsItem = workspaceItemService.create(context, childCol, false);
        Item item2 = installItemService.installItem(context, wsItem);
        // Create a bitstream for one item
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bitstream = itemService.createSingleBitstream(context, new FileInputStream(f), item);

        // Done creating the objects. Turn auth system back on
        context.restoreAuthSystemState();

        // Set the Community Admin as our current user
        context.setCurrentUser(commAdmin);

        // Test deletion of single Bitstream as a Community Admin (delete just flags as deleted)
        UUID bitstreamId = bitstream.getID();
        bitstreamService.delete(context, bitstream);
        assertTrue("Community Admin unable to flag Bitstream as deleted",
                bitstream.isDeleted());
        // NOTE: A Community Admin CANNOT "expunge" a Bitstream, as delete() removes all their permissions

        // Test deletion of single Item as a Community Admin
        UUID itemId = item2.getID();
        itemService.delete(context, item2);
        assertThat("Community Admin unable to delete sub-Item",
                itemService.find(context, itemId), nullValue());

        // Test deletion of single Collection as a Community Admin
        UUID collId = grandchildCol.getID();
        collectionService.delete(context, grandchildCol);
        assertThat("Community Admin unable to delete sub-Collection",
                collectionService.find(context, collId), nullValue());

        // Test deletion of single Sub-Community as a Community Admin
        UUID commId = child2.getID();
        communityService.delete(context, child2);
        assertThat("Community Admin unable to delete sub-Community",
                communityService.find(context, commId), nullValue());

        // Test deletion of Sub-Community Hierarchy as a Community Admin
        commId = child.getID();
        collId = childCol.getID();
        itemId = item.getID();
        communityService.delete(context, child);
        assertThat("Community Admin unable to delete sub-Community in hierarchy",
                communityService.find(context, commId), nullValue());
        assertThat("Community Admin unable to delete sub-Collection in hierarchy",
                collectionService.find(context, collId), nullValue());
        assertThat("Community Admin unable to delete sub-Item in hierarchy",
                itemService.find(context, itemId), nullValue());
    }

    /**
     * Tests that ensure Collection Admin deletion permissions are being properly
     * inherited to all objects in the Collection hierarchy.
     */
    @Test
    public void testCollectionAdminDeletions() throws SQLException, AuthorizeException, IOException
    {
        //Turn off auth while we create the EPerson and structure
        context.turnOffAuthorisationSystem();

        // Create our Collection Admin
        EPerson collAdmin = ePersonService.create(context);
        collAdmin.setEmail("coll-admin@dspace.org");
        ePersonService.update(context, collAdmin);

        // Create our Collection and add the user as an Administrator of that collection
        Community parentCom = communityService.create(null, context);
        Collection collection = collectionService.create(context, parentCom);

        Group adminGroup = collectionService.createAdministrators(context, collection);
        groupService.addMember(context, adminGroup, collAdmin);
        groupService.update(context, adminGroup);

        // Create an item in this Collection
        WorkspaceItem wsItem = workspaceItemService.create(context, collection, false);
        Item item = installItemService.installItem(context, wsItem);
        // Create two bundles in this item
        Bundle bundle = bundleService.create(context, item, "Bundle1");
        Bundle bundle2 = bundleService.create(context, item, "Bundle2");
        // Create two bitstreams, one in each bundle
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bitstream = itemService.createSingleBitstream(context, new FileInputStream(f), item, "Bundle1");
        Bitstream bitstream2 = itemService.createSingleBitstream(context, new FileInputStream(f), item, "Bundle2");
        // Done creating the objects. Turn auth system back on
        context.restoreAuthSystemState();

        // Set the Collection Admin as our current user
        context.setCurrentUser(collAdmin);

        // Test deletion of single Bitstream as a Collection Admin (delete just flags as deleted)
        UUID bitstreamId = bitstream2.getID();
        bitstreamService.delete(context, bitstream2);
        assertTrue("Collection Admin unable to flag Bitstream as deleted",
                bitstream2.isDeleted());
        // NOTE: A Collection Admin CANNOT "expunge" a Bitstream, as delete() removes all their permissions

        // Test deletion of single Bundle as a Collection Admin
        UUID bundleId = bundle2.getID();
        bundleService.delete(context, bundle2);
        assertThat("Collection Admin unable to delete Bundle",
                bundleService.find(context, bundleId), nullValue());

        // Test deletion of single Item as a Collection Admin
        UUID itemId = item.getID();
        bundleId = bundle.getID();
        bitstreamId = bitstream.getID();
        itemService.delete(context, item);
        assertThat("Collection Admin unable to delete sub-Item",
                itemService.find(context, itemId), nullValue());
        assertThat("Collection Admin unable to delete sub-Bundle",
                bundleService.find(context, bundleId), nullValue());
        assertTrue("Collection Admin unable to flag sub-Bitstream as deleted",
                bitstream.isDeleted());
    }
}
