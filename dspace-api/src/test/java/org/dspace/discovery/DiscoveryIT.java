/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.WorkspaceItemService;
import org.junit.Before;
import org.junit.Test;

/**
 * This class will aim to test Discovery related use cases
 */
public class DiscoveryIT extends AbstractIntegrationTestWithDatabase {

    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected SearchService searchService = SearchUtils.getSearchService();


    Community community;
    Collection col;

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
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        community = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

        col = CollectionBuilder.createCollection(context, community).build();

        leftIs = WorkspaceItemBuilder.createWorkspaceItem(context, col).withAbstract("headache").build();
        rightIs = WorkspaceItemBuilder.createWorkspaceItem(context, col).withAbstract("headache").build();

        context.restoreAuthSystemState();
    }

    @Test
    public void deleteWorkspaceItemSolrRecordAfterDeletionFromDbTest() throws Exception {
        context.turnOffAuthorisationSystem();
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setQuery("*:*");
        discoverQuery.addFilterQueries("search.resourceid:" + leftIs.getID());
        DiscoverResult discoverResult = searchService.search(context, discoverQuery);
        List<IndexableObject> indexableObjects = discoverResult.getIndexableObjects();
        assertEquals(1, indexableObjects.size());
        workspaceItemService.deleteAll(context, leftIs);
        discoverResult = searchService.search(context, discoverQuery);
        indexableObjects = discoverResult.getIndexableObjects();
        assertEquals(0, indexableObjects.size());
        context.restoreAuthSystemState();
    }
}
