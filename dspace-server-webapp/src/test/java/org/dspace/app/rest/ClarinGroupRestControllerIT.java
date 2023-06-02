/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.Assert.assertTrue;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.PoolTaskBuilder;
import org.dspace.content.Collection;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test to test the /api/clarin/eperson/groups/* endpoints.
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
public class ClarinGroupRestControllerIT extends AbstractControllerIntegrationTest {
    Collection collection;
    @Autowired
    PoolTaskService poolTaskService;

    @Before
    public void setup() {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("test").build();
        collection = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        context.restoreAuthSystemState();
    }

    @Test
    public void addChildGroupTest() throws Exception {
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        context.turnOffAuthorisationSystem();
        EPerson member = EPersonBuilder.createEPerson(context).build();
        Group parentGroup = GroupBuilder.createGroup(context).build();
        Group parentGroupWithPreviousSubgroup = GroupBuilder.createGroup(context).build();
        Group subGroup = GroupBuilder.createGroup(context).withParent(parentGroupWithPreviousSubgroup)
                .addMember(eperson).build();
        Group childGroup1 = GroupBuilder.createGroup(context).addMember(member).build();
        Group childGroup2 = GroupBuilder.createGroup(context).build();
        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(
                post("/api/clarin/eperson/groups/" + parentGroup.getID() + "/subgroups")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(REST_SERVER_URL + "eperson/groups/" + childGroup1.getID() + "/\n"
                                + REST_SERVER_URL + "eperson/groups/" + childGroup2.getID()
                        )
        ).andExpect(status().isOk());
        getClient(authToken).perform(
                post("/api/clarin/eperson/groups/" + parentGroupWithPreviousSubgroup.getID() + "/subgroups")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(REST_SERVER_URL + "eperson/groups/" + childGroup1.getID() + "/\n"
                                + REST_SERVER_URL + "eperson/groups/" + childGroup2.getID()
                        )
        ).andExpect(status().isOk());

        parentGroup = context.reloadEntity(parentGroup);
        parentGroupWithPreviousSubgroup = context.reloadEntity(parentGroupWithPreviousSubgroup);
        subGroup = context.reloadEntity(subGroup);
        childGroup1 = context.reloadEntity(childGroup1);
        childGroup2 = context.reloadEntity(childGroup2);

        assertTrue(
                groupService.isMember(parentGroup, childGroup1)
        );
        assertTrue(
                groupService.isMember(parentGroup, childGroup2)
        );
        // member of the added groups should be member of the group now
        assertTrue(
                groupService.isMember(context, member, parentGroup)
        );

        // verify that the previous subGroup is still here
        assertTrue(
                groupService.isMember(parentGroupWithPreviousSubgroup, childGroup1)
        );
        assertTrue(
                groupService.isMember(parentGroupWithPreviousSubgroup, childGroup2)
        );
        assertTrue(
                groupService.isMember(parentGroupWithPreviousSubgroup, subGroup)
        );
        // and that both the member of the added groups than existing ones are still member
        assertTrue(
                groupService.isMember(context, member, parentGroupWithPreviousSubgroup)
        );
        assertTrue(
                groupService.isMember(context, eperson, parentGroupWithPreviousSubgroup)
        );
    }

    @Test
    public void addTasklistitemMembersTest() throws Exception {
        context.turnOffAuthorisationSystem();
        //create eperson, which will be added to the group
        EPerson ePerson = EPersonBuilder.createEPerson(context)
                .withEmail("eperson@example.com")
                .withPassword("dspace")
                .build();
        //create pooltask with workflowitem
        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer1@example.com")
                .withPassword(password).build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .withWorkflowGroup(1, reviewer1, admin).build();
        PoolTask poolTask = PoolTaskBuilder.createPoolTask(context, col1, reviewer1)
                .withTitle("Test Metaphysics")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .withSubject("ExtraEntry").build();
        XmlWorkflowItem wf = poolTask.getWorkflowItem();
        context.restoreAuthSystemState();

        //add eperson to group connected with workflowitem
        ObjectMapper mapper = new ObjectMapper();
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/clarin/eperson/groups/tasklistitem")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .param("workflowitem_id", wf.getID().toString())
                        .param("epersonUUID", ePerson.getID().toString()))
                .andExpect(status().isOk());

        //control if eperson is added to correct group
        poolTask = poolTaskService.find(context, poolTask.getID());
        assertTrue(poolTask.getGroup().getMembers().contains(ePerson));
    }
}
