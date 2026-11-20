/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.rest.matcher.CrisMetricsMatcher;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.CrisMetricsBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.metrics.scopus.UpdateScopusMetrics;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test class for the CrisMetrics endpoint
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class CrisMetricsRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private ConfigurationService configurationService;
    @Test
    public void findAllTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                .withDoiIdentifier("10.1016/19")
                .withTitle("Title item A").build();

        CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withMetricType("view")
                .withMetricCount(2312)
                .isLast(true).build();
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/cris/metrics"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                .withDoiIdentifier("10.1016/19")
                .withTitle("Title item A").build();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2020);
        calendar.set(Calendar.MONTH, 3);
        calendar.set(Calendar.DATE, 21);

        CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withAcquisitionDate(calendar.getTime())
                .withMetricType("view")
                .withMetricCount(2312)
                .isLast(true).build();

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(
                get("/api/cris/metrics/" + CrisMetricsBuilder.getRestStoredMetricId(metric.getID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(
                        CrisMetricsMatcher.matchCrisMetrics(metric)
                )))
                .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/cris/metrics/"
                        + CrisMetricsBuilder.getRestStoredMetricId(metric.getID()))));

    }

    @Test
    public void findOneForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                .withDoiIdentifier("10.1016/19")
                .withTitle("Title item A").build();

        CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withMetricType("view")
                .withMetricCount(2312)
                .isLast(true).build();

        authorizeService.removePoliciesActionFilter(context, itemA, Constants.READ);
        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(
                get("/api/cris/metrics/" + CrisMetricsBuilder.getRestStoredMetricId(metric.getID())))
                .andExpect(status().isForbidden());

    }

    @Test
    public void findOneAnonymousTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                .withDoiIdentifier("10.1016/19")
                .withTitle("Title item A").build();

        CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withMetricType("view")
                .withMetricCount(2312)
                .isLast(true).build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/cris/metrics/" + CrisMetricsBuilder.getRestStoredMetricId(metric.getID())))
                .andExpect(status().isOk());
    }

    @Test
    public void findOneisNotFoundTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                .withDoiIdentifier("10.1016/19")
                .withTitle("Title item A").build();

        CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withMetricType("view")
                .withMetricCount(2312)
                .isLast(true).build();

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/cris/metrics/" + Integer.MAX_VALUE))
                .andExpect(status().isNotFound());

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/cris/metrics/" + Integer.MAX_VALUE))
                .andExpect(status().isNotFound());
        getClient(tokenAdmin)
                .perform(get("/api/cris/metrics/" + CrisMetricsBuilder.getRestStoredMetricId(Integer.MAX_VALUE)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void findOneByAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                .withDoiIdentifier("10.1016/19")
                .withTitle("Title item A").build();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2020);
        calendar.set(Calendar.MONTH, 9);
        calendar.set(Calendar.DATE, 17);

        CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withAcquisitionDate(calendar.getTime())
                .withMetricType("view")
                .withMetricCount(2312)
                .isLast(true).build();

        authorizeService.removePoliciesActionFilter(context, itemA, Constants.READ);
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String restMetricId = CrisMetricsBuilder.getRestStoredMetricId(metric.getID());
        getClient(tokenAdmin).perform(
                get("/api/cris/metrics/" + restMetricId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(
                        CrisMetricsMatcher.matchCrisMetrics(metric)
                )))
                .andExpect(jsonPath("$._links.self.href", Matchers
                        .containsString("/api/cris/metrics/" + restMetricId)));

    }

    @Test
    public void findLinkedEntitiesMetricsWithoutNotExistedInSolrDocumentTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                .withDoiIdentifier("10.1016/19")
                .withTitle("Title item A").build();

        Item itemB = ItemBuilder.createItem(context, col1)
                .withDoiIdentifier("30.1100/31")
                .withTitle("Title item B").build();

        CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withMetricType("view")
                .withMetricCount(2312)
                .isLast(true).build();

        CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withMetricType(UpdateScopusMetrics.SCOPUS_CITATION)
                .withMetricCount(43)
                .isLast(true).build();

        CrisMetricsBuilder.createCrisMetrics(context, itemB)
                .withMetricType(UpdateScopusMetrics.SCOPUS_CITATION)
                .withMetricCount(103)
                .isLast(true).build();
        context.restoreAuthSystemState();

        String embeddedView = "<a title=\"\" href=\"\">View</a>";
        String embeddedDownload = "<a title=\"\" href=\"\">Downloads</a>";

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/core/items/" + itemA.getID() + "/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.metrics").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$._embedded.metrics").value(Matchers.containsInAnyOrder(
                        CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "embedded-view", embeddedView),
                        CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "embedded-download", embeddedDownload)
                        )))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString("api/core/items/" + itemA.getID() + "/metrics")))
                .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    public void findLinkedEntitiesMetricsWithoutNotExistedInSolrDocumentAndUsageAdminNotConfigured() throws Exception {
        configurationService.setProperty("usage-statistics.authorization.admin.usage", null);

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withDoiIdentifier("10.1016/19")
                                .withTitle("Title item A")
                                .build();

        Item itemB = ItemBuilder.createItem(context, col1)
                                .withDoiIdentifier("30.1100/31")
                                .withTitle("Title item B")
                                .build();

        CrisMetricsBuilder.createCrisMetrics(context, itemA)
                          .withMetricType("view")
                          .withMetricCount(2312)
                          .isLast(true)
                          .build();

        CrisMetricsBuilder.createCrisMetrics(context, itemA)
                          .withMetricType(UpdateScopusMetrics.SCOPUS_CITATION)
                          .withMetricCount(43)
                          .isLast(true)
                          .build();

        CrisMetricsBuilder.createCrisMetrics(context, itemB)
                          .withMetricType(UpdateScopusMetrics.SCOPUS_CITATION)
                          .withMetricCount(103)
                          .isLast(true)
                          .build();

        context.restoreAuthSystemState();

        String embeddedView = "http://localhost:4000/statistics/items/" + itemA.getID().toString();
        String embeddedDownload = "http://localhost:4000/statistics/items/" + itemA.getID().toString();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/core/items/" + itemA.getID() + "/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.metrics").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$._embedded.metrics").value(Matchers.containsInAnyOrder(
                    CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "embedded-view", embeddedView),
                    CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "embedded-download", embeddedDownload)
                    )))
                .andExpect(jsonPath("$._links.self.href",Matchers.containsString("api/core/items/" + itemA.getID() +
                                    "/metrics")))
                .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    public void findLinkedEntitiesMetricsWithUserNotLoggedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                .withDoiIdentifier("10.1016/19")
                .withTitle("Title item A").build();

        CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withMetricType("view")
                .withMetricCount(2312)
                .isLast(true).build();

        CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withMetricType(UpdateScopusMetrics.SCOPUS_CITATION)
                .withMetricCount(43)
                .isLast(true).build();

        context.restoreAuthSystemState();

        String embeddedView = "<a title=\"\" href=\"\">View</a>";
        String embeddedDownload = "<a title=\"\" href=\"\">Downloads</a>";

        getClient().perform(get("/api/core/items/" + itemA.getID() + "/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.metrics").value(Matchers.hasSize(2)))
            .andExpect(jsonPath("$._embedded.metrics").value(Matchers.containsInAnyOrder(
                CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "embedded-view", embeddedView),
                CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "embedded-download", embeddedDownload))))
            .andExpect(jsonPath("$._links.self.href",
                Matchers.containsString("api/core/items/" + itemA.getID() + "/metrics")))
            .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    public void findLinkedEntitiesMetricsWithUserNotLoggedAndUsageAdminNotConfiguredTest() throws Exception {
        configurationService.setProperty("usage-statistics.authorization.admin.usage", null);

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                .withDoiIdentifier("10.1016/19")
                .withTitle("Title item A").build();

        CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withMetricType("view")
                .withMetricCount(2312)
                .isLast(true).build();

        CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withMetricType(UpdateScopusMetrics.SCOPUS_CITATION)
                .withMetricCount(43)
                .isLast(true).build();

        String embeddedView = "http://localhost:4000/statistics/items/" + itemA.getID().toString();
        String embeddedDownload = "http://localhost:4000/statistics/items/" + itemA.getID().toString();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/items/" + itemA.getID() + "/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.metrics").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$._embedded.metrics").value(Matchers.containsInAnyOrder(
                    CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "embedded-view", embeddedView),
                    CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "embedded-download", embeddedDownload))))
                .andExpect(jsonPath("$._links.self.href",
                    Matchers.containsString("api/core/items/" + itemA.getID() + "/metrics")))
                .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    public void findLinkedEntitiesMetricNotFoundTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                .withDoiIdentifier("10.1016/19")
                .withTitle("Title item A").build();

        CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withMetricType("view")
                .withMetricCount(2312)
                .isLast(true).build();

        CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withMetricType(UpdateScopusMetrics.SCOPUS_CITATION)
                .withMetricCount(43)
                .isLast(true).build();

        context.restoreAuthSystemState();
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/items/" + UUID.randomUUID().toString() + "/metrics"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void findLinkedEntitiesMetricsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withEntityType("Publication")
                .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                .withDoiIdentifier("10.1016/j.gene.2009.04.019")
                .withTitle("Title item A").build();

        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.YEAR, 2019);
        calendar.set(Calendar.MONTH, 9);
        calendar.set(Calendar.DATE, 31);

        Date date = calendar.getTime();

        String remark = "{\"identifier\":\"2-s2.0-67349162500\", \"detailUrl\":\"https://www.scopus.com/inward/citedby.uri?"
                + "partnerIDu003dHzOxMe3bu0026scpu003d67349162500u0026originu003dinward"
                + "\",\"pmid\":\"19406218\",\"doi\":\"10.1016/j.gene.2009.04.019\"}";

        CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withAcquisitionDate(date)
                .withMetricType("ScopusCitation")
                .withMetricCount(4)
                .withRemark(remark)
                .withDeltaPeriod1(3.0)
                .withDeltaPeriod2(12.0)
                .withRank(50.0)
                .isLast(true).build();

        CrisMetrics metric2 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withAcquisitionDate(date)
                .withMetricType("view")
                .withMetricCount(4501)
                .isLast(true).build();

        CrisMetrics metric3 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withAcquisitionDate(date)
                .withMetricType("wosCitation")
                // without a metric count "null"
                .isLast(true).build();

        context.restoreAuthSystemState();

        String[] args = new String[]{"update-metrics-in-solr"};

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);

        assertEquals(0, status);
        String remarkGoogleScholar = "scholar.google.com/scholar?q=Title+item+A";
        String remarkAltmetric = "10.1016/j.gene.2009.04.019";
        String remarkPlumX = "10.1016/j.gene.2009.04.019";
        String remarkEmbeddedDownload = "http://localhost:4000/statistics/items/" + itemA.getID().toString();
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String remarkDimensions = "10.1016/j.gene.2009.04.019";
        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID() + "/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.metrics", Matchers.containsInAnyOrder(
                        CrisMetricsMatcher.matchCrisMetrics(metric),
                        CrisMetricsMatcher.matchCrisMetrics(metric2),
                        CrisMetricsMatcher.matchCrisMetrics(metric3),
                        CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "google-scholar",remarkGoogleScholar),
                        CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "altmetric", remarkAltmetric),
                        CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "embedded-download",
                                                                                   remarkEmbeddedDownload),
                        CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "plumX", remarkPlumX),
                        CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "dimensions",
                            remarkDimensions)
                )))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString("api/core/items/" + itemA.getID() + "/metrics")))
                .andExpect(jsonPath("$.page.totalElements", is(8)));
    }

    @Test
    public void tryToDeletItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                .withDoiIdentifier("10.1016/19")
                .withTitle("Title item A").build();

        CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withMetricType(UpdateScopusMetrics.SCOPUS_CITATION)
                .withMetricCount(21)
                .withDeltaPeriod1(3.0)
                .withDeltaPeriod2(12.0)
                .withRank(10.0)
                .isLast(true).build();

        context.restoreAuthSystemState();
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(delete("/api/core/items/" + itemA.getID())).andExpect(status().isNoContent());
        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID())).andExpect(status().isNotFound());
    }

    @Test
    public void findLinkedEntitiesMetricsAndEmbeddedViewAndDownloadsMetricsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withEntityType("Publication")
                .withName("Collection 1").build();
        String itemDoi = "10.1016/j.gene.2009.04.019";
        Item itemA = ItemBuilder.createItem(context, col1)
                .withDoiIdentifier(itemDoi)
                .withTitle("Title item A").build();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2019);
        calendar.set(Calendar.MONTH, 9);
        calendar.set(Calendar.DATE, 31);
        Date date = calendar.getTime();
        String remark = "{\"identifier\":\"2-s2.0-67349162500\", \"detailUrl\":\"https://www.scopus.com/inward/citedby.uri?"
                + "partnerIDu003dHzOxMe3bu0026scpu003d67349162500u0026originu003dinward"
                + "\",\"pmid\":\"19406218\",\"doi\":\"10.1016/j.gene.2009.04.019\"}";
        CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withAcquisitionDate(date)
                .withMetricType("ScopusCitation")
                .withMetricCount(4)
                .withRemark(remark)
                .withDeltaPeriod1(3.0)
                .withDeltaPeriod2(12.0)
                .withRank(50.0)
                .isLast(true).build();
        CrisMetrics metric3 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withAcquisitionDate(date)
                .withMetricType("wosCitation")
                // without a metric count "null"
                .isLast(true).build();
        context.restoreAuthSystemState();
        //save all metrics on solr search core
        String[] args = new String[]{"update-metrics-in-solr"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertEquals(0, status);

        String googleScholarRemark = "https://scholar.google.com/scholar?q=Title+item+A";
        String embeddedViewRemark = "http://localhost:4000/statistics/items/" + itemA.getID().toString();
        String embeddedDownloadRemark = "http://localhost:4000/statistics/items/" + itemA.getID().toString();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID() + "/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.metrics", Matchers.containsInAnyOrder(
                        CrisMetricsMatcher.matchCrisMetrics(metric),
                        CrisMetricsMatcher.matchCrisMetrics(metric3),
                        CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "google-scholar",
                            googleScholarRemark),
                        CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "embedded-view",
                            embeddedViewRemark),
                        CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "embedded-download",
                            embeddedDownloadRemark),
                        CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "altmetric",
                            itemDoi),
                        CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "plumX",
                            itemDoi),
                        CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "dimensions",
                            itemDoi)
                )))
                .andExpect(jsonPath("$._links.self.href",
                                    Matchers.containsString("api/core/items/" + itemA.getID() + "/metrics")))
                .andExpect(jsonPath("$.page.totalElements", is(8)));
    }
    @Test
    public void findLinkedEntitiesMetricsWithViewAndDownloadsMetricsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withEntityType("Publication")
                .withName("Collection 1").build();
        String itemDoi = "10.1016/j.gene.2009.04.019";
        Item itemA = ItemBuilder.createItem(context, col1)
                .withDoiIdentifier(itemDoi)
                .withTitle("Title item A").build();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2019);
        calendar.set(Calendar.MONTH, 9);
        calendar.set(Calendar.DATE, 31);
        Date date = calendar.getTime();
        String remark = "{\"identifier\":\"2-s2.0-67349162500\", \"detailUrl\":\"https://www.scopus.com/inward/citedby.uri?"
                + "partnerIDu003dHzOxMe3bu0026scpu003d67349162500u0026originu003dinward"
                + "\",\"pmid\":\"19406218\",\"doi\":\"10.1016/j.gene.2009.04.019\"}";
        CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withAcquisitionDate(date)
                .withMetricType("ScopusCitation")
                .withMetricCount(4)
                .withRemark(remark)
                .withDeltaPeriod1(3.0)
                .withDeltaPeriod2(12.0)
                .withRank(50.0)
                .isLast(true).build();
        CrisMetrics metric3 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withAcquisitionDate(date)
                .withMetricType("wosCitation")
                // without a metric count "null"
                .isLast(true).build();
        //remark for view
        String remark_view = configurationService.getProperty("dspace.ui.url") + "/statistics/items/" + itemA.getID();
        //view metric
        CrisMetrics metric_view = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withAcquisitionDate(new Date())
                .withMetricType("view")
                .withMetricCount(10)
                .withRemark(remark_view)
                .withDeltaPeriod1(2.0)
                .withDeltaPeriod2(3.0)
                .withRank(20.0)
                .isLast(true).build();
        //download metric
        CrisMetrics metric_download = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                .withAcquisitionDate(new Date())
                .withMetricType("download")
                .withMetricCount(12)
                .withRemark(remark_view)
                .withDeltaPeriod1(3.0)
                .withDeltaPeriod2(4.0)
                .withRank(30.0)
                .isLast(true).build();
        context.restoreAuthSystemState();
        //save all metrics on solr search core
        String[] args = new String[]{"update-metrics-in-solr"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertEquals(0, status);

        String googleScholar = "https://scholar.google.com/scholar?q=Title+item+A";

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID() + "/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.metrics", Matchers.containsInAnyOrder(
                        CrisMetricsMatcher.matchCrisMetrics(metric),
                        CrisMetricsMatcher.matchCrisMetrics(metric_view),
                        CrisMetricsMatcher.matchCrisMetrics(metric3),
                        CrisMetricsMatcher.matchCrisMetrics(metric_download),
                        CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "google-scholar", googleScholar),
                        CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "altmetric", itemDoi),
                        CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "plumX", itemDoi),
                        CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "dimensions", itemDoi)
                )))
                .andExpect(jsonPath("$._links.self.href",
                                    Matchers.containsString("api/core/items/" + itemA.getID() + "/metrics")))
                .andExpect(jsonPath("$.page.totalElements", is(8)));
    }

    @Test
    public void checkMetricsAfterReindexingOfAnItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Title item A")
                                .build();

        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.YEAR, 2019);
        calendar.set(Calendar.MONTH, 9);
        calendar.set(Calendar.DATE, 31);

        Date date = calendar.getTime();

        CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                               .withAcquisitionDate(date)
                                               .withMetricType(UpdateScopusMetrics.SCOPUS_CITATION)
                                               .withMetricCount(4)
                                               .isLast(true).build();

        CrisMetrics metric2 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                .withAcquisitionDate(date)
                                                .withMetricType("view")
                                                .withMetricCount(4501)
                                                .isLast(true).build();

        context.restoreAuthSystemState();
        context.commit();

        String[] args = new String[]{"update-metrics-in-solr"};

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

        String googleScholar = "https://scholar.google.com/scholar?q=Title+item+A";
        String embeddedDownload = "<a title=\"\" href=\"\">Downloads</a>";

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenEperson = getAuthToken(eperson.getEmail(), password);

        getClient(tokenEperson).perform(get("/api/core/items/" + itemA.getID() + "/metrics"))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$._embedded.metrics", Matchers.containsInAnyOrder(
                                          CrisMetricsMatcher.matchCrisMetrics(metric),
                                          CrisMetricsMatcher.matchCrisMetrics(metric2),
                                          CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "google-scholar",
                                                                                                      googleScholar),
                                          CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "embedded-download",
                                                                                                       embeddedDownload)
                                          )))
                               .andExpect(jsonPath("$.page.totalElements", is(4)));

        List<Operation> ops = new ArrayList<>();
        List<Map<String, String>> values = new ArrayList<>();
        Map<String, String> value = new HashMap<>();
        value.put("value", "New Title");
        values.add(value);
        ops.add(new ReplaceOperation("/metadata/dc.title", values));

        getClient(tokenAdmin).perform(patch("/api/core/items/" + itemA.getID())
                             .content(getPatchContent(ops))
                             .contentType(contentType))
                             .andExpect(status().isOk());

        googleScholar = "https://scholar.google.com/scholar?q=New+Title";

        getClient(tokenEperson).perform(get("/api/core/items/" + itemA.getID() + "/metrics"))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$._embedded.metrics", Matchers.containsInAnyOrder(
                                          CrisMetricsMatcher.matchCrisMetrics(metric),
                                          CrisMetricsMatcher.matchCrisMetrics(metric2),
                                          CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "google-scholar",
                                                                                                      googleScholar),
                                          CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "embedded-download",
                                                                                                       embeddedDownload)
                                          )))
                               .andExpect(jsonPath("$.page.totalElements", is(4)));
    }

}
