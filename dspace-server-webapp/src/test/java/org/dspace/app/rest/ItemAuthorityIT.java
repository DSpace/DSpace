/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.matcher.ItemAuthorityMatcher.matchItemAuthorityProperties;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.dspace.app.rest.matcher.ItemAuthorityMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.authority.ItemAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.core.service.PluginService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.orcid.client.OrcidClient;
import org.dspace.orcid.factory.OrcidServiceFactory;
import org.dspace.orcid.factory.OrcidServiceFactoryImpl;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.orcid.jaxb.model.v3.release.search.expanded.ExpandedSearch;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class handles ItemAuthority related IT.
 *
 * @author Mykhaylo Boychuk (4Science.it)
 */
public class ItemAuthorityIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private PluginService pluginService;

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    @Autowired
    private OrcidClient orcidClient;

    private OrcidClient orcidClientMock = mock(OrcidClient.class);

    @Before
    public void setup() {
        ((OrcidServiceFactoryImpl) OrcidServiceFactory.getInstance()).setOrcidClient(orcidClientMock);
        when(orcidClientMock.expandedSearch(any(), anyInt(), anyInt())).thenReturn(new ExpandedSearch());
    }

    @After
    public void after() {
        ((OrcidServiceFactoryImpl) OrcidServiceFactory.getInstance()).setOrcidClient(orcidClient);
    }

    @Test
    public void singleItemAuthorityTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Test collection")
                .build();

        Item orgUnit_1 = ItemBuilder.createItem(context, col1)
                .withTitle("OrgUnit_1")
                .withEntityType("orgunit")
                .build();

        Item orgUnit_2 = ItemBuilder.createItem(context, col1)
                .withTitle("OrgUnit_2")
                .withEntityType("orgunit")
                .build();

        Item author_1 = ItemBuilder.createItem(context, col1)
                .withTitle("Author 1")
                .withEntityType("person")
                .withPersonMainAffiliation(orgUnit_1.getName(), orgUnit_1.getID().toString())
                .withPersonAffiliation(orgUnit_1.getName(), orgUnit_1.getID().toString())
                .build();

        Item author_2 = ItemBuilder.createItem(context, col1)
                .withTitle("Author 2")
                .withPersonMainAffiliation(orgUnit_1.getName(), orgUnit_1.getID().toString())
                .withPersonAffiliation(orgUnit_1.getName(), orgUnit_1.getID().toString())
                .withEntityType("person")
                .build();

        Item author_3 = ItemBuilder.createItem(context, col1)
                .withTitle("Author 3")
                .withPersonMainAffiliation(orgUnit_2.getName(), orgUnit_2.getID().toString())
                .withPersonAffiliation(orgUnit_2.getName(), orgUnit_2.getID().toString())
                .withEntityType("person")
                .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
                        .param("metadata", "dc.contributor.author")
                        .param("collection", col1.getID().toString())
                        .param("filter", "author"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                            ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(author_1.getID().toString(),
                                "Author 1", "Author 1", "vocabularyEntry",
                                Map.of("data-oairecerif_author_affiliation", "OrgUnit_1::"
                                    + orgUnit_1.getID(),
                                    "oairecerif_author_affiliation", "OrgUnit_1::"
                                        + orgUnit_1.getID())),
                            ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(author_2.getID().toString(),
                                "Author 2", "Author 2", "vocabularyEntry",
                                Map.of("data-oairecerif_author_affiliation", "OrgUnit_1::"
                                    + orgUnit_1.getID(),
                                    "oairecerif_author_affiliation", "OrgUnit_1::"
                                        + orgUnit_1.getID())),
                            ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(author_3.getID().toString(),
                                "Author 3", "Author 3", "vocabularyEntry",
                                Map.of("data-oairecerif_author_affiliation", "OrgUnit_2::"
                                    + orgUnit_2.getID(),
                                    "oairecerif_author_affiliation", "OrgUnit_2::"
                                        + orgUnit_2.getID()))
                        )))
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));
    }
    @Test
    public void alternativeNamesAuthorityTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Test collection")
                .build();

        Item orgUnit_1 = ItemBuilder.createItem(context, col1)
                .withTitle("OrgUnit_1")
                .withEntityType("orgunit")
                .build();

        Item author_1 = ItemBuilder.createItem(context, col1)
                .withTitle("Author 1")
                .withVariantName("Author Variant")
                .withEntityType("person")
                .withPersonAffiliation(orgUnit_1.getName(), orgUnit_1.getID().toString())
                .build();

        Item author_2 = ItemBuilder.createItem(context, col1)
                .withTitle("Author 2")
                .withVariantName("Author 2 Variant")
                .withPersonAffiliation(orgUnit_1.getName(), orgUnit_1.getID().toString())
                .withEntityType("person")
                .build();

        Item author_3 = ItemBuilder.createItem(context, col1)
                .withTitle("Author 3")
                .withPersonAffiliation(orgUnit_1.getName(), orgUnit_1.getID().toString())
                .withEntityType("person")
                .build();

        context.restoreAuthSystemState();
        String author1Alternatives = "Author 1::" + author_1.getID() + "|||Author Variant::" + author_1.getID();
        Map<String,String> author1Extras = Map.of(
                "data-oairecerif_author_affiliation", "OrgUnit_1::" + orgUnit_1.getID(),
                "oairecerif_author_affiliation", "OrgUnit_1::" + orgUnit_1.getID(),
                "alternative-names", author1Alternatives);
        String author2Alternatives = "Author 2::" + author_2.getID() + "|||Author 2 Variant::" + author_2.getID();
        Map<String,String> author2Extras = Map.of(
                "data-oairecerif_author_affiliation", "OrgUnit_1::" + orgUnit_1.getID(),
                "oairecerif_author_affiliation", "OrgUnit_1::" + orgUnit_1.getID(),
                "alternative-names", author2Alternatives);
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
                        .param("metadata", "dc.contributor.author")
                        .param("collection", col1.getID().toString())
                        .param("filter", "author"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                            ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(author_1.getID().toString(),
                                "Author 1", "Author 1", "vocabularyEntry", author1Extras),
                            ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(author_2.getID().toString(),
                                "Author 2", "Author 2", "vocabularyEntry", author2Extras),
                            ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(author_3.getID().toString(),
                                "Author 3", "Author 3", "vocabularyEntry", Map.of(
                                        "data-oairecerif_author_affiliation", "OrgUnit_1::" + orgUnit_1.getID(),
                                            "oairecerif_author_affiliation", "OrgUnit_1::" + orgUnit_1.getID()))
                        )))
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));
    }

    @Test
    public void multiItemAuthorityTest() throws Exception {
       context.turnOffAuthorisationSystem();

       configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
                new String[] {
                        "org.dspace.content.authority.ItemMultiAuthority = AuthorAuthority",
                        "org.dspace.content.authority.ItemAuthority = OrgUnitAuthority"
                });

       configurationService.setProperty("solr.authority.server", "${solr.server}/authority");
       configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
       configurationService.setProperty("choices.presentation.dc.contributor.author", "authorLookup");
       configurationService.setProperty("authority.controlled.dc.contributor.author", "true");

       configurationService.setProperty("choices.plugin.person.affiliation.name", "OrgUnitAuthority");
       configurationService.setProperty("choices.presentation.person.affiliation.name", "authorLookup");
       configurationService.setProperty("authority.controlled.person.affiliation.name", "true");

       // These clears have to happen so that the config is actually reloaded in those classes. This is needed for
       // the properties that we're altering above and this is only used within the tests
       pluginService.clearNamedPluginClasses();
       choiceAuthorityService.clearCache();

       parentCommunity = CommunityBuilder.createCommunity(context).build();
       Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).build();

       Item orgUnit_1 = ItemBuilder.createItem(context, col1)
                                   .withTitle("OrgUnit_1")
                                   .withEntityType("orgunit")
                                   .build();

       Item orgUnit_2 = ItemBuilder.createItem(context, col1)
                                   .withTitle("OrgUnit_2")
                                   .withEntityType("orgunit")
                                   .build();

       Item author_1 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author 1")
                                  .withPersonAffiliation(orgUnit_1.getName(), orgUnit_1.getID().toString())
                                  .withPersonAffiliation(orgUnit_2.getName(), orgUnit_2.getID().toString())
                                  .withEntityType("person")
                                  .build();

       Item author_2 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author 2")
                                  .withPersonAffiliation(orgUnit_2.getName(), orgUnit_2.getID().toString())
                                  .withEntityType("person")
                                  .build();

       context.restoreAuthSystemState();

       String token = getAuthToken(eperson.getEmail(), password);
       getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
                       .param("metadata", "dc.contributor.author")
                       .param("collection", col1.getID().toString())
                       .param("filter", "author"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                               // filled with AuthorAuthority extra metadata generator
                               ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(author_1.getID().toString(),
                               "Author 1(OrgUnit_1)", "Author 1", "vocabularyEntry",
                               Map.of("data-oairecerif_author_affiliation", "OrgUnit_1::" + orgUnit_1.getID(),
                                   "oairecerif_author_affiliation", "OrgUnit_1::" + orgUnit_1.getID())),
                               ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(author_1.getID().toString(),
                               "Author 1(OrgUnit_2)", "Author 1", "vocabularyEntry",
                               Map.of("data-oairecerif_author_affiliation", "OrgUnit_2::" + orgUnit_2.getID(),
                                   "oairecerif_author_affiliation", "OrgUnit_2::" + orgUnit_2.getID())),
                               ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(author_2.getID().toString(),
                               "Author 2(OrgUnit_2)", "Author 2", "vocabularyEntry",
                               Map.of("data-oairecerif_author_affiliation", "OrgUnit_2::" + orgUnit_2.getID(),
                                   "oairecerif_author_affiliation", "OrgUnit_2::" + orgUnit_2.getID())),
                               ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(author_1.getID().toString(),
                               "Author 1", "Author 1", "vocabularyEntry",
                               Map.of()),
                               ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(author_2.getID().toString(),
                               "Author 2", "Author 2", "vocabularyEntry",
                               Map.of()),
                               // filled with EditorAuthority extra metadata generator
                               ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(author_1.getID().toString(),
                               "Author 1", "Author 1", "vocabularyEntry", Map.of()),
                               ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(author_2.getID().toString(),
                               "Author 2", "Author 2", "vocabularyEntry", Map.of())
                               )))
                       .andExpect(jsonPath("$.page.totalElements", Matchers.is(7)));
    }

    @Test
    public void singleItemAuthorityWithoutOrgUnitTest() throws Exception {
       context.turnOffAuthorisationSystem();

       parentCommunity = CommunityBuilder.createCommunity(context).build();
       Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Test collection")
                                          .build();

       Item author_1 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author 1")
                                  .withEntityType("person")
                                  .build();

       context.restoreAuthSystemState();

       String token = getAuthToken(eperson.getEmail(), password);
       getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
                       .param("metadata", "dc.contributor.author")
                       .param("collection", col1.getID().toString())
                       .param("filter", "author"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._embedded.entries", Matchers.contains(
                           ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(author_1.getID().toString(),
                                "Author 1", "Author 1", "vocabularyEntry",
                                Map.of())
                       )))
                       .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));
    }

    @Test
    public void ePersonAuthorityTest() throws Exception {
       context.turnOffAuthorisationSystem();

       EPerson ePerson1 = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("Andrea", "Bollini")
                                        .withEmail("Andrea.Bollini@example.com")
                                        .build();

       EPerson ePerson2 = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("Mykhaylo", "Boychuk")
                                        .withEmail("Mykhaylo.Boychuk@example.com")
                                        .build();

       EPerson ePerson3 = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("Luca", "Giamminonni")
                                        .withEmail("Luca.Giamminonni@example.com")
                                        .build();

       EPerson ePerson4 = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("Andrea", "Pascarelli")
                                        .withEmail("Andrea.Pascarelli@example.com")
                                        .build();

       context.restoreAuthSystemState();

       String token = getAuthToken(admin.getEmail(), password);
       getClient(token).perform(get("/api/submission/vocabularies/EPersonAuthority/entries")
                       .param("filter", "Andrea"))
                       .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                                ItemAuthorityMatcher.matchItemAuthorityProperties(ePerson1.getID().toString(),
                                        ePerson1.getFullName(), ePerson1.getFullName(), "vocabularyEntry"),
                                ItemAuthorityMatcher.matchItemAuthorityProperties(ePerson4.getID().toString(),
                                        ePerson4.getFullName(), ePerson4.getFullName(), "vocabularyEntry"))))
                .andExpect(jsonPath("$._embedded.entries", Matchers.not(
                        ItemAuthorityMatcher.matchItemAuthorityProperties(ePerson2.getID().toString(),
                                ePerson2.getFullName(), ePerson2.getFullName(), "vocabularyEntry"))))
                .andExpect(jsonPath("$._embedded.entries", Matchers.not(
                        ItemAuthorityMatcher.matchItemAuthorityProperties(ePerson3.getID().toString(),
                                ePerson3.getFullName(), ePerson3.getFullName(), "vocabularyEntry"))))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)));
    }

    @Test
    public void ePersonAuthorityNoComparisonTest() throws Exception {
       context.turnOffAuthorisationSystem();

       EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Andrea", "Bollini")
            .withEmail("Andrea.Bollini@example.com")
            .build();

       EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Mykhaylo", "Boychuk")
            .withEmail("Mykhaylo.Boychuk@example.com")
            .build();

       context.restoreAuthSystemState();

       String token = getAuthToken(eperson.getEmail(), password);
       getClient(token).perform(get("/api/submission/vocabularies/EPersonAuthority/entries")
                       .param("filter", "wrong text"))
                       .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));
    }

    @Test
    public void ePersonAuthorityEmptyQueryTest() throws Exception {
       context.turnOffAuthorisationSystem();

       EPersonBuilder.createEPerson(context)
           .withNameInMetadata("Andrea", "Bollini")
           .withEmail("Andrea.Bollini@example.com")
           .build();

       EPersonBuilder.createEPerson(context)
           .withNameInMetadata("Mykhaylo", "Boychuk")
           .withEmail("Mykhaylo.Boychuk@example.com")
           .build();

       context.restoreAuthSystemState();

       String token = getAuthToken(eperson.getEmail(), password);
       getClient(token).perform(get("/api/submission/vocabularies/EPersonAuthority/entries")
                       .param("filter", ""))
                       .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void ePersonAuthorityUnauthorizedTest() throws Exception {

       getClient().perform(get("/api/submission/vocabularies/EPersonAuthority/entries")
                  .param("filter", "wrong text"))
                  .andExpect(status().isUnauthorized());
    }

    @Test
    public void groupAuthorityTest() throws Exception {
       context.turnOffAuthorisationSystem();

       Group simpleGroup = GroupBuilder.createGroup(context)
                                 .withName("Simple Group")
                                 .build();

       Group groupA = GroupBuilder.createGroup(context)
                                  .withName("Group A")
                                  .build();

       Group admins = GroupBuilder.createGroup(context)
                                  .withName("Admins")
                                  .build();

       context.restoreAuthSystemState();

       String token = getAuthToken(eperson.getEmail(), password);
       getClient(token).perform(get("/api/submission/vocabularies/GroupAuthority/entries")
                       .param("filter", "Group"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                               ItemAuthorityMatcher.matchItemAuthorityProperties(simpleGroup.getID().toString(),
                                       simpleGroup.getName(), simpleGroup.getName(), "vocabularyEntry"),
                               ItemAuthorityMatcher.matchItemAuthorityProperties(groupA.getID().toString(),
                                       groupA.getName(), groupA.getName(), "vocabularyEntry"))))
                       .andExpect(jsonPath("$._embedded.entries", Matchers.not(ItemAuthorityMatcher
                               .matchItemAuthorityProperties(admins.getID().toString(),admins.getName(),
                                                             admins.getName(), "vocabularyEntry"))))
                       .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)));
    }

    @Test
    public void groupAuthorityEmptyQueryTest() throws Exception {
       context.turnOffAuthorisationSystem();

       GroupBuilder.createGroup(context)
           .withName("Simple Group")
           .build();

       GroupBuilder.createGroup(context)
           .withName("Group A")
           .build();

       context.restoreAuthSystemState();

       String token = getAuthToken(eperson.getEmail(), password);
       getClient(token).perform(get("/api/submission/vocabularies/GroupAuthority/entries")
                       .param("filter", ""))
                       .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void groupAuthorityUnauthorizedTest() throws Exception {

       getClient().perform(get("/api/submission/vocabularies/GroupAuthority/entries")
                  .param("filter", "wrong text"))
                  .andExpect(status().isUnauthorized());
    }

    @Test
    public void itemAuthorityWithValidExternalSourceTest() throws Exception {
        Map<String, String> exptectedMap = new HashMap<String, String>(
                Map.of("dc.contributor.author", "authorAuthority"));
       context.turnOffAuthorisationSystem();

       configurationService.setProperty("choises.externalsource.dc.contributor.author", "authorAuthority");

       // These clears have to happen so that the config is actually reloaded in those classes. This is needed for
       // the properties that we're altering above and this is only used within the tests
       pluginService.clearNamedPluginClasses();
       choiceAuthorityService.clearCache();

       context.restoreAuthSystemState();

       String token = getAuthToken(eperson.getEmail(), password);
       getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$.entity", Matchers.is("Person")))
                       .andExpect(jsonPath("$.externalSource", Matchers.is(exptectedMap)));
    }

    @Test
    public void itemAuthorityWithNotValidExternalSourceTest() throws Exception {
        Map<String, String> exptectedMap = new HashMap<String, String>();
       context.turnOffAuthorisationSystem();

       configurationService.setProperty("choises.externalsource.dc.contributor.author", "fakeAuthorAuthority");

       // These clears have to happen so that the config is actually reloaded in those classes. This is needed for
       // the properties that we're altering above and this is only used within the tests
       pluginService.clearNamedPluginClasses();
       choiceAuthorityService.clearCache();

       context.restoreAuthSystemState();

       String token = getAuthToken(eperson.getEmail(), password);
       getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$.entity", Matchers.is("Person")))
                       .andExpect(jsonPath("$.externalSource", Matchers.is(exptectedMap)));
    }

    @Test
    public void entityTypeAuthorityFilters() throws Exception {
        context.turnOffAuthorisationSystem();

        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
            new String[] { "org.dspace.content.authority.ItemAuthority = PersonAuthority" });

        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");

        configurationService.setProperty("cris.ItemAuthority.PersonAuthority.entityType", "EntityPerson");

        // These clears have to happen so that the config is actually reloaded in those
        // classes. This is needed for
        // the properties that we're altering above and this is only used within the
        // tests
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();

        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).build();

        Item person1 = ItemBuilder.createItem(context, col1)
            .withTitle("Person 1")
            .withType("mytype")
            .withEntityType("EntityPerson").build();

        ItemBuilder.createItem(context, col1)
            .withTitle("Person 2")
            .withEntityType("EntityPerson")
            .build();

        ItemBuilder.createItem(context, col1)
            .withTitle("Person 3")
            .withType("anotherType")
            .withEntityType("EntityPerson").build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token)
            .perform(get("/api/submission/vocabularies/PersonAuthority/entries").param("filter", "Person"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.entries",
                Matchers.containsInAnyOrder(ItemAuthorityMatcher.matchItemAuthorityProperties(
                    person1.getID().toString(), "Person 1", "Person 1", "vocabularyEntry"))));

    }

    @Test
    public void authorityNameAuthorityFilters() throws Exception {
        context.turnOffAuthorisationSystem();

        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
            new String[] { "org.dspace.content.authority.ItemAuthority = EntityPersonAuthority",
                "org.dspace.content.authority.ItemAuthority = PersonAuthority" });

        configurationService.setProperty("choices.plugin.dc.contributor.author", "EntityPersonAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("cris.ItemAuthority.EntityPersonAuthority.entityType", "Person");
        configurationService.setProperty("cris.ItemAuthority.PersonAuthority.entityType", "Person");

        // These clears have to happen so that the config is actually reloaded in those
        // classes. This is needed for
        // the properties that we're altering above and this is only used within the
        // tests
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();

        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).build();

        Item person1 = ItemBuilder.createItem(context, col1)
            .withTitle("Author 1")
            .withType("mytype")
            .withEntityType("Person").build();

        Item person2 = ItemBuilder.createItem(context, col1)
            .withTitle("Author 2")
            .withEntityType("Person")
            .build();

        Item person3 = ItemBuilder.createItem(context, col1)
            .withTitle("Author 3")
            .withType("anotherType")
            .withEntityType("Person").build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token)
            .perform(get("/api/submission/vocabularies/EntityPersonAuthority/entries").param("filter", "Author"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.entries",
                Matchers.containsInAnyOrder(ItemAuthorityMatcher.matchItemAuthorityProperties(
                    person1.getID().toString(), "Author 1", "Author 1", "vocabularyEntry"))));

        getClient(token).perform(get("/api/submission/vocabularies/PersonAuthority/entries").param("filter", "Author"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(3)))
            .andExpect(jsonPath("$._embedded.entries",
                Matchers.containsInAnyOrder(
                    ItemAuthorityMatcher.matchItemAuthorityProperties(person1.getID().toString(),
                        "Author 1", "Author 1", "vocabularyEntry"),
                    ItemAuthorityMatcher.matchItemAuthorityProperties(person2.getID().toString(),
                        "Author 2", "Author 2", "vocabularyEntry"),
                    ItemAuthorityMatcher.matchItemAuthorityProperties(person3.getID().toString(),
                        "Author 3", "Author 3", "vocabularyEntry"))));

    }
    @Test
    public void personAuthorityTests() throws Exception {
        context.turnOffAuthorisationSystem();

        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
            new String[] { "org.dspace.content.authority.ItemAuthority = PersonAuthority" });
        configurationService.setProperty("choices.plugin.dc.contributor.author", "PersonAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("cris.ItemAuthority.PersonAuthority.entityType", "Person");

        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();

        parentCommunity = CommunityBuilder.createCommunity(context).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Person")
            .build();

        String token = getAuthToken(eperson.getEmail(), password);
        // AuthorAuthority Test
        Item person1 = ItemBuilder.createItem(context, col1)
            .withTitle("Bollini, Andrea")
            .withFullName("Andrea Bollini")
            .withGivenName("Andrea")
            .withFamilyName("Bollini")
            .build();

        Item person2 = ItemBuilder.createItem(context, col1)
            .withTitle("Bollini, Riccardo Andrea")
            .withVariantName("Riccardo Andrea Bollini")
            .build();

        Item person3 = ItemBuilder.createItem(context, col1)
            .withTitle("Giamminonni, Andrea")
            .withVariantName("Giamminonni Andrea")
            .build();

        Item person4 = ItemBuilder.createItem(context, col1)
            .withTitle("Cortese, Claudio")
            .withGivenName("Claudio Andrea Paolo")
            .withFamilyName("Cortese")
            .build();

        context.restoreAuthSystemState();
        String person4Id = person4.getID().toString();

        getClient(token).perform(get("/api/submission/vocabularies/PersonAuthority/entries")
            .param("filter", "Cortese Claudio")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.entries", contains(matchItemAuthorityProperties(
                person4Id, "Cortese, Claudio", "Cortese, Claudio", "vocabularyEntry"))));

        getClient(token).perform(get("/api/submission/vocabularies/PersonAuthority/entries")
            .param("filter", "Claudio Cortese")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.entries", contains(matchItemAuthorityProperties(
                person4Id, "Cortese, Claudio", "Cortese, Claudio", "vocabularyEntry"))));

        getClient(token).perform(get("/api/submission/vocabularies/PersonAuthority/entries")
            .param("filter", "Claudio Andrea Paolo Cortese"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));

    }

    @Test
    public void testAuthorStrictMatchAuthority() throws Exception {

        context.turnOffAuthorisationSystem();

        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
            new String[] { "org.dspace.content.authority.ItemAuthority = AuthorStrictMatchAuthority" });
        configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorStrictMatchAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("cris.ItemAuthority.AuthorStrictMatchAuthority.forceInternalName", true);
        configurationService.setProperty("cris.ItemAuthority.AuthorStrictMatchAuthority.entityType", "Person");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("cris.ItemAuthority.PersonAuthority.entityType", "Person");

        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();

        parentCommunity = CommunityBuilder.createCommunity(context).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Person")
            .build();

        Item person1 = ItemBuilder.createItem(context, col1)
            .withTitle("Bollini, Andrea")
            .withFullName("Andrea Bollini")
            .withVariantName("Andrea Bollini Test")
            .withGivenName("Andrea")
            .withFamilyName("Bollini")
            .build();

        Item person2 = ItemBuilder.createItem(context, col1)
            .withTitle("Bollini, Riccardo Andrea")
            .withVariantName("Riccardo Andrea Bollini")
            .build();


        context.restoreAuthSystemState();

        String person1Id = person1.getID().toString();
        String person2Id = person2.getID().toString();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/submission/vocabularies/AuthorStrictMatchAuthority/entries")
            .param("filter", "Bollini Andrea")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.entries", contains(matchItemAuthorityProperties(
                person1Id, "Bollini, Andrea", "Bollini, Andrea", "vocabularyEntry"))));

        getClient(token).perform(get("/api/submission/vocabularies/AuthorStrictMatchAuthority/entries")
            .param("filter", "Bollini A.")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));

        getClient(token).perform(get("/api/submission/vocabularies/AuthorStrictMatchAuthority/entries")
            .param("filter", "A. Bollini")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));

        getClient(token).perform(get("/api/submission/vocabularies/AuthorStrictMatchAuthority/entries")
            .param("filter", "Riccardo Andrea Bollini")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.entries", contains(matchItemAuthorityProperties(
                person2Id, "Bollini, Riccardo Andrea", "Bollini, Riccardo Andrea", "vocabularyEntry"))));

        getClient(token).perform(get("/api/submission/vocabularies/AuthorStrictMatchAuthority/entries")
            .param("filter", "Andrea Bollini Test")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.entries", contains(matchItemAuthorityProperties(
                person1Id, "Bollini, Andrea", "Bollini, Andrea", "vocabularyEntry"))));

        getClient(token).perform(get("/api/submission/vocabularies/AuthorStrictMatchAuthority/entries")
            .param("filter", "Bollini Andrea Riccardo")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));

        getClient(token).perform(get("/api/submission/vocabularies/AuthorStrictMatchAuthority/entries")
            .param("filter", "Andrea Giamminonni")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));
    }

    @Test
    public void authorCoarseAuthorityTests() throws Exception {
        context.turnOffAuthorisationSystem();

        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
            new String[] { "org.dspace.content.authority.ItemAuthority = AuthorCoarseMatchAuthority" });
        configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorCoarseMatchAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("cris.ItemAuthority.AuthorCoarseMatchAuthority.forceInternalName", true);
        configurationService.setProperty("cris.ItemAuthority.AuthorCoarseMatchAuthority.entityType", "Person");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("cris.ItemAuthority.PersonAuthority.entityType", "Person");

        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();

        parentCommunity = CommunityBuilder.createCommunity(context).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Person")
            .build();

        String token = getAuthToken(eperson.getEmail(), password);
        // AuthorAuthority Test
        Item person1 = ItemBuilder.createItem(context, col1)
            .withTitle("Bollini, Andrea")
            .withFullName("Andrea Bollini")
            .withGivenName("Andrea")
            .withFamilyName("Bollini")
            .build();

        Item person2 = ItemBuilder.createItem(context, col1)
            .withTitle("Bollini, Riccardo Andrea")
            .withVariantName("Riccardo Andrea Bollini")
            .build();

        Item person3 = ItemBuilder.createItem(context, col1)
            .withTitle("Giamminonni, Andrea")
            .withVariantName("Giamminonni Andrea")
            .build();

        Item person4 = ItemBuilder.createItem(context, col1)
            .withTitle("Cortese, Claudio")
            .withGivenName("Claudio Andrea Paolo")
            .withFamilyName("Cortese")
            .build();

        context.restoreAuthSystemState();
        String person4Id = person4.getID().toString();

        getClient(token).perform(get("/api/submission/vocabularies/AuthorCoarseMatchAuthority/entries")
            .param("filter", "Cortese Claudio")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.entries", contains(matchItemAuthorityProperties(
                person4Id, "Cortese, Claudio", "Cortese, Claudio", "vocabularyEntry"))));

        getClient(token).perform(get("/api/submission/vocabularies/AuthorCoarseMatchAuthority/entries")
            .param("filter", "Claudio Cortese")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.entries", contains(matchItemAuthorityProperties(
                person4Id, "Cortese, Claudio", "Cortese, Claudio", "vocabularyEntry"))));

        getClient(token).perform(get("/api/submission/vocabularies/AuthorCoarseMatchAuthority/entries")
            .param("filter", "Claudio Andrea Paolo Cortese")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));

        getClient(token).perform(get("/api/submission/vocabularies/AuthorCoarseMatchAuthority/entries")
            .param("filter", "Cortese Claudio Andrea Paolo")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.entries", contains(matchItemAuthorityProperties(
                person4Id, "Cortese, Claudio", "Cortese, Claudio", "vocabularyEntry"))));

        getClient(token).perform(get("/api/submission/vocabularies/AuthorCoarseMatchAuthority/entries")
            .param("filter", "Claudio Paolo Cortese")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.entries", contains(matchItemAuthorityProperties(
                person4Id, "Cortese, Claudio", "Cortese, Claudio", "vocabularyEntry"))));

        getClient(token).perform(get("/api/submission/vocabularies/AuthorCoarseMatchAuthority/entries")
            .param("filter", "Claudio Andrea Cortese")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.entries", contains(matchItemAuthorityProperties(
                person4Id, "Cortese, Claudio", "Cortese, Claudio", "vocabularyEntry"))));

        getClient(token).perform(get("/api/submission/vocabularies/AuthorCoarseMatchAuthority/entries")
            .param("filter", "Andrea Paolo Cortese")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.entries", contains(matchItemAuthorityProperties(
                person4Id, "Cortese, Claudio", "Cortese, Claudio", "vocabularyEntry"))));

        getClient(token).perform(get("/api/submission/vocabularies/AuthorCoarseMatchAuthority/entries")
            .param("filter", "Cortese Andrea Paolo")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.entries", contains(matchItemAuthorityProperties(
                person4Id, "Cortese, Claudio", "Cortese, Claudio", "vocabularyEntry"))));

        getClient(token).perform(get("/api/submission/vocabularies/AuthorCoarseMatchAuthority/entries")
            .param("filter", "Andrea Cortese")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.entries", contains(matchItemAuthorityProperties(
                person4Id, "Cortese, Claudio", "Cortese, Claudio", "vocabularyEntry"))));

        getClient(token).perform(get("/api/submission/vocabularies/AuthorCoarseMatchAuthority/entries")
            .param("filter", "Cortese Andrea")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.entries", contains(matchItemAuthorityProperties(
                person4Id, "Cortese, Claudio", "Cortese, Claudio", "vocabularyEntry"))));

        getClient(token).perform(get("/api/submission/vocabularies/AuthorCoarseMatchAuthority/entries")
            .param("filter", "Claudio Cortese")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.entries", contains(matchItemAuthorityProperties(
                person4Id, "Cortese, Claudio", "Cortese, Claudio", "vocabularyEntry"))));

        getClient(token).perform(get("/api/submission/vocabularies/AuthorCoarseMatchAuthority/entries")
            .param("filter", "Cortese Claudio")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.entries", contains(matchItemAuthorityProperties(
                person4Id, "Cortese, Claudio", "Cortese, Claudio", "vocabularyEntry"))));

        getClient(token).perform(get("/api/submission/vocabularies/AuthorCoarseMatchAuthority/entries")
            .param("filter", "Paolo Cortese")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.entries", contains(matchItemAuthorityProperties(
                person4Id, "Cortese, Claudio", "Cortese, Claudio", "vocabularyEntry"))));

        getClient(token).perform(get("/api/submission/vocabularies/AuthorCoarseMatchAuthority/entries")
            .param("filter", "Cortese Paolo")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.entries", contains(matchItemAuthorityProperties(
                person4Id, "Cortese, Claudio", "Cortese, Claudio", "vocabularyEntry"))));

        getClient(token).perform(get("/api/submission/vocabularies/AuthorCoarseMatchAuthority/entries")
            .param("filter", "Cortese A. P.")
            .param("exact", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.entries", contains(matchItemAuthorityProperties(
                person4Id, "Cortese, Claudio", "Cortese, Claudio", "vocabularyEntry"))));
    }

    @Test
    public void itemAuthoritySourceReferenceTest() throws Exception {
        context.turnOffAuthorisationSystem();

        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
            new String[] { "org.dspace.content.authority.ItemAuthority = PersonAuthority" });

        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");

        configurationService.setProperty("cris.ItemAuthority.PersonAuthority.entityType", "EntityPerson");

        // set authority source reference
        configurationService.setProperty("cris.ItemAuthority.PersonAuthority.source", "ORCID");

        // These clears have to happen so that the config is actually reloaded in those
        // classes. This is needed for
        // the properties that we're altering above and this is only used within the
        // tests
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();

        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).build();

        Item person1 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Person 1")
                                  .withType("mytype")
                                  .withEntityType("EntityPerson").build();

        ItemBuilder.createItem(context, col1)
                   .withTitle("Person 2")
                   .withEntityType("EntityPerson")
                   .build();

        ItemBuilder.createItem(context, col1)
                   .withTitle("Person 3")
                   .withType("anotherType")
                   .withEntityType("EntityPerson").build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token)
            .perform(get("/api/submission/vocabularies/PersonAuthority/entries").param("filter", "Person"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.entries",
                Matchers.containsInAnyOrder(ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(
                    person1.getID().toString(), "Person 1", "Person 1", "vocabularyEntry", Map.of(),
                    ItemAuthority.DEFAULT))));
    }

    @Test
    public void ignoreWithdrawnAndNonDiscoverableItemAuthorityTest() throws Exception {
        context.turnOffAuthorisationSystem();

        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
                new String[] { "org.dspace.content.authority.ItemAuthority = PersonAuthority" });

        configurationService.setProperty("cris.ItemAuthority.PersonAuthority.entityType", "Person");

        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();

        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).build();

        Item person1 = ItemBuilder.createItem(context, col1)
                .withTitle("Author 1")
                .withType("mytype")
                .withEntityType("Person")
                .build();

        Item person2 = ItemBuilder.createItem(context, col1)
                .withTitle("Author 2")
                .withEntityType("Person")
                .build();

        ItemBuilder.createItem(context, col1)
                .withTitle("Author 3")
                .withType("anotherType")
                .withEntityType("Person")
                .makeUnDiscoverable()
                .build();

        ItemBuilder.createItem(context, col1)
                .withTitle("Author 4")
                .withEntityType("Person")
                .withdrawn()
                .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/PersonAuthority/entries").param("filter", "Author"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", Matchers.is(2)))
                .andExpect(jsonPath("$._embedded.entries",
                        Matchers.containsInAnyOrder(
                                ItemAuthorityMatcher.matchItemAuthorityProperties(person1.getID().toString(),
                                        "Author 1", "Author 1", "vocabularyEntry"),
                                ItemAuthorityMatcher.matchItemAuthorityProperties(person2.getID().toString(),
                                        "Author 2", "Author 2", "vocabularyEntry"))));
    }

    @Override
    @After
    // We need to cleanup the authorities cache once than the configuration has been restored
    public void destroy() throws Exception {
        super.destroy();
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
    }
}
