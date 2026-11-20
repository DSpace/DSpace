/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;

/**
 * Integration test to test the /api/config/submissionforms endpoint
 * (Class has to start or end with IT to be picked up by the failsafe plugin)
 */
public class SubmissionUploadsControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private MultipartProperties multipartProperties;

    @Test
    public void findAll() throws Exception {
        //When we call the root endpoint as anonymous user
        getClient().perform(get("/api/config/submissionuploads"))
                   //The status has to be 403 Not Authorized
                   .andExpect(status().isUnauthorized());


        String token = getAuthToken(admin.getEmail(), password);

        //When we call the root endpoint
        getClient(token).perform(get("/api/config/submissionuploads"))
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
                                       Matchers.startsWith(REST_SERVER_URL + "config/submissionuploads")))

                   //The array of browse index should have a size greater or equals to 1
                   .andExpect(jsonPath("$._embedded.submissionuploads", hasSize(greaterThanOrEqualTo(1))))
        ;
    }

    @Test
    public void findAllWithNewlyCreatedAccountTest() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/config/submissionuploads"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(1)))
                   .andExpect(jsonPath("$.page.totalPages", greaterThanOrEqualTo(1)))
                   .andExpect(jsonPath("$.page.number", is(0)))
                   .andExpect(jsonPath("$._links.self.href",
                                       Matchers.startsWith(REST_SERVER_URL + "config/submissionuploads")))
                   .andExpect(jsonPath("$._embedded.submissionuploads", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    public void maxSizeIsTheConfiguredLimitCappedByTheMultipartLimit() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        int multipartLimit = (int) multipartProperties.getMaxFileSize().toBytes();
        try {
            // Without upload.max the servlet container's multipart limit is the effective limit
            configurationService.setProperty("upload.max", null);
            getClient(token).perform(get("/api/config/submissionuploads/upload"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.maxSize", is(multipartLimit)));

            // A smaller upload.max applies as is
            configurationService.setProperty("upload.max", 1024);
            getClient(token).perform(get("/api/config/submissionuploads/upload"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.maxSize", is(1024)));

            // A larger upload.max can never exceed what the container accepts
            configurationService.setProperty("upload.max", 2L * multipartLimit);
            getClient(token).perform(get("/api/config/submissionuploads/upload"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.maxSize", is(multipartLimit)));
        } finally {
            configurationService.setProperty("upload.max", null);
        }
    }
}
