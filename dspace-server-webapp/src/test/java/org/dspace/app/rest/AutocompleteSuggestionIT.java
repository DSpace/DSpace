/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.matcher.SubmissionFormFieldMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.discovery.SolrSearchCore;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class AutocompleteSuggestionIT extends AbstractControllerIntegrationTest {
    // Items with subject metadata, commit, test REST method for expected results
    // Test test submission for expected vocabulary type
    @Autowired
    private SolrSearchCore solrSearchCore;

    Logger log = LogManager.getLogger(this.getClass());

    @Test
    public void findFieldWithValuePairsConfig() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        String submissionFormName = "vocabulary-suggest-test";

        getClient(token).perform(get("/api/config/submissionforms/" + submissionFormName))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.id", is(submissionFormName)))
                .andExpect(jsonPath("$.name", is(submissionFormName)))
                .andExpect(jsonPath("$.type", is("submissionform")))
                .andExpect(jsonPath("$._links.self.href", Matchers
                        .startsWith(REST_SERVER_URL + "config/submissionforms/" + submissionFormName)))
                .andExpect(jsonPath("$.rows[0].fields", contains(
                        SubmissionFormFieldMatcher.matchFormFieldDefinition("onebox", "Subject", null,
                                null, true,
                                "Enter appropriate subject keywords or phrases.",
                                null, "dc.subject", "subject", "suggest")
                )))
        ;
    }

    @Test
    public void findSubjectTermSuggestions() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .withAuthor("Doe, John")
                .withSubject("Totora Grove")
                .withSubject("Punga Grove")
                .withSubject("Kowhai Flower")
                .withSubject("Flower Arranging")
                .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Expect two terms for "grove"
        getClient(token).perform(get("/api/discover/suggest")
                        .queryParam("dict", "subject")
                        .queryParam("q", "grove")
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.suggest.subject.grove.numFound", is(2)))
                .andExpect(jsonPath("$.suggest.subject.grove.suggestions[*].term",
                        containsInAnyOrder("Punga <b>Grove</b>", "Totora <b>Grove</b>"
                        )));

        // Expect zero terms for "test"
        getClient(token).perform(get("/api/discover/suggest")
                        .queryParam("dict", "subject")
                        .queryParam("q", "test")
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.suggest.subject.test.numFound", is(0)));
    }

}
