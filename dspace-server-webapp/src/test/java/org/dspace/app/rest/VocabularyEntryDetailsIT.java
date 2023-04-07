/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.dspace.app.rest.matcher.VocabularyEntryDetailsMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * 
 * 
 * @author Mykhaylo Boychuk (4science.it)
 */
public class VocabularyEntryDetailsIT extends AbstractControllerIntegrationTest {
    @Test
    public void discoverableNestedLinkTest() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._links",Matchers.allOf(
                                hasJsonPath("$.vocabularyEntryDetails.href",
                                         is("http://localhost/api/submission/vocabularyEntryDetails")),
                                hasJsonPath("$.vocabularyEntryDetails-search.href",
                                         is("http://localhost/api/submission/vocabularyEntryDetails/search"))
                        )));
    }

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
                                        "HUMANITIES and RELIGION::Religion/Theology")))
                        .andExpect(jsonPath("$.selectable", is(true)))
                        .andExpect(jsonPath("$.otherInformation.id", is("SCB110")))
                        .andExpect(jsonPath("$.otherInformation.note", is("Religionsvetenskap/Teologi")))
                        .andExpect(jsonPath("$.otherInformation.parent",
                                is("HUMANITIES and RELIGION")))
                        .andExpect(jsonPath("$._links.parent.href",
                                endsWith("api/submission/vocabularyEntryDetails/srsc:SCB110/parent")))
                        .andExpect(jsonPath("$._links.children.href",
                                endsWith("api/submission/vocabularyEntryDetails/srsc:SCB110/children")));
    }

    @Test
    public void findOneAnonymousTest() throws Exception {
        String idAuthority = "srsc:SCB110";
        getClient().perform(get("/api/submission/vocabularyEntryDetails/" + idAuthority))
                   .andExpect(status().isOk());
    }

    @Test
    public void findOneNotFoundTest() throws Exception {
        String idAuthority = "srsc:not-existing";
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularyEntryDetails/" + idAuthority))
                   .andExpect(status().isNotFound());

        // try with a special id missing only the entry-id part
        getClient(token).perform(get("/api/submission/vocabularyEntryDetails/srsc:"))
                   .andExpect(status().isNotFound());

        // try to retrieve the xml root that is not a entry itself
        getClient(token).perform(get("/api/submission/vocabularyEntryDetails/srsc:ResearchSubjectCategories"))
                   .andExpect(status().isNotFound());

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
                  "HUMANITIES and RELIGION"),
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB12", "LAW/JURISPRUDENCE",
                  "LAW/JURISPRUDENCE"),
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB13", "SOCIAL SCIENCES",
                  "SOCIAL SCIENCES"),
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB14", "MATHEMATICS",
                  "MATHEMATICS"),
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB15", "NATURAL SCIENCES",
                  "NATURAL SCIENCES"),
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB16", "TECHNOLOGY",
                  "TECHNOLOGY"),
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB17",
                   "FORESTRY, AGRICULTURAL SCIENCES and LANDSCAPE PLANNING",
                  "FORESTRY, AGRICULTURAL SCIENCES and LANDSCAPE PLANNING"),
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB18", "MEDICINE",
                  "MEDICINE"),
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB19", "ODONTOLOGY",
                  "ODONTOLOGY"),
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB21", "PHARMACY",
                  "PHARMACY"),
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB22", "VETERINARY MEDICINE",
                  "VETERINARY MEDICINE"),
          VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB23", "INTERDISCIPLINARY RESEARCH AREAS",
                  "INTERDISCIPLINARY RESEARCH AREAS")
          )))
          .andExpect(jsonPath("$.page.totalElements", Matchers.is(12)));

        getClient(tokenEPerson).perform(get("/api/submission/vocabularyEntryDetails/search/top")
         .param("vocabulary", "srsc"))
         .andExpect(status().isOk())
         .andExpect(jsonPath("$._embedded.vocabularyEntryDetails", Matchers.containsInAnyOrder(
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB11", "HUMANITIES and RELIGION",
                         "HUMANITIES and RELIGION"),
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB12", "LAW/JURISPRUDENCE",
                         "LAW/JURISPRUDENCE"),
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB13", "SOCIAL SCIENCES",
                         "SOCIAL SCIENCES"),
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB14", "MATHEMATICS",
                         "MATHEMATICS"),
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB15", "NATURAL SCIENCES",
                         "NATURAL SCIENCES"),
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB16", "TECHNOLOGY",
                         "TECHNOLOGY"),
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB17",
                          "FORESTRY, AGRICULTURAL SCIENCES and LANDSCAPE PLANNING",
                         "FORESTRY, AGRICULTURAL SCIENCES and LANDSCAPE PLANNING"),
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB18", "MEDICINE",
                         "MEDICINE"),
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB19", "ODONTOLOGY",
                         "ODONTOLOGY"),
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB21", "PHARMACY",
                         "PHARMACY"),
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB22", "VETERINARY MEDICINE",
                         "VETERINARY MEDICINE"),
                 VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB23", "INTERDISCIPLINARY RESEARCH AREAS",
                         "INTERDISCIPLINARY RESEARCH AREAS")
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
                           "MATHEMATICS::Algebra, geometry and mathematical analysis"),
                   VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB1402", "Applied mathematics",
                           "MATHEMATICS::Applied mathematics"),
                   VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB1409", "Other mathematics",
                           "MATHEMATICS::Other mathematics")
                  )))
                 .andExpect(jsonPath("$._embedded.children[*].otherInformation.parent",
                         Matchers.everyItem(is("MATHEMATICS"))))
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
                          "HUMANITIES and RELIGION"),
                  VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB12", "LAW/JURISPRUDENCE",
                          "LAW/JURISPRUDENCE"),
                  VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB13", "SOCIAL SCIENCES",
                          "SOCIAL SCIENCES"),
                  VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB14", "MATHEMATICS",
                          "MATHEMATICS"),
                  VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB15", "NATURAL SCIENCES",
                          "NATURAL SCIENCES")
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
                   "TECHNOLOGY"),
           VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB17",
                        "FORESTRY, AGRICULTURAL SCIENCES and LANDSCAPE PLANNING",
                   "FORESTRY, AGRICULTURAL SCIENCES and LANDSCAPE PLANNING"),
           VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB18", "MEDICINE",
                   "MEDICINE"),
           VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB19", "ODONTOLOGY",
                   "ODONTOLOGY"),
           VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB21", "PHARMACY",
                   "PHARMACY")
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
                           "VETERINARY MEDICINE"),
                   VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB23", "INTERDISCIPLINARY RESEARCH AREAS",
                           "INTERDISCIPLINARY RESEARCH AREAS")
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
    public void searchTopAnonymousTest() throws Exception {
        getClient().perform(get("/api/submission/vocabularyEntryDetails/search/top")
                   .param("vocabulary", "srsc"))
                   .andExpect(status().isOk());
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
                             "MATHEMATICS::Algebra, geometry and mathematical analysis"),
                     VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:SCB1402", "Applied mathematics",
                             "MATHEMATICS::Applied mathematics")
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
                            "MATHEMATICS::Other mathematics")
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
                      "MATHEMATICS::Applied mathematics::Numerical analysis"),
              VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:VR140203", "Mathematical statistics",
                      "MATHEMATICS::Applied mathematics::Mathematical statistics"),
              VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:VR140204", "Optimization, systems theory",
                      "MATHEMATICS::Applied mathematics::Optimization, systems theory"),
              VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:VR140205", "Theoretical computer science",
                      "MATHEMATICS::Applied mathematics::Theoretical computer science")
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
    public void srscSearchTopAnonymousTest() throws Exception {
        getClient().perform(get("/api/submission/vocabularyEntryDetails/search/top")
                   .param("vocabulary", "srsc"))
                   .andExpect(status().isOk());
    }

    @Test
    public void findParentByChildTest() throws Exception {
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/submission/vocabularyEntryDetails/srsc:SCB180/parent"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", is(
                         VocabularyEntryDetailsMatcher.matchAuthorityEntry(
                                 "srsc:SCB18", "MEDICINE","MEDICINE")
                  )));
    }

    @Test
    public void findParentByChildBadRequestTest() throws Exception {
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/submission/vocabularyEntryDetails/" + UUID.randomUUID() + "/parent"))
                               .andExpect(status().isBadRequest());
    }

    @Test
    public void findParentByChildAnonymousTest() throws Exception {
        getClient().perform(get("/api/submission/vocabularyEntryDetails/srsc:SCB180/parent"))
                   .andExpect(status().isOk());
    }

    @Test
    public void findParentTopTest() throws Exception {
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson)
            .perform(get("/api/submission/vocabularyEntryDetails/srsc:SCB11/parent"))
            .andExpect(status().isNoContent());
    }

    @Test
    public void srscProjectionTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(
                get("/api/submission/vocabularyEntryDetails/srsc:SCB110").param("projection", "full"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.parent",
                         VocabularyEntryDetailsMatcher.matchAuthorityEntry(
                                 "srsc:SCB11", "HUMANITIES and RELIGION",
                                 "HUMANITIES and RELIGION")))
                 .andExpect(jsonPath("$._embedded.children._embedded.children", matchAllSrscSC110Children()))
                 .andExpect(jsonPath("$._embedded.children._embedded.children[*].otherInformation.parent",
                         Matchers.everyItem(
                                 is("HUMANITIES and RELIGION::Religion/Theology"))));

        getClient(tokenAdmin).perform(
                get("/api/submission/vocabularyEntryDetails/srsc:SCB110").param("embed", "children"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.children._embedded.children", matchAllSrscSC110Children()))
                 .andExpect(jsonPath("$._embedded.children._embedded.children[*].otherInformation.parent",
                         Matchers.everyItem(
                                 is("HUMANITIES and RELIGION::Religion/Theology"))));

        getClient(tokenAdmin).perform(
                get("/api/submission/vocabularyEntryDetails/srsc:SCB110").param("embed", "parent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.parent",
                        VocabularyEntryDetailsMatcher.matchAuthorityEntry(
                                "srsc:SCB11", "HUMANITIES and RELIGION",
                                "HUMANITIES and RELIGION")));
    }

    private Matcher<Iterable<? extends Object>> matchAllSrscSC110Children() {
        return Matchers.containsInAnyOrder(
           VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:VR110102",
                   "History of religion",
                   "HUMANITIES and RELIGION::Religion/Theology::History of religion"),
           VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:VR110103",
                   "Church studies",
                   "HUMANITIES and RELIGION::Religion/Theology::Church studies"),
           VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:VR110104",
                   "Missionary studies",
                   "HUMANITIES and RELIGION::Religion/Theology::Missionary studies"),
           VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:VR110105",
                   "Systematic theology",
                   "HUMANITIES and RELIGION::Religion/Theology::Systematic theology"),
           VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:VR110106",
                   "Islamology",
                   "HUMANITIES and RELIGION::Religion/Theology::Islamology"),
           VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:VR110107",
                   "Faith and reason",
                   "HUMANITIES and RELIGION::Religion/Theology::Faith and reason"),
           VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:VR110108",
                   "Sociology of religion",
                   "HUMANITIES and RELIGION::Religion/Theology::Sociology of religion"),
           VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:VR110109",
                   "Psychology of religion",
                   "HUMANITIES and RELIGION::Religion/Theology::Psychology of religion"),
           VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:VR110110",
                   "Philosophy of religion",
                   "HUMANITIES and RELIGION::Religion/Theology::Philosophy of religion"),
           VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:VR110111",
                   "New Testament exegesis",
                   "HUMANITIES and RELIGION::Religion/Theology::New Testament exegesis"),
           VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:VR110112",
                   "Old Testament exegesis",
                   "HUMANITIES and RELIGION::Religion/Theology::Old Testament exegesis"),
           VocabularyEntryDetailsMatcher.matchAuthorityEntry("srsc:VR110113",
                   "Dogmatics with symbolics",
                   "HUMANITIES and RELIGION::Religion/Theology::Dogmatics with symbolics")
          );
    }

    @Test
    public void vocabularyEntryDetailSerchMethodWithSingleModelTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/submission/vocabularyEntryDetail/search"))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void vocabularyEntryDetailSerchMethodWithPluralModelTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/submission/vocabularyEntryDetails/search"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._links.top.href", Matchers.allOf(
                                 Matchers.containsString("/api/submission/vocabularyEntryDetails/search/top"))));
    }
}
