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
import java.util.Locale;
import java.util.UUID;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.matcher.AuthorityEntryMatcher;
import org.dspace.app.rest.repository.SubmissionFormRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authority.PersonAuthorityValue;
import org.dspace.authority.factory.AuthorityServiceFactory;
import org.dspace.content.authority.ChoiceAuthorityServiceImpl;
import org.dspace.content.authority.DCInputAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.core.LegacyPluginServiceImpl;
import org.dspace.core.service.PluginService;
import org.dspace.eperson.EPerson;
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
    @Autowired
    private SubmissionFormRestRepository submissionFormRestRepository;
    @Autowired
    private LegacyPluginServiceImpl legacyPluginService;
    @Autowired
    private ChoiceAuthorityServiceImpl choiceAuthorityServiceImpl;


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
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));
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

    @Test
    public void commonIsoLanguagesTest() throws Exception {
        context.turnOffAuthorisationSystem();

        String[] supportedLanguage = {"it","uk"};
        configurationService.setProperty("webui.supported.locales",supportedLanguage);
        DCInputAuthority.reset();
        legacyPluginService.clearNamedPluginClasses();
        choiceAuthorityServiceImpl.clearCache();

        Locale it = new Locale("it");
        Locale uk = new Locale("uk");
        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        // user select italian language
        getClient(tokenEPerson).perform(get("/api/integration/authorities/common_iso_languages/entries").locale(it)
                 .param("metadata","dc.language.iso"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                         AuthorityEntryMatcher.matchProperties("en_US", "Inglese (USA)","en_US"),
                         AuthorityEntryMatcher.matchProperties("es", "Spagnolo","es"),
                         AuthorityEntryMatcher.matchProperties("fr", "Francese","fr"),
                         AuthorityEntryMatcher.matchProperties("en", "Inglese","en"),
                         AuthorityEntryMatcher.matchProperties("de", "Tedesco","de"),
                         AuthorityEntryMatcher.matchProperties("zh", "Cinese","zh"),
                         AuthorityEntryMatcher.matchProperties("ja", "Giapponese","ja"),
                         AuthorityEntryMatcher.matchProperties("it", "Italiano","it"),
                         AuthorityEntryMatcher.matchProperties("uk", "Ucraino","uk"),
                         AuthorityEntryMatcher.matchProperties("pt", "Portogallo","pt"),
                         AuthorityEntryMatcher.matchProperties("other", "(Altro)","other"),
                         AuthorityEntryMatcher.matchProperties("", "N/A","")
                         )))
                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(12)));

        // user select ukranian language
        getClient(tokenEPerson).perform(get("/api/integration/authorities/common_iso_languages/entries").locale(uk)
                 .param("metadata","dc.language.iso"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                         AuthorityEntryMatcher.matchProperties("en_US", "Американська (USA)","en_US"),
                         AuthorityEntryMatcher.matchProperties("es", "Iспанська","es"),
                         AuthorityEntryMatcher.matchProperties("fr", "Французька","fr"),
                         AuthorityEntryMatcher.matchProperties("en", "Англiйська","en"),
                         AuthorityEntryMatcher.matchProperties("de", "Нiмецька","de"),
                         AuthorityEntryMatcher.matchProperties("zh", "Китайська","zh"),
                         AuthorityEntryMatcher.matchProperties("ja", "Японська","ja"),
                         AuthorityEntryMatcher.matchProperties("it", "Iталiйська","it"),
                         AuthorityEntryMatcher.matchProperties("uk", "Український","uk"),
                         AuthorityEntryMatcher.matchProperties("pt", "Португальська","pt"),
                         AuthorityEntryMatcher.matchProperties("other", "(Iнша)","other"),
                         AuthorityEntryMatcher.matchProperties("", "N/A","")
                         )))
                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(12)));

        configurationService.setProperty("webui.supported.locales",null);
        DCInputAuthority.reset();
        legacyPluginService.clearNamedPluginClasses();
        choiceAuthorityServiceImpl.clearCache();
    }

    @Test
    public void userWithPreferLanguageCommonIsoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        String[] supportedLanguage = {"it","uk"};
        configurationService.setProperty("webui.supported.locales",supportedLanguage);
        DCInputAuthority.reset();
        legacyPluginService.clearNamedPluginClasses();
        choiceAuthorityServiceImpl.clearCache();

        EPerson epersonIT = EPersonBuilder.createEPerson(context)
                           .withEmail("epersonIT@example.com")
                           .withPassword(password)
                           .withLanguage("it")
                           .build();

        EPerson epersonUK = EPersonBuilder.createEPerson(context)
                           .withEmail("epersonUK@example.com")
                           .withPassword(password)
                           .withLanguage("uk")
                           .build();

        context.restoreAuthSystemState();

        String tokenEPersonIT = getAuthToken(epersonIT.getEmail(), password);
        // user with italian prefer language
        getClient(tokenEPersonIT).perform(get("/api/integration/authorities/common_iso_languages/entries")
                 .param("metadata","dc.language.iso"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                         AuthorityEntryMatcher.matchProperties("en_US", "Inglese (USA)","en_US"),
                         AuthorityEntryMatcher.matchProperties("es", "Spagnolo","es"),
                         AuthorityEntryMatcher.matchProperties("fr", "Francese","fr"),
                         AuthorityEntryMatcher.matchProperties("en", "Inglese","en"),
                         AuthorityEntryMatcher.matchProperties("de", "Tedesco","de"),
                         AuthorityEntryMatcher.matchProperties("zh", "Cinese","zh"),
                         AuthorityEntryMatcher.matchProperties("ja", "Giapponese","ja"),
                         AuthorityEntryMatcher.matchProperties("it", "Italiano","it"),
                         AuthorityEntryMatcher.matchProperties("uk", "Ucraino","uk"),
                         AuthorityEntryMatcher.matchProperties("pt", "Portogallo","pt"),
                         AuthorityEntryMatcher.matchProperties("other", "(Altro)","other"),
                         AuthorityEntryMatcher.matchProperties("", "N/A","")
                         )))
                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(12)));

        String tokenEPersonUK = getAuthToken(epersonUK.getEmail(), password);
        // user with ukranian prefer language
        getClient(tokenEPersonUK).perform(get("/api/integration/authorities/common_iso_languages/entries")
                 .param("metadata","dc.language.iso"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                         AuthorityEntryMatcher.matchProperties("en_US", "Американська (USA)","en_US"),
                         AuthorityEntryMatcher.matchProperties("es", "Iспанська","es"),
                         AuthorityEntryMatcher.matchProperties("fr", "Французька","fr"),
                         AuthorityEntryMatcher.matchProperties("en", "Англiйська","en"),
                         AuthorityEntryMatcher.matchProperties("de", "Нiмецька","de"),
                         AuthorityEntryMatcher.matchProperties("zh", "Китайська","zh"),
                         AuthorityEntryMatcher.matchProperties("ja", "Японська","ja"),
                         AuthorityEntryMatcher.matchProperties("it", "Iталiйська","it"),
                         AuthorityEntryMatcher.matchProperties("uk", "Український","uk"),
                         AuthorityEntryMatcher.matchProperties("pt", "Португальська","pt"),
                         AuthorityEntryMatcher.matchProperties("other", "(Iнша)","other"),
                         AuthorityEntryMatcher.matchProperties("", "N/A","")
                         )))
                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(12)));

        configurationService.setProperty("webui.supported.locales",null);
        DCInputAuthority.reset();
        legacyPluginService.clearNamedPluginClasses();
        choiceAuthorityServiceImpl.clearCache();
    }

    @Test
    public void userChoiceItalianLanguageCommonIsoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        String[] supportedLanguage = {"it","uk"};
        configurationService.setProperty("webui.supported.locales",supportedLanguage);
        DCInputAuthority.reset();
        legacyPluginService.clearNamedPluginClasses();
        choiceAuthorityServiceImpl.clearCache();

        EPerson epersonUK = EPersonBuilder.createEPerson(context)
                           .withEmail("epersonUK@example.com")
                           .withPassword(password)
                           .withLanguage("uk")
                           .build();

        context.restoreAuthSystemState();

        String tokenEPersonUK = getAuthToken(epersonUK.getEmail(), password);
         // user prefer ukranian but choice italian language
        getClient(tokenEPersonUK).perform(get("/api/integration/authorities/common_iso_languages/entries")
                 .locale(new Locale("it"))
                 .param("metadata","dc.language.iso"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                         AuthorityEntryMatcher.matchProperties("en_US", "Inglese (USA)","en_US"),
                         AuthorityEntryMatcher.matchProperties("es", "Spagnolo","es"),
                         AuthorityEntryMatcher.matchProperties("fr", "Francese","fr"),
                         AuthorityEntryMatcher.matchProperties("en", "Inglese","en"),
                         AuthorityEntryMatcher.matchProperties("de", "Tedesco","de"),
                         AuthorityEntryMatcher.matchProperties("zh", "Cinese","zh"),
                         AuthorityEntryMatcher.matchProperties("ja", "Giapponese","ja"),
                         AuthorityEntryMatcher.matchProperties("it", "Italiano","it"),
                         AuthorityEntryMatcher.matchProperties("uk", "Ucraino","uk"),
                         AuthorityEntryMatcher.matchProperties("pt", "Portogallo","pt"),
                         AuthorityEntryMatcher.matchProperties("other", "(Altro)","other"),
                         AuthorityEntryMatcher.matchProperties("", "N/A","")
                         )))
                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(12)));

        configurationService.setProperty("webui.supported.locales",null);
        DCInputAuthority.reset();
        legacyPluginService.clearNamedPluginClasses();
        choiceAuthorityServiceImpl.clearCache();
    }

    @Test
    public void defaultLanguageCommonIsoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        String[] supportedLanguage = {"it","uk"};
        configurationService.setProperty("webui.supported.locales",supportedLanguage);
        configurationService.setProperty("default.locale","it");
        DCInputAuthority.reset();
        legacyPluginService.clearNamedPluginClasses();
        choiceAuthorityServiceImpl.clearCache();

        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        // user have not a preferred language and does not choose any language
        getClient(tokenEPerson).perform(get("/api/integration/authorities/common_iso_languages/entries")
                 .param("metadata","dc.language.iso"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                         AuthorityEntryMatcher.matchProperties("en_US", "Inglese (USA)","en_US"),
                         AuthorityEntryMatcher.matchProperties("es", "Spagnolo","es"),
                         AuthorityEntryMatcher.matchProperties("fr", "Francese","fr"),
                         AuthorityEntryMatcher.matchProperties("en", "Inglese","en"),
                         AuthorityEntryMatcher.matchProperties("de", "Tedesco","de"),
                         AuthorityEntryMatcher.matchProperties("zh", "Cinese","zh"),
                         AuthorityEntryMatcher.matchProperties("ja", "Giapponese","ja"),
                         AuthorityEntryMatcher.matchProperties("it", "Italiano","it"),
                         AuthorityEntryMatcher.matchProperties("uk", "Ucraino","uk"),
                         AuthorityEntryMatcher.matchProperties("pt", "Portogallo","pt"),
                         AuthorityEntryMatcher.matchProperties("other", "(Altro)","other"),
                         AuthorityEntryMatcher.matchProperties("", "N/A","")
                         )))
                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(12)));

        configurationService.setProperty("webui.supported.locales",null);
        DCInputAuthority.reset();
        legacyPluginService.clearNamedPluginClasses();
        choiceAuthorityServiceImpl.clearCache();
    }

    @Override
    public void destroy() throws Exception {
        AuthorityServiceFactory.getInstance().getAuthorityIndexingService().cleanIndex();
        super.destroy();
    }
}
