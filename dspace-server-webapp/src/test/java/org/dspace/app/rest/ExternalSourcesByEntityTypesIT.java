/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import org.dspace.app.rest.matcher.EntityTypeMatcher;
import org.dspace.app.rest.matcher.ExternalSourceMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.external.provider.AbstractExternalDataProvider;
import org.dspace.external.service.ExternalDataService;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ExternalSourcesByEntityTypesIT extends AbstractControllerIntegrationTest {
    @Autowired
    private ExternalDataService externalDataService;

    @Test
    public void findExternalSourcesByEntityType() throws Exception {
        getClient()
                .perform(get("/api/integration/externalsources/search/findByEntityType").param("entityType",
                        "Publication"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$._embedded.externalsources", Matchers.contains(
                                ExternalSourceMatcher.matchExternalSource("mock", "mock", false),
                                ExternalSourceMatcher.matchExternalSource("mock2", "mock2", false),
                                ExternalSourceMatcher.matchExternalSource("mock3", "mock3", false),
                                ExternalSourceMatcher.matchExternalSource("pubmed", "pubmed", false),
                                ExternalSourceMatcher.matchExternalSource("suggestion", "suggestion", false),
                                ExternalSourceMatcher.matchExternalSource("crossref", "crossref", false),
                                ExternalSourceMatcher.matchExternalSource("orcidWorks", "orcidWorks", false)
                                )))
                            .andExpect(jsonPath("$.page.totalElements", Matchers.is(7)));
        // mock and ORCID are configured without any entity type
        getClient()
                .perform(get("/api/integration/externalsources/search/findByEntityType").param("entityType", "Funding"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.externalsources", Matchers.contains(
                ExternalSourceMatcher.matchExternalSource("mock", "mock", false),
                ExternalSourceMatcher.matchExternalSource("pubmed", "pubmed", false),
                ExternalSourceMatcher.matchExternalSource("suggestion", "suggestion", false),
                ExternalSourceMatcher.matchExternalSource("openaireProject", "openaireProject", false)
            )))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(4)));
    }

    @Test
    public void findExternalSourcesByEntityTypePaginated() throws Exception {
        getClient()
                .perform(get("/api/integration/externalsources/search/findByEntityType")
                        .param("entityType", "Publication").param("size", "2").param("page", "1"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$._embedded.externalsources", Matchers.contains(
                                ExternalSourceMatcher.matchExternalSource("mock3", "mock3", false),
                                ExternalSourceMatcher.matchExternalSource("pubmed", "pubmed", false)
                            )))
                            .andExpect(jsonPath("$.page.totalElements", Matchers.is(7)));
    }
    @Test
    public void findAllByAuthorizedExternalSource() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType publicationType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType orgUnitType = EntityTypeBuilder.createEntityTypeBuilder(context, "OrgUnit").build();
        EntityType projectType = EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();
        EntityType fundingType = EntityTypeBuilder.createEntityTypeBuilder(context, "Funding").build();
        // ** GIVEN **
        // 1. A community-collection structure with one parent community with
        // sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withEntityType("OrgUnit")
                .withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                .withEntityType("Publication").withSubmitterGroup(eperson).withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, parentCommunity).withEntityType("Project")
                .withSubmitterGroup(eperson).withName("Collection 3").build();
        Collection col4 = CollectionBuilder.createCollection(context, parentCommunity).withEntityType("Funding")
                .withSubmitterGroup(eperson).withName("Collection 4").build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/entitytypes/search/findAllByAuthorizedExternalSource"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.entitytypes",
                        containsInAnyOrder(
                                EntityTypeMatcher.matchEntityTypeEntry(publicationType),
                                EntityTypeMatcher.matchEntityTypeEntry(fundingType),
                                EntityTypeMatcher.matchEntityTypeEntry(projectType))))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/core/entitytypes/search/findAllByAuthorizedExternalSource"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.entitytypes",
                        containsInAnyOrder(
                                EntityTypeMatcher.matchEntityTypeEntry(orgUnitType),
                                EntityTypeMatcher.matchEntityTypeEntry(fundingType),
                                EntityTypeMatcher.matchEntityTypeEntry(projectType),
                                EntityTypeMatcher.matchEntityTypeEntry(publicationType))))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(4)));
        getClient(adminToken)
                .perform(get("/api/core/entitytypes/search/findAllByAuthorizedExternalSource").param("size", "1")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.entitytypes", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(4)));
        // temporary alter the mock, orcid, suggestion data providers to restrict them to Publication
        try {
            ((AbstractExternalDataProvider) externalDataService.getExternalDataProvider("mock"))
                    .setSupportedEntityTypes(Arrays.asList("Publication"));
            ((AbstractExternalDataProvider) externalDataService.getExternalDataProvider("suggestion"))
                    .setSupportedEntityTypes(Arrays.asList("Publication"));
            ((AbstractExternalDataProvider) externalDataService.getExternalDataProvider("pubmed"))
                    .setSupportedEntityTypes(Arrays.asList("Publication"));

            getClient(token).perform(get("/api/core/entitytypes/search/findAllByAuthorizedExternalSource"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.entitytypes",
                            containsInAnyOrder(
                                    EntityTypeMatcher.matchEntityTypeEntry(publicationType),
                                    EntityTypeMatcher.matchEntityTypeEntry(fundingType))))
                    .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)));

            getClient(adminToken).perform(get("/api/core/entitytypes/search/findAllByAuthorizedExternalSource"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.entitytypes",
                            containsInAnyOrder(
                                    EntityTypeMatcher.matchEntityTypeEntry(orgUnitType),
                                    EntityTypeMatcher.matchEntityTypeEntry(fundingType),
                                    EntityTypeMatcher.matchEntityTypeEntry(publicationType))))
                    .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

        } finally {
            ((AbstractExternalDataProvider) externalDataService.getExternalDataProvider("mock"))
                    .setSupportedEntityTypes(null);
            ((AbstractExternalDataProvider) externalDataService.getExternalDataProvider("suggestion"))
                    .setSupportedEntityTypes(null);
            ((AbstractExternalDataProvider) externalDataService.getExternalDataProvider("pubmed"))
                .setSupportedEntityTypes(null);
        }
    }

}
