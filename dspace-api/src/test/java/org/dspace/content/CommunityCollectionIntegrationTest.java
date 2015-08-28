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
public class CommunityCollectionIntegrationTest extends AbstractIntegrationTest
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(CommunityCollectionIntegrationTest.class);

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
        assertThat("testCreateTree 0", parent.getParentCommunities().size(), not(0));
        assertThat("testCreateTree 1", child1.getParentCommunities().get(0), equalTo(parent));
        assertThat("testCreateTree 2", (Community) collectionService.getParentObject(context, col1), equalTo(child1));
        assertThat("testCreateTree 3", (Community) collectionService.getParentObject(context, col2), equalTo(child1));
    }

     /**
      * Tests that count items works as expected
      */
    @Test
    @PerfTest(invocations = 50, threads = 1)
    @Required(percentile95 = 2000, average= 1800)
    public void testCountItems() throws SQLException, AuthorizeException, IOException {
        //make it an even number, not too high to reduce time during testing
        int totalitems = 4;

        //we create the structure
        context.turnOffAuthorisationSystem();
        Community parent = communityService.create(null, context);
        Community child1 = communityService.create(parent, context);

        Collection col1 = collectionService.create(context, child1);
        Collection col2 = collectionService.create(context, child1);

        for(int count = 0; count < totalitems/2; count++)
        {

            Item item1 = installItemService.installItem(context, workspaceItemService.create(context, col1, false));
            Item item2 = installItemService.installItem(context, workspaceItemService.create(context, col2, false));
        }

        context.restoreAuthSystemState();

        //verify it works as expected
        assertThat("testCountItems 0", itemService.countItems(context, col1), equalTo(totalitems/2));
        assertThat("testCountItems 1", itemService.countItems(context, col2), equalTo(totalitems/2));
        assertThat("testCountItems 2", communityService.countItems(context, child1), equalTo(totalitems));
        assertThat("testCountItems 3", communityService.countItems(context, parent), equalTo(totalitems));
    }
}
