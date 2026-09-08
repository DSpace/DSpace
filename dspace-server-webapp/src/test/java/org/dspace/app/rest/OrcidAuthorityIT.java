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
import java.util.Map;

import org.dspace.app.rest.matcher.ItemAuthorityMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.authority.ItemAuthority;
import org.dspace.content.authority.OrcidAuthority;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.orcid.client.OrcidClient;
import org.dspace.orcid.client.OrcidConfiguration;
import org.dspace.orcid.exception.OrcidClientException;
import org.dspace.orcid.factory.OrcidServiceFactory;
import org.dspace.orcid.factory.OrcidServiceFactoryImpl;
import org.dspace.orcid.model.OrcidTokenResponseDTO;
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

    private static final String ORCID_INFO = OrcidAuthority.DEFAULT_ORCID_KEY;
    private static final String ORCID_INSTITUTION = OrcidAuthority.DEFAULT_INSTITUTION_KEY;

    private static final String READ_PUBLIC_TOKEN = "062d9f30-7e11-47ef-bd95-eaa2f2452565";

    private OrcidClient orcidClient = OrcidServiceFactory.getInstance().getOrcidClient();

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private OrcidConfiguration orcidConfiguration = OrcidServiceFactory.getInstance().getOrcidConfiguration();

    private PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();

    private ChoiceAuthorityService choiceAuthorityService = ContentAuthorityServiceFactory
        .getInstance().getChoiceAuthorityService();

    private MetadataAuthorityService metadataAuthorityService = ContentAuthorityServiceFactory
        .getInstance().getMetadataAuthorityService();

    private OrcidClient orcidClientMock = mock(OrcidClient.class);

    private String originalClientId;

    private String originalClientSecret;

    private Collection collection;

    @Before
    public void setup() {
        choiceAuthorityService.getChoiceAuthoritiesNames(); // initialize the ChoiceAuthorityService

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
        setupAuthorAuthorityAndOrgUnit();

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
                            orcidEntry("From Orcid 1 Author", GENERATE, "0000-1111-2222-3333"),
                            orcidEntry("From Orcid 2 Author", GENERATE, "0000-2222-3333-4444"),
                            orcidEntry("From Orcid 3 Author", GENERATE, "0000-5555-6666-7777"))))
                        .andExpect(jsonPath("$.page.size", Matchers.is(20)))
                        .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(7)));


        verify(orcidClientMock).getReadPublicAccessToken();
        verify(orcidClientMock).expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 16);
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testWithWillBeReferencedAuthorityPrefix() throws Exception {
        setupAuthorAuthorityAndOrgUnit();

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
                            orcidEntry("From Orcid 1 Author", REFERENCE, "0000-1111-2222-3333"),
                            orcidEntry("From Orcid 2 Author", REFERENCE, "0000-2222-3333-4444"),
                            orcidEntry("From Orcid 3 Author", REFERENCE, "0000-5555-6666-7777"))))
                        .andExpect(jsonPath("$.page.size", Matchers.is(20)))
                        .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(7)));

        verify(orcidClientMock).getReadPublicAccessToken();
        verify(orcidClientMock).expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 16);
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testWithPagination() throws Exception {
        setupAuthorAuthorityAndOrgUnit();

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
                            orcidEntry("From Orcid 1 Author", REFERENCE, "0000-1111-2222-3333"),
                            orcidEntry("From Orcid 2 Author", REFERENCE, "0000-2222-3333-4444"),
                            orcidEntry("From Orcid 3 Author", REFERENCE, "0000-5555-6666-7777"))))
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
                            orcidEntry("From Orcid 1 Author", REFERENCE, "0000-1111-2222-3333"))))
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
                            orcidEntry("From Orcid 2 Author", REFERENCE, "0000-2222-3333-4444"),
                            orcidEntry("From Orcid 3 Author", REFERENCE, "0000-5555-6666-7777"))))
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
                            orcidEntry("From Orcid 3 Author", REFERENCE, "0000-5555-6666-7777"))))
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
        setupAuthorAuthorityAndOrgUnit();

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
        setupAuthorAuthorityAndOrgUnit();

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
                            orcidEntry("From Orcid 1 Author", GENERATE, "0000-1111-2222-3333"),
                            orcidEntry("From Orcid 2 Author", GENERATE, "0000-2222-3333-4444"))))
                        .andExpect(jsonPath("$.page.size", Matchers.is(20)))
                        .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(4)));

        verify(orcidClientMock).expandedSearch(expectedQuery, 0, 18);
        verifyNoMoreInteractions(orcidClientMock);
    }

    @Test
    public void testWithoutClientSecretConfiguration() throws Exception {
        setupAuthorAuthority();

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
                            orcidEntry("From Orcid 1 Author", GENERATE, "0000-1111-2222-3333"),
                            orcidEntry("From Orcid 2 Author", GENERATE, "0000-2222-3333-4444"))))
                        .andExpect(jsonPath("$.page.size", Matchers.is(20)))
                        .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(4)));

        verify(orcidClientMock).expandedSearch(expectedQuery, 0, 18);
        verifyNoMoreInteractions(orcidClientMock);
    }

    @Test
    public void testWithComposedName() throws Exception {
        setupAuthorAuthority();

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
                            orcidEntry("From Orcid 1 Author", REFERENCE, "0000-1111-2222-3333"),
                            orcidEntry("From Orcid 2 Author", REFERENCE, "0000-2222-3333-4444"),
                            orcidEntry("From Orcid 3 Author", REFERENCE, "0000-5555-6666-7777"))))
                        .andExpect(jsonPath("$.page.size", Matchers.is(20)))
                        .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

        verify(orcidClientMock).getReadPublicAccessToken();
        verify(orcidClientMock).expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 20);
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testWithLastNameAndFirstNameSeparatedByComma() throws Exception {
        setupAuthorAuthority();

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
                            orcidEntry("From Orcid 1 Author", REFERENCE, "0000-1111-2222-3333"),
                            orcidEntry("From Orcid 2 Author", REFERENCE, "0000-2222-3333-4444"),
                            orcidEntry("From Orcid 3 Author", REFERENCE, "0000-5555-6666-7777"))))
                        .andExpect(jsonPath("$.page.size", Matchers.is(20)))
                        .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

        verify(orcidClientMock).getReadPublicAccessToken();
        verify(orcidClientMock).expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 20);
        verifyNoMoreInteractions(orcidClientMock);

    }


    @Test
    public void testWithLatinValueLastNameAndFirstNameSeparatedByComma() throws Exception {
        setupAuthorAuthority();

        String expectedQuery = "(given-names:Wayne+OR+family-name:Wayne+OR+other-names:Wayne)"
            + "+AND+(given-names:Bruce+OR+family-name:Bruce+OR+other-names:Bruce)";

        when(orcidClientMock.expandedSearch(eq(READ_PUBLIC_TOKEN), eq(expectedQuery), anyInt(), anyInt()))
            .thenReturn(expandedSearch(0l, List.of()));

        List<ExpandedResult> orcidSearchResults = List.of(expandedResult("Vincenzo", "Mecca", "0000-1111-2222-3333"));

        when(orcidClientMock.expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 20))
            .thenReturn(expandedSearch(1, orcidSearchResults));

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
                                     .param("filter", "Wayne, Bruce"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                            orcidEntry("Mecca, Vincenzo", REFERENCE, "0000-1111-2222-3333"))))
                        .andExpect(jsonPath("$.page.size", Matchers.is(20)))
                        .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));

        verify(orcidClientMock).getReadPublicAccessToken();
        verify(orcidClientMock).expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 20);
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testWithNonLatinValueLastNameAndFirstNameSeparatedByComma() throws Exception {

        String expectedQuery = "(given-names:Wayne+OR+family-name:Wayne+OR+other-names:Wayne)"
            + "+AND+(given-names:Bruce+OR+family-name:Bruce+OR+other-names:Bruce)";

        when(orcidClientMock.expandedSearch(eq(READ_PUBLIC_TOKEN), eq(expectedQuery), anyInt(), anyInt()))
            .thenReturn(expandedSearch(0l, List.of()));

        List<ExpandedResult> orcidSearchResults = List.of(
            expandedResult("Vins", "@4Science", "0000-1111-2222-3333"),
            expandedResult("V1n5", "M3cc4", "0000-4444-5555-6666")
        );

        when(orcidClientMock.expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 20))
            .thenReturn(expandedSearch(2, orcidSearchResults));

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
                                     .param("filter", "Wayne, Bruce"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                            orcidEntry("@4science Vins", REFERENCE, "0000-1111-2222-3333"),
                            orcidEntry("M3cc4 V1n5", REFERENCE, "0000-4444-5555-6666")
                        )))
                        .andExpect(jsonPath("$.page.size", Matchers.is(20)))
                        .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)));

        verify(orcidClientMock).getReadPublicAccessToken();
        verify(orcidClientMock).expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 20);
        verifyNoMoreInteractions(orcidClientMock);
    }

    @Test
    public void testWithMultipleValueLastNameAndFirstNameSeparatedByComma() throws Exception {
        setupAuthorAuthority();

        String expectedQuery = "(given-names:Wayne+OR+family-name:Wayne+OR+other-names:Wayne)"
            + "+AND+(given-names:Bruce+OR+family-name:Bruce+OR+other-names:Bruce)";

        when(orcidClientMock.expandedSearch(eq(READ_PUBLIC_TOKEN), eq(expectedQuery), anyInt(), anyInt()))
            .thenReturn(expandedSearch(0l, List.of()));

        List<ExpandedResult> orcidSearchResults = List.of(
            expandedResult("Vincenzo", "Mecca", "0000-1111-2222-3333"),
            expandedResult("Vins", "@4Science", "0000-4444-5555-6666"),
            expandedResult("V1n5", "M3cc4", "0000-7777-8888-9999")
        );

        when(orcidClientMock.expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 20))
            .thenReturn(expandedSearch(3, orcidSearchResults));

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
                                     .param("filter", "Wayne, Bruce"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                            orcidEntry("Mecca, Vincenzo", REFERENCE, "0000-1111-2222-3333"),
                            orcidEntry("@4science Vins", REFERENCE, "0000-4444-5555-6666"),
                            orcidEntry("M3cc4 V1n5", REFERENCE, "0000-7777-8888-9999")
                        )))
                        .andExpect(jsonPath("$.page.size", Matchers.is(20)))
                        .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

        verify(orcidClientMock).getReadPublicAccessToken();
        verify(orcidClientMock).expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 20);
        verifyNoMoreInteractions(orcidClientMock);
    }

    @Test
    public void testWithAffiliationExtra() throws Exception {
        setupAuthorAuthorityAndOrgUnit();

        List<ExpandedResult> orcidSearchResults = List.of(
            expandedResult("Author", "From Orcid 1", "0000-1111-2222-3333"),
            expandedResult("AUTHOR", "FROM ORCID 2", "0000-2222-3333-4444", new String[] {"Org1", "Org2"}),
            expandedResult("Author", "From Orcid 3", "0000-5555-6666-7777", new String[] {"Organization"}));

        String expectedQuery = "(given-names:author+OR+family-name:author+OR+other-names:author)";

        when(orcidClientMock.expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 20))
            .thenReturn(expandedSearch(3, orcidSearchResults));

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
                                     .param("filter", "author"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                            orcidEntry("From Orcid 1 Author", REFERENCE, "0000-1111-2222-3333"),
                            orcidEntryWithAffiliation("From Orcid 2 Author", REFERENCE, "0000-2222-3333-4444",
                                                      "Org1, Org2"),
                            orcidEntryWithAffiliation("From Orcid 3 Author", REFERENCE, "0000-5555-6666-7777",
                                                      "Organization"))))
                        .andExpect(jsonPath("$.page.size", Matchers.is(20)))
                        .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

        verify(orcidClientMock).getReadPublicAccessToken();
        verify(orcidClientMock).expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 20);
        verifyNoMoreInteractions(orcidClientMock);
    }

    @Test
    public void testSourceReference() throws Exception {
        setupAuthorAuthority();

        List<ExpandedResult> orcidSearchResults = List.of(
            expandedResult("Author", "From Orcid 1", "0000-1111-2222-3333"),
            expandedResult("AUTHOR", "FROM ORCID 2", "0000-2222-3333-4444"));

        String expectedQuery = "(given-names:author+OR+family-name:author+OR+other-names:author)";

        when(orcidClientMock.expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 20))
            .thenReturn(expandedSearch(2, orcidSearchResults));

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
                                     .param("filter", "author"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                            orcidEntry("From Orcid 1 Author", REFERENCE, "0000-1111-2222-3333", getSource()),
                            orcidEntry("From Orcid 2 Author", REFERENCE, "0000-2222-3333-4444", getSource()))))
                        .andExpect(jsonPath("$.page.size", Matchers.is(20)))
                        .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)));

        verify(orcidClientMock).getReadPublicAccessToken();
        verify(orcidClientMock).expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 20);
        verifyNoMoreInteractions(orcidClientMock);
    }

    @Test
    public void testMultipleSourcesReferences() throws Exception {
        setupAuthorAuthorityAndOrgUnit();

        context.turnOffAuthorisationSystem();

        List<ExpandedResult> orcidSearchResults = List.of(
            expandedResult("Author", "From Orcid 1", "0000-1111-2222-3333"),
            expandedResult("AUTHOR", "FROM ORCID 2", "0000-2222-3333-4444"));

        String expectedQuery = "(given-names:author+OR+family-name:author+OR+other-names:author)";

        when(orcidClientMock.expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 18))
            .thenReturn(expandedSearch(2, orcidSearchResults));

        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).build();

        Item orgUnit_1 = ItemBuilder.createItem(context, col1)
                                    .withTitle("OrgUnit_1")
                                    .withEntityType("orgunit")
                                    .build();

        Item orgUnit_2 = ItemBuilder.createItem(context, col1)
                                    .withTitle("OrgUnit_2")
                                    .withEntityType("orgunit")
                                    .build();

        Item author_1 = ItemBuilder.createItem(context, col1)
                                   .withTitle("Author 1")
                                   .withPersonMainAffiliation(orgUnit_1.getName(), orgUnit_1.getID().toString())
                                   .withPersonAffiliation(orgUnit_1.getName(), orgUnit_1.getID().toString())
                                   .withEntityType("person")
                                   .build();

        Item author_2 = ItemBuilder.createItem(context, col1)
                                   .withTitle("Author 2")
                                   .withPersonMainAffiliation(orgUnit_2.getName(), orgUnit_2.getID().toString())
                                   .withPersonAffiliation(orgUnit_2.getName(), orgUnit_2.getID().toString())
                                   .withEntityType("person")
                                   .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
                                     .param("filter", "author"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                            // source should be local
                            ItemAuthorityMatcher
                                .matchItemAuthorityWithOtherInformations(author_1.getID().toString(),
                                                                         "Author 1", "Author 1",
                                                                         "vocabularyEntry",
                                                                         Map.of(
                                                                             "data-oairecerif_author_affiliation",
                                                                             "OrgUnit_1::" + orgUnit_1.getID(),
                                                                             "oairecerif_author_affiliation",
                                                                             "OrgUnit_1::" + orgUnit_1.getID()),
                                                                         ItemAuthority.DEFAULT),
                            ItemAuthorityMatcher
                                .matchItemAuthorityWithOtherInformations(author_2.getID().toString(),
                                                                         "Author 2", "Author 2",
                                                                         "vocabularyEntry",
                                                                         Map.of(
                                                                             "data-oairecerif_author_affiliation",
                                                                             "OrgUnit_2::" + orgUnit_2.getID(),
                                                                             "oairecerif_author_affiliation",
                                                                             "OrgUnit_2::" + orgUnit_2.getID()),
                                                                         ItemAuthority.DEFAULT),
                            // source should be orcid as configured
                            orcidEntry("From Orcid 1 Author", REFERENCE, "0000-1111-2222-3333", getSource()),
                            orcidEntry("From Orcid 2 Author", REFERENCE, "0000-2222-3333-4444", getSource())
                        )))
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(4)));
    }

    @Test
    public void testWithORCIDIdentifier() throws Exception {

        List<ExpandedResult> orcidSearchResults = List.of(
            expandedResult("Author", "From Orcid 1", "0000-1111-2222-3333"));

        String expectedQuery = "(orcid:0000-1111-2222-3333)";

        when(orcidClientMock.expandedSearch(READ_PUBLIC_TOKEN, expectedQuery, 0, 20))
            .thenReturn(expandedSearch(2, orcidSearchResults));

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/AuthorAuthority/entries")
                                     .param("filter", "0000-1111-2222-3333"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                            orcidEntry("From Orcid 1 Author", REFERENCE, "0000-1111-2222-3333", getSource()))))
                        .andExpect(jsonPath("$.page.size", Matchers.is(20)))
                        .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));

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
                          .withPersonAffiliation(affiliation.getName(), affiliation.getID().toString())
                          .build();
    }

    private Matcher<? super Object> affiliationEntry(Item item, String title, String otherInfoValue) {
        return ItemAuthorityMatcher
            .matchItemAuthorityWithOtherInformations(id(item), title,
                                                     title, "vocabularyEntry",
                                                     otherInfoValue.equals("") ? Map.of() :
                                                         Map.of(
                                                             "data-oairecerif_author_affiliation",
                                                             otherInfoValue,
                                                             "oairecerif_author_affiliation",
                                                             otherInfoValue));
    }

    private Matcher<? super Object> orcidEntry(String title, String authorityPrefix, String orcid) {
        String authority = authorityPrefix + "ORCID::" + orcid;
        return ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(authority, title,
                                                                            title, "vocabularyEntry", ORCID_INFO,
                                                                            orcid);
    }

    private Matcher<? super Object> orcidEntry(String title, String authorityPrefix, String orcid, String source) {
        String authority = authorityPrefix + "ORCID::" + orcid;
        return ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations(authority, title,
                                                                            title, "vocabularyEntry", ORCID_INFO, orcid,
                                                                            source);
    }

    private Matcher<? super Object> orcidEntryWithAffiliation(String title, String authorityPrefix,
                                                              String orcid, String affiliation) {
        String authority = authorityPrefix + "ORCID::" + orcid;
        return ItemAuthorityMatcher.matchItemAuthorityWithTwoMetadataInOtherInformations(
            authority, title, title, "vocabularyEntry",
            Map.of(
                "data-" + ORCID_INFO, orcid,
                ORCID_INFO, orcid,
                "data-oairecerif_author_affiliation", affiliation,
                "oairecerif_author_affiliation", affiliation
            )
        );
    }

    private String id(Item item) {
        return item.getID().toString();
    }

    private String getSource() {
        return configurationService.getProperty(
            "cris.ItemAuthority.AuthorAuthority.source", ItemAuthority.DEFAULT);
    }

    private void setupAuthorAuthorityAndOrgUnit() throws SubmissionConfigReaderException {
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
                                         new String[] {
                                             "org.dspace.content.authority.ItemAuthority = OrgUnitAuthority",
                                             "org.dspace.content.authority.OrcidAuthority = AuthorAuthority"
                                         });
        configurationService.setProperty("cris.ItemAuthority.OrgUnitAuthority.entityType", "OrgUnit");
        configurationService.setProperty("cris.ItemAuthority.AuthorAuthority.entityType", "Person");
        configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("choices.plugin.person.affiliation.name", "OrgUnitAuthority");
        configurationService.setProperty("choices.presentation.person.affiliation.name", "suggest");
        configurationService.setProperty("authority.controlled.person.affiliation.name", "true");
        configurationService.setProperty("choices.plugin.oairecerif.person.affiliation", "OrgUnitAuthority");
        configurationService.setProperty("choices.presentation.oairecerif.person.affiliation", "suggest");
        configurationService.setProperty("authority.controlled.oairecerif.person.affiliation", "true");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();
    }

    private void setupAuthorAuthority() throws SubmissionConfigReaderException {
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
                                         new String[] {
                                             "org.dspace.content.authority.OrcidAuthority = AuthorAuthority"});
        configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("cris.ItemAuthority.AuthorAuthority.entityType", "Person");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();
    }

}
