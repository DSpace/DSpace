/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.UUID;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.matcher.VocabularyMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authority.PersonAuthorityValue;
import org.dspace.authority.factory.AuthorityServiceFactory;
import org.dspace.content.Collection;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class handles all Authority related IT. It alters some config to run the tests, but it gets cleared again
 * after every test
 */
public class VocabularyRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    private PluginService pluginService;

    @Autowired
    private ChoiceAuthorityService cas;

    @Before
    public void setup() throws Exception {
        super.setUp();
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
                "org.dspace.content.authority.SolrAuthority = SolrAuthorAuthority");

        configurationService.setProperty("solr.authority.server",
                "${solr.server}/authority");
        configurationService.setProperty("choices.plugin.dc.contributor.author",
                "SolrAuthorAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author",
                "authorLookup");
        configurationService.setProperty("authority.controlled.dc.contributor.author",
                "true");

        configurationService.setProperty("authority.author.indexer.field.1",
                "dc.contributor.author");


        // These clears have to happen so that the config is actually reloaded in those classes. This is needed for
        // the properties that we're altering above and this is only used within the tests
        pluginService.clearNamedPluginClasses();
        cas.clearCache();

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("A parent community for all our test")
                .build();
        context.restoreAuthSystemState();
        PersonAuthorityValue person1 = new PersonAuthorityValue();
        person1.setId(String.valueOf(UUID.randomUUID()));
        person1.setLastName("Shirasaka");
        person1.setFirstName("Seiko");
        person1.setValue("Shirasaka, Seiko");
        person1.setField("dc_contributor_author");
        person1.setLastModified(new Date());
        person1.setCreationDate(new Date());
        AuthorityServiceFactory.getInstance().getAuthorityIndexingService().indexContent(person1);

        PersonAuthorityValue person2 = new PersonAuthorityValue();
        person2.setId(String.valueOf(UUID.randomUUID()));
        person2.setLastName("Miller");
        person2.setFirstName("Tyler E");
        person2.setValue("Miller, Tyler E");
        person2.setField("dc_contributor_author");
        person2.setLastModified(new Date());
        person2.setCreationDate(new Date());
        AuthorityServiceFactory.getInstance().getAuthorityIndexingService().indexContent(person2);

        AuthorityServiceFactory.getInstance().getAuthorityIndexingService().commit();
    }

    @Test
    public void findAllTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.vocabularies", Matchers.containsInAnyOrder(
                     VocabularyMatcher.matchProperties("srsc", "srsc", false, true),
                     VocabularyMatcher.matchProperties("common_types", "common_types", true, false),
                     VocabularyMatcher.matchProperties("common_iso_languages", "common_iso_languages", true , false),
                     VocabularyMatcher.matchProperties("SolrAuthorAuthority", "SolrAuthorAuthority", false , false)
                 )))
        .andExpect(jsonPath("$._links.self.href",
            Matchers.containsString("api/submission/vocabularies")))
        .andExpect(jsonPath("$.page.totalElements", is(4)));
    }

    @Test
    public void findOneSRSC_Test() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/srsc"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", is(
                            VocabularyMatcher.matchProperties("srsc", "srsc", false, true)
                        )));
    }

    @Test
    public void findOneCommonTypesTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/common_types"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", is(
                            VocabularyMatcher.matchProperties("common_types", "common_types", true, false)
                        )));
    }

    @Test
    public void correctSrscQueryTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Test collection")
                                                 .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(
            get("/api/submission/vocabularies/srsc/entries")
                .param("metadata", "dc.subject")
                .param("collection", collection.getID().toString())
                .param("filter", "Research")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.vocabularyEntries", Matchers.containsInAnyOrder(
                   VocabularyMatcher.matchVocabularyEntry("Family research",
                         "Research Subject Categories::SOCIAL SCIENCES::Social sciences::Social work::Family research",
                         "vocabularyEntry"),
                   VocabularyMatcher.matchVocabularyEntry("Youth research",
                         "Research Subject Categories::SOCIAL SCIENCES::Social sciences::Social work::Youth research",
                         "vocabularyEntry")
                   )))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)))
                .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
                .andExpect(jsonPath("$.page.size", Matchers.is(2)));
    }

    @Test
    public void controlledVocabularyEntriesWrongCollectionUUIDTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/srsc/entries")
                .param("metadata", "dc.subject")
                .param("collection", UUID.randomUUID().toString())
                .param("filter", "Research"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void controlledVocabularyEntriesMissingCollectionUUID_Test() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/srsc/entries")
                .param("metadata", "dc.subject")
                .param("filter", "Research"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void controlledVocabularyEntriesWrongMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Test collection")
                                                 .build();
        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/srsc/entries")
                .param("metadata", "dc.type")
                .param("collection", collection.getID().toString())
                .param("filter", "Research"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void notScrollableVocabularyRequiredQueryTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Test collection")
                                                 .build();
        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/srsc/entries")
                .param("metadata", "dc.not.existing")
                .param("collection", collection.getID().toString()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void noResultsSrscQueryTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Test collection")
                                                 .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(
            get("/api/submission/vocabularies/srsc/entries")
                .param("metadata", "dc.subject")
                .param("collection", collection.getID().toString())
                .param("filter", "Research2")
                .param("size", "1000"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));
    }

    @Test
    public void vocabularyEntriesCommon_typesTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Test collection")
                                                 .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/common_types/entries")
                .param("metadata", "dc.type")
                .param("collection", collection.getID().toString())
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.vocabularyEntries", Matchers.containsInAnyOrder(
                           VocabularyMatcher.matchVocabularyEntry("Animation", "Animation", "vocabularyEntry"),
                           VocabularyMatcher.matchVocabularyEntry("Article", "Article", "vocabularyEntry")
                           )))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(22)))
                .andExpect(jsonPath("$.page.totalPages", Matchers.is(11)))
                .andExpect(jsonPath("$.page.size", Matchers.is(2)));
    }

    @Test
    public void vocabularyEntriesCommon_typesWithQueryTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Test collection")
                                                 .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/common_types/entries")
                .param("metadata", "dc.type")
                .param("collection", collection.getID().toString())
                .param("filter", "Book")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.vocabularyEntries", Matchers.containsInAnyOrder(
                        VocabularyMatcher.matchVocabularyEntry("Book", "Book", "vocabularyEntry"),
                        VocabularyMatcher.matchVocabularyEntry("Book chapter", "Book chapter", "vocabularyEntry")
                        )))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)))
                .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
                .andExpect(jsonPath("$.page.size", Matchers.is(2)));
    }

    @Test
    public void correctSolrQueryTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Test collection")
                                                 .build();
        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(
                get("/api/submission/vocabularies/SolrAuthorAuthority/entries")
                        .param("metadata", "dc.contributor.author")
                        .param("collection", collection.getID().toString())
                        .param("filter", "Shirasaka")
                        .param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));
    }

    @Test
    public void noResultsSolrQueryTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Test collection")
                                                 .build();
        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(
                get("/api/submission/vocabularies/SolrAuthorAuthority/entries")
                        .param("metadata", "dc.contributor.author")
                        .param("collection", collection.getID().toString())
                        .param("filter", "Smith")
                        .param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));
    }

    @Test
    public void findByMetadataAndCollectionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Test collection")
                                                 .build();
        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/search/byMetadataAndCollection")
                        .param("metadata", "dc.type")
                        .param("collection", collection.getID().toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", is(
                            VocabularyMatcher.matchProperties("common_types", "common_types", true, false)
                        )));
    }

    @Test
    public void findByMetadataAndCollectionUnprocessableEntityTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Test collection")
                                                 .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/search/byMetadataAndCollection")
                        .param("metadata", "dc.not.exist")
                        .param("collection", collection.getID().toString()))
                        .andExpect(status().isUnprocessableEntity());

        getClient(token).perform(get("/api/submission/vocabularies/search/byMetadataAndCollection")
                .param("metadata", "dc.type")
                .param("collection", UUID.randomUUID().toString()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void findByMetadataAndCollectionBadRequestTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Test collection")
                                                 .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        //missing metadata
        getClient(token).perform(get("/api/submission/vocabularies/search/byMetadataAndCollection")
                        .param("collection", collection.getID().toString()))
                        .andExpect(status().isBadRequest());

        //missing collection
        getClient(token).perform(get("/api/submission/vocabularies/search/byMetadataAndCollection")
                .param("metadata", "dc.type"))
                .andExpect(status().isBadRequest());
    }
}
