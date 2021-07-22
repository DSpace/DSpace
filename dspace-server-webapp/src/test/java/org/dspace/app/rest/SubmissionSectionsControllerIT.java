/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static java.util.Map.of;
import static org.dspace.app.rest.matcher.SubmissionSectionMatcher.matches;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Integration test to test the /api/config/submissionforms endpoint
 * (Class has to start or end with IT to be picked up by the failsafe plugin)
 */
public class SubmissionSectionsControllerIT extends AbstractControllerIntegrationTest {

    @Test
    public void testFindAll() throws Exception {
        //When we call the root endpoint as anonymous user
        getClient().perform(get("/api/config/submissionsections"))
                   //The status has to be 403 Not Authorized
                   .andExpect(status().isUnauthorized());


        String token = getAuthToken(eperson.getEmail(), password);

        //When we call the root endpoint
        getClient(token).perform(get("/api/config/submissionsections"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))

                   //By default we expect at least 1 submission forms so this to be reflected in the page object
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(1)))
                   .andExpect(jsonPath("$.page.totalPages", greaterThanOrEqualTo(1)))
                   .andExpect(jsonPath("$.page.number", is(0)))
                   .andExpect(jsonPath("$._links.self.href",
                                       Matchers.startsWith(REST_SERVER_URL + "config/submissionsections")))

                   //The array of browse index should have a size greater or equals to 1
                   .andExpect(jsonPath("$._embedded.submissionsections", hasSize(greaterThanOrEqualTo(1))
                   ))
        ;
    }

    @Test
    public void testFindOne() throws Exception {

        getClient().perform(get("/api/config/submissionsections/collection"))
            .andExpect(status().isUnauthorized());

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/config/submissionsections/collection"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", matches("collection", true, "collection",
                of("submission", "hidden", "workflow", "hidden", "edit", "hidden"))));

        getClient(token).perform(get("/api/config/submissionsections/traditionalpageone"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", matches("traditionalpageone", true, "submission-form")));

        getClient(token).perform(get("/api/config/submissionsections/traditionalpagethree-cris-open"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", matches("traditionalpagethree-cris-open", true, "submission-form", true)));
    
        getClient(token).perform(get("/api/config/submissionsections/traditionalpagethree-cris-collapsed"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", matches("traditionalpagethree-cris-collapsed", true, "submission-form", false)));

        getClient(token).perform(get("/api/config/submissionsections/license"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", matches("license", true, "license",
                of("workflow", "read-only", "edit", "read-only"))));
    }

}