/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.dspace.app.rest.matcher.VocabularyEntryDetailsMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * 
 * 
 * @author Mykhaylo Boychuk (4science.it)
 */
public class VocabularyEntryDetailsIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAllTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/submission/vocabularyEntryDetails"))
                            .andExpect(status()
                            .isMethodNotAllowed());
    }

    @Test
    public void findOneTest() throws Exception {
        String idAuthority = "srsc:SCB110";
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularyEntryDetails/" + idAuthority))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$",
                                VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB110", "Religion/Theology",
                                        "Research Subject Categories::HUMANITIES and RELIGION::Religion/Theology")))
                        .andExpect(jsonPath("$.selectable", is(true)))
                        .andExpect(jsonPath("$.otherInformation.id", is("SCB110")))
                        .andExpect(jsonPath("$.otherInformation.note", is("Religionsvetenskap/Teologi")))
                        .andExpect(jsonPath("$.otherInformation.parent",
                                is("Research Subject Categories::HUMANITIES and RELIGION")))
                        .andExpect(jsonPath("$._links.parent.href",
                                endsWith("api/submission/vocabularyEntryDetails/srsc:SCB110/parent")))
                        .andExpect(jsonPath("$._links.children.href",
                                endsWith("api/submission/vocabularyEntryDetails/srsc:SCB110/children")));
    }

    @Test
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
          .andExpect(jsonPath("$._embedded.vocabularyEntryDetails", Matchers.containsInAnyOrder(
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB11", "HUMANITIES and RELIGION",
                  "Research Subject Categories::HUMANITIES and RELIGION"),
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB12", "LAW/JURISPRUDENCE",
                  "Research Subject Categories::LAW/JURISPRUDENCE"),
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB13", "SOCIAL SCIENCES",
                  "Research Subject Categories::SOCIAL SCIENCES"),
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB14", "MATHEMATICS",
                  "Research Subject Categories::MATHEMATICS"),
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB15", "NATURAL SCIENCES",
                  "Research Subject Categories::NATURAL SCIENCES"),
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB16", "TECHNOLOGY",
                  "Research Subject Categories::TECHNOLOGY"),
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB17",
                   "FORESTRY, AGRICULTURAL SCIENCES and LANDSCAPE PLANNING",
                  "Research Subject Categories::FORESTRY, AGRICULTURAL SCIENCES and LANDSCAPE PLANNING"),
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB18", "MEDICINE",
                  "Research Subject Categories::MEDICINE"),
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB19", "ODONTOLOGY",
                  "Research Subject Categories::ODONTOLOGY"),
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB21", "PHARMACY",
                  "Research Subject Categories::PHARMACY"),
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB22", "VETERINARY MEDICINE",
                  "Research Subject Categories::VETERINARY MEDICINE"),
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB23", "INTERDISCIPLINARY RESEARCH AREAS",
                  "Research Subject Categories::INTERDISCIPLINARY RESEARCH AREAS")
          )))
          .andExpect(jsonPath("$.page.totalElements", Matchers.is(12)));

        getClient(tokenEPerson).perform(get("/api/submission/vocabularyEntryDetails/search/top")
         .param("vocabulary", "srsc"))
         .andExpect(status().isOk())
         .andExpect(jsonPath("$._embedded.vocabularyEntryDetails", Matchers.containsInAnyOrder(
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB11", "HUMANITIES and RELIGION",
                         "Research Subject Categories::HUMANITIES and RELIGION"),
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB12", "LAW/JURISPRUDENCE",
                         "Research Subject Categories::LAW/JURISPRUDENCE"),
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB13", "SOCIAL SCIENCES",
                         "Research Subject Categories::SOCIAL SCIENCES"),
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB14", "MATHEMATICS",
                         "Research Subject Categories::MATHEMATICS"),
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB15", "NATURAL SCIENCES",
                         "Research Subject Categories::NATURAL SCIENCES"),
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB16", "TECHNOLOGY",
                         "Research Subject Categories::TECHNOLOGY"),
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB17",
                          "FORESTRY, AGRICULTURAL SCIENCES and LANDSCAPE PLANNING",
                         "Research Subject Categories::FORESTRY, AGRICULTURAL SCIENCES and LANDSCAPE PLANNING"),
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB18", "MEDICINE",
                         "Research Subject Categories::MEDICINE"),
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB19", "ODONTOLOGY",
                         "Research Subject Categories::ODONTOLOGY"),
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB21", "PHARMACY",
                         "Research Subject Categories::PHARMACY"),
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB22", "VETERINARY MEDICINE",
                         "Research Subject Categories::VETERINARY MEDICINE"),
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB23", "INTERDISCIPLINARY RESEARCH AREAS",
                         "Research Subject Categories::INTERDISCIPLINARY RESEARCH AREAS")
          )))
         .andExpect(jsonPath("$.page.totalElements", Matchers.is(12)));
    }

    @Test
    public void srscSearchFirstLevel_MATHEMATICS_Test() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/submission/vocabularyEntryDetails/srsc:SCB14/children"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.children", Matchers.containsInAnyOrder(
                   VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB1401",
                           "Algebra, geometry and mathematical analysis",
                           "Research Subject Categories::MATHEMATICS::Algebra, geometry and mathematical analysis"),
                   VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB1402", "Applied mathematics",
                           "Research Subject Categories::MATHEMATICS::Applied mathematics"),
                   VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB1409", "Other mathematics",
                           "Research Subject Categories::MATHEMATICS::Other mathematics")
                  )))
                 .andExpect(jsonPath("$._embedded.children[*].otherInformation.parent",
                         Matchers.everyItem(is("Research Subject Categories::MATHEMATICS"))))
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
          .andExpect(jsonPath("$._embedded.vocabularyEntryDetails", Matchers.containsInAnyOrder(
                  VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB11", "HUMANITIES and RELIGION",
                          "Research Subject Categories::HUMANITIES and RELIGION"),
                  VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB12", "LAW/JURISPRUDENCE",
                          "Research Subject Categories::LAW/JURISPRUDENCE"),
                  VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB13", "SOCIAL SCIENCES",
                          "Research Subject Categories::SOCIAL SCIENCES"),
                  VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB14", "MATHEMATICS",
                          "Research Subject Categories::MATHEMATICS"),
                  VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB15", "NATURAL SCIENCES",
                          "Research Subject Categories::NATURAL SCIENCES")
              )))
          .andExpect(jsonPath("$.page.totalElements", is(12)))
          .andExpect(jsonPath("$.page.totalPages", is(3)))
          .andExpect(jsonPath("$.page.number", is(0)));

        //second page
        getClient(tokenAdmin).perform(get("/api/submission/vocabularyEntryDetails/search/top")
                 .param("vocabulary", "srsc")
                 .param("page", "1")
                 .param("size", "5"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$._embedded.vocabularyEntryDetails", Matchers.containsInAnyOrder(
           VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB16", "TECHNOLOGY",
                   "Research Subject Categories::TECHNOLOGY"),
           VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB17",
                        "FORESTRY, AGRICULTURAL SCIENCES and LANDSCAPE PLANNING",
                   "Research Subject Categories::FORESTRY, AGRICULTURAL SCIENCES and LANDSCAPE PLANNING"),
           VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB18", "MEDICINE",
                   "Research Subject Categories::MEDICINE"),
           VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB19", "ODONTOLOGY",
                   "Research Subject Categories::ODONTOLOGY"),
           VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB21", "PHARMACY",
                   "Research Subject Categories::PHARMACY")
               )))
           .andExpect(jsonPath("$.page.totalElements", is(12)))
           .andExpect(jsonPath("$.page.totalPages", is(3)))
           .andExpect(jsonPath("$.page.number", is(1)));

        // third page
        getClient(tokenAdmin).perform(get("/api/submission/vocabularyEntryDetails/search/top")
                 .param("vocabulary", "srsc")
                 .param("page", "2")
                 .param("size", "5"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$._embedded.vocabularyEntryDetails", Matchers.containsInAnyOrder(
                   VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB22", "VETERINARY MEDICINE",
                           "Research Subject Categories::VETERINARY MEDICINE"),
                   VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB23", "INTERDISCIPLINARY RESEARCH AREAS",
                           "Research Subject Categories::INTERDISCIPLINARY RESEARCH AREAS")
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
    public void srscSearchByParentFirstLevelPaginationTest() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        // first page
        getClient(token).perform(get("/api/submission/vocabularyEntryDetails/srsc:SCB14/children")
                 .param("page", "0")
                 .param("size", "2"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.children", Matchers.containsInAnyOrder(
                     VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB1401",
                             "Algebra, geometry and mathematical analysis",
                             "Research Subject Categories::MATHEMATICS::Algebra, geometry and mathematical analysis"),
                     VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB1402", "Applied mathematics",
                             "Research Subject Categories::MATHEMATICS::Applied mathematics")
                     )))
                 .andExpect(jsonPath("$.page.totalElements", is(3)))
                 .andExpect(jsonPath("$.page.totalPages", is(2)))
                 .andExpect(jsonPath("$.page.number", is(0)));

        // second page
        getClient(token).perform(get("/api/submission/vocabularyEntryDetails/srsc:SCB14/children")
                .param("page", "1")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.children", Matchers.contains(
                    VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB1409", "Other mathematics",
                            "Research Subject Categories::MATHEMATICS::Other mathematics")
                    )))
                .andExpect(jsonPath("$.page.totalElements", is(3)))
                .andExpect(jsonPath("$.page.totalPages", is(2)))
                .andExpect(jsonPath("$.page.number", is(1)));
    }

    @Test
    public void srscSearchByParentSecondLevel_Applied_mathematics_Test() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularyEntryDetails/srsc:SCB1402/children"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.children", Matchers.containsInAnyOrder(
              VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:VR140202", "Numerical analysis",
                      "Research Subject Categories::MATHEMATICS::Applied mathematics::Numerical analysis"),
              VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:VR140203", "Mathematical statistics",
                      "Research Subject Categories::MATHEMATICS::Applied mathematics::Mathematical statistics"),
              VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:VR140204", "Optimization, systems theory",
                      "Research Subject Categories::MATHEMATICS::Applied mathematics::Optimization, systems theory"),
              VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:VR140205", "Theoretical computer science",
                      "Research Subject Categories::MATHEMATICS::Applied mathematics::Theoretical computer science")
             )))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(4)));
    }

    @Test
    public void srscSearchByParentEmptyTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/submission/vocabularyEntryDetails/srsc:VR140202/children"))
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
        getClient().perform(get("/api/submission/vocabularyEntryDetails/search/top")
                   .param("vocabulary", "srsc"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findParentByChildTest() throws Exception {
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/submission/vocabularyEntryDetails/srsc:SCB11/parent"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", is(VocabularyEntryDetailsMatcher.matchAuthorityEntry(
                         "srsc:ResearchSubjectCategories", "Research Subject Categories", "Research Subject Categories")
                  )));
    }

    @Test
    public void findParentByChildSecondLevelTest() throws Exception {
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/submission/vocabularyEntryDetails/srsc:SCB180/parent"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", is(
                         VocabularyEntryDetailsMatcher.matchAuthorityEntry(
                                 "srsc:SCB18", "MEDICINE","Research Subject Categories::MEDICINE")
                  )));
    }

    @Test
    public void findParentByChildBadRequestTest() throws Exception {
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/submission/vocabularyEntryDetails/" + UUID.randomUUID() + "/parent"))
                               .andExpect(status().isBadRequest());
    }

    @Test
    public void findParentByChildUnauthorizedTest() throws Exception {
        getClient().perform(get("/api/submission/vocabularyEntryDetails/srsc:SCB180/parent"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findParentRootChildTest() throws Exception {
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson)
            .perform(get("/api/submission/vocabularyEntryDetails/srsc:ResearchSubjectCategory/parent"))
            .andExpect(status().isNoContent());
    }
}
