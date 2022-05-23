/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.authorization.impl.CanSendFeedbackFeature;
import org.dspace.app.rest.converter.SiteConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Site;
import org.dspace.content.service.SiteService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for the canSendFeedback authorization feature.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class CanSendFeedbackFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private SiteService siteService;
    @Autowired
    private SiteConverter siteConverter;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    final String feature = "canSendFeedback";

    private AuthorizationFeature canSendFeedbackFeature;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        canSendFeedbackFeature = authorizationFeatureService.find(CanSendFeedbackFeature.NAME);
    }

    @Test
    public void canSendFeedbackFeatureTest() throws Exception {
        configurationService.setProperty("feedback.recipient", "myemail@test.com");

        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, DefaultProjection.DEFAULT);

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenEperson = getAuthToken(eperson.getEmail(), password);

        // define authorizations
        Authorization authAdminSite = new Authorization(admin, canSendFeedbackFeature, siteRest);
        Authorization authAnonymousSite = new Authorization(null, canSendFeedbackFeature, siteRest);
        Authorization authEPersonSite = new Authorization(eperson, canSendFeedbackFeature, siteRest);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminSite.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(
                                        AuthorizationMatcher.matchAuthorization(authAdminSite))));

        getClient(tokenEperson).perform(get("/api/authz/authorizations/" + authEPersonSite.getID()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$", Matchers.is(
                                          AuthorizationMatcher.matchAuthorization(authEPersonSite))));

        getClient().perform(get("/api/authz/authorizations/" + authAnonymousSite.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                              AuthorizationMatcher.matchAuthorization(authAnonymousSite))));
    }

    @Test
    public void canSendFeedbackFeatureWithRecipientEmailNotConfiguredTest() throws Exception {
        configurationService.setProperty("feedback.recipient", null);

        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, DefaultProjection.DEFAULT);

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenEperson = getAuthToken(eperson.getEmail(), password);

        // define authorizations
        Authorization authAdminSite = new Authorization(admin, canSendFeedbackFeature, siteRest);
        Authorization authAnonymousSite = new Authorization(null, canSendFeedbackFeature, siteRest);
        Authorization authEPersonSite = new Authorization(eperson, canSendFeedbackFeature, siteRest);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminSite.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenEperson).perform(get("/api/authz/authorizations/" + authEPersonSite.getID()))
                               .andExpect(status().isNotFound());

        getClient().perform(get("/api/authz/authorizations/" + authAnonymousSite.getID()))
                   .andExpect(status().isNotFound());
    }

}