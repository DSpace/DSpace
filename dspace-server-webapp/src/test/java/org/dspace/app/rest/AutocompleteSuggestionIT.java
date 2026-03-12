/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.matcher.SubmissionFormFieldMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.util.service.MetadataExposureService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SolrSearchCore;
import org.dspace.discovery.SolrSuggestService;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests for suggestion dictionaries
 * Note: Dictionaries are "snapshots" and won't apply incremenetal updates
 * if data is deleted / removed from the main search document of a given object.
 * Build must be run by an administrator with/without other reindex commands or a GET
 * to /discover/suggest/build
 * In this test, we call rebuildAllDictionaries directly to simulate that behaviour
 * so we can at least test that the subsequent dictionary will not contain values
 * we expect to be missing, after a change to data or configuration.
 */
public class AutocompleteSuggestionIT extends AbstractControllerIntegrationTest {
    // Items with subject metadata, commit, test REST method for expected results
    // Test test submission for expected vocabulary type
    @Autowired
    private SolrSearchCore solrSearchCore;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private IndexingService indexingService;
    @Autowired
    private SolrSuggestService solrSuggestService;
    @Autowired
    private MetadataExposureService metadataExposureService;

    Logger log = LogManager.getLogger(this.getClass());

    Group adminGroup;
    String handle = "autocomplete/test";

    /**
     * Reset configuration back to its pre-test state
     */
    private void resetConfiguration() {
        configurationService.reloadConfig();
    }

    /*
     * Reindex item
     * Because of the way the mock solr search core works
     * we have to commit "extra hard" here...
     */
    private void reindexItem(Item item) throws Exception {
        indexingService.updateIndex(context, true);
        indexingService.commit();
        solrSearchCore.getSolr().commit(true, true);
    }

    @BeforeClass
    public static void beforeAll() throws Exception {

    }

    @Before
    public void beforeEach() throws Exception {
        configurationService.setProperty("discovery.suggest.field",
                new String[]{"dc.subject", "dc.contributor.author"});
        configurationService.setProperty("discovery.suggest.allowed-dictionaries",
                new String[]{"subject", "authors", "countries_file"});
        adminGroup = groupService.findByName(context, Group.ADMIN);
        solrSuggestService.rebuildAllDictionaries();
    }

    @After
    public void afterEach() {
        resetConfiguration();
        metadataExposureService.reload();
    }

    @Test
    public void findFieldWithValuePairsConfig() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        String submissionFormName = "vocabulary-suggest-test";

