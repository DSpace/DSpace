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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.rest.matcher.CrisMetricsMatcher;
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
import org.dspace.discovery.IndexingService;
import org.dspace.metrics.scopus.UpdateScopusMetrics;
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
    private IndexingService crisIndexingService;

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

        CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, itemA)
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
        getClient(tokenEperson).perform(get("/api/cris/metrics/" + metric.getID()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$", is(
                                          CrisMetricsMatcher.matchCrisMetrics(metric)
                                          )))
                               .andExpect(jsonPath("$._links.self.href", Matchers
                               .containsString("/api/cris/metrics/" + metric.getID())));

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
        getClient(tokenEperson).perform(get("/api/cris/metrics/" + metric.getID()))
                               .andExpect(status().isForbidden());

    }

    @Test
    public void findOneUnauthorizedTest() throws Exception {
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

        getClient().perform(get("/api/cris/metrics/" + metric.getID()))
                   .andExpect(status().isUnauthorized());
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

        CrisMetrics metrics1 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                 .withMetricType("view")
                                                 .withMetricCount(2312)
                                                 .isLast(true).build();

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/cris/metrics/" + Integer.MAX_VALUE))
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
        getClient(tokenAdmin).perform(get("/api/cris/metrics/" + metric.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", is(
                                        CrisMetricsMatcher.matchCrisMetrics(metric)
                                        )))
                             .andExpect(jsonPath("$._links.self.href", Matchers
                             .containsString("/api/cris/metrics/" + metric.getID())));

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

        CrisMetrics metrics1 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                 .withMetricType("view")
                                                 .withMetricCount(2312)
                                                 .isLast(true).build();

        CrisMetrics metrics2 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                 .withMetricType(UpdateScopusMetrics.SCOPUS_CITATION)
                                                 .withMetricCount(43)
                                                 .isLast(true).build();

        CrisMetrics metrics3 = CrisMetricsBuilder.createCrisMetrics(context, itemB)
                                                 .withMetricType(UpdateScopusMetrics.SCOPUS_CITATION)
                                                 .withMetricCount(103)
                                                 .isLast(true).build();
        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/core/items/" + itemA.getID() + "/metrics"))
                               .andExpect(status().isOk())
                               .andExpect(content().contentType(contentType))
                               .andExpect(jsonPath("$._embedded.metrics").value(Matchers.hasSize(0)))
                               .andExpect(jsonPath("$._links.self.href",
                                   Matchers.containsString("api/core/items/" + itemA.getID() + "/metrics")))
                               .andExpect(jsonPath("$.page.totalElements", is(0)));
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

        CrisMetrics metrics1 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                 .withMetricType("view")
                                                 .withMetricCount(2312)
                                                 .isLast(true).build();

        CrisMetrics metrics2 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                 .withMetricType(UpdateScopusMetrics.SCOPUS_CITATION)
                                                 .withMetricCount(43)
                                                 .isLast(true).build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/items/" + itemA.getID() + "/metrics"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.metrics").value(Matchers.hasSize(0)))
                   .andExpect(jsonPath("$._links.self.href",
                              Matchers.containsString("api/core/items/" + itemA.getID() + "/metrics")))
                   .andExpect(jsonPath("$.page.totalElements", is(0)));
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

        CrisMetrics metrics1 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                 .withMetricType("view")
                                                 .withMetricCount(2312)
                                                 .isLast(true).build();

        CrisMetrics metrics2 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
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
                                           .withRelationshipType("Publication")
                                           .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withDoiIdentifier("10.1016/j.gene.2009.04.019")
                                .withTitle("Title item A").build();

        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.YEAR, 2019);
        calendar.set(Calendar.MONTH, 9);
        calendar.set(Calendar.DATE, 31);

        Date date = calendar.getTime();

        String remark = "{identifier:2-s2.0-67349162500, link:https://www.scopus.com/inward/citedby.uri?"
                      + "partnerIDu003dHzOxMe3bu0026scpu003d67349162500u0026originu003dinward"
                      + "pmid:19406218,doi:10.1016/j.gene.2009.04.019}";

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

        String[] args = new String[] {"update-metrics-in-solr"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);

        assertEquals(0, status);

        crisIndexingService.retriveSolrDocByUniqueID(itemA.getID().toString());

        String tokenEperson = getAuthToken(admin.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/core/items/" + itemA.getID() + "/metrics"))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$._embedded.metrics", Matchers.containsInAnyOrder(
                                          CrisMetricsMatcher.matchCrisMetrics(metric),
                                          CrisMetricsMatcher.matchCrisMetrics(metric2),
                                          CrisMetricsMatcher.matchCrisMetrics(metric3),
                                          CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "google-scholar"),
                                          CrisMetricsMatcher.matchCrisDynamicMetrics(itemA.getID(), "altmetrics")
                                          )))
                               .andExpect(jsonPath("$._links.self.href",
                                   Matchers.containsString("api/core/items/" + itemA.getID() + "/metrics")))
                               .andExpect(jsonPath("$.page.totalElements", is(5)));
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

        CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, itemA)
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

}