/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.orcid.client.OrcidClient;
import org.dspace.app.orcid.model.OrcidTokenResponseDTO;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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

    private OrcidClient originalOrcidClient;

    private OrcidClient orcidClientMock = mock(OrcidClient.class);

    @Autowired
    private OrcidRestController orcidRestController;

    @Autowired
    private ConfigurationService configurationService;

    private Collection profileCollection;

    @Before
    public void setup() {
        originalOrcidClient = orcidRestController.getOrcidClient();
        orcidRestController.setOrcidClient(orcidClientMock);

        context.turnOffAuthorisationSystem();

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
        orcidRestController.setOrcidClient(originalOrcidClient);
    }

    @Test
    public void testProfileConfiguration() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profileItem = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Test user")
            .withCrisOwner("Test User", eperson.getID().toString())
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
    }

    @Test
    public void testWithoutCode() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profileItem = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Test user")
            .withCrisOwner("Test User", eperson.getID().toString())
            .build();

        context.restoreAuthSystemState();

        when(orcidClientMock.getAccessToken(CODE)).thenReturn(buildOrcidTokenResponse(ORCID, ACCESS_TOKEN));

        getClient().perform(get("/api/" + RestModel.CRIS + "/orcid/{itemId}", profileItem.getID())
            .param("url", "/home"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(orcidClientMock);
    }

    @Test
    public void testWithoutUrl() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profileItem = ItemBuilder.createItem(context, profileCollection)
            .withTitle("Test user")
            .withCrisOwner("Test User", eperson.getID().toString())
            .build();

        context.restoreAuthSystemState();

        when(orcidClientMock.getAccessToken(CODE)).thenReturn(buildOrcidTokenResponse(ORCID, ACCESS_TOKEN));

        getClient().perform(get("/api/" + RestModel.CRIS + "/orcid/{itemId}", profileItem.getID())
            .param("code", CODE))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(orcidClientMock);
    }

    @Test
    public void testWithProfileItemNotFound() throws Exception {

        when(orcidClientMock.getAccessToken(CODE)).thenReturn(buildOrcidTokenResponse(ORCID, ACCESS_TOKEN));

        getClient().perform(get("/api/" + RestModel.CRIS + "/orcid/{itemId}", "af097328-ac1c-4a3e-9eb4-069897874910")
            .param("code", CODE)
            .param("url", "/home"))
            .andExpect(status().isNotFound());

        verifyNoInteractions(orcidClientMock);
    }

    @Test
    public void testWithInvalidProfile() throws Exception {

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
