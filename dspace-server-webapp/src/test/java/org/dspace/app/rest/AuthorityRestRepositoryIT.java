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
import java.util.Locale;
import java.util.UUID;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.matcher.AuthorityEntryMatcher;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
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
public class AuthorityRestRepositoryIT extends AbstractEntityIntegrationTest {

    @Autowired
    ConfigurationService configurationService;
    @Autowired
    private PluginService pluginService;
    @Autowired
    private ChoiceAuthorityService cas;
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
    public void srscSearchTopTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/integration/authorities/srsc/entries/search/top"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
              AuthorityEntryMatcher.matchAuthority("SCB11", "HUMANITIES and RELIGION"),
              AuthorityEntryMatcher.matchAuthority("SCB12", "LAW/JURISPRUDENCE"),
              AuthorityEntryMatcher.matchAuthority("SCB13", "SOCIAL SCIENCES"),
              AuthorityEntryMatcher.matchAuthority("SCB14", "MATHEMATICS"),
              AuthorityEntryMatcher.matchAuthority("SCB15", "NATURAL SCIENCES"),
              AuthorityEntryMatcher.matchAuthority("SCB16", "TECHNOLOGY"),
              AuthorityEntryMatcher.matchAuthority("SCB17", "FORESTRY, AGRICULTURAL SCIENCES and LANDSCAPE PLANNING"),
              AuthorityEntryMatcher.matchAuthority("SCB18", "MEDICINE"),
              AuthorityEntryMatcher.matchAuthority("SCB19", "ODONTOLOGY"),
              AuthorityEntryMatcher.matchAuthority("SCB21", "PHARMACY"),
              AuthorityEntryMatcher.matchAuthority("SCB22", "VETERINARY MEDICINE"),
              AuthorityEntryMatcher.matchAuthority("SCB23", "INTERDISCIPLINARY RESEARCH AREAS")
              )))
          .andExpect(jsonPath("$.page.totalElements", Matchers.is(12)));
    }

    @Test
    public void srscSearchTopPaginationTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/integration/authorities/srsc/entries/search/top")
                             .param("page", "0")
                             .param("size", "5"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
              AuthorityEntryMatcher.matchAuthority("SCB11", "HUMANITIES and RELIGION"),
              AuthorityEntryMatcher.matchAuthority("SCB12", "LAW/JURISPRUDENCE"),
              AuthorityEntryMatcher.matchAuthority("SCB13", "SOCIAL SCIENCES"),
              AuthorityEntryMatcher.matchAuthority("SCB14", "MATHEMATICS"),
              AuthorityEntryMatcher.matchAuthority("SCB15", "NATURAL SCIENCES")
              )))
          .andExpect(jsonPath("$.page.totalElements", is(12)))
          .andExpect(jsonPath("$.page.totalPages", is(3)))
          .andExpect(jsonPath("$.page.number", is(0)));

        //second page
        getClient(tokenAdmin).perform(get("/api/integration/authorities/srsc/entries/search/top")
                 .param("page", "1")
                 .param("size", "5"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
               AuthorityEntryMatcher.matchAuthority("SCB16", "TECHNOLOGY"),
               AuthorityEntryMatcher.matchAuthority("SCB17", "FORESTRY, AGRICULTURAL SCIENCES and LANDSCAPE PLANNING"),
               AuthorityEntryMatcher.matchAuthority("SCB18", "MEDICINE"),
               AuthorityEntryMatcher.matchAuthority("SCB19", "ODONTOLOGY"),
               AuthorityEntryMatcher.matchAuthority("SCB21", "PHARMACY")
               )))
           .andExpect(jsonPath("$.page.totalElements", is(12)))
           .andExpect(jsonPath("$.page.totalPages", is(3)))
           .andExpect(jsonPath("$.page.number", is(1)));

        // third page
        getClient(tokenAdmin).perform(get("/api/integration/authorities/srsc/entries/search/top")
                 .param("page", "2")
                 .param("size", "5"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
               AuthorityEntryMatcher.matchAuthority("SCB22", "VETERINARY MEDICINE"),
               AuthorityEntryMatcher.matchAuthority("SCB23", "INTERDISCIPLINARY RESEARCH AREAS")
               )))
           .andExpect(jsonPath("$.page.totalElements", is(12)))
           .andExpect(jsonPath("$.page.totalPages", is(3)))
           .andExpect(jsonPath("$.page.number", is(2)));
    }

    @Test
    public void srscSearchByParentFirstLevel_MATHEMATICS_Test() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/integration/authorities/srsc/entries/search/byParent")
                 .param("id", "SCB14"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
                     AuthorityEntryMatcher.matchAuthority("SCB1401", "Algebra, geometry and mathematical analysis"),
                     AuthorityEntryMatcher.matchAuthority("SCB1402", "Applied mathematics"),
                     AuthorityEntryMatcher.matchAuthority("SCB1409", "Other mathematics")
                     )))
                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));
    }

    @Test
    public void srscSearchByParentFirstLevelPaginationTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        // first page
        getClient(tokenAdmin).perform(get("/api/integration/authorities/srsc/entries/search/byParent")
                 .param("id", "SCB14")
                 .param("page", "0")
                 .param("size", "2"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
                     AuthorityEntryMatcher.matchAuthority("SCB1401", "Algebra, geometry and mathematical analysis"),
                     AuthorityEntryMatcher.matchAuthority("SCB1402", "Applied mathematics")
                     )))
                 .andExpect(jsonPath("$.page.totalElements", is(3)))
                 .andExpect(jsonPath("$.page.totalPages", is(2)))
                 .andExpect(jsonPath("$.page.number", is(0)));

        // second page
        getClient(tokenAdmin).perform(get("/api/integration/authorities/srsc/entries/search/byParent")
                .param("id", "SCB14")
                .param("page", "1")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
                    AuthorityEntryMatcher.matchAuthority("SCB1409", "Other mathematics")
                    )))
                .andExpect(jsonPath("$.page.totalElements", is(3)))
                .andExpect(jsonPath("$.page.totalPages", is(2)))
                .andExpect(jsonPath("$.page.number", is(1)));
    }

    @Test
    public void srscSearchByParentSecondLevel_Applied_mathematics_Test() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/integration/authorities/srsc/entries/search/byParent")
                             .param("id", "SCB1402"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
                                     AuthorityEntryMatcher.matchAuthority("VR140202", "Numerical analysis"),
                                     AuthorityEntryMatcher.matchAuthority("VR140203", "Mathematical statistics"),
                                     AuthorityEntryMatcher.matchAuthority("VR140204", "Optimization, systems theory"),
                                     AuthorityEntryMatcher.matchAuthority("VR140205", "Theoretical computer science")
                                     )))
                             .andExpect(jsonPath("$.page.totalElements", Matchers.is(4)));
    }

    @Test
    public void srscSearchByParentEmptyTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/integration/authorities/srsc/entries/search/byParent")
                             .param("id", "VR140202"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));
    }

    @Test
    public void srscSearchByParentWrongIdTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/integration/authorities/srsc/entries/search/byParent")
                             .param("id", "WRONG_ID"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));
    }

    @Test
    public void srscSearchTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/integration/authorities/srsc/entries/search"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._links.byParent.href", Matchers.containsString(
                                                 "api/integration/authorities/srsc/entries/search/byParent")))
                             .andExpect(jsonPath("$._links.top.href", Matchers.containsString(
                                                 "api/integration/authorities/srsc/entries/search/top")));
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
        configurationService.setProperty("default.locale","en");
        DCInputAuthority.reset();
        legacyPluginService.clearNamedPluginClasses();
        choiceAuthorityServiceImpl.clearCache();
    }

    @Test
    public void srscSearchTopLanguageSupportTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String[] supportedLanguage = {"en","it","uk"};
        configurationService.setProperty("webui.supported.locales",supportedLanguage);
        legacyPluginService.clearNamedPluginClasses();
        choiceAuthorityServiceImpl.clearCache();

        Locale it = new Locale("it");
        Locale uk = new Locale("uk");
        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(get("/api/integration/authorities/srsc/entries/search/top").locale(it))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
             AuthorityEntryMatcher.matchAuthority("SCB11", "UMANITA e RELIGIONE"),
             AuthorityEntryMatcher.matchAuthority("SCB12", "DIRITTO/GIURISPRUDENZA"),
             AuthorityEntryMatcher.matchAuthority("SCB13", "SCIENZE SOCIALI"),
             AuthorityEntryMatcher.matchAuthority("SCB14", "MATEMATICA"),
             AuthorityEntryMatcher.matchAuthority("SCB15", "SCIENZE NATURALI"),
             AuthorityEntryMatcher.matchAuthority("SCB16", "TECNOLOGIA"),
             AuthorityEntryMatcher.matchAuthority("SCB17", "FORESTRE, SCIENZE AGRICOLE e PIANIFICAZIONE DEL PAESAGGIO"),
             AuthorityEntryMatcher.matchAuthority("SCB18", "MEDICINA"),
             AuthorityEntryMatcher.matchAuthority("SCB19", "ODONTOLOGIA"),
             AuthorityEntryMatcher.matchAuthority("SCB21", "FARMACIA"),
             AuthorityEntryMatcher.matchAuthority("SCB22", "MEDICINA VETERINARIA"),
             AuthorityEntryMatcher.matchAuthority("SCB23", "AREE DI RICERCA INTERDISCIPLINARI")
              )))
          .andExpect(jsonPath("$.page.totalElements", Matchers.is(12)));

        getClient(tokenEPerson).perform(get("/api/integration/authorities/srsc/entries/search/top").locale(uk))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
             AuthorityEntryMatcher.matchAuthority("SCB11", "ЛЮДИНА та РЕЛІГІЯ"),
             AuthorityEntryMatcher.matchAuthority("SCB12", "ЗАКОН/ЮРИСПРУДЕНЦIЯ"),
             AuthorityEntryMatcher.matchAuthority("SCB13", "СОЦІАЛЬНІ НАУКИ"),
             AuthorityEntryMatcher.matchAuthority("SCB14", "МАТЕМАТИКА"),
             AuthorityEntryMatcher.matchAuthority("SCB15", "ПРИРОДНІ НАУКИ"),
             AuthorityEntryMatcher.matchAuthority("SCB16", "ТЕХНОЛОГІЯ"),
             AuthorityEntryMatcher.matchAuthority("SCB17", "ЛIСОВЕ ГОСПОДАРСТВО,"
                                                + " СIЛЬСЬКОГОСПОДАРСЬКі НАУКИ ТА ПЛАНУВАННЯ ЗЕМЛЕРОБСТВА"),
             AuthorityEntryMatcher.matchAuthority("SCB18", "МЕДИЦИНА"),
             AuthorityEntryMatcher.matchAuthority("SCB19", "ОДОНТОЛОГІЯ"),
             AuthorityEntryMatcher.matchAuthority("SCB21", "ЛIКИ"),
             AuthorityEntryMatcher.matchAuthority("SCB22", "ВЕТЕРИНАРНА МЕДИЦИНА"),
             AuthorityEntryMatcher.matchAuthority("SCB23", "МІЖДИСЦІПЛІНАРНІ ДОСЛІДЖЕННЯ")
              )))
          .andExpect(jsonPath("$.page.totalElements", Matchers.is(12)));

        configurationService.setProperty("webui.supported.locales",null);
        legacyPluginService.clearNamedPluginClasses();
        choiceAuthorityServiceImpl.clearCache();
    }

    @Test
    public void srscSearchTopUnsupportedLocaleTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Locale kz = new Locale("kz");
        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        // if locale is not supported, return vocabulary with default language
        getClient(tokenEPerson).perform(get("/api/integration/authorities/srsc/entries/search/top").locale(kz))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
              AuthorityEntryMatcher.matchAuthority("SCB11", "HUMANITIES and RELIGION"),
              AuthorityEntryMatcher.matchAuthority("SCB12", "LAW/JURISPRUDENCE"),
              AuthorityEntryMatcher.matchAuthority("SCB13", "SOCIAL SCIENCES"),
              AuthorityEntryMatcher.matchAuthority("SCB14", "MATHEMATICS"),
              AuthorityEntryMatcher.matchAuthority("SCB15", "NATURAL SCIENCES"),
              AuthorityEntryMatcher.matchAuthority("SCB16", "TECHNOLOGY"),
              AuthorityEntryMatcher.matchAuthority("SCB17", "FORESTRY, AGRICULTURAL SCIENCES and LANDSCAPE PLANNING"),
              AuthorityEntryMatcher.matchAuthority("SCB18", "MEDICINE"),
              AuthorityEntryMatcher.matchAuthority("SCB19", "ODONTOLOGY"),
              AuthorityEntryMatcher.matchAuthority("SCB21", "PHARMACY"),
              AuthorityEntryMatcher.matchAuthority("SCB22", "VETERINARY MEDICINE"),
              AuthorityEntryMatcher.matchAuthority("SCB23", "INTERDISCIPLINARY RESEARCH AREAS")
              )))
          .andExpect(jsonPath("$.page.totalElements", Matchers.is(12)));
    }

    @Test
    public void srscSearchTopUserWithPreferLanguageTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String[] supportedLanguage = {"en","it","uk"};
        configurationService.setProperty("webui.supported.locales",supportedLanguage);
        legacyPluginService.clearNamedPluginClasses();
        choiceAuthorityServiceImpl.clearCache();

        Locale it = new Locale("it");

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
        String tokenEPersonUK = getAuthToken(epersonUK.getEmail(), password);

        getClient(tokenEPersonIT).perform(get("/api/integration/authorities/srsc/entries/search/top"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
             AuthorityEntryMatcher.matchAuthority("SCB11", "UMANITA e RELIGIONE"),
             AuthorityEntryMatcher.matchAuthority("SCB12", "DIRITTO/GIURISPRUDENZA"),
             AuthorityEntryMatcher.matchAuthority("SCB13", "SCIENZE SOCIALI"),
             AuthorityEntryMatcher.matchAuthority("SCB14", "MATEMATICA"),
             AuthorityEntryMatcher.matchAuthority("SCB15", "SCIENZE NATURALI"),
             AuthorityEntryMatcher.matchAuthority("SCB16", "TECNOLOGIA"),
             AuthorityEntryMatcher.matchAuthority("SCB17", "FORESTRE, SCIENZE AGRICOLE e PIANIFICAZIONE DEL PAESAGGIO"),
             AuthorityEntryMatcher.matchAuthority("SCB18", "MEDICINA"),
             AuthorityEntryMatcher.matchAuthority("SCB19", "ODONTOLOGIA"),
             AuthorityEntryMatcher.matchAuthority("SCB21", "FARMACIA"),
             AuthorityEntryMatcher.matchAuthority("SCB22", "MEDICINA VETERINARIA"),
             AuthorityEntryMatcher.matchAuthority("SCB23", "AREE DI RICERCA INTERDISCIPLINARI")
              )))
          .andExpect(jsonPath("$.page.totalElements", Matchers.is(12)));

        getClient(tokenEPersonUK).perform(get("/api/integration/authorities/srsc/entries/search/top"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
             AuthorityEntryMatcher.matchAuthority("SCB11", "ЛЮДИНА та РЕЛІГІЯ"),
             AuthorityEntryMatcher.matchAuthority("SCB12", "ЗАКОН/ЮРИСПРУДЕНЦIЯ"),
             AuthorityEntryMatcher.matchAuthority("SCB13", "СОЦІАЛЬНІ НАУКИ"),
             AuthorityEntryMatcher.matchAuthority("SCB14", "МАТЕМАТИКА"),
             AuthorityEntryMatcher.matchAuthority("SCB15", "ПРИРОДНІ НАУКИ"),
             AuthorityEntryMatcher.matchAuthority("SCB16", "ТЕХНОЛОГІЯ"),
             AuthorityEntryMatcher.matchAuthority("SCB17", "ЛIСОВЕ ГОСПОДАРСТВО,"
                                                + " СIЛЬСЬКОГОСПОДАРСЬКі НАУКИ ТА ПЛАНУВАННЯ ЗЕМЛЕРОБСТВА"),
             AuthorityEntryMatcher.matchAuthority("SCB18", "МЕДИЦИНА"),
             AuthorityEntryMatcher.matchAuthority("SCB19", "ОДОНТОЛОГІЯ"),
             AuthorityEntryMatcher.matchAuthority("SCB21", "ЛIКИ"),
             AuthorityEntryMatcher.matchAuthority("SCB22", "ВЕТЕРИНАРНА МЕДИЦИНА"),
             AuthorityEntryMatcher.matchAuthority("SCB23", "МІЖДИСЦІПЛІНАРНІ ДОСЛІДЖЕННЯ")
              )))
          .andExpect(jsonPath("$.page.totalElements", Matchers.is(12)));

        getClient(tokenEPersonUK).perform(get("/api/integration/authorities/srsc/entries/search/top").locale(it))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
           AuthorityEntryMatcher.matchAuthority("SCB11", "UMANITA e RELIGIONE"),
           AuthorityEntryMatcher.matchAuthority("SCB12", "DIRITTO/GIURISPRUDENZA"),
           AuthorityEntryMatcher.matchAuthority("SCB13", "SCIENZE SOCIALI"),
           AuthorityEntryMatcher.matchAuthority("SCB14", "MATEMATICA"),
           AuthorityEntryMatcher.matchAuthority("SCB15", "SCIENZE NATURALI"),
           AuthorityEntryMatcher.matchAuthority("SCB16", "TECNOLOGIA"),
           AuthorityEntryMatcher.matchAuthority("SCB17", "FORESTRE, SCIENZE AGRICOLE e PIANIFICAZIONE DEL PAESAGGIO"),
           AuthorityEntryMatcher.matchAuthority("SCB18", "MEDICINA"),
           AuthorityEntryMatcher.matchAuthority("SCB19", "ODONTOLOGIA"),
           AuthorityEntryMatcher.matchAuthority("SCB21", "FARMACIA"),
           AuthorityEntryMatcher.matchAuthority("SCB22", "MEDICINA VETERINARIA"),
           AuthorityEntryMatcher.matchAuthority("SCB23", "AREE DI RICERCA INTERDISCIPLINARI")
            )))
        .andExpect(jsonPath("$.page.totalElements", Matchers.is(12)));

        configurationService.setProperty("webui.supported.locales",null);
        legacyPluginService.clearNamedPluginClasses();
        choiceAuthorityServiceImpl.clearCache();
    }

    @Test
    public void srscSearchByParentFirstLevel_MATHEMATICS_SupportLanguageTest() throws Exception {
         context.turnOffAuthorisationSystem();
         String[] supportedLanguage = {"en","it","uk"};
         configurationService.setProperty("webui.supported.locales",supportedLanguage);
         legacyPluginService.clearNamedPluginClasses();
         choiceAuthorityServiceImpl.clearCache();

         Locale it = new Locale("it");
         Locale uk = new Locale("uk");
         context.restoreAuthSystemState();

         String tokenAdmin = getAuthToken(admin.getEmail(), password);
         getClient(tokenAdmin).perform(get("/api/integration/authorities/srsc/entries/search/byParent")
                 .locale(it)
                 .param("id", "SCB14"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
                     AuthorityEntryMatcher.matchAuthority("SCB1401", "Algebra, geometria e analisi matematica"),
                     AuthorityEntryMatcher.matchAuthority("SCB1402", "Matematica applicata"),
                     AuthorityEntryMatcher.matchAuthority("SCB1409", "Altra matematica")
                     )))
                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

         getClient(tokenAdmin).perform(get("/api/integration/authorities/srsc/entries/search/byParent")
                 .locale(uk)
                 .param("id", "SCB14"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
                     AuthorityEntryMatcher.matchAuthority("SCB1401", "Алгебра, геометрія та математичний аналіз"),
                     AuthorityEntryMatcher.matchAuthority("SCB1402", "Прикладна математика"),
                     AuthorityEntryMatcher.matchAuthority("SCB1409", "Інша математика")
                     )))
                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

         configurationService.setProperty("webui.supported.locales",null);
         legacyPluginService.clearNamedPluginClasses();
         choiceAuthorityServiceImpl.clearCache();
    }

    @Test
    public void testVcbSearchTopSupportLanguageTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String[] supportedLanguage = {"en","it"};
        configurationService.setProperty("webui.supported.locales",supportedLanguage);
        legacyPluginService.clearNamedPluginClasses();
        choiceAuthorityServiceImpl.clearCache();

        EPerson epersonIT = EPersonBuilder.createEPerson(context)
                           .withEmail("epersonIT@example.com")
                           .withPassword(password)
                           .withLanguage("it")
                           .build();

        Locale it = new Locale("it");
        Locale en = new Locale("en");
        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        String tokenEPersonIT = getAuthToken(epersonIT.getEmail(), password);

        // user explicitly chooses the English language
        getClient(tokenEPerson).perform(get("/api/integration/authorities/testVcb/entries/search/top")
                 .locale(en))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
                     AuthorityEntryMatcher.matchAuthority("TVC11", "LEVEL 1"),
                     AuthorityEntryMatcher.matchAuthority("TVC12", "LEVEL 2"),
                     AuthorityEntryMatcher.matchAuthority("TVC13", "LEVEL 3"))))
                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

        // user explicitly chooses the Italian language
        getClient(tokenEPerson).perform(get("/api/integration/authorities/testVcb/entries/search/top")
                 .locale(it))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
                     AuthorityEntryMatcher.matchAuthority("TVC11", "LIVELLO 1"),
                     AuthorityEntryMatcher.matchAuthority("TVC12", "LIVELLO 2"),
                     AuthorityEntryMatcher.matchAuthority("TVC13", "LIVELLO 3"))))
                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

        // user does not choose any language, so he is assigned the default one
        getClient(tokenEPerson).perform(get("/api/integration/authorities/testVcb/entries/search/top"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
                     AuthorityEntryMatcher.matchAuthority("TVC11", "LEVEL 1"),
                     AuthorityEntryMatcher.matchAuthority("TVC12", "LEVEL 2"),
                     AuthorityEntryMatcher.matchAuthority("TVC13", "LEVEL 3"))))
                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

        // user has as preference the Italian language
        getClient(tokenEPersonIT).perform(get("/api/integration/authorities/testVcb/entries/search/top"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
                     AuthorityEntryMatcher.matchAuthority("TVC11", "LIVELLO 1"),
                     AuthorityEntryMatcher.matchAuthority("TVC12", "LIVELLO 2"),
                     AuthorityEntryMatcher.matchAuthority("TVC13", "LIVELLO 3"))))
                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

        configurationService.setProperty("webui.supported.locales",null);
        legacyPluginService.clearNamedPluginClasses();
        choiceAuthorityServiceImpl.clearCache();
    }

    @Test
    public void testVcbSearchByParentSupportLanguageTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String[] supportedLanguage = {"en","it"};
        configurationService.setProperty("webui.supported.locales",supportedLanguage);
        legacyPluginService.clearNamedPluginClasses();
        choiceAuthorityServiceImpl.clearCache();

        EPerson epersonIT = EPersonBuilder.createEPerson(context)
                           .withEmail("epersonIT@example.com")
                           .withPassword(password)
                           .withLanguage("it")
                           .build();

        Locale it = new Locale("it");
        Locale en = new Locale("en");
        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        String tokenEPersonIT = getAuthToken(epersonIT.getEmail(), password);

        // user explicitly chooses the English language
        getClient(tokenEPerson).perform(get("/api/integration/authorities/testVcb/entries/search/byParent")
                 .locale(en)
                 .param("id", "TVC13"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
                         AuthorityEntryMatcher.matchAuthority("TVC131", "LEVEL 3.1"),
                         AuthorityEntryMatcher.matchAuthority("TVC132", "LEVEL 3.2"),
                         AuthorityEntryMatcher.matchAuthority("TVC133", "LEVEL 3.3"))))
                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

        // user explicitly chooses the Italian language
        getClient(tokenEPerson).perform(get("/api/integration/authorities/testVcb/entries/search/byParent")
                 .locale(it)
                 .param("id", "TVC13"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
                     AuthorityEntryMatcher.matchAuthority("TVC131", "LIVELLO 3.1"),
                     AuthorityEntryMatcher.matchAuthority("TVC132", "LIVELLO 3.2"),
                     AuthorityEntryMatcher.matchAuthority("TVC133", "LIVELLO 3.3"))))
                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

        // user does not choose any language, so he is assigned the default one
        getClient(tokenEPerson).perform(get("/api/integration/authorities/testVcb/entries/search/byParent")
                 .param("id", "TVC13"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
                     AuthorityEntryMatcher.matchAuthority("TVC131", "LEVEL 3.1"),
                     AuthorityEntryMatcher.matchAuthority("TVC132", "LEVEL 3.2"),
                     AuthorityEntryMatcher.matchAuthority("TVC133", "LEVEL 3.3"))))
                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

        // user has as preference the Italian language
        getClient(tokenEPersonIT).perform(get("/api/integration/authorities/testVcb/entries/search/byParent")
                 .param("id", "TVC13"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.containsInAnyOrder(
                     AuthorityEntryMatcher.matchAuthority("TVC131", "LIVELLO 3.1"),
                     AuthorityEntryMatcher.matchAuthority("TVC132", "LIVELLO 3.2"),
                     AuthorityEntryMatcher.matchAuthority("TVC133", "LIVELLO 3.3"))))
                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

        configurationService.setProperty("webui.supported.locales",null);
        legacyPluginService.clearNamedPluginClasses();
        choiceAuthorityServiceImpl.clearCache();
    }

    @Override
    public void destroy() throws Exception {
        AuthorityServiceFactory.getInstance().getAuthorityIndexingService().cleanIndex();
        super.destroy();
    }
}
