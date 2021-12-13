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

import org.dspace.app.rest.matcher.AccessConditionOptionMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Integration test class for the submissionAccessOptions endpoint.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class SubmissionAccessOptionRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAllTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/config/submissionaccessoptions"))
                            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void findOneTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/config/submissionaccessoptions/defaultAC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("defaultAC")))
                .andExpect(jsonPath("$.discoverable", is(true)))
                .andExpect(jsonPath("$.accessConditionOptions", Matchers.containsInAnyOrder(
                    AccessConditionOptionMatcher.matchAccessConditionOption(
                          "openaccess","Anonymous", false , false, null, null),
                    AccessConditionOptionMatcher.matchAccessConditionOption(
                          "embargo","Anonymous", true , false, "+36MONTHS", null),
                    AccessConditionOptionMatcher.matchAccessConditionOption(
                          "administrator","Administrator", false , false, null, null))
                    ))
                .andExpect(jsonPath("$.type", is("submissionaccessoption")));


        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(get("/api/config/submissionaccessoptions/defaultAC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("defaultAC")))
                .andExpect(jsonPath("$.discoverable", is(true)))
                .andExpect(jsonPath("$.accessConditionOptions", Matchers.containsInAnyOrder(
                    AccessConditionOptionMatcher.matchAccessConditionOption(
                          "openaccess","Anonymous", false , false, null, null),
                    AccessConditionOptionMatcher.matchAccessConditionOption(
                          "embargo","Anonymous", true , false, "+36MONTHS", null),
                    AccessConditionOptionMatcher.matchAccessConditionOption(
                          "administrator","Administrator", false , false, null, null))
                    ))
                .andExpect(jsonPath("$.type", is("submissionaccessoption")));

        getClient().perform(get("/api/config/submissionaccessoptions/defaultAC"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.id", is("defaultAC")))
                   .andExpect(jsonPath("$.discoverable", is(true)))
                   .andExpect(jsonPath("$.accessConditionOptions", Matchers.containsInAnyOrder(
                       AccessConditionOptionMatcher.matchAccessConditionOption(
                             "openaccess","Anonymous", false , false, null, null),
                       AccessConditionOptionMatcher.matchAccessConditionOption(
                             "embargo","Anonymous", true , false, "+36MONTHS", null),
                       AccessConditionOptionMatcher.matchAccessConditionOption(
                             "administrator","Administrator", false , false, null, null))
                       ))
                   .andExpect(jsonPath("$.type", is("submissionaccessoption")));
    }

    @Test
    public void findOneNotFoundTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/config/submissionaccessoptions/" + UUID.randomUUID()))
                            .andExpect(status().isNotFound());
    }

}