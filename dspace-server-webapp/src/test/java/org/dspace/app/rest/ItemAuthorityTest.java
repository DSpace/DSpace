/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.matcher.ItemAuthorityMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author Mykhaylo Boychuk (4Science.it)
 */
public class ItemAuthorityTest extends AbstractControllerIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private PluginService pluginService;

    @Autowired
    private ChoiceAuthorityService cas;

    @Test
    public void singleItemAuthorityTest() throws Exception {
       context.turnOffAuthorisationSystem();

       parentCommunity = CommunityBuilder.createCommunity(context).build();
       Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Test collection")
                                          .build();

       Item author_1 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author 1")
                                  .withRelationshipType("person")
                                  .build();

       Item author_2 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author 2")
                                  .withRelationshipType("person")
                                  .build();

       Item author_3 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author 3")
                                  .withRelationshipType("person")
                                  .build();

       Item OrgUnit_1 = ItemBuilder.createItem(context, col1)
                                   .withTitle("OrgUnit_1")
                                   .withRelationshipType("orgunit")
                                   .build();

       Item OrgUnit_2 = ItemBuilder.createItem(context, col1)
                                   .withTitle("OrgUnit_2")
                                   .withRelationshipType("orgunit")
                                   .build();

       itemService.addMetadata(context, author_1, "crisrp", "dept", null, null, "OrgUnit_1",
                                                   OrgUnit_1.getID().toString(), Choices.CF_ACCEPTED);
       itemService.addMetadata(context, author_2, "crisrp", "dept", null, null, "OrgUnit_1",
                                                   OrgUnit_1.getID().toString(), Choices.CF_ACCEPTED);
       itemService.addMetadata(context, author_3, "crisrp", "dept", null, null, "OrgUnit_2",
                                                   OrgUnit_2.getID().toString(), Choices.CF_ACCEPTED);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
                        .param("metadata", "dc.contributor.author")
                        .param("collection", col1.getID().toString())
                        .param("filter", "author"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                             ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(author_1.getID().toString(),
                                       "Author 1", "Author 1","vocabularyEntry","contributor_department","OrgUnit_1"),
                             ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(author_2.getID().toString(),
                                       "Author 2", "Author 2","vocabularyEntry","contributor_department","OrgUnit_1"),
                             ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(author_3.getID().toString(),
                                       "Author 3", "Author 3","vocabularyEntry","contributor_department","OrgUnit_2")
                             )))
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));
    }

    @Test
    public void multiItemAuthorityTest() throws Exception {
       context.turnOffAuthorisationSystem();

       configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
                "org.dspace.content.authority.ItemMultiAuthority = AuthorAuthority");

       configurationService.setProperty("solr.authority.server", "${solr.server}/authority");
       configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
       configurationService.setProperty("choices.presentation.dc.contributor.author", "authorLookup");
       configurationService.setProperty("authority.controlled.dc.contributor.author", "true");

       // These clears have to happen so that the config is actually reloaded in those classes. This is needed for
       // the properties that we're altering above and this is only used within the tests
       pluginService.clearNamedPluginClasses();
       cas.clearCache();

       parentCommunity = CommunityBuilder.createCommunity(context).build();
       Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).build();

       Item author_1 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author 1")
                                  .withRelationshipType("person")
                                  .build();

       Item author_2 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author 2")
                                  .withRelationshipType("person")
                                  .build();

       Item OrgUnit_1 = ItemBuilder.createItem(context, col1)
                                   .withTitle("OrgUnit_1")
                                   .withRelationshipType("orgunit")
                                   .build();

       Item OrgUnit_2 = ItemBuilder.createItem(context, col1)
                                   .withTitle("OrgUnit_2")
                                   .withRelationshipType("orgunit")
                                   .build();

       itemService.addMetadata(context, author_1, "crisrp", "dept", null, null, "OrgUnit_1",
                                                   OrgUnit_1.getID().toString(), Choices.CF_ACCEPTED);
       itemService.addMetadata(context, author_1, "crisrp", "dept", null, null, "OrgUnit_2",
                                                   OrgUnit_2.getID().toString(), Choices.CF_ACCEPTED);
       itemService.addMetadata(context, author_2, "crisrp", "dept", null, null, "OrgUnit_2",
                                                   OrgUnit_2.getID().toString(), Choices.CF_ACCEPTED);
       context.restoreAuthSystemState();

       String token = getAuthToken(eperson.getEmail(), password);
       getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
                       .param("metadata", "dc.contributor.author")
                       .param("collection", col1.getID().toString())
                       .param("filter", "author"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                               ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(author_1.getID().toString(),
                              "Author 1(OrgUnit_1)", "Author 1","vocabularyEntry","contributor_department","OrgUnit_1"),
                               ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(author_1.getID().toString(),
                              "Author 1(OrgUnit_2)", "Author 1","vocabularyEntry","contributor_department","OrgUnit_2"),
                               ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(author_2.getID().toString(),
                              "Author 2(OrgUnit_2)", "Author 2","vocabularyEntry","contributor_department","OrgUnit_2")
                               )))
                       .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));
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
                                  .withRelationshipType("person")
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
                             "Author 1", "Author 1","vocabularyEntry","contributor_department","")
                              )))
                       .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));
    }

    @Override
    @After
    // We need to cleanup the authorities cache once than the configuration has been restored
    public void destroy() throws Exception {
        super.destroy();
        pluginService.clearNamedPluginClasses();
        cas.clearCache();
    }
}
