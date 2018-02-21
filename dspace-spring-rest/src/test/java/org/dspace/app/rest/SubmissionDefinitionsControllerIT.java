/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.matcher.SubmissionDefinitionsMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

/**
 * Integration test to test the /api/config/submissiondefinitions endpoint
 * (Class has to start or end with IT to be picked up by the failsafe plugin)
 */
public class SubmissionDefinitionsControllerIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAll() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/api/config/submissiondefinitions"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))

                   //By default we expect at least 1 submission definition so this to be reflected in the page object
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(1)))
                   .andExpect(jsonPath("$.page.totalPages", greaterThanOrEqualTo(1)))
                   .andExpect(jsonPath("$.page.number", is(0)))
                   .andExpect(
                       jsonPath("$._links.search.href", is(REST_SERVER_URL + "config/submissiondefinitions/search")))

                   //The array of browse index should have a size greater or equals to 1
                   .andExpect(jsonPath("$._embedded.submissiondefinitions", hasSize(greaterThanOrEqualTo(1))))
        ;
    }

    @Test
    public void findDefault() throws Exception {
        getClient().perform(get("/api/config/submissiondefinitions/traditional"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))

                   //Check that the JSON root matches the expected "traditional" submission definition
                   .andExpect(jsonPath("$", SubmissionDefinitionsMatcher
                       .matchSubmissionDefinition(true, "traditional", "traditional")))
        ;
    }

    @Test
    public void findByCollection() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        getClient().perform(get("/api/config/submissiondefinitions/search/findByCollection")
                                .param("uuid", col1.getID().toString()))
                   //** THEN **
                   //The status has to be 200
                   .andExpect(status().isOk())
                   .andDo(MockMvcResultHandlers.print())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", SubmissionDefinitionsMatcher
                       .matchSubmissionDefinition(true, "traditional", "traditional")));
    }

    @Test
    public void findCollections() throws Exception {
        //Match only that a section exists with a submission configuration behind
        getClient().perform(get("/api/config/submissiondefinitions/traditional/collections"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk());
    }

    @Test
    public void findSections() throws Exception {
        getClient().perform(get("/api/config/submissiondefinitions/traditional/sections"))
                   // The status has to be 200 OK
                   .andExpect(status().isOk())
                   // We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))
                   // Match only that a section exists with a submission configuration behind
                   .andExpect(jsonPath("$._embedded.submissionsections", hasSize(5)))
                   .andExpect(jsonPath("$._embedded.submissionsections",
                                       Matchers.hasItem(
                                           allOf(
                                               hasJsonPath("$.id", is("traditionalpageone")),
                                               hasJsonPath("$.sectionType", is("submission-form")),
                                               hasJsonPath("$.type", is("submissionsection")),
                                               hasJsonPath("$._links.config.href",
                                                           is(REST_SERVER_URL +
                                                                  "config/submissionforms/traditionalpageone")),
                                               hasJsonPath("$._links.self.href",
                                                           is(REST_SERVER_URL +
                                                                  "config/submissionsections/traditionalpageone"))
                                           ))))
        ;
    }
}