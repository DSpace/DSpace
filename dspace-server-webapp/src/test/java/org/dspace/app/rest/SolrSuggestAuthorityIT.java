/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.matcher.VocabularyMatcher.matchVocabularyEntry;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link org.dspace.content.authority.SolrSuggestAuthority}
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class SolrSuggestAuthorityIT extends AbstractControllerIntegrationTest {

    @Autowired
    private PluginService pluginService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    @Before
    public void setup() throws SubmissionConfigReaderException {
        choiceAuthorityService.getChoiceAuthoritiesNames(); // initialize the ChoiceAuthorityService
        context.turnOffAuthorisationSystem();
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
                new String[] { "org.dspace.content.authority.SolrSuggestAuthority = SolrSuggestSubjectAuthority" });

        configurationService.setProperty("choices.plugin.dc.subject", "SolrSuggestSubjectAuthority");
        configurationService.setProperty("choices.presentation.dc.subject", "suggest");
        configurationService.setProperty("authority.controlled.dc.subject", "true");
        configurationService.setProperty("SolrSuggestAuthority.dc_subject.facetname", "subject");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        context.restoreAuthSystemState();
    }

    @After
    public void teardown() throws SubmissionConfigReaderException {
        context.turnOffAuthorisationSystem();
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        context.restoreAuthSystemState();
    }

    @Test
    public void solrSuggestAuthorityNoSuggestionsTest() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/SolrSuggestSubjectAuthority/entries")
                        .param("filter", "test"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.entries").isEmpty())
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));
    }

    @Test
    public void solrSuggestAuthorityTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Test collection")
                                           .withEntityType("Publication")
                                           .build();

        ItemBuilder.createItem(context, col1)
                   .withTitle("Publication title 1")
                   .withSubject("test subject")
                   .build();

        ItemBuilder.createItem(context, col1)
                   .withTitle("Publication title 2")
                   .withSubject("subject test")
                   .build();

        context.restoreAuthSystemState();

        // expect to get only one value, because the control is based on the prefix
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/SolrSuggestSubjectAuthority/entries")
                        .param("filter", "test"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.entries", contains(
                                matchVocabularyEntry("test subject", "test subject", "vocabularyEntry"))))
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));
    }

    @Test
    public void solrSuggestAuthorityIsNotPrefixTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Test collection")
                                           .withEntityType("Publication")
                                           .build();

        ItemBuilder.createItem(context, col1)
                   .withTitle("Publication title 1")
                   .withSubject("first test subject")
                   .build();

        ItemBuilder.createItem(context, col1)
                   .withTitle("Publication title 2")
                   .withSubject("second subject test")
                   .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/SolrSuggestSubjectAuthority/entries")
                        .param("filter", "test"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.entries").isEmpty())
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));
    }

    @Test
    public void solrSuggestAuthorityPartOfPrefixTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Test collection")
                                           .withEntityType("Publication")
                                           .build();

        ItemBuilder.createItem(context, col1)
                   .withTitle("Publication title 1")
                   .withSubject("coaching")
                   .build();

        ItemBuilder.createItem(context, col1)
                   .withTitle("Publication title 2")
                   .withSubject("committed relationships")
                   .build();

        ItemBuilder.createItem(context, col1)
                   .withTitle("Publication title 3")
                   .withSubject("community support")
                   .build();

        ItemBuilder.createItem(context, col1)
                   .withTitle("Publication title 4")
                   .withSubject("Completed Project")
                   .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/SolrSuggestSubjectAuthority/entries")
                        .param("filter", "comm"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                          matchVocabularyEntry("committed relationships", "committed relationships", "vocabularyEntry"),
                          matchVocabularyEntry("community support", "community support", "vocabularyEntry")
                          )))
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)));

        getClient(token).perform(get("/api/submission/vocabularies/SolrSuggestSubjectAuthority/entries")
                        .param("filter", "co"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                          matchVocabularyEntry("coaching", "coaching", "vocabularyEntry"),
                          matchVocabularyEntry("Completed Project", "Completed Project", "vocabularyEntry"),
                          matchVocabularyEntry("community support", "community support", "vocabularyEntry"),
                          matchVocabularyEntry("committed relationships", "committed relationships", "vocabularyEntry")
                          )))
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(4)));
    }

    @Test
    public void solrSuggestAuthorityUnprocessableEntityTest() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/SolrSuggestSubjectAuthority/entries")
                        .param("filter", ""))
                        .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void solrSuggestAuthorityExactFilterTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Test collection")
                                           .withEntityType("Publication")
                                           .build();

        ItemBuilder.createItem(context, col1)
                   .withTitle("Publication title 1")
                   .withSubject("coaching")
                   .build();

        ItemBuilder.createItem(context, col1)
                   .withTitle("Publication title 2")
                   .withSubject("committed relationships")
                   .build();

        ItemBuilder.createItem(context, col1)
                   .withTitle("Publication title 3")
                   .withSubject("community support")
                   .build();

        ItemBuilder.createItem(context, col1)
                   .withTitle("Publication title 4")
                   .withSubject("Completed Project")
                   .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/SolrSuggestSubjectAuthority/entries")
                        .param("filter", "comm")
                        .param("exact", "true"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));

        getClient(token).perform(get("/api/submission/vocabularies/SolrSuggestSubjectAuthority/entries")
                        .param("filter", "committed")
                        .param("exact", "true"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));

        getClient(token).perform(get("/api/submission/vocabularies/SolrSuggestSubjectAuthority/entries")
                        .param("filter", "coaching")
                        .param("exact", "true"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.entries", contains(
                                   matchVocabularyEntry("coaching", "coaching", "vocabularyEntry")
                                   )))
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));
    }

}
