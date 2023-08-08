/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.repository.ItemFilterRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;

/**
 * Integration test for {@link ItemFilterRestRepository}
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class ItemFilterRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void findOneTest() throws Exception {
        getClient()
            .perform(get("/api/config/itemfilters/test"))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void findAllPaginatedSortedTest() throws Exception {
        getClient().perform(get("/api/config/itemfilters")
                       .param("size", "30"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(21)))
                   .andExpect(jsonPath("$.page.totalPages", is(1)))
                   .andExpect(jsonPath("$.page.size", is(30)))
                   .andExpect(jsonPath("$._embedded.itemfilters", contains(
                       hasJsonPath("$.id", is("a-common-or_statement")),
                       hasJsonPath("$.id", is("always_true_filter")),
                       hasJsonPath("$.id", is("dc-identifier-uri-contains-doi_condition")),
                       hasJsonPath("$.id", is("demo_filter")),
                       hasJsonPath("$.id", is("doi-filter")),
                       hasJsonPath("$.id", is("driver-document-type_condition")),
                       hasJsonPath("$.id", is("example-doi_filter")),
                       hasJsonPath("$.id", is("has-at-least-one-bitstream_condition")),
                       hasJsonPath("$.id", is("has-bitstream_filter")),
                       hasJsonPath("$.id", is("has-one-bitstream_condition")),
                       hasJsonPath("$.id", is("in-outfit-collection_condition")),
                       hasJsonPath("$.id", is("is-archived_condition")),
                       hasJsonPath("$.id", is("is-withdrawn_condition")),
                       hasJsonPath("$.id", is("item-is-public_condition")),
                       hasJsonPath("$.id", is("openaire_filter")),
                       hasJsonPath("$.id", is("simple-demo_filter")),
                       hasJsonPath("$.id", is("title-contains-demo_condition")),
                       hasJsonPath("$.id", is("title-starts-with-pattern_condition")),
                       hasJsonPath("$.id", is("type-equals-dataset_condition")),
                       hasJsonPath("$.id", is("type-equals-journal-article_condition")),
                       hasJsonPath("$.id", is("type_filter")))));
    }
}