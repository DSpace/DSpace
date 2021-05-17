/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.dspace.app.rest.matcher.ExternalSourceEntryMatcher;
import org.dspace.app.rest.matcher.ExternalSourceMatcher;
import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ExternalSourcesRestControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void findAllExternalSources() throws Exception {
        getClient().perform(get("/api/integration/externalsources"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.externalsources", Matchers.hasItems(
                ExternalSourceMatcher.matchExternalSource("mock", "mock", false),
                ExternalSourceMatcher.matchExternalSource("orcid", "orcid", false),
                ExternalSourceMatcher.matchExternalSource("sherpaJournalIssn", "sherpaJournalIssn", false),
                ExternalSourceMatcher.matchExternalSource("sherpaJournal", "sherpaJournal", false),
                ExternalSourceMatcher.matchExternalSource("sherpaPublisher", "sherpaPublisher", false),
                ExternalSourceMatcher.matchExternalSource("pubmed", "pubmed", false))))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(14)));
    }

    @Test
    public void findOneExternalSourcesExistingSources() throws Exception {
        getClient().perform(get("/api/integration/externalsources/mock"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       ExternalSourceMatcher.matchExternalSource("mock", "mock", false)
                   )));
    }
    @Test
    public void findOneExternalSourcesNotExistingSources() throws Exception {
        getClient().perform(get("/api/integration/externalsources/mocktwo"))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void findOneExternalSourceEntryValue() throws Exception {
        getClient().perform(get("/api/integration/externalsources/mock/entryValues/one"))
                   .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(
                        ExternalSourceEntryMatcher.matchExternalSourceEntry("one", "one", "one", "mock")
                    )));
    }

    @Test
    public void findOneExternalSourceEntryValueInvalidEntryId() throws Exception {
        getClient().perform(get("/api/integration/externalsources/mock/entryValues/entryIdInvalid"))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void findOneExternalSourceEntryValueInvalidSource() throws Exception {
        getClient().perform(get("/api/integration/externalsources/mocktwo/entryValues/one"))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void findOneExternalSourceEntriesInvalidSource() throws Exception {
        getClient().perform(get("/api/integration/externalsources/mocktwo/entries")
                                .param("query", "test"))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void findOneExternalSourceEntriesApplicableQuery() throws Exception {
        getClient().perform(get("/api/integration/externalsources/mock/entries")
                                .param("query", "one"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.externalSourceEntries", Matchers.containsInAnyOrder(
                       ExternalSourceEntryMatcher.matchExternalSourceEntry("one", "one", "one", "mock"),
                       ExternalSourceEntryMatcher.matchExternalSourceEntry("onetwo", "onetwo", "onetwo", "mock")
                   )))
                    .andExpect(jsonPath("$.page", PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2)));
    }

    @Test
    public void findOneExternalSourceEntriesApplicableQueryPagination() throws Exception {
        getClient().perform(get("/api/integration/externalsources/mock/entries")
                                .param("query", "one").param("size", "1"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.externalSourceEntries", Matchers.hasItem(
                       ExternalSourceEntryMatcher.matchExternalSourceEntry("onetwo", "onetwo", "onetwo", "mock")
                   )))
                   .andExpect(jsonPath("$.page", PageMatcher.pageEntryWithTotalPagesAndElements(0, 1, 2, 2)));

        getClient().perform(get("/api/integration/externalsources/mock/entries")
                                .param("query", "one").param("size", "1").param("page", "1"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.externalSourceEntries", Matchers.hasItem(
                       ExternalSourceEntryMatcher.matchExternalSourceEntry("one", "one", "one", "mock")
                   )))
                   .andExpect(jsonPath("$.page", PageMatcher.pageEntryWithTotalPagesAndElements(1, 1, 2, 2)));
    }

    @Test
    public void findOneExternalSourceEntriesNoReturnQuery() throws Exception {
        getClient().perform(get("/api/integration/externalsources/mock/entries")
                                .param("query", "randomqueryfornoresults"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void findOneExternalSourceEntriesNoQuery() throws Exception {
        getClient().perform(get("/api/integration/externalsources/mock/entries"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void testAuthorityImportDataProviderExternalSource() throws Exception {
        context.turnOffAuthorisationSystem();
        configurationService.setProperty("choises.externalsource.dc.contributor.author", "authorAuthority");
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withWorkflowGroup(1, admin).build();

        //2. a workflow item
        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withAuthorAffiliation("Affiliation one").withAuthorAffiliation("Affiliation two")
                .withSubject("ExtraEntry")
                .build();

        UUID itemUUID = witem.getItem().getID();

        context.restoreAuthSystemState();

        String exteranlSourceId = UUIDUtils.toString(itemUUID) + ":0";
        getClient().perform(get("/api/integration/externalsources/authorAuthority/entryValues/" + exteranlSourceId))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                           ExternalSourceEntryMatcher.matchItemWithGivenMetadata("dc.title",
                                   "Smith, Donald", "0"))))
                   .andExpect(jsonPath("$", Matchers.is(
                           ExternalSourceEntryMatcher.matchItemWithGivenMetadata("person.affiliation.name",
                                   "Affiliation one", "0"))));

        exteranlSourceId = UUIDUtils.toString(itemUUID) + ":1";
        getClient().perform(get("/api/integration/externalsources/authorAuthority/entryValues/" + exteranlSourceId))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                           ExternalSourceEntryMatcher.matchItemWithGivenMetadata("dc.title",
                                   "Doe, John", "0"))))
                   .andExpect(jsonPath("$", Matchers.is(
                           ExternalSourceEntryMatcher.matchItemWithGivenMetadata("person.affiliation.name",
                                   "Affiliation two", "0"))));

        exteranlSourceId = UUIDUtils.toString(itemUUID) + ":2";
        getClient().perform(get("/api/integration/externalsources/authorAuthority/entryValues/" + exteranlSourceId))
                   .andExpect(status().isBadRequest());
    }

}
