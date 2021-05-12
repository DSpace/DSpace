/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.webhook;

import static java.util.function.Predicate.not;
import static org.dspace.app.matcher.LambdaMatcher.has;
import static org.dspace.app.orcid.webhook.OrcidWebhookMode.ALL;
import static org.dspace.app.orcid.webhook.OrcidWebhookMode.DISABLED;
import static org.dspace.app.orcid.webhook.OrcidWebhookMode.ONLY_LINKED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.function.Predicate;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.orcid.client.OrcidClient;
import org.dspace.app.orcid.factory.OrcidServiceFactory;
import org.dspace.app.orcid.model.OrcidTokenResponseDTO;
import org.dspace.app.orcid.service.OrcidSynchronizationService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link OrcidWebhookConsumer}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidWebhookConsumerIT extends AbstractIntegrationTestWithDatabase {

    private static final String CLIENT_CREDENTIALS_TOKEN = "32c83ccb-c6d5-4981-b6ea-6a34a36de8ab";

    private static final String ACCESS_TOKEN = "0023a083-1844-47ee-ab12-d066e735e092";

    private static final String ORCID = "0000-1111-2222-3333";

    private OrcidWebhookServiceImpl orcidWebhookService;

    private ConfigurationService configurationService;

    private OrcidSynchronizationService orcidSynchronizationService;

    private ItemService itemService;

    private OrcidClient orcidClient;

    private OrcidClient orcidClientMock;

    private Collection persons;

    @Before
    public void setup() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        persons = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Person")
            .withName("Profiles")
            .build();

        context.restoreAuthSystemState();

        orcidWebhookService = (OrcidWebhookServiceImpl) OrcidServiceFactory.getInstance().getOrcidWebhookService();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        orcidSynchronizationService = OrcidServiceFactory.getInstance().getOrcidSynchronizationService();
        itemService = ContentServiceFactory.getInstance().getItemService();

        orcidClientMock = mock(OrcidClient.class);
        orcidClient = orcidWebhookService.getOrcidClient();

        orcidWebhookService.setOrcidClient(orcidClientMock);

        when(orcidClientMock.getWebhookAccessToken()).thenReturn(buildTokenResponse(CLIENT_CREDENTIALS_TOKEN));

    }

    @After
    public void after() {
        orcidWebhookService.setOrcidClient(orcidClient);
    }

    @Test
    public void testWebhookRegistrationWithAllMode() throws Exception {

        configurationService.setProperty("orcid.webhook.registration-mode", ALL.name().toLowerCase());

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, persons)
            .withTitle("Profile")
            .withOrcidIdentifier(ORCID)
            .build();

        context.restoreAuthSystemState();

        profile = context.reloadEntity(profile);
        assertThat(profile.getMetadata(), has(webhookMetadataField()));

        verify(orcidClientMock).getWebhookAccessToken();
        verify(orcidClientMock).registerWebhook(CLIENT_CREDENTIALS_TOKEN, ORCID, expectedWebhookUrl());
        verifyNoMoreInteractions(orcidClientMock);

        // trigger again the consumer to verify that no actions are performed

        addMetadata(profile, "crisrp", "education", null, "High School", null);
        context.commit();

        profile = context.reloadEntity(profile);
        assertThat(profile.getMetadata(), has(webhookMetadataField()));

        verifyNoMoreInteractions(orcidClientMock);

        // unlink the profile from orcid to verify that the webhook unregistration occurs

        orcidSynchronizationService.unlinkProfile(context, profile);

        profile = context.reloadEntity(profile);
        assertThat(profile.getMetadata(), has(not(webhookMetadataField())));

        verify(orcidClientMock, times(2)).getWebhookAccessToken();
        verify(orcidClientMock).unregisterWebhook(CLIENT_CREDENTIALS_TOKEN, ORCID, expectedWebhookUrl());
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testNoWebhookRegistrationOccursWithAllModeWithoutOrcid() throws Exception {

        configurationService.setProperty("orcid.webhook.registration-mode", ALL.name().toLowerCase());

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, persons)
            .withTitle("Profile")
            .build();

        context.restoreAuthSystemState();

        profile = context.reloadEntity(profile);
        assertThat(profile.getMetadata(), has(not(webhookMetadataField())));

        verifyNoInteractions(orcidClientMock);

    }

    @Test
    public void testWebhookRegistrationWithOnlyLinkedMode() throws Exception {

        configurationService.setProperty("orcid.webhook.registration-mode", ONLY_LINKED.name().toLowerCase());

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, persons)
            .withTitle("Profile")
            .withOrcidIdentifier(ORCID)
            .withOrcidAccessToken(ACCESS_TOKEN)
            .build();

        context.restoreAuthSystemState();

        profile = context.reloadEntity(profile);
        assertThat(profile.getMetadata(), has(webhookMetadataField()));

        verify(orcidClientMock).getWebhookAccessToken();
        verify(orcidClientMock).registerWebhook(CLIENT_CREDENTIALS_TOKEN, ORCID, expectedWebhookUrl());
        verifyNoMoreInteractions(orcidClientMock);

        // trigger again the consumer to verify that no actions are performed

        addMetadata(profile, "crisrp", "education", null, "High School", null);
        context.commit();

        profile = context.reloadEntity(profile);
        assertThat(profile.getMetadata(), has(webhookMetadataField()));

        verifyNoMoreInteractions(orcidClientMock);

        // unlink the profile from orcid to verify that the webhook unregistration occurs

        orcidSynchronizationService.unlinkProfile(context, profile);

        profile = context.reloadEntity(profile);
        assertThat(profile.getMetadata(), has(not(webhookMetadataField())));

        verify(orcidClientMock, times(2)).getWebhookAccessToken();
        verify(orcidClientMock).unregisterWebhook(CLIENT_CREDENTIALS_TOKEN, ORCID, expectedWebhookUrl());
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testNoWebhookRegistrationOccursWithOnlyLinkedModeWithoutOrcid() throws Exception {

        configurationService.setProperty("orcid.webhook.registration-mode", ONLY_LINKED.name().toLowerCase());

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, persons)
            .withTitle("Profile")
            .withOrcidAccessToken(ACCESS_TOKEN)
            .build();

        context.restoreAuthSystemState();

        profile = context.reloadEntity(profile);
        assertThat(profile.getMetadata(), has(not(webhookMetadataField())));

        verifyNoInteractions(orcidClientMock);

    }

    @Test
    public void testNoWebhookRegistrationOccursWithOnlyLinkedModeWithoutAccessToken() throws Exception {

        configurationService.setProperty("orcid.webhook.registration-mode", ONLY_LINKED.name().toLowerCase());

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, persons)
            .withTitle("Profile")
            .withOrcidIdentifier(ORCID)
            .build();

        context.restoreAuthSystemState();

        profile = context.reloadEntity(profile);
        assertThat(profile.getMetadata(), has(not(webhookMetadataField())));

        verifyNoInteractions(orcidClientMock);

    }

    @Test
    public void testNoWebhookRegistrationOccursWithDisabledMode() throws Exception {

        configurationService.setProperty("orcid.webhook.registration-mode", DISABLED.name().toLowerCase());

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, persons)
            .withTitle("Profile")
            .withOrcidIdentifier(ORCID)
            .withOrcidAccessToken(ACCESS_TOKEN)
            .build();

        context.restoreAuthSystemState();

        profile = context.reloadEntity(profile);
        assertThat(profile.getMetadata(), has(not(webhookMetadataField())));

        verifyNoInteractions(orcidClientMock);

    }

    @Test
    public void testNoWebhookRegistrationOccursWithUnknownMode() throws Exception {

        configurationService.setProperty("orcid.webhook.registration-mode", "unknown");

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, persons)
            .withTitle("Profile")
            .withOrcidIdentifier(ORCID)
            .withOrcidAccessToken(ACCESS_TOKEN)
            .build();

        context.restoreAuthSystemState();

        profile = context.reloadEntity(profile);
        assertThat(profile.getMetadata(), has(not(webhookMetadataField())));

        verifyNoInteractions(orcidClientMock);

    }

    private String expectedWebhookUrl() {
        return "http://localhost/api/cris/orcid/" + ORCID + "/webhook/01dfd257-c13f-43df-a0e2-9bb6c3cc7069";
    }

    private Predicate<MetadataValue> webhookMetadataField() {
        return value -> value.getMetadataField().toString('.').equals("cris.orcid.webhook");
    }

    private OrcidTokenResponseDTO buildTokenResponse(String accessToken) {
        OrcidTokenResponseDTO response = new OrcidTokenResponseDTO();
        response.setAccessToken(accessToken);
        return response;
    }

    private void addMetadata(Item item, String schema, String element, String qualifier, String value,
        String authority) throws Exception {
        context.turnOffAuthorisationSystem();
        item = context.reloadEntity(item);
        itemService.addMetadata(context, item, schema, element, qualifier, null, value, authority, 600);
        itemService.update(context, item);
        context.restoreAuthSystemState();
    }
}
