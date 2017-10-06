/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.hamcrest.Matcher;
import org.junit.Test;

/**
 * Integration test to test the /api/discover/browses endpoint
 */
public class BrowsesResourceControllerTest extends AbstractControllerIntegrationTest {

    @Test
    public void findAll() throws Exception {
        //When we call the root endpoint
        mockMvc.perform(get("/api/discover/browses"))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))

                //Our default Discovery config has 4 browse indexes so we expect this to be reflected in the page object
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(4)))
                .andExpect(jsonPath("$.page.totalPages", is(1)))
                .andExpect(jsonPath("$.page.number", is(0)))

                //The array of browse index should have a size 4
                .andExpect(jsonPath("$._embedded.browses", hasSize(4)))

                //Check that all (and only) the default browse indexes are present
                .andExpect(jsonPath("$._embedded.browses", containsInAnyOrder(
                        dateIssuedBrowseIndex("asc"),
                        contributorBrowseIndex("asc"),
                        titleBrowseIndex("asc"),
                        subjectBrowseIndex("asc")
                )))
        ;
    }

    private Matcher<? super Object> subjectBrowseIndex(final String order) {
        return allOf(
                hasJsonPath("$.metadata", contains("dc.subject.*")),
                hasJsonPath("$.metadataBrowse", is(true)),
                hasJsonPath("$.order", equalToIgnoringCase(order)),
                hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned"))
        );
    }

    private Matcher<? super Object> titleBrowseIndex(final String order) {
        return allOf(
                hasJsonPath("$.metadata", contains("dc.title")),
                hasJsonPath("$.metadataBrowse", is(false)),
                hasJsonPath("$.order", equalToIgnoringCase(order)),
                hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned"))
        );
    }

    private Matcher<? super Object> contributorBrowseIndex(final String order) {
        return allOf(
                hasJsonPath("$.metadata", contains("dc.contributor.*", "dc.creator")),
                hasJsonPath("$.metadataBrowse", is(true)),
                hasJsonPath("$.order", equalToIgnoringCase(order)),
                hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned"))
        );
    }

    private Matcher<? super Object> dateIssuedBrowseIndex(final String order) {
        return allOf(
                hasJsonPath("$.metadata", contains("dc.date.issued")),
                hasJsonPath("$.metadataBrowse", is(false)),
                hasJsonPath("$.order", equalToIgnoringCase(order)),
                hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned"))
        );
    }

}