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

import java.io.InputStream;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.converter.BitstreamConverter;
import org.dspace.app.rest.converter.CollectionConverter;
import org.dspace.app.rest.converter.CommunityConverter;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.converter.SiteConverter;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.content.service.SiteService;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for the canViewUsageStatistics authorization feature
 */
public class ViewUsageStatisticsFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private Utils utils;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private SiteConverter siteConverter;

    @Autowired
    private CommunityConverter communityConverter;

    @Autowired
    private CollectionConverter collectionConverter;

    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    private BitstreamConverter bitstreamConverter;

    @Autowired
    private SiteService siteService;

    private Site site;
    private SiteRest siteRest;
    private Community communityA;
    private CommunityRest communityARest;
    private Collection collectionA;
    private CollectionRest collectionARest;
    private Item itemA;
    private ItemRest itemARest;
    private Bitstream bitstreamA;
    private BitstreamRest bitstreamARest;
    private Bundle bundleA;

    final String feature = "canViewUsageStatistics";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        site = siteService.findSite(context);
        communityA = CommunityBuilder.createCommunity(context)
            .withName("communityA")
            .build();
        collectionA = CollectionBuilder.createCollection(context, communityA)
            .withName("collectionA")
            .build();
        itemA = ItemBuilder.createItem(context, collectionA)
            .withTitle("itemA")
            .build();
        bundleA = BundleBuilder.createBundle(context, itemA)
            .withName("ORIGINAL")
            .build();
        String bitstreamContent = "Dummy content";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstreamA = BitstreamBuilder.createBitstream(context, bundleA, is)
                .withName("bistreamA")
                .build();
        }

        context.restoreAuthSystemState();

        siteRest = siteConverter.convert(site, Projection.DEFAULT);
        communityARest = communityConverter.convert(communityA, Projection.DEFAULT);
        collectionARest = collectionConverter.convert(collectionA, Projection.DEFAULT);
        itemARest = itemConverter.convert(itemA, Projection.DEFAULT);
        bitstreamARest = bitstreamConverter.convert(bitstreamA, Projection.DEFAULT);
        configurationService.setProperty("usage-statistics.authorization.admin.usage", true);
    }

    @Test
    public void adminBitstreamTestNotFound() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(bitstreamARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void adminItemAdminRequiredSuccess() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void adminCollectionAdminRequiredSuccess() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(collectionARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void adminCommunityAdminRequiredSuccess() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(communityARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
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
    public void ePersonItemAdminRequiredNotFound() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void ePersonCollectionAdminRequiredNotFound() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(collectionARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void ePersonCommunityAdminRequiredNotFound() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(communityARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
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
    public void ePersonItemAdminNotRequiredSuccess() throws Exception {
        configurationService.setProperty("usage-statistics.authorization.admin.usage", false);

        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void ePersonCollectionAdminNotRequiredSuccess() throws Exception {
        configurationService.setProperty("usage-statistics.authorization.admin.usage", false);

        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(collectionARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void ePersonCommunityAdminNotRequiredSuccess() throws Exception {
        configurationService.setProperty("usage-statistics.authorization.admin.usage", false);

        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(communityARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void ePersonSiteAdminNotRequiredSuccess() throws Exception {
        configurationService.setProperty("usage-statistics.authorization.admin.usage", false);

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
    public void ePersonPrivateItemAdminNotRequiredNotFound() throws Exception {
        configurationService.setProperty("usage-statistics.authorization.admin.usage", false);
        authorizeService.removeAllPolicies(context, itemA);

        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void anonymousItemAdminNotRequiredSuccess() throws Exception {
        configurationService.setProperty("usage-statistics.authorization.admin.usage", false);

        getClient().perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                               .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void anonymousCollectionAdminNotRequiredSuccess() throws Exception {
        configurationService.setProperty("usage-statistics.authorization.admin.usage", false);

        getClient().perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(collectionARest, "self").getHref()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                               .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void anonymousCommunityAdminNotRequiredSuccess() throws Exception {
        configurationService.setProperty("usage-statistics.authorization.admin.usage", false);

        getClient().perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(communityARest, "self").getHref()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                               .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void anonymousSiteAdminNotRequiredSuccess() throws Exception {
        configurationService.setProperty("usage-statistics.authorization.admin.usage", false);

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
