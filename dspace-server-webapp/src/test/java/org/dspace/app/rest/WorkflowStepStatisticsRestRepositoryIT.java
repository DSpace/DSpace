/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.apache.commons.lang3.time.DateUtils.addDays;
import static org.dspace.app.rest.matcher.WorkflowStepStatisticsMatcher.match;
import static org.dspace.app.rest.matcher.WorkflowStepStatisticsMatcher.matchActionCount;
import static org.dspace.app.rest.matcher.WorkflowStepStatisticsMatcher.matchActionCounts;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.repository.WorkflowStepStatisticsRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.statistics.MockSolrStatisticsCore;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class WorkflowStepStatisticsRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ClaimedTaskService claimedTaskService;

    @Autowired
    private PoolTaskService poolTaskService;

    @Autowired
    private XmlWorkflowItemService workflowItemService;

    @Autowired
    private MockSolrStatisticsCore mockSolrStatisticsCore;

    private Collection collection;

    private EPerson user;

    @Before
    public void setup() throws Exception {

        configurationService.setProperty("solr-statistics.autoCommit", false);

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        user = createEPerson("user1@email.com", "First", "User");

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications")
            .withEntityType("Publication")
            .withWorkflowGroup("reviewer", user)
            .withWorkflowGroup("editor", user)
            .build();

        EntityType publicationType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publicationType, publicationType,
            "isCorrectionOfItem", "isCorrectedByItem", 0, 1, 0, 1);

        context.setCurrentUser(eperson);

        mockSolrStatisticsCore.getSolr().commit();

        context.restoreAuthSystemState();

    }

    @After
    public void after() throws SQLException, IOException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        workflowItemService.deleteByCollection(context, collection);
        context.restoreAuthSystemState();
    }

    @Test
    public void testSearchCurrentWorkflows() throws Exception {

        context.turnOffAuthorisationSystem();

        XmlWorkflowItem firstWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem secondWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem thirdWorkflowItem = createWorkflowItem(collection);

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/current"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(1)))
            .andExpect(jsonPath("$._embedded.workflowSteps", contains(match("reviewstep", "reviewstep", 3))))
            .andExpect(jsonPath("$._embedded.workflowSteps[0]", matchActionCount("claimaction", 3)));

        ClaimedTask claimedTask = claimTask(firstWorkflowItem, user);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/current"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(1)))
            .andExpect(jsonPath("$._embedded.workflowSteps", contains(match("reviewstep", "reviewstep", 3))))
            .andExpect(jsonPath("$._embedded.workflowSteps[0]",
                matchActionCounts("claimaction", 2, "reviewaction", 1)));

        approveClaimedTaskViaRest(user, claimedTask);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/current"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(2)))
            .andExpect(jsonPath("$._embedded.workflowSteps", contains(
                match("reviewstep", "reviewstep", 2),
                match("editstep", "editstep", 1))))
            .andExpect(jsonPath("$._embedded.workflowSteps[0]", matchActionCount("claimaction", 2)))
            .andExpect(jsonPath("$._embedded.workflowSteps[1]", matchActionCount("claimaction", 1)));

        claimTaskAndReject(secondWorkflowItem, user);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/current"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(2)))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                match("reviewstep", "reviewstep", 1),
                match("editstep", "editstep", 1))))
            .andExpect(jsonPath("$._embedded.workflowSteps[0]", matchActionCount("claimaction", 1)))
            .andExpect(jsonPath("$._embedded.workflowSteps[1]", matchActionCount("claimaction", 1)));

        claimTaskAndApprove(thirdWorkflowItem, user);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/current"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(1)))
            .andExpect(jsonPath("$._embedded.workflowSteps", contains(match("editstep", "editstep", 2))))
            .andExpect(jsonPath("$._embedded.workflowSteps[0]", matchActionCount("claimaction", 2)));

        claimedTask = claimTask(firstWorkflowItem, user);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/current"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(1)))
            .andExpect(jsonPath("$._embedded.workflowSteps", contains(match("editstep", "editstep", 2))))
            .andExpect(jsonPath("$._embedded.workflowSteps[0]",
                matchActionCounts("claimaction", 1, "editaction", 1)));

        rejectClaimedTaskViaRest(user, claimedTask, "Bad item");

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/current"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(1)))
            .andExpect(jsonPath("$._embedded.workflowSteps", contains(match("editstep", "editstep", 1))))
            .andExpect(jsonPath("$._embedded.workflowSteps[0]", matchActionCount("claimaction", 1)));

        claimTaskAndApprove(thirdWorkflowItem, user);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/current"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps").doesNotExist());

    }

    @Test
    public void testSearchCurrentWorkflowsWithSizeParameter() throws Exception {

        context.turnOffAuthorisationSystem();

        XmlWorkflowItem firstWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem secondWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem thirdWorkflowItem = createWorkflowItem(collection);

        claimTaskAndApprove(firstWorkflowItem, user);

        claimTaskAndApprove(thirdWorkflowItem, user);
        claimTask(thirdWorkflowItem, user);

        claimTask(secondWorkflowItem, user);

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/current")
            .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(1)))
            .andExpect(jsonPath("$._embedded.workflowSteps", contains(match("editstep", "editstep", 2))))
            .andExpect(jsonPath("$._embedded.workflowSteps[0]",
                matchActionCounts("claimaction", 1, "editaction", 1)));

    }

    @Test
    public void testSearchByDateRangeWithoutParameters() throws Exception {

        context.turnOffAuthorisationSystem();

        XmlWorkflowItem firstWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem secondWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem thirdWorkflowItem = createWorkflowItem(collection);

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(1)))
            .andExpect(jsonPath("$._embedded.workflowSteps", contains(
                match("submit", "submit", 3))))
            .andExpect(jsonPath("$._embedded.workflowSteps[0]", matchActionCount("submit", 3)));

        ClaimedTask claimedTask = claimTask(firstWorkflowItem, user);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(2)))
            .andExpect(jsonPath("$._embedded.workflowSteps", contains(
                match("submit", "submit", 3),
                match("reviewstep", "reviewstep", 1))))
            .andExpect(jsonPath("$._embedded.workflowSteps[0]", matchActionCount("submit", 3)))
            .andExpect(jsonPath("$._embedded.workflowSteps[1]", matchActionCount("claimaction", 1)));

        approveClaimedTaskViaRest(user, claimedTask);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(2)))
            .andExpect(jsonPath("$._embedded.workflowSteps", contains(
                match("submit", "submit", 3),
                match("reviewstep", "reviewstep", 2))))
            .andExpect(jsonPath("$._embedded.workflowSteps[0]", matchActionCount("submit", 3)))
            .andExpect(jsonPath("$._embedded.workflowSteps[1]",
                matchActionCounts("claimaction", 1, "reviewaction", 1)));

        claimTaskAndApprove(firstWorkflowItem, user);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(4)))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                match("submit", "submit", 3),
                match("reviewstep", "reviewstep", 2),
                match("editstep", "editstep", 2),
                match("item", "item", 1))))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                matchActionCount("submit", 3),
                matchActionCounts("claimaction", 1, "reviewaction", 1),
                matchActionCounts("claimaction", 1, "editaction", 1),
                matchActionCount("approve", 1))));

        claimTaskAndReject(secondWorkflowItem, user);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(5)))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                match("reviewstep", "reviewstep", 4),
                match("submit", "submit", 3),
                match("editstep", "editstep", 2),
                match("item", "item", 1),
                match("workspace", "workspace", 1))))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                matchActionCounts("claimaction", 2, "reviewaction", 2),
                matchActionCount("submit", 3),
                matchActionCounts("claimaction", 1, "editaction", 1),
                matchActionCount("approve", 1),
                matchActionCount("reject", 1))));

        claimTaskAndApprove(thirdWorkflowItem, user);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(5)))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                match("reviewstep", "reviewstep", 6),
                match("submit", "submit", 3),
                match("editstep", "editstep", 2),
                match("item", "item", 1),
                match("workspace", "workspace", 1))))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                matchActionCounts("claimaction", 3, "reviewaction", 3),
                matchActionCount("submit", 3),
                matchActionCounts("claimaction", 1, "editaction", 1),
                matchActionCount("approve", 1),
                matchActionCount("reject", 1))));

        claimTaskAndApprove(thirdWorkflowItem, user);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(5)))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                match("reviewstep", "reviewstep", 6),
                match("submit", "submit", 3),
                match("editstep", "editstep", 4),
                match("item", "item", 2),
                match("workspace", "workspace", 1))))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                matchActionCounts("claimaction", 3, "reviewaction", 3),
                matchActionCount("submit", 3),
                matchActionCounts("claimaction", 2, "editaction", 2),
                matchActionCount("approve", 2),
                matchActionCount("reject", 1))));

        context.turnOffAuthorisationSystem();

        XmlWorkflowItem fourthWorkflowItem = createWorkflowItem(collection);

        context.restoreAuthSystemState();

        claimTaskAndApprove(fourthWorkflowItem, user);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(5)))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                match("reviewstep", "reviewstep", 8),
                match("submit", "submit", 4),
                match("editstep", "editstep", 4),
                match("item", "item", 2),
                match("workspace", "workspace", 1))))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                matchActionCounts("claimaction", 4, "reviewaction", 4),
                matchActionCount("submit", 4),
                matchActionCounts("claimaction", 2, "editaction", 2),
                matchActionCount("approve", 2),
                matchActionCount("reject", 1))));

        claimTaskAndReject(fourthWorkflowItem, user);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(5)))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                match("reviewstep", "reviewstep", 8),
                match("submit", "submit", 4),
                match("editstep", "editstep", 6),
                match("item", "item", 2),
                match("workspace", "workspace", 2))))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                matchActionCounts("claimaction", 4, "reviewaction", 4),
                matchActionCount("submit", 4),
                matchActionCounts("claimaction", 3, "editaction", 3),
                matchActionCount("approve", 2),
                matchActionCount("reject", 2))));
    }

    @Test
    public void testSearchByDateRangeExcludingClaimActions() throws Exception {

        configurationService.setProperty("statistics.workflow.actions-to-filter", "claimaction");

        context.turnOffAuthorisationSystem();

        XmlWorkflowItem firstWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem secondWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem thirdWorkflowItem = createWorkflowItem(collection);

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(1)))
            .andExpect(jsonPath("$._embedded.workflowSteps", contains(
                match("submit", "submit", 3))))
            .andExpect(jsonPath("$._embedded.workflowSteps[0]", matchActionCount("submit", 3)));

        ClaimedTask claimedTask = claimTask(firstWorkflowItem, user);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(1)))
            .andExpect(jsonPath("$._embedded.workflowSteps", contains(
                match("submit", "submit", 3))))
            .andExpect(jsonPath("$._embedded.workflowSteps[0]", matchActionCount("submit", 3)));

        approveClaimedTaskViaRest(user, claimedTask);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(2)))
            .andExpect(jsonPath("$._embedded.workflowSteps", contains(
                match("submit", "submit", 3),
                match("reviewstep", "reviewstep", 1))))
            .andExpect(jsonPath("$._embedded.workflowSteps[0]", matchActionCount("submit", 3)))
            .andExpect(jsonPath("$._embedded.workflowSteps[1]", matchActionCount("reviewaction", 1)));

        claimTaskAndApprove(firstWorkflowItem, user);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(4)))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                match("submit", "submit", 3),
                match("reviewstep", "reviewstep", 1),
                match("editstep", "editstep", 1),
                match("item", "item", 1))))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                matchActionCount("submit", 3),
                matchActionCount("reviewaction", 1),
                matchActionCount("editaction", 1),
                matchActionCount("approve", 1))));

        claimTaskAndReject(secondWorkflowItem, user);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(5)))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                match("reviewstep", "reviewstep", 2),
                match("submit", "submit", 3),
                match("editstep", "editstep", 1),
                match("item", "item", 1),
                match("workspace", "workspace", 1))))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                matchActionCount("reviewaction", 2),
                matchActionCount("submit", 3),
                matchActionCount("editaction", 1),
                matchActionCount("approve", 1),
                matchActionCount("reject", 1))));

        claimTaskAndApprove(thirdWorkflowItem, user);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(5)))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                match("reviewstep", "reviewstep", 3),
                match("submit", "submit", 3),
                match("editstep", "editstep", 1),
                match("item", "item", 1),
                match("workspace", "workspace", 1))))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                matchActionCount("reviewaction", 3),
                matchActionCount("submit", 3),
                matchActionCount("editaction", 1),
                matchActionCount("approve", 1),
                matchActionCount("reject", 1))));

        claimTaskAndApprove(thirdWorkflowItem, user);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(5)))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                match("reviewstep", "reviewstep", 3),
                match("submit", "submit", 3),
                match("editstep", "editstep", 2),
                match("item", "item", 2),
                match("workspace", "workspace", 1))))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                matchActionCount("reviewaction", 3),
                matchActionCount("submit", 3),
                matchActionCount("editaction", 2),
                matchActionCount("approve", 2),
                matchActionCount("reject", 1))));

        context.turnOffAuthorisationSystem();

        XmlWorkflowItem fourthWorkflowItem = createWorkflowItem(collection);

        context.restoreAuthSystemState();

        claimTaskAndApprove(fourthWorkflowItem, user);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(5)))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                match("reviewstep", "reviewstep", 4),
                match("submit", "submit", 4),
                match("editstep", "editstep", 2),
                match("item", "item", 2),
                match("workspace", "workspace", 1))))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                matchActionCount("reviewaction", 4),
                matchActionCount("submit", 4),
                matchActionCount("editaction", 2),
                matchActionCount("approve", 2),
                matchActionCount("reject", 1))));

        claimTaskAndReject(fourthWorkflowItem, user);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(5)))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                match("reviewstep", "reviewstep", 4),
                match("submit", "submit", 4),
                match("editstep", "editstep", 3),
                match("item", "item", 2),
                match("workspace", "workspace", 2))))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                matchActionCount("reviewaction", 4),
                matchActionCount("submit", 4),
                matchActionCount("editaction", 3),
                matchActionCount("approve", 2),
                matchActionCount("reject", 2))));
    }

    @Test
    public void testSearchByDateRangeWithSize() throws Exception {

        context.turnOffAuthorisationSystem();

        XmlWorkflowItem firstWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem secondWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem thirdWorkflowItem = createWorkflowItem(collection);
        createWorkflowItem(collection);

        claimTaskAndApprove(firstWorkflowItem, user);
        claimTaskAndApprove(firstWorkflowItem, user);
        claimTask(secondWorkflowItem, user);
        claimTaskAndApprove(thirdWorkflowItem, user);

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange")
            .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(2)))
            .andExpect(jsonPath("$._embedded.workflowSteps", contains(
                match("reviewstep", "reviewstep", 5),
                match("submit", "submit", 4))))
            .andExpect(jsonPath("$._embedded.workflowSteps[0]", matchActionCounts("claimaction", 3, "reviewaction", 2)))
            .andExpect(jsonPath("$._embedded.workflowSteps[1]", matchActionCount("submit", 4)));

    }

    @Test
    public void testSearchByDateRange() throws Exception {

        context.turnOffAuthorisationSystem();

        XmlWorkflowItem firstWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem secondWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem thirdWorkflowItem = createWorkflowItem(collection);
        createWorkflowItem(collection);

        claimTaskAndApprove(firstWorkflowItem, user);
        claimTask(secondWorkflowItem, user);
        claimTaskAndApprove(thirdWorkflowItem, user);

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange")
            .param("startDate", formatDate(addDays(new Date(), -1)))
            .param("endDate", formatDate(addDays(new Date(), 1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(2)))
            .andExpect(jsonPath("$._embedded.workflowSteps", contains(
                match("reviewstep", "reviewstep", 5),
                match("submit", "submit", 4))))
            .andExpect(jsonPath("$._embedded.workflowSteps[0]", matchActionCounts("claimaction", 3, "reviewaction", 2)))
            .andExpect(jsonPath("$._embedded.workflowSteps[1]", matchActionCount("submit", 4)));

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange")
            .param("endDate", formatDate(addDays(new Date(), 1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(2)))
            .andExpect(jsonPath("$._embedded.workflowSteps", contains(
                match("reviewstep", "reviewstep", 5),
                match("submit", "submit", 4))))
            .andExpect(jsonPath("$._embedded.workflowSteps[0]", matchActionCounts("claimaction", 3, "reviewaction", 2)))
            .andExpect(jsonPath("$._embedded.workflowSteps[1]", matchActionCount("submit", 4)));

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange")
            .param("startDate", formatDate(addDays(new Date(), -1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(2)))
            .andExpect(jsonPath("$._embedded.workflowSteps", contains(
                match("reviewstep", "reviewstep", 5),
                match("submit", "submit", 4))))
            .andExpect(jsonPath("$._embedded.workflowSteps[0]", matchActionCounts("claimaction", 3, "reviewaction", 2)))
            .andExpect(jsonPath("$._embedded.workflowSteps[1]", matchActionCount("submit", 4)));

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange")
            .param("startDate", formatDate(addDays(new Date(), 1)))
            .param("endDate", formatDate(addDays(new Date(), 2))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps").doesNotExist());

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange")
            .param("startDate", formatDate(addDays(new Date(), -2)))
            .param("endDate", formatDate(addDays(new Date(), -1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps").doesNotExist());

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange")
            .param("startDate", formatDate(addDays(new Date(), 1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps").doesNotExist());

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange")
            .param("endDate", formatDate(addDays(new Date(), -1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps").doesNotExist());

    }

    @Test
    public void testSearchByDateRangeWithCollectionScope() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection anotherCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications 2")
            .withEntityType("Publication")
            .withWorkflowGroup("reviewer", user)
            .withWorkflowGroup("editor", user)
            .build();

        XmlWorkflowItem firstWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem secondWorkflowItem = createWorkflowItem(anotherCollection);
        XmlWorkflowItem thirdWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem fourthWorkflowItem = createWorkflowItem(anotherCollection);

        context.restoreAuthSystemState();

        claimTaskAndApprove(firstWorkflowItem, user);
        claimTaskAndApprove(firstWorkflowItem, user);
        claimTaskAndApprove(secondWorkflowItem, user);
        claimTask(secondWorkflowItem, user);

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(4)))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                match("reviewstep", "reviewstep", 4),
                match("submit", "submit", 4),
                match("editstep", "editstep", 3),
                match("item", "item", 1))))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                matchActionCount("submit", 4),
                matchActionCounts("claimaction", 2, "reviewaction", 2),
                matchActionCounts("claimaction", 2, "editaction", 1),
                matchActionCount("approve", 1))));

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange")
            .param("collection", collection.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(4)))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                match("reviewstep", "reviewstep", 2),
                match("submit", "submit", 2),
                match("editstep", "editstep", 2),
                match("item", "item", 1))))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                matchActionCount("submit", 2),
                matchActionCounts("claimaction", 1, "reviewaction", 1),
                matchActionCounts("claimaction", 1, "editaction", 1),
                matchActionCount("approve", 1))));

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange")
            .param("collection", anotherCollection.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(3)))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                match("reviewstep", "reviewstep", 2),
                match("submit", "submit", 2),
                match("editstep", "editstep", 1))))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                matchActionCount("submit", 2),
                matchActionCounts("claimaction", 1, "reviewaction", 1),
                matchActionCount("claimaction", 1))));

        claimTaskAndReject(thirdWorkflowItem, user);
        claimTaskAndApprove(fourthWorkflowItem, user);
        claimTaskAndApprove(fourthWorkflowItem, user);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(5)))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                match("reviewstep", "reviewstep", 8),
                match("submit", "submit", 4),
                match("editstep", "editstep", 5),
                match("item", "item", 2),
                match("workspace", "workspace", 1))))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                matchActionCount("submit", 4),
                matchActionCounts("claimaction", 4, "reviewaction", 4),
                matchActionCounts("claimaction", 3, "editaction", 2),
                matchActionCount("approve", 2),
                matchActionCount("reject", 1))));

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange")
            .param("collection", collection.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(5)))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                match("reviewstep", "reviewstep", 4),
                match("submit", "submit", 2),
                match("editstep", "editstep", 2),
                match("item", "item", 1),
                match("workspace", "workspace", 1))))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                matchActionCount("submit", 2),
                matchActionCounts("claimaction", 2, "reviewaction", 2),
                matchActionCounts("claimaction", 1, "editaction", 1),
                matchActionCount("approve", 1),
                matchActionCount("reject", 1))));

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange")
            .param("collection", anotherCollection.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowSteps", hasSize(4)))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                match("reviewstep", "reviewstep", 4),
                match("submit", "submit", 2),
                match("editstep", "editstep", 3),
                match("item", "item", 1))))
            .andExpect(jsonPath("$._embedded.workflowSteps", containsInAnyOrder(
                matchActionCount("submit", 2),
                matchActionCounts("claimaction", 2, "reviewaction", 2),
                matchActionCounts("claimaction", 2, "editaction", 1),
                matchActionCount("approve", 1))));

    }

    @Test
    public void testSearchByDateRangeWithInvalidCollectionScope() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange")
            .param("collection", "invalid"))
            .andExpect(status().isBadRequest());

    }

    @Test
    public void testSearchByDateRangeWithInvalidDates() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange")
            .param("startDate", "invalid date")
            .param("endDate", formatDate(addDays(new Date(), 1))))
            .andExpect(status().isUnprocessableEntity());

        getClient(adminToken).perform(get("/api/statistics/wfSteps/search/byDateRange")
            .param("startDate", formatDate(addDays(new Date(), -1)))
            .param("endDate", "18/12/2021"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testSearchByDateRangeWithNotAdminUser() throws Exception {

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/statistics/wfSteps/search/byDateRange"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void testFindOne() throws Exception {

        context.turnOffAuthorisationSystem();

        XmlWorkflowItem firstWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem secondWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem thirdWorkflowItem = createWorkflowItem(collection);

        claimTaskAndApprove(firstWorkflowItem, user);
        claimTaskAndApprove(firstWorkflowItem, user);

        claimTaskAndApprove(secondWorkflowItem, user);
        claimTaskAndReject(secondWorkflowItem, user);

        claimTaskAndReject(thirdWorkflowItem, user);

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        String stepName = "reviewstep";

        getClient(adminToken).perform(get("/api/statistics/wfSteps/" + stepName))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", match(stepName, stepName, 6)))
            .andExpect(jsonPath("$", matchActionCounts("claimaction", 3, "reviewaction", 3)));

        configurationService.setProperty("statistics.workflow.actions-to-filter", "claimaction");

        getClient(adminToken).perform(get("/api/statistics/wfSteps/" + stepName))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", match(stepName, stepName, 3)))
            .andExpect(jsonPath("$", matchActionCount("reviewaction", 3)));

        stepName = "item";

        getClient(adminToken).perform(get("/api/statistics/wfSteps/" + stepName))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", match(stepName, stepName, 1)))
            .andExpect(jsonPath("$", matchActionCount("approve", 1)));

        stepName = "workspace";

        getClient(adminToken).perform(get("/api/statistics/wfSteps/" + stepName))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", match(stepName, stepName, 2)))
            .andExpect(jsonPath("$", matchActionCount("reject", 2)));

    }

    @Test
    public void testFindOneWithoutWorkflowStepStatistics() throws Exception {

        String stepName = "defaultWorkflow.reviewstep.claimaction";

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/" + stepName))
            .andExpect(status().isNotFound());

        stepName = "archived";

        getClient(adminToken).perform(get("/api/statistics/wfSteps/" + stepName))
            .andExpect(status().isNotFound());
    }

    @Test
    public void testFindOneWithUnknownStep() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/wfSteps/" + UUID.randomUUID().toString()))
            .andExpect(status().isNotFound());
    }

    @Test
    public void testFindOneWithNotAdminUser() throws Exception {

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/statistics/wfSteps/archived"))
            .andExpect(status().isForbidden());
    }

    private String formatDate(Date date) {
        return WorkflowStepStatisticsRestRepository.DATE_FORMATTER.format(date);
    }

    private void claimTaskAndApprove(XmlWorkflowItem workflowItem, EPerson user) throws Exception {
        ClaimedTask claimedTask = claimTask(workflowItem, user);
        approveClaimedTaskViaRest(user, claimedTask);
    }

    private void claimTaskAndReject(XmlWorkflowItem workflowItem, EPerson user) throws Exception {
        ClaimedTask claimedTask = claimTask(workflowItem, user);
        rejectClaimedTaskViaRest(user, claimedTask, "Bad Item");
    }

    private ClaimedTask claimTask(XmlWorkflowItem workflowItem, EPerson user)
        throws SQLException, Exception {
        List<PoolTask> poolTasks = poolTaskService.find(context, workflowItem);
        assertThat(poolTasks, hasSize(1));

        PoolTask poolTask = poolTasks.get(0);
        performActionOnPoolTaskViaRest(user, poolTask);

        ClaimedTask claimedTask = claimedTaskService.findByWorkflowIdAndEPerson(context, workflowItem, user);
        assertThat(claimedTask, notNullValue());

        return claimedTask;
    }

    private void rejectClaimedTaskViaRest(EPerson user, ClaimedTask task, String reason) throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("submit_reject", "submit_reject");
        params.add("reason", reason);
        performActionOnClaimedTaskViaRest(user, task, params);
    }

    private void approveClaimedTaskViaRest(EPerson user, ClaimedTask task) throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("submit_approve", "submit_approve");
        performActionOnClaimedTaskViaRest(user, task, params);
    }

    private void performActionOnClaimedTaskViaRest(EPerson user, ClaimedTask task, MultiValueMap<String, String> params)
        throws Exception {

        getClient(getAuthToken(user.getEmail(), password))
            .perform(post(BASE_REST_SERVER_URL + "/api/workflow/claimedtasks/{id}", task.getID()).params(params)
                .contentType("application/x-www-form-urlencoded"))
            .andExpect(status().isNoContent());

    }

    private void performActionOnPoolTaskViaRest(EPerson user, PoolTask task) throws Exception {
        getClient(getAuthToken(user.getEmail(), password)).perform(post("/api/workflow/claimedtasks")
            .contentType(RestMediaTypes.TEXT_URI_LIST)
            .content("/api/workflow/pooltasks/" + task.getID()))
            .andExpect(status().isCreated());
    }

    private XmlWorkflowItem createWorkflowItem(Collection collection) throws IOException {
        return WorkflowItemBuilder.createWorkflowItem(context, collection)
            .withTitle("My Publication")
            .withIssueDate("2017-10-17")
            .withAuthor("Mario Rossi")
            .grantLicense()
            .build();
    }

    private EPerson createEPerson(String email, String firstName, String lastName) throws SQLException {
        return EPersonBuilder.createEPerson(context)
            .withEmail(email)
            .withNameInMetadata(firstName, lastName)
            .withPassword(password)
            .build();
    }
}
