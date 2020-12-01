/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.rest.matcher.CrisMetricsMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.CrisMetricsBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test class for the CrisMetrics endpoint
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class CristMetricsRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    AuthorizeService authorizeService;

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
        getClient(tokenAdmin).perform(get("/api/authz/resourcepolicies"))
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

        CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, itemA)
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

        CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, itemA)
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

}