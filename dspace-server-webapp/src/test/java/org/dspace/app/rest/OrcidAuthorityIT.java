/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.authority.service.AuthorityValueService.GENERATE;
import static org.dspace.authority.service.AuthorityValueService.REFERENCE;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.dspace.app.orcid.client.OrcidClient;
import org.dspace.app.orcid.client.OrcidConfiguration;
import org.dspace.app.orcid.exception.OrcidClientException;
import org.dspace.app.orcid.factory.OrcidServiceFactory;
import org.dspace.app.orcid.factory.OrcidServiceFactoryImpl;
import org.dspace.app.orcid.model.OrcidTokenResponseDTO;
import org.dspace.app.rest.matcher.ItemAuthorityMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.authority.OrcidAuthority;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.orcid.jaxb.model.v3.release.search.expanded.ExpandedResult;
import org.orcid.jaxb.model.v3.release.search.expanded.ExpandedSearch;

/**
 * Integration tests for {@link OrcidAuthority}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidAuthorityIT extends AbstractControllerIntegrationTest {

    private static final String AFFILIATION_INFO = "data-oairecerif_author_affiliation";
    private static final String ORCID_INFO = OrcidAuthority.ORCID_EXTRA;
    private static final String ORCID_INSTITUTION = OrcidAuthority.INSTITUTION_EXTRA;

    private static final String READ_PUBLIC_TOKEN = "062d9f30-7e11-47ef-bd95-eaa2f2452565";

    private OrcidClient orcidClient = OrcidServiceFactory.getInstance().getOrcidClient();

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private OrcidConfiguration orcidConfiguration = OrcidServiceFactory.getInstance().getOrcidConfiguration();

    private OrcidClient orcidClientMock = mock(OrcidClient.class);

    private String originalClientId;

    private String originalClientSecret;

    private Collection collection;

    @Before
    public void setup() {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Test collection")
            .build();

        originalClientId = orcidConfiguration.getClientId();
        originalClientSecret = orcidConfiguration.getClientSecret();

        orcidConfiguration.setClientId("DSPACE-CLIENT-ID");
        orcidConfiguration.setClientSecret("SECRET");

        context.restoreAuthSystemState();

        ((OrcidServiceFactoryImpl) OrcidServiceFactory.getInstance()).setOrcidClient(orcidClientMock);
        when(orcidClientMock.getReadPublicAccessToken()).thenReturn(buildTokenResponse(READ_PUBLIC_TOKEN));
    }

    @After
    public void cleanUp() {
        OrcidAuthority.setAccessToken(null);
        orcidConfiguration.setClientId(originalClientId);
        orcidConfiguration.setClientSecret(originalClientSecret);
        ((OrcidServiceFactoryImpl) OrcidServiceFactory.getInstance()).setOrcidClient(orcidClient);
    }

    @Test
    public void testWithWillBeGeneratedAuthorityPrefix() throws Exception {

        configurationService.setProperty("orcid.authority.prefix", "will be generated::ORCID::");

        context.turnOffAuthorisationSystem();

        Item orgUnit_1 = buildOrgUnit("OrgUnit_1");
        Item orgUnit_2 = buildOrgUnit("OrgUnit_2");
        Item author_1 = buildPerson("Author 1", orgUnit_1);
        Item author_2 = buildPerson("Author 2");
        Item author_3 = buildPerson("Author 3", orgUnit_2);
        Item author_4 = buildPerson("Author 4", orgUnit_1);

        context.restoreAuthSystemState();

        List<ExpandedResult> orcidSearchResults = List.of(
            expandedResult("Author", "From Orcid 1", "0000-1111-2222-3333"),
            expandedResult("AUTHOR", "FROM ORCID 2", "0000-2222-3333-4444"),
            expandedResult("Author", "From Orcid 3", "0000-5555-6666-7777"));

        String expectedQuery = "(given-names:author+OR+family-name:author+OR+other-names:author)";

        when(orcidClientMock.expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 16))
            .thenReturn(expandedSearch(3, orcidSearchResults));

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
            .param("filter", "author"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                affiliationEntry(author_1, "Author 1", "OrgUnit_1::" + id(orgUnit_1)),
                affiliationEntry(author_2, "Author 2", ""),
                affiliationEntry(author_3, "Author 3", "OrgUnit_2::" + id(orgUnit_2)),
                affiliationEntry(author_4, "Author 4", "OrgUnit_1::" + id(orgUnit_1)),
                orcidEntry("Author From Orcid 1", GENERATE, "0000-1111-2222-3333"),
                orcidEntry("Author From Orcid 2", GENERATE, "0000-2222-3333-4444"),
                orcidEntry("Author From Orcid 3", GENERATE, "0000-5555-6666-7777"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(20)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(7)));


        verify(orcidClientMock).getReadPublicAccessToken();
        verify(orcidClientMock).expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 16);
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testWithWillBeReferencedAuthorityPrefix() throws Exception {

        context.turnOffAuthorisationSystem();

        Item orgUnit_1 = buildOrgUnit("OrgUnit_1");
        Item orgUnit_2 = buildOrgUnit("OrgUnit_2");
        Item author_1 = buildPerson("Author 1", orgUnit_1);
        Item author_2 = buildPerson("Author 2");
        Item author_3 = buildPerson("Author 3", orgUnit_2);
        Item author_4 = buildPerson("Author 4", orgUnit_1);

        context.restoreAuthSystemState();

        List<ExpandedResult> orcidSearchResults = List.of(
            expandedResult("Author", "From Orcid 1", "0000-1111-2222-3333"),
            expandedResult("AUTHOR", "FROM ORCID 2", "0000-2222-3333-4444"),
            expandedResult("Author", "From Orcid 3", "0000-5555-6666-7777"));

        String expectedQuery = "(given-names:author+OR+family-name:author+OR+other-names:author)";

        when(orcidClientMock.expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 16))
            .thenReturn(expandedSearch(3, orcidSearchResults));

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
            .param("filter", "author"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                affiliationEntry(author_1, "Author 1", "OrgUnit_1::" + id(orgUnit_1)),
                affiliationEntry(author_2, "Author 2", ""),
                affiliationEntry(author_3, "Author 3", "OrgUnit_2::" + id(orgUnit_2)),
                affiliationEntry(author_4, "Author 4", "OrgUnit_1::" + id(orgUnit_1)),
                orcidEntry("Author From Orcid 1", REFERENCE, "0000-1111-2222-3333"),
                orcidEntry("Author From Orcid 2", REFERENCE, "0000-2222-3333-4444"),
                orcidEntry("Author From Orcid 3", REFERENCE, "0000-5555-6666-7777"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(20)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(7)));

        verify(orcidClientMock).getReadPublicAccessToken();
        verify(orcidClientMock).expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 16);
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testWithPagination() throws Exception {

        context.turnOffAuthorisationSystem();

        Item orgUnit_1 = buildOrgUnit("OrgUnit_1");
        Item orgUnit_2 = buildOrgUnit("OrgUnit_2");
        Item author_1 = buildPerson("Author 1", orgUnit_1);
        Item author_2 = buildPerson("Author 2");
        Item author_3 = buildPerson("Author 3", orgUnit_2);
        Item author_4 = buildPerson("Author 4", orgUnit_1);

        context.restoreAuthSystemState();

        List<ExpandedResult> results = List.of(
            expandedResult("Author", "From Orcid 1", "0000-1111-2222-3333"),
            expandedResult("AUTHOR", "FROM ORCID 2", "0000-2222-3333-4444"),
            expandedResult("Author", "From Orcid 3", "0000-5555-6666-7777"));

        String expectedQuery = "(given-names:author+OR+family-name:author+OR+other-names:author)";

        when(orcidClientMock.expandedSearch(eq(READ_PUBLIC_TOKEN), eq(expectedQuery), anyInt(), anyInt()))
            .thenAnswer((i) -> buildExpandedSearchFromSublist(results, i.getArgument(2), i.getArgument(3)));

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
            .param("filter", "author")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                affiliationEntry(author_1, "Author 1", "OrgUnit_1::" + id(orgUnit_1)),
                affiliationEntry(author_2, "Author 2", ""),
                affiliationEntry(author_3, "Author 3", "OrgUnit_2::" + id(orgUnit_2)),
                affiliationEntry(author_4, "Author 4", "OrgUnit_1::" + id(orgUnit_1)),
                orcidEntry("Author From Orcid 1", REFERENCE, "0000-1111-2222-3333"),
                orcidEntry("Author From Orcid 2", REFERENCE, "0000-2222-3333-4444"),
                orcidEntry("Author From Orcid 3", REFERENCE, "0000-5555-6666-7777"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(10)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(7)));

        verify(orcidClientMock).getReadPublicAccessToken();
        verify(orcidClientMock).expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 6);

        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
            .param("filter", "author")
            .param("page", "1")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", empty()))
            .andExpect(jsonPath("$.page.size", Matchers.is(10)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(7)));

        verify(orcidClientMock).expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 6, 10);

        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
            .param("filter", "author")
            .param("page", "0")
            .param("size", "4"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                affiliationEntry(author_1, "Author 1", "OrgUnit_1::" + id(orgUnit_1)),
                affiliationEntry(author_2, "Author 2", ""),
                affiliationEntry(author_3, "Author 3", "OrgUnit_2::" + id(orgUnit_2)),
                affiliationEntry(author_4, "Author 4", "OrgUnit_1::" + id(orgUnit_1)))))
            .andExpect(jsonPath("$.page.size", Matchers.is(4)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(2)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(7)));

        verify(orcidClientMock).expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 0);

        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
            .param("filter", "author")
            .param("page", "0")
            .param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                affiliationEntry(author_1, "Author 1", "OrgUnit_1::" + id(orgUnit_1)),
                affiliationEntry(author_2, "Author 2", ""),
                affiliationEntry(author_3, "Author 3", "OrgUnit_2::" + id(orgUnit_2)),
                affiliationEntry(author_4, "Author 4", "OrgUnit_1::" + id(orgUnit_1)),
                orcidEntry("Author From Orcid 1", REFERENCE, "0000-1111-2222-3333"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(5)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(2)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(7)));

        verify(orcidClientMock).expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 1);

        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
            .param("filter", "author")
            .param("page", "1")
            .param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                orcidEntry("Author From Orcid 2", REFERENCE, "0000-2222-3333-4444"),
                orcidEntry("Author From Orcid 3", REFERENCE, "0000-5555-6666-7777"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(5)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(2)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(7)));

        verify(orcidClientMock).expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 1, 5);

        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
            .param("filter", "author")
            .param("page", "1")
            .param("size", "6"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                orcidEntry("Author From Orcid 3", REFERENCE, "0000-5555-6666-7777"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(6)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(2)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(7)));

        verify(orcidClientMock).expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 2, 6);

        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testWithErrorRetrivingAccessToken() throws Exception {

        context.turnOffAuthorisationSystem();

        Item orgUnit_1 = buildOrgUnit("OrgUnit_1");
        Item orgUnit_2 = buildOrgUnit("OrgUnit_2");
        Item author_1 = buildPerson("Author 1", orgUnit_1);
        Item author_2 = buildPerson("Author 2");
        Item author_3 = buildPerson("Author 3", orgUnit_2);
        Item author_4 = buildPerson("Author 4", orgUnit_1);

        context.restoreAuthSystemState();

        when(orcidClientMock.getReadPublicAccessToken()).thenThrow(new OrcidClientException(500, "ERROR"));

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
            .param("filter", "author"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                affiliationEntry(author_1, "Author 1", "OrgUnit_1::" + id(orgUnit_1)),
                affiliationEntry(author_2, "Author 2", ""),
                affiliationEntry(author_3, "Author 3", "OrgUnit_2::" + id(orgUnit_2)),
                affiliationEntry(author_4, "Author 4", "OrgUnit_1::" + id(orgUnit_1)))))
            .andExpect(jsonPath("$.page.size", Matchers.is(20)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(4)));

        verify(orcidClientMock).getReadPublicAccessToken();
        verifyNoMoreInteractions(orcidClientMock);
    }

    @Test
    public void testWithErrorSearchingForProfileOnOrcid() throws Exception {

        context.turnOffAuthorisationSystem();

        Item orgUnit_1 = buildOrgUnit("OrgUnit_1");
        Item orgUnit_2 = buildOrgUnit("OrgUnit_2");
        Item author_1 = buildPerson("Author 1", orgUnit_1);
        Item author_2 = buildPerson("Author 2");
        Item author_3 = buildPerson("Author 3", orgUnit_2);
        Item author_4 = buildPerson("Author 4", orgUnit_1);

        context.restoreAuthSystemState();

        String expectedQuery = "(given-names:author+OR+family-name:author+OR+other-names:author)";

        when(orcidClientMock.expandedSearch(eq(READ_PUBLIC_TOKEN), eq(expectedQuery), anyInt(), anyInt()))
            .thenThrow(new OrcidClientException(500, "ERROR"));

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
            .param("filter", "author"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                affiliationEntry(author_1, "Author 1", "OrgUnit_1::" + id(orgUnit_1)),
                affiliationEntry(author_2, "Author 2", ""),
                affiliationEntry(author_3, "Author 3", "OrgUnit_2::" + id(orgUnit_2)),
                affiliationEntry(author_4, "Author 4", "OrgUnit_1::" + id(orgUnit_1)))))
            .andExpect(jsonPath("$.page.size", Matchers.is(20)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(4)));

        verify(orcidClientMock).getReadPublicAccessToken();
        verify(orcidClientMock).expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 16);
        verifyNoMoreInteractions(orcidClientMock);
    }

    @Test
    public void testWithoutRecordFromOrcid() throws Exception {

        context.turnOffAuthorisationSystem();

        Item orgUnit_1 = buildOrgUnit("OrgUnit_1");
        Item orgUnit_2 = buildOrgUnit("OrgUnit_2");
        Item author_1 = buildPerson("Author 1", orgUnit_1);
        Item author_2 = buildPerson("Author 2");
        Item author_3 = buildPerson("Author 3", orgUnit_2);
        Item author_4 = buildPerson("Author 4", orgUnit_1);

        context.restoreAuthSystemState();

        String expectedQuery = "(given-names:author+OR+family-name:author+OR+other-names:author)";

        when(orcidClientMock.expandedSearch(eq(READ_PUBLIC_TOKEN), eq(expectedQuery), anyInt(), anyInt()))
            .thenReturn(expandedSearch(0l, List.of()));

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
            .param("filter", "author"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                affiliationEntry(author_1, "Author 1", "OrgUnit_1::" + id(orgUnit_1)),
                affiliationEntry(author_2, "Author 2", ""),
                affiliationEntry(author_3, "Author 3", "OrgUnit_2::" + id(orgUnit_2)),
                affiliationEntry(author_4, "Author 4", "OrgUnit_1::" + id(orgUnit_1)))))
            .andExpect(jsonPath("$.page.size", Matchers.is(20)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(4)));

        verify(orcidClientMock).getReadPublicAccessToken();
        verify(orcidClientMock).expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 16);
        verifyNoMoreInteractions(orcidClientMock);
    }

    @Test
    public void testWithoutClientIdConfiguration() throws Exception {

        configurationService.setProperty("orcid.authority.prefix", "will be generated::ORCID::");

        orcidConfiguration.setClientId(null);

        context.turnOffAuthorisationSystem();

        Item author_1 = buildPerson("Author 1");
        Item author_2 = buildPerson("Author 2");

        context.restoreAuthSystemState();

        String expectedQuery = "(given-names:author+OR+family-name:author+OR+other-names:author)";

        List<ExpandedResult> results = List.of(
            expandedResult("Author", "From Orcid 1", "0000-1111-2222-3333"),
            expandedResult("AUTHOR", "FROM ORCID 2", "0000-2222-3333-4444"));

        when(orcidClientMock.expandedSearch(eq(expectedQuery), anyInt(), anyInt()))
            .thenReturn(expandedSearch(2, results));

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
            .param("filter", "author"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                affiliationEntry(author_1, "Author 1", ""),
                affiliationEntry(author_2, "Author 2", ""),
                orcidEntry("Author From Orcid 1", GENERATE, "0000-1111-2222-3333"),
                orcidEntry("Author From Orcid 2", GENERATE, "0000-2222-3333-4444"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(20)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(4)));

        verify(orcidClientMock).expandedSearch(expectedQuery, 0, 18);
        verifyNoMoreInteractions(orcidClientMock);
    }

    @Test
    public void testWithoutClientSecretConfiguration() throws Exception {

        configurationService.setProperty("orcid.authority.prefix", "will be generated::ORCID::");

        orcidConfiguration.setClientSecret(null);

        context.turnOffAuthorisationSystem();

        Item author_1 = buildPerson("Author 1");
        Item author_2 = buildPerson("Author 2");

        context.restoreAuthSystemState();

        String expectedQuery = "(given-names:author+OR+family-name:author+OR+other-names:author)";

        List<ExpandedResult> results = List.of(
            expandedResult("Author", "From Orcid 1", "0000-1111-2222-3333"),
            expandedResult("AUTHOR", "FROM ORCID 2", "0000-2222-3333-4444"));

        when(orcidClientMock.expandedSearch(eq(expectedQuery), anyInt(), anyInt()))
            .thenReturn(expandedSearch(2, results));

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
            .param("filter", "author"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                affiliationEntry(author_1, "Author 1", ""),
                affiliationEntry(author_2, "Author 2", ""),
                orcidEntry("Author From Orcid 1", GENERATE, "0000-1111-2222-3333"),
                orcidEntry("Author From Orcid 2", GENERATE, "0000-2222-3333-4444"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(20)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(4)));

        verify(orcidClientMock).expandedSearch(expectedQuery, 0, 18);
        verifyNoMoreInteractions(orcidClientMock);
    }

    @Test
    public void testWithComposedName() throws Exception {

        String expectedQuery = "(given-names:John+OR+family-name:John+OR+other-names:John)"
            + "+AND+(given-names:Bruce+OR+family-name:Bruce+OR+other-names:Bruce)"
            + "+AND+(given-names:Wayne+OR+family-name:Wayne+OR+other-names:Wayne)";

        when(orcidClientMock.expandedSearch(eq(READ_PUBLIC_TOKEN), eq(expectedQuery), anyInt(), anyInt()))
            .thenReturn(expandedSearch(0l, List.of()));

        List<ExpandedResult> orcidSearchResults = List.of(
            expandedResult("Author", "From Orcid 1", "0000-1111-2222-3333"),
            expandedResult("AUTHOR", "FROM ORCID 2", "0000-2222-3333-4444"),
            expandedResult("Author", "From Orcid 3", "0000-5555-6666-7777"));

        when(orcidClientMock.expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 20))
            .thenReturn(expandedSearch(3, orcidSearchResults));

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
            .param("filter", "John Bruce Wayne"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                orcidEntry("Author From Orcid 1", REFERENCE, "0000-1111-2222-3333"),
                orcidEntry("Author From Orcid 2", REFERENCE, "0000-2222-3333-4444"),
                orcidEntry("Author From Orcid 3", REFERENCE, "0000-5555-6666-7777"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(20)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

        verify(orcidClientMock).getReadPublicAccessToken();
        verify(orcidClientMock).expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 20);
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testWithLastNameAndFirstNameSeparatedByComma() throws Exception {

        String expectedQuery = "(given-names:Wayne+OR+family-name:Wayne+OR+other-names:Wayne)"
            + "+AND+(given-names:Bruce+OR+family-name:Bruce+OR+other-names:Bruce)";

        when(orcidClientMock.expandedSearch(eq(READ_PUBLIC_TOKEN), eq(expectedQuery), anyInt(), anyInt()))
            .thenReturn(expandedSearch(0l, List.of()));

        List<ExpandedResult> orcidSearchResults = List.of(
            expandedResult("Author", "From Orcid 1", "0000-1111-2222-3333"),
            expandedResult("AUTHOR", "FROM ORCID 2", "0000-2222-3333-4444"),
            expandedResult("Author", "From Orcid 3", "0000-5555-6666-7777"));

        when(orcidClientMock.expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 20))
            .thenReturn(expandedSearch(3, orcidSearchResults));

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
            .param("filter", "Wayne, Bruce"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                orcidEntry("Author From Orcid 1", REFERENCE, "0000-1111-2222-3333"),
                orcidEntry("Author From Orcid 2", REFERENCE, "0000-2222-3333-4444"),
                orcidEntry("Author From Orcid 3", REFERENCE, "0000-5555-6666-7777"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(20)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

        verify(orcidClientMock).getReadPublicAccessToken();
        verify(orcidClientMock).expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 20);
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testWithAffiliationExtra() throws Exception {

        List<ExpandedResult> orcidSearchResults = List.of(
            expandedResult("Author", "From Orcid 1", "0000-1111-2222-3333"),
            expandedResult("AUTHOR", "FROM ORCID 2", "0000-2222-3333-4444", new String[] { "Org1", "Org2" }),
            expandedResult("Author", "From Orcid 3", "0000-5555-6666-7777", new String[] { "Organization" }));

        String expectedQuery = "(given-names:author+OR+family-name:author+OR+other-names:author)";

        when(orcidClientMock.expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 20))
            .thenReturn(expandedSearch(3, orcidSearchResults));

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
            .param("filter", "author"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                orcidEntry("Author From Orcid 1", REFERENCE, "0000-1111-2222-3333"),
                orcidEntryWithInstitution("Author From Orcid 2", REFERENCE, "0000-2222-3333-4444", "Org1, Org2"),
                orcidEntryWithInstitution("Author From Orcid 3", REFERENCE, "0000-5555-6666-7777", "Organization"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(20)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

        verify(orcidClientMock).getReadPublicAccessToken();
        verify(orcidClientMock).expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 20);
        verifyNoMoreInteractions(orcidClientMock);
    }

    private ExpandedSearch buildExpandedSearchFromSublist(List<ExpandedResult> totalResults, int start, int rows) {
        int total = totalResults.size();
        if (start > total) {
            return expandedSearch(total, List.of());
        }
        return expandedSearch(total, totalResults.subList(start, rows > total ? total : rows));
    }

    private OrcidTokenResponseDTO buildTokenResponse(String token) {
        OrcidTokenResponseDTO response = new OrcidTokenResponseDTO();
        response.setAccessToken(token);
        return response;
    }

    private ExpandedSearch expandedSearch(long numFound, List<ExpandedResult> results) {
        ExpandedSearch expandedSearch = new ExpandedSearch();
        expandedSearch.setNumFound(numFound);
        expandedSearch.getResults().addAll(results);
        return expandedSearch;
    }

    private ExpandedResult expandedResult(String givenName, String familyName, String orcid, String[] institutions) {
        ExpandedResult result = new ExpandedResult();
        result.setGivenNames(givenName);
        result.setFamilyNames(familyName);
        result.setOrcidId(orcid);
        result.setInstitutionNames(institutions);
        return result;
    }

    private ExpandedResult expandedResult(String givenName, String familyName, String orcid) {
        return expandedResult(givenName, familyName, orcid, null);
    }

    private Item buildOrgUnit(String title) {
        return ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .withEntityType("OrgUnit")
            .build();
    }

    private Item buildPerson(String title) {
        return ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .withEntityType("Person")
            .build();
    }

    private Item buildPerson(String title, Item affiliation) {
        return ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .withEntityType("Person")
            .withPersonMainAffiliation(affiliation.getName(), affiliation.getID().toString())
            .build();
    }

    private Matcher<? super Object> affiliationEntry(Item item, String title, String otherInfoValue) {
        return ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(id(item), title,
            title, "vocabularyEntry", AFFILIATION_INFO, otherInfoValue);
    }

    private Matcher<? super Object> orcidEntry(String title, String authorityPrefix, String orcid) {
        String authority = authorityPrefix + "ORCID::" + orcid;
        return ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(authority, title,
            title, "vocabularyEntry", ORCID_INFO, orcid);
    }

    private Matcher<? super Object> orcidEntryWithInstitution(String title, String authorityPrefix,
        String orcid, String institutions) {
        String authority = authorityPrefix + "ORCID::" + orcid;
        return ItemAuthorityMatcher.matchItemAuthorityWithTwoMetadataInOtherInformations(authority, title,
            title, "vocabularyEntry", ORCID_INFO, orcid, ORCID_INSTITUTION, institutions);
    }

    private String id(Item item) {
        return item.getID().toString();
    }
}
