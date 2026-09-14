/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Class to the methods from the SubmissionCCLicenseUrlRepository
 * CC Licenses from questions are now gotten from index.rdf and
 * return a document that would be put into the cc license bundle
 */
public class SubmissionCCLicenseUrlRepositoryIT extends AbstractControllerIntegrationTest {


    @Test
    public void searchRightsByQuestionsTest() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get(
                "/api/config/submissioncclicenseUrls/search/rightsByQuestions?license=license1&answer_commercial=n" +
                        "&answer_derivatives=sa&answer_jurisdiction=us"))
                   .andExpect(status().isOk());
    }

    @Test
    public void searchRightsByQuestionsTestLicenseForLicenseWithoutQuestions() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken)
                .perform(get("/api/config/submissioncclicenseUrls/search/rightsByQuestions?license=license271"))
                   .andExpect(status().isOk());
    }

    @Test
    public void searchRightsByQuestionsNonExistingLicense() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get(
                "/api/config/submissioncclicenseUrls/search/rightsByQuestions?license=nonexisting-license" +
                        "&answer_derivatives=sa&answer_jurisdiction=us"))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void searchRightsByQuestionsMissingRequiredAnswer() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get(
                "/api/config/submissioncclicenseUrls/search/rightsByQuestions?license=license1&answer_commercial=n" +
                        "&answer_jurisdiction=us"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void searchRightsByQuestionsAdditionalNonExistingAnswer() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get(
                "/api/config/submissioncclicenseUrls/search/rightsByQuestions?license=license1&answer_commercial=n" +
                        "&answer_derivatives=sa&answer_jurisdiction=us&answer_fake=fake"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void searchRightsByQuestionsAdditionalUnAuthorized() throws Exception {

        getClient().perform(get(
                "/api/config/submissioncclicenseUrls/search/rightsByQuestions?license=license1&answer_commercial=n" +
                        "&answer_derivatives=sa&answer_jurisdiction=us"))
                               .andExpect(status().isUnauthorized());

    }

    @Test
    public void submissionCCLicenseUrlSerchMethodWithSingleModelTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/config/submissioncclicenseUrl/search"))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void submissionCCLicenseUrlSerchMethodWithPluralModelTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/config/submissioncclicenseUrls/search"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._links.rightsByQuestions.href", Matchers.allOf(Matchers
                                 .containsString("/api/config/submissioncclicenseUrls/search/rightsByQuestions"))));
    }

    @Test
    public void discoverableNestedLinkTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._links",Matchers.allOf(
                                hasJsonPath("$.submissioncclicenseUrls.href",
                                         is("http://localhost/api/config/submissioncclicenseUrls")),
                                hasJsonPath("$.submissioncclicenseUrls-search.href",
                                         is("http://localhost/api/config/submissioncclicenseUrls/search"))
                        )));
    }

}
