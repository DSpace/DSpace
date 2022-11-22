/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.BrowseIndexMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.services.ConfigurationService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test to test the /api/discover/browselinks endpoint
 *
 * @author Kim Shepherd
 */
public class BrowseLinkRestRepositoryIT extends AbstractControllerIntegrationTest {
    @Autowired
    ConfigurationService configurationService;

    @Autowired
    MetadataAuthorityService metadataAuthorityService;

    /**
     * Expect a single author browse definition
     * @throws Exception
     */
    @Test
    public void findOne() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/api/discover/browselinks/dc.contributor.author"))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))

                //The array of browse index should have a size 1
                .andExpect(jsonPath("$.id", is("author")))

                //Check that all (and only) the default browse indexes are present
                .andExpect(jsonPath("$.metadataBrowse", is(true)))
        ;
    }

    /**
     * Expect a list of browse definitions that are also configured for link rendering
     * @throws Exception
     */
    @Test
    public void findAll() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/api/discover/browselinks"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))

                   // Expect TWO results, author and browse (see dspace-api test data local.cfg_
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(2)))
                   .andExpect(jsonPath("$.page.totalPages", is(1)))
                   .andExpect(jsonPath("$.page.number", is(0)))

                   //The array of browse index should have a size 2
                   .andExpect(jsonPath("$._embedded.browses", hasSize(2)))

                   //Check that all (and only) the default browse indexes are present
                   .andExpect(jsonPath("$._embedded.browses", containsInAnyOrder(
                       BrowseIndexMatcher.contributorBrowseIndex("asc"),
                       BrowseIndexMatcher.subjectBrowseIndex("asc")
                   )))
        ;
    }



}
