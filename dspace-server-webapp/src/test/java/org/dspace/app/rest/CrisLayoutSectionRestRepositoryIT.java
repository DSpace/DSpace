/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withBrowseComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withFacetComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withIdAndBrowseComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withIdAndFacetComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withIdAndSearchComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withIdAndTopComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withSearchComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withTopComponent;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;

/**
 * Integration tests for {@link CrisLayoutSectionRestRepository}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CrisLayoutSectionRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void testFindAll() throws Exception {

        String[] expectedBrowseNames = new String[] { "rodept", "author", "title", "type", "dateissued", "subject" };

        getClient().perform(get("/api/layout/sections"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.sections", hasSize(4)))

            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndBrowseComponent("publications", 0, 0, "col-md-4", expectedBrowseNames))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndSearchComponent("publications", 0, 1, "col-md-8", "publication"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndTopComponent("publications", 1, 0, "col-md-6",
                    "publication", "dc.date.accessioned", "desc"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndTopComponent("publications", 1, 1, "col-md-6", "publication", "dc.title", "asc"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndFacetComponent("publications", 2, 0, "col-md-12", "publication"))))

            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndBrowseComponent("researcherprofiles", 0, 0, "col-md-4", "rpname", "rpdept"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndSearchComponent("researcherprofiles", 0, 1, "col-md-8", "person"))))

            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndBrowseComponent("fundings", 0, 0, "col-md-4", "pjtitle"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndSearchComponent("fundings", 0, 1, "col-md-8", "project"))))

            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndBrowseComponent("orgunits", 0, 0, "col-md-4", "ouname"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndSearchComponent("orgunits", 0, 1, "col-md-8", "organization"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndFacetComponent("orgunits", 1, 0, "col-md-12", "organization"))));
    }

    @Test
    public void testFindOne() throws Exception {

        String[] expectedBrowseNames = new String[] { "rodept", "author", "title", "type", "dateissued", "subject" };

        getClient().perform(get("/api/layout/sections/{id}", "publications"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is("publications")))
            .andExpect(jsonPath("$", withBrowseComponent(0, 0, "col-md-4", expectedBrowseNames)))
            .andExpect(jsonPath("$", withSearchComponent(0, 1, "col-md-8", "publication")))
            .andExpect(jsonPath("$", withTopComponent(1, 0, "col-md-6", "publication", "dc.date.accessioned", "desc")))
            .andExpect(jsonPath("$", withTopComponent(1, 1, "col-md-6", "publication", "dc.title", "asc")))
            .andExpect(jsonPath("$", withFacetComponent(2, 0, "col-md-12", "publication")));
    }

    @Test
    public void testFindOneWithUnknownSectionId() throws Exception {

        getClient().perform(get("/api/layout/sections/{id}", "unknown-section-id"))
            .andExpect(status().isNotFound());
    }
}
