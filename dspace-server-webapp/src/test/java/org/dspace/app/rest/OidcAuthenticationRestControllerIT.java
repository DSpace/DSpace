/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.text.ParseException;
import java.util.Map;
import javax.servlet.http.Cookie;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import org.dspace.app.rest.model.AuthnRest;
import org.dspace.app.rest.security.jwt.EPersonClaimProvider;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authenticate.OidcAuthenticationBean;
import org.dspace.authenticate.oidc.OidcClient;
import org.dspace.authenticate.oidc.OidcClientException;
import org.dspace.authenticate.oidc.model.OidcTokenResponseDTO;
import org.dspace.builder.EPersonBuilder;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for {@link OidcAuthenticationRestController}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OidcAuthenticationRestControllerIT extends AbstractControllerIntegrationTest {

    private final static String CODE = "123456";
    private final static String EMAIL = "email";
    private final static String FIRST_NAME = "first_name";
    private final static String LAST_NAME = "last_name";

    private final static String ACCESS_TOKEN = "c41e37e5-c2de-4177-91d6-ed9e9d1f31bf";
    private final static String REFRESH_TOKEN = "0062a9eb-d4ec-4d94-9491-95dd75376d3e";
    private final static String[] OIDC_SCOPES = { "FirstScope", "SecondScope" };

    private OidcClient originalOidcClient;

    private OidcClient oidcClientMock = mock(OidcClient.class);

    private EPerson createdEperson;

    @Autowired
    private OidcAuthenticationBean oidcAuthentication;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private EPersonService ePersonService;

    @Before
    public void setup() {
        originalOidcClient = oidcAuthentication.getOidcClient();
        oidcAuthentication.setOidcClient(oidcClientMock);

        configurationService.setProperty("authentication-oidc.user-info.email", EMAIL);
        configurationService.setProperty("authentication-oidc.user-info.first-name", FIRST_NAME);
        configurationService.setProperty("authentication-oidc.user-info.last-name", LAST_NAME);

        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod",
            asList("org.dspace.authenticate.OidcAuthentication", "org.dspace.authenticate.PasswordAuthentication"));
    }

    @After
    public void after() throws Exception {
        oidcAuthentication.setOidcClient(originalOidcClient);
        if (createdEperson != null) {
            context.turnOffAuthorisationSystem();
            ePersonService.delete(context, createdEperson);
            context.restoreAuthSystemState();
        }
    }

    @Test
    public void testEPersonCreationViaOidcLogin() throws Exception {

        when(oidcClientMock.getAccessToken(CODE)).thenReturn(buildOidcTokenResponse(ACCESS_TOKEN));
        when(oidcClientMock.getUserInfo(ACCESS_TOKEN)).thenReturn(buildUserInfo("test@email.it", "Test", "User"));

        MvcResult mvcResult = getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/oidc")
            .param("code", CODE))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(configurationService.getProperty("dspace.ui.url")))
            .andExpect(cookie().exists("Authorization-cookie"))
            .andReturn();

        verify(oidcClientMock).getAccessToken(CODE);
        verify(oidcClientMock).getUserInfo(ACCESS_TOKEN);
        verifyNoMoreInteractions(oidcClientMock);

        String ePersonId = getEPersonIdFromAuthorizationCookie(mvcResult);

        createdEperson = ePersonService.find(context, UUIDUtils.fromString(ePersonId));
        assertThat(createdEperson, notNullValue());
        assertThat(createdEperson.getEmail(), equalTo("test@email.it"));
        assertThat(createdEperson.getFullName(), equalTo("Test User"));
        assertThat(createdEperson.getNetid(), equalTo("test@email.it"));
        assertThat(createdEperson.canLogIn(), equalTo(true));

    }

    @Test
    public void testEPersonCreationViaOidcLoginWithoutEmail() throws Exception {

        when(oidcClientMock.getAccessToken(CODE)).thenReturn(buildOidcTokenResponse(ACCESS_TOKEN));
        when(oidcClientMock.getUserInfo(ACCESS_TOKEN)).thenReturn(buildUserInfo("test@email.it"));

        MvcResult mvcResult = getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/oidc")
            .param("code", CODE))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(configurationService.getProperty("dspace.ui.url")))
            .andExpect(cookie().exists("Authorization-cookie"))
            .andReturn();

        verify(oidcClientMock).getAccessToken(CODE);
        verify(oidcClientMock).getUserInfo(ACCESS_TOKEN);
        verifyNoMoreInteractions(oidcClientMock);

        String ePersonId = getEPersonIdFromAuthorizationCookie(mvcResult);

        createdEperson = ePersonService.find(context, UUIDUtils.fromString(ePersonId));
        assertThat(createdEperson, notNullValue());
    }

    @Test
    public void testWithoutSelfRegistrationEnabled() throws Exception {

        configurationService.setProperty("authentication-oidc.can-self-register", "false");
        when(oidcClientMock.getAccessToken(CODE)).thenReturn(buildOidcTokenResponse(ACCESS_TOKEN));
        when(oidcClientMock.getUserInfo(ACCESS_TOKEN)).thenReturn(buildUserInfo("test@email.it"));

        MvcResult mvcResult = getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/oidc")
            .param("code", CODE))
            .andExpect(status().isUnauthorized())
            .andExpect(cookie().doesNotExist("Authorization-cookie"))
            .andExpect(header().exists("WWW-Authenticate"))
            .andReturn();

        String authenticateHeader = mvcResult.getResponse().getHeader("WWW-Authenticate");
        assertThat(authenticateHeader, containsString("oidc realm=\"DSpace REST API\""));

        verify(oidcClientMock).getAccessToken(CODE);
        verify(oidcClientMock).getUserInfo(ACCESS_TOKEN);
        verifyNoMoreInteractions(oidcClientMock);

    }

    @Test
    public void testWithoutAuthorizationCode() throws Exception {

        getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/oidc"))
            .andExpect(status().isUnauthorized())
            .andExpect(cookie().doesNotExist("Authorization-cookie"))
            .andExpect(header().exists("WWW-Authenticate"));

        verifyNoInteractions(oidcClientMock);

    }

    @Test
    public void testEPersonLoggedInByEmail() throws Exception {

        when(oidcClientMock.getAccessToken(CODE)).thenReturn(buildOidcTokenResponse(ACCESS_TOKEN));
        when(oidcClientMock.getUserInfo(ACCESS_TOKEN)).thenReturn(buildUserInfo("test@email.it"));

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withEmail("test@email.it")
            .withNameInMetadata("Test", "User")
            .withCanLogin(true)
            .build();

        context.restoreAuthSystemState();

        MvcResult mvcResult = getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/oidc")
            .param("code", CODE))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(configurationService.getProperty("dspace.ui.url")))
            .andExpect(cookie().exists("Authorization-cookie"))
            .andReturn();

        verify(oidcClientMock).getAccessToken(CODE);
        verify(oidcClientMock).getUserInfo(ACCESS_TOKEN);
        verifyNoMoreInteractions(oidcClientMock);

        String ePersonId = getEPersonIdFromAuthorizationCookie(mvcResult);
        assertThat(ePersonId, notNullValue());
        assertThat(ePersonId, equalTo(ePerson.getID().toString()));

    }

    @Test
    public void testEPersonCannotLogInByEmail() throws Exception {

        when(oidcClientMock.getAccessToken(CODE)).thenReturn(buildOidcTokenResponse(ACCESS_TOKEN));
        when(oidcClientMock.getUserInfo(ACCESS_TOKEN)).thenReturn(buildUserInfo("test@email.it"));

        context.turnOffAuthorisationSystem();

        EPersonBuilder.createEPerson(context)
            .withEmail("test@email.it")
            .withNameInMetadata("Test", "User")
            .withCanLogin(false)
            .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/oidc")
            .param("code", CODE))
            .andExpect(status().isUnauthorized())
            .andExpect(cookie().doesNotExist("Authorization-cookie"))
            .andExpect(header().exists("WWW-Authenticate"));

        verify(oidcClientMock).getAccessToken(CODE);
        verify(oidcClientMock).getUserInfo(ACCESS_TOKEN);
        verifyNoMoreInteractions(oidcClientMock);

    }

    @Test
    public void testNoAuthenticationIfAnErrorOccursRetrivingOidcToken() throws Exception {

        when(oidcClientMock.getAccessToken(CODE)).thenThrow(new OidcClientException(500, "internal error"));

        context.turnOffAuthorisationSystem();

        EPersonBuilder.createEPerson(context)
            .withEmail("test@email.it")
            .withNameInMetadata("Test", "User")
            .withCanLogin(false)
            .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/oidc")
            .param("code", CODE))
            .andExpect(status().isUnauthorized())
            .andExpect(cookie().doesNotExist("Authorization-cookie"))
            .andExpect(header().exists("WWW-Authenticate"));

        verify(oidcClientMock).getAccessToken(CODE);
        verifyNoMoreInteractions(oidcClientMock);

    }

    @Test
    public void testNoAuthenticationIfAnErrorOccursRetrivingOidcPerson() throws Exception {

        when(oidcClientMock.getAccessToken(CODE)).thenReturn(buildOidcTokenResponse(ACCESS_TOKEN));
        when(oidcClientMock.getUserInfo(ACCESS_TOKEN)).thenThrow(new OidcClientException(500, "Internal Error"));

        context.turnOffAuthorisationSystem();

        EPersonBuilder.createEPerson(context)
            .withEmail("test@email.it")
            .withNameInMetadata("Test", "User")
            .withCanLogin(false)
            .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/oidc")
            .param("code", CODE))
            .andExpect(status().isUnauthorized())
            .andExpect(cookie().doesNotExist("Authorization-cookie"))
            .andExpect(header().exists("WWW-Authenticate"));

        verify(oidcClientMock).getAccessToken(CODE);
        verify(oidcClientMock).getUserInfo(ACCESS_TOKEN);
        verifyNoMoreInteractions(oidcClientMock);

    }

    private OidcTokenResponseDTO buildOidcTokenResponse(String accessToken) {
        OidcTokenResponseDTO token = new OidcTokenResponseDTO();
        token.setAccessToken(accessToken);
        token.setTokenType("Bearer");
        token.setRefreshToken(REFRESH_TOKEN);
        token.setScope(String.join(" ", OIDC_SCOPES));
        return token;
    }

    private Map<String, Object> buildUserInfo(String email) {
        return Map.of(EMAIL, email);
    }

    private Map<String, Object> buildUserInfo(String email, String firstName, String lastName) {
        return Map.of(EMAIL, email, FIRST_NAME, firstName, LAST_NAME, lastName);
    }

    private String getEPersonIdFromAuthorizationCookie(MvcResult mvcResult) throws ParseException, JOSEException {
        Cookie authorizationCookie = mvcResult.getResponse().getCookie("Authorization-cookie");
        SignedJWT jwt = SignedJWT.parse(authorizationCookie.getValue());
        return (String) jwt.getJWTClaimsSet().getClaim(EPersonClaimProvider.EPERSON_ID);
    }
}
