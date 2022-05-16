/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.matcher.VocabularyMatcher.matchVocabularyEntry;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.EPersonBuilder;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Integration tests for {@link EPersonAuthority}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class EPersonAuthorityIT extends AbstractControllerIntegrationTest {


    @Test
    public void testEPersonAuthorityWithFirstName() throws Exception {

        context.turnOffAuthorisationSystem();
        String firstEPersonId = createEPerson("Luca", "Giamminonni");
        String secondEPersonId = createEPerson("Andrea", "Bollini");
        String thirdEPersonId = createEPerson("Luca", "Bollini");
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/EPersonAuthority/entries")
            .param("filter", "Luca"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                matchVocabularyEntry("Luca Giamminonni", "Luca Giamminonni", "vocabularyEntry", firstEPersonId),
                matchVocabularyEntry("Luca Bollini", "Luca Bollini", "vocabularyEntry", thirdEPersonId))))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)));

        getClient(token).perform(get("/api/submission/vocabularies/EPersonAuthority/entries")
            .param("filter", "Andrea"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                matchVocabularyEntry("Andrea Bollini", "Andrea Bollini", "vocabularyEntry", secondEPersonId))))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));

    }

    @Test
    public void testEPersonAuthorityWithLastName() throws Exception {

        context.turnOffAuthorisationSystem();
        String firstEPersonId = createEPerson("Luca", "Giamminonni");
        String secondEPersonId = createEPerson("Andrea", "Bollini");
        String thirdEPersonId = createEPerson("Luca", "Bollini");
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/EPersonAuthority/entries")
            .param("filter", "Giamminonni"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                matchVocabularyEntry("Luca Giamminonni", "Luca Giamminonni", "vocabularyEntry", firstEPersonId))))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));

        getClient(token).perform(get("/api/submission/vocabularies/EPersonAuthority/entries")
            .param("filter", "Bollini"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                matchVocabularyEntry("Andrea Bollini", "Andrea Bollini", "vocabularyEntry", secondEPersonId),
                matchVocabularyEntry("Luca Bollini", "Luca Bollini", "vocabularyEntry", thirdEPersonId))))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)));

    }

    @Test
    public void testEPersonAuthorityWithId() throws Exception {

        context.turnOffAuthorisationSystem();
        String firstEPersonId = createEPerson("Luca", "Giamminonni");
        String secondEPersonId = createEPerson("Andrea", "Bollini");
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/EPersonAuthority/entries")
            .param("filter", firstEPersonId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                matchVocabularyEntry("Luca Giamminonni", "Luca Giamminonni", "vocabularyEntry", firstEPersonId))))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));

        getClient(token).perform(get("/api/submission/vocabularies/EPersonAuthority/entries")
            .param("filter", secondEPersonId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                matchVocabularyEntry("Andrea Bollini", "Andrea Bollini", "vocabularyEntry", secondEPersonId))))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));

    }

    @Test
    public void testEPersonAuthorityWithAnonymousUser() throws Exception {

        context.turnOffAuthorisationSystem();
        createEPerson("Luca", "Giamminonni");
        createEPerson("Andrea", "Bollini");
        context.restoreAuthSystemState();

        getClient().perform(get("/api/submission/vocabularies/EPersonAuthority/entries")
            .param("filter", "Luca"))
            .andExpect(status().isUnauthorized());

    }

    @Test
    public void testEPersonAuthorityWithNotAdminUser() throws Exception {

        context.turnOffAuthorisationSystem();
        createEPerson("Luca", "Giamminonni");
        createEPerson("Andrea", "Bollini");
        createEPerson("Luca", "Bollini");
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/EPersonAuthority/entries")
            .param("filter", "Luca"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", empty()))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));

    }

    private String createEPerson(String firstName, String lastName) throws SQLException {
        return EPersonBuilder.createEPerson(context)
            .withNameInMetadata(firstName, lastName)
            .build()
            .getID()
            .toString();
    }

}
