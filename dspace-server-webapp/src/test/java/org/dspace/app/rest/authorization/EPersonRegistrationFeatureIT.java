/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.authorization.impl.EPersonRegistrationFeature;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.converter.SiteConverter;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.Site;
import org.dspace.content.service.SiteService;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class EPersonRegistrationFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private ConverterService converterService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private SiteService siteService;

    @Autowired
    private SiteConverter siteConverter;

    @Autowired
    private Utils utils;

    private AuthorizationFeature epersonRegistrationFeature;

    public static final String[] SHIB_ONLY = {"org.dspace.authenticate.ShibAuthentication"};

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        epersonRegistrationFeature = authorizationFeatureService.find(EPersonRegistrationFeature.NAME);
    }

    @Test
    public void userRegistrationEnabledSuccessTest() throws Exception {

        Site site = siteService.findSite(context);
        SiteRest SiteRest = siteConverter.convert(site, Projection.DEFAULT);
        String siteUri = utils.linkToSingleResource(SiteRest, "self").getHref();


        getClient().perform(get("/api/authz/authorizations/search/object")
                                          .param("uri", siteUri)
                                          .param("feature", epersonRegistrationFeature.getName()))
                             .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", greaterThan(0)));
    }

    @Test
    public void userRegistrationDisabledUnAuthorizedTest() throws Exception {

        Site site = siteService.findSite(context);
        SiteRest SiteRest = siteConverter.convert(site, Projection.DEFAULT);
        String siteUri = utils.linkToSingleResource(SiteRest, "self").getHref();

        configurationService.setProperty("user.registration", false);

        getClient().perform(get("/api/authz/authorizations/search/object")
                .param("uri", siteUri)
                .param("feature", epersonRegistrationFeature.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(0)));

    }


    @Test
    public void userRegistrationEnabledShibTest() throws Exception {

        Site site = siteService.findSite(context);
        SiteRest SiteRest = siteConverter.convert(site, Projection.DEFAULT);
        String siteUri = utils.linkToSingleResource(SiteRest, "self").getHref();


        getClient().perform(get("/api/authz/authorizations/search/object")
                .param("uri", siteUri)
                .param("feature", epersonRegistrationFeature.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", greaterThan(0)));

        //Enable Shibboleth and password login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);

        getClient().perform(get("/api/authz/authorizations/search/object")
                .param("uri", siteUri)
                .param("feature", epersonRegistrationFeature.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(0)));

    }
}
