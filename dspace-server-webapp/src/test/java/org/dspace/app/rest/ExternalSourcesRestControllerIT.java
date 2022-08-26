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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import org.dspace.app.rest.matcher.EntityTypeMatcher;
import org.dspace.app.rest.matcher.ExternalSourceEntryMatcher;
import org.dspace.app.rest.matcher.ExternalSourceMatcher;
import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.content.EntityType;
import org.dspace.external.provider.AbstractExternalDataProvider;
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.external.service.ExternalDataService;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ExternalSourcesRestControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ExternalDataService externalDataService;

    @Test
    public void findAllExternalSources() throws Exception {
        getClient().perform(get("/api/integration/externalsources"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$._embedded.externalsources", Matchers.hasItems(
                                ExternalSourceMatcher.matchExternalSource("mock", "mock", false),
                                ExternalSourceMatcher.matchExternalSource("orcid", "orcid", false),
                                ExternalSourceMatcher.matchExternalSource("scopus", "scopus", false),
                                ExternalSourceMatcher.matchExternalSource(
                                    "sherpaJournalIssn", "sherpaJournalIssn", false),
                                ExternalSourceMatcher.matchExternalSource(
                                    "sherpaJournal", "sherpaJournal", false),
                                ExternalSourceMatcher.matchExternalSource(
                                    "sherpaPublisher", "sherpaPublisher", false),
                                ExternalSourceMatcher.matchExternalSource(
                                        "pubmed", "pubmed", false),
                                ExternalSourceMatcher.matchExternalSource(
                                        "openAIREFunding", "openAIREFunding", false)
                            )))
                            .andExpect(jsonPath("$.page.totalElements", Matchers.is(9)));
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
        getClient().perform(get("/api/integration/externalsources/mock2"))
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
    public void findExternalSourcesByEntityTypeTest() throws Exception {
        List<ExternalDataProvider> publicationProviders =
                externalDataService.getExternalDataProvidersForEntityType("Publication");
        List<ExternalDataProvider> journalProviders =
                externalDataService.getExternalDataProvidersForEntityType("Journal");

        getClient().perform(get("/api/integration/externalsources/search/findByEntityType")
                   .param("entityType", "Publication"))
                   .andExpect(status().isOk())
                   // Expect *at least* 3 Publication sources
                   .andExpect(jsonPath("$._embedded.externalsources", Matchers.hasItems(
                              ExternalSourceMatcher.matchExternalSource(publicationProviders.get(0)),
                              ExternalSourceMatcher.matchExternalSource(publicationProviders.get(1))
                              )))
                   .andExpect(jsonPath("$.page.totalElements", Matchers.is(publicationProviders.size())));

        getClient().perform(get("/api/integration/externalsources/search/findByEntityType")
                   .param("entityType", "Journal"))
                   .andExpect(status().isOk())
                   // Expect *at least* 5 Journal sources
                   .andExpect(jsonPath("$._embedded.externalsources", Matchers.hasItems(
                              ExternalSourceMatcher.matchExternalSource(journalProviders.get(0)),
                              ExternalSourceMatcher.matchExternalSource(journalProviders.get(1)),
                              ExternalSourceMatcher.matchExternalSource(journalProviders.get(2)),
                              ExternalSourceMatcher.matchExternalSource(journalProviders.get(3))
                              )))
                   .andExpect(jsonPath("$.page.totalElements", Matchers.is(journalProviders.size())));
    }

    @Test
    public void findExternalSourcesByEntityTypeBadRequestTest() throws Exception {
        getClient().perform(get("/api/integration/externalsources/search/findByEntityType"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void findExternalSourcesByEntityTypePaginationTest() throws Exception {
        List<ExternalDataProvider> journalProviders =
                externalDataService.getExternalDataProvidersForEntityType("Journal");
        int numJournalProviders = journalProviders.size();

        // If we return 2 per page, determine number of pages we expect
        int pageSize = 2;
        int numberOfPages = (int) Math.ceil((double) numJournalProviders / pageSize);

        getClient().perform(get("/api/integration/externalsources/search/findByEntityType")
                   .param("entityType", "Journal")
                   .param("size", String.valueOf(pageSize)))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.externalsources", Matchers.contains(
                              ExternalSourceMatcher.matchExternalSource(journalProviders.get(0)),
                              ExternalSourceMatcher.matchExternalSource(journalProviders.get(1))
                              )))
                   .andExpect(jsonPath("$.page.totalPages", Matchers.is(numberOfPages)))
                   .andExpect(jsonPath("$.page.totalElements", Matchers.is(numJournalProviders)));

        getClient().perform(get("/api/integration/externalsources/search/findByEntityType")
                   .param("entityType", "Journal")
                   .param("page", "1")
                   .param("size", String.valueOf(pageSize)))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.externalsources", Matchers.contains(
                              ExternalSourceMatcher.matchExternalSource(journalProviders.get(2)),
                              ExternalSourceMatcher.matchExternalSource(journalProviders.get(3))
                              )))
                   .andExpect(jsonPath("$.page.totalPages", Matchers.is(numberOfPages)))
                   .andExpect(jsonPath("$.page.totalElements", Matchers.is(numJournalProviders)));
    }

    @Test
    public void findSupportedEntityTypesOfAnExternalDataProviderTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EntityType publication = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType orgUnit = EntityTypeBuilder.createEntityTypeBuilder(context, "OrgUnit").build();
        EntityType project = EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();
        EntityType funding = EntityTypeBuilder.createEntityTypeBuilder(context, "Funding").build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        try {
            ((AbstractExternalDataProvider) externalDataService.getExternalDataProvider("mock"))
                               .setSupportedEntityTypes(Arrays.asList("Publication", "OrgUnit"));
            ((AbstractExternalDataProvider) externalDataService.getExternalDataProvider("pubmed"))
                       .setSupportedEntityTypes(Arrays.asList("Project","Publication", "Funding"));

            getClient(tokenAdmin).perform(get("/api/integration/externalsources/mock/entityTypes"))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$._embedded.entityTypes", containsInAnyOrder(
                                            EntityTypeMatcher.matchEntityTypeEntry(publication),
                                            EntityTypeMatcher.matchEntityTypeEntry(orgUnit)
                                            )))
                                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)));

            getClient(tokenAdmin).perform(get("/api/integration/externalsources/pubmed/entityTypes"))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$._embedded.entityTypes", containsInAnyOrder(
                                            EntityTypeMatcher.matchEntityTypeEntry(project),
                                            EntityTypeMatcher.matchEntityTypeEntry(publication),
                                            EntityTypeMatcher.matchEntityTypeEntry(funding)
                                            )))
                                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));
        } finally {
            ((AbstractExternalDataProvider) externalDataService.getExternalDataProvider("mock"))
                    .setSupportedEntityTypes(null);
            ((AbstractExternalDataProvider) externalDataService.getExternalDataProvider("pubmed"))
                    .setSupportedEntityTypes(null);
        }
    }

    @Test
    public void findSupportedEntityTypesOfAnExternalDataProviderNotFoundTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/integration/externalsources/WrongProvider/entityTypes"))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void findSupportedEntityTypesOfAnExternalDataProviderEmptyResponseTest() throws Exception {
        ((AbstractExternalDataProvider) externalDataService.getExternalDataProvider("mock"))
                                                           .setSupportedEntityTypes(null);

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/integration/externalsources/mock/entityTypes"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._embedded.entityTypes").isEmpty())
                             .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));
    }

    @Test
    public void findSupportedEntityTypesOfAnExternalDataProviderPaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EntityType publication = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType orgUnit = EntityTypeBuilder.createEntityTypeBuilder(context, "OrgUnit").build();
        EntityType project = EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        try {
            ((AbstractExternalDataProvider) externalDataService.getExternalDataProvider("mock"))
                    .setSupportedEntityTypes(Arrays.asList("Publication", "OrgUnit", "Project"));

            getClient(tokenAdmin).perform(get("/api/integration/externalsources/mock/entityTypes")
                                 .param("size", "2"))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$._embedded.entityTypes", containsInAnyOrder(
                                            EntityTypeMatcher.matchEntityTypeEntry(publication),
                                            EntityTypeMatcher.matchEntityTypeEntry(project)
                                            )))
                                 .andExpect(jsonPath("$.page.totalPages", Matchers.is(2)))
                                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

            getClient(tokenAdmin).perform(get("/api/integration/externalsources/mock/entityTypes")
                                 .param("page", "1")
                                 .param("size", "2"))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$._embedded.entityTypes", contains(
                                            EntityTypeMatcher.matchEntityTypeEntry(orgUnit)
                                            )))
                                 .andExpect(jsonPath("$.page.totalPages", Matchers.is(2)))
                                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

        } finally {
            ((AbstractExternalDataProvider) externalDataService.getExternalDataProvider("mock"))
                    .setSupportedEntityTypes(null);
        }
    }

}
