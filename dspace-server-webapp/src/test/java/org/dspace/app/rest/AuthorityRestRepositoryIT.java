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

import java.util.Date;
import java.util.UUID;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.dspace.app.rest.matcher.AuthorityEntryMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authority.PersonAuthorityValue;
import org.dspace.authority.factory.AuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class handles all Authority related IT. It alters some config to run the tests, but it gets cleared again
 * after every test
 */
public class AuthorityRestRepositoryIT extends AbstractControllerIntegrationTest {

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
    public void correctSrscQueryTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(
            get("/api/integration/authorities/srsc/entries")
                .param("metadata", "dc.subject")
                .param("query", "Research")
                .param("size", "1000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.totalElements", Matchers.is(26)));
    }

    @Test
    public void noResultsSrscQueryTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(
            get("/api/integration/authorities/srsc/entries")
                .param("metadata", "dc.subject")
                .param("query", "Research2")
                .param("size", "1000"))
                        .andExpect(status().isNoContent());
    }

    @Test
    @Ignore
    /**
     * This functionality is currently broken, it returns all 22 values
     */
    public void correctCommonTypesTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(
            get("/api/integration/authorities/common_types/entries")
                .param("metadata", "dc.type")
                .param("query", "Book")
                .param("size", "1000"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)));
    }

    @Test
    public void correctSolrQueryTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(
                get("/api/integration/authorities/SolrAuthorAuthority/entries")
                        .param("metadata", "dc.contributor.author")
                        .param("query", "Shirasaka")
                        .param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));
    }

    @Test
    public void noResultsSolrQueryTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(
                get("/api/integration/authorities/SolrAuthorAuthority/entries")
                        .param("metadata", "dc.contributor.author")
                        .param("query", "Smith")
                        .param("size", "1000"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void retrieveSrscValueTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        // When full projection is requested, response should include expected properties, links, and embeds.
        getClient(token).perform(
                get("/api/integration/authorities/srsc/entryValues/SCB1922").param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", AuthorityEntryMatcher.matchFullEmbeds()))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));
    }

    @Test
    public void noResultsSrscValueTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(
                get("/api/integration/authorities/srsc/entryValues/DOESNTEXIST"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void retrieveCommonTypesValueTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(
                get("/api/integration/authorities/common_types/entryValues/Book").param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
        ;

    }

    @Test
    public void retrieveCommonTypesWithSpaceValueTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(
                get("/api/integration/authorities/common_types/entryValues/Learning+Object"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));
    }

    @Test
    public void retrieveSolrValueTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        SolrQuery query = new SolrQuery();
        query.setQuery("*:*");
        QueryResponse queryResponse = AuthorityServiceFactory.getInstance().getAuthoritySearchService().search(query);
        String id = String.valueOf(queryResponse.getResults().get(0).getFieldValue("id"));

        getClient(token).perform(
                get("/api/integration/authorities/SolrAuthorAuthority/entryValues/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));
    }

    @Override
    public void destroy() throws Exception {
        AuthorityServiceFactory.getInstance().getAuthorityIndexingService().cleanIndex();
        super.destroy();
    }
}
