/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.authorization.impl.LoginOnBehalfOfFeature;
import org.dspace.app.rest.converter.CommunityConverter;
import org.dspace.app.rest.converter.SiteConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Site;
import org.dspace.content.service.SiteService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class LoginOnBehalfOfFeatureRestIT extends AbstractControllerIntegrationTest {

    @Autowired
    private SiteConverter siteConverter;

    @Autowired
    private CommunityConverter communityConverter;

    @Autowired
    private SiteService siteService;

    @Autowired
    private Utils utils;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    private AuthorizationFeature loginOnBehalfOf;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        loginOnBehalfOf = authorizationFeatureService.find(LoginOnBehalfOfFeature.NAME);
    }

    @Test
    public void loginOnBehalfOfTest() throws Exception {
        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, Projection.DEFAULT);
        String siteUri = utils.linkToSingleResource(siteRest, "self").getHref();

        String token = getAuthToken(admin.getEmail(), password);

        configurationService.setProperty("org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff", true);
        configurationService.setProperty("webui.user.assumelogin", true);

        Authorization loginOnBehalfOfAuthorization = new Authorization(admin, loginOnBehalfOf, siteRest);
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", siteUri)
                                     .param("eperson", String.valueOf(admin.getID()))
                                     .param("feature", loginOnBehalfOf.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.authorizations", Matchers.hasItem(
                            AuthorizationMatcher.matchAuthorization(loginOnBehalfOfAuthorization))));
    }

    @Test
    public void loginOnBehalfNonSiteObjectOfTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        context.restoreAuthSystemState();

        CommunityRest communityRest = communityConverter.convert(parentCommunity, Projection.DEFAULT);
        String communityUri = utils.linkToSingleResource(communityRest, "self").getHref();

        String token = getAuthToken(admin.getEmail(), password);

        configurationService.setProperty("org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff", true);
        configurationService.setProperty("webui.user.assumelogin", true);

        Authorization loginOnBehalfOfAuthorization = new Authorization(admin, loginOnBehalfOf, communityRest);
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", communityUri)
                                     .param("eperson", String.valueOf(admin.getID()))
                                     .param("feature", loginOnBehalfOf.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void loginOnBehalfOfNonAdminUserNotFoundTest() throws Exception {
        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, Projection.DEFAULT);
        String siteUri = utils.linkToSingleResource(siteRest, "self").getHref();

        String token = getAuthToken(admin.getEmail(), password);

        configurationService.setProperty("org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff", true);
        configurationService.setProperty("webui.user.assumelogin", true);

        Authorization loginOnBehalfOfAuthorization = new Authorization(eperson, loginOnBehalfOf, siteRest);
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", siteUri)
                                     .param("eperson", String.valueOf(eperson.getID()))
                                     .param("feature", loginOnBehalfOf.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void loginOnBehalfOfNonAdminUserAssumeLoginPropertyFalseNotFoundTest() throws Exception {
        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, Projection.DEFAULT);
        String siteUri = utils.linkToSingleResource(siteRest, "self").getHref();

        String token = getAuthToken(admin.getEmail(), password);

        configurationService.setProperty("org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff", true);
        configurationService.setProperty("webui.user.assumelogin", false);

        Authorization loginOnBehalfOfAuthorization = new Authorization(eperson, loginOnBehalfOf, siteRest);
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", siteUri)
                                     .param("eperson", String.valueOf(eperson.getID()))
                                     .param("feature", loginOnBehalfOf.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void loginOnBehalfOfAssumeLoginPropertyFalseNotFoundTest() throws Exception {
        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, Projection.DEFAULT);
        String siteUri = utils.linkToSingleResource(siteRest, "self").getHref();

        String token = getAuthToken(admin.getEmail(), password);

        configurationService.setProperty("org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff", true);
        configurationService.setProperty("webui.user.assumelogin", false);

        Authorization loginOnBehalfOfAuthorization = new Authorization(admin, loginOnBehalfOf, siteRest);
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", siteUri)
                                     .param("eperson", String.valueOf(admin.getID()))
                                     .param("feature", loginOnBehalfOf.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)));
    }
}
