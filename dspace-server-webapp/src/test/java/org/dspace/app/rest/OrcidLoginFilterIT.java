/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static java.util.Arrays.asList;
import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.UUID;
import javax.servlet.http.Cookie;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import org.dspace.app.rest.model.AuthnRest;
import org.dspace.app.rest.security.OrcidLoginFilter;
import org.dspace.app.rest.security.jwt.EPersonClaimProvider;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authenticate.OrcidAuthenticationBean;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.orcid.OrcidToken;
import org.dspace.orcid.client.OrcidClient;
import org.dspace.orcid.exception.OrcidClientException;
import org.dspace.orcid.model.OrcidTokenResponseDTO;
import org.dspace.orcid.service.OrcidTokenService;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.orcid.jaxb.model.v3.release.record.Email;
import org.orcid.jaxb.model.v3.release.record.Emails;
import org.orcid.jaxb.model.v3.release.record.FamilyName;
import org.orcid.jaxb.model.v3.release.record.GivenNames;
import org.orcid.jaxb.model.v3.release.record.Name;
import org.orcid.jaxb.model.v3.release.record.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for {@link OrcidLoginFilter}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidLoginFilterIT extends AbstractControllerIntegrationTest {

    public static final String[] PASSWORD_ONLY = { "org.dspace.authenticate.PasswordAuthentication" };

    private final static String ORCID = "0000-1111-2222-3333";
    private final static String CODE = "123456";

    private final static String ACCESS_TOKEN = "c41e37e5-c2de-4177-91d6-ed9e9d1f31bf";
    private final static String[] ORCID_SCOPES = { "FirstScope", "SecondScope" };

    private OrcidClient originalOrcidClient;

    private OrcidClient orcidClientMock = mock(OrcidClient.class);

    private EPerson createdEperson;

    @Autowired
    private OrcidAuthenticationBean orcidAuthentication;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private OrcidTokenService orcidTokenService;

    @Before
    public void setup() {
        originalOrcidClient = orcidAuthentication.getOrcidClient();
        orcidAuthentication.setOrcidClient(orcidClientMock);

        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod",
            asList("org.dspace.authenticate.OrcidAuthentication",
                "org.dspace.authenticate.PasswordAuthentication"));
    }

    @After
    public void after() throws Exception {
        orcidAuthentication.setOrcidClient(originalOrcidClient);
        if (createdEperson != null) {
            context.turnOffAuthorisationSystem();
            ePersonService.delete(context, createdEperson);
            context.restoreAuthSystemState();
        }
        orcidTokenService.deleteAll(context);
    }

    @Test
    public void testNoRedirectIfOrcidDisabled() throws Exception {
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", PASSWORD_ONLY);

        getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/orcid")
            .param("code", CODE))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void testEPersonCreationViaOrcidLogin() throws Exception {

        when(orcidClientMock.getAccessToken(CODE)).thenReturn(buildOrcidTokenResponse(ORCID, ACCESS_TOKEN));
        when(orcidClientMock.getPerson(ACCESS_TOKEN, ORCID)).thenReturn(buildPerson("Test", "User", "test@email.it"));

        MvcResult mvcResult = getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/orcid")
            .param("code", CODE))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(configurationService.getProperty("dspace.ui.url")))
            .andExpect(cookie().exists("Authorization-cookie"))
            .andReturn();

        verify(orcidClientMock).getAccessToken(CODE);
        verify(orcidClientMock).getPerson(ACCESS_TOKEN, ORCID);
        verifyNoMoreInteractions(orcidClientMock);

        String ePersonId = getEPersonIdFromAuthorizationCookie(mvcResult);

        createdEperson = ePersonService.find(context, UUIDUtils.fromString(ePersonId));
        assertThat(createdEperson, notNullValue());
        assertThat(createdEperson.getEmail(), equalTo("test@email.it"));
        assertThat(createdEperson.getFullName(), equalTo("Test User"));
        assertThat(createdEperson.getNetid(), equalTo(ORCID));
        assertThat(createdEperson.canLogIn(), equalTo(true));
        assertThat(createdEperson.getMetadata(), hasItem(with("eperson.orcid", ORCID)));
        assertThat(createdEperson.getMetadata(), hasItem(with("eperson.orcid.scope", ORCID_SCOPES[0], 0)));
        assertThat(createdEperson.getMetadata(), hasItem(with("eperson.orcid.scope", ORCID_SCOPES[1], 1)));

        assertThat(getOrcidAccessToken(createdEperson), is(ACCESS_TOKEN));
    }

    @Test
    public void testEPersonCreationViaOrcidLoginWithoutEmail() throws Exception {

        when(orcidClientMock.getAccessToken(CODE)).thenReturn(buildOrcidTokenResponse(ORCID, ACCESS_TOKEN));
        when(orcidClientMock.getPerson(ACCESS_TOKEN, ORCID)).thenReturn(buildPerson("Test", "User"));

        getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/orcid")
            .param("code", CODE))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost:4000/error?status=401&code=orcid.generic-error"));

        verify(orcidClientMock).getAccessToken(CODE);
        verify(orcidClientMock).getPerson(ACCESS_TOKEN, ORCID);
        verifyNoMoreInteractions(orcidClientMock);
    }

    @Test
    public void testWithoutSelfRegistrationEnabled() throws Exception {

        configurationService.setProperty("authentication-orcid.can-self-register", "false");

        when(orcidClientMock.getAccessToken(CODE)).thenReturn(buildOrcidTokenResponse(ORCID, ACCESS_TOKEN));
        when(orcidClientMock.getPerson(ACCESS_TOKEN, ORCID)).thenReturn(buildPerson("Test", "User"));

        getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/orcid")
            .param("code", CODE))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost:4000/error?status=401&code=orcid.generic-error"))
            .andExpect(cookie().doesNotExist("Authorization-cookie"))
            .andReturn();

        verify(orcidClientMock).getAccessToken(CODE);
        verify(orcidClientMock).getPerson(ACCESS_TOKEN, ORCID);
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testWithoutAuthorizationCode() throws Exception {

        getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/orcid"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(orcidClientMock);

    }

    @Test
    public void testEPersonLoggedInByNetId() throws Exception {

        when(orcidClientMock.getAccessToken(CODE)).thenReturn(buildOrcidTokenResponse(ORCID, ACCESS_TOKEN));

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withEmail("test@email.it")
            .withNetId(ORCID)
            .withNameInMetadata("Test", "User")
            .withCanLogin(true)
            .build();

        context.restoreAuthSystemState();

        MvcResult mvcResult = getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/orcid")
            .param("code", CODE))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(configurationService.getProperty("dspace.ui.url")))
            .andExpect(cookie().exists("Authorization-cookie"))
            .andReturn();

        verify(orcidClientMock).getAccessToken(CODE);
        verifyNoMoreInteractions(orcidClientMock);

        String ePersonId = getEPersonIdFromAuthorizationCookie(mvcResult);
        assertThat(ePersonId, notNullValue());
        assertThat(ePersonId, equalTo(ePerson.getID().toString()));

    }

    @Test
    public void testEPersonCannotLogInByNetId() throws Exception {

        when(orcidClientMock.getAccessToken(CODE)).thenReturn(buildOrcidTokenResponse(ORCID, ACCESS_TOKEN));

        context.turnOffAuthorisationSystem();

        EPersonBuilder.createEPerson(context)
            .withEmail("test@email.it")
            .withNetId(ORCID)
            .withNameInMetadata("Test", "User")
            .withCanLogin(false)
            .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/orcid")
            .param("code", CODE))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost:4000/error?status=401&code=orcid.generic-error"))
            .andExpect(cookie().doesNotExist("Authorization-cookie"));

        verify(orcidClientMock).getAccessToken(CODE);
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testEPersonLoggedInByEmail() throws Exception {

        when(orcidClientMock.getAccessToken(CODE)).thenReturn(buildOrcidTokenResponse(ORCID, ACCESS_TOKEN));
        when(orcidClientMock.getPerson(ACCESS_TOKEN, ORCID)).thenReturn(buildPerson("Test", "User", "test@email.it"));

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withEmail("test@email.it")
            .withNameInMetadata("Test", "User")
            .withCanLogin(true)
            .build();

        context.restoreAuthSystemState();

        MvcResult mvcResult = getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/orcid")
            .param("code", CODE))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(configurationService.getProperty("dspace.ui.url")))
            .andExpect(cookie().exists("Authorization-cookie"))
            .andReturn();

        verify(orcidClientMock).getAccessToken(CODE);
        verify(orcidClientMock).getPerson(ACCESS_TOKEN, ORCID);
        verifyNoMoreInteractions(orcidClientMock);

        String ePersonId = getEPersonIdFromAuthorizationCookie(mvcResult);
        assertThat(ePersonId, notNullValue());
        assertThat(ePersonId, equalTo(ePerson.getID().toString()));

    }

    @Test
    public void testEPersonCannotLogInByEmail() throws Exception {

        when(orcidClientMock.getAccessToken(CODE)).thenReturn(buildOrcidTokenResponse(ORCID, ACCESS_TOKEN));
        when(orcidClientMock.getPerson(ACCESS_TOKEN, ORCID)).thenReturn(buildPerson("Test", "User", "test@email.it"));

        context.turnOffAuthorisationSystem();

        EPersonBuilder.createEPerson(context)
            .withEmail("test@email.it")
            .withNameInMetadata("Test", "User")
            .withCanLogin(false)
            .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/orcid")
            .param("code", CODE))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost:4000/error?status=401&code=orcid.generic-error"))
            .andExpect(cookie().doesNotExist("Authorization-cookie"));

        verify(orcidClientMock).getAccessToken(CODE);
        verify(orcidClientMock).getPerson(ACCESS_TOKEN, ORCID);
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testNoAuthenticationIfAnErrorOccursRetrivingOrcidToken() throws Exception {

        when(orcidClientMock.getAccessToken(CODE)).thenThrow(new OrcidClientException(500, "internal error"));

        context.turnOffAuthorisationSystem();

        EPersonBuilder.createEPerson(context)
            .withEmail("test@email.it")
            .withNameInMetadata("Test", "User")
            .withCanLogin(false)
            .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/orcid")
            .param("code", CODE))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost:4000/error?status=401&code=orcid.generic-error"))
            .andExpect(cookie().doesNotExist("Authorization-cookie"));

        verify(orcidClientMock).getAccessToken(CODE);
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testNoAuthenticationIfAnErrorOccursRetrivingOrcidPerson() throws Exception {

        when(orcidClientMock.getAccessToken(CODE)).thenReturn(buildOrcidTokenResponse(ORCID, ACCESS_TOKEN));
        when(orcidClientMock.getPerson(ACCESS_TOKEN, ORCID)).thenThrow(new OrcidClientException(500, "Internal Error"));

        context.turnOffAuthorisationSystem();

        EPersonBuilder.createEPerson(context)
            .withEmail("test@email.it")
            .withNameInMetadata("Test", "User")
            .withCanLogin(false)
            .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/orcid")
            .param("code", CODE))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost:4000/error?status=401&code=orcid.generic-error"))
            .andExpect(cookie().doesNotExist("Authorization-cookie"));

        verify(orcidClientMock).getAccessToken(CODE);
        verify(orcidClientMock).getPerson(ACCESS_TOKEN, ORCID);
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testLoggedInEPersonWithProfile() throws Exception {

        when(orcidClientMock.getAccessToken(CODE)).thenReturn(buildOrcidTokenResponse(ORCID, ACCESS_TOKEN));

        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
            .withName("Community")
            .build();

        CollectionBuilder.createCollection(context, community)
            .withName("Persons")
            .withEntityType("Person")
            .build();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withEmail("test@email.it")
            .withPassword(password)
            .withNetId(ORCID)
            .withNameInMetadata("Test", "User")
            .withCanLogin(true)
            .build();

        context.restoreAuthSystemState();

        String ePersonToken = getAuthToken("test@email.it", password);

        getClient(ePersonToken).perform(post("/api/eperson/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(ePerson.getID().toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")));

        MvcResult mvcResult = getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/orcid")
            .param("code", CODE))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(configurationService.getProperty("dspace.ui.url")))
            .andExpect(cookie().exists("Authorization-cookie"))
            .andReturn();

        verify(orcidClientMock).getAccessToken(CODE);
        verifyNoMoreInteractions(orcidClientMock);

        String ePersonId = getEPersonIdFromAuthorizationCookie(mvcResult);
        assertThat(ePersonId, notNullValue());
        assertThat(ePersonId, equalTo(ePerson.getID().toString()));

        String profileItemId = getItemIdByProfileId(ePersonToken, ePersonId);
        Item profileItem = itemService.find(context, UUID.fromString(profileItemId));
        assertThat(profileItem, notNullValue());
        assertThat(profileItem.getMetadata(), hasItem(with("person.identifier.orcid", ORCID)));
        assertThat(profileItem.getMetadata(), hasItem(with("dspace.orcid.scope", ORCID_SCOPES[0], 0)));
        assertThat(profileItem.getMetadata(), hasItem(with("dspace.orcid.scope", ORCID_SCOPES[1], 1)));

        assertThat(getOrcidAccessToken(profileItem), is(ACCESS_TOKEN));

    }

    @Test
    public void testRedirectToGivenTrustedUrl() throws Exception {

        context.turnOffAuthorisationSystem();

        EPersonBuilder.createEPerson(context)
            .withEmail("test@email.it")
            .withNetId(ORCID)
            .withNameInMetadata("Test", "User")
            .withCanLogin(true)
            .build();

        context.restoreAuthSystemState();

        when(orcidClientMock.getAccessToken(CODE)).thenReturn(buildOrcidTokenResponse(ORCID, ACCESS_TOKEN));

        String token = getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/orcid")
            .param("redirectUrl", "http://localhost:8080/server/api/authn/status")
            .param("code", CODE))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost:8080/server/api/authn/status"))
            .andReturn().getResponse().getHeader("Authorization");

        getClient(token).perform(get("/api/authn/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated", is(true)))
            .andExpect(jsonPath("$.authenticationMethod", is("orcid")));

        verify(orcidClientMock).getAccessToken(CODE);
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testRedirectToGivenUntrustedUrl() throws Exception {

        context.turnOffAuthorisationSystem();

        EPersonBuilder.createEPerson(context)
            .withEmail("test@email.it")
            .withNetId(ORCID)
            .withNameInMetadata("Test", "User")
            .withCanLogin(true)
            .build();

        context.restoreAuthSystemState();

        when(orcidClientMock.getAccessToken(CODE)).thenReturn(buildOrcidTokenResponse(ORCID, ACCESS_TOKEN));

        getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/orcid")
            .param("redirectUrl", "http://dspace.org")
            .param("code", CODE))
            .andExpect(status().isBadRequest());

        verify(orcidClientMock).getAccessToken(CODE);
        verifyNoMoreInteractions(orcidClientMock);

    }

    private OrcidTokenResponseDTO buildOrcidTokenResponse(String orcid, String accessToken) {
        OrcidTokenResponseDTO token = new OrcidTokenResponseDTO();
        token.setAccessToken(accessToken);
        token.setOrcid(orcid);
        token.setTokenType("Bearer");
        token.setName("Test User");
        token.setScope(String.join(" ", ORCID_SCOPES));
        return token;
    }

    private Person buildPerson(String firstName, String lastName) {
        return buildPerson(firstName, lastName, null);
    }

    private Person buildPerson(String firstName, String lastName, String email) {
        Person person = new Person();

        if (email != null) {
            person.setEmails(buildEmails(email));
        }

        Name name = new Name();
        name.setFamilyName(new FamilyName(lastName));
        name.setGivenNames(new GivenNames(firstName));
        person.setName(name);

        return person;
    }

    private Emails buildEmails(String email) {
        Email emailObject = new Email();
        emailObject.setEmail(email);
        Emails emails = new Emails();
        emails.getEmails().add(emailObject);
        return emails;
    }

    private String getEPersonIdFromAuthorizationCookie(MvcResult mvcResult) throws ParseException, JOSEException {
        Cookie authorizationCookie = mvcResult.getResponse().getCookie("Authorization-cookie");
        SignedJWT jwt = SignedJWT.parse(authorizationCookie.getValue());
        return (String) jwt.getJWTClaimsSet().getClaim(EPersonClaimProvider.EPERSON_ID);
    }

    private String getItemIdByProfileId(String token, String id) throws SQLException, Exception {
        MvcResult result = getClient(token).perform(get("/api/eperson/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String getOrcidAccessToken(EPerson ePerson) {
        OrcidToken orcidToken = orcidTokenService.findByEPerson(context, ePerson);
        return orcidToken != null ? orcidToken.getAccessToken() : null;
    }

    private String getOrcidAccessToken(Item item) {
        OrcidToken orcidToken = orcidTokenService.findByProfileItem(context, item);
        return orcidToken != null ? orcidToken.getAccessToken() : null;
    }
}
