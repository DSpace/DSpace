/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static java.util.Collections.emptyList;
import static java.util.function.Predicate.not;
import static org.dspace.app.matcher.LambdaMatcher.has;
import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import org.apache.commons.codec.binary.StringUtils;
import org.dspace.app.orcid.client.OrcidClient;
import org.dspace.app.orcid.exception.OrcidClientException;
import org.dspace.app.orcid.model.OrcidTokenResponseDTO;
import org.dspace.app.orcid.webhook.CheckOrcidAuthorization;
import org.dspace.app.orcid.webhook.OrcidWebhookServiceImpl;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.suggestion.Suggestion;
import org.dspace.app.suggestion.orcid.OrcidPublicationLoader;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.eperson.EPerson;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.orcid.jaxb.model.v3.release.record.Person;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link OrcidRestController}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidRestControllerIT extends AbstractControllerIntegrationTest {

    private final static String ORCID = "0000-1111-2222-3333";
    private final static String CODE = "123456";

    private final static String ACCESS_TOKEN = "c41e37e5-c2de-4177-91d6-ed9e9d1f31bf";
    private final static String REFRESH_TOKEN = "0062a9eb-d4ec-4d94-9491-95dd75376d3e";
    private final static String[] ORCID_SCOPES = { "FirstScope", "SecondScope" };

    @Autowired
    private OrcidClient orcidClient;

    private OrcidClient orcidClientMock = mock(OrcidClient.class);

    private ExternalDataProvider originalExternalDataProvider;

    private ExternalDataProvider externalDataProviderMock = mock(ExternalDataProvider.class);

    @Autowired
    private OrcidRestController orcidRestController;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private OrcidPublicationLoader orcidPublicationLoader;

    @Autowired
    private CheckOrcidAuthorization checkOrcidAuthorization;

    @Autowired
    private OrcidWebhookServiceImpl orcidWebhookService;

    private Collection profileCollection;

    private EPerson user;

    @Before
    public void setup() {
        orcidRestController.setOrcidClient(orcidClientMock);
        checkOrcidAuthorization.setOrcidClient(orcidClientMock);
        orcidWebhookService.setOrcidClient(orcidClientMock);

        originalExternalDataProvider = orcidPublicationLoader.getProvider();
        orcidPublicationLoader.setProvider(externalDataProviderMock);

        when(externalDataProviderMock.getSourceIdentifier())
            .thenReturn(originalExternalDataProvider.getSourceIdentifier());

        context.turnOffAuthorisationSystem();

        user = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withPassword(password)
            .withEmail("test@user.it")
            .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent community")
            .build();

        profileCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Persons")
            .withEntityType("Person")
            .build();

        context.restoreAuthSystemState();
    }

    @After
    public void after() throws Exception {
        orcidRestController.setOrcidClient(orcidClient);
        checkOrcidAuthorization.setOrcidClient(orcidClient);
        orcidWebhookService.setOrcidClient(orcidClient);
        orcidPublicationLoader.setProvider(originalExternalDataProvider);

    }

    @Test
    public void testLinkProfileFromCodeProfileConfiguration() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profileItem = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Test user")
            .withCrisOwner("Test User", user.getID().toString())
            .build();

        context.restoreAuthSystemState();

        when(orcidClientMock.getAccessToken(CODE)).thenReturn(buildOrcidTokenResponse(ORCID, ACCESS_TOKEN));

        getClient().perform(get("/api/" + RestModel.CRIS + "/orcid/{itemId}", profileItem.getID())
            .param("code", CODE)
            .param("url", "/home"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(configurationService.getProperty("dspace.ui.url") + "/home"));

        verify(orcidClientMock).getAccessToken(CODE);
        verifyNoMoreInteractions(orcidClientMock);

        profileItem = context.reloadEntity(profileItem);
        assertThat(profileItem, notNullValue());
        assertThat(profileItem.getMetadata(), hasItem(with("person.identifier.orcid", ORCID)));
        assertThat(profileItem.getMetadata(), hasItem(with("cris.orcid.access-token", ACCESS_TOKEN)));
        assertThat(profileItem.getMetadata(), hasItem(with("cris.orcid.refresh-token", REFRESH_TOKEN)));
        assertThat(profileItem.getMetadata(), hasItem(with("cris.orcid.scope", ORCID_SCOPES[0], 0)));
        assertThat(profileItem.getMetadata(), hasItem(with("cris.orcid.scope", ORCID_SCOPES[1], 1)));

        user = context.reloadEntity(user);
        assertThat(user.getNetid(), is(ORCID));
    }

    @Test
    public void testLinkProfileFromCodeProfileConfigurationWithAnotherEPersonWithSameNetId() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profileItem = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Test user")
            .withCrisOwner("Test User", user.getID().toString())
            .build();

        EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withPassword(password)
            .withEmail("test@anotherUser.it")
            .withNetId(ORCID)
            .build();

        context.restoreAuthSystemState();

        when(orcidClientMock.getAccessToken(CODE)).thenReturn(buildOrcidTokenResponse(ORCID, ACCESS_TOKEN));

        getClient().perform(get("/api/" + RestModel.CRIS + "/orcid/{itemId}", profileItem.getID())
            .param("code", CODE)
            .param("url", "/home"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(configurationService.getProperty("dspace.ui.url") + "/home"));

        verify(orcidClientMock).getAccessToken(CODE);
        verifyNoMoreInteractions(orcidClientMock);

        profileItem = context.reloadEntity(profileItem);
        assertThat(profileItem, notNullValue());
        assertThat(profileItem.getMetadata(), hasItem(with("person.identifier.orcid", ORCID)));
        assertThat(profileItem.getMetadata(), hasItem(with("cris.orcid.access-token", ACCESS_TOKEN)));
        assertThat(profileItem.getMetadata(), hasItem(with("cris.orcid.refresh-token", REFRESH_TOKEN)));
        assertThat(profileItem.getMetadata(), hasItem(with("cris.orcid.scope", ORCID_SCOPES[0], 0)));
        assertThat(profileItem.getMetadata(), hasItem(with("cris.orcid.scope", ORCID_SCOPES[1], 1)));

        user = context.reloadEntity(user);
        assertThat(user.getNetid(), nullValue());
    }

    @Test
    public void testLinkProfileFromCodeWithoutCode() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profileItem = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Test user")
            .withCrisOwner("Test User", user.getID().toString())
            .build();

        context.restoreAuthSystemState();

        when(orcidClientMock.getAccessToken(CODE)).thenReturn(buildOrcidTokenResponse(ORCID, ACCESS_TOKEN));

        getClient().perform(get("/api/" + RestModel.CRIS + "/orcid/{itemId}", profileItem.getID())
            .param("url", "/home"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(orcidClientMock);
    }

    @Test
    public void testLinkProfileFromCodeWithoutUrl() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profileItem = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Test user")
            .withCrisOwner("Test User", user.getID().toString())
            .build();

        context.restoreAuthSystemState();

        when(orcidClientMock.getAccessToken(CODE)).thenReturn(buildOrcidTokenResponse(ORCID, ACCESS_TOKEN));

        getClient().perform(get("/api/" + RestModel.CRIS + "/orcid/{itemId}", profileItem.getID())
            .param("code", CODE))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(orcidClientMock);
    }

    @Test
    public void testLinkProfileFromCodeWithProfileItemNotFound() throws Exception {

        when(orcidClientMock.getAccessToken(CODE)).thenReturn(buildOrcidTokenResponse(ORCID, ACCESS_TOKEN));

        getClient().perform(get("/api/" + RestModel.CRIS + "/orcid/{itemId}", "af097328-ac1c-4a3e-9eb4-069897874910")
            .param("code", CODE)
            .param("url", "/home"))
            .andExpect(status().isNotFound());

        verifyNoInteractions(orcidClientMock);
    }

    @Test
    public void testLinkProfileFromCodeWithInvalidProfile() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profileItem = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Test user")
            .build();

        context.restoreAuthSystemState();

        when(orcidClientMock.getAccessToken(CODE)).thenReturn(buildOrcidTokenResponse(ORCID, ACCESS_TOKEN));

        getClient().perform(get("/api/" + RestModel.CRIS + "/orcid/{itemId}", profileItem.getID())
            .param("code", CODE)
            .param("url", "/home"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(orcidClientMock);
    }

    @Test
    public void testWebhookPublicationRetrieving() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profileItem = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Walter White")
            .withOrcidIdentifier(ORCID)
            .build();

        context.restoreAuthSystemState();

        ExternalDataObject firstExternalDataObject = externalDataObject(ORCID, "12345", "EDO 1",
            "2020-02-01", "Description 1", List.of("Walter White"));
        ExternalDataObject secondExternalDataObject = externalDataObject(ORCID, "11111", "EDO 2",
            "2021-02-01", "Description 2", List.of("Walter White"));
        ExternalDataObject thirdExternalDataObject = externalDataObject(ORCID, "23456", "EDO 3",
            "2022-02-01", "Description 3", List.of("Walter White", "Jesse Pinkman"));

        List<ExternalDataObject> externalDataObjects = List.of(firstExternalDataObject,
            secondExternalDataObject, thirdExternalDataObject);

        when(externalDataProviderMock.searchExternalDataObjects(ORCID, 0, -1)).thenReturn(externalDataObjects);

        getClient().perform(post("/api/" + RestModel.CRIS + "/orcid/" + ORCID + "/webhook/" + getRegistrationToken()))
            .andExpect(status().isNoContent());

        List<Suggestion> suggestions = findAllUnprocessedSuggestions(profileItem);
        assertThat(suggestions, hasSize(3));
        assertThat(suggestions, has(suggestion(profileItem, ORCID + "::12345", "EDO 1",
            "2020-02-01", "Description 1", List.of("Walter White"))));
        assertThat(suggestions, has(suggestion(profileItem, ORCID + "::11111", "EDO 2",
            "2021-02-01", "Description 2", List.of("Walter White"))));
        assertThat(suggestions, has(suggestion(profileItem, ORCID + "::23456", "EDO 3",
            "2022-02-01", "Description 3", List.of("Walter White", "Jesse Pinkman"))));

        orcidPublicationLoader.flagRelatedSuggestionsAsProcessed(context, firstExternalDataObject);
        orcidPublicationLoader.rejectSuggestion(context, profileItem.getID(), ORCID + "::23456");

        suggestions = findAllUnprocessedSuggestions(profileItem);
        assertThat(suggestions, hasSize(1));
        assertThat(suggestions, has(suggestion(profileItem, ORCID + "::11111", "EDO 2",
            "2021-02-01", "Description 2", List.of("Walter White"))));

        // provide a new external data object and verify that a new suggestion is availabled

        ExternalDataObject fourthExternalDataObject = externalDataObject(ORCID, "77777", "EDO 4",
            "2019-02-01", "Description 4", List.of("Walter White", "Jesse Pinkman"));

        List<ExternalDataObject> newExternalDataObjects = List.of(firstExternalDataObject,
            secondExternalDataObject, thirdExternalDataObject, fourthExternalDataObject);

        when(externalDataProviderMock.searchExternalDataObjects(ORCID, 0, -1)).thenReturn(newExternalDataObjects);

        getClient().perform(post("/api/" + RestModel.CRIS + "/orcid/" + ORCID + "/webhook/" + getRegistrationToken()))
            .andExpect(status().isNoContent());

        List<Suggestion> newSuggestions = findAllUnprocessedSuggestions(profileItem);
        assertThat(newSuggestions, hasSize(2));
        assertThat(newSuggestions, has(suggestion(profileItem, ORCID + "::11111", "EDO 2",
            "2021-02-01", "Description 2", List.of("Walter White"))));
        assertThat(newSuggestions, has(suggestion(profileItem, ORCID + "::77777", "EDO 4",
            "2019-02-01", "Description 4", List.of("Walter White", "Jesse Pinkman"))));

        verify(externalDataProviderMock, times(2)).searchExternalDataObjects(ORCID, 0, -1);

    }

    @Test
    public void testWebhookWithNoProfilesFound() throws Exception {

        String webhookAccessToken = "b03a76e3-42af-45de-94ad-9d825141a152";
        when(orcidClientMock.getWebhookAccessToken()).thenReturn(buildOrcidTokenResponse(ORCID, webhookAccessToken));

        getClient().perform(post("/api/" + RestModel.CRIS + "/orcid/" + ORCID + "/webhook/" + getRegistrationToken()))
            .andExpect(status().isNoContent());

        verify(orcidClientMock).getWebhookAccessToken();
        verify(orcidClientMock).unregisterWebhook(eq(webhookAccessToken), eq(ORCID), any());
        verifyNoMoreInteractions(externalDataProviderMock, orcidClientMock);

    }

    @Test
    public void testWebhookWithWrongRegistrationToken() throws Exception {

        when(externalDataProviderMock.searchExternalDataObjects(ORCID, 0, -1)).thenReturn(emptyList());

        String randomRegistrationToken = UUID.randomUUID().toString();
        getClient().perform(post("/api/" + RestModel.CRIS + "/orcid/" + ORCID + "/webhook/" + randomRegistrationToken))
            .andExpect(status().isNoContent());

        verifyNoInteractions(externalDataProviderMock);
    }

    @Test
    public void testWebhookWithStillValidAccessToken() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profileItem = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Walter White")
            .withOrcidIdentifier(ORCID)
            .withOrcidAccessToken(ACCESS_TOKEN)
            .build();

        context.restoreAuthSystemState();

        when(externalDataProviderMock.searchExternalDataObjects(ORCID, 0, -1)).thenReturn(emptyList());
        when(orcidClientMock.getPerson(ACCESS_TOKEN, ORCID)).thenReturn(new Person());

        getClient().perform(post("/api/" + RestModel.CRIS + "/orcid/" + ORCID + "/webhook/" + getRegistrationToken()))
            .andExpect(status().isNoContent());

        verify(orcidClientMock).getPerson(ACCESS_TOKEN, ORCID);
        verifyNoMoreInteractions(orcidClientMock);

        profileItem = context.reloadEntity(profileItem);

        assertThat(profileItem.getMetadata(), has(metadataField("cris.orcid.access-token")));

    }

    @Test
    public void testWebhookWithInvalidAccessToken() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profileItem = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Walter White")
            .withOrcidIdentifier(ORCID)
            .withOrcidAccessToken(ACCESS_TOKEN)
            .withOrcidWebhook("2020-01-01")
            .withOrcidAuthenticated("2020-02-01")
            .build();

        context.restoreAuthSystemState();

        configurationService.setProperty("orcid.webhook.registration-mode", "all");

        when(externalDataProviderMock.searchExternalDataObjects(ORCID, 0, -1)).thenReturn(emptyList());
        when(orcidClientMock.getPerson(ACCESS_TOKEN, ORCID)).thenThrow(new OrcidClientException(401, "Unauthorized"));

        getClient().perform(post("/api/" + RestModel.CRIS + "/orcid/" + ORCID + "/webhook/" + getRegistrationToken()))
            .andExpect(status().isNoContent());

        verify(orcidClientMock).getPerson(ACCESS_TOKEN, ORCID);
        verifyNoMoreInteractions(orcidClientMock);

        profileItem = context.reloadEntity(profileItem);

        assertThat(profileItem.getMetadata(), has(not(metadataField("cris.orcid.access-token"))));
        assertThat(profileItem.getMetadata(), has(not(metadataField("cris.orcid.authenticated"))));

    }

    @Test
    public void testWebhookWithInvalidAccessTokenAndOnlyLinkedWebhook() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profileItem = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Walter White")
            .withOrcidIdentifier(ORCID)
            .withOrcidAccessToken(ACCESS_TOKEN)
            .withOrcidWebhook("2020-02-01")
            .withOrcidAuthenticated("2020-02-01")
            .build();

        context.restoreAuthSystemState();

        configurationService.setProperty("orcid.webhook.registration-mode", "only_linked");

        String webhookAccessToken = "b03a76e3-42af-45de-94ad-9d825141a152";

        when(externalDataProviderMock.searchExternalDataObjects(ORCID, 0, -1)).thenReturn(emptyList());
        when(orcidClientMock.getPerson(ACCESS_TOKEN, ORCID)).thenThrow(new OrcidClientException(401, "Unauthorized"));
        when(orcidClientMock.getWebhookAccessToken()).thenReturn(buildOrcidTokenResponse(ORCID, webhookAccessToken));

        getClient().perform(post("/api/" + RestModel.CRIS + "/orcid/" + ORCID + "/webhook/" + getRegistrationToken()))
            .andExpect(status().isNoContent());

        verify(orcidClientMock).getPerson(ACCESS_TOKEN, ORCID);
        verify(orcidClientMock).getWebhookAccessToken();
        verify(orcidClientMock).unregisterWebhook(eq(webhookAccessToken), eq(ORCID), any());
        verifyNoMoreInteractions(orcidClientMock);

        profileItem = context.reloadEntity(profileItem);

        assertThat(profileItem.getMetadata(), has(not(metadataField("cris.orcid.access-token"))));
        assertThat(profileItem.getMetadata(), has(not(metadataField("cris.orcid.authenticated"))));
        assertThat(profileItem.getMetadata(), has(not(metadataField("cris.orcid.webhook"))));
    }

    @Test
    public void testWebhookWithInvalidAccessTokenAndOnlyLinkedWebhookWithoutPreviousRegistration() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profileItem = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Walter White")
            .withOrcidIdentifier(ORCID)
            .withOrcidAccessToken(ACCESS_TOKEN)
            .build();

        context.restoreAuthSystemState();

        configurationService.setProperty("orcid.webhook.registration-mode", "only_linked");

        String webhookAccessToken = "b03a76e3-42af-45de-94ad-9d825141a152";

        when(externalDataProviderMock.searchExternalDataObjects(ORCID, 0, -1)).thenReturn(emptyList());
        when(orcidClientMock.getPerson(ACCESS_TOKEN, ORCID)).thenThrow(new OrcidClientException(401, "Unauthorized"));
        when(orcidClientMock.getWebhookAccessToken()).thenReturn(buildOrcidTokenResponse(ORCID, webhookAccessToken));

        getClient().perform(post("/api/" + RestModel.CRIS + "/orcid/" + ORCID + "/webhook/" + getRegistrationToken()))
            .andExpect(status().isNoContent());

        verify(orcidClientMock).getPerson(ACCESS_TOKEN, ORCID);
        verifyNoMoreInteractions(orcidClientMock);

        profileItem = context.reloadEntity(profileItem);

        assertThat(profileItem.getMetadata(), has(not(metadataField("cris.orcid.access-token"))));
        assertThat(profileItem.getMetadata(), has(not(metadataField("cris.orcid.authenticated"))));
    }

    @Test
    public void testWebhookWithInternalServerErrorFromOrcid() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profileItem = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Walter White")
            .withOrcidIdentifier(ORCID)
            .withOrcidAccessToken(ACCESS_TOKEN)
            .withOrcidAuthenticated("2020-02-01")
            .build();

        context.restoreAuthSystemState();

        when(externalDataProviderMock.searchExternalDataObjects(ORCID, 0, -1)).thenReturn(emptyList());
        when(orcidClientMock.getPerson(ACCESS_TOKEN, ORCID)).thenThrow(new OrcidClientException(500, "ERROR"));

        getClient().perform(post("/api/" + RestModel.CRIS + "/orcid/" + ORCID + "/webhook/" + getRegistrationToken()))
            .andExpect(status().isNoContent());

        verify(orcidClientMock).getPerson(ACCESS_TOKEN, ORCID);
        verifyNoMoreInteractions(orcidClientMock);

        profileItem = context.reloadEntity(profileItem);

        assertThat(profileItem.getMetadata(), has(metadataField("cris.orcid.access-token")));
        assertThat(profileItem.getMetadata(), has(metadataField("cris.orcid.authenticated")));

    }

    private Predicate<MetadataValue> metadataField(String metadataField) {
        return metadataValue -> metadataValue.getMetadataField().toString('.').equals(metadataField);
    }

    private ExternalDataObject externalDataObject(String orcid, String putCode, String title, String date,
        String description, List<String> authors) {

        String sourceIdentifier = originalExternalDataProvider.getSourceIdentifier();
        ExternalDataObject externalDataObject = new ExternalDataObject(sourceIdentifier);
        externalDataObject.setId(orcid + "::" + putCode);
        externalDataObject.setDisplayValue(title);
        externalDataObject.setValue(title);
        externalDataObject.addMetadata(new MetadataValueDTO("dc", "title", null, null, title));
        externalDataObject.addMetadata(new MetadataValueDTO("dc", "date", "issued", null, date));
        externalDataObject.addMetadata(new MetadataValueDTO("dc", "description", "abstract", null, description));
        authors.forEach(author -> externalDataObject
            .addMetadata(new MetadataValueDTO("dc", "contributor", "author", null, author)));

        return externalDataObject;
    }

    private Predicate<Suggestion> suggestion(Item target, String id, String title, String date,
        String description, List<String> authors) {

        String source = originalExternalDataProvider.getSourceIdentifier();

        return suggestion -> suggestion.getScore().equals(100d)
            && suggestion.getID().equals(source + ":" + target.getID().toString() + ":" + id)
            && suggestion.getEvidences().size() == 1
            && suggestion.getTarget().equals(target)
            && suggestion.getSource().equals(source)
            && suggestion.getDisplay().equals(title)
            && suggestion.getExternalSourceUri().equals(expectedExternalSourceUri(source, id))
            && contains(suggestion, "dc", "title", null, title)
            && contains(suggestion, "dc", "date", "issued", date)
            && contains(suggestion, "dc", "description", "abstract", description)
            && authors.stream().allMatch(author -> contains(suggestion, "dc", "contributor", "author", author));

    }

    private boolean contains(Suggestion suggestion, String schema, String element, String qualifier, String value) {
        return suggestion.getMetadata().stream()
            .filter(metadataValue -> StringUtils.equals(schema, metadataValue.getSchema()))
            .filter(metadataValue -> StringUtils.equals(element, metadataValue.getElement()))
            .filter(metadataValue -> StringUtils.equals(qualifier, metadataValue.getQualifier()))
            .anyMatch(metadataValue -> StringUtils.equals(value, metadataValue.getValue()));
    }

    private String expectedExternalSourceUri(String sourceIdentifier, String recordId) {
        String serverUrl = configurationService.getProperty("dspace.server.url");
        return serverUrl + "/api/integration/externalsources/" + sourceIdentifier + "/entryValues/" + recordId;
    }

    private String getRegistrationToken() {
        return configurationService.getProperty("orcid.webhook.registration-token");
    }

    private List<Suggestion> findAllUnprocessedSuggestions(Item profile) {
        return orcidPublicationLoader.findAllUnprocessedSuggestions(context, profile.getID(), 10, 0, true);
    }

    private OrcidTokenResponseDTO buildOrcidTokenResponse(String orcid, String accessToken) {
        OrcidTokenResponseDTO token = new OrcidTokenResponseDTO();
        token.setAccessToken(accessToken);
        token.setOrcid(orcid);
        token.setTokenType("Bearer");
        token.setRefreshToken(REFRESH_TOKEN);
        token.setName("Test User");
        token.setScope(String.join(" ", ORCID_SCOPES));
        return token;
    }
}
