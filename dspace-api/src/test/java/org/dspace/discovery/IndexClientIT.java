/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * This class will aim to test the index client
 */
public class IndexClientIT extends AbstractIntegrationTestWithDatabase {

    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected SearchService searchService;

    CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

    ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    MockSolrSearchCore solrSearchCore = DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName(null, MockSolrSearchCore.class);

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        searchService = SearchUtils.getSearchService();
    }

    /**
     * Test if index by query works by updating two items (without reindexing)
     * and updating only one of them using the --query parameter in index-discovery.
     * @throws Exception Thrown if it's not possible to fetch items from Solr.
     */
    @Test
    public void verifyIndexByQuery() throws Exception {
        context.turnOffAuthorisationSystem();

        // Create two items - one will be indexed by query, the other one will not
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1").build();

        Item item1 = ItemBuilder.createItem(context, col1)
                .withTitle("Indexed item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .withSubject("Entry to reindex")
                .build();

        Item item2 = ItemBuilder.createItem(context, col1)
                .withTitle("Indexed item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria")
                .withSubject("Do not reindex")
                .build();

        context.setDispatcher("noindex");
        configurationService.setProperty("discovery.removestale.attempts", 0);

        // Change the items without indexing
        List<MetadataValue> metadata = item1.getMetadata();
        for (MetadataValue mdv1 : metadata) {
            if (
                    mdv1.getMetadataField().getElement().equals("contributor") &&
                            mdv1.getMetadataField().getQualifier().equals("author")
            ) {
                mdv1.setValue("Doe, Jane (indexed)");
            }
        }
        item1.setMetadata(metadata);

        List<MetadataValue> metadata2 = item2.getMetadata();
        for (MetadataValue mdv2 : metadata2) {
            if (
                    mdv2.getMetadataField().getElement().equals("contributor") &&
                            mdv2.getMetadataField().getQualifier().equals("author")
            ) {
                mdv2.setValue("Kowalski, Jan (indexed)");
            }
        }
        item2.setMetadata(metadata2);

        // Run indexing by query only on the first item
        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "index-discovery", "-f", "-q", "dc.subject:(\"Entry to reindex\")" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        // Confirm only the first item has been indexed
        List<String> infoMessages = testDSpaceRunnableHandler.getInfoMessages();
        assertThat(infoMessages, hasItem(containsString("Indexing object: " + item1.getID().toString() + " [1 / 1]")));
        assertThat(infoMessages, not(hasItem(containsString("Indexing object: " + item2.getID().toString()))));

        // Fetch both items from Solr index
        QueryResponse result;
        try {
            result = solrSearchCore.getSolr().query(new SolrQuery("search.resourcetype:\"Item\""));
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }

        SolrDocumentList results = result.getResults();
        assertEquals(2, results.size());

        // Confirm that the second item's author name has not changed in solr
        for (SolrDocument entries : results) {
            if (entries.get("dc.subject").equals(List.of("Entry to reindex"))) {
                assertEquals(List.of("Doe, Jane (indexed)"), entries.get("dc.contributor.author"));
            } else {
                assertEquals(List.of("Smith, Maria"), entries.get("dc.contributor.author"));
            }
        }

        // Cleanup
        collectionService.delete(context, col1);
        context.restoreAuthSystemState();
    }
}
