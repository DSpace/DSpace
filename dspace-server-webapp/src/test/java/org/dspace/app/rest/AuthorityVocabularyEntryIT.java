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

import java.util.UUID;

import org.dspace.app.rest.matcher.AuthorityEntryMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.Test;

public class AuthorityVocabularyEntryIT extends AbstractControllerIntegrationTest {

    @Test
    public void findOneTest() throws Exception {
        String idAuthority = "srsc:SCB110";
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularyEntryDetails/" + idAuthority))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id", is("srsc:SCB110")))
                        .andExpect(jsonPath("$.value",
                                         is("Research Subject Categories::HUMANITIES and RELIGION::Religion/Theology")))
                        .andExpect(jsonPath("$.display", is("Religion/Theology")))
                        .andExpect(jsonPath("$.selectable", is(true)))
                        .andExpect(jsonPath("$.otherInformation.id", is("SCB110")))
                        .andExpect(jsonPath("$.otherInformation.note", is("Religionsvetenskap/Teologi")))
                        .andExpect(jsonPath("$.otherInformation.parent", is("HUMANITIES and RELIGION")));
    }

    @Test
    public void findOneBadRequestTest() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularyEntryDetails/" + UUID.randomUUID().toString()))
                        .andExpect(status().isBadRequest());
    }

    public void findOneUnauthorizedTest() throws Exception {
        String idAuthority = "srsc:SCB110";
        getClient().perform(get("/api/submission/vocabularyEntryDetails/" + idAuthority))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void srscSearchTopTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/submission/vocabularyEntryDetails/search/top")
          .param("vocabulary", "srsc"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", Matchers.containsInAnyOrder(
          AuthorityEntryMatcher.matchAuthority("srsc:SCB11", "HUMANITIES and RELIGION"),
          AuthorityEntryMatcher.matchAuthority("srsc:SCB12", "LAW/JURISPRUDENCE"),
          AuthorityEntryMatcher.matchAuthority("srsc:SCB13", "SOCIAL SCIENCES"),
          AuthorityEntryMatcher.matchAuthority("srsc:SCB14", "MATHEMATICS"),
          AuthorityEntryMatcher.matchAuthority("srsc:SCB15", "NATURAL SCIENCES"),
          AuthorityEntryMatcher.matchAuthority("srsc:SCB16", "TECHNOLOGY"),
          AuthorityEntryMatcher.matchAuthority("srsc:SCB17", "FORESTRY, AGRICULTURAL SCIENCES and LANDSCAPE PLANNING"),
          AuthorityEntryMatcher.matchAuthority("srsc:SCB18", "MEDICINE"),
          AuthorityEntryMatcher.matchAuthority("srsc:SCB19", "ODONTOLOGY"),
          AuthorityEntryMatcher.matchAuthority("srsc:SCB20", "PHARMACY"),
          AuthorityEntryMatcher.matchAuthority("srsc:SCB21", "VETERINARY MEDICINE"),
          AuthorityEntryMatcher.matchAuthority("srsc:SCB22", "INTERDISCIPLINARY RESEARCH AREAS")
          )))
          .andExpect(jsonPath("$.page.totalElements", Matchers.is(12)));

        getClient(tokenEPerson).perform(get("/api/submission/vocabularyEntryDetails/search/top")
         .param("vocabulary", "srsc"))
         .andExpect(status().isOk())
         .andExpect(jsonPath("$", Matchers.containsInAnyOrder(
          AuthorityEntryMatcher.matchAuthority("srsc:SCB11", "HUMANITIES and RELIGION"),
          AuthorityEntryMatcher.matchAuthority("srsc:SCB12", "LAW/JURISPRUDENCE"),
          AuthorityEntryMatcher.matchAuthority("srsc:SCB13", "SOCIAL SCIENCES"),
          AuthorityEntryMatcher.matchAuthority("srsc:SCB14", "MATHEMATICS"),
          AuthorityEntryMatcher.matchAuthority("srsc:SCB15", "NATURAL SCIENCES"),
          AuthorityEntryMatcher.matchAuthority("srsc:SCB16", "TECHNOLOGY"),
          AuthorityEntryMatcher.matchAuthority("srsc:SCB17", "FORESTRY, AGRICULTURAL SCIENCES and LANDSCAPE PLANNING"),
          AuthorityEntryMatcher.matchAuthority("srsc:SCB18", "MEDICINE"),
          AuthorityEntryMatcher.matchAuthority("srsc:SCB19", "ODONTOLOGY"),
          AuthorityEntryMatcher.matchAuthority("srsc:SCB20", "PHARMACY"),
          AuthorityEntryMatcher.matchAuthority("srsc:SCB21", "VETERINARY MEDICINE"),
          AuthorityEntryMatcher.matchAuthority("srsc:SCB22", "INTERDISCIPLINARY RESEARCH AREAS")
          )))
         .andExpect(jsonPath("$.page.totalElements", Matchers.is(12)));
    }

    @Test
    public void srscSearchFirstLevel_MATHEMATICS_Test() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/submission/vocabularyEntryDetails/" + "srsc:SCB14" + "/children"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.containsInAnyOrder(
                   AuthorityEntryMatcher.matchAuthority("srsc:SCB1401", "Algebra, geometry and mathematical analysis"),
                   AuthorityEntryMatcher.matchAuthority("srsc:SCB1402", "Applied mathematics"),
                   AuthorityEntryMatcher.matchAuthority("srsc:SCB1409", "Other mathematics")
                 )))
                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));
    }

    @Test
    public void srscSearchTopPaginationTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/submission/vocabularyEntryDetails/search/top")
                             .param("vocabulary", "srsc")
                             .param("page", "0")
                             .param("size", "5"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", Matchers.containsInAnyOrder(
                  AuthorityEntryMatcher.matchAuthority("srsc:SCB11", "HUMANITIES and RELIGION"),
                  AuthorityEntryMatcher.matchAuthority("srsc:SCB12", "LAW/JURISPRUDENCE"),
                  AuthorityEntryMatcher.matchAuthority("srsc:SCB13", "SOCIAL SCIENCES"),
                  AuthorityEntryMatcher.matchAuthority("srsc:SCB14", "MATHEMATICS"),
                  AuthorityEntryMatcher.matchAuthority("srsc:SCB15", "NATURAL SCIENCES")
              )))
          .andExpect(jsonPath("$.page.totalElements", is(12)))
          .andExpect(jsonPath("$.page.totalPages", is(3)))
          .andExpect(jsonPath("$.page.number", is(0)));

        //second page
        getClient(tokenAdmin).perform(get("/api/submission/authorities/srsc/entries/search/top")
                 .param("page", "1")
                 .param("size", "5"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", Matchers.containsInAnyOrder(
           AuthorityEntryMatcher.matchAuthority("srsc:SCB16", "TECHNOLOGY"),
           AuthorityEntryMatcher.matchAuthority("srsc:SCB17", "FORESTRY, AGRICULTURAL SCIENCES and LANDSCAPE PLANNING"),
           AuthorityEntryMatcher.matchAuthority("srsc:SCB18", "MEDICINE"),
           AuthorityEntryMatcher.matchAuthority("srsc:SCB19", "ODONTOLOGY"),
           AuthorityEntryMatcher.matchAuthority("srsc:SCB20", "PHARMACY")
               )))
           .andExpect(jsonPath("$.page.totalElements", is(12)))
           .andExpect(jsonPath("$.page.totalPages", is(3)))
           .andExpect(jsonPath("$.page.number", is(1)));

        // third page
        getClient(tokenAdmin).perform(get("/api/submission/authorities/srsc/entries/search/top")
                 .param("page", "2")
                 .param("size", "5"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", Matchers.containsInAnyOrder(
                   AuthorityEntryMatcher.matchAuthority("srsc:SCB21", "VETERINARY MEDICINE"),
                   AuthorityEntryMatcher.matchAuthority("srsc:SCB22", "INTERDISCIPLINARY RESEARCH AREAS")
               )))
           .andExpect(jsonPath("$.page.totalElements", is(12)))
           .andExpect(jsonPath("$.page.totalPages", is(3)))
           .andExpect(jsonPath("$.page.number", is(2)));
    }

    @Test
    public void searchTopBadRequestTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/submission/vocabularyEntryDetails/search/top")
                             .param("vocabulary", UUID.randomUUID().toString()))
                             .andExpect(status().isBadRequest());
    }

    @Test
    public void searchTopUnauthorizedTest() throws Exception {
        getClient().perform(get("/api/submission/vocabularyEntryDetails/search/top")
                   .param("vocabulary", "srsc:SCB16"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void retrieveSrscValueTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularyEntryDetails/" + "SCB1922")
                .param("projection", "full"))
                .andExpect(status().isOk());
    }

    @Test
    public void srscSearchByParentFirstLevelPaginationTest() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        // first page
        getClient(token).perform(get("/api/submission/vocabularyEntryDetails/" + "srsc:SCB14" + "/children")
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
        getClient(token).perform(get("/api/submission/vocabularyEntryDetails/" + "srsc:SCB14" + "/children")
                .param("page", "1")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.contains(
                    AuthorityEntryMatcher.matchAuthority("SCB1409", "Other mathematics")
                    )))
                .andExpect(jsonPath("$.page.totalElements", is(3)))
                .andExpect(jsonPath("$.page.totalPages", is(2)))
                .andExpect(jsonPath("$.page.number", is(1)));
    }

    @Test
    public void srscSearchByParentSecondLevel_Applied_mathematics_Test() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularyEntryDetails/" + "srsc:SCB1402" + "/children"))
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
        getClient(tokenAdmin).perform(get("/api/submission/vocabularyEntryDetails/" + "srsc:VR140202" + "/children"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));
    }

    @Test
    public void srscSearchByParentWrongIdTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/submission/vocabularyEntryDetails/"
                                                          + UUID.randomUUID() + "/children"))
                             .andExpect(status().isBadRequest());
    }

    @Test
    public void srscSearchTopUnauthorizedTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/submission/vocabularyEntryDetails/search/top")
          .param("vocabulary", "srsc"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    public void srscSearchParentByChildrenTest() throws Exception {
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/submission/vocabularyEntryDetails/" + "srsc:VR140202" + "/children"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.authorityEntries", Matchers.contains(
                            AuthorityEntryMatcher.matchAuthority("SCB1402", "Applied mathematics")
                            )))
                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));
    }

    @Test
    public void srscSearchParentByChildrenRootTest() throws Exception {
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/submission/vocabularyEntryDetails/" + "srsc:SCB11" + "/children"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));
    }
}
