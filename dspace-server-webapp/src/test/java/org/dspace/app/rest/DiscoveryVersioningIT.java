/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.dspace.content.Relationship.LatestVersionStatus.BOTH;
import static org.dspace.content.Relationship.LatestVersionStatus.LEFT_ONLY;
import static org.dspace.content.Relationship.LatestVersionStatus.RIGHT_ONLY;
import static org.dspace.util.RelationshipVersioningUtils.isRel;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.apache.commons.lang3.function.FailableFunction;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.builder.VersionBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SolrSearchCore;
import org.dspace.versioning.Version;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * The purpose of this test class is to verify how the built-in discovery configurations behave
 * in relation to versioned items.
 * DSpace needs to index both the latest version of an item, and all previous versions, into Solr.
 * Some discovery configurations should show all versions, while others should only consider the latest versions.
 */
public class DiscoveryVersioningIT extends AbstractControllerIntegrationTest {

    @Autowired
    private SearchService searchService;

    @Autowired
    private IndexingService indexingService;

    @Autowired
    private SolrSearchCore solrSearchCore;

    @Autowired
    private ItemService itemService;

    @Autowired
    private InstallItemService installItemService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private RelationshipService relationshipService;

    protected Community community;
    protected Collection collection;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        community = CommunityBuilder.createCommunity(context)
            .withName("community")
            .build();

        collection = CollectionBuilder.createCollection(context, community)
            .withName("collection")
            .build();

