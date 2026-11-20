/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.apache.commons.lang3.time.DateUtils.addDays;
import static org.dspace.app.rest.matcher.WorkflowOwnerStatisticsMatcher.match;
import static org.dspace.app.rest.matcher.WorkflowOwnerStatisticsMatcher.matchActionCount;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
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

import org.dspace.app.rest.repository.WorkflowOwnerStatisticsRestRepository;
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

public class WorkflowOwnerStatisticsRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ClaimedTaskService claimedTaskService;

    @Autowired
    private PoolTaskService poolTaskService;

    @Autowired
    private XmlWorkflowItemService workflowItemService;

    private Collection collection;

    private EPerson firstUser;

    private EPerson secondUser;

    private EPerson thirdUser;

    @Before
    public void setup() throws Exception {

        configurationService.setProperty("solr-statistics.autoCommit", false);

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        firstUser = createEPerson("user1@email.com", "First", "User");
        secondUser = createEPerson("user2@email.com", "Second", "User");
        thirdUser = createEPerson("user3@email.com", "Third", "User");

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications")
            .withEntityType("Publication")
            .withWorkflowGroup("reviewer", firstUser, secondUser)
            .withWorkflowGroup("editor", firstUser, thirdUser)
            .build();

        EntityType publicationType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publicationType, publicationType,
            "isCorrectionOfItem", "isCorrectedByItem", 0, 1, 0, 1);

        context.setCurrentUser(eperson);

        context.restoreAuthSystemState();

    }

    @After
    public void after() throws SQLException, IOException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        workflowItemService.deleteByCollection(context, collection);
        context.restoreAuthSystemState();
    }

    @Test
    public void testSearchByDateRangeWithoutParameters() throws Exception {

        context.turnOffAuthorisationSystem();

        XmlWorkflowItem firstWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem secondWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem thirdWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem fourthWorkflowItem = createWorkflowItem(collection);

        claimTaskAndApprove(firstWorkflowItem, firstUser);
        claimTaskAndApprove(firstWorkflowItem, firstUser);

        claimTaskAndApprove(secondWorkflowItem, secondUser);
        claimTaskAndReject(secondWorkflowItem, thirdUser, "bad item");

        claimTask(thirdWorkflowItem, secondUser);

        claimTaskAndApprove(fourthWorkflowItem, firstUser);
        claimTaskAndApprove(fourthWorkflowItem, thirdUser);

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/workflowOwners/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowOwners", hasSize(3)))
            .andExpect(jsonPath("$._embedded.workflowOwners", contains(
                match(firstUser, 6), match(thirdUser, 4), match(secondUser, 3))))
            .andExpect(jsonPath("$._embedded.workflowOwners[0]", allOf(
                matchActionCount("reviewstep.claimaction", 2),
                matchActionCount("reviewstep.reviewaction", 2),
                matchActionCount("editstep.claimaction", 1),
                matchActionCount("editstep.editaction", 1))))
            .andExpect(jsonPath("$._embedded.workflowOwners[1]", allOf(
                matchActionCount("editstep.claimaction", 2),
                matchActionCount("editstep.editaction", 2))))
            .andExpect(jsonPath("$._embedded.workflowOwners[2]", allOf(
                matchActionCount("reviewstep.claimaction", 2),
                matchActionCount("reviewstep.reviewaction", 1))));

    }

    @Test
    public void testSearchByDateRangeExcludingClaimActions() throws Exception {

        configurationService.setProperty("statistics.workflow.actions-to-filter", "claimaction");

        context.turnOffAuthorisationSystem();

        XmlWorkflowItem firstWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem secondWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem thirdWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem fourthWorkflowItem = createWorkflowItem(collection);

        claimTaskAndApprove(firstWorkflowItem, firstUser);
        claimTaskAndApprove(firstWorkflowItem, firstUser);

        claimTaskAndApprove(secondWorkflowItem, secondUser);
        claimTaskAndReject(secondWorkflowItem, thirdUser, "bad item");

        claimTask(thirdWorkflowItem, secondUser);

        claimTaskAndApprove(fourthWorkflowItem, firstUser);
        claimTaskAndApprove(fourthWorkflowItem, thirdUser);

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/workflowOwners/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowOwners", hasSize(3)))
            .andExpect(jsonPath("$._embedded.workflowOwners", contains(
                match(firstUser, 3), match(thirdUser, 2), match(secondUser, 1))))
            .andExpect(jsonPath("$._embedded.workflowOwners[0]", allOf(
                matchActionCount("reviewstep.reviewaction", 2),
                matchActionCount("editstep.editaction", 1))))
            .andExpect(jsonPath("$._embedded.workflowOwners[1]", allOf(
                matchActionCount("editstep.editaction", 2))))
            .andExpect(jsonPath("$._embedded.workflowOwners[2]", allOf(
                matchActionCount("reviewstep.reviewaction", 1))));

    }

    @Test
    public void testSearchByDateRangeWithLimit() throws Exception {

        context.turnOffAuthorisationSystem();

        XmlWorkflowItem firstWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem secondWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem thirdWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem fourthWorkflowItem = createWorkflowItem(collection);

        claimTaskAndApprove(firstWorkflowItem, firstUser);
        claimTaskAndApprove(firstWorkflowItem, firstUser);

        claimTaskAndApprove(secondWorkflowItem, secondUser);
        claimTaskAndReject(secondWorkflowItem, thirdUser, "bad item");

        claimTask(thirdWorkflowItem, secondUser);

        claimTaskAndApprove(fourthWorkflowItem, firstUser);
        claimTaskAndApprove(fourthWorkflowItem, thirdUser);

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/workflowOwners/search/byDateRange")
            .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowOwners", hasSize(2)))
            .andExpect(jsonPath("$._embedded.workflowOwners", contains(match(firstUser, 6), match(thirdUser, 4))))
            .andExpect(jsonPath("$._embedded.workflowOwners[0]", allOf(
                matchActionCount("reviewstep.claimaction", 2),
                matchActionCount("reviewstep.reviewaction", 2),
                matchActionCount("editstep.claimaction", 1),
                matchActionCount("editstep.editaction", 1))))
            .andExpect(jsonPath("$._embedded.workflowOwners[1]", allOf(
                matchActionCount("editstep.claimaction", 2),
                matchActionCount("editstep.editaction", 2))));

    }

    @Test
    public void testSearchByDateRange() throws Exception {

        context.turnOffAuthorisationSystem();

        XmlWorkflowItem firstWorkflowItem = createWorkflowItem(collection);

        claimTaskAndApprove(firstWorkflowItem, firstUser);
        claimTaskAndApprove(firstWorkflowItem, firstUser);

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/workflowOwners/search/byDateRange")
            .param("startDate", formatDate(addDays(new Date(), -1)))
            .param("endDate", formatDate(addDays(new Date(), 1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowOwners", contains(match(firstUser, 4))))
            .andExpect(jsonPath("$._embedded.workflowOwners[0]", allOf(
                matchActionCount("reviewstep.claimaction", 1),
                matchActionCount("reviewstep.reviewaction", 1),
                matchActionCount("editstep.claimaction", 1),
                matchActionCount("editstep.editaction", 1))));

        getClient(adminToken).perform(get("/api/statistics/workflowOwners/search/byDateRange")
            .param("endDate", formatDate(addDays(new Date(), 1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowOwners", contains(match(firstUser, 4))))
            .andExpect(jsonPath("$._embedded.workflowOwners[0]", allOf(
                matchActionCount("reviewstep.claimaction", 1),
                matchActionCount("reviewstep.reviewaction", 1),
                matchActionCount("editstep.claimaction", 1),
                matchActionCount("editstep.editaction", 1))));

        getClient(adminToken).perform(get("/api/statistics/workflowOwners/search/byDateRange")
            .param("startDate", formatDate(addDays(new Date(), -1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowOwners", contains(match(firstUser, 4))))
            .andExpect(jsonPath("$._embedded.workflowOwners[0]", allOf(
                matchActionCount("reviewstep.claimaction", 1),
                matchActionCount("reviewstep.reviewaction", 1),
                matchActionCount("editstep.claimaction", 1),
                matchActionCount("editstep.editaction", 1))));

        getClient(adminToken).perform(get("/api/statistics/workflowOwners/search/byDateRange")
            .param("startDate", formatDate(addDays(new Date(), 1)))
            .param("endDate", formatDate(addDays(new Date(), 2))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowOwners").doesNotExist());

        getClient(adminToken).perform(get("/api/statistics/workflowOwners/search/byDateRange")
            .param("startDate", formatDate(addDays(new Date(), -2)))
            .param("endDate", formatDate(addDays(new Date(), -1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowOwners").doesNotExist());

        getClient(adminToken).perform(get("/api/statistics/workflowOwners/search/byDateRange")
            .param("startDate", formatDate(addDays(new Date(), 1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowOwners").doesNotExist());

        getClient(adminToken).perform(get("/api/statistics/workflowOwners/search/byDateRange")
            .param("endDate", formatDate(addDays(new Date(), -1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowOwners").doesNotExist());

    }

    @Test
    public void testSearchByDateRangeWithCollectionScope() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection anotherCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications 2")
            .withEntityType("Publication")
            .withWorkflowGroup("reviewer", firstUser, secondUser)
            .withWorkflowGroup("editor", firstUser, thirdUser)
            .build();

        XmlWorkflowItem firstWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem secondWorkflowItem = createWorkflowItem(anotherCollection);
        XmlWorkflowItem thirdWorkflowItem = createWorkflowItem(anotherCollection);
        XmlWorkflowItem fourthWorkflowItem = createWorkflowItem(collection);

        claimTaskAndApprove(firstWorkflowItem, firstUser);
        claimTaskAndApprove(firstWorkflowItem, firstUser);

        claimTaskAndApprove(secondWorkflowItem, secondUser);
        claimTaskAndReject(secondWorkflowItem, thirdUser, "bad item");

        claimTask(thirdWorkflowItem, secondUser);

        claimTaskAndApprove(fourthWorkflowItem, firstUser);
        claimTaskAndApprove(fourthWorkflowItem, thirdUser);

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/workflowOwners/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowOwners", hasSize(3)))
            .andExpect(jsonPath("$._embedded.workflowOwners", contains(
                match(firstUser, 6), match(thirdUser, 4), match(secondUser, 3))))
            .andExpect(jsonPath("$._embedded.workflowOwners[0]", allOf(
                matchActionCount("reviewstep.claimaction", 2),
                matchActionCount("reviewstep.reviewaction", 2),
                matchActionCount("editstep.claimaction", 1),
                matchActionCount("editstep.editaction", 1))))
            .andExpect(jsonPath("$._embedded.workflowOwners[1]", allOf(
                matchActionCount("editstep.claimaction", 2),
                matchActionCount("editstep.editaction", 2))))
            .andExpect(jsonPath("$._embedded.workflowOwners[2]", allOf(
                matchActionCount("reviewstep.claimaction", 2),
                matchActionCount("reviewstep.reviewaction", 1))));

        getClient(adminToken).perform(get("/api/statistics/workflowOwners/search/byDateRange")
            .param("collection", collection.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowOwners", hasSize(2)))
            .andExpect(jsonPath("$._embedded.workflowOwners", contains(match(firstUser, 6), match(thirdUser, 2))))
            .andExpect(jsonPath("$._embedded.workflowOwners[0]", allOf(
                matchActionCount("reviewstep.claimaction", 2),
                matchActionCount("reviewstep.reviewaction", 2),
                matchActionCount("editstep.claimaction", 1),
                matchActionCount("editstep.editaction", 1))))
            .andExpect(jsonPath("$._embedded.workflowOwners[1]", allOf(
                matchActionCount("editstep.claimaction", 1),
                matchActionCount("editstep.editaction", 1))));

        getClient(adminToken).perform(get("/api/statistics/workflowOwners/search/byDateRange")
            .param("collection", anotherCollection.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowOwners", hasSize(2)))
            .andExpect(jsonPath("$._embedded.workflowOwners", contains(match(secondUser, 3), match(thirdUser, 2))))
            .andExpect(jsonPath("$._embedded.workflowOwners[0]", allOf(
                matchActionCount("reviewstep.claimaction", 2),
                matchActionCount("reviewstep.reviewaction", 1))))
            .andExpect(jsonPath("$._embedded.workflowOwners[1]", allOf(
                matchActionCount("editstep.claimaction", 1),
                matchActionCount("editstep.editaction", 1))));

        getClient(adminToken).perform(get("/api/statistics/workflowOwners/search/byDateRange")
            .param("collection", anotherCollection.getID().toString())
            .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowOwners", hasSize(1)))
            .andExpect(jsonPath("$._embedded.workflowOwners", contains(match(secondUser, 3))))
            .andExpect(jsonPath("$._embedded.workflowOwners[0]", allOf(
                matchActionCount("reviewstep.claimaction", 2),
                matchActionCount("reviewstep.reviewaction", 1))));

    }

    @Test
    public void testSearchByDateRangeWithNotFoundCollectionScope() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/statistics/workflowOwners/search/byDateRange")
            .param("collection", UUID.randomUUID().toString()))
            .andExpect(status().isBadRequest());

    }

    @Test
    public void testSearchByDateRangeWithInvalidCollectionScope() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/statistics/workflowOwners/search/byDateRange")
            .param("collection", "invalid"))
            .andExpect(status().isBadRequest());

    }

    @Test
    public void testSearchByDateRangeWithInvalidDates() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/workflowOwners/search/byDateRange")
            .param("startDate", "invalid date")
            .param("endDate", formatDate(addDays(new Date(), 1))))
            .andExpect(status().isUnprocessableEntity());

        getClient(adminToken).perform(get("/api/statistics/workflowOwners/search/byDateRange")
            .param("startDate", formatDate(addDays(new Date(), -1)))
            .param("endDate", "18/12/2021"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testSearchByDateRangeWithNotAdminUser() throws Exception {

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/statistics/workflowOwners/search/byDateRange"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void testFindOne() throws Exception {

        context.turnOffAuthorisationSystem();

        XmlWorkflowItem firstWorkflowItem = createWorkflowItem(collection);
        XmlWorkflowItem secondWorkflowItem = createWorkflowItem(collection);

        claimTaskAndApprove(firstWorkflowItem, firstUser);
        claimTaskAndApprove(firstWorkflowItem, firstUser);

        claimTaskAndApprove(secondWorkflowItem, firstUser);
        claimTaskAndApprove(secondWorkflowItem, thirdUser);

        context.restoreAuthSystemState();


        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/workflowOwners/" + firstUser.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", match(firstUser, 6)))
            .andExpect(jsonPath("$", matchActionCount("reviewstep.claimaction", 2)))
            .andExpect(jsonPath("$", matchActionCount("reviewstep.reviewaction", 2)))
            .andExpect(jsonPath("$", matchActionCount("editstep.claimaction", 1)))
            .andExpect(jsonPath("$", matchActionCount("editstep.editaction", 1)));

    }

    @Test
    public void testFindOneWithoutWorkflowOwner() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/workflowOwners/" + firstUser.getID().toString()))
            .andExpect(status().isNotFound());
    }

    @Test
    public void testFindOneWithUnknownUser() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/workflowOwners/" + UUID.randomUUID().toString()))
            .andExpect(status().isNotFound());
    }

    @Test
    public void testFindOneWithNotAdminUser() throws Exception {

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/statistics/workflowOwners/" + UUID.randomUUID().toString()))
            .andExpect(status().isForbidden());
    }

    private String formatDate(Date date) {
        return WorkflowOwnerStatisticsRestRepository.DATE_FORMATTER.format(date);
    }

    private void claimTaskAndApprove(XmlWorkflowItem workflowItem, EPerson user) throws Exception {
        ClaimedTask claimedTask = claimTask(workflowItem, user);
        approveClaimedTaskViaRest(user, claimedTask);
    }

    private void claimTaskAndReject(XmlWorkflowItem workflowItem, EPerson user, String reason) throws Exception {
        ClaimedTask claimedTask = claimTask(workflowItem, user);
        rejectClaimedTaskViaRest(user, claimedTask, reason);
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
