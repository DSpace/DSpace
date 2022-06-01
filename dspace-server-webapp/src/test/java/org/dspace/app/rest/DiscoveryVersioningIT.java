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
import static org.dspace.util.RelationshipVersioningTestUtils.isRel;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.builder.VersionBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.RelationshipType;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SolrSearchCore;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.versioning.Version;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
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

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        community = CommunityBuilder.createCommunity(context)
            .withName("community")
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

        if (expectedValues == null) {
            assertNull(actualValues);
        } else {
            assertThat(actualValues, containsInAnyOrder(expectedValues.toArray()));
        }
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

    protected Collection createCollection() {
        return createCollection("collection without entity type", null);
    }

    protected Collection createCollection(String entityType) {
        return createCollection("collection: " + entityType, entityType);
    }

    protected Collection createCollection(String name, String entityType) {
        context.turnOffAuthorisationSystem();

        CollectionBuilder builder = CollectionBuilder.createCollection(context, community);

        if (name != null) {
            builder.withName(name);
        }

        if (entityType != null) {
            builder.withEntityType(entityType);
        }

        Collection collection = builder.build();

        context.restoreAuthSystemState();

        return collection;
    }

    @Test
    public void test_discoveryXml_default_expectLatestVersionsOnly() throws Exception {
        final String configuration = null;

        Collection collection = createCollection();

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

        Collection collection = createCollection();

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

        Collection collection = createCollection();

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

        Collection collection = createCollection();

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

        Collection collection = createCollection();

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

        Collection collection = createCollection();

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

        Collection collection = createCollection("Publication");

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: entity type inherited from collection, necessary to get past the default filter query
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

        Collection collection = createCollection("Publication");

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: entity type inherited from collection, necessary to get past the default filter query
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

        Collection collection = createCollection("Person");

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: entity type inherited from collection, necessary to get past the default filter query
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

        Collection collection = createCollection("Person");

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: entity type inherited from collection, necessary to get past the default filter query
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

        Collection collection = createCollection("OrgUnit");

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: entity type inherited from collection, necessary to get past the default filter query
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

        Collection collection = createCollection("OrgUnit");

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: entity type inherited from collection, necessary to get past the default filter query
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

        Collection collection = createCollection("JournalIssue");

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: entity type inherited from collection, necessary to get past the default filter query
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

        Collection collection = createCollection("JournalIssue");

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: entity type inherited from collection, necessary to get past the default filter query
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

        Collection collection = createCollection("JournalVolume");

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: entity type inherited from collection, necessary to get past the default filter query
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


        Collection collection = createCollection("JournalVolume");

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: entity type inherited from collection, necessary to get past the default filter query
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


        Collection collection = createCollection("Journal");

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: entity type inherited from collection, necessary to get past the default filter query
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

        Collection collection = createCollection("Journal");

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: entity type inherited from collection, necessary to get past the default filter query
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

        Collection collection = createCollection("Project");

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: entity type inherited from collection, necessary to get past the default filter query
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

        Collection collection = createCollection("Project");

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: entity type inherited from collection, necessary to get past the default filter query
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

        Collection collection = createCollection("Person");

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: entity type inherited from collection, necessary to get past the default filter query
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


        Collection collection = createCollection("OrgUnit");

        // create item 1.1 (first version of item 1)
        context.turnOffAuthorisationSystem();
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("item 1.1")
            // NOTE: entity type inherited from collection, necessary to get past the default filter query
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

        Collection publicationCollection = createCollection(publicationEntityType.getLabel());

        // create publication 1.1
        Item pub1_1 = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("publication 1")
            .build();
        String idPub1_1 = pub1_1.getID().toString();

        Collection projectCollection = createCollection(projectEntityType.getLabel());

        // create project 1.1
        Item pro1_1 = ItemBuilder.createItem(context, projectCollection)
            .withTitle("project 1")
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

        // init - test relation.*.latestForDiscovery metadata of publication 1.1
        List<MetadataValue> mdvs1a = itemService
            .getMetadata(pub1_1, "relation", "isProjectOfPublication", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(1, mdvs1a.size());
        assertEquals(pro1_1.getID().toString(), mdvs1a.get(0).getValue());
        assertEquals(-1, mdvs1a.get(0).getPlace());

        // init - test relation.* metadata of project 1.1
        List<MetadataValue> mdvs2 = itemService
            .getMetadata(pro1_1, "relation", "isPublicationOfProject", null, Item.ANY);
        Assert.assertEquals(1, mdvs2.size());
        assertEquals(pub1_1.getID().toString(), mdvs2.get(0).getValue());
        assertEquals(0, mdvs2.get(0).getPlace());

        // init - test relation.*.latestForDiscovery metadata of project 1.1
        List<MetadataValue> mdvs2a = itemService
            .getMetadata(pro1_1, "relation", "isPublicationOfProject", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(1, mdvs2a.size());
        assertEquals(pub1_1.getID().toString(), mdvs2a.get(0).getValue());
        assertEquals(-1, mdvs2a.get(0).getPlace());

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
        verifySolrField(pub1_1, "archived", List.of("false"));
        verifySolrField(pub1_1, "latestVersion", List.of(false));

        // after archive pub 1.2 - test relation.*.latestForDiscovery metadata of publication 1.1
        List<MetadataValue> mdvs3a = itemService
            .getMetadata(pub1_1, "relation", "isProjectOfPublication", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(0, mdvs3a.size());
        verifySolrField(pub1_1, "relation.isProjectOfPublication.latestForDiscovery", null);

        // after archive pub 1.2 - test relation.* metadata of publication 1.2
        List<MetadataValue> mdvs4 = itemService
            .getMetadata(pub1_2, "relation", "isProjectOfPublication", null, Item.ANY);
        Assert.assertEquals(1, mdvs4.size());
        assertEquals(pro1_1.getID().toString(), mdvs4.get(0).getValue());
        assertEquals(0, mdvs4.get(0).getPlace());
        verifySolrField(pub1_2, "relation.isProjectOfPublication", List.of(pro1_1.getID().toString()));
        verifySolrField(pub1_2, "archived", List.of("true"));
        verifySolrField(pub1_2, "latestVersion", List.of(true));

        // after archive pub 1.2 - test relation.*.latestForDiscovery metadata of publication 1.2
        List<MetadataValue> mdvs4a = itemService
            .getMetadata(pub1_2, "relation", "isProjectOfPublication", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(1, mdvs4a.size());
        assertEquals(pro1_1.getID().toString(), mdvs4a.get(0).getValue());
        assertEquals(-1, mdvs4a.get(0).getPlace());
        verifySolrField(
            pub1_2, "relation.isProjectOfPublication.latestForDiscovery", List.of(pro1_1.getID().toString())
        );

        // after archive pub 1.2 - test relation.* metadata of project 1.1
        List<MetadataValue> mdvs5 = itemService
            .getMetadata(pro1_1, "relation", "isPublicationOfProject", null, Item.ANY);
        Assert.assertEquals(1, mdvs5.size());
        assertEquals(pub1_2.getID().toString(), mdvs5.get(0).getValue());
        assertEquals(0, mdvs5.get(0).getPlace());
        verifySolrField(pro1_1, "relation.isPublicationOfProject", List.of(pub1_2.getID().toString()));
        verifySolrField(pro1_1, "archived", List.of("true"));
        verifySolrField(pro1_1, "latestVersion", List.of(true));

        // after archive pub 1.2 - test relation.*.latestForDiscovery metadata of project 1.1
        List<MetadataValue> mdvs5a = itemService
            .getMetadata(pro1_1, "relation", "isPublicationOfProject", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(2, mdvs5a.size());
        assertThat(mdvs5a, containsInAnyOrder(
            allOf(
                hasProperty("value", is(pub1_1.getID().toString())),
                hasProperty("place", is(-1))
            ),
            allOf(
                hasProperty("value", is(pub1_2.getID().toString())),
                hasProperty("place", is(-1))
            )
        ));
        verifySolrField(pro1_1, "relation.isPublicationOfProject.latestForDiscovery", List.of(
            pub1_1.getID().toString(),
            pub1_2.getID().toString()
        ));

        // after archive pub 1.2 - search for related items of publication 1.1
        verifyRestSearchObjects(
            null, "project-relationships",
            (r) -> r.param("f.isPublicationOfProject", idPub1_1 + ",equals"),
            List.of(
                matchSearchResult(pro1_1, "project 1")
            )
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
        verifySolrField(pub1_1, "archived", List.of("false"));
        verifySolrField(pub1_1, "latestVersion", List.of(false));

        // after create pro 1.2 - test relation.*.latestForDiscovery metadata of publication 1.1
        List<MetadataValue> mdvs6a = itemService
            .getMetadata(pub1_1, "relation", "isProjectOfPublication", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(0, mdvs6a.size());
        verifySolrField(pub1_1, "relation.isProjectOfPublication.latestForDiscovery", null);

        // after create pro 1.2 - test relation.* metadata of publication 1.2
        List<MetadataValue> mdvs7 = itemService
            .getMetadata(pub1_2, "relation", "isProjectOfPublication", null, Item.ANY);
        Assert.assertEquals(1, mdvs7.size());
        assertEquals(pro1_1.getID().toString(), mdvs7.get(0).getValue());
        assertEquals(0, mdvs7.get(0).getPlace());
        verifySolrField(pub1_2, "relation.isProjectOfPublication", List.of(pro1_1.getID().toString()));
        verifySolrField(pub1_2, "archived", List.of("true"));
        verifySolrField(pub1_2, "latestVersion", List.of(true));

        // after create pro 1.2 - test relation.*.latestForDiscovery metadata of publication 1.2
        List<MetadataValue> mdvs7a = itemService
            .getMetadata(pub1_2, "relation", "isProjectOfPublication", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(2, mdvs7a.size());
        assertThat(mdvs7a, containsInAnyOrder(
            allOf(
                hasProperty("value", is(pro1_1.getID().toString())),
                hasProperty("place", is(-1))
            ),
            allOf(
                hasProperty("value", is(pro1_2.getID().toString())),
                hasProperty("place", is(-1))
            )
        ));
        verifySolrField(pub1_2, "relation.isProjectOfPublication.latestForDiscovery", List.of(
            pro1_1.getID().toString(),
            pro1_2.getID().toString()
        ));

        // after create pro 1.2 - test relation.* metadata of project 1.1
        List<MetadataValue> mdvs8 = itemService
            .getMetadata(pro1_1, "relation", "isPublicationOfProject", null, Item.ANY);
        Assert.assertEquals(1, mdvs8.size());
        assertEquals(pub1_2.getID().toString(), mdvs8.get(0).getValue());
        assertEquals(0, mdvs8.get(0).getPlace());
        verifySolrField(pro1_1, "relation.isPublicationOfProject", List.of(pub1_2.getID().toString()));
        verifySolrField(pro1_1, "archived", List.of("true"));
        verifySolrField(pro1_1, "latestVersion", List.of(true));

        // after create pro 1.2 - test relation.*.latestForDiscovery metadata of project 1.1
        List<MetadataValue> mdvs8a = itemService
            .getMetadata(pro1_1, "relation", "isPublicationOfProject", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(2, mdvs8a.size());
        assertThat(mdvs8a, containsInAnyOrder(
            allOf(
                hasProperty("value", is(pub1_1.getID().toString())),
                hasProperty("place", is(-1))
            ),
            allOf(
                hasProperty("value", is(pub1_2.getID().toString())),
                hasProperty("place", is(-1))
            )
        ));
        verifySolrField(pro1_1, "relation.isPublicationOfProject.latestForDiscovery", List.of(
            pub1_1.getID().toString(),
            pub1_2.getID().toString()
        ));

        // after create pro 1.2 - test relation.* metadata of project 1.2
        List<MetadataValue> mdvs9 = itemService
            .getMetadata(pro1_2, "relation", "isPublicationOfProject", null, Item.ANY);
        Assert.assertEquals(1, mdvs9.size());
        assertEquals(pub1_2.getID().toString(), mdvs9.get(0).getValue());
        assertEquals(0, mdvs9.get(0).getPlace());
        // NOTE: project 1.2 is still in the workspace,
        //       so it should not be indexed as an item (see ItemIndexFactory#getIndexableObjects)
        verifyNotIndexed(pro1_2);

        // after create pro 1.2 - test relation.*.latestForDiscovery metadata of project 1.2
        List<MetadataValue> mdvs9a = itemService
            .getMetadata(pro1_2, "relation", "isPublicationOfProject", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(0, mdvs9a.size());
        // NOTE: project 1.2 is still in the workspace,
        //       so it should not be indexed as an item (see ItemIndexFactory#getIndexableObjects)
        verifyNotIndexed(pro1_2);

        // after create pro 1.2 - search for related items of publication 1.1
        verifyRestSearchObjects(
            null, "project-relationships",
            (r) -> r.param("f.isPublicationOfProject", idPub1_1 + ",equals"),
            List.of(
                matchSearchResult(pro1_1, "project 1")
            )
        );

        // after create pro 1.2 - search for related items of publication 1.2
        verifyRestSearchObjects(
            null, "project-relationships",
            (r) -> r.param("f.isPublicationOfProject", idPub1_2 + ",equals"),
            List.of(
                matchSearchResult(pro1_1, "project 1")
                // NOTE: project 1.2 is still in the workspace
            )
        );

        // after create pro 1.2 - search for related items of project 1.1
        verifyRestSearchObjects(
            null, "publication-relationships",
            (r) -> r.param("f.isProjectOfPublication", idPro1_1 + ",equals"),
            List.of(
                matchSearchResult(pub1_2, "publication 1")
            )
        );

        // after create pro 1.2 - search for related items of project 1.2
        verifyRestSearchObjects(
            null, "publication-relationships",
            (r) -> r.param("f.isProjectOfPublication", idPro1_2 + ",equals"),
            List.of(
                matchSearchResult(pub1_2, "publication 1")
            )
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
        verifySolrField(pub1_1, "archived", List.of("false"));
        verifySolrField(pub1_1, "latestVersion", List.of(false));

        // after archive pro 1.2 - test relation.*.latestForDiscovery metadata of publication 1.1
        List<MetadataValue> mdvs10a = itemService
            .getMetadata(pub1_1, "relation", "isProjectOfPublication", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(0, mdvs10a.size());
        verifySolrField(pub1_1, "relation.isProjectOfPublication.latestForDiscovery", null);

        // after archive pro 1.2 - test relation.* metadata of publication 1.2
        List<MetadataValue> mdvs11 = itemService
            .getMetadata(pub1_2, "relation", "isProjectOfPublication", null, Item.ANY);
        Assert.assertEquals(1, mdvs11.size());
        assertEquals(pro1_2.getID().toString(), mdvs11.get(0).getValue());
        assertEquals(0, mdvs11.get(0).getPlace());
        verifySolrField(pub1_2, "relation.isProjectOfPublication", List.of(pro1_2.getID().toString()));
        verifySolrField(pub1_2, "archived", List.of("true"));
        verifySolrField(pub1_2, "latestVersion", List.of(true));

        // after archive pro 1.2 - test relation.*.latestForDiscovery metadata of publication 1.2
        List<MetadataValue> mdvs11a = itemService
            .getMetadata(pub1_2, "relation", "isProjectOfPublication", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(2, mdvs11a.size());
        assertThat(mdvs11a, containsInAnyOrder(
            allOf(
                hasProperty("value", is(pro1_1.getID().toString())),
                hasProperty("place", is(-1))
            ),
            allOf(
                hasProperty("value", is(pro1_2.getID().toString())),
                hasProperty("place", is(-1))
            )
        ));
        verifySolrField(pub1_2, "relation.isProjectOfPublication.latestForDiscovery", List.of(
            pro1_1.getID().toString(),
            pro1_2.getID().toString()
        ));

        // after archive pro 1.2 - test relation.* metadata of project 1.1
        List<MetadataValue> mdvs12 = itemService
            .getMetadata(pro1_1, "relation", "isPublicationOfProject", null, Item.ANY);
        Assert.assertEquals(1, mdvs12.size());
        assertEquals(pub1_2.getID().toString(), mdvs12.get(0).getValue());
        assertEquals(0, mdvs12.get(0).getPlace());
        verifySolrField(pro1_1, "relation.isPublicationOfProject", List.of(pub1_2.getID().toString()));
        verifySolrField(pro1_1, "archived", List.of("false"));
        verifySolrField(pro1_1, "latestVersion", List.of(false));

        // after archive pro 1.2 - test relation.*.latestForDiscovery metadata of project 1.1
        List<MetadataValue> mdvs12a = itemService
            .getMetadata(pro1_1, "relation", "isPublicationOfProject", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(1, mdvs12a.size());
        assertEquals(pub1_1.getID().toString(), mdvs12a.get(0).getValue());
        assertEquals(-1, mdvs12a.get(0).getPlace());
        verifySolrField(
            pro1_1, "relation.isPublicationOfProject.latestForDiscovery", List.of(pub1_1.getID().toString())
        );

        // after archive pro 1.2 - test relation.* metadata of project 1.2
        List<MetadataValue> mdvs13 = itemService
            .getMetadata(pro1_2, "relation", "isPublicationOfProject", null, Item.ANY);
        Assert.assertEquals(1, mdvs13.size());
        assertEquals(pub1_2.getID().toString(), mdvs13.get(0).getValue());
        assertEquals(0, mdvs13.get(0).getPlace());
        verifySolrField(pro1_2, "relation.isPublicationOfProject", List.of(pub1_2.getID().toString()));
        verifySolrField(pro1_2, "archived", List.of("true"));
        verifySolrField(pro1_2, "latestVersion", List.of(true));

        // after archive pro 1.2 - test relation.*.latestForDiscovery metadata of project 1.2
        List<MetadataValue> mdvs13a = itemService
            .getMetadata(pro1_2, "relation", "isPublicationOfProject", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(1, mdvs13a.size());
        assertEquals(pub1_2.getID().toString(), mdvs13a.get(0).getValue());
        assertEquals(-1, mdvs13a.get(0).getPlace());
        verifySolrField(
            pro1_2, "relation.isPublicationOfProject.latestForDiscovery", List.of(pub1_2.getID().toString())
        );

        // after archive pro 1.2 - search for related items of publication 1.1
        verifyRestSearchObjects(
            null, "project-relationships",
            (r) -> r.param("f.isPublicationOfProject", idPub1_1 + ",equals"),
            List.of(
                matchSearchResult(pro1_1, "project 1")
            )
        );

        // after archive pro 1.2 - search for related items of publication 1.2
        verifyRestSearchObjects(
            null, "project-relationships",
            (r) -> r.param("f.isPublicationOfProject", idPub1_2 + ",equals"),
            List.of(
                matchSearchResult(pro1_2, "project 1")
            )
        );

        // after archive pro 1.2 - search for related items of project 1.1
        verifyRestSearchObjects(
            null, "publication-relationships",
            (r) -> r.param("f.isProjectOfPublication", idPro1_1 + ",equals"),
            List.of(
                matchSearchResult(pub1_2, "publication 1")
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

    @Test
    public void test_forceReindexAfterNewVersionInWorkspace() throws Exception {
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

        Collection publicationCollection = createCollection(publicationEntityType.getLabel());

        // create publication 1.1
        Item pub1_1 = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("publication 1")
            .build();
        String idPub1_1 = pub1_1.getID().toString();

        Collection projectCollection = createCollection(projectEntityType.getLabel());

        // create project 1.1
        Item pro1_1 = ItemBuilder.createItem(context, projectCollection)
            .withTitle("project 1")
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

        // init - test relation.*.latestForDiscovery metadata of publication 1.1
        List<MetadataValue> mdvs1a = itemService
            .getMetadata(pub1_1, "relation", "isProjectOfPublication", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(1, mdvs1a.size());
        assertEquals(pro1_1.getID().toString(), mdvs1a.get(0).getValue());
        assertEquals(-1, mdvs1a.get(0).getPlace());

        // init - test relation.* metadata of project 1.1
        List<MetadataValue> mdvs2 = itemService
            .getMetadata(pro1_1, "relation", "isPublicationOfProject", null, Item.ANY);
        Assert.assertEquals(1, mdvs2.size());
        assertEquals(pub1_1.getID().toString(), mdvs2.get(0).getValue());
        assertEquals(0, mdvs2.get(0).getPlace());

        // init - test relation.*.latestForDiscovery metadata of project 1.1
        List<MetadataValue> mdvs2a = itemService
            .getMetadata(pro1_1, "relation", "isPublicationOfProject", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(1, mdvs2a.size());
        assertEquals(pub1_1.getID().toString(), mdvs2a.get(0).getValue());
        assertEquals(-1, mdvs2a.get(0).getPlace());

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
        verifySolrField(pub1_1, "archived", List.of("false"));
        verifySolrField(pub1_1, "latestVersion", List.of(false));

        // after archive pub 1.2 - test relation.*.latestForDiscovery metadata of publication 1.1
        List<MetadataValue> mdvs3a = itemService
            .getMetadata(pub1_1, "relation", "isProjectOfPublication", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(0, mdvs3a.size());
        verifySolrField(pub1_1, "relation.isProjectOfPublication.latestForDiscovery", null);

        // after archive pub 1.2 - test relation.* metadata of publication 1.2
        List<MetadataValue> mdvs4 = itemService
            .getMetadata(pub1_2, "relation", "isProjectOfPublication", null, Item.ANY);
        Assert.assertEquals(1, mdvs4.size());
        assertEquals(pro1_1.getID().toString(), mdvs4.get(0).getValue());
        assertEquals(0, mdvs4.get(0).getPlace());
        verifySolrField(pub1_2, "relation.isProjectOfPublication", List.of(pro1_1.getID().toString()));
        verifySolrField(pub1_2, "archived", List.of("true"));
        verifySolrField(pub1_2, "latestVersion", List.of(true));

        // after archive pub 1.2 - test relation.*.latestForDiscovery metadata of publication 1.2
        List<MetadataValue> mdvs4a = itemService
            .getMetadata(pub1_2, "relation", "isProjectOfPublication", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(1, mdvs4a.size());
        assertEquals(pro1_1.getID().toString(), mdvs4a.get(0).getValue());
        assertEquals(-1, mdvs4a.get(0).getPlace());
        verifySolrField(
            pub1_2, "relation.isProjectOfPublication.latestForDiscovery", List.of(pro1_1.getID().toString())
        );

        // after archive pub 1.2 - test relation.* metadata of project 1.1
        List<MetadataValue> mdvs5 = itemService
            .getMetadata(pro1_1, "relation", "isPublicationOfProject", null, Item.ANY);
        Assert.assertEquals(1, mdvs5.size());
        assertEquals(pub1_2.getID().toString(), mdvs5.get(0).getValue());
        assertEquals(0, mdvs5.get(0).getPlace());
        verifySolrField(pro1_1, "relation.isPublicationOfProject", List.of(pub1_2.getID().toString()));
        verifySolrField(pro1_1, "archived", List.of("true"));
        verifySolrField(pro1_1, "latestVersion", List.of(true));

        // after archive pub 1.2 - test relation.*.latestForDiscovery metadata of project 1.1
        List<MetadataValue> mdvs5a = itemService
            .getMetadata(pro1_1, "relation", "isPublicationOfProject", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(2, mdvs5a.size());
        assertThat(mdvs5a, containsInAnyOrder(
            allOf(
                hasProperty("value", is(pub1_1.getID().toString())),
                hasProperty("place", is(-1))
            ),
            allOf(
                hasProperty("value", is(pub1_2.getID().toString())),
                hasProperty("place", is(-1))
            )
        ));
        verifySolrField(pro1_1, "relation.isPublicationOfProject.latestForDiscovery", List.of(
            pub1_1.getID().toString(),
            pub1_2.getID().toString()
        ));

        // after archive pub 1.2 - search for related items of publication 1.1
        verifyRestSearchObjects(
            null, "project-relationships",
            (r) -> r.param("f.isPublicationOfProject", idPub1_1 + ",equals"),
            List.of(
                matchSearchResult(pro1_1, "project 1")
            )
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

        // force reindex => expect that project 1.1 is still marked as latest version (for now)
        indexingService.indexContent(context, new IndexableItem(pub1_1), true);
        indexingService.indexContent(context, new IndexableItem(pub1_2), true);
        indexingService.indexContent(context, new IndexableItem(pro1_1), true);
        // NOTE: project 1.2 shouldn't be indexed yet
        indexingService.commit();

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
        verifySolrField(pub1_1, "archived", List.of("false"));
        verifySolrField(pub1_1, "latestVersion", List.of(false));

        // after create pro 1.2 - test relation.*.latestForDiscovery metadata of publication 1.1
        List<MetadataValue> mdvs6a = itemService
            .getMetadata(pub1_1, "relation", "isProjectOfPublication", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(0, mdvs6a.size());
        verifySolrField(pub1_1, "relation.isProjectOfPublication.latestForDiscovery", null);

        // after create pro 1.2 - test relation.* metadata of publication 1.2
        List<MetadataValue> mdvs7 = itemService
            .getMetadata(pub1_2, "relation", "isProjectOfPublication", null, Item.ANY);
        Assert.assertEquals(1, mdvs7.size());
        assertEquals(pro1_1.getID().toString(), mdvs7.get(0).getValue());
        assertEquals(0, mdvs7.get(0).getPlace());
        verifySolrField(pub1_2, "relation.isProjectOfPublication", List.of(pro1_1.getID().toString()));
        verifySolrField(pub1_2, "archived", List.of("true"));
        verifySolrField(pub1_2, "latestVersion", List.of(true));

        // after create pro 1.2 - test relation.*.latestForDiscovery metadata of publication 1.2
        List<MetadataValue> mdvs7a = itemService
            .getMetadata(pub1_2, "relation", "isProjectOfPublication", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(2, mdvs7a.size());
        assertThat(mdvs7a, containsInAnyOrder(
            allOf(
                hasProperty("value", is(pro1_1.getID().toString())),
                hasProperty("place", is(-1))
            ),
            allOf(
                hasProperty("value", is(pro1_2.getID().toString())),
                hasProperty("place", is(-1))
            )
        ));
        verifySolrField(pub1_2, "relation.isProjectOfPublication.latestForDiscovery", List.of(
            pro1_1.getID().toString(),
            pro1_2.getID().toString()
        ));

        // after create pro 1.2 - test relation.* metadata of project 1.1
        List<MetadataValue> mdvs8 = itemService
            .getMetadata(pro1_1, "relation", "isPublicationOfProject", null, Item.ANY);
        Assert.assertEquals(1, mdvs8.size());
        assertEquals(pub1_2.getID().toString(), mdvs8.get(0).getValue());
        assertEquals(0, mdvs8.get(0).getPlace());
        verifySolrField(pro1_1, "relation.isPublicationOfProject", List.of(pub1_2.getID().toString()));
        verifySolrField(pro1_1, "archived", List.of("true"));
        verifySolrField(pro1_1, "latestVersion", List.of(true));

        // after create pro 1.2 - test relation.*.latestForDiscovery metadata of project 1.1
        List<MetadataValue> mdvs8a = itemService
            .getMetadata(pro1_1, "relation", "isPublicationOfProject", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(2, mdvs8a.size());
        assertThat(mdvs8a, containsInAnyOrder(
            allOf(
                hasProperty("value", is(pub1_1.getID().toString())),
                hasProperty("place", is(-1))
            ),
            allOf(
                hasProperty("value", is(pub1_2.getID().toString())),
                hasProperty("place", is(-1))
            )
        ));
        verifySolrField(pro1_1, "relation.isPublicationOfProject.latestForDiscovery", List.of(
            pub1_1.getID().toString(),
            pub1_2.getID().toString()
        ));

        // after create pro 1.2 - test relation.* metadata of project 1.2
        List<MetadataValue> mdvs9 = itemService
            .getMetadata(pro1_2, "relation", "isPublicationOfProject", null, Item.ANY);
        Assert.assertEquals(1, mdvs9.size());
        assertEquals(pub1_2.getID().toString(), mdvs9.get(0).getValue());
        assertEquals(0, mdvs9.get(0).getPlace());
        // NOTE: project 1.2 is still in the workspace,
        //       so it should not be indexed as an item (see ItemIndexFactory#getIndexableObjects)
        verifyNotIndexed(pro1_2);

        // after create pro 1.2 - test relation.*.latestForDiscovery metadata of project 1.2
        List<MetadataValue> mdvs9a = itemService
            .getMetadata(pro1_2, "relation", "isPublicationOfProject", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(0, mdvs9a.size());
        // NOTE: project 1.2 is still in the workspace,
        //       so it should not be indexed as an item (see ItemIndexFactory#getIndexableObjects)
        verifyNotIndexed(pro1_2);

        // after create pro 1.2 - search for related items of publication 1.1
        verifyRestSearchObjects(
            null, "project-relationships",
            (r) -> r.param("f.isPublicationOfProject", idPub1_1 + ",equals"),
            List.of(
                matchSearchResult(pro1_1, "project 1")
            )
        );

        // after create pro 1.2 - search for related items of publication 1.2
        verifyRestSearchObjects(
            null, "project-relationships",
            (r) -> r.param("f.isPublicationOfProject", idPub1_2 + ",equals"),
            List.of(
                matchSearchResult(pro1_1, "project 1")
                // NOTE: project 1.2 is still in the workspace
            )
        );

        // after create pro 1.2 - search for related items of project 1.1
        verifyRestSearchObjects(
            null, "publication-relationships",
            (r) -> r.param("f.isProjectOfPublication", idPro1_1 + ",equals"),
            List.of(
                matchSearchResult(pub1_2, "publication 1")
            )
        );

        // after create pro 1.2 - search for related items of project 1.2
        verifyRestSearchObjects(
            null, "publication-relationships",
            (r) -> r.param("f.isProjectOfPublication", idPro1_2 + ",equals"),
            List.of(
                matchSearchResult(pub1_2, "publication 1")
            )
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
        verifySolrField(pub1_1, "archived", List.of("false"));
        verifySolrField(pub1_1, "latestVersion", List.of(false));

        // after archive pro 1.2 - test relation.*.latestForDiscovery metadata of publication 1.1
        List<MetadataValue> mdvs10a = itemService
            .getMetadata(pub1_1, "relation", "isProjectOfPublication", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(0, mdvs10a.size());
        verifySolrField(pub1_1, "relation.isProjectOfPublication.latestForDiscovery", null);

        // after archive pro 1.2 - test relation.* metadata of publication 1.2
        List<MetadataValue> mdvs11 = itemService
            .getMetadata(pub1_2, "relation", "isProjectOfPublication", null, Item.ANY);
        Assert.assertEquals(1, mdvs11.size());
        assertEquals(pro1_2.getID().toString(), mdvs11.get(0).getValue());
        assertEquals(0, mdvs11.get(0).getPlace());
        verifySolrField(pub1_2, "relation.isProjectOfPublication", List.of(pro1_2.getID().toString()));
        verifySolrField(pub1_2, "archived", List.of("true"));
        verifySolrField(pub1_2, "latestVersion", List.of(true));

        // after archive pro 1.2 - test relation.*.latestForDiscovery metadata of publication 1.2
        List<MetadataValue> mdvs11a = itemService
            .getMetadata(pub1_2, "relation", "isProjectOfPublication", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(2, mdvs11a.size());
        assertThat(mdvs11a, containsInAnyOrder(
            allOf(
                hasProperty("value", is(pro1_1.getID().toString())),
                hasProperty("place", is(-1))
            ),
            allOf(
                hasProperty("value", is(pro1_2.getID().toString())),
                hasProperty("place", is(-1))
            )
        ));
        verifySolrField(pub1_2, "relation.isProjectOfPublication.latestForDiscovery", List.of(
            pro1_1.getID().toString(),
            pro1_2.getID().toString()
        ));

        // after archive pro 1.2 - test relation.* metadata of project 1.1
        List<MetadataValue> mdvs12 = itemService
            .getMetadata(pro1_1, "relation", "isPublicationOfProject", null, Item.ANY);
        Assert.assertEquals(1, mdvs12.size());
        assertEquals(pub1_2.getID().toString(), mdvs12.get(0).getValue());
        assertEquals(0, mdvs12.get(0).getPlace());
        verifySolrField(pro1_1, "relation.isPublicationOfProject", List.of(pub1_2.getID().toString()));
        verifySolrField(pro1_1, "archived", List.of("false"));
        verifySolrField(pro1_1, "latestVersion", List.of(false));

        // after archive pro 1.2 - test relation.*.latestForDiscovery metadata of project 1.1
        List<MetadataValue> mdvs12a = itemService
            .getMetadata(pro1_1, "relation", "isPublicationOfProject", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(1, mdvs12a.size());
        assertEquals(pub1_1.getID().toString(), mdvs12a.get(0).getValue());
        assertEquals(-1, mdvs12a.get(0).getPlace());
        verifySolrField(
            pro1_1, "relation.isPublicationOfProject.latestForDiscovery", List.of(pub1_1.getID().toString())
        );

        // after archive pro 1.2 - test relation.* metadata of project 1.2
        List<MetadataValue> mdvs13 = itemService
            .getMetadata(pro1_2, "relation", "isPublicationOfProject", null, Item.ANY);
        Assert.assertEquals(1, mdvs13.size());
        assertEquals(pub1_2.getID().toString(), mdvs13.get(0).getValue());
        assertEquals(0, mdvs13.get(0).getPlace());
        verifySolrField(pro1_2, "relation.isPublicationOfProject", List.of(pub1_2.getID().toString()));
        verifySolrField(pro1_2, "archived", List.of("true"));
        verifySolrField(pro1_2, "latestVersion", List.of(true));

        // after archive pro 1.2 - test relation.*.latestForDiscovery metadata of project 1.2
        List<MetadataValue> mdvs13a = itemService
            .getMetadata(pro1_2, "relation", "isPublicationOfProject", "latestForDiscovery", Item.ANY);
        Assert.assertEquals(1, mdvs13a.size());
        assertEquals(pub1_2.getID().toString(), mdvs13a.get(0).getValue());
        assertEquals(-1, mdvs13a.get(0).getPlace());
        verifySolrField(
            pro1_2, "relation.isPublicationOfProject.latestForDiscovery", List.of(pub1_2.getID().toString())
        );

        // after archive pro 1.2 - search for related items of publication 1.1
        verifyRestSearchObjects(
            null, "project-relationships",
            (r) -> r.param("f.isPublicationOfProject", idPub1_1 + ",equals"),
            List.of(
                matchSearchResult(pro1_1, "project 1")
            )
        );

        // after archive pro 1.2 - search for related items of publication 1.2
        verifyRestSearchObjects(
            null, "project-relationships",
            (r) -> r.param("f.isPublicationOfProject", idPub1_2 + ",equals"),
            List.of(
                matchSearchResult(pro1_2, "project 1")
            )
        );

        // after archive pro 1.2 - search for related items of project 1.1
        verifyRestSearchObjects(
            null, "publication-relationships",
            (r) -> r.param("f.isProjectOfPublication", idPro1_1 + ",equals"),
            List.of(
                matchSearchResult(pub1_2, "publication 1")
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

    @Test
    public void test_rebuildIndexAllVersionsShouldStillBePresentInSolrCore() throws Exception {
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

        Collection publicationCollection = createCollection(publicationEntityType.getLabel());

        Collection projectCollection = createCollection(projectEntityType.getLabel());

        // create publication 1.1
        Item pub1_1 = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("pub 1.1")
            // NOTE: entity type is inherited from collection
            .build();

        // create project 1.1
        Item pro1_1 = ItemBuilder.createItem(context, projectCollection)
            .withTitle("pro 1.1")
            // NOTE: entity type is inherited from collection
            .build();

        // create relationship between publication 1.1 and project 1.1
        RelationshipBuilder.createRelationshipBuilder(context, pub1_1, pro1_1, isProjectOfPublication)
            .build();

        context.restoreAuthSystemState();

        // create new version of publication 1.1 => publication 1.2
        Item pub1_2 = createNewVersion(context.reloadEntity(pub1_1), "pub 1.2");

        // create new version of publication 1.2 => publication 1.3
        Item pub1_3 = createNewVersion(context.reloadEntity(pub1_2), "pub 1.3");

        // create new version of publication 1.3 => publication 1.4
        Item pub1_4 = createNewVersion(context.reloadEntity(pub1_3), "pub 1.4");

        // cache busting
        pub1_1 = context.reloadEntity(pub1_1);
        pub1_2 = context.reloadEntity(pub1_2);
        pub1_3 = context.reloadEntity(pub1_3);
        pub1_4 = context.reloadEntity(pub1_4);
        pro1_1 = context.reloadEntity(pro1_1);

        // before reindex - verify publication 1.1
        verifyIndexed(pub1_1);
        verifySolrField(pub1_1, "archived", List.of("false"));
        verifySolrField(pub1_1, "latestVersion", List.of(false));
        verifySolrField(pub1_1, "relation.isProjectOfPublication", List.of(pro1_1.getID().toString()));
        verifySolrField(pub1_1, "relation.isProjectOfPublication.latestForDiscovery", null);

        // before reindex - verify publication 1.2
        verifyIndexed(pub1_2);
        verifySolrField(pub1_2, "archived", List.of("false"));
        verifySolrField(pub1_2, "latestVersion", List.of(false));
        verifySolrField(pub1_2, "relation.isProjectOfPublication", List.of(pro1_1.getID().toString()));
        verifySolrField(pub1_2, "relation.isProjectOfPublication.latestForDiscovery", null);

        // before reindex - verify publication 1.3
        verifyIndexed(pub1_3);
        verifySolrField(pub1_3, "archived", List.of("false"));
        verifySolrField(pub1_3, "latestVersion", List.of(false));
        verifySolrField(pub1_3, "relation.isProjectOfPublication", List.of(pro1_1.getID().toString()));
        verifySolrField(pub1_3, "relation.isProjectOfPublication.latestForDiscovery", null);

        // before reindex - verify publication 1.4
        verifyIndexed(pub1_4);
        verifySolrField(pub1_4, "archived", List.of("true"));
        verifySolrField(pub1_4, "latestVersion", List.of(true));
        verifySolrField(pub1_4, "relation.isProjectOfPublication", List.of(pro1_1.getID().toString()));
        verifySolrField(pub1_4, "relation.isProjectOfPublication.latestForDiscovery", List.of(
            pro1_1.getID().toString()
        ));

        // before reindex - verify project 1.1
        verifyIndexed(pro1_1);
        verifySolrField(pro1_1, "archived", List.of("true"));
        verifySolrField(pro1_1, "latestVersion", List.of(true));
        verifySolrField(pro1_1, "relation.isPublicationOfProject", List.of(pub1_4.getID().toString()));
        verifySolrField(pro1_1, "relation.isPublicationOfProject.latestForDiscovery", List.of(
            pub1_1.getID().toString(), pub1_2.getID().toString(), pub1_3.getID().toString(), pub1_4.getID().toString()
        ));

        // force reindex all items
        indexingService.deleteIndex();
        indexingService.createIndex(context);

        // after reindex - verify publication 1.1
        verifyIndexed(pub1_1);
        verifySolrField(pub1_1, "archived", List.of("false"));
        verifySolrField(pub1_1, "latestVersion", List.of(false));
        verifySolrField(pub1_1, "relation.isProjectOfPublication", List.of(pro1_1.getID().toString()));
        verifySolrField(pub1_1, "relation.isProjectOfPublication.latestForDiscovery", null);

        // after reindex - verify publication 1.2
        verifyIndexed(pub1_2);
        verifySolrField(pub1_2, "archived", List.of("false"));
        verifySolrField(pub1_2, "latestVersion", List.of(false));
        verifySolrField(pub1_2, "relation.isProjectOfPublication", List.of(pro1_1.getID().toString()));
        verifySolrField(pub1_2, "relation.isProjectOfPublication.latestForDiscovery", null);

        // after reindex - verify publication 1.3
        verifyIndexed(pub1_3);
        verifySolrField(pub1_3, "archived", List.of("false"));
        verifySolrField(pub1_3, "latestVersion", List.of(false));
        verifySolrField(pub1_3, "relation.isProjectOfPublication", List.of(pro1_1.getID().toString()));
        verifySolrField(pub1_3, "relation.isProjectOfPublication.latestForDiscovery", null);

        // after reindex - verify publication 1.4
        verifyIndexed(pub1_4);
        verifySolrField(pub1_4, "archived", List.of("true"));
        verifySolrField(pub1_4, "latestVersion", List.of(true));
        verifySolrField(pub1_4, "relation.isProjectOfPublication", List.of(pro1_1.getID().toString()));
        verifySolrField(pub1_4, "relation.isProjectOfPublication.latestForDiscovery", List.of(
            pro1_1.getID().toString()
        ));

        // after reindex - verify project 1.1
        verifyIndexed(pro1_1);
        verifySolrField(pro1_1, "archived", List.of("true"));
        verifySolrField(pro1_1, "latestVersion", List.of(true));
        verifySolrField(pro1_1, "relation.isPublicationOfProject", List.of(pub1_4.getID().toString()));
        verifySolrField(pro1_1, "relation.isPublicationOfProject.latestForDiscovery", List.of(
            pub1_1.getID().toString(), pub1_2.getID().toString(), pub1_3.getID().toString(), pub1_4.getID().toString()
        ));
    }

    // NOTE: this test makes sure that the changes to ItemIndexFactoryImpl don't break old behavior
    @Test
    public void test_rebuildIndex_itemInWorkspaceNotIndexed() throws Exception {
        Collection collection = createCollection();

        context.turnOffAuthorisationSystem();
        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle("item 1")
            .build();
        context.restoreAuthSystemState();

        verifyNotIndexed(wsi.getItem());

        // force reindex all items
        indexingService.deleteIndex();
        indexingService.createIndex(context);

        verifyNotIndexed(wsi.getItem());
    }

    // NOTE: this test makes sure that the changes to ItemIndexFactoryImpl don't break old behavior
    @Test
    public void test_rebuildIndex_itemInWorkflowNotIndexed() throws Exception {
        context.turnOffAuthorisationSystem();

        // NOTE: this collection uses the "selectSingleReviewer" workflow, see workflow.xml
        Collection collection = CollectionBuilder.createCollection(context, community, "123456789/workflow-test-1")
            .withName("collection")
            .build();

        // NOTE: the "selectSingleReviewer" requires the "ReviewManagers" group to be present and
        //       contain at least one member
        GroupBuilder.createGroup(context)
            .withName("ReviewManagers")
            .addMember(admin)
            .build();

        XmlWorkflowItem wfi = WorkflowItemBuilder.createWorkflowItem(context, collection)
            .withTitle("item 1")
            .build();

        context.restoreAuthSystemState();

        verifyNotIndexed(wfi.getItem());

        // force reindex all items
        indexingService.deleteIndex();
        indexingService.createIndex(context);

        verifyNotIndexed(wfi.getItem());
    }

    // NOTE: this test makes sure that the changes to ItemIndexFactoryImpl don't break old behavior
    @Test
    public void test_rebuildIndex_templateItemNotIndexed() throws Exception {
        Collection collection = createCollection();

        context.turnOffAuthorisationSystem();
        Item templateItem = itemService.createTemplateItem(context, collection);
        context.restoreAuthSystemState();

        verifyNotIndexed(templateItem);

        // force reindex all items
        indexingService.deleteIndex();
        indexingService.createIndex(context);

        verifyNotIndexed(templateItem);
    }

}