        context.restoreAuthSystemState();
    }

    @Override
    @After
    public void destroy() throws Exception {
        super.destroy();
    }

    protected Matcher<? super Object> matchSearchResult(Item item, String title) {
        return allOf(
            hasJsonPath("$.type", is("discover")),
            hasJsonPath("$._links.indexableObject.href", containsString("/api/core/items/" + item.getID())),
            hasJsonPath("$._embedded.indexableObject", allOf(
                hasJsonPath("$.uuid", is(item.getID().toString())),
                hasJsonPath("$.metadata", allOf(
                    matchMetadata("dc.title", title)
                ))
            ))
        );
    }

    protected Matcher<? super Object> matchSearchResult(Community community) {
        return allOf(
            hasJsonPath("$.type", is("discover")),
            hasJsonPath("$._links.indexableObject.href", containsString("/api/core/communities/" + community.getID())),
            hasJsonPath("$._embedded.indexableObject", allOf(
                hasJsonPath("$.uuid", is(community.getID().toString()))
            ))
        );
    }

    protected Matcher<? super Object> matchSearchResult(Collection collection) {
        return allOf(
            hasJsonPath("$.type", is("discover")),
            hasJsonPath("$._links.indexableObject.href", containsString("/api/core/collections/" + collection.getID())),
            hasJsonPath("$._embedded.indexableObject", allOf(
                hasJsonPath("$.uuid", is(collection.getID().toString()))
            ))
        );
    }

    protected void verifyRestSearchObjects(
        String configuration, List<Matcher<? super Object>> searchResultMatchers
    ) throws Exception {
        verifyRestSearchObjects(null, configuration, searchResultMatchers);
    }

    protected void verifyRestSearchObjects(
        String authToken, String configuration, List<Matcher<? super Object>> searchResultMatchers
    ) throws Exception {
        verifyRestSearchObjects(authToken, configuration, (r) -> r, searchResultMatchers);
    }

    protected void verifyRestSearchObjects(
        String authToken, String configuration,
        FailableFunction<MockHttpServletRequestBuilder, MockHttpServletRequestBuilder, Exception> modifyRequest,
        List<Matcher<? super Object>> searchResultMatchers
    ) throws Exception {
        MockHttpServletRequestBuilder minRequest = get("/api/discover/search/objects")
            .param("configuration", configuration);

        MockHttpServletRequestBuilder modifiedRequest = modifyRequest.apply(minRequest);

        getClient(authToken).perform(modifiedRequest)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("discover")))
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                // assume everything fits on one page
                PageMatcher.pageEntryWithTotalPagesAndElements(
                    0, 20, searchResultMatchers.size() == 0 ? 0 : 1, searchResultMatchers.size()
                )
            )))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                searchResultMatchers
            )));
    }

    protected DiscoverResult getItemSearchResult(Item item) throws Exception {
        DiscoverQuery query = new DiscoverQuery();
        query.setQuery(String.format(
            "search.resourcetype:\"Item\" AND search.resourceid:\"%s\"", item.getID()
        ));
        return searchService.search(context, query);
    }

    protected void verifyIndexed(Item item) throws Exception {
        DiscoverResult searchResult = getItemSearchResult(item);
        Assert.assertEquals(1, searchResult.getTotalSearchResults());
    }

    protected void verifyNotIndexed(Item item) throws Exception {
        DiscoverResult searchResult = getItemSearchResult(item);
        Assert.assertEquals(0, searchResult.getTotalSearchResults());
    }

    protected void verifySolrField(Item item, String fieldName, List<Object> expectedValues) throws Exception {
        QueryResponse result = solrSearchCore.getSolr().query(new SolrQuery(String.format(
            "search.resourcetype:\"Item\" AND search.resourceid:\"%s\"", item.getID()
        )));

        SolrDocumentList docs = result.getResults();
        Assert.assertEquals(1, docs.size());
        SolrDocument doc = docs.get(0);

        java.util.Collection<Object> actualValues = doc.getFieldValues(fieldName);

        assertThat(actualValues, containsInAnyOrder(expectedValues.toArray()));
    }

    protected Item createNewVersion(Item currentItem, String newTitle) throws Exception {
        return createNewVersion(currentItem, newTitle, null);
    }

    protected Item createNewVersion(Item currentItem, String newTitle, Boolean isDiscoverable) throws Exception {
        context.turnOffAuthorisationSystem();

        // create a new version, the resulting item is not yet archived
        Version v2 = VersionBuilder.createVersion(context, currentItem, "create: " + newTitle)
            .build();
        Item newItem = v2.getItem();
        Assert.assertNotEquals(currentItem, newItem);

        if (isDiscoverable != null) {
            newItem.setDiscoverable(isDiscoverable);
        }

        // modify the new version
        itemService.replaceMetadata(
            context, newItem, "dc", "title", null, Item.ANY, newTitle, null, -1, 0
        );
        itemService.update(context, newItem);
        context.commit();

        // archive the new version, this implies that VersioningConsumer will unarchive the previous version
        installItemService.installItem(context, workspaceItemService.findByItem(context, newItem));
        context.commit();
        indexingService.commit();

        context.restoreAuthSystemState();

        return newItem;
    }

    @Test
    public void test_discoveryXml_default_expectLatestVersionsOnly() throws Exception {
        final String configuration = null;

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            .build();
        context.restoreAuthSystemState();

        // verify item 1.1 appears in the solr core
        verifyIndexed(i1_1);

        // verify item 1.1 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(community),
            matchSearchResult(collection),
            matchSearchResult(i1_1, "item 1.1")
        ));

        // create item 1.2 (second version of item 1)
        Item i1_2 = createNewVersion(i1_1, "item 1.2");

        // verify both items appear in the solr core
        verifyIndexed(i1_1);
        verifyIndexed(i1_2);

        // verify only item 1.2 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(community),
            matchSearchResult(collection),
            matchSearchResult(i1_2, "item 1.2")
        ));
    }

    @Test
    public void test_discoveryXml_defaultRelationships_allVersions() throws Exception {
        final String configuration = "default-relationships";

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            .build();
        context.restoreAuthSystemState();

        // verify item 1.1 appears in the solr core
        verifyIndexed(i1_1);

        // verify item 1.1 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(community),
            matchSearchResult(collection),
            matchSearchResult(i1_1, "item 1.1")
        ));

        // create item 1.2 (second version of item 1)
        Item i1_2 = createNewVersion(i1_1, "item 1.2");

        // verify both items appear in the solr core
        verifyIndexed(i1_1);
        verifyIndexed(i1_2);

        // verify both items appear in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(community),
            matchSearchResult(collection),
            matchSearchResult(i1_1, "item 1.1"),
            matchSearchResult(i1_2, "item 1.2")
        ));
    }

    @Test
    public void test_discoveryXml_site_expectLatestVersionsOnly() throws Exception {
        final String configuration = "site";

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            .build();
        context.restoreAuthSystemState();

        // verify item 1.1 appears in the solr core
        verifyIndexed(i1_1);

        // verify item 1.1 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(community),
            matchSearchResult(collection),
            matchSearchResult(i1_1, "item 1.1")
        ));

        // create item 1.2 (second version of item 1)
        Item i1_2 = createNewVersion(i1_1, "item 1.2");

        // verify only item 1.2 appears in the solr core
        verifyIndexed(i1_1);
        verifyIndexed(i1_2);

        // verify only item 1.2 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(community),
            matchSearchResult(collection),
            matchSearchResult(i1_2, "item 1.2")
        ));
    }

    @Test
    public void test_discoveryXml_workspace_expectLatestVersionsOnly() throws Exception {
        final String configuration = "workspace";

        // NOTE: this makes sure that the admin user is the creator of the item, so the "submitter_authority"
        //       filter passes (see SolrServiceWorkspaceWorkflowRestrictionPlugin)
        context.setCurrentUser(admin);

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            .build();
        context.restoreAuthSystemState();

        // verify item 1.1 appears in the solr core
        verifyIndexed(i1_1);

        // verify item 1.1 appears in /api/discover/search/objects
        verifyRestSearchObjects(getAuthToken(admin.getEmail(), password), configuration, List.of(
            matchSearchResult(i1_1, "item 1.1")
        ));

        // create item 1.2 (second version of item 1)
        Item i1_2 = createNewVersion(i1_1, "item 1.2");

        // verify only item 1.2 appears in the solr core
        verifyIndexed(i1_1);
        verifyIndexed(i1_2);

        // verify only item 1.2 appears in /api/discover/search/objects
        verifyRestSearchObjects(getAuthToken(admin.getEmail(), password), configuration, List.of(
            matchSearchResult(i1_2, "item 1.2")
        ));
    }

    // NOTE: no test for discovery.xml configuration "workflow", because it by definition excludes items

    // NOTE: no test for discovery.xml configuration "workflowAdmin", because it by definition excludes items

    @Test
    public void test_discoveryXml_undiscoverable_expectLatestVersionsOnly() throws Exception {
        final String configuration = "undiscoverable";

        // NOTE: needed to avoid NOT(discoverable:false) filter on solr queries (see SolrServicePrivateItemPlugin)
        //       when using the searchService directly
        context.setCurrentUser(admin);

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: necessary to get past the default filter query
            .makeUnDiscoverable()
            .build();
        context.restoreAuthSystemState();

        // verify item 1.1 appears in the solr core
        verifyIndexed(i1_1);

        // verify item 1.1 appears in /api/discover/search/objects
        verifyRestSearchObjects(getAuthToken(admin.getEmail(), password), configuration, List.of(
            matchSearchResult(i1_1, "item 1.1")
        ));

        // create item 1.2 (second version of item 1)
        // NOTE: necessary to force discoverable = false, to get past the default filter query
        Item i1_2 = createNewVersion(i1_1, "item 1.2", false);

        // verify only item 1.2 appears in the solr core
        verifyIndexed(i1_1);
        verifyIndexed(i1_2);

        // verify only item 1.2 appears in /api/discover/search/objects
        verifyRestSearchObjects(getAuthToken(admin.getEmail(), password), configuration, List.of(
            matchSearchResult(i1_2, "item 1.2")
        ));
    }

    @Test
    public void test_discoveryXml_administrativeView_expectLatestVersionsOnly() throws Exception {
        final String configuration = "administrativeView";

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            .build();
        context.restoreAuthSystemState();

        // verify item 1.1 appears in the solr core
        verifyIndexed(i1_1);

        // verify item 1.1 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1")
        ));

        // create item 1.2 (second version of item 1)
        Item i1_2 = createNewVersion(i1_1, "item 1.2");

        // verify only item 1.2 appears in the solr core
        verifyIndexed(i1_1);
        verifyIndexed(i1_2);

        // verify only item 1.2 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_2, "item 1.2")
        ));
    }

    @Test
    public void test_discoveryXml_publication_expectLatestVersionsOnly() throws Exception {
        final String configuration = "publication";

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: necessary to get past the default filter query
            .withEntityType("Publication")
            .build();
        context.restoreAuthSystemState();

        // verify item 1.1 appears in the solr core
        verifyIndexed(i1_1);

        // verify item 1.1 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1")
        ));

        // create item 1.2 (second version of item 1)
        Item i1_2 = createNewVersion(i1_1, "item 1.2");

        // verify both items appear in the solr core
        verifyIndexed(i1_1);
        verifyIndexed(i1_2);

        // verify only item 1.2 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_2, "item 1.2")
        ));
    }

    @Test
    public void test_discoveryXml_publicationRelationships_allVersions() throws Exception {
        final String configuration = "publication-relationships";

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: necessary to get past the default filter query
            .withEntityType("Publication")
            .build();
        context.restoreAuthSystemState();

        // verify item 1.1 appears in the solr core
        verifyIndexed(i1_1);

        // verify item 1.1 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1")
        ));

        // create item 1.2 (second version of item 1)
        Item i1_2 = createNewVersion(i1_1, "item 1.2");

        // verify both items appear in the solr core
        verifyIndexed(i1_1);
        verifyIndexed(i1_2);

        // verify both items appear in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1"),
            matchSearchResult(i1_2, "item 1.2")
        ));
    }

    @Test
    public void test_discoveryXml_person_expectLatestVersionsOnly() throws Exception {
        final String configuration = "person";

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: necessary to get past the default filter query
            .withEntityType("Person")
            .build();
        context.restoreAuthSystemState();

        // verify item 1.1 appears in the solr core
        verifyIndexed(i1_1);

        // verify item 1.1 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1")
        ));

        // create item 1.2 (second version of item 1)
        Item i1_2 = createNewVersion(i1_1, "item 1.2");

        // verify both items appear in the solr core
        verifyIndexed(i1_1);
        verifyIndexed(i1_2);

        // verify only item 1.2 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_2, "item 1.2")
        ));
    }

    @Test
    public void test_discoveryXml_personRelationships_allVersions() throws Exception {
        final String configuration = "person-relationships";

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: necessary to get past the default filter query
            .withEntityType("Person")
            .build();
        context.restoreAuthSystemState();

        // verify item 1.1 appears in the solr core
        verifyIndexed(i1_1);

        // verify item 1.1 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1")
        ));

        // create item 1.2 (second version of item 1)
        Item i1_2 = createNewVersion(i1_1, "item 1.2");

        // verify both items appear in the solr core
        verifyIndexed(i1_1);
        verifyIndexed(i1_2);

        // verify both items appear in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1"),
            matchSearchResult(i1_2, "item 1.2")
        ));
    }

    @Test
    public void test_discoveryXml_orgunit_expectLatestVersionsOnly() throws Exception {
        final String configuration = "orgunit";

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: necessary to get past the default filter query
            .withEntityType("OrgUnit")
            .build();
        context.restoreAuthSystemState();

        // verify item 1.1 appears in the solr core
        verifyIndexed(i1_1);

        // verify item 1.1 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1")
        ));

        // create item 1.2 (second version of item 1)
        Item i1_2 = createNewVersion(i1_1, "item 1.2");

        // verify both items appear in the solr core
        verifyIndexed(i1_1);
        verifyIndexed(i1_2);

        // verify only item 1.2 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_2, "item 1.2")
        ));
    }

    @Test
    public void test_discoveryXml_orgunitRelationships_allVersions() throws Exception {
        final String configuration = "orgunit-relationships";

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: necessary to get past the default filter query
            .withEntityType("OrgUnit")
            .build();
        context.restoreAuthSystemState();

        // verify item 1.1 appears in the solr core
        verifyIndexed(i1_1);

        // verify item 1.1 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1")
        ));

        // create item 1.2 (second version of item 1)
        Item i1_2 = createNewVersion(i1_1, "item 1.2");

        // verify both items appear in the solr core
        verifyIndexed(i1_1);
        verifyIndexed(i1_2);

        // verify both items appear in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1"),
            matchSearchResult(i1_2, "item 1.2")
        ));
    }

    @Test
    public void test_discoveryXml_journalissue_expectLatestVersionsOnly() throws Exception {
        final String configuration = "journalissue";

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: necessary to get past the default filter query
            .withEntityType("JournalIssue")
            .build();
        context.restoreAuthSystemState();

        // verify item 1.1 appears in the solr core
        verifyIndexed(i1_1);

        // verify item 1.1 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1")
        ));

        // create item 1.2 (second version of item 1)
        Item i1_2 = createNewVersion(i1_1, "item 1.2");

        // verify both items appear in the solr core
        verifyIndexed(i1_1);
        verifyIndexed(i1_2);

        // verify only item 1.2 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_2, "item 1.2")
        ));
    }

    @Test
    public void test_discoveryXml_journalissueRelationships_allVersions() throws Exception {
        final String configuration = "journalissue-relationships";

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: necessary to get past the default filter query
            .withEntityType("JournalIssue")
            .build();
        context.restoreAuthSystemState();

        // verify item 1.1 appears in the solr core
        verifyIndexed(i1_1);

        // verify item 1.1 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1")
        ));

        // create item 1.2 (second version of item 1)
        Item i1_2 = createNewVersion(i1_1, "item 1.2");

        // verify both items appear in the solr core
        verifyIndexed(i1_1);
        verifyIndexed(i1_2);

        // verify both items appear in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1"),
            matchSearchResult(i1_2, "item 1.2")
        ));
    }

    @Test
    public void test_discoveryXml_journalvolume_expectLatestVersionsOnly() throws Exception {
        final String configuration = "journalvolume";

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: necessary to get past the default filter query
            .withEntityType("JournalVolume")
            .build();
        context.restoreAuthSystemState();

        // verify item 1.1 appears in the solr core
        verifyIndexed(i1_1);

        // verify item 1.1 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1")
        ));

        // create item 1.2 (second version of item 1)
        Item i1_2 = createNewVersion(i1_1, "item 1.2");

        // verify both items appear in the solr core
        verifyIndexed(i1_1);
        verifyIndexed(i1_2);

        // verify only item 1.2 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_2, "item 1.2")
        ));
    }

    @Test
    public void test_discoveryXml_journalvolumeRelationships_allVersions() throws Exception {
        final String configuration = "journalvolume-relationships";

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: necessary to get past the default filter query
            .withEntityType("JournalVolume")
            .build();
        context.restoreAuthSystemState();

        // verify item 1.1 appears in the solr core
        verifyIndexed(i1_1);

        // verify item 1.1 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1")
        ));

        // create item 1.2 (second version of item 1)
        Item i1_2 = createNewVersion(i1_1, "item 1.2");

        // verify both items appear in the solr core
        verifyIndexed(i1_1);
        verifyIndexed(i1_2);

        // verify both items appear in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1"),
            matchSearchResult(i1_2, "item 1.2")
        ));
    }

    @Test
    public void test_discoveryXml_journal_expectLatestVersionsOnly() throws Exception {
        final String configuration = "journal";

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: necessary to get past the default filter query
            .withEntityType("Journal")
            .build();
        context.restoreAuthSystemState();

        // verify item 1.1 appears in the solr core
        verifyIndexed(i1_1);

        // verify item 1.1 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1")
        ));

        // create item 1.2 (second version of item 1)
        Item i1_2 = createNewVersion(i1_1, "item 1.2");

        // verify both items appear in the solr core
        verifyIndexed(i1_1);
        verifyIndexed(i1_2);

        // verify only item 1.2 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_2, "item 1.2")
        ));
    }

    @Test
    public void test_discoveryXml_journalRelationships_allVersions() throws Exception {
        final String configuration = "journal-relationships";

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: necessary to get past the default filter query
            .withEntityType("Journal")
            .build();
        context.restoreAuthSystemState();

        // verify item 1.1 appears in the solr core
        verifyIndexed(i1_1);

        // verify item 1.1 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1")
        ));

        // create item 1.2 (second version of item 1)
        Item i1_2 = createNewVersion(i1_1, "item 1.2");

        // verify both items appear in the solr core
        verifyIndexed(i1_1);
        verifyIndexed(i1_2);

        // verify both items appear in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1"),
            matchSearchResult(i1_2, "item 1.2")
        ));
    }

    @Test
    public void test_discoveryXml_project_expectLatestVersionsOnly() throws Exception {
        final String configuration = "project";

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: necessary to get past the default filter query
            .withEntityType("Project")
            .build();
        context.restoreAuthSystemState();

        // verify item 1.1 appears in the solr core
        verifyIndexed(i1_1);

        // verify item 1.1 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1")
        ));

        // create item 1.2 (second version of item 1)
        Item i1_2 = createNewVersion(i1_1, "item 1.2");

        // verify both items appear in the solr core
        verifyIndexed(i1_1);
        verifyIndexed(i1_2);

        // verify only item 1.2 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_2, "item 1.2")
        ));
    }

    @Test
    public void test_discoveryXml_projectRelationships_allVersions() throws Exception {
        final String configuration = "project-relationships";

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: necessary to get past the default filter query
            .withEntityType("Project")
            .build();
        context.restoreAuthSystemState();

        // verify item 1.1 appears in the solr core
        verifyIndexed(i1_1);

        // verify item 1.1 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1")
        ));

        // create item 1.2 (second version of item 1)
        Item i1_2 = createNewVersion(i1_1, "item 1.2");

        // verify both items appear in the solr core
        verifyIndexed(i1_1);
        verifyIndexed(i1_2);

        // verify both items appear in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1"),
            matchSearchResult(i1_2, "item 1.2")
        ));
    }

    @Test
    public void test_discoveryXml_personOrOrgunit_expectLatestVersionsOnly() throws Exception {
        final String configuration = "personOrOrgunit";

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: necessary to get past the default filter query
            .withEntityType("Person")
            .build();
        context.restoreAuthSystemState();

        // verify item 1.1 appears in the solr core
        verifyIndexed(i1_1);

        // verify item 1.1 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1")
        ));

        // create item 1.2 (second version of item 1)
        Item i1_2 = createNewVersion(i1_1, "item 1.2");

        // verify only item 1.2 appears in the solr core
        verifyIndexed(i1_1);
        verifyIndexed(i1_2);

        // verify only item 1.2 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_2, "item 1.2")
        ));
    }

    @Test
    public void test_discoveryXml_openAIREFundingAgency_expectLatestVersionsOnly() throws Exception {
        final String configuration = "openAIREFundingAgency";

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: necessary to get past the default filter query
            .withEntityType("OrgUnit")
            // NOTE: necessary to get past the default filter query
            .withType("FundingOrganization")
            .build();
        context.restoreAuthSystemState();

        // verify item 1.1 appears in the solr core
        verifyIndexed(i1_1);

        // verify item 1.1 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_1, "item 1.1")
        ));

        // create item 1.2 (second version of item 1)
        Item i1_2 = createNewVersion(i1_1, "item 1.2");

        // verify only item 1.2 appears in the solr core
        verifyIndexed(i1_1);
        verifyIndexed(i1_2);

        // verify only item 1.2 appears in /api/discover/search/objects
        verifyRestSearchObjects(configuration, List.of(
            matchSearchResult(i1_2, "item 1.2")
        ));
    }

    @Test
    public void test_reindexAfterUpdatingLatestVersionStatus() throws Exception {
        // NOTE: VersioningConsumer updates the latest version status of relationships
        //       this implies the relation.* fields change so the relevant items should be re-indexed

        context.turnOffAuthorisationSystem();

        EntityType publicationEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication")
            .build();

        EntityType projectEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Project")
            .build();

        RelationshipType isProjectOfPublication = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                context, publicationEntityType, projectEntityType,
                "isProjectOfPublication", "isPublicationOfProject",
                null, null, null, null
            )
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        // create publication 1.1
        Item pub1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("publication 1")
            .withMetadata("dspace", "entity", "type", publicationEntityType.getLabel())
            .build();
        String idPub1_1 = pub1_1.getID().toString();

        // create project 1.1
        Item pro1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("project 1")
            .withMetadata("dspace", "entity", "type", projectEntityType.getLabel())
            .build();
        String idPro1_1 = pro1_1.getID().toString();

        // create relationship between publication 1.1 and project 1.1
        RelationshipBuilder.createRelationshipBuilder(context, pub1_1, pro1_1, isProjectOfPublication)
            .build();

        context.restoreAuthSystemState();

        // init - test relationships of publication 1.1
        assertThat(
            relationshipService.findByItem(context, pub1_1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(pub1_1, isProjectOfPublication, pro1_1, BOTH, 0, 0)
            ))
        );

        // init - test relationships of project 1.1
        assertThat(
            relationshipService.findByItem(context, pro1_1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(pub1_1, isProjectOfPublication, pro1_1, BOTH, 0, 0)
            ))
        );

        // init - test relation.* metadata of publication 1.1
        List<MetadataValue> mdvs1 = itemService
            .getMetadata(pub1_1, "relation", "isProjectOfPublication", null, Item.ANY);
        Assert.assertEquals(1, mdvs1.size());
        assertEquals(pro1_1.getID().toString(), mdvs1.get(0).getValue());
        assertEquals(0, mdvs1.get(0).getPlace());

        // init - test relation.* metadata of project 1.1
        List<MetadataValue> mdvs2 = itemService
            .getMetadata(pro1_1, "relation", "isPublicationOfProject", null, Item.ANY);
        Assert.assertEquals(1, mdvs2.size());
        assertEquals(pub1_1.getID().toString(), mdvs2.get(0).getValue());
        assertEquals(0, mdvs2.get(0).getPlace());

        // init - search for related items of publication 1.1
        verifyRestSearchObjects(
            null, "project-relationships",
            (r) -> r.param("f.isPublicationOfProject", idPub1_1 + ",equals"),
            List.of(
                matchSearchResult(pro1_1, "project 1")
            )
        );

        // init - search for related items of project 1.1
        verifyRestSearchObjects(
            null, "publication-relationships",
            (r) -> r.param("f.isProjectOfPublication", idPro1_1 + ",equals"),
            List.of(
                matchSearchResult(pub1_1, "publication 1")
            )
        );

        // create new version of publication 1.1 => publication 1.2
        context.turnOffAuthorisationSystem();
        Item pub1_2 = VersionBuilder.createVersion(context, pub1_1, "pub 1.2").build().getItem();
        String idPub1_2 = pub1_2.getID().toString();
        context.commit();
        indexingService.commit();
        Assert.assertNotEquals(pub1_1, pub1_2);
        installItemService.installItem(context, workspaceItemService.findByItem(context, pub1_2));
        context.commit();
        indexingService.commit();
        context.restoreAuthSystemState();

        // cache busting
        pub1_1 = context.reloadEntity(pub1_1);
        pub1_2 = context.reloadEntity(pub1_2);
        pro1_1 = context.reloadEntity(pro1_1);

        // after archive pub 1.2 - test relationships of publication 1.1
        assertThat(
            relationshipService.findByItem(context, pub1_1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(pub1_1, isProjectOfPublication, pro1_1, RIGHT_ONLY, 0, 0)
            ))
        );

        // after archive pub 1.2 - test relationships of publication 1.2
        assertThat(
            relationshipService.findByItem(context, pub1_2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(pub1_2, isProjectOfPublication, pro1_1, BOTH, 0, 0)
            ))
        );

        // after archive pub 1.2 - test relationships of project 1.1
        assertThat(
            relationshipService.findByItem(context, pro1_1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(pub1_1, isProjectOfPublication, pro1_1, RIGHT_ONLY, 0, 0),
                isRel(pub1_2, isProjectOfPublication, pro1_1, BOTH, 0, 0)
            ))
        );

        // after archive pub 1.2 - test relation.* metadata of publication 1.1
        List<MetadataValue> mdvs3 = itemService
            .getMetadata(pub1_1, "relation", "isProjectOfPublication", null, Item.ANY);
        Assert.assertEquals(1, mdvs3.size());
        assertEquals(pro1_1.getID().toString(), mdvs3.get(0).getValue());
        assertEquals(0, mdvs3.get(0).getPlace());
        verifySolrField(pub1_1, "relation.isProjectOfPublication", List.of(pro1_1.getID().toString()));

        // after archive pub 1.2 - test relation.* metadata of publication 1.2
        List<MetadataValue> mdvs4 = itemService
            .getMetadata(pub1_2, "relation", "isProjectOfPublication", null, Item.ANY);
        Assert.assertEquals(1, mdvs4.size());
        assertEquals(pro1_1.getID().toString(), mdvs4.get(0).getValue());
        assertEquals(0, mdvs4.get(0).getPlace());
        verifySolrField(pub1_2, "relation.isProjectOfPublication", List.of(pro1_1.getID().toString()));

        // after archive pub 1.2 - test relation.* metadata of project 1.1
        List<MetadataValue> mdvs5 = itemService
            .getMetadata(pro1_1, "relation", "isPublicationOfProject", null, Item.ANY);
        Assert.assertEquals(1, mdvs5.size());
        assertEquals(pub1_2.getID().toString(), mdvs5.get(0).getValue());
        assertEquals(0, mdvs5.get(0).getPlace());
        verifySolrField(pro1_1, "relation.isPublicationOfProject", List.of(pub1_2.getID().toString()));

        // after archive pub 1.2 - search for related items of publication 1.1
        verifyRestSearchObjects(
            null, "project-relationships",
            (r) -> r.param("f.isPublicationOfProject", idPub1_1 + ",equals"),
            List.of()
        );

        // after archive pub 1.2 - search for related items of publication 1.2
        verifyRestSearchObjects(
            null, "project-relationships",
            (r) -> r.param("f.isPublicationOfProject", idPub1_2 + ",equals"),
            List.of(
                matchSearchResult(pro1_1, "project 1")
            )
        );

        // after archive pub 1.2 - search for related items of project 1.1
        verifyRestSearchObjects(
            null, "publication-relationships",
            (r) -> r.param("f.isProjectOfPublication", idPro1_1 + ",equals"),
            List.of(
                matchSearchResult(pub1_1, "publication 1"),
                matchSearchResult(pub1_2, "publication 1")
            )
        );

        // create new version of project 1.1 => project 1.2 BUT DON'T ARCHIVE YET
        context.turnOffAuthorisationSystem();
        Item pro1_2 = VersionBuilder.createVersion(context, context.reloadEntity(pro1_1), "pro 1.2").build().getItem();
        String idPro1_2 = pro1_2.getID().toString();
        context.commit();
        indexingService.commit();
        Assert.assertNotEquals(pro1_1, pro1_2);
        context.restoreAuthSystemState();

        // cache busting
        pub1_1 = context.reloadEntity(pub1_1);
        pub1_2 = context.reloadEntity(pub1_2);
        pro1_1 = context.reloadEntity(pro1_1);
        pro1_2 = context.reloadEntity(pro1_2);

        // after create pro 1.2 - test relationships of publication 1.1
        assertThat(
            relationshipService.findByItem(context, pub1_1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(pub1_1, isProjectOfPublication, pro1_1, RIGHT_ONLY, 0, 0)
            ))
        );

        // after create pro 1.2 - test relationships of publication 1.2
        assertThat(
            relationshipService.findByItem(context, pub1_2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(pub1_2, isProjectOfPublication, pro1_1, BOTH, 0, 0),
                isRel(pub1_2, isProjectOfPublication, pro1_2, LEFT_ONLY, 0, 0)
            ))
        );

        // after create pro 1.2 - test relationships of project 1.1
        assertThat(
            relationshipService.findByItem(context, pro1_1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(pub1_1, isProjectOfPublication, pro1_1, RIGHT_ONLY, 0, 0),
                isRel(pub1_2, isProjectOfPublication, pro1_1, BOTH, 0, 0)
            ))
        );

        // after create pro 1.2 - test relationships of project 1.2
        assertThat(
            relationshipService.findByItem(context, pro1_2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(pub1_2, isProjectOfPublication, pro1_2, LEFT_ONLY, 0, 0)
            ))
        );

        // after create pro 1.2 - test relation.* metadata of publication 1.1
        List<MetadataValue> mdvs6 = itemService
            .getMetadata(pub1_1, "relation", "isProjectOfPublication", null, Item.ANY);
        Assert.assertEquals(1, mdvs6.size());
        assertEquals(pro1_1.getID().toString(), mdvs6.get(0).getValue());
        assertEquals(0, mdvs6.get(0).getPlace());
        verifySolrField(pub1_1, "relation.isProjectOfPublication", List.of(pro1_1.getID().toString()));

        // after create pro 1.2 - test relation.* metadata of publication 1.2
        List<MetadataValue> mdvs7 = itemService
            .getMetadata(pub1_2, "relation", "isProjectOfPublication", null, Item.ANY);
        Assert.assertEquals(1, mdvs7.size());
        assertEquals(pro1_1.getID().toString(), mdvs7.get(0).getValue());
        assertEquals(0, mdvs7.get(0).getPlace());
        verifySolrField(pub1_2, "relation.isProjectOfPublication", List.of(pro1_1.getID().toString()));

        // after create pro 1.2 - test relation.* metadata of project 1.1
        List<MetadataValue> mdvs8 = itemService
            .getMetadata(pro1_1, "relation", "isPublicationOfProject", null, Item.ANY);
        Assert.assertEquals(1, mdvs8.size());
        assertEquals(pub1_2.getID().toString(), mdvs8.get(0).getValue());
        assertEquals(0, mdvs8.get(0).getPlace());
        verifySolrField(pro1_1, "relation.isPublicationOfProject", List.of(pub1_2.getID().toString()));

        // after create pro 1.2 - test relation.* metadata of project 1.2
        List<MetadataValue> mdvs9 = itemService
            .getMetadata(pro1_2, "relation", "isPublicationOfProject", null, Item.ANY);
        Assert.assertEquals(1, mdvs9.size());
        assertEquals(pub1_2.getID().toString(), mdvs9.get(0).getValue());
        assertEquals(0, mdvs9.get(0).getPlace());
        verifySolrField(pro1_2, "relation.isPublicationOfProject", List.of(pub1_2.getID().toString()));

        // after create pro 1.2 - search for related items of publication 1.1
        verifyRestSearchObjects(
            null, "project-relationships",
            (r) -> r.param("f.isPublicationOfProject", idPub1_1 + ",equals"),
            List.of()
        );

        // after create pro 1.2 - search for related items of publication 1.2 (ANONYMOUS)
        verifyRestSearchObjects(
            null, "project-relationships",
            (r) -> r.param("f.isPublicationOfProject", idPub1_2 + ",equals"),
            List.of(
                matchSearchResult(pro1_1, "project 1")
                // NOTE: project 1.2 is not yet visible for anonymous users
            )
        );

        // after create pro 1.2 - search for related items of publication 1.2 (ADMIN)
        verifyRestSearchObjects(
            getAuthToken(admin.getEmail(), password), "project-relationships",
            (r) -> r.param("f.isPublicationOfProject", idPub1_2 + ",equals"),
            List.of(
                matchSearchResult(pro1_1, "project 1"),
                // NOTE: project 1.2 is already visible for admin users
                matchSearchResult(pro1_2, "project 1")
            )
        );

        // after create pro 1.2 - search for related items of project 1.1
        verifyRestSearchObjects(
            null, "publication-relationships",
            (r) -> r.param("f.isProjectOfPublication", idPro1_1 + ",equals"),
            List.of(
                matchSearchResult(pub1_1, "publication 1"),
                matchSearchResult(pub1_2, "publication 1")
            )
        );

        // after create pro 1.2 - search for related items of project 1.2
        verifyRestSearchObjects(
            null, "publication-relationships",
            (r) -> r.param("f.isProjectOfPublication", idPro1_2 + ",equals"),
            List.of()
        );

        // archive project 1.2
        context.turnOffAuthorisationSystem();
        installItemService.installItem(context, workspaceItemService.findByItem(context, pro1_2));
        context.commit();
        indexingService.commit();
        context.restoreAuthSystemState();

        // cache busting
        pub1_1 = context.reloadEntity(pub1_1);
        pub1_2 = context.reloadEntity(pub1_2);
        pro1_1 = context.reloadEntity(pro1_1);
        pro1_2 = context.reloadEntity(pro1_2);

        // after archive pro 1.2 - test relationships of publication 1.1
        assertThat(
            relationshipService.findByItem(context, pub1_1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(pub1_1, isProjectOfPublication, pro1_1, RIGHT_ONLY, 0, 0)
            ))
        );

        // after archive pro 1.2 - test relationships of publication 1.2
        assertThat(
            relationshipService.findByItem(context, pub1_2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(pub1_2, isProjectOfPublication, pro1_1, LEFT_ONLY, 0, 0),
                isRel(pub1_2, isProjectOfPublication, pro1_2, BOTH, 0, 0)
            ))
        );

        // after archive pro 1.2 - test relationships of project 1.1
        assertThat(
            relationshipService.findByItem(context, pro1_1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(pub1_1, isProjectOfPublication, pro1_1, RIGHT_ONLY, 0, 0),
                isRel(pub1_2, isProjectOfPublication, pro1_1, LEFT_ONLY, 0, 0)
            ))
        );

        // after archive pro 1.2 - test relationships of project 1.2
        assertThat(
            relationshipService.findByItem(context, pro1_2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(pub1_2, isProjectOfPublication, pro1_2, BOTH, 0, 0)
            ))
        );

        // after archive pro 1.2 - test relation.* metadata of publication 1.1
        List<MetadataValue> mdvs10 = itemService
            .getMetadata(pub1_1, "relation", "isProjectOfPublication", null, Item.ANY);
        Assert.assertEquals(1, mdvs10.size());
        assertEquals(pro1_1.getID().toString(), mdvs10.get(0).getValue());
        assertEquals(0, mdvs10.get(0).getPlace());
        verifySolrField(pub1_1, "relation.isProjectOfPublication", List.of(pro1_1.getID().toString()));

        // after archive pro 1.2 - test relation.* metadata of publication 1.2
        List<MetadataValue> mdvs11 = itemService
            .getMetadata(pub1_2, "relation", "isProjectOfPublication", null, Item.ANY);
        Assert.assertEquals(1, mdvs11.size());
        assertEquals(pro1_2.getID().toString(), mdvs11.get(0).getValue());
        assertEquals(0, mdvs11.get(0).getPlace());
        verifySolrField(pub1_2, "relation.isProjectOfPublication", List.of(pro1_2.getID().toString()));

        // after archive pro 1.2 - test relation.* metadata of project 1.1
        List<MetadataValue> mdvs12 = itemService
            .getMetadata(pro1_1, "relation", "isPublicationOfProject", null, Item.ANY);
        Assert.assertEquals(1, mdvs12.size());
        assertEquals(pub1_2.getID().toString(), mdvs12.get(0).getValue());
        assertEquals(0, mdvs12.get(0).getPlace());
        verifySolrField(pro1_1, "relation.isPublicationOfProject", List.of(pub1_2.getID().toString()));

        // after archive pro 1.2 - test relation.* metadata of project 1.2
        List<MetadataValue> mdvs13 = itemService
            .getMetadata(pro1_2, "relation", "isPublicationOfProject", null, Item.ANY);
        Assert.assertEquals(1, mdvs13.size());
        assertEquals(pub1_2.getID().toString(), mdvs13.get(0).getValue());
        assertEquals(0, mdvs13.get(0).getPlace());
        verifySolrField(pro1_2, "relation.isPublicationOfProject", List.of(pub1_2.getID().toString()));

        // after archive pro 1.2 - search for related items of publication 1.1
        verifyRestSearchObjects(
            null, "project-relationships",
            (r) -> r.param("f.isPublicationOfProject", idPub1_1 + ",equals"),
            List.of()
        );

        // after archive pro 1.2 - search for related items of publication 1.2
        verifyRestSearchObjects(
            null, "project-relationships",
            (r) -> r.param("f.isPublicationOfProject", idPub1_2 + ",equals"),
            List.of(
                matchSearchResult(pro1_1, "project 1"),
                matchSearchResult(pro1_2, "project 1")
            )
        );

        // after archive pro 1.2 - search for related items of project 1.1
        verifyRestSearchObjects(
            null, "publication-relationships",
            (r) -> r.param("f.isProjectOfPublication", idPro1_1 + ",equals"),
            List.of(
                matchSearchResult(pub1_1, "publication 1")
            )
        );

        // after archive pro 1.2 - search for related items of project 1.2
        verifyRestSearchObjects(
            null, "publication-relationships",
            (r) -> r.param("f.isProjectOfPublication", idPro1_2 + ",equals"),
            List.of(
                matchSearchResult(pub1_2, "publication 1")
            )
        );
    }

}
