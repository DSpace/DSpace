/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static java.lang.Thread.sleep;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.Base64;
import java.util.Map;
import javax.servlet.http.Cookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.builder.BitstreamBuilder;
import org.dspace.app.rest.builder.BundleBuilder;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.builder.GroupBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.matcher.AuthenticationStatusMatcher;
import org.dspace.app.rest.matcher.EPersonMatcher;
import org.dspace.app.rest.matcher.HalMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

/**
 * Integration test that covers various authentication scenarios
 *
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Giuseppe Digilio (giuseppe dot digilio at 4science dot it)
 */
public class AuthenticationRestControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    ConfigurationService configurationService;

    public static final String[] PASS_ONLY = {"org.dspace.authenticate.PasswordAuthentication"};
    public static final String[] SHIB_ONLY = {"org.dspace.authenticate.ShibAuthentication"};
    public static final String[] SHIB_AND_PASS =
            {"org.dspace.authenticate.ShibAuthentication",
             "org.dspace.authenticate.PasswordAuthentication"};
    public static final String[] SHIB_AND_IP =
            {"org.dspace.authenticate.IPAuthentication",
            "org.dspace.authenticate.ShibAuthentication"};

    @Before
    public void setup() throws Exception {
        super.setUp();
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", PASS_ONLY);
    }

    @Test
    @Ignore
    // Ignored until an endpoint is added to return all groups. Anonymous is not considered a direct group.
    public void testStatusAuthenticated() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authn/status").param("projection", "full"))

                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", AuthenticationStatusMatcher.matchFullEmbeds()))
                        .andExpect(jsonPath("$", AuthenticationStatusMatcher.matchLinks()))
                        //We expect the content type to be "application/hal+json;charset=UTF-8"
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")))

                        .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                        .andExpect(jsonPath("$._embedded.eperson",
                                EPersonMatcher.matchEPersonWithGroups(eperson.getEmail(), "Anonymous")))
        ;

        getClient(token).perform(get("/api/authn/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
        ;
    }

    @Test
    public void testStatusNotAuthenticated() throws Exception {

        getClient().perform(get("/api/authn/status"))

                   .andExpect(status().isOk())

                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.okay", is(true)))
                   .andExpect(jsonPath("$.authenticated", is(false)))
                   .andExpect(jsonPath("$.type", is("status")))
                   .andExpect(header().string("WWW-Authenticate",
                           "password realm=\"DSpace REST API\""));
    }

    @Test
    public void testStatusAuthenticatedWithCookie() throws Exception {
        //Enable Shibboleth login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);

        //Simulate that a shibboleth authentication has happened
        String token = getClient().perform(post("/api/authn/login")
                .requestAttr("SHIB-MAIL", eperson.getEmail())
                .requestAttr("SHIB-SCOPED-AFFILIATION", "faculty;staff"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getHeader(AUTHORIZATION_HEADER).replace("Bearer ", "");

        Cookie[] cookies = new Cookie[1];
        cookies[0] = new Cookie(AUTHORIZATION_COOKIE, token);

        //Check if we are authenticated with a status request with authorization cookie
        getClient().perform(get("/api/authn/status")
                .secure(true)
                .cookie(cookies))
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.type", is("status")));

        //Logout
        getClient(token).perform(get("/api/authn/logout"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void testTwoAuthenticationTokens() throws Exception {
        String token1 = getAuthToken(eperson.getEmail(), password);

        //Sleep so tokens are different
        sleep(1200);

        String token2 = getAuthToken(eperson.getEmail(), password);

        assertNotEquals(token1, token2);

        getClient(token1).perform(get("/api/authn/status").param("projection", "full"))

                         .andExpect(status().isOk())

                         //We expect the content type to be "application/hal+json;charset=UTF-8"
                         .andExpect(content().contentType(contentType))
                         .andExpect(jsonPath("$.okay", is(true)))
                         .andExpect(jsonPath("$.authenticated", is(true)))
                         .andExpect(jsonPath("$.type", is("status")))

                         .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                         .andExpect(jsonPath("$._embedded.eperson",
                                EPersonMatcher.matchEPersonOnEmail(eperson.getEmail())));

        getClient(token2).perform(get("/api/authn/status")
                         .param("projection", "full"))

                         .andExpect(status().isOk())

                         //We expect the content type to be "application/hal+json;charset=UTF-8"
                         .andExpect(content().contentType(contentType))
                         .andExpect(jsonPath("$.okay", is(true)))
                         .andExpect(jsonPath("$.authenticated", is(true)))
                         .andExpect(jsonPath("$.type", is("status")))

                         .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                         .andExpect(jsonPath("$._embedded.eperson",
                                EPersonMatcher.matchEPersonOnEmail(eperson.getEmail())));

    }

    @Test
    public void testTamperingWithToken() throws Exception {
        //Receive a valid token
        String token = getAuthToken(eperson.getEmail(), password);

        //Check it is really valid
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())

                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        //The group we try to add to our token
        context.turnOffAuthorisationSystem();
        Group internalGroup = GroupBuilder.createGroup(context)
                                          .withName("Internal Group")
                                          .build();
        context.restoreAuthSystemState();

        //Tamper with the token, insert id of group we don't belong to
        String[] jwtSplit = token.split("\\.");

        //We try to inject a special group ID to spoof membership
        String tampered = new String(Base64.getUrlEncoder().encode(
            new String(Base64.getUrlDecoder().decode(
                token.split("\\.")[1]))
                .replaceAll("\\[]", "[\"" + internalGroup.getID() + "\"]")
                .getBytes()));

        String tamperedToken = jwtSplit[0] + "." + tampered + "." + jwtSplit[2];

        //Try to get authenticated with the tampered token
        getClient(tamperedToken).perform(get("/api/authn/status"))
                                .andExpect(status().isOk())

                                .andExpect(jsonPath("$.okay", is(true)))
                                .andExpect(jsonPath("$.authenticated", is(false)))
                                .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void testLogout() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())

                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        getClient(token).perform(get("/api/authn/logout"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())

                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void testLogoutInvalidatesAllTokens() throws Exception {
        String token1 = getAuthToken(eperson.getEmail(), password);

        //Sleep so tokens are different
        sleep(1200);

        String token2 = getAuthToken(eperson.getEmail(), password);

        assertNotEquals(token1, token2);

        getClient(token1).perform(get("/api/authn/logout"));

        getClient(token1).perform(get("/api/authn/status"))
                         .andExpect(status().isOk())

                         .andExpect(jsonPath("$.okay", is(true)))
                         .andExpect(jsonPath("$.authenticated", is(false)))
                         .andExpect(jsonPath("$.type", is("status")));


        getClient(token2).perform(get("/api/authn/status"))
                         .andExpect(status().isOk())

                         .andExpect(jsonPath("$.okay", is(true)))
                         .andExpect(jsonPath("$.authenticated", is(false)))
                         .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void testRefreshToken() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        //Sleep so tokens are different
        sleep(1200);

        String newToken = getClient(token).perform(post("/api/authn/login"))
                                          .andExpect(status().isOk())
                                          .andReturn().getResponse().getHeader("Authorization");

        assertNotEquals(token, newToken);

        getClient(newToken).perform(get("/api/authn/status"))
                           .andExpect(status().isOk())

                           .andExpect(jsonPath("$.okay", is(true)))
                           .andExpect(jsonPath("$.authenticated", is(true)))
                           .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void testReuseTokenWithDifferentIP() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authn/status"))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.type", is("status")));

        getClient(token).perform(get("/api/authn/status")
                                     .header("X-FORWARDED-FOR", "1.1.1.1"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));


        getClient(token).perform(get("/api/authn/status")
                                    .with(ip("1.1.1.1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(false)))
                .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void testFailedLoginResponseCode() throws Exception {
        getClient().perform(post("/api/authn/login")
                                .param("user", eperson.getEmail()).param("password", "fakePassword"))

                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void testLoginLogoutStatusLink() throws Exception {
        getClient().perform(get("/api/authn"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._links.login.href", endsWith("login")))
                   .andExpect(jsonPath("$._links.logout.href", endsWith("logout")))
                   .andExpect(jsonPath("$._links.status.href", endsWith("status")));
    }

    /**
     * Check if we can just request a new token after we logged out
     *
     * @throws Exception
     */
    @Test
    public void testLoginAgainAfterLogout() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        //Check if we have a valid token
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        //Logout
        getClient(token).perform(get("/api/authn/logout"))
                        .andExpect(status().isNoContent());

        //Check if we are actually logged out
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));

        //request a new token
        token = getAuthToken(eperson.getEmail(), password);

        //Check if we succesfully authenticated again
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));


    }

    @Test
    public void testLoginEmptyRequest() throws Exception {
        getClient().perform(post("/api/authn/login"))
                   .andExpect(status().isUnauthorized())
                   .andExpect(status().reason(containsString("Login failed")));
    }

    @Test
    public void testLoginGetRequest() throws Exception {
       getClient().perform(get("/api/authn/login")
            .param("user", eperson.getEmail())
            .param("password", password))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void testShibbolethLoginURLWithDefaultLazyURL() throws Exception {
        context.turnOffAuthorisationSystem();
        //Enable Shibboleth login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);

        //Create a reviewers group
        Group reviewersGroup = GroupBuilder.createGroup(context)
                .withName("Reviewers")
                .build();

        //Faculty members are assigned to the Reviewers group
        configurationService.setProperty("authentication-shibboleth.role.faculty", "Reviewers");
        context.restoreAuthSystemState();

        getClient().perform(post("/api/authn/login").header("Referer", "http://my.uni.edu"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate",
                        "shibboleth realm=\"DSpace REST API\", " +
                                "location=\"https://localhost/Shibboleth.sso/Login?" +
                                "target=http%3A%2F%2Flocalhost%2Fapi%2Fauthn%2Fshibboleth%3F" +
                                "redirectUrl%3Dhttp%3A%2F%2Fmy.uni.edu\""));
    }

    @Test
    public void testShibbolethLoginURLWithServerlURLConteiningPort() throws Exception {
        context.turnOffAuthorisationSystem();
        //Enable Shibboleth login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);
        configurationService.setProperty("dspace.server.url", "http://localhost:8080/server");
        configurationService.setProperty("authentication-shibboleth.lazysession.secure", false);

        //Create a reviewers group
        Group reviewersGroup = GroupBuilder.createGroup(context)
                .withName("Reviewers")
                .build();

        //Faculty members are assigned to the Reviewers group
        configurationService.setProperty("authentication-shibboleth.role.faculty", "Reviewers");
        context.restoreAuthSystemState();

        getClient().perform(post("/api/authn/login").header("Referer", "http://my.uni.edu"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate",
                        "shibboleth realm=\"DSpace REST API\", " +
                                "location=\"http://localhost:8080/Shibboleth.sso/Login?" +
                                "target=http%3A%2F%2Flocalhost%3A8080%2Fserver%2Fapi%2Fauthn%2Fshibboleth%3F" +
                                "redirectUrl%3Dhttp%3A%2F%2Fmy.uni.edu\""));
    }

    @Test
    public void testShibbolethLoginURLWithConfiguredLazyURL() throws Exception {
        context.turnOffAuthorisationSystem();
        //Enable Shibboleth login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);
        configurationService.setProperty("authentication-shibboleth.lazysession.loginurl",
                "http://shibboleth.org/Shibboleth.sso/Login");

        //Create a reviewers group
        Group reviewersGroup = GroupBuilder.createGroup(context)
                .withName("Reviewers")
                .build();

        //Faculty members are assigned to the Reviewers group
        configurationService.setProperty("authentication-shibboleth.role.faculty", "Reviewers");
        context.restoreAuthSystemState();

        getClient().perform(post("/api/authn/login").header("Referer", "http://my.uni.edu"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate",
                        "shibboleth realm=\"DSpace REST API\", " +
                                "location=\"http://shibboleth.org/Shibboleth.sso/Login?" +
                                "target=http%3A%2F%2Flocalhost%2Fapi%2Fauthn%2Fshibboleth%3F" +
                                "redirectUrl%3Dhttp%3A%2F%2Fmy.uni.edu\""));
    }

    @Test
    public void testShibbolethLoginURLWithConfiguredLazyURLWithPort() throws Exception {
        context.turnOffAuthorisationSystem();
        //Enable Shibboleth login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);
        configurationService.setProperty("authentication-shibboleth.lazysession.loginurl",
                "http://shibboleth.org:8080/Shibboleth.sso/Login");

        //Create a reviewers group
        Group reviewersGroup = GroupBuilder.createGroup(context)
                .withName("Reviewers")
                .build();

        //Faculty members are assigned to the Reviewers group
        configurationService.setProperty("authentication-shibboleth.role.faculty", "Reviewers");
        context.restoreAuthSystemState();

        getClient().perform(post("/api/authn/login").header("Referer", "http://my.uni.edu"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate",
                        "shibboleth realm=\"DSpace REST API\", " +
                                "location=\"http://shibboleth.org:8080/Shibboleth.sso/Login?" +
                                "target=http%3A%2F%2Flocalhost%2Fapi%2Fauthn%2Fshibboleth%3F" +
                                "redirectUrl%3Dhttp%3A%2F%2Fmy.uni.edu\""));
    }

    @Test
    @Ignore
    // Ignored until an endpoint is added to return all groups
    public void testShibbolethLoginRequestAttribute() throws Exception {
        context.turnOffAuthorisationSystem();
        //Enable Shibboleth login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);

        //Create a reviewers group
        Group reviewersGroup = GroupBuilder.createGroup(context)
                .withName("Reviewers")
                .build();

        //Faculty members are assigned to the Reviewers group
        configurationService.setProperty("authentication-shibboleth.role.faculty", "Reviewers");
        context.restoreAuthSystemState();

        getClient().perform(post("/api/authn/login").header("Referer", "http://my.uni.edu"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate",
                        "shibboleth realm=\"DSpace REST API\", " +
                                "location=\"https://localhost/Shibboleth.sso/Login?" +
                                "target=http%3A%2F%2Flocalhost%2Fapi%2Fauthn%2Fshibboleth%3F" +
                                "redirectUrl%3Dhttp%3A%2F%2Fmy.uni.edu\""));

        //Simulate that a shibboleth authentication has happened

        String token = getClient().perform(post("/api/authn/login")
                    .requestAttr("SHIB-MAIL", eperson.getEmail())
                    .requestAttr("SHIB-SCOPED-AFFILIATION", "faculty;staff"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(AUTHORIZATION_HEADER);

        getClient(token).perform(get("/api/authn/status").param("projection", "full"))
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.type", is("status")))
                .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._embedded.eperson",
                        EPersonMatcher.matchEPersonWithGroups(eperson.getEmail(), "Anonymous", "Reviewers")));
    }

    @Test
    @Ignore
    // Ignored until an endpoint is added to return all groups
    public void testShibbolethLoginRequestHeaderWithIpAuthentication() throws Exception {
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_AND_IP);
        configurationService.setProperty("authentication-ip.Administrator", "123.123.123.123");


        getClient().perform(post("/api/authn/login")
                            .header("Referer", "http://my.uni.edu")
                            .with(ip("123.123.123.123")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate",
                        "ip realm=\"DSpace REST API\", shibboleth realm=\"DSpace REST API\", " +
                                "location=\"https://localhost/Shibboleth.sso/Login?" +
                                "target=http%3A%2F%2Flocalhost%2Fapi%2Fauthn%2Fshibboleth%3F" +
                                "redirectUrl%3Dhttp%3A%2F%2Fmy.uni.edu\""));

        //Simulate that a shibboleth authentication has happened
        String token = getClient().perform(post("/api/authn/login")
                    .with(ip("123.123.123.123"))
                    .header("SHIB-MAIL", eperson.getEmail()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(AUTHORIZATION_HEADER);

        getClient(token).perform(get("/api/authn/status").param("projection", "full")
                                    .with(ip("123.123.123.123")))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.type", is("status")))
                .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._embedded.eperson",
                        EPersonMatcher.matchEPersonWithGroups(eperson.getEmail(), "Anonymous", "Administrator")));

        //Simulate that a new shibboleth authentication has happened from another IP
        token = getClient().perform(post("/api/authn/login")
                .with(ip("234.234.234.234"))
                .header("SHIB-MAIL", eperson.getEmail()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(AUTHORIZATION_HEADER);

        getClient(token).perform(get("/api/authn/status").param("projection", "full")
                .with(ip("234.234.234.234")))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.type", is("status")))
                .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._embedded.eperson",
                        EPersonMatcher.matchEPersonWithGroups(eperson.getEmail(), "Anonymous")));
    }

    @Test
    public void testShibbolethAndPasswordAuthentication() throws Exception {
        //Enable Shibboleth and password login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_AND_PASS);

        //Check if WWW-Authenticate header contains shibboleth and password
        getClient().perform(get("/api/authn/status").header("Referer", "http://my.uni.edu"))
                .andExpect(status().isOk())
                .andExpect(header().string("WWW-Authenticate",
                        "shibboleth realm=\"DSpace REST API\", " +
                                "location=\"https://localhost/Shibboleth.sso/Login?" +
                                "target=http%3A%2F%2Flocalhost%2Fapi%2Fauthn%2Fshibboleth%3F" +
                                "redirectUrl%3Dhttp%3A%2F%2Fmy.uni.edu\"" +
                                ", password realm=\"DSpace REST API\""));

        //Simulate a password authentication
        String token = getAuthToken(eperson.getEmail(), password);

        //Check if we have a valid token
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        //Logout
        getClient(token).perform(get("/api/authn/logout"))
                        .andExpect(status().isNoContent());

        //Check if we are actually logged out
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));

        //Simulate that a shibboleth authentication has happened
        token = getClient().perform(post("/api/authn/login")
                .requestAttr("SHIB-MAIL", eperson.getEmail())
                .requestAttr("SHIB-SCOPED-AFFILIATION", "faculty;staff"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getHeader(AUTHORIZATION_HEADER).replace("Bearer ", "");

        //Check if we have a valid token
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        //Logout
        getClient(token).perform(get("/api/authn/logout"))
                        .andExpect(status().isNoContent());

        //Check if we are actually logged out (again)
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));

    }

    @Test
    public void testOnlyPasswordAuthenticationWorks() throws Exception {
        //Enable only password login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", PASS_ONLY);

        //Check if WWW-Authenticate header contains only
        getClient().perform(get("/api/authn/status").header("Referer", "http://my.uni.edu"))
            .andExpect(status().isOk())
            .andExpect(header().string("WWW-Authenticate",
                    "password realm=\"DSpace REST API\""));

        //Simulate a password authentication
        String token = getAuthToken(eperson.getEmail(), password);

        //Check if we have a valid token
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        //Logout
        getClient(token).perform(get("/api/authn/logout"))
                        .andExpect(status().isNoContent());

        //Check if we are actually logged out
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void testShibbolethAuthenticationDoesNotWorkWithPassOnly() throws Exception {
        //Enable only password login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", PASS_ONLY);

        //Check if WWW-Authenticate header contains only password
        getClient().perform(get("/api/authn/status").header("Referer", "http://my.uni.edu"))
        .andExpect(status().isOk())
        .andExpect(header().string("WWW-Authenticate",
                "password realm=\"DSpace REST API\""));

        //Check if a shibboleth authentication fails
        getClient().perform(post("/api/authn/login")
                .requestAttr("SHIB-MAIL", eperson.getEmail())
                .requestAttr("SHIB-SCOPED-AFFILIATION", "faculty;staff"))
            .andExpect(status().isUnauthorized());

    }

    @Test
    public void testOnlyShibbolethAuthenticationWorks() throws Exception {
        //Enable only Shibboleth login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);

        //Check if WWW-Authenticate header contains only shibboleth
        getClient().perform(get("/api/authn/status").header("Referer", "http://my.uni.edu"))
            .andExpect(status().isOk())
            .andExpect(header().string("WWW-Authenticate",
                    "shibboleth realm=\"DSpace REST API\", " +
                            "location=\"https://localhost/Shibboleth.sso/Login?" +
                            "target=http%3A%2F%2Flocalhost%2Fapi%2Fauthn%2Fshibboleth%3F" +
                            "redirectUrl%3Dhttp%3A%2F%2Fmy.uni.edu\""));

        //Simulate that a shibboleth authentication has happened
        String token = getClient().perform(post("/api/authn/login")
                .requestAttr("SHIB-MAIL", eperson.getEmail())
                .requestAttr("SHIB-SCOPED-AFFILIATION", "faculty;staff"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getHeader(AUTHORIZATION_HEADER);

        //Logout
        getClient(token).perform(get("/api/authn/logout"))
                        .andExpect(status().isNoContent());

        //Check if we are actually logged out
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void testPasswordAuthenticationDoesNotWorkWithShibOnly() throws Exception {
        //Enable only Shibboleth login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);

        getClient().perform(post("/api/authn/login")
                .param("user", eperson.getEmail())
                .param("password", password))
            .andExpect(status().isUnauthorized());

    }

    @Test
    public void testShortLivedToken() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        // Verify the main session salt doesn't change
        String salt = eperson.getSessionSalt();

        getClient(token).perform(post("/api/authn/shortlivedtokens"))
            .andExpect(jsonPath("$.token", notNullValue()))
            .andExpect(jsonPath("$.type", is("shortlivedtoken")))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/authn/shortlivedtokens")));

        assertEquals(salt, eperson.getSessionSalt());
    }

    @Test
    public void testShortLivedTokenNotAuthenticated() throws Exception {
        getClient().perform(post("/api/authn/shortlivedtokens"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void testShortLivedTokenToDowloadBitstream() throws Exception {
        Bitstream bitstream = createPrivateBitstream();
        String shortLivedToken = getShortLivedToken(eperson);

        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()
                + "/content?authentication-token=" + shortLivedToken))
            .andExpect(status().isOk());
    }

    @Test
    public void testShortLivedTokenToDowloadBitstreamUnauthorized() throws Exception {
        Bitstream bitstream = createPrivateBitstream();

        context.turnOffAuthorisationSystem();
        EPerson testEPerson = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("John", "Doe")
            .withEmail("UnauthorizedUser@example.com")
            .withPassword(password)
            .build();
        context.restoreAuthSystemState();

        String shortLivedToken = getShortLivedToken(testEPerson);
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()
                + "/content?authentication-token=" + shortLivedToken))
            .andExpect(status().isForbidden());
    }

    @Test
    public void testLoginTokenToDowloadBitstream() throws Exception {
        Bitstream bitstream = createPrivateBitstream();

        String loginToken = getAuthToken(eperson.getEmail(), password);
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()
                + "/content?authentication-token=" + loginToken))
            .andExpect(status().isForbidden());
    }

    @Test
    public void testExpiredShortLivedTokenToDowloadBitstream() throws Exception {
        Bitstream bitstream = createPrivateBitstream();
        configurationService.setProperty("jwt.shortLived.token.expiration", "1");
        String shortLivedToken = getShortLivedToken(eperson);
        Thread.sleep(1);
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()
                + "/content?authentication-token=" + shortLivedToken))
            .andExpect(status().isForbidden());
    }

    @Test
    public void testShortLivedAndLoginTokenSeparation() throws Exception {
        configurationService.setProperty("jwt.shortLived.token.expiration", "1");

        String token = getAuthToken(eperson.getEmail(), password);
        Thread.sleep(2);
        getClient(token).perform(get("/api/authn/status").param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated", is(true)));
    }

    // TODO: fix the exception. For now we want to verify a short lived token can't be used to login
    @Test(expected = Exception.class)
    public void testLoginWithShortLivedToken() throws Exception {
        String shortLivedToken = getShortLivedToken(eperson);

        getClient().perform(post("/api/authn/login?authentication-token=" + shortLivedToken))
            .andExpect(status().isInternalServerError());
        // TODO: This internal server error needs to be fixed. This should actually produce a forbidden status
        //.andExpect(status().isForbidden());
    }

    @Test
    public void testGenerateShortLivedTokenWithShortLivedToken() throws Exception {
        String shortLivedToken = getShortLivedToken(eperson);

        getClient().perform(post("/api/authn/shortlivedtokens?authentication-token=" + shortLivedToken))
            .andExpect(status().isForbidden());
    }

    private String getShortLivedToken(EPerson requestUser) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        String token = getAuthToken(requestUser.getEmail(), password);
        MvcResult mvcResult = getClient(token).perform(post("/api/authn/shortlivedtokens"))
            .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        return String.valueOf(map.get("token"));
    }

    private Bitstream createPrivateBitstream() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Bundle bundle1 = BundleBuilder.createBundle(context, publicItem1)
            .withName("TEST BUNDLE")
            .build();

        //2. An item restricted to a specific internal group
        Group staffGroup = GroupBuilder.createGroup(context)
            .withName("Staff")
            .addMember(eperson)
            .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                createBitstream(context, bundle1, is)
                .withName("Bitstream")
                .withDescription("description")
                .withMimeType("text/plain")
                .withReaderGroup(staffGroup)
                .build();
        }

        context.restoreAuthSystemState();

        return bitstream;
    }
}

