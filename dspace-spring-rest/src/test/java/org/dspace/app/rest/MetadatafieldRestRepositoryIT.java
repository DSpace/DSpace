/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MetadatafieldRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAll() throws Exception{

        getClient().perform(get("/api/core/metadatafields"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(contentType))
                    .andExpect(jsonPath("$._embedded.metadatafields[0].type", Matchers.is("metadatafield")))
                    .andExpect(jsonPath("$._links.first.href", Matchers.containsString("/api/core/metadatafields")))
                    .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/metadatafields")))
                    .andExpect(jsonPath("$._links.next.href", Matchers.containsString("/api/core/metadatafields")))
                    .andExpect(jsonPath("$._links.last.href", Matchers.containsString("/api/core/metadatafields")));
    }
}
