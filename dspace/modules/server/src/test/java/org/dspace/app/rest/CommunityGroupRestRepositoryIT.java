package org.dspace.app.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;

import org.dspace.app.rest.matcher.CommunityGroupMatcher;
import org.dspace.app.rest.matcher.CommunityMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Community;
import org.dspace.content.CommunityGroup;
import org.dspace.core.Context;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Integration test for UnitRestRepository
 */
public class CommunityGroupRestRepositoryIT extends AbstractControllerIntegrationTest {

        @Before
        public void setup() {
        }

        /**
         * Creates a unit with the given name and group members using UnitBuilder
         *
         * @param name             the name of the Unit to create
         * @param communityGroupId an (possibly empty) list of Groups to associate with
         *                         the unit.
         * @return the create Unit
         * @throws SQLException if a database error occurs.
         */
        private Community createCommunityWithGroup(String name, int communityGroupId) throws SQLException {
                Context localContext = new Context();
                localContext.turnOffAuthorisationSystem();

                CommunityBuilder commBuilder = CommunityBuilder.createCommunity(localContext)
                                .withName(name)
                                .withCommunityGroup(communityGroupId);
                Community community = commBuilder.build();

                localContext.complete();
                localContext.restoreAuthSystemState();
                return community;
        }

        @Test
        public void findAllTest() throws Exception {

                // When we call the root endpoint
                getClient().perform(get("/api/core/communitygroups"))
                                // The status has to be 200 OK
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(contentType))
                                // The array of communitygroups should have a size 2
                                .andExpect(jsonPath("$._embedded.communitygroups", hasSize(2)))
                                // The created units should be listed
                                .andExpect(jsonPath("$._embedded.communitygroups", Matchers.containsInAnyOrder(
                                                CommunityGroupMatcher.matchCommunityGroupWithId(CommunityGroup.FACULTY),
                                                CommunityGroupMatcher.matchCommunityGroupWithId(CommunityGroup.UM))));
        }

        @Ignore("The endpoint depends on Solr indexing/search")
        @Test
        public void findCommunityGroupCommunities() throws Exception {

                Community facultyCommunity = createCommunityWithGroup("testcommunity1", CommunityGroup.FACULTY);
                Community umCommunity = createCommunityWithGroup("testcommunity2", CommunityGroup.UM);

                String token = getAuthToken(admin.getEmail(), password);

                // Check the communities in faculty communitygroup
                getClient(token).perform(
                                get("/api/core/communitygroups/" + String.valueOf(CommunityGroup.FACULTY)
                                                + "/communities"))
                                // The status has to be 200 OK
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(contentType))
                                // The array of communitygroups should have a size 2
                                .andExpect(jsonPath("$._embedded.communities", hasSize(1)))
                                // The created units should be listed
                                .andExpect(jsonPath("$._embedded.communities", Matchers.contains(
                                                CommunityMatcher.matchCommunity(facultyCommunity))));

                // Check the communities in um communitygroup
                getClient(token).perform(
                                get("/api/core/communitygroups/" + String.valueOf(CommunityGroup.UM) + "/communities"))
                                // The status has to be 200 OK
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(contentType))
                                // The array of communitygroups should have a size 2
                                .andExpect(jsonPath("$._embedded.communities", hasSize(1)))
                                // The created units should be listed
                                .andExpect(jsonPath("$._embedded.communities", Matchers.contains(
                                                CommunityMatcher.matchCommunity(umCommunity))));
        }
}
