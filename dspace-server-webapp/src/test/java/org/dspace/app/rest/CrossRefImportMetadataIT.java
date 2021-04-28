/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class CrossRefImportMetadataIT extends AbstractControllerIntegrationTest {

    @Test
    public void importMetadataFromCrossrefByDoiTest() throws Exception {

         getClient().perform(get("/api/integration/externalsources/crossref/entries")
                    .param("query", "10.1111/jfbc.13557"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.externalSourceEntries[0].id", is("10.1111/jfbc.13557")))
                    .andExpect(jsonPath("$._embedded.externalSourceEntries[0].display",
                                     is("Food‐derived antioxidants and COVID‐19")))
                    .andExpect(jsonPath("$.page.totalElements", is(1)));

    }

    @Test
    public void importMetadataFromCrossrefByFreeTextTest() throws Exception {

         getClient().perform(get("/api/integration/externalsources/crossref/entries")
                    .param("query", "1.11/jfbc.1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.totalElements", greaterThan(1)));

    }

}