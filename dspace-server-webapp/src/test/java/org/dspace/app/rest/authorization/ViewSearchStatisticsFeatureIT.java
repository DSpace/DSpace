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

import org.dspace.app.rest.converter.CommunityConverter;
import org.dspace.app.rest.converter.SiteConverter;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Community;
import org.dspace.content.Site;
import org.dspace.content.service.SiteService;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for the canViewSearchStatistics authorization feature
 */
public class ViewSearchStatisticsFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private Utils utils;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private SiteConverter siteConverter;

    @Autowired
    SiteService siteService;


    @Autowired
    private CommunityConverter communityConverter;

    private Site site;
    private SiteRest siteRest;
    private Community communityA;
    private CommunityRest communityARest;

    final String feature = "canViewSearchStatistics";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();


        site = siteService.findSite(context);
        siteRest = siteConverter.convert(site, Projection.DEFAULT);
        communityA = CommunityBuilder.createCommunity(context)
                                     .withName("communityA")
                                     .build();
        context.restoreAuthSystemState();
        communityARest = communityConverter.convert(communityA, Projection.DEFAULT);

    }

    @Test
    public void adminCommunityAdminRequiredNotFound() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(communityARest, "self").getHref()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.page.totalElements", is(0)))
                             .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void adminSiteAdminRequiredSuccess() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(siteRest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void ePersonSiteAdminRequiredNotFound() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(siteRest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void ePersonSiteAdminNotRequiredSuccess() throws Exception {
        configurationService.setProperty("usage-statistics.authorization.admin.search", false);

        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(siteRest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void anonymousSiteAdminNotRequiredSuccess() throws Exception {
        configurationService.setProperty("usage-statistics.authorization.admin.search", false);

        getClient().perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(siteRest, "self").getHref()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                               .andExpect(jsonPath("$._embedded").exists());
    }
}
