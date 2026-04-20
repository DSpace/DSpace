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

import org.dspace.app.rest.authorization.impl.QAAuthorizationFeature;
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
 * Test suite for the Quality Assurance Authorization feature
 * 
 * @author Francesco Bacchelli (francesco.bacchelli at 4science.it)
 *
 */
public class QAAuthorizationFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private SiteService siteService;

    @Autowired
    private SiteConverter siteConverter;

    @Autowired
    private ConfigurationService configurationService;


    private AuthorizationFeature qaAuthorizationFeature;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        qaAuthorizationFeature = authorizationFeatureService.find(QAAuthorizationFeature.NAME);
        context.restoreAuthSystemState();
    }

    @Test
    public void testQAAuthorizationSuccess() throws Exception {
        configurationService.setProperty("qaevents.enabled", true);
        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, DefaultProjection.DEFAULT);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        Authorization authAdminSite = new Authorization(admin, qaAuthorizationFeature, siteRest);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminSite.getID()))
                   .andExpect(jsonPath("$", Matchers.is(
                              AuthorizationMatcher.matchAuthorization(authAdminSite))));
    }

    @Test
    public void testQAAuthorizationFail() throws Exception {
        configurationService.setProperty("qaevents.enabled", false);
        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, DefaultProjection.DEFAULT);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        Authorization authAdminSite = new Authorization(admin, qaAuthorizationFeature, siteRest);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + authAdminSite.getID()))
            .andExpect(status().isNotFound());
    }
}