        getClient(token).perform(get("/api/config/submissionforms/" + submissionFormName))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.id", is(submissionFormName)))
                .andExpect(jsonPath("$.name", is(submissionFormName)))
                .andExpect(jsonPath("$.type", is("submissionform")))
                .andExpect(jsonPath("$._links.self.href", Matchers
                        .startsWith(REST_SERVER_URL + "config/submissionforms/" + submissionFormName)))
                .andExpect(jsonPath("$.rows[0].fields", contains(
                        SubmissionFormFieldMatcher.matchFormFieldDefinition("onebox", "Subject", null,
                                null, true,
                                "Enter appropriate subject keywords or phrases.",
                                null, "dc.subject", "subject", "suggest")
                )))
        ;
    }

    @Test
    public void findTermSuggestionsFromMetadata() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item item1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .withAuthor("Doe, John")
                .withSubject("Totora Grove")
                .withSubject("Punga Grove")
                .withSubject("Kowhai Flower")
                .withSubject("Flower Arranging")
                .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Expect two terms for "grove"
        getClient(token).perform(get("/api/discover/suggest")
                        .queryParam("dict", "subject")
                        .queryParam("q", "grove")
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.suggest.subject.grove.numFound", is(2)))
                .andExpect(jsonPath("$.suggest.subject.grove.suggestions[*].term",
                        containsInAnyOrder("Punga <b>Grove</b>", "Totora <b>Grove</b>"
                        )));

        // Expect zero terms for "test"
        getClient(token).perform(get("/api/discover/suggest")
                        .queryParam("dict", "subject")
                        .queryParam("q", "test")
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.suggest.subject.test.numFound", is(0)));

        context.ignoreAuthorization();
        solrSuggestService.rebuildAllDictionaries();
        context.restoreAuthSystemState();
    }

    @Test
    public void findTermSuggestionsFromFlatFile() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/discover/suggest")
                        .queryParam("dict", "countries_file")
                        .queryParam("q", "madagascar")
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.suggest.countries_file.madagascar.numFound", is(1)));
    }

    @Test
    public void dontFindHiddenMetadatTermSuggestions() throws Exception {
        context.turnOffAuthorisationSystem();
        configurationService.addPropertyValue("metadata.hide.dc.subject", true);
        metadataExposureService.reload();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item item1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .withAuthor("Doe, John")
                .withSubject("Totora Grove")
                .withSubject("Punga Grove")
                .withSubject("Kowhai Flower")
                .withSubject("Flower Arranging")
                .build();
        reindexItem(item1);
        solrSuggestService.rebuildAllDictionaries();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Expect ZERO terms for "grove" - this field is hidden
        getClient(token).perform(get("/api/discover/suggest")
                        .queryParam("dict", "subject")
                        .queryParam("q", "grove")
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.suggest.subject.grove.numFound", is(0)));

        resetConfiguration();
    }

    @Test
    public void testSuggestTermsFromRestrictedItem() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item restrictedItem = ItemBuilder.createItem(context, col1)
                .withTitle("Restricted item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .withAuthor("Doe, John")
                .withSubject("Totara Grove")
                .withSubject("Punga Grove")
                .withReaderGroup(adminGroup)
                .build();

        context.restoreAuthSystemState();

        reindexItem(restrictedItem);
        solrSuggestService.rebuildAllDictionaries();

        String token = getAuthToken(eperson.getEmail(), password);

        // Expect ZERO terms - this item is not publicly readable
        getClient(token).perform(get("/api/discover/suggest")
                        .queryParam("dict", "subject")
                        .queryParam("q", "grove")
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.suggest.subject.grove.numFound", is(0)));
    }

    @Test
    public void testSuggestTermsFromWithdrawnItem() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item item1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .withAuthor("Doe, John")
                .withSubject("Totora Grove")
                .withSubject("Kowhai Grove")
                .withSubject("Punga Grove")
                .withSubject("Kowhai Flower")
                .withSubject("Flower Arranging")
                .withdrawn()
                .build();
        itemService.withdraw(context, item1);
        reindexItem(item1);
        solrSuggestService.rebuildAllDictionaries();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Expect ZERO terms for "grove" - this item is withdrawn
        getClient(token).perform(get("/api/discover/suggest")
                        .queryParam("dict", "subject")
                        .queryParam("q", "grove")
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.suggest.subject.grove.numFound", is(0)));

    }

    @Test
    public void testSuggestTermsFromNonDiscoverableItem() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item item1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .withAuthor("Doe, John")
                .withSubject("Totora Grove")
                .withSubject("Punga Grove")
                .withSubject("Kowhai Flower")
                .withSubject("Flower Arranging")
                .build();
        item1.setDiscoverable(false);
        itemService.update(context, item1);
        reindexItem(item1);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Expect ZERO terms for "grove" - this item is set as undiscoverable
        getClient(token).perform(get("/api/discover/suggest")
                        .queryParam("dict", "subject")
                        .queryParam("q", "grove")
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.suggest.subject.grove.numFound", is(0)));
    }

    @Test
    public void testSuggestTermsFromNonArchivedItem() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item item1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .withAuthor("Doe, John")
                .withSubject("Totora Grove")
                .withSubject("Punga Grove")
                .withSubject("Kowhai Flower")
                .withSubject("Flower Arranging")
                .build();
        item1.setArchived(false);
        itemService.update(context, item1);
        reindexItem(item1);
        solrSuggestService.rebuildAllDictionaries();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Expect ZERO terms for "grove" - this item is set as 'not archived'
        // (wrapped by in-progress item, withdrawn, etc)
        getClient(token).perform(get("/api/discover/suggest")
                        .queryParam("dict", "subject")
                        .queryParam("q", "grove")
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.suggest.subject.grove.numFound", is(0)));
    }

    /**
     * Test that a non-admin request to /build (rebuild dictionaries)
     * fails with 401, with or without a parameter
     */
    @Test
    public void nonAdminDictionaryBuildShouldFail() throws Exception {
        getClient().perform(
                get("/api/discover/suggest/build")
        ).andExpect(status().isUnauthorized());
        getClient().perform(
                get("/api/discover/suggest/build").param("dict", "subject")
        ).andExpect(status().isUnauthorized());
    }

    /**
     * Test that an unauthenticated request returns HTTP 401 Unauthorized.
     */
    @Test
    public void unauthenticatedRequestShouldReturn401() throws Exception {
        getClient().perform(
                        get("/api/discover/suggest")
                                .param("dict", "subject")
                                .param("q", "test"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test that a non-allowed dictionary request returns HTTP 403 Forbidden.
     */
    @Test
    public void nonAllowedDictionaryShouldReturn403() throws Exception {
        String userToken = getAuthToken(eperson.getEmail(), password);

        getClient(userToken).perform(
                        get("/api/discover/suggest")
                                .param("dict", "not_in_allowlist")
                                .param("q", "test"))
                .andExpect(status().isForbidden());
    }

    /**
     * Test that missing required parameters return HTTP 400 Bad Request.
     */
    @Test
    public void missingParametersShouldReturn400() throws Exception {
        String userToken = getAuthToken(eperson.getEmail(), password);

        // Missing 'q' parameter
        getClient(userToken).perform(
                        get("/api/discover/suggest")
                                .param("dict", "subject"))
                .andExpect(status().isBadRequest());

        // Missing 'dict' parameter
        getClient(userToken).perform(
                        get("/api/discover/suggest")
                                .param("q", "test"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test that when discovery.suggest.allowed-dictionaries is empty (not configured),
     * ALL dictionaries are denied (HTTP 403).
     */
    @Test
    public void emptyAllowedDictionariesShouldDenyAll() throws Exception {
        // Set the allowed-dictionaries to an empty value
        configurationService.setProperty("discovery.suggest.allowed-dictionaries", new String[]{});

        String userToken = getAuthToken(eperson.getEmail(), password);

        // Even a previously-allowed dictionary like "subject" should now be forbidden
        getClient(userToken).perform(
                        get("/api/discover/suggest")
                                .param("dict", "subject")
                                .param("q", "test"))
                .andExpect(status().isForbidden());

        // Another dictionary should also be forbidden
        getClient(userToken).perform(
                        get("/api/discover/suggest")
                                .param("dict", "countries_file")
                                .param("q", "test"))
                .andExpect(status().isForbidden());
    }

    /**
     * Test that when discovery.suggest.allowed-dictionaries contains the wildcard "*",
     * ALL dictionaries are allowed (HTTP 200).
     */
    @Test
    public void wildcardAllowedDictionariesShouldAllowAll() throws Exception {
        // Set the allowed-dictionaries to wildcard
        configurationService.setProperty("discovery.suggest.allowed-dictionaries", new String[]{"*"});

        String userToken = getAuthToken(eperson.getEmail(), password);

        // Any dictionary name should be allowed (returns 200, not 403)
        getClient(userToken).perform(
                        get("/api/discover/suggest")
                                .param("dict", "subject")
                                .param("q", "test"))
                .andExpect(status().isOk());

        // Even an arbitrary dictionary name should be allowed with wildcard
        getClient(userToken).perform(
                        get("/api/discover/suggest")
                                .param("dict", "any_dictionary_name")
                                .param("q", "test"))
                .andExpect(status().isOk());
    }

    /**
     * Test that the /suggest/build endpoint also respects empty allowed-dictionaries
     * by returning 403 when the list is empty.
     */
    @Test
    public void emptyAllowedDictionariesBuildShouldDenyAll() throws Exception {
        configurationService.setProperty("discovery.suggest.allowed-dictionaries", new String[]{});

        String adminToken = getAuthToken(admin.getEmail(), password);

        // Building a specific dictionary should be forbidden when allowlist is empty
        getClient(adminToken).perform(
                        get("/api/discover/suggest/build")
                                .param("dict", "subject"))
                .andExpect(status().isForbidden());
    }

    /**
     * Test that the /suggest/build endpoint works with wildcard "*" for any dictionary.
     */
    @Test
    public void wildcardAllowedDictionariesBuildShouldAllowAll() throws Exception {
        configurationService.setProperty("discovery.suggest.allowed-dictionaries", new String[]{"*"});

        String adminToken = getAuthToken(admin.getEmail(), password);

        // Building any specific dictionary should be allowed
        getClient(adminToken).perform(
                        get("/api/discover/suggest/build")
                                .param("dict", "subject"))
                .andExpect(status().isOk());
    }
}
