package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.test.AbstractControllerIntegrationTest.REST_SERVER_URL;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.exception.UnitNameNotProvidedException;
import org.dspace.app.rest.matcher.CommunityGroupMatcher;
import org.dspace.app.rest.matcher.CommunityMatcher;
import org.dspace.app.rest.matcher.HalMatcher;
import org.dspace.app.rest.matcher.UnitMatcher;
import org.dspace.app.rest.model.UnitRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.UnitBuilder;
import org.dspace.content.Community;
import org.dspace.content.CommunityGroup;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.Group;
import org.dspace.eperson.Unit;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.UnitService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Integration test for UnitRestRepository
 */
public class CommunityGroupRestRepositoryIT extends AbstractControllerIntegrationTest {
        @Autowired
        private ConfigurationService configurationService;

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

                String token = getAuthToken(admin.getEmail(), password);

                // When we call the root endpoint
                getClient(token).perform(get("/api/communitygroups"))
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

        @Test
        public void findCommunityGroupCommunities() throws Exception {

                Community facultyCommunity = createCommunityWithGroup("testcommunity1", CommunityGroup.FACULTY);
                Community umCommunity = createCommunityWithGroup("testcommunity2", CommunityGroup.UM);

                String token = getAuthToken(admin.getEmail(), password);

                // Check the communities in faculty communitygroup
                getClient(token).perform(
                                get("/api/communitygroups/" + String.valueOf(CommunityGroup.FACULTY) + "/communities"))
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
                                get("/api/communitygroups/" + String.valueOf(CommunityGroup.UM) + "/communities"))
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
