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
 * Since the CC Licenses and the corresponding URIs are obtained from the CC License API, a mock service has been
 * implemented.
 * This mock service will return a fixed set of CC Licenses using a similar structure to the ones obtained from the
 * CC License API.
 * Refer to {@link org.dspace.license.MockCCLicenseConnectorServiceImpl} for more information
 */
public class SubmissionCCLicenseUrlRepositoryIT extends AbstractControllerIntegrationTest {


    @Test
    public void searchRightsByQuestionsTest() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get(
                "/api/config/submissioncclicenseUrls/search/rightsByQuestions?license=license2&answer_license2-field0" +
                        "=license2-field0-enum1"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.url", is("mock-license-uri")))
                   .andExpect(jsonPath("$.type", is("submissioncclicenseUrl")))
                   .andExpect(jsonPath("$._links.self.href",
                                       is("http://localhost/api/config/submissioncclicenseUrls/search/rightsByQuestions" +
                                                  "?license=license2" +
                                                  "&answer_license2-field0=license2-field0-enum1")));
    }

    @Test
    public void searchRightsByQuestionsTestLicenseForLicenseWithoutQuestions() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken)
                .perform(get("/api/config/submissioncclicenseUrls/search/rightsByQuestions?license=license3"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.url", is("mock-license-uri")))
                   .andExpect(jsonPath("$.type", is("submissioncclicenseUrl")))
                   .andExpect(jsonPath("$._links.self.href",
                                       is("http://localhost/api/config/submissioncclicenseUrls/search/rightsByQuestions" +
                                                  "?license=license3")));
    }

    @Test
    public void searchRightsByQuestionsNonExistingLicense() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get(
                "/api/config/submissioncclicenseUrls/search/rightsByQuestions?license=nonexisting-license" +
                        "&answer_license2-field0=license2-field0-enum1"))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void searchRightsByQuestionsMissingRequiredAnswer() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get(
                "/api/config/submissioncclicenseUrls/search/rightsByQuestions?license=license1&answer_license1field0" +
                        "=license1field0enum1"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void searchRightsByQuestionsAdditionalNonExistingAnswer() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get(
                "/api/config/submissioncclicenseUrls/search/rightsByQuestions?license=license2" +
                        "&answer_license2field0=license2field0enum1&answer_nonexisting=test"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void searchRightsByQuestionsAdditionalUnAuthorized() throws Exception {

        getClient().perform(get(
                "/api/config/submissioncclicenseUrls/search/rightsByQuestions?license=license2&answer_license2-field0" +
                        "=license2-field0-enum1"))
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
