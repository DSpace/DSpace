package org.dspace.app.rest;

import org.dspace.app.rest.builder.GroupBuilder;
import org.dspace.app.rest.matcher.GroupMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.eperson.Group;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class GroupRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAllTest() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/api/eperson/groups"))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                //The array of groups should have a size 2
                .andExpect(jsonPath("$._embedded.groups", hasSize(2)))
                // The default groups should consist of "Anonymous" and "Anonymous"
                .andExpect(jsonPath("$._embedded.groups", Matchers.containsInAnyOrder(
                        GroupMatcher.matchGroupWithName("Administrator"),
                        GroupMatcher.matchGroupWithName("Anonymous")
                )))
        ;
    }

    @Test
    public void findAllPaginationTest() throws Exception{
        context.turnOffAuthorisationSystem();
        getClient().perform(get("/api/eperson/groups"))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(1)))
                .andExpect(jsonPath("$.page.number", is(0)));
    }


    @Test
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();

        String testGroupName = "Test group";
        Group group = GroupBuilder.createGroup(context)
                .withName(testGroupName)
                .build();

        String generatedGroupId = group.getID().toString();
        String groupIdCall = "/api/eperson/groups/" + generatedGroupId;
        getClient().perform(get(groupIdCall))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$",Matchers.is(
                        GroupMatcher.matchGroupEntry(group.getID(), group.getName())
                )))
        ;
        getClient().perform(get("/api/eperson/groups"))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.totalElements", is(3)));

    }

    @Test
    public void findOneRelsTest() throws Exception{
        context.turnOffAuthorisationSystem();

        Group group = GroupBuilder.createGroup(context)
                .withName("Group1")
                .build();

        Group group2 = GroupBuilder.createGroup(context)
                .withName("Group2")
                .build();

        getClient().perform(get("/api/eperson/groups/" + group2.getID()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", Matchers.is(
                        GroupMatcher.matchGroupEntry(group2.getID(), group2.getName())
                )))
                .andExpect(jsonPath("$", Matchers.not(
                        Matchers.is(
                                GroupMatcher.matchGroupEntry(group.getID(), group.getName())
                        )
                )))
                .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/eperson/groups/" + group2.getID())));
    }

}
