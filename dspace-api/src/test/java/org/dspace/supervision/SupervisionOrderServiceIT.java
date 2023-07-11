/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.supervision;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.supervision.factory.SupervisionOrderServiceFactory;
import org.dspace.supervision.service.SupervisionOrderService;
import org.junit.Test;

/**
 * Unit tests for the {@link SupervisionOrderService}
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
public class SupervisionOrderServiceIT extends AbstractIntegrationTestWithDatabase {

    protected SupervisionOrderService supervisionOrderService =
        SupervisionOrderServiceFactory.getInstance().getSupervisionOrderService();

    @Test
    public void createSupervisionOrderTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity =
            CommunityBuilder.createCommunity(context)
                            .withName("parent community")
                            .build();

        Collection collection =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("collection")
                             .withEntityType("Publication")
                             .build();

        WorkspaceItem workspaceItem =
            WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                .withTitle("workspace item")
                                .withIssueDate("2023-01-24")
                                .grantLicense()
                                .build();

        Item item = workspaceItem.getItem();

        EPerson userA =
            EPersonBuilder.createEPerson(context)
                          .withCanLogin(true)
                          .withEmail("userA@example.org")
                          .build();

        EPerson userB =
            EPersonBuilder.createEPerson(context)
                          .withCanLogin(true)
                          .withEmail("userB@example.org")
                          .build();

        Group groupA = GroupBuilder.createGroup(context)
                                   .withName("group A")
                                   .addMember(userA)
                                   .build();

        Group groupB = GroupBuilder.createGroup(context)
                                   .withName("group B")
                                   .addMember(userB)
                                   .build();

        SupervisionOrder supervisionOrderOne =
            supervisionOrderService.create(context, item, groupA);

        SupervisionOrder supervisionOrderTwo =
            supervisionOrderService.create(context, item, groupB);

        context.restoreAuthSystemState();

        assertThat(supervisionOrderOne, notNullValue());
        assertThat(supervisionOrderOne.getItem().getID(), is(item.getID()));
        assertThat(supervisionOrderOne.getGroup().getID(), is(groupA.getID()));

        assertThat(supervisionOrderTwo, notNullValue());
        assertThat(supervisionOrderTwo.getItem().getID(), is(item.getID()));
        assertThat(supervisionOrderTwo.getGroup().getID(), is(groupB.getID()));

    }

    @Test
    public void findSupervisionOrderTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity =
            CommunityBuilder.createCommunity(context)
                            .withName("parent community")
                            .build();

        Collection collection =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("collection")
                             .withEntityType("Publication")
                             .build();

        WorkspaceItem workspaceItem =
            WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                .withTitle("workspace item")
                                .withIssueDate("2023-01-24")
                                .grantLicense()
                                .build();

        EPerson userA =
            EPersonBuilder.createEPerson(context)
                          .withCanLogin(true)
                          .withEmail("userA@example.org")
                          .build();

        Group groupA = GroupBuilder.createGroup(context)
                                   .withName("group A")
                                   .addMember(userA)
                                   .build();

        SupervisionOrder supervisionOrderOne =
            supervisionOrderService.create(context, workspaceItem.getItem(), groupA);

        context.restoreAuthSystemState();

        SupervisionOrder supervisionOrder =
            supervisionOrderService.find(context, supervisionOrderOne.getID());

        assertThat(supervisionOrder, notNullValue());
        assertThat(supervisionOrder.getID(), is(supervisionOrderOne.getID()));

        assertThat(supervisionOrder.getGroup().getID(),
            is(supervisionOrderOne.getGroup().getID()));

        assertThat(supervisionOrder.getItem().getID(),
            is(supervisionOrderOne.getItem().getID()));

    }

    @Test
    public void findAllSupervisionOrdersTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity =
            CommunityBuilder.createCommunity(context)
                            .withName("parent community")
                            .build();

        Collection collection =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("collection")
                             .withEntityType("Publication")
                             .build();

        WorkspaceItem workspaceItem =
            WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                .withTitle("workspace item")
                                .withIssueDate("2023-01-24")
                                .grantLicense()
                                .build();

        WorkspaceItem workspaceItemTwo =
            WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                .withTitle("workspace item two")
                                .withIssueDate("2023-01-25")
                                .grantLicense()
                                .build();

        EPerson userA =
            EPersonBuilder.createEPerson(context)
                          .withCanLogin(true)
                          .withEmail("userA@example.org")
                          .build();

        EPerson userB =
            EPersonBuilder.createEPerson(context)
                          .withCanLogin(true)
                          .withEmail("userB@example.org")
                          .build();

        Group groupA = GroupBuilder.createGroup(context)
                                   .withName("group A")
                                   .addMember(userA)
                                   .build();

        Group groupB = GroupBuilder.createGroup(context)
                                   .withName("group B")
                                   .addMember(userB)
                                   .build();

        supervisionOrderService.create(context, workspaceItem.getItem(), groupA);
        supervisionOrderService.create(context, workspaceItem.getItem(), groupB);
        supervisionOrderService.create(context, workspaceItemTwo.getItem(), groupA);

        context.restoreAuthSystemState();

        assertThat(supervisionOrderService.findAll(context), hasSize(3));
    }

    @Test
    public void findSupervisionOrderByItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity =
            CommunityBuilder.createCommunity(context)
                            .withName("parent community")
                            .build();

        Collection collection =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("collection")
                             .withEntityType("Publication")
                             .build();

        WorkspaceItem workspaceItem =
            WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                .withTitle("workspace item")
                                .withIssueDate("2023-01-24")
                                .grantLicense()
                                .build();

        WorkspaceItem workspaceItemTwo =
            WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                .withTitle("workspace item two")
                                .withIssueDate("2023-01-25")
                                .grantLicense()
                                .build();

        EPerson userA =
            EPersonBuilder.createEPerson(context)
                          .withCanLogin(true)
                          .withEmail("userA@example.org")
                          .build();

        Group groupA = GroupBuilder.createGroup(context)
                                   .withName("group A")
                                   .addMember(userA)
                                   .build();

        Group groupB = GroupBuilder.createGroup(context)
                                   .withName("group B")
                                   .addMember(eperson)
                                   .build();

        supervisionOrderService.create(context, workspaceItem.getItem(), groupA);
        supervisionOrderService.create(context, workspaceItem.getItem(), groupB);
        supervisionOrderService.create(context, workspaceItemTwo.getItem(), groupA);

        context.restoreAuthSystemState();

        assertThat(supervisionOrderService.findByItem(context, workspaceItem.getItem()), hasSize(2));
        assertThat(supervisionOrderService.findByItem(context, workspaceItemTwo.getItem()), hasSize(1));

    }

    @Test
    public void findSupervisionOrderByItemAndGroupTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity =
            CommunityBuilder.createCommunity(context)
                            .withName("parent community")
                            .build();

        Collection collection =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("collection")
                             .withEntityType("Publication")
                             .build();

        WorkspaceItem workspaceItem =
            WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                .withTitle("workspace item")
                                .withIssueDate("2023-01-24")
                                .grantLicense()
                                .build();

        Item item = workspaceItem.getItem();

        EPerson userA =
            EPersonBuilder.createEPerson(context)
                          .withCanLogin(true)
                          .withEmail("userA@example.org")
                          .build();

        Group groupA = GroupBuilder.createGroup(context)
                                   .withName("group A")
                                   .addMember(userA)
                                   .build();

        Group groupB = GroupBuilder.createGroup(context)
                                   .withName("group B")
                                   .addMember(eperson)
                                   .build();

        supervisionOrderService.create(context, item, groupA);

        context.restoreAuthSystemState();

        SupervisionOrder supervisionOrderA =
            supervisionOrderService.findByItemAndGroup(context, item, groupA);

        assertThat(supervisionOrderA, notNullValue());
        assertThat(supervisionOrderA.getItem().getID(), is(item.getID()));
        assertThat(supervisionOrderA.getGroup().getID(), is(groupA.getID()));

        // no supervision order on item and groupB
        assertThat(supervisionOrderService.findByItemAndGroup(context, item, groupB), nullValue());

    }

    @Test
    public void isSupervisorTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity =
            CommunityBuilder.createCommunity(context)
                            .withName("parent community")
                            .build();

        Collection collection =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("collection")
                             .withEntityType("Publication")
                             .build();

        WorkspaceItem workspaceItem =
            WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                .withTitle("workspace item")
                                .withIssueDate("2023-01-24")
                                .grantLicense()
                                .build();

        EPerson userA =
            EPersonBuilder.createEPerson(context)
                          .withCanLogin(true)
                          .withEmail("userA@example.org")
                          .build();

        EPerson userB =
            EPersonBuilder.createEPerson(context)
                          .withCanLogin(true)
                          .withEmail("userB@example.org")
                          .build();

        Group groupA = GroupBuilder.createGroup(context)
                                   .withName("group A")
                                   .addMember(userA)
                                   .build();

        GroupBuilder.createGroup(context)
                    .withName("group B")
                    .addMember(userB)
                    .build();

        supervisionOrderService.create(context, workspaceItem.getItem(), groupA);

        context.restoreAuthSystemState();

        assertThat(supervisionOrderService.isSupervisor(
            context, userA, workspaceItem.getItem()), is(true));

        // userB is not a supervisor on workspace Item
        assertThat(supervisionOrderService.isSupervisor(
            context, userB, workspaceItem.getItem()), is(false));
    }

}
