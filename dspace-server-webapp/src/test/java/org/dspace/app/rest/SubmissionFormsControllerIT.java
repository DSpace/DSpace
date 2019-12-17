/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.SubmissionFormFieldMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Integration test to test the /api/config/submissionforms endpoint
 * (Class has to start or end with IT to be picked up by the failsafe plugin)
 */
public class SubmissionFormsControllerIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAll() throws Exception {
        //When we call the root endpoint as anonymous user
        getClient().perform(get("/api/config/submissionforms"))
                   //The status has to be 403 Not Authorized
                   .andExpect(status().isUnauthorized());


        String token = getAuthToken(admin.getEmail(), password);

        //When we call the root endpoint
        getClient(token).perform(get("/api/config/submissionforms"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))
                   //The configuration file for the test env includes 3 forms
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", equalTo(4)))
                   .andExpect(jsonPath("$.page.totalPages", equalTo(1)))
                   .andExpect(jsonPath("$.page.number", is(0)))
                   .andExpect(
                       jsonPath("$._links.self.href", Matchers.startsWith(REST_SERVER_URL + "config/submissionforms")))
                   //The array of submissionforms should have a size of 3
                   .andExpect(jsonPath("$._embedded.submissionforms", hasSize(equalTo(4))))
        ;
    }

    @Test
    public void findTraditionalPageOne() throws Exception {
        //When we call the root endpoint as anonymous user
        getClient().perform(get("/api/config/submissionforms/traditionalpageone"))
                   //The status has to be 403 Not Authorized
                   .andExpect(status().isUnauthorized());

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/config/submissionforms/traditionalpageone"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))
                   //Check that the JSON root matches the expected "traditionalpageone" input forms
                   .andExpect(jsonPath("$.id", is("traditionalpageone")))
                   .andExpect(jsonPath("$.name", is("traditionalpageone")))
                   .andExpect(jsonPath("$.type", is("submissionform")))
                   .andExpect(jsonPath("$._links.self.href", Matchers
                       .startsWith(REST_SERVER_URL + "config/submissionforms/traditionalpageone")))
                   // check the first two rows
                   .andExpect(jsonPath("$.rows[0].fields", contains(
                        SubmissionFormFieldMatcher.matchFormFieldDefinition("lookup-name", "Author",
                null, true, "Enter the names of the authors of this item.", "dc.contributor.author"))))
                   .andExpect(jsonPath("$.rows[1].fields", contains(
                        SubmissionFormFieldMatcher.matchFormFieldDefinition("onebox", "Title",
                                "You must enter a main title for this item.", false,
                                "Enter the main title of the item.", "dc.title"))))
                   // check a row with multiple fields
                   .andExpect(jsonPath("$.rows[3].fields",
                        contains(
                                SubmissionFormFieldMatcher.matchFormFieldDefinition("date", "Date of Issue",
                                        "You must enter at least the year.", false,
                                        "Please give the date", "col-sm-4",
                                        "dc.date.issued"),
                                SubmissionFormFieldMatcher.matchFormFieldDefinition("onebox", "Publisher",
                                        null, false,"Enter the name of",
                                        "col-sm-8","dc.publisher"))))
        ;
    }
}
