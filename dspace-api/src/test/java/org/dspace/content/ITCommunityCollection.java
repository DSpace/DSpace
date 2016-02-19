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

import java.io.IOException;
import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.dspace.AbstractIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * This is an integration test to ensure collections and communities interact properly
 *
 * The code below is attached as an example. Performance checks by ContiPerf
 * can be applied at method level or at class level. This shows the syntax
 * for class-level checks.
 * @PerfTest(invocations = 1000, threads = 20)
 * @Required(max = 1200, average = 250)
 *
 * @author pvillega
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
}
