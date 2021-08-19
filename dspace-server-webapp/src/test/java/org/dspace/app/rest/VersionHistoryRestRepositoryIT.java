/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.Date;

import org.dspace.app.rest.matcher.VersionHistoryMatcher;
import org.dspace.app.rest.matcher.VersionMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.VersionBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.services.ConfigurationService;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.service.VersioningService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test class for the VersionHistory endpoint.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class VersionHistoryRestRepositoryIT extends AbstractControllerIntegrationTest {


    VersionHistory versionHistory;
    Item item;
    Version version;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private VersionHistoryService versionHistoryService;

    @Autowired
    private VersioningService versioningService;

    @Before
    public void setup() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        versionHistory = versionHistoryService.create(context);
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. Three public items that are readable by Anonymous with different subjects
        item = ItemBuilder.createItem(context, col1)
                          .withTitle("Public item 1")
                          .withIssueDate("2017-10-17")
                          .withAuthor("Smith, Donald").withAuthor("Doe, John")
                          .withSubject("ExtraEntry")
                          .build();
        version = VersionBuilder.createVersionWithVersionHistory(context, item, "test", versionHistory, 0).build();
        context.restoreAuthSystemState();
    }

    @Test
    public void findOneTest() throws Exception {
        getClient().perform(get("/api/versioning/versionhistories/" + versionHistory.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", is(VersionHistoryMatcher.matchEntry(versionHistory))));

    }


    @Test
    public void findOneForbiddenTest() throws Exception {

        configurationService.setProperty("versioning.item.history.view.admin", true);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/versioning/versionhistories/" + versionHistory.getID()))
                        .andExpect(status().isForbidden());
        configurationService.setProperty("versioning.item.history.view.admin", false);
    }

    @Test
    public void findOneWrongIDTest() throws Exception {
        int wrongVersionHistoryId = (versionHistory.getID() + 5) * 57;
        getClient().perform(get("/api/versioning/versionhistories/" + wrongVersionHistoryId))
                   .andExpect(status().isNotFound());

    }
    @Test
    public void findVersionsOneWrongIDTest() throws Exception {
        int wrongVersionHistoryId = (versionHistory.getID() + 5) * 57;
        getClient().perform(get("/api/versioning/versionhistories/" + wrongVersionHistoryId + "/versions"))
                   .andExpect(status().isNotFound());

    }

    @Test
    public void findVersionsOfVersionHistoryTest() throws Exception {
        Version version = versionHistoryService.getFirstVersion(context, versionHistory);
        getClient().perform(get("/api/versioning/versionhistories/" + versionHistory.getID() + "/versions"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.versions", contains(VersionMatcher.matchEntry(version))));

        context.turnOffAuthorisationSystem();
        Version secondVersion = versioningService
            .createNewVersion(context, versionHistory, item, "test", new Date(), 0);
        context.restoreAuthSystemState();

        getClient().perform(get("/api/versioning/versionhistories/" + versionHistory.getID() + "/versions"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.versions", containsInAnyOrder(VersionMatcher.matchEntry(version),
                                                                                  VersionMatcher
                                                                                      .matchEntry(secondVersion))));

    }
    @Test
    public void findVersionsOfVersionHistoryPaginationTest() throws Exception {

        context.turnOffAuthorisationSystem();
        Version version = versionHistoryService.getFirstVersion(context, versionHistory);
        Version secondVersion = VersionBuilder.createVersionWithVersionHistory(context, item, "test", versionHistory, 3)
                                              .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/api/versioning/versionhistories/" + versionHistory.getID() + "/versions")
                                .param("size", "1"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.versions", contains(VersionMatcher.matchEntry(secondVersion))))
                   .andExpect(jsonPath("$._embedded.versions",
                                       Matchers.not(contains(VersionMatcher.matchEntry(version)))));
        getClient().perform(get("/api/versioning/versionhistories/" + versionHistory.getID() + "/versions")
                                .param("size", "1")
                                .param("page", "1"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.versions", contains(VersionMatcher.matchEntry(version))))
                   .andExpect(jsonPath("$._embedded.versions",
                                       Matchers.not(contains(VersionMatcher.matchEntry(secondVersion)))));

    }

    @Test
    public void findOldestVersionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Collection test").build();

        Item item = ItemBuilder.createItem(context, col)
                          .withTitle("Public test item")
                          .withIssueDate("2021-04-27")
                          .withAuthor("Doe, John")
                          .withSubject("ExtraEntry")
                          .build();

        Item item2 = ItemBuilder.createItem(context, col)
                                .withTitle("Public test item 2")
                                .withAuthor("Doe, John")
                                .withSubject("ExtraEntry")
                                .build();

        VersionHistory versionHistory = versionHistoryService.create(context);
        Version v7 = VersionBuilder.createVersionWithVersionHistory(context, item, "test", versionHistory, 7)
                                    .build();
        VersionBuilder.createVersionWithVersionHistory(context, item2, "test 2", versionHistory, 98)
                                    .build();

        context.restoreAuthSystemState();

        Integer id = versionHistory.getID();
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/versioning/versionhistories/" + id + "/oldestversion"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(VersionMatcher.matchEntry(v7))))
                             .andExpect(jsonPath("$._links.self.href", Matchers.allOf(Matchers.containsString(
                                        "api/versioning/versions/" + v7.getID())
                                        )))
                             .andExpect(jsonPath("$._links.versionhistory.href", Matchers.allOf(Matchers.containsString(
                                        "api/versioning/versions/" + v7.getID() + "/versionhistory"
                                        ))))
                             .andExpect(jsonPath("$._links.item.href", Matchers.allOf(Matchers.containsString(
                                        "api/versioning/versions/" + v7.getID() + "/item")
                                        )));
    }

    @Test
    public void findOldestVersionNotFoundTest() throws Exception {
        context.turnOffAuthorisationSystem();
        VersionHistory versionHistory = versionHistoryService.create(context);

        context.restoreAuthSystemState();

        Integer id = versionHistory.getID();
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/versioning/versionhistories/" + id + "/oldestversion"))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void findOldestVersionForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Collection test").build();

        Item item = ItemBuilder.createItem(context, col)
                          .withTitle("Public test item")
                          .withIssueDate("2021-04-27")
                          .withAuthor("Doe, John")
                          .withSubject("ExtraEntry")
                          .build();

        Item item2 = ItemBuilder.createItem(context, col)
                                .withTitle("Public test item 2")
                                .withAuthor("Doe, John")
                                .withSubject("ExtraEntry")
                                .build();

        VersionHistory versionHistory = versionHistoryService.create(context);
        VersionBuilder.createVersionWithVersionHistory(context, item, "test", versionHistory, 7).build();
        VersionBuilder.createVersionWithVersionHistory(context, item2, "test 2", versionHistory, 98).build();

        context.restoreAuthSystemState();

        Integer id = versionHistory.getID();
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(get("/api/versioning/versionhistories/" + id + "/oldestversion"))
                               .andExpect(status().isForbidden());
    }

    @Test
    public void findOldestVersionUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Collection test").build();

        Item item = ItemBuilder.createItem(context, col)
                          .withTitle("Public test item")
                          .withIssueDate("2021-04-27")
                          .withAuthor("Doe, John")
                          .withSubject("ExtraEntry")
                          .build();

        Item item2 = ItemBuilder.createItem(context, col)
                                .withTitle("Public test item 2")
                                .withAuthor("Doe, John")
                                .withSubject("ExtraEntry")
                                .build();

        VersionHistory versionHistory = versionHistoryService.create(context);
        VersionBuilder.createVersionWithVersionHistory(context, item, "test", versionHistory, 7).build();
        VersionBuilder.createVersionWithVersionHistory(context, item2, "test 2", versionHistory, 98).build();

        context.restoreAuthSystemState();

        Integer id = versionHistory.getID();
        getClient().perform(get("/api/versioning/versionhistories/" + id + "/oldestversion"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOldestVersionNTest() throws Exception {
        Integer id = Integer.MAX_VALUE;
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/versioning/versionhistories/" + id + "/oldestversion"))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void findOldestVersionBadRequestTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/versioning/versionhistories/" + null + "/oldestversion"))
                             .andExpect(status().isBadRequest());
    }

    @Test
    public void findOldestVersionWithNegativVersionHistoryIdTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/versioning/versionhistories/" + -1 + "/oldestversion"))
                             .andExpect(status().isBadRequest());
    }

    @Test
    public void findCurrentVersionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Collection test").build();

        Item item = ItemBuilder.createItem(context, col)
                          .withTitle("Public test item")
                          .withIssueDate("2021-04-27")
                          .withAuthor("Doe, John")
                          .withSubject("ExtraEntry")
                          .build();

        Item item2 = ItemBuilder.createItem(context, col)
                                .withTitle("Public test item 2")
                                .withAuthor("Doe, John")
                                .withSubject("ExtraEntry")
                                .build();

        VersionHistory versionHistory = versionHistoryService.create(context);
        VersionBuilder.createVersionWithVersionHistory(context, item, "test", versionHistory, 7)
                      .build();
        Version v98 = VersionBuilder.createVersionWithVersionHistory(context, item2, "test 2", versionHistory, 98)
                                    .build();

        context.restoreAuthSystemState();

        Integer id = versionHistory.getID();
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/versioning/versionhistories/" + id + "/currentversion"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(VersionMatcher.matchEntry(v98))))
                             .andExpect(jsonPath("$._links.self.href", Matchers.allOf(Matchers.containsString(
                                        "api/versioning/versions/" + v98.getID())
                                        )))
                             .andExpect(jsonPath("$._links.versionhistory.href", Matchers.allOf(Matchers.containsString(
                                        "api/versioning/versions/" + v98.getID() + "/versionhistory"
                                        ))))
                             .andExpect(jsonPath("$._links.item.href", Matchers.allOf(Matchers.containsString(
                                        "api/versioning/versions/" + v98.getID() + "/item")
                                        )));
    }

    @Test
    public void findCurrentVersionNotFoundTest() throws Exception {
        context.turnOffAuthorisationSystem();
        VersionHistory versionHistory = versionHistoryService.create(context);
        context.restoreAuthSystemState();

        Integer id = versionHistory.getID();
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/versioning/versionhistories/" + id + "/currentversion"))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void findCurrentVersionForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Collection test").build();

        Item item = ItemBuilder.createItem(context, col)
                          .withTitle("Public test item")
                          .withIssueDate("2021-04-27")
                          .withAuthor("Doe, John")
                          .withSubject("ExtraEntry")
                          .build();

        Item item2 = ItemBuilder.createItem(context, col)
                                .withTitle("Public test item 2")
                                .withAuthor("Doe, John")
                                .withSubject("ExtraEntry")
                                .build();

        VersionHistory versionHistory = versionHistoryService.create(context);
        VersionBuilder.createVersionWithVersionHistory(context, item, "test", versionHistory, 7).build();
        VersionBuilder.createVersionWithVersionHistory(context, item2, "test 2", versionHistory, 98).build();

        context.restoreAuthSystemState();

        Integer id = versionHistory.getID();
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(get("/api/versioning/versionhistories/" + id + "/currentversion"))
                               .andExpect(status().isForbidden());
    }

    @Test
    public void findCurrentVersionUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Collection test").build();

        Item item = ItemBuilder.createItem(context, col)
                          .withTitle("Public test item")
                          .withIssueDate("2021-04-27")
                          .withAuthor("Doe, John")
                          .withSubject("ExtraEntry")
                          .build();

        Item item2 = ItemBuilder.createItem(context, col)
                                .withTitle("Public test item 2")
                                .withAuthor("Doe, John")
                                .withSubject("ExtraEntry")
                                .build();

        VersionHistory versionHistory = versionHistoryService.create(context);
        VersionBuilder.createVersionWithVersionHistory(context, item, "test", versionHistory, 7).build();
        VersionBuilder.createVersionWithVersionHistory(context, item2, "test 2", versionHistory, 98).build();

        context.restoreAuthSystemState();

        Integer id = versionHistory.getID();
        getClient().perform(get("/api/versioning/versionhistories/" + id + "/currentversion"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findCurrentVersionWithVersionHistoryNotFoundTest() throws Exception {
        Integer id = Integer.MAX_VALUE;
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/versioning/versionhistories/" + id + "/currentversion"))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void findCurrentVersionBadRequestTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/versioning/versionhistories/" + null + "/currentversion"))
                             .andExpect(status().isBadRequest());
    }

    @Test
    public void findCurrentVersionWithNegativVersionHistoryIdTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/versioning/versionhistories/" + -1 + "/currentversion"))
                             .andExpect(status().isBadRequest());
    }

    @Test
    public void findLastVersionWorkspaceItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Collection test").build();

        Item item = ItemBuilder.createItem(context, col)
                               .withTitle("Public test item")
                               .withIssueDate("2021-04-27")
                               .withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .build();

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col)
                                                  .withTitle("Workspace Item 1")
                                                  .withIssueDate("2017-10-17")
                                                  .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                                  .withSubject("ExtraEntry")
                                                  .build();

        VersionHistory versionHistory = versionHistoryService.create(context);
        VersionBuilder.createVersionWithVersionHistory(context, item, "test", versionHistory, 6).build();
        Version v10 = VersionBuilder.createVersionWithVersionHistory(context, witem.getItem(),
                      "test", versionHistory, 10).build();

        context.restoreAuthSystemState();

        Integer id = versionHistory.getID();
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/versioning/versionhistories/" + id + "/lastversion"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(VersionMatcher.matchEntry(v10))));
    }

    @Test
    public void findLastVersionItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Collection test").build();

        Item item = ItemBuilder.createItem(context, col)
                               .withTitle("Public test item")
                               .withIssueDate("2021-04-27")
                               .withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .build();

        Item item2 = ItemBuilder.createItem(context, col)
                                .withTitle("Public test item 2")
                                .withAuthor("Doe, John")
                                .withSubject("ExtraEntry")
                                .build();

        VersionHistory versionHistory = versionHistoryService.create(context);
        Version v6 = VersionBuilder.createVersionWithVersionHistory(context, item, "test", versionHistory, 6).build();
        VersionBuilder.createVersionWithVersionHistory(context, item2, "test", versionHistory, 3).build();

        context.restoreAuthSystemState();

        Integer id = versionHistory.getID();
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/versioning/versionhistories/" + id + "/lastversion"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(VersionMatcher.matchEntry(v6))));
    }

    @Test
    public void findLastVersionWorkflowItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Collection test").build();

        Item item = ItemBuilder.createItem(context, col)
                               .withTitle("Public test item")
                               .withIssueDate("2021-04-27")
                               .withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .build();

        XmlWorkflowItem workflowItem = WorkflowItemBuilder.createWorkflowItem(context, col)
                                                          .withTitle("Workflow Item")
                                                          .withIssueDate("2021-05-21")
                                                          .build();

        VersionHistory versionHistory = versionHistoryService.create(context);
        VersionBuilder.createVersionWithVersionHistory(context, item, "test", versionHistory, 6).build();
        Version v10 = VersionBuilder.createVersionWithVersionHistory(context, workflowItem.getItem(),
                                     "test", versionHistory, 10).build();

        context.restoreAuthSystemState();

        Integer id = versionHistory.getID();
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/versioning/versionhistories/" + id + "/lastversion"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(VersionMatcher.matchEntry(v10))));
    }

    @Test
    public void findLastVersionNotFoundTest() throws Exception {
        Integer id = Integer.MAX_VALUE;
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/versioning/versionhistories/" + id + "/lastversion"))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void findLastVersionBadRequestTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/versioning/versionhistories/" + null + "/lastversion"))
                             .andExpect(status().isBadRequest());
    }

    @Test
    public void findLastVersionWithNegativVersionHistoryIdTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/versioning/versionhistories/" + -1 + "/lastversion"))
                             .andExpect(status().isBadRequest());
    }
}

