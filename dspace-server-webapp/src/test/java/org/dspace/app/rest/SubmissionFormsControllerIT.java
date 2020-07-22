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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;

import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.matcher.SubmissionFormFieldMatcher;
import org.dspace.app.rest.repository.SubmissionFormRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.util.DCInputsReaderException;
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
                   //The configuration file for the test env includes 3 forms
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", equalTo(4)))
                   .andExpect(jsonPath("$.page.totalPages", equalTo(1)))
                   .andExpect(jsonPath("$.page.number", is(0)))
                   .andExpect(
                       jsonPath("$._links.self.href", Matchers.startsWith(REST_SERVER_URL + "config/submissionforms")))
                   //The array of submissionforms should have a size of 3
                   .andExpect(jsonPath("$._embedded.submissionforms", hasSize(equalTo(4))))
        ;
    }

    @Test
    public void findAllWithNewlyCreatedAccountTest() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/config/submissionforms"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", equalTo(4)))
                .andExpect(jsonPath("$.page.totalPages", equalTo(1)))
                .andExpect(jsonPath("$.page.number", is(0)))
                .andExpect(jsonPath("$._links.self.href", Matchers.startsWith(REST_SERVER_URL
                           + "config/submissionforms")))
                .andExpect(jsonPath("$._embedded.submissionforms", hasSize(equalTo(4))));
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
                        SubmissionFormFieldMatcher.matchFormFieldDefinition("name", "Author",
                null, true,"Add an author", "dc.contributor.author"))))
                   .andExpect(jsonPath("$.rows[1].fields", contains(
                        SubmissionFormFieldMatcher.matchFormFieldDefinition("onebox", "Title",
                                "You must enter a main title for this item.", false,
                                "Enter the main title of the item.", "dc.title"))))
                   // check a row with multiple fields
                   .andExpect(jsonPath("$.rows[3].fields",
                        contains(
                                SubmissionFormFieldMatcher.matchFormFieldDefinition("date", "Date of Issue",
                                        "You must enter at least the year.", false,
                                        "Please give the date", "col-sm-4",
                                        "dc.date.issued"),
                                SubmissionFormFieldMatcher.matchFormFieldDefinition("onebox", "Publisher",
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
                        SubmissionFormFieldMatcher.matchFormFieldDefinition("name", "Author",
                          null, true,"Add an author", "dc.contributor.author"))))
                   .andExpect(jsonPath("$.rows[1].fields", contains(
                        SubmissionFormFieldMatcher.matchFormFieldDefinition("onebox", "Title",
                                "You must enter a main title for this item.", false,
                                "Enter the main title of the item.", "dc.title"))))
                   .andExpect(jsonPath("$.rows[3].fields",contains(
                                SubmissionFormFieldMatcher.matchFormFieldDefinition("date", "Date of Issue",
                                        "You must enter at least the year.", false,
                                        "Please give the date", "col-sm-4",
                                        "dc.date.issued"),
                                SubmissionFormFieldMatcher.matchFormFieldDefinition("onebox", "Publisher",
                                        null, false,"Enter the name of",
                                        "col-sm-8","dc.publisher"))));
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
                    false,"Select the journal related to this volume.", "isVolumeOfJournal",
                        "creativework.publisher:somepublishername", "periodical", false))))
        ;
    }

    @Test
    public void languageSupportTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String[] supportedLanguage = {"it","uk"};
        configurationService.setProperty("webui.supported.locales",supportedLanguage);
        submissionFormRestRepository.reload();

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
                            .matchFormFieldDefinition("name", "Autore", "\u00C8" + " richiesto almeno un autore", true,
                                                      "Aggiungi un autore", "dc.contributor.author"))))
                 .andExpect(jsonPath("$.rows[1].fields", contains(SubmissionFormFieldMatcher
                            .matchFormFieldDefinition("onebox", "Titolo",
                            "\u00C8" + " necessario inserire un titolo principale per questo item", false,
                            "Inserisci titolo principale di questo item", "dc.title"))))
                 .andExpect(jsonPath("$.rows[2].fields", contains(SubmissionFormFieldMatcher
                            .matchFormFieldDefinition("dropdown", "Lingua", null, false,
                            "Selezionare la lingua del contenuto principale dell'item."
                          + " Se la lingua non compare nell'elenco, selezionare (Altro)."
                          + " Se il contenuto non ha davvero una lingua"
                          + " (ad esempio, se è un set di dati o un'immagine) selezionare (N/A)", "dc.language.iso"))));

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
                           .matchFormFieldDefinition("name", "Автор", "Потрібно ввести хочаб одного автора!",
                                               true, "Додати автора", "dc.contributor.author"))))
                 .andExpect(jsonPath("$.rows[1].fields", contains(SubmissionFormFieldMatcher
                           .matchFormFieldDefinition("onebox", "Заголовок",
                           "Заговолок файла обов'язковий !", false,
                           "Ввести основний заголовок файла", "dc.title"))))
                 .andExpect(jsonPath("$.rows[2].fields", contains(SubmissionFormFieldMatcher
                           .matchFormFieldDefinition("dropdown", "Мова", null, false,
                           "Виберiть мову головного змiсту файлу, як що мови немає у списку, вибрати (Iнша)."
                         + " Як що вмiст вайлу не є текстовим, наприклад є фотографiєю, тодi вибрати (N/A)",
                           "dc.language.iso"))));

                 resetLocalesConfiguration();
    }

    @Test
    public void preferLanguageTest() throws Exception {
        context.turnOffAuthorisationSystem();

        String[] supportedLanguage = {"it","uk"};
        configurationService.setProperty("webui.supported.locales",supportedLanguage);
        submissionFormRestRepository.reload();

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
                            .matchFormFieldDefinition("name", "Autore", "\u00C8" + " richiesto almeno un autore", true,
                                                      "Aggiungi un autore", "dc.contributor.author"))))
                 .andExpect(jsonPath("$.rows[1].fields", contains(SubmissionFormFieldMatcher
                            .matchFormFieldDefinition("onebox", "Titolo",
                            "\u00C8" + " necessario inserire un titolo principale per questo item", false,
                            "Inserisci titolo principale di questo item", "dc.title"))))
                 .andExpect(jsonPath("$.rows[2].fields", contains(SubmissionFormFieldMatcher
                            .matchFormFieldDefinition("dropdown", "Lingua", null, false,
                            "Selezionare la lingua del contenuto principale dell'item."
                          + " Se la lingua non compare nell'elenco, selezionare (Altro)."
                          + " Se il contenuto non ha davvero una lingua"
                          + " (ad esempio, se è un set di dati o un'immagine) selezionare (N/A)", "dc.language.iso"))));

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
                           .matchFormFieldDefinition("name", "Автор", "Потрібно ввести хочаб одного автора!",
                                               true, "Додати автора", "dc.contributor.author"))))
                 .andExpect(jsonPath("$.rows[1].fields", contains(SubmissionFormFieldMatcher
                           .matchFormFieldDefinition("onebox", "Заголовок",
                           "Заговолок файла обов'язковий !", false,
                           "Ввести основний заголовок файла", "dc.title"))))
                 .andExpect(jsonPath("$.rows[2].fields", contains(SubmissionFormFieldMatcher
                           .matchFormFieldDefinition("dropdown", "Мова", null, false,
                           "Виберiть мову головного змiсту файлу, як що мови немає у списку, вибрати (Iнша)."
                         + " Як що вмiст вайлу не є текстовим, наприклад є фотографiєю, тодi вибрати (N/A)",
                           "dc.language.iso"))));

                 resetLocalesConfiguration();
    }

    @Test
    public void userChoiceAnotherLanguageTest() throws Exception {
        context.turnOffAuthorisationSystem();

        String[] supportedLanguage = {"it","uk"};
        configurationService.setProperty("webui.supported.locales",supportedLanguage);
        submissionFormRestRepository.reload();

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
                           .matchFormFieldDefinition("name", "Autore", "\u00C8" + " richiesto almeno un autore", true,
                                                     "Aggiungi un autore", "dc.contributor.author"))))
                 .andExpect(jsonPath("$.rows[1].fields", contains(SubmissionFormFieldMatcher
                           .matchFormFieldDefinition("onebox", "Titolo",
                           "\u00C8" + " necessario inserire un titolo principale per questo item", false,
                           "Inserisci titolo principale di questo item", "dc.title"))))
                 .andExpect(jsonPath("$.rows[2].fields", contains(SubmissionFormFieldMatcher
                           .matchFormFieldDefinition("dropdown", "Lingua", null, false,
                           "Selezionare la lingua del contenuto principale dell'item."
                         + " Se la lingua non compare nell'elenco, selezionare (Altro)."
                         + " Se il contenuto non ha davvero una lingua"
                         + " (ad esempio, se è un set di dati o un'immagine) selezionare (N/A)", "dc.language.iso"))));

                 resetLocalesConfiguration();
    }

    @Test
    public void defaultLanguageTest() throws Exception {
        context.turnOffAuthorisationSystem();

        String[] supportedLanguage = {"it","uk"};
        configurationService.setProperty("default.locale","it");
        configurationService.setProperty("webui.supported.locales",supportedLanguage);
        submissionFormRestRepository.reload();

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
                            .matchFormFieldDefinition("name", "Autore", "\u00C8 richiesto almeno un autore", true,
                                                      "Aggiungi un autore", "dc.contributor.author"))))
                 .andExpect(jsonPath("$.rows[1].fields", contains(SubmissionFormFieldMatcher
                            .matchFormFieldDefinition("onebox", "Titolo",
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
        submissionFormRestRepository.reload();

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
                            .matchFormFieldDefinition("name", "Autore", "\u00C8 richiesto almeno un autore", true,
                                                      "Aggiungi un autore", "dc.contributor.author"))))
                 .andExpect(jsonPath("$.rows[1].fields", contains(SubmissionFormFieldMatcher
                            .matchFormFieldDefinition("onebox", "Titolo",
                            "\u00C8 necessario inserire un titolo principale per questo item", false,
                            "Inserisci titolo principale di questo item", "dc.title"))));

        resetLocalesConfiguration();
    }

    private void resetLocalesConfiguration() throws DCInputsReaderException {
        configurationService.setProperty("default.locale","en");
        configurationService.setProperty("webui.supported.locales",null);
        submissionFormRestRepository.reload();
    }
}
