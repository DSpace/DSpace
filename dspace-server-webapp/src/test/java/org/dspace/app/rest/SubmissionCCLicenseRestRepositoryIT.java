/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.SubmissionCCLicenseMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Class to the methods from the SubmissionCCLicenseRestRepository
 * Since the CC Licenses are obtained from the CC License API, a mock service has been implemented
 * This mock service will return a fixed set of CC Licenses using a similar structure to the ones obtained from the
 * CC License API.
 * Refer to {@link org.dspace.license.MockCCLicenseConnectorServiceImpl} for more information
 */
public class SubmissionCCLicenseRestRepositoryIT extends AbstractControllerIntegrationTest {


    /**
     * Test the findAll method form the SubmissionCCLicenseRestRepository
     *
     * @throws Exception
     */
    @Test
    public void findAllTest() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get("/api/config/submissioncclicenses"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.submissioncclicenses", Matchers.containsInAnyOrder(
                           SubmissionCCLicenseMatcher.matchLicenseEntry(1, new int[]{3, 2, 3}),
                           SubmissionCCLicenseMatcher.matchLicenseEntry(2, new int[]{2}),
                           SubmissionCCLicenseMatcher.matchLicenseEntry(3, new int[]{})
                   )));
    }

    @Test
    public void findOneTest() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get("/api/config/submissioncclicenses/license1"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.is(
                           SubmissionCCLicenseMatcher.matchLicenseEntry(1, new int[]{3, 2, 3})
                   )));
    }

    @Test
    public void findOneTestNonExistingLicense() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get("/api/config/submissioncclicenses/non-existing-license"))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void findAllTestUnAuthorized() throws Exception {
        getClient().perform(get("/api/config/submissioncclicenses"))
                               .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneTestUnAuthorized() throws Exception {

        getClient().perform(get("/api/config/submissioncclicenses/license1"))
                               .andExpect(status().isUnauthorized());
    }

}
