/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;

import org.dspace.app.rest.matcher.SubmissionFormFieldMatcher;
import org.dspace.app.rest.repository.SubmissionFormRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.builder.EPersonBuilder;
import org.dspace.content.authority.DCInputAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.core.service.PluginService;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test to test the /api/config/submissionforms endpoint
 * (Class has to start or end with IT to be picked up by the failsafe plugin)
 */
public class SubmissionFormsControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private SubmissionFormRestRepository submissionFormRestRepository;
    @Autowired
    private PluginService pluginService;
    @Autowired
    private ChoiceAuthorityService cas;

    @Test
    public void findAll() throws Exception {
        //When we call the root endpoint as anonymous user
        getClient().perform(get("/api/config/submissionforms"))
                   //The status has to be 403 Not Authorized
                   .andExpect(status().isUnauthorized());


        String token = getAuthToken(admin.getEmail(), password);

        //When we call the root endpoint
        getClient(token).perform(get("/api/config/submissionforms"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))
                   //The configuration file for the test env includes 6 forms
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", equalTo(8)))
                   .andExpect(jsonPath("$.page.totalPages", equalTo(1)))
                   .andExpect(jsonPath("$.page.number", is(0)))
                   .andExpect(
                       jsonPath("$._links.self.href", Matchers.startsWith(REST_SERVER_URL + "config/submissionforms")))
                   //The array of submissionforms should have a size of 8
                   .andExpect(jsonPath("$._embedded.submissionforms", hasSize(equalTo(8))))
        ;
    }

    @Test
    public void findAllWithNewlyCreatedAccountTest() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/config/submissionforms"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", equalTo(8)))
                .andExpect(jsonPath("$.page.totalPages", equalTo(1)))
                .andExpect(jsonPath("$.page.number", is(0)))
                .andExpect(jsonPath("$._links.self.href", Matchers.startsWith(REST_SERVER_URL
                           + "config/submissionforms")))
                .andExpect(jsonPath("$._embedded.submissionforms", hasSize(equalTo(8))));
    }

    @Test
    public void findTraditionalPageOne() throws Exception {
        //When we call the root endpoint as anonymous user
        getClient().perform(get("/api/config/submissionforms/traditionalpageone"))
                   //The status has to be 403 Not Authorized
                   .andExpect(status().isUnauthorized());

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/config/submissionforms/traditionalpageone"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))
                   //Check that the JSON root matches the expected "traditionalpageone" input forms
                   .andExpect(jsonPath("$.id", is("traditionalpageone")))
                   .andExpect(jsonPath("$.name", is("traditionalpageone")))
                   .andExpect(jsonPath("$.type", is("submissionform")))
                   .andExpect(jsonPath("$._links.self.href", Matchers
                       .startsWith(REST_SERVER_URL + "config/submissionforms/traditionalpageone")))
                   // check the first two rows
                   .andExpect(jsonPath("$.rows[0].fields", contains(
                        SubmissionFormFieldMatcher.matchFormFieldDefinition("name", "Author", null,
                null, true,"Add an author", "dc.contributor.author"))))
                   .andExpect(jsonPath("$.rows[1].fields", contains(
                        SubmissionFormFieldMatcher.matchFormFieldDefinition("onebox", "Title", null,
                                "You must enter a main title for this item.", false,
                                "Enter the main title of the item.", "dc.title"))))
                   // check a row with multiple fields
                   .andExpect(jsonPath("$.rows[3].fields",
                        contains(
                                SubmissionFormFieldMatcher.matchFormFieldDefinition("date", "Date of Issue",
                                        null, "You must enter at least the year.", false,
                                        "Please give the date", "col-sm-4",
                                        "dc.date.issued"),
                                SubmissionFormFieldMatcher.matchFormFieldDefinition("onebox", "Publisher", null,
                                        null, false,"Enter the name of",
                                        "col-sm-8","dc.publisher"))))
        ;
    }

    @Test
    public void findTraditionalPageOneWithNewlyCreatedAccountTest() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/config/submissionforms/traditionalpageone"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.id", is("traditionalpageone")))
                   .andExpect(jsonPath("$.name", is("traditionalpageone")))
                   .andExpect(jsonPath("$.type", is("submissionform")))
                   .andExpect(jsonPath("$._links.self.href", Matchers
                       .startsWith(REST_SERVER_URL + "config/submissionforms/traditionalpageone")))
                   .andExpect(jsonPath("$.rows[0].fields", contains(
                        SubmissionFormFieldMatcher.matchFormFieldDefinition("name", "Author", null,
                          null, true,"Add an author", "dc.contributor.author"))))
                   .andExpect(jsonPath("$.rows[1].fields", contains(
                        SubmissionFormFieldMatcher.matchFormFieldDefinition("onebox", "Title", null,
                                "You must enter a main title for this item.", false,
                                "Enter the main title of the item.", "dc.title"))))
                   .andExpect(jsonPath("$.rows[3].fields",contains(
                                SubmissionFormFieldMatcher.matchFormFieldDefinition("date", "Date of Issue", null,
                                        "You must enter at least the year.", false,
                                        "Please give the date", "col-sm-4",
                                        "dc.date.issued"),
                                SubmissionFormFieldMatcher.matchFormFieldDefinition("onebox", "Publisher", null,
                                        null, false,"Enter the name of",
                                        "col-sm-8","dc.publisher"))));
    }

    @Test
    public void findFieldWithAuthorityConfig() throws Exception {
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
                new String[] {
                    "org.dspace.content.authority.SolrAuthority = SolrAuthorAuthority",
                    "org.dspace.content.authority.SolrAuthority = SolrEditorAuthority",
                    "org.dspace.content.authority.SolrAuthority = SolrSubjectAuthority"
                });

        configurationService.setProperty("solr.authority.server",
                "${solr.server}/authority");
        configurationService.setProperty("choices.plugin.dc.contributor.author",
                "SolrAuthorAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author",
                "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author",
                "true");
        configurationService.setProperty("authority.author.indexer.field.1",
                "dc.contributor.author");
        configurationService.setProperty("choices.plugin.dc.contributor.editor",
                "SolrEditorAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.editor",
                "authorLookup");
        configurationService.setProperty("authority.controlled.dc.contributor.editor",
                "true");
        configurationService.setProperty("authority.author.indexer.field.2",
                "dc.contributor.editor");
        configurationService.setProperty("choices.plugin.dc.subject",
                "SolrSubjectAuthority");
        configurationService.setProperty("choices.presentation.dc.subject",
                "lookup");
        configurationService.setProperty("authority.controlled.dc.subject",
                "true");
        configurationService.setProperty("authority.author.indexer.field.3",
                "dc.subject");

        // These clears have to happen so that the config is actually reloaded in those classes. This is needed for
        // the properties that we're altering above and this is only used within the tests
        submissionFormRestRepository.reload();
        DCInputAuthority.reset();
        pluginService.clearNamedPluginClasses();
        cas.clearCache();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/config/submissionforms/sampleauthority"))
                        //The status has to be 200 OK
                        .andExpect(status().isOk())
                        //We expect the content type to be "application/hal+json;charset=UTF-8"
                        .andExpect(content().contentType(contentType))
                        //Check that the JSON root matches the expected "sampleauthority" input forms
                        .andExpect(jsonPath("$.id", is("sampleauthority")))
                        .andExpect(jsonPath("$.name", is("sampleauthority")))
                        .andExpect(jsonPath("$.type", is("submissionform")))
                        .andExpect(jsonPath("$._links.self.href", Matchers
                            .startsWith(REST_SERVER_URL + "config/submissionforms/sampleauthority")))
                        // our test configuration include the dc.contributor.author, dc.contributor.editor and
                        // dc.subject fields with in separate rows all linked to an authority with different
                        // presentation modes (suggestion, name-lookup, lookup)
                        .andExpect(jsonPath("$.rows[0].fields", contains(
                                SubmissionFormFieldMatcher.matchFormFieldDefinition("onebox", "Author", null,
                                        null, true,
                                "Author field that can be associated with an authority providing suggestion",
                                null, "dc.contributor.author", "SolrAuthorAuthority")
                            )))
                        .andExpect(jsonPath("$.rows[1].fields", contains(
                                SubmissionFormFieldMatcher.matchFormFieldDefinition("lookup-name", "Editor", null,
                                        null, false,
                                "Editor field that can be associated with an authority "
                                + "providing the special name lookup",
                                null, "dc.contributor.editor", "SolrEditorAuthority")
                            )))
                        .andExpect(jsonPath("$.rows[2].fields", contains(
                                SubmissionFormFieldMatcher.matchFormFieldDefinition("lookup", "Subject", null,
                                        null, true,
                                "Subject field that can be associated with an authority providing lookup",
                                null, "dc.subject", "SolrSubjectAuthority")
                            )))
                        ;
        // we need to force a reload of the config now to be able to reload also the cache of the other
        // authority related services. As this is needed just by this test method it is more efficient do it
        // here instead that force these reload for each method extending the destroy method
        configurationService.reloadConfig();
        submissionFormRestRepository.reload();
        DCInputAuthority.reset();
        pluginService.clearNamedPluginClasses();
        cas.clearCache();
    }

    @Test
    public void findFieldWithValuePairsConfig() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/config/submissionforms/traditionalpageone"))
                        //The status has to be 200 OK
                        .andExpect(status().isOk())
                        //We expect the content type to be "application/hal+json;charset=UTF-8"
                        .andExpect(content().contentType(contentType))
                        //Check that the JSON root matches the expected "traditionalpageone" input forms
                        .andExpect(jsonPath("$.id", is("traditionalpageone")))
                        .andExpect(jsonPath("$.name", is("traditionalpageone")))
                        .andExpect(jsonPath("$.type", is("submissionform")))
                        .andExpect(jsonPath("$._links.self.href", Matchers
                            .startsWith(REST_SERVER_URL + "config/submissionforms/traditionalpageone")))
                        // our test configuration include the dc.type field with a value pair in the 8th row
                        .andExpect(jsonPath("$.rows[7].fields", contains(
                                SubmissionFormFieldMatcher.matchFormFieldDefinition("dropdown", "Type", null,
                                        null, true,
                                "Select the type(s) of content of the item. To select more than one value in the " +
                                "list, you may have to hold down the \"CTRL\" or \"Shift\" key.",
                                null, "dc.type", "common_types")
                            )))
        ;
    }

    @Test
    public void findFieldWithTypeBindConfig() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/config/submissionforms/traditionalpageone"))
                // The status has to be 200 OK
                .andExpect(status().isOk())
                // We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                // Check that the JSON root matches the expected "traditionalpageone" input forms
                .andExpect(jsonPath("$.id", is("traditionalpageone")))
                .andExpect(jsonPath("$.name", is("traditionalpageone")))
                .andExpect(jsonPath("$.type", is("submissionform")))
                .andExpect(jsonPath("$._links.self.href", Matchers
                        .startsWith(REST_SERVER_URL + "config/submissionforms/traditionalpageone")))
                // check a row with type-bind 'Technical Report'
                .andExpect(jsonPath("$.rows[5].fields", contains(
                SubmissionFormFieldMatcher.matchFormFieldDefinition("series", "Series/Report No.",
                        "Technical Report", null, true,
                        "Enter the series and number assigned to this item by your community.",
                        "dc.relation.ispartofseries"))))
                // check the same row with a NON-matching type-bind 'Article' (expect false)
                .andExpect(((jsonPath("$.rows[5].fields", not(contains(
                SubmissionFormFieldMatcher.matchFormFieldDefinition("series", "Series/Report No.",
                        "Article", null, true,
                        "Enter the series and number assigned to this item by your community.",
                        "dc.relation.ispartofseries")))))));
    }

    @Test
    public void findOpenRelationshipConfig() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/config/submissionforms/traditionalpageone"))
                        //The status has to be 200 OK
                        .andExpect(status().isOk())
                        //We expect the content type to be "application/hal+json;charset=UTF-8"
                        .andExpect(content().contentType(contentType))
                        //Check that the JSON root matches the expected "traditionalpageone" input forms
                        .andExpect(jsonPath("$.id", is("traditionalpageone")))
                        .andExpect(jsonPath("$.name", is("traditionalpageone")))
                        .andExpect(jsonPath("$.type", is("submissionform")))
                        .andExpect(jsonPath("$._links.self.href", Matchers
                            .startsWith(REST_SERVER_URL + "config/submissionforms/traditionalpageone")))
                        // check the first two rows
                        .andExpect(jsonPath("$.rows[0].fields", contains(
                            SubmissionFormFieldMatcher.matchFormOpenRelationshipFieldDefinition("name",
                        "Author", null, true,"Add an author",
                    "dc.contributor.author", "isAuthorOfPublication", null,
            "person", true))))
        ;
    }

    @Test
    public void findClosedRelationshipConfig() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/config/submissionforms/journalVolumeStep"))
                        //The status has to be 200 OK
                        .andExpect(status().isOk())
                        //We expect the content type to be "application/hal+json;charset=UTF-8"
                        .andExpect(content().contentType(contentType))
                        //Check that the JSON root matches the expected "traditionalpageone" input forms
                        .andExpect(jsonPath("$.id", is("journalVolumeStep")))
                        .andExpect(jsonPath("$.name", is("journalVolumeStep")))
                        .andExpect(jsonPath("$.type", is("submissionform")))
                        .andExpect(jsonPath("$._links.self.href", Matchers
                            .startsWith(REST_SERVER_URL + "config/submissionforms/journalVolumeStep")))
                        // check the first two rows
                        .andExpect(jsonPath("$.rows[0].fields", contains(
                            SubmissionFormFieldMatcher.matchFormClosedRelationshipFieldDefinition("Journal", null,
                    false,"Select the journal related to this volume.", "isJournalOfVolume",
                        "creativework.publisher:somepublishername", "periodical", false))))
        ;
    }

    @Test
    public void languageSupportTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String[] supportedLanguage = {"it","uk"};
        configurationService.setProperty("default.locale","it");
        configurationService.setProperty("webui.supported.locales",supportedLanguage);
        // These clears have to happen so that the config is actually reloaded in those classes. This is needed for
        // the properties that we're altering above and this is only used within the tests
        submissionFormRestRepository.reload();
        DCInputAuthority.reset();
        pluginService.clearNamedPluginClasses();
        cas.clearCache();

        Locale uk = new Locale("uk");
        Locale it = new Locale("it");

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);

        // user select italian language
        getClient(tokenEperson).perform(get("/api/config/submissionforms/languagetest").locale(it))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$.id", is("languagetest")))
                 .andExpect(jsonPath("$.name", is("languagetest")))
                 .andExpect(jsonPath("$.type", is("submissionform")))
                 .andExpect(jsonPath("$._links.self.href", Matchers
                            .startsWith(REST_SERVER_URL + "config/submissionforms/languagetest")))
                 .andExpect(jsonPath("$.rows[0].fields", contains(SubmissionFormFieldMatcher
                            .matchFormFieldDefinition("name", "Autore", null,
                                    "\u00C8" + " richiesto almeno un autore", true,
                                                      "Aggiungi un autore", "dc.contributor.author"))))
                 .andExpect(jsonPath("$.rows[1].fields", contains(SubmissionFormFieldMatcher
                            .matchFormFieldDefinition("onebox", "Titolo", null,
                            "\u00C8" + " necessario inserire un titolo principale per questo item", false,
                            "Inserisci titolo principale di questo item", "dc.title"))))
                 .andExpect(jsonPath("$.rows[2].fields", contains(SubmissionFormFieldMatcher
                            .matchFormFieldDefinition("dropdown", "Lingua", null, null, false,
                            "Selezionare la lingua del contenuto principale dell'item."
                          + " Se la lingua non compare nell'elenco, selezionare (Altro)."
                          + " Se il contenuto non ha davvero una lingua"
                          + " (ad esempio, se è un set di dati o un'immagine) selezionare (N/A)",
                          null, "dc.language.iso", "common_iso_languages"))));

        // user select ukranian language
        getClient(tokenEperson).perform(get("/api/config/submissionforms/languagetest").locale(uk))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$.id", is("languagetest")))
                 .andExpect(jsonPath("$.name", is("languagetest")))
                 .andExpect(jsonPath("$.type", is("submissionform")))
                 .andExpect(jsonPath("$._links.self.href", Matchers
                           .startsWith(REST_SERVER_URL + "config/submissionforms/languagetest")))
                 .andExpect(jsonPath("$.rows[0].fields", contains(SubmissionFormFieldMatcher
                           .matchFormFieldDefinition("name", "Автор", null, "Потрібно ввести хочаб одного автора!",
                                               true, "Додати автора", "dc.contributor.author"))))
                 .andExpect(jsonPath("$.rows[1].fields", contains(SubmissionFormFieldMatcher
                           .matchFormFieldDefinition("onebox", "Заголовок", null,
                           "Заговолок файла обов'язковий !", false,
                           "Ввести основний заголовок файла", "dc.title"))))
                 .andExpect(jsonPath("$.rows[2].fields", contains(SubmissionFormFieldMatcher
                           .matchFormFieldDefinition("dropdown", "Мова", null, null, false,
                           "Виберiть мову головного змiсту файлу, як що мови немає у списку, вибрати (Iнша)."
                         + " Як що вмiст вайлу не є текстовим, наприклад є фотографiєю, тодi вибрати (N/A)",
                         null, "dc.language.iso", "common_iso_languages"))));
         resetLocalesConfiguration();
    }

    @Test
    public void preferLanguageTest() throws Exception {
        context.turnOffAuthorisationSystem();

        String[] supportedLanguage = {"it","uk"};
        configurationService.setProperty("default.locale","it");
        configurationService.setProperty("webui.supported.locales",supportedLanguage);
        // These clears have to happen so that the config is actually reloaded in those classes. This is needed for
        // the properties that we're altering above and this is only used within the tests
        submissionFormRestRepository.reload();
        DCInputAuthority.reset();
        pluginService.clearNamedPluginClasses();
        cas.clearCache();

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

        String tokenEpersonIT = getAuthToken(epersonIT.getEmail(), password);
        String tokenEpersonUK = getAuthToken(epersonUK.getEmail(), password);

        // user with italian prefer language
        getClient(tokenEpersonIT).perform(get("/api/config/submissionforms/languagetest"))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$.id", is("languagetest")))
                 .andExpect(jsonPath("$.name", is("languagetest")))
                 .andExpect(jsonPath("$.type", is("submissionform")))
                 .andExpect(jsonPath("$._links.self.href", Matchers
                            .startsWith(REST_SERVER_URL + "config/submissionforms/languagetest")))
                 .andExpect(jsonPath("$.rows[0].fields", contains(SubmissionFormFieldMatcher
                            .matchFormFieldDefinition("name", "Autore", null,
                                    "\u00C8" + " richiesto almeno un autore", true,
                                                      "Aggiungi un autore", "dc.contributor.author"))))
                 .andExpect(jsonPath("$.rows[1].fields", contains(SubmissionFormFieldMatcher
                            .matchFormFieldDefinition("onebox", "Titolo", null,
                            "\u00C8" + " necessario inserire un titolo principale per questo item", false,
                            "Inserisci titolo principale di questo item", "dc.title"))))
                 .andExpect(jsonPath("$.rows[2].fields", contains(SubmissionFormFieldMatcher
                            .matchFormFieldDefinition("dropdown", "Lingua", null, null, false,
                            "Selezionare la lingua del contenuto principale dell'item."
                          + " Se la lingua non compare nell'elenco, selezionare (Altro)."
                          + " Se il contenuto non ha davvero una lingua"
                          + " (ad esempio, se è un set di dati o un'immagine) selezionare (N/A)",
                          null, "dc.language.iso", "common_iso_languages"))));

        // user with ukranian prefer language
        getClient(tokenEpersonUK).perform(get("/api/config/submissionforms/languagetest"))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$.id", is("languagetest")))
                 .andExpect(jsonPath("$.name", is("languagetest")))
                 .andExpect(jsonPath("$.type", is("submissionform")))
                 .andExpect(jsonPath("$._links.self.href", Matchers
                           .startsWith(REST_SERVER_URL + "config/submissionforms/languagetest")))
                 .andExpect(jsonPath("$.rows[0].fields", contains(SubmissionFormFieldMatcher
                           .matchFormFieldDefinition("name",  "Автор", null, "Потрібно ввести хочаб одного автора!",
                                               true, "Додати автора", "dc.contributor.author"))))
                 .andExpect(jsonPath("$.rows[1].fields", contains(SubmissionFormFieldMatcher
                           .matchFormFieldDefinition("onebox", "Заголовок", null,
                           "Заговолок файла обов'язковий !", false,
                           "Ввести основний заголовок файла", "dc.title"))))
                 .andExpect(jsonPath("$.rows[2].fields", contains(SubmissionFormFieldMatcher
                           .matchFormFieldDefinition("dropdown", "Мова", null, null, false,
                           "Виберiть мову головного змiсту файлу, як що мови немає у списку, вибрати (Iнша)."
                         + " Як що вмiст вайлу не є текстовим, наприклад є фотографiєю, тодi вибрати (N/A)",
                         null, "dc.language.iso", "common_iso_languages"))));
         resetLocalesConfiguration();
    }

    @Test
    public void userChoiceAnotherLanguageTest() throws Exception {
        context.turnOffAuthorisationSystem();

        String[] supportedLanguage = {"it","uk"};
        configurationService.setProperty("default.locale","it");
        configurationService.setProperty("webui.supported.locales",supportedLanguage);
        // These clears have to happen so that the config is actually reloaded in those classes. This is needed for
        // the properties that we're altering above and this is only used within the tests
        submissionFormRestRepository.reload();
        DCInputAuthority.reset();
        pluginService.clearNamedPluginClasses();
        cas.clearCache();

        Locale it = new Locale("it");

        EPerson epersonUK = EPersonBuilder.createEPerson(context)
                           .withEmail("epersonUK@example.com")
                           .withPassword(password)
                           .withLanguage("uk")
                           .build();

        context.restoreAuthSystemState();

        String tokenEpersonUK = getAuthToken(epersonUK.getEmail(), password);

        // user prefer ukranian but choice italian language
        getClient(tokenEpersonUK).perform(get("/api/config/submissionforms/languagetest").locale(it))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$.id", is("languagetest")))
                 .andExpect(jsonPath("$.name", is("languagetest")))
                 .andExpect(jsonPath("$.type", is("submissionform")))
                 .andExpect(jsonPath("$._links.self.href", Matchers
                           .startsWith(REST_SERVER_URL + "config/submissionforms/languagetest")))
                 .andExpect(jsonPath("$.rows[0].fields", contains(SubmissionFormFieldMatcher
                           .matchFormFieldDefinition("name", "Autore", null,
                                   "\u00C8" + " richiesto almeno un autore", true,
                                                     "Aggiungi un autore", "dc.contributor.author"))))
                 .andExpect(jsonPath("$.rows[1].fields", contains(SubmissionFormFieldMatcher
                           .matchFormFieldDefinition("onebox", "Titolo", null,
                           "\u00C8" + " necessario inserire un titolo principale per questo item", false,
                           "Inserisci titolo principale di questo item", "dc.title"))))
                 .andExpect(jsonPath("$.rows[2].fields", contains(SubmissionFormFieldMatcher
                           .matchFormFieldDefinition("dropdown", "Lingua", null, null, false,
                           "Selezionare la lingua del contenuto principale dell'item."
                         + " Se la lingua non compare nell'elenco, selezionare (Altro)."
                         + " Se il contenuto non ha davvero una lingua"
                         + " (ad esempio, se è un set di dati o un'immagine) selezionare (N/A)",
                         null, "dc.language.iso", "common_iso_languages"))));
         resetLocalesConfiguration();
    }

    @Test
    public void defaultLanguageTest() throws Exception {
        context.turnOffAuthorisationSystem();

        String[] supportedLanguage = {"it","uk"};
        configurationService.setProperty("default.locale","it");
        configurationService.setProperty("webui.supported.locales",supportedLanguage);
        // These clears have to happen so that the config is actually reloaded in those classes. This is needed for
        // the properties that we're altering above and this is only used within the tests
        submissionFormRestRepository.reload();
        DCInputAuthority.reset();
        pluginService.clearNamedPluginClasses();
        cas.clearCache();

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/config/submissionforms/languagetest"))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$.id", is("languagetest")))
                 .andExpect(jsonPath("$.name", is("languagetest")))
                 .andExpect(jsonPath("$.type", is("submissionform")))
                 .andExpect(jsonPath("$._links.self.href", Matchers
                            .startsWith(REST_SERVER_URL + "config/submissionforms/languagetest")))
                 .andExpect(jsonPath("$.rows[0].fields", contains(SubmissionFormFieldMatcher
                            .matchFormFieldDefinition("name", "Autore", null, "\u00C8 richiesto almeno un autore", true,
                                                      "Aggiungi un autore", "dc.contributor.author"))))
                 .andExpect(jsonPath("$.rows[1].fields", contains(SubmissionFormFieldMatcher
                            .matchFormFieldDefinition("onebox", "Titolo", null,
                            "\u00C8 necessario inserire un titolo principale per questo item", false,
                            "Inserisci titolo principale di questo item", "dc.title"))));
         resetLocalesConfiguration();
    }

    @Test
    public void supportLanguageUsingMultipleLocaleTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String[] supportedLanguage = {"it","uk","en"};
        configurationService.setProperty("default.locale","en");
        configurationService.setProperty("webui.supported.locales",supportedLanguage);
        // These clears have to happen so that the config is actually reloaded in those classes. This is needed for
        // the properties that we're altering above and this is only used within the tests
        submissionFormRestRepository.reload();
        DCInputAuthority.reset();
        pluginService.clearNamedPluginClasses();
        cas.clearCache();

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/config/submissionforms/languagetest")
                 .header("Accept-Language", "fr;q=1, it;q=0.9"))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$.id", is("languagetest")))
                 .andExpect(jsonPath("$.name", is("languagetest")))
                 .andExpect(jsonPath("$.type", is("submissionform")))
                 .andExpect(jsonPath("$._links.self.href", Matchers
                            .startsWith(REST_SERVER_URL + "config/submissionforms/languagetest")))
                 .andExpect(jsonPath("$.rows[0].fields", contains(SubmissionFormFieldMatcher
                            .matchFormFieldDefinition("name", "Autore", null, "\u00C8 richiesto almeno un autore", true,
                                                      "Aggiungi un autore", "dc.contributor.author"))))
                 .andExpect(jsonPath("$.rows[1].fields", contains(SubmissionFormFieldMatcher
                            .matchFormFieldDefinition("onebox", "Titolo", null,
                            "\u00C8 necessario inserire un titolo principale per questo item", false,
                            "Inserisci titolo principale di questo item", "dc.title"))));

        resetLocalesConfiguration();
    }

    @Test
    public void multipleExternalSourcesTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/config/submissionforms/traditionalpageone"))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                //Check that the JSON root matches the expected "traditionalpageone" input forms
                .andExpect(jsonPath("$.id", is("traditionalpageone")))
                .andExpect(jsonPath("$.name", is("traditionalpageone")))
                .andExpect(jsonPath("$.type", is("submissionform")))
                .andExpect(jsonPath("$._links.self.href", Matchers
                        .startsWith(REST_SERVER_URL + "config/submissionforms/traditionalpageone")))
                // check the external sources of the first field in the first row
                .andExpect(jsonPath("$.rows[0].fields[0].selectableRelationship.externalSources",
                        contains(is("orcid"), is("my_staff_db"))))
        ;
    }

    @Test
    public void noExternalSourcesTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/config/submissionforms/journalVolumeStep"))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                //Check that the JSON root matches the expected "journalVolumeStep" input forms
                .andExpect(jsonPath("$.id", is("journalVolumeStep")))
                .andExpect(jsonPath("$.name", is("journalVolumeStep")))
                .andExpect(jsonPath("$.type", is("submissionform")))
                .andExpect(jsonPath("$._links.self.href", Matchers
                        .startsWith(REST_SERVER_URL + "config/submissionforms/journalVolumeStep")))
                // check the external sources of the first field in the first row
                .andExpect(jsonPath("$.rows[0].fields[0].selectableRelationship.externalSources", nullValue()))
        ;
    }

    private void resetLocalesConfiguration() throws DCInputsReaderException {
        configurationService.setProperty("default.locale","en");
        configurationService.setProperty("webui.supported.locales",null);
        submissionFormRestRepository.reload();
        DCInputAuthority.reset();
        pluginService.clearNamedPluginClasses();
        cas.clearCache();
    }

    @Test
    public void findAllPaginationTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/config/submissionforms")
                 .param("size", "2")
                 .param("page", "0"))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$._embedded.submissionforms[0].id", is("bitstream-metadata")))
                 .andExpect(jsonPath("$._embedded.submissionforms[1].id", is("journalVolumeStep")))
                 .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                         Matchers.containsString("/api/config/submissionforms?"),
                         Matchers.containsString("page=0"), Matchers.containsString("size=2"))))
                 .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                         Matchers.containsString("/api/config/submissionforms?"),
                         Matchers.containsString("page=0"), Matchers.containsString("size=2"))))
                 .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                         Matchers.containsString("/api/config/submissionforms?"),
                         Matchers.containsString("page=1"), Matchers.containsString("size=2"))))
                 .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                         Matchers.containsString("/api/config/submissionforms?"),
                         Matchers.containsString("page=3"), Matchers.containsString("size=2"))))
                 .andExpect(jsonPath("$.page.size", is(2)))
                 .andExpect(jsonPath("$.page.totalElements", equalTo(8)))
                 .andExpect(jsonPath("$.page.totalPages", equalTo(4)))
                 .andExpect(jsonPath("$.page.number", is(0)));

        getClient(tokenAdmin).perform(get("/api/config/submissionforms")
                 .param("size", "2")
                 .param("page", "1"))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$._embedded.submissionforms[0].id", is("languagetest")))
                 .andExpect(jsonPath("$._embedded.submissionforms[1].id", is("qualdroptest")))
                 .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                         Matchers.containsString("/api/config/submissionforms?"),
                         Matchers.containsString("page=0"), Matchers.containsString("size=2"))))
                 .andExpect(jsonPath("$._links.prev.href", Matchers.allOf(
                         Matchers.containsString("/api/config/submissionforms?"),
                         Matchers.containsString("page=0"), Matchers.containsString("size=2"))))
                 .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                         Matchers.containsString("/api/config/submissionforms?"),
                         Matchers.containsString("page=1"), Matchers.containsString("size=2"))))
                 .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                         Matchers.containsString("/api/config/submissionforms?"),
                         Matchers.containsString("page=2"), Matchers.containsString("size=2"))))
                 .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                         Matchers.containsString("/api/config/submissionforms?"),
                         Matchers.containsString("page=3"), Matchers.containsString("size=2"))))
                 .andExpect(jsonPath("$.page.size", is(2)))
                 .andExpect(jsonPath("$.page.totalElements", equalTo(8)))
                 .andExpect(jsonPath("$.page.totalPages", equalTo(4)))
                 .andExpect(jsonPath("$.page.number", is(1)));

        getClient(tokenAdmin).perform(get("/api/config/submissionforms")
                .param("size", "2")
                .param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.submissionforms[0].id", is("traditionalpagetwo")))
                .andExpect(jsonPath("$._embedded.submissionforms[1].id", is("sampleauthority")))
                .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                        Matchers.containsString("/api/config/submissionforms?"),
                        Matchers.containsString("page=0"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.prev.href", Matchers.allOf(
                        Matchers.containsString("/api/config/submissionforms?"),
                        Matchers.containsString("page=1"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                        Matchers.containsString("/api/config/submissionforms?"),
                        Matchers.containsString("page=2"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                        Matchers.containsString("/api/config/submissionforms?"),
                        Matchers.containsString("page=3"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.totalElements", equalTo(8)))
                .andExpect(jsonPath("$.page.totalPages", equalTo(4)))
                .andExpect(jsonPath("$.page.number", is(2)));

        getClient(tokenAdmin).perform(get("/api/config/submissionforms")
            .param("size", "2")
            .param("page", "3"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.submissionforms[0].id", is("traditionalpageone")))
            .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                Matchers.containsString("/api/config/submissionforms?"),
                Matchers.containsString("page=0"), Matchers.containsString("size=2"))))
            .andExpect(jsonPath("$._links.prev.href", Matchers.allOf(
                Matchers.containsString("/api/config/submissionforms?"),
                Matchers.containsString("page=2"), Matchers.containsString("size=2"))))
            .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                Matchers.containsString("/api/config/submissionforms?"),
                Matchers.containsString("page=3"), Matchers.containsString("size=2"))))
            .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                Matchers.containsString("/api/config/submissionforms?"),
                Matchers.containsString("page=3"), Matchers.containsString("size=2"))))
            .andExpect(jsonPath("$.page.size", is(2)))
            .andExpect(jsonPath("$.page.totalElements", equalTo(8)))
            .andExpect(jsonPath("$.page.totalPages", equalTo(4)))
            .andExpect(jsonPath("$.page.number", is(3)));
    }
}
