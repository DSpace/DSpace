/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package com.atmire.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;

public class WbVocabularyEntryDetailsIT extends AbstractControllerIntegrationTest {

    @Test
    public void testCustomWbSyntax() throws Exception {

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token)
            .perform(get("/api/submission/vocabularyEntryDetails/topic:Information_Technology"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.value", is(
                "Information and Communication Technologies :: Information Technology"
            )));

        getClient(token)
            .perform(get("/api/submission/vocabularies/topic/entries")
                .param("filter", "Information and Communication Technologies :: Information Technology")
                .param("exact", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries[0].value", is(
                "Information and Communication Technologies :: Information Technology"
            )));

        getClient(token)
            .perform(get("/api/submission/vocabularyEntryDetails/search/top?vocabulary=topic"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.vocabularyEntryDetails", everyItem(
                hasJsonPath("$.value", not(startsWith("Topics:")))
            )));
    }
}
