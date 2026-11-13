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

import org.dspace.app.rest.converter.SiteConverter;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.Site;
import org.dspace.content.service.SiteService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for {@link org.dspace.app.rest.authorization.impl.CanViewLoginStatisticsFeature}
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class CanViewLoginStatisticsFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private SiteService siteService;

    @Autowired
    private SiteConverter siteConverter;

    @Autowired
    private Utils utils;


    private SiteRest siteRest;

    @Before
    public void init() throws Exception {
        context.turnOffAuthorisationSystem();

        Site site = siteService.findSite(context);


        context.restoreAuthSystemState();

        siteRest = siteConverter.convert(site, Projection.DEFAULT);
    }

    @Test
    public void adminCanSeeLoginStatistics() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", "canViewLoginStatistics")
                .param("uri", utils.linkToSingleResource(siteRest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void notAdminCannotSeeLoginStatistics() throws Exception {
        String personToken = getAuthToken(eperson.getEmail(), password);
        getClient(personToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", "canViewLoginStatistics")
                .param("uri", utils.linkToSingleResource(siteRest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void anonymousCannotSeeLoginStatistics() throws Exception {
        getClient().perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", "canViewLoginStatistics")
                .param("uri", utils.linkToSingleResource(siteRest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }
}
