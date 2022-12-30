/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;
import static com.jayway.jsonpath.JsonPath.read;
import static org.dspace.app.rest.matcher.ItemMatcher.matchItemWithTitleAndDateIssued;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.ws.rs.core.MediaType;

import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.repository.SupervisionOrderRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.SupervisionOrderBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.supervision.SupervisionOrder;
import org.dspace.supervision.service.SupervisionOrderService;
import org.hamcrest.Matchers;
import org.junit.Assert;
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

        parentCommunity = CommunityBuilder.createCommunity(context)
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

        parentCommunity = CommunityBuilder.createCommunity(context)
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

        parentCommunity = CommunityBuilder.createCommunity(context)
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

        parentCommunity = CommunityBuilder.createCommunity(context)
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
    public void findOneByAdminAndItemIsWithdrawnTest() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
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

        SupervisionOrder supervisionOrder =
            SupervisionOrderBuilder.createSupervisionOrder(context, item, group)
                                   .build();

        context.restoreAuthSystemState();

        String patchBody = getPatchContent(List.of(new ReplaceOperation("/withdrawn", true)));

        String adminToken = getAuthToken(admin.getEmail(), password);
        // withdraw item
        getClient(adminToken).perform(patch("/api/core/items/" + item.getID())
                            .content(patchBody)
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.withdrawn", Matchers.is(true)))
                        .andExpect(jsonPath("$.inArchive", Matchers.is(false)));

        getClient(adminToken).perform(get("/api/core/supervisionorders/" + supervisionOrder.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", matchSuperVisionOrder(context.reloadEntity(supervisionOrder))));

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

        parentCommunity = CommunityBuilder.createCommunity(context)
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

        SupervisionOrderBuilder.createSupervisionOrder(context, item, group).build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/supervisionorders/search/byItem")
                       .param("uuid", item.getID().toString()))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findByItemByNotAdminUserTest() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
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

        parentCommunity = CommunityBuilder.createCommunity(context)
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

        parentCommunity = CommunityBuilder.createCommunity(context)
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
                                .param("type", "EDITOR")
                                .contentType(contentType))
                             .andExpect(status().isCreated());

        getClient(adminToken).perform(post("/api/core/supervisionorders/")
                                .param("uuid", itemOne.getID().toString())
                                .param("group", groupA.getID().toString())
                                .param("type", "OBSERVER")
                                .contentType(contentType))
                             .andExpect(status().isConflict());
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
                       .withIssueDate("2017-10-17")
                       .build();

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

        context.restoreAuthSystemState();

        AtomicInteger supervisionOrderIdOne = new AtomicInteger();
        AtomicInteger supervisionOrderIdTwo = new AtomicInteger();

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

        SupervisionOrder supervisionOrderOne =
            supervisionOrderService.find(context, supervisionOrderIdOne.get());

        SupervisionOrder supervisionOrderTwo =
            supervisionOrderService.find(context, supervisionOrderIdTwo.get());

        getClient(adminToken).perform(get("/api/core/supervisionorders/" + supervisionOrderOne.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$",
                                 matchSuperVisionOrder(context.reloadEntity(supervisionOrderOne))));

        getClient(adminToken).perform(get("/api/core/supervisionorders/" + supervisionOrderTwo.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$",
                                 matchSuperVisionOrder(context.reloadEntity(supervisionOrderTwo))));

        String authTokenA = getAuthToken(userA.getEmail(), password);
        String authTokenB = getAuthToken(userB.getEmail(), password);

        String patchBody = getPatchContent(List.of(
            new AddOperation("/metadata/dc.title", List.of(Map.of("value", "new title")))
        ));

        // update title of itemOne by userA is Ok
        getClient(authTokenA).perform(patch("/api/core/items/" + itemOne.getID())
                                 .content(patchBody)
                                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", is(
                                 matchItemWithTitleAndDateIssued(context.reloadEntity(itemOne),
                                     "new title", "2017-10-17")
                             )));

        // update title of itemOne by userB is Forbidden
        getClient(authTokenB).perform(patch("/api/core/items/" + itemOne.getID())
                                 .content(patchBody)
                                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isForbidden());
    }

    @Test
    public void deleteByAnonymousUserTest() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
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

        parentCommunity = CommunityBuilder.createCommunity(context)
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

        parentCommunity = CommunityBuilder.createCommunity(context)
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

}