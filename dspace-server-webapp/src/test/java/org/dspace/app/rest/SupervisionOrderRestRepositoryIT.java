/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;
import static com.jayway.jsonpath.JsonPath.read;
import static org.dspace.app.rest.matcher.SupervisionOrderMatcher.matchSuperVisionOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.MediaType;

import org.dspace.app.rest.matcher.WorkflowItemMatcher;
import org.dspace.app.rest.matcher.WorkspaceItemMatcher;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.repository.SupervisionOrderRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.SupervisionOrderBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.InstallItemService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.supervision.SupervisionOrder;
import org.dspace.supervision.service.SupervisionOrderService;
import org.dspace.workflow.WorkflowItem;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test against class {@link SupervisionOrderRestRepository}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class SupervisionOrderRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private SupervisionOrderService supervisionOrderService;

    @Autowired
    private InstallItemService installItemService;

    @Before
    public void init() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        context.restoreAuthSystemState();
    }

    @Test
    public void findAllByAnonymousUserTest() throws Exception {
        getClient().perform(get("/api/core/supervisionorders/"))
                             .andExpect(status().isUnauthorized());
    }

    @Test
    public void findAllByNotAdminUserTest() throws Exception {

        String authToken = getAuthToken(eperson.getEmail(), password);

        getClient(authToken).perform(get("/api/core/supervisionorders/"))
                             .andExpect(status().isForbidden());
    }

    @Test
    public void findAllByAdminTest() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection col1 =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Collection 1")
                             .build();

        Item item =
            ItemBuilder.createItem(context, col1)
                       .withTitle("item title")
                       .build();

        Group groupA = GroupBuilder.createGroup(context)
                                       .withName("group A")
                                       .addMember(admin)
                                       .build();

        Group groupB = GroupBuilder.createGroup(context)
                                   .withName("group B")
                                   .addMember(eperson)
                                   .build();

        SupervisionOrder supervisionOrderOne =
            SupervisionOrderBuilder.createSupervisionOrder(context, item, groupA)
                                   .build();

        SupervisionOrder supervisionOrderTwo =
            SupervisionOrderBuilder.createSupervisionOrder(context, item, groupB)
                                   .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/core/supervisionorders/"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.page.totalElements", is(2)))
                             .andExpect(jsonPath("$._embedded.supervisionorders",
                                 containsInAnyOrder(
                                     matchSuperVisionOrder(supervisionOrderOne),
                                     matchSuperVisionOrder(supervisionOrderTwo)
                                 )))
                             .andExpect(jsonPath("$._links.self.href", containsString("/api/core/supervisionorders")));
    }

    @Test
    public void findOneByAnonymousUserTest() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection col1 =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Collection 1")
                             .build();

        Item item =
            ItemBuilder.createItem(context, col1)
                       .withTitle("item title")
                       .build();

        Group group =
            GroupBuilder.createGroup(context)
                        .withName("group")
                        .addMember(eperson)
                        .build();

        SupervisionOrder supervisionOrder =
            SupervisionOrderBuilder.createSupervisionOrder(context, item, group)
                                   .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/supervisionorders/" + supervisionOrder.getID()))
                             .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneByNotAdminUserTest() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection col1 =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Collection 1")
                             .build();

        Item item =
            ItemBuilder.createItem(context, col1)
                       .withTitle("item title")
                       .build();

        Group group =
            GroupBuilder.createGroup(context)
                        .withName("group")
                        .addMember(eperson)
                        .build();

        SupervisionOrder supervisionOrder =
            SupervisionOrderBuilder.createSupervisionOrder(context, item, group)
                                   .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);

        getClient(authToken).perform(get("/api/core/supervisionorders/" + supervisionOrder.getID()))
                             .andExpect(status().isForbidden());
    }

    @Test
    public void findOneByAdminTest() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection col1 =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Collection 1")
                             .build();

        Item item =
            ItemBuilder.createItem(context, col1)
                       .withTitle("item title")
                       .build();

        Group group =
            GroupBuilder.createGroup(context)
                        .withName("group")
                        .addMember(eperson)
                        .build();

        SupervisionOrder supervisionOrder =
            SupervisionOrderBuilder.createSupervisionOrder(context, item, group)
                                   .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/core/supervisionorders/" + supervisionOrder.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", matchSuperVisionOrder(supervisionOrder)));
    }

    @Test
    public void findOneByAdminButNotFoundTest() throws Exception {
        int fakeId = 12354326;
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/core/supervisionorders/" + fakeId))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void findByItemByAnonymousUserTest() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection col1 =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Collection 1")
                             .build();

        Item item =
            ItemBuilder.createItem(context, col1)
                       .withTitle("item title")
                       .build();

        Group group =
            GroupBuilder.createGroup(context)
                        .withName("group")
                        .addMember(eperson)
                        .build();

        SupervisionOrderBuilder.createSupervisionOrder(context, item, group).build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/supervisionorders/search/byItem")
                       .param("uuid", item.getID().toString()))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findByItemByNotAdminUserTest() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection col1 =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Collection 1")
                             .build();

        Item item =
            ItemBuilder.createItem(context, col1)
                       .withTitle("item title")
                       .build();

        Group group =
            GroupBuilder.createGroup(context)
                        .withName("group")
                        .addMember(eperson)
                        .build();

        SupervisionOrderBuilder.createSupervisionOrder(context, item, group).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);

        getClient(authToken).perform(get("/api/core/supervisionorders/search/byItem")
                                .param("uuid", item.getID().toString()))
                            .andExpect(status().isForbidden());
    }

    @Test
    public void findByItemByAdminButNotFoundItemTest() throws Exception {
        String fakeItemId = "d9dcf4c3-093d-413e-a538-93d8589d3ea6";

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/core/supervisionorders/search/byItem")
                                 .param("uuid", fakeItemId))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void findByItemByAdminTest() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity =
            CommunityBuilder.createCommunity(context)
                            .withName("Parent Community")
                            .build();

        Collection col1 =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Collection 1")
                             .build();

        Item itemOne =
            ItemBuilder.createItem(context, col1)
                       .withTitle("item title")
                       .build();

        Group groupA =
            GroupBuilder.createGroup(context)
                        .withName("group A")
                        .addMember(admin)
                        .build();

        Group groupB =
            GroupBuilder.createGroup(context)
                        .withName("group B")
                        .addMember(eperson)
                        .build();

        SupervisionOrder supervisionOrderOne =
            SupervisionOrderBuilder.createSupervisionOrder(context, itemOne, groupA)
                                   .build();

        SupervisionOrder supervisionOrderTwo =
            SupervisionOrderBuilder.createSupervisionOrder(context, itemOne, groupB)
                                   .build();

        Item itemTwo =
            ItemBuilder.createItem(context, col1)
                       .withTitle("item two title")
                       .build();

        SupervisionOrder supervisionOrderItemTwo =
            SupervisionOrderBuilder.createSupervisionOrder(context, itemTwo, groupA)
                                   .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/core/supervisionorders/search/byItem")
                                 .param("uuid", itemOne.getID().toString()))
                             .andExpect(jsonPath("$.page.totalElements", is(2)))
                             .andExpect(jsonPath("$._embedded.supervisionorders", containsInAnyOrder(
                                 matchSuperVisionOrder(supervisionOrderOne),
                                 matchSuperVisionOrder(supervisionOrderTwo)
                             )))
                             .andExpect(jsonPath("$._links.self.href", containsString(
                                 "/api/core/supervisionorders/search/byItem?uuid=" + itemOne.getID()))
                             );

        getClient(adminToken).perform(get("/api/core/supervisionorders/search/byItem")
                                 .param("uuid", itemTwo.getID().toString()))
                             .andExpect(jsonPath("$.page.totalElements", is(1)))
                             .andExpect(jsonPath("$._embedded.supervisionorders", contains(
                                 matchSuperVisionOrder(supervisionOrderItemTwo)
                             )))
                             .andExpect(jsonPath("$._links.self.href", containsString(
                                 "/api/core/supervisionorders/search/byItem?uuid=" + itemTwo.getID()))
                             );
    }

    @Test
    public void createByAnonymousUserTest() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity =
            CommunityBuilder.createCommunity(context)
                            .withName("Parent Community")
                            .build();

        Collection col1 =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Collection 1")
                             .build();

        Item item =
            ItemBuilder.createItem(context, col1)
                       .withTitle("item title")
                       .build();

        Group group =
            GroupBuilder.createGroup(context)
                        .withName("group")
                        .addMember(eperson)
                        .build();

        context.restoreAuthSystemState();

        getClient().perform(post("/api/core/supervisionorders/")
                       .param("uuid", item.getID().toString())
                       .param("group", group.getID().toString())
                       .param("type", "EDITOR")
                       .contentType(contentType))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void createByNotAdminUserTest() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity =
            CommunityBuilder.createCommunity(context)
                            .withName("Parent Community")
                            .build();

        Collection col1 =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Collection 1")
                             .build();

        Item item =
            ItemBuilder.createItem(context, col1)
                       .withTitle("item title")
                       .build();

        Group group =
            GroupBuilder.createGroup(context)
                        .withName("group")
                        .addMember(eperson)
                        .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(post("/api/core/supervisionorders/")
                                .param("uuid", item.getID().toString())
                                .param("group", group.getID().toString())
                                .param("type", "EDITOR")
                                .contentType(contentType))
                            .andExpect(status().isForbidden());
    }

    @Test
    public void createByAdminButMissingParametersTest() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity =
            CommunityBuilder.createCommunity(context)
                            .withName("Parent Community")
                            .build();

        Collection col1 =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Collection 1")
                             .build();

        Item itemOne =
            ItemBuilder.createItem(context, col1)
                       .withTitle("item title")
                       .build();

        Group groupA =
            GroupBuilder.createGroup(context)
                        .withName("group A")
                        .addMember(eperson)
                        .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(post("/api/core/supervisionorders/")
                                 .param("group", groupA.getID().toString())
                                 .param("type", "EDITOR")
                                 .contentType(contentType))
                             .andExpect(status().isBadRequest());

        getClient(adminToken).perform(post("/api/core/supervisionorders/")
                                 .param("uuid", itemOne.getID().toString())
                                 .param("type", "EDITOR")
                                 .contentType(contentType))
                             .andExpect(status().isBadRequest());

        getClient(adminToken).perform(post("/api/core/supervisionorders/")
                                 .param("uuid", itemOne.getID().toString())
                                 .param("group", groupA.getID().toString())
                                 .contentType(contentType))
                             .andExpect(status().isBadRequest());
    }

    @Test
    public void createByAdminButIncorrectTypeParameterTest() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection col1 =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Collection 1")
                             .build();

        Item item =
            ItemBuilder.createItem(context, col1)
                       .withTitle("item title")
                       .build();

        Group group =
            GroupBuilder.createGroup(context)
                        .withName("group")
                        .addMember(eperson)
                        .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(post("/api/core/supervisionorders/")
                                 .param("uuid", item.getID().toString())
                                 .param("group", group.getID().toString())
                                 .param("type", "WRONG")
                                 .contentType(contentType))
                             .andExpect(status().isBadRequest());
    }

    @Test
    public void createByAdminButNotFoundItemOrGroupTest() throws Exception {

        String fakeItemId = "d9dcf4c3-093d-413e-a538-93d8589d3ea6";
        String fakeGroupId = "d9dcf4c3-093d-413e-a538-93d8589d3ea6";

        context.turnOffAuthorisationSystem();

        Collection col1 =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Collection 1")
                             .build();

        Item itemOne =
            ItemBuilder.createItem(context, col1)
                       .withTitle("item title")
                       .build();

        Group groupA = GroupBuilder.createGroup(context)
                                   .withName("group A")
                                   .addMember(eperson)
                                   .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(post("/api/core/supervisionorders/")
                                 .param("uuid", fakeItemId)
                                 .param("group", groupA.getID().toString())
                                 .param("type", "EDITOR")
                                 .contentType(contentType))
                             .andExpect(status().isUnprocessableEntity());

        getClient(adminToken).perform(post("/api/core/supervisionorders/")
                                 .param("uuid", itemOne.getID().toString())
                                 .param("group", fakeGroupId)
                                 .param("type", "EDITOR")
                                 .contentType(contentType))
                             .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void createTheSameSupervisionOrderTwiceTest() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity =
            CommunityBuilder.createCommunity(context)
                            .withName("Parent Community")
                            .build();

        Collection col1 =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Collection 1")
                             .build();

        WorkspaceItem workspaceItem =
            WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                                .withTitle("Workspace Item 1")
                                .withIssueDate("2017-10-17")
                                .withSubject("ExtraEntry")
                                .grantLicense()
                                .build();

        Item itemOne = workspaceItem.getItem();

        Group groupA =
            GroupBuilder.createGroup(context)
                        .withName("group A")
                        .addMember(eperson)
                        .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(post("/api/core/supervisionorders/")
                                .param("uuid", itemOne.getID().toString())
                                .param("group", groupA.getID().toString())
                                .param("type", "EDITOR")
                                .contentType(contentType))
                             .andExpect(status().isCreated());

        getClient(adminToken).perform(post("/api/core/supervisionorders/")
                                .param("uuid", itemOne.getID().toString())
                                .param("group", groupA.getID().toString())
                                .param("type", "OBSERVER")
                                .contentType(contentType))
                             .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void createSupervisionOrderOnArchivedItemTest() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity =
            CommunityBuilder.createCommunity(context)
                            .withName("Parent Community")
                            .build();

        Collection col1 =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Collection 1")
                             .build();

        // create archived item
        Item itemOne =
            ItemBuilder.createItem(context, col1)
                       .withTitle("item title")
                       .withIssueDate("2017-10-17")
                       .build();

        Group groupA =
            GroupBuilder.createGroup(context)
                        .withName("group A")
                        .addMember(eperson)
                        .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(post("/api/core/supervisionorders/")
                                .param("uuid", itemOne.getID().toString())
                                .param("group", groupA.getID().toString())
                                .param("type", "OBSERVER")
                                .contentType(contentType))
                             .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void createByAdminTest() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson userA =
            EPersonBuilder.createEPerson(context)
                          .withCanLogin(true)
                          .withEmail("test1@email.com")
                          .withPassword(password)
                          .build();

        EPerson userB =
            EPersonBuilder.createEPerson(context)
                          .withCanLogin(true)
                          .withEmail("test2@email.com")
                          .withPassword(password)
                          .build();

        EPerson userC =
            EPersonBuilder.createEPerson(context)
                          .withCanLogin(true)
                          .withEmail("test3@email.com")
                          .withPassword(password)
                          .build();

        parentCommunity =
            CommunityBuilder.createCommunity(context)
                            .withName("Parent Community")
                            .build();

        Collection publications =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Publications")
                             .withEntityType("Publication")
                             .build();

        InputStream pdf = getClass().getResourceAsStream("simple-article.pdf");

        WorkspaceItem witem =
            WorkspaceItemBuilder.createWorkspaceItem(context, publications)
                                .withTitle("Workspace Item 1")
                                .withIssueDate("2017-10-17")
                                .withSubject("ExtraEntry")
                                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf)
                                .grantLicense()
                                .build();

        Item itemOne = witem.getItem();

        Group groupA =
            GroupBuilder.createGroup(context)
                        .withName("group A")
                        .addMember(userA)
                        .build();

        Group groupB =
            GroupBuilder.createGroup(context)
                        .withName("group B")
                        .addMember(userB)
                        .build();

        Group groupC =
            GroupBuilder.createGroup(context)
                        .withName("group C")
                        .addMember(userC)
                        .build();

        context.restoreAuthSystemState();

        AtomicInteger supervisionOrderIdOne = new AtomicInteger();
        AtomicInteger supervisionOrderIdTwo = new AtomicInteger();
        AtomicInteger supervisionOrderIdThree = new AtomicInteger();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(post("/api/core/supervisionorders/")
                                .param("uuid", itemOne.getID().toString())
                                .param("group", groupA.getID().toString())
                                .param("type", "EDITOR")
                                .contentType(contentType))
                             .andExpect(status().isCreated())
                             .andDo(result -> supervisionOrderIdOne
                                 .set(read(result.getResponse().getContentAsString(), "$.id")));

        getClient(adminToken).perform(post("/api/core/supervisionorders/")
                                .param("uuid", itemOne.getID().toString())
                                .param("group", groupB.getID().toString())
                                .param("type", "OBSERVER")
                                .contentType(contentType))
                             .andExpect(status().isCreated())
                             .andDo(result -> supervisionOrderIdTwo
                                 .set(read(result.getResponse().getContentAsString(), "$.id")));

        getClient(adminToken).perform(post("/api/core/supervisionorders/")
                                          .param("uuid", itemOne.getID().toString())
                                          .param("group", groupC.getID().toString())
                                          .param("type", "NONE")
                                          .contentType(contentType))
                             .andExpect(status().isCreated())
                             .andDo(result -> supervisionOrderIdThree
                                 .set(read(result.getResponse().getContentAsString(), "$.id")));

        SupervisionOrder supervisionOrderOne =
            supervisionOrderService.find(context, supervisionOrderIdOne.get());

        SupervisionOrder supervisionOrderTwo =
            supervisionOrderService.find(context, supervisionOrderIdTwo.get());

        SupervisionOrder supervisionOrderThree =
            supervisionOrderService.find(context, supervisionOrderIdThree.get());

        getClient(adminToken).perform(get("/api/core/supervisionorders/" + supervisionOrderOne.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$",
                                 matchSuperVisionOrder(context.reloadEntity(supervisionOrderOne))));

        getClient(adminToken).perform(get("/api/core/supervisionorders/" + supervisionOrderTwo.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$",
                                 matchSuperVisionOrder(context.reloadEntity(supervisionOrderTwo))));

        getClient(adminToken).perform(get("/api/core/supervisionorders/" + supervisionOrderThree.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$",
                                                 matchSuperVisionOrder(context.reloadEntity(supervisionOrderThree))));

        String authTokenA = getAuthToken(userA.getEmail(), password);
        String authTokenB = getAuthToken(userB.getEmail(), password);
        String authTokenC = getAuthToken(userC.getEmail(), password);

        String patchBody = getPatchContent(List.of(
            new ReplaceOperation("/sections/traditionalpageone/dc.title/0", Map.of("value", "New Title"))
        ));

        // update title of workspace item by userA is Ok
        getClient(authTokenA).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                                 .content(patchBody)
                                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.errors").doesNotExist())
                             .andExpect(jsonPath("$",
                                 // check the new title and untouched values
                                 Matchers.is(WorkspaceItemMatcher
                                     .matchItemWithTitleAndDateIssuedAndSubject(
                                         witem,
                                         "New Title", "2017-10-17",
                                         "ExtraEntry"))));

        getClient(authTokenA).perform(get("/api/submission/workspaceitems/" + witem.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.errors").doesNotExist())
                             .andExpect(jsonPath("$",
                                 Matchers.is(
                                     WorkspaceItemMatcher
                                         .matchItemWithTitleAndDateIssuedAndSubject(
                                             witem,
                                             "New Title", "2017-10-17",
                                             "ExtraEntry")
                                 )));

        // supervisor of a NONE type cannot see workspace item
        getClient(authTokenC).perform(get("/api/submission/workspaceitems/" + witem.getID()))
                             .andExpect(status().isForbidden());


        // update title of workspace item by userB is Forbidden
        getClient(authTokenB).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                                 .content(patchBody)
                                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isForbidden());

        // supervisor of a NONE type cannot patch workspace item
        getClient(authTokenC).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                                          .content(patchBody)
                                          .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isForbidden());
    }

    @Test
    public void deleteByAnonymousUserTest() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection col1 =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Collection 1")
                             .build();

        Item item =
            ItemBuilder.createItem(context, col1)
                       .withTitle("item title")
                       .build();

        Group group =
            GroupBuilder.createGroup(context)
                        .withName("group")
                        .addMember(eperson)
                        .build();

        SupervisionOrder supervisionOrder =
            SupervisionOrderBuilder.createSupervisionOrder(context, item, group)
                                   .build();

        context.restoreAuthSystemState();

        getClient().perform(delete("/api/core/supervisionorders/" + supervisionOrder.getID()))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteByNotAdminUserTest() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection col1 =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Collection 1")
                             .build();

        Item item =
            ItemBuilder.createItem(context, col1)
                       .withTitle("item title")
                       .build();

        Group group =
            GroupBuilder.createGroup(context)
                        .withName("group")
                        .addMember(eperson)
                        .build();

        SupervisionOrder supervisionOrder =
            SupervisionOrderBuilder.createSupervisionOrder(context, item, group)
                                   .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);

        getClient(authToken).perform(delete("/api/core/supervisionorders/" + supervisionOrder.getID()))
                            .andExpect(status().isForbidden());
    }

    @Test
    public void deleteByAdminButNotFoundTest() throws Exception {
        int fakeId = 12354326;
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(delete("/api/core/supervisionorders/" + fakeId))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void deleteByAdminTest() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection col1 =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Collection 1")
                             .build();

        Item item =
            ItemBuilder.createItem(context, col1)
                       .withTitle("item title")
                       .build();

        Group group =
            GroupBuilder.createGroup(context)
                        .withName("group")
                        .addMember(eperson)
                        .build();

        SupervisionOrder supervisionOrder =
            SupervisionOrderBuilder.createSupervisionOrder(context, item, group)
                                   .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/core/supervisionorders/" + supervisionOrder.getID()))
                             .andExpect(status().isOk());

        getClient(adminToken).perform(delete("/api/core/supervisionorders/" + supervisionOrder.getID()))
                             .andExpect(status().isNoContent());

        getClient(adminToken).perform(get("/api/core/supervisionorders/" + supervisionOrder.getID()))
                             .andExpect(status().isNotFound());

        String patchBody = getPatchContent(List.of(new ReplaceOperation("/withdrawn", true)));

        getClient(getAuthToken(eperson.getEmail(), password)).perform(patch("/api/core/items/" + item.getID())
                                          .content(patchBody)
                                          .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isForbidden());

        Assert.assertTrue(item.getResourcePolicies().stream()
                              .noneMatch(rp -> group.getID().equals(rp.getGroup().getID())));
    }

    @Test
    public void deleteItemThenSupervisionOrderBeDeletedTest() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection col1 =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Collection 1")
                             .build();

        Item item =
            ItemBuilder.createItem(context, col1)
                       .withTitle("item title")
                       .build();

        Group group =
            GroupBuilder.createGroup(context)
                        .withName("group")
                        .addMember(eperson)
                        .build();

        SupervisionOrder supervisionOrder =
            SupervisionOrderBuilder.createSupervisionOrder(context, item, group)
                                   .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/core/supervisionorders/" + supervisionOrder.getID()))
                             .andExpect(status().isOk());

        getClient(adminToken).perform(delete("/api/core/items/" + item.getID()))
                             .andExpect(status().isNoContent());

        getClient(adminToken).perform(get("/api/core/supervisionorders/" + supervisionOrder.getID()))
                             .andExpect(status().isNotFound());

    }

    @Test
    public void deleteGroupThenSupervisionOrderBeDeletedTest() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection col1 =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Collection 1")
                             .build();

        Item item =
            ItemBuilder.createItem(context, col1)
                       .withTitle("item title")
                       .build();

        Group group =
            GroupBuilder.createGroup(context)
                        .withName("group")
                        .addMember(eperson)
                        .build();

        SupervisionOrder supervisionOrder =
            SupervisionOrderBuilder.createSupervisionOrder(context, item, group)
                                   .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/core/supervisionorders/" + supervisionOrder.getID()))
                             .andExpect(status().isOk());

        getClient(adminToken).perform(delete("/api/eperson/groups/" + group.getID()))
                             .andExpect(status().isNoContent());

        getClient(adminToken).perform(get("/api/core/supervisionorders/" + supervisionOrder.getID()))
                             .andExpect(status().isNotFound());

    }

    @Test
    public void deleteWorkspaceItemThenSupervisionOrderIsDeletedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity =
            CommunityBuilder.createCommunity(context)
                            .withName("Parent Community")
                            .build();

        Collection publications =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Publications")
                             .withEntityType("Publication")
                             .build();

        Group groupA =
            GroupBuilder.createGroup(context)
                        .withName("group A")
                        .addMember(eperson)
                        .build();

        WorkspaceItem witem =
            WorkspaceItemBuilder.createWorkspaceItem(context, publications)
                                .withTitle("Workspace Item 1")
                                .withIssueDate("2017-10-17")
                                .withSubject("ExtraEntry")
                                .grantLicense()
                                .build();

        context.restoreAuthSystemState();

        AtomicReference<Integer> idRef = new AtomicReference<>();
        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(post("/api/core/supervisionorders/")
                                 .param("uuid", witem.getItem().getID().toString())
                                 .param("group", groupA.getID().toString())
                                 .param("type", "EDITOR")
                                 .contentType(contentType))
                             .andExpect(status().isCreated())
                             .andDo(result -> idRef
                                 .set(read(result.getResponse().getContentAsString(), "$.id")));

        getClient(adminToken).perform(get("/api/core/supervisionorders/" + idRef.get()))
                             .andExpect(status().isOk());

        getClient(adminToken).perform(delete("/api/submission/workspaceitems/" + witem.getID()))
                             .andExpect(status().isNoContent());

        getClient(adminToken).perform(get("/api/core/supervisionorders/" + idRef.get()))
                             .andExpect(status().isNotFound());

    }

    @Test
    public void installWorkspaceItemThenSupervisionOrderIsDeletedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity =
            CommunityBuilder.createCommunity(context)
                            .withName("Parent Community")
                            .build();

        Collection publications =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Publications")
                             .withEntityType("Publication")
                             .build();

        EPerson userA =
            EPersonBuilder.createEPerson(context)
                          .withCanLogin(true)
                          .withEmail("test1@email.com")
                          .withPassword(password)
                          .build();

        Group groupA =
            GroupBuilder.createGroup(context)
                        .withName("group A")
                        .addMember(userA)
                        .build();

        Group groupB =
            GroupBuilder.createGroup(context)
                        .withName("group B")
                        .addMember(eperson)
                        .build();

        WorkspaceItem witem =
            WorkspaceItemBuilder.createWorkspaceItem(context, publications)
                                .withTitle("Workspace Item 1")
                                .withIssueDate("2017-10-17")
                                .withSubject("ExtraEntry")
                                .grantLicense()
                                .build();

        context.restoreAuthSystemState();

        AtomicReference<Integer> idRefOne = new AtomicReference<>();
        AtomicReference<Integer> idRefTwo = new AtomicReference<>();
        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(post("/api/core/supervisionorders/")
                                 .param("uuid", witem.getItem().getID().toString())
                                 .param("group", groupA.getID().toString())
                                 .param("type", "EDITOR")
                                 .contentType(contentType))
                             .andExpect(status().isCreated())
                             .andDo(result -> idRefOne
                                 .set(read(result.getResponse().getContentAsString(), "$.id")));

        getClient(adminToken).perform(post("/api/core/supervisionorders/")
                                 .param("uuid", witem.getItem().getID().toString())
                                 .param("group", groupB.getID().toString())
                                 .param("type", "EDITOR")
                                 .contentType(contentType))
                             .andExpect(status().isCreated())
                             .andDo(result -> idRefTwo
                                 .set(read(result.getResponse().getContentAsString(), "$.id")));

        getClient(adminToken).perform(get("/api/core/supervisionorders/" + idRefOne.get()))
                             .andExpect(status().isOk());

        getClient(adminToken).perform(get("/api/core/supervisionorders/" + idRefTwo.get()))
                             .andExpect(status().isOk());

        // install item then supervision orders will be deleted
        context.turnOffAuthorisationSystem();
        installItemService.installItem(context, context.reloadEntity(witem));
        context.restoreAuthSystemState();

        getClient(adminToken).perform(get("/api/core/supervisionorders/" + idRefOne.get()))
                             .andExpect(status().isNotFound());

        getClient(adminToken).perform(get("/api/core/supervisionorders/" + idRefTwo.get()))
                             .andExpect(status().isNotFound());

    }

    @Test
    public void createOnArchivedAndWithdrawnItemsNotAllowedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity =
            CommunityBuilder.createCommunity(context)
                            .withName("Parent Community")
                            .build();

        Collection publications =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Publications")
                             .withEntityType("Publication")
                             .build();

        EPerson userA =
            EPersonBuilder.createEPerson(context)
                          .withCanLogin(true)
                          .withEmail("test1@email.com")
                          .withPassword(password)
                          .build();

        Group groupA =
            GroupBuilder.createGroup(context)
                        .withName("group A")
                        .addMember(userA)
                        .build();

        Group groupB =
            GroupBuilder.createGroup(context)
                        .withName("group B")
                        .addMember(eperson)
                        .build();

        Item item = ItemBuilder.createItem(context, publications)
                               .withTitle("Workspace Item 1")
                               .withIssueDate("2017-10-17")
                               .withSubject("ExtraEntry")
                               .grantLicense()
                               .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(post("/api/core/supervisionorders/")
                                          .param("uuid",item.getID().toString())
                                          .param("group", groupA.getID().toString())
                                          .param("type", "EDITOR")
                                          .contentType(contentType))
                             .andExpect(status().isUnprocessableEntity());

        getClient(adminToken).perform(post("/api/core/supervisionorders/")
                                          .param("uuid", item.getID().toString())
                                          .param("group", groupB.getID().toString())
                                          .param("type", "OBSERVER")
                                          .contentType(contentType))
                             .andExpect(status().isUnprocessableEntity());


        // withdraw the item, supervision order creation still not possible

        String patchBody = getPatchContent(List.of(new ReplaceOperation("/withdrawn", true)));

        getClient(adminToken).perform(patch("/api/core/items/" + item.getID())
                                          .content(patchBody)
                                          .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk());

        getClient(adminToken).perform(post("/api/core/supervisionorders/")
                                          .param("uuid",item.getID().toString())
                                          .param("group", groupA.getID().toString())
                                          .param("type", "EDITOR")
                                          .contentType(contentType))
                             .andExpect(status().isUnprocessableEntity());

        getClient(adminToken).perform(post("/api/core/supervisionorders/")
                                          .param("uuid", item.getID().toString())
                                          .param("group", groupB.getID().toString())
                                          .param("type", "OBSERVER")
                                          .contentType(contentType))
                             .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void createSupervisionOnWorkspaceThenSubmitToWorkflowTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity =
            CommunityBuilder.createCommunity(context)
                            .withName("Parent Community")
                            .build();

        EPerson reviewer =
            EPersonBuilder.createEPerson(context)
                          .withEmail("reviewer1@example.com")
                          .withPassword(password)
                          .build();

        Collection collection =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Collection 1")
                             .withWorkflowGroup("reviewer", reviewer)
                             .build();

        EPerson userA =
            EPersonBuilder.createEPerson(context)
                          .withCanLogin(true)
                          .withEmail("userA@test.com")
                          .withPassword(password)
                          .build();

        Group groupA =
            GroupBuilder.createGroup(context)
                        .withName("group A")
                        .addMember(userA)
                        .build();

        InputStream pdf = getClass().getResourceAsStream("simple-article.pdf");

        WorkspaceItem witem =
            WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                .withTitle("Workspace Item 1")
                                .withIssueDate("2017-10-17")
                                .withSubject("ExtraEntry")
                                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf)
                                .grantLicense()
                                .build();

        context.restoreAuthSystemState();

        // create a supervision order on workspaceItem to groupA
        getClient(getAuthToken(admin.getEmail(), password))
            .perform(post("/api/core/supervisionorders/")
                         .param("uuid", witem.getItem().getID().toString())
                         .param("group", groupA.getID().toString())
                         .param("type", "EDITOR")
                         .contentType(contentType))
            .andExpect(status().isCreated());

        String authTokenA = getAuthToken(userA.getEmail(), password);

        // a simple patch to update an existent metadata
        String patchBody =
            getPatchContent(List.of(
                new ReplaceOperation("/sections/traditionalpageone/dc.title/0", Map.of("value", "New Title"))
            ));

        // supervisor update the title of the workspaceItem
        getClient(authTokenA).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                                          .content(patchBody)
                                          .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.errors").doesNotExist())
                             .andExpect(jsonPath("$",
                                                 // check the new title and untouched values
                                                 Matchers.is(
                                                     WorkspaceItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                                         witem, "New Title", "2017-10-17", "ExtraEntry"
                                                     ))));

        // supervisor check that title has been updated
        getClient(authTokenA).perform(get("/api/submission/workspaceitems/" + witem.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.errors").doesNotExist())
                             .andExpect(jsonPath("$", Matchers.is(
                                 WorkspaceItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                     witem, "New Title", "2017-10-17", "ExtraEntry"
                                 ))));

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {
            String adminToken = getAuthToken(admin.getEmail(), password);
            String reviewerToken = getAuthToken(reviewer.getEmail(), password);

            // submit the workspaceitem to start the workflow
            getClient(adminToken).perform(post(BASE_REST_SERVER_URL + "/api/workflow/workflowitems")
                                              .content("/api/submission/workspaceitems/" + witem.getID())
                                              .contentType(textUriContentType))
                                 .andExpect(status().isCreated())
                                 .andDo(result ->
                                            idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            // supervisor can read the workflowitem
            getClient(authTokenA)
                .perform(get("/api/workflow/workflowitems/" + idRef.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssued(
                    null, "New Title", "2017-10-17"))))
                .andExpect(jsonPath("$.sections.defaultAC.discoverable", is(true)));

            // reviewer can read the workflowitem
            getClient(reviewerToken)
                .perform(get("/api/workflow/workflowitems/" + idRef.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssued(
                    null, "New Title", "2017-10-17"))))
                .andExpect(jsonPath("$.sections.defaultAC.discoverable", is(true)));

            // a simple patch to update an existent metadata
            String patchBodyTwo =
                getPatchContent(List.of(new ReplaceOperation("/metadata/dc.title", Map.of("value", "edited title"))));

            // supervisor can't edit item in workflow
            getClient(authTokenA).perform(patch("/api/core/items/" + witem.getItem().getID())
                                              .content(patchBodyTwo)
                                              .contentType(contentType))
                                 .andExpect(status().isForbidden());

            // supervisor can't edit the workflow item
            getClient(authTokenA).perform(patch("/api/workflow/workflowitems/" + idRef.get())
                                              .content(patchBody)
                                              .contentType(contentType))
                                 .andExpect(status().isUnprocessableEntity());

        } finally {
            if (idRef.get() != null) {
                WorkflowItemBuilder.deleteWorkflowItem(idRef.get());
            }
        }
    }

    @Test
    public void supervisionOrderAddedToWorkflowItemThenSentBackToWorkspace() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withEntityType("Publication")
                                                 .withWorkflowGroup("reviewer", admin).build();

        WorkflowItem workflowItem = WorkflowItemBuilder.createWorkflowItem(context, collection)
                                                       .withSubmitter(admin)
                                                       .withTitle("this is the title")
                                                       .withIssueDate("1982-12-17")
                                                       .grantLicense().build();


        EPerson userA =
            EPersonBuilder.createEPerson(context)
                          .withCanLogin(true)
                          .withEmail("userA@test.com")
                          .withPassword(password)
                          .build();

        Group groupA =
            GroupBuilder.createGroup(context)
                        .withName("group A")
                        .addMember(userA)
                        .build();
        context.restoreAuthSystemState();

        getClient(getAuthToken(admin.getEmail(), password))
            .perform(post("/api/core/supervisionorders/")
                         .param("uuid", workflowItem.getItem().getID().toString())
                         .param("group", groupA.getID().toString())
                         .param("type", "EDITOR")
                         .contentType(contentType))
            .andExpect(status().isCreated());

        String workflowItemPatchBody =
            getPatchContent(List.of(new ReplaceOperation("/metadata/dc.title", Map.of("value", "edited title"))));

        String authTokenA = getAuthToken(userA.getEmail(), password);
        // supervisor can't edit item in workflow
        getClient(authTokenA).perform(patch("/api/core/items/" + workflowItem.getItem().getID())
                                          .content(workflowItemPatchBody)
                                          .contentType(contentType))
                             .andExpect(status().isForbidden());

        AtomicReference<Integer> idRef = new AtomicReference<>();

        try {
            // Delete the workflowitem
            String adminToken = getAuthToken(admin.getEmail(), password);

            getClient(adminToken).perform(delete("/api/workflow/workflowitems/" + workflowItem.getID()))
                                 .andExpect(status().is(204));

            getClient(adminToken).perform(get("/api/submission/workspaceitems/search/findBySubmitter")
                                              .param("uuid", admin.getID().toString()))
                                 .andExpect(status().isOk())
                                 .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(),
                                                                 "$._embedded.workspaceitems[0].id")));

            String workspaceItemPatchBody = getPatchContent(List.of(
                new ReplaceOperation("/sections/traditionalpageone/dc.title/0", Map.of("value", "New Title"))
            ));

            getClient(authTokenA).perform(patch("/api/submission/workspaceitems/" + idRef.get())
                                              .content(workspaceItemPatchBody)
                                              .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                                 .andExpect(status().isOk());
        } finally {
            Integer id = idRef.get();
            if (Objects.nonNull(id)) {
                WorkspaceItemBuilder.deleteWorkspaceItem(id);
            }
        }
    }
}