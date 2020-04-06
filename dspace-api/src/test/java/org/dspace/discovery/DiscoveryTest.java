/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.AbstractIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This class will aim to test Discovery related use cases
 */
public class DiscoveryTest extends AbstractIntegrationTest {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(DiscoveryTest.class);

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected IndexingService indexingService = DSpaceServicesFactory.getInstance().getServiceManager()
                                                                     .getServiceByName(IndexingService.class.getName(),
                                                                                       IndexingService.class);
    protected SearchService searchService = SearchUtils.getSearchService();


    WorkspaceItem leftIs;
    WorkspaceItem rightIs;

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init() {
        super.init();
        try {
            context.turnOffAuthorisationSystem();
            Community community = communityService.create(null, context);

            Collection col = collectionService.create(context, community);
            leftIs = workspaceItemService.create(context, col, false);
            rightIs = workspaceItemService.create(context, col, false);

            itemService.addMetadata(context, leftIs.getItem(), "dc", "description", "abstract", null, "headache");
            itemService.addMetadata(context, rightIs.getItem(), "dc", "description", "abstract", null, "headache");

            workspaceItemService.update(context, leftIs);
            workspaceItemService.update(context, rightIs);

            indexingService.commit();

            context.restoreAuthSystemState();
            context.dispatchEvents();
        } catch (AuthorizeException ex) {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init: " + ex.getMessage());
        } catch (SQLException ex) {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("Error in init", ex);
            fail("Error in init: " + ex.getMessage());
        }
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
        context.abort();
        super.destroy();
    }

    @Test
    public void deleteWorkspaceItemSolrRecordAfterDeletionFromDbTest() throws Exception {
        context.turnOffAuthorisationSystem();
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setQuery("*:*");
        discoverQuery.addFilterQueries("search.resourceid:" + leftIs.getID());
        DiscoverResult discoverResult = searchService.search(context, discoverQuery);
        List<IndexableObject> indexableObjects = discoverResult.getIndexableObjects();
        assertTrue(indexableObjects.size() == 1);
        workspaceItemService.deleteAll(context, leftIs);
        discoverResult = searchService.search(context, discoverQuery);
        indexableObjects = discoverResult.getIndexableObjects();
        assertTrue(indexableObjects.size() == 0);
        context.restoreAuthSystemState();
    }
}
