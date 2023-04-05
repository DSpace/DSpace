/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static java.util.UUID.randomUUID;
import static org.dspace.builder.ResourcePolicyBuilder.createResourcePolicy;
import static org.dspace.core.Constants.WRITE;
import static org.junit.Assert.assertEquals;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.dspace.app.rest.matcher.CommunityGroupMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Community;
import org.dspace.content.CommunityGroup;
import org.junit.Before;
import org.junit.Test;

public class CommunityCommunityGroupControllerIT extends AbstractControllerIntegrationTest {

        private String adminAuthToken;
        private Community community;
        private int defaultGroupID = CommunityGroup.FACULTY;
        private int umGroupID = CommunityGroup.UM;

        @Before
        public void setup() throws Exception {
                context.turnOffAuthorisationSystem();
                community = CommunityBuilder.createCommunity(context)
                                .withName("Test Community")
                                .withCommunityGroup(defaultGroupID)
                                .build();
                adminAuthToken = getAuthToken(admin.getEmail(), password);
                context.restoreAuthSystemState();
        }

        @Test
        public void getCommunityGroup() throws Exception {

                getClient()
                                .perform(get(getCommunityCommunityGroupUrl(community.getID())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$",
                                                CommunityGroupMatcher.matchCommunityGroupWithId(defaultGroupID)));
                ;
        }

        @Test
        public void updateCommunityGroupBadRequest() throws Exception {

                getClient(adminAuthToken).perform(
                                put(getCommunityCommunityGroupUrl(community.getID()))
                                                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                                .content(getCommunityGroupUrl(-1)))
                                .andExpect(status().isBadRequest());

                getClient(adminAuthToken).perform(
                                put(getCommunityCommunityGroupUrl(community.getID()))
                                                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                                .content(getCommunityGroupUrl(defaultGroupID) + "\n"
                                                                + getCommunityGroupUrl(umGroupID)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        public void updateCommunityGroupNotFound() throws Exception {

                getClient(adminAuthToken).perform(
                                put("/api/core/communities/" + randomUUID() + "/communityGroup")
                                                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                                .content(getCommunityGroupUrl(defaultGroupID)))
                                .andExpect(status().isNotFound());
        }

        @Test
        public void updateCommunityGroupUnauthorized() throws Exception {

                getClient().perform(
                                put(getCommunityCommunityGroupUrl(community.getID()))
                                                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                                .content(getCommunityGroupUrl(umGroupID)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        public void updateCommunityGroupForbidden() throws Exception {

                getClient(getAuthToken(eperson.getEmail(), password)).perform(
                                put(getCommunityCommunityGroupUrl(community.getID()))
                                                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                                .content(getCommunityGroupUrl(umGroupID)))
                                .andExpect(status().isForbidden());
        }

        @Test
        public void updateCommunityGroupEPerson() throws Exception {

                context.turnOffAuthorisationSystem();

                createResourcePolicy(context)
                                .withUser(eperson)
                                .withAction(WRITE)
                                .withDspaceObject(community)
                                .build();

                context.restoreAuthSystemState();

                getClient(getAuthToken(eperson.getEmail(), password)).perform(
                                put(getCommunityCommunityGroupUrl(community.getID()))
                                                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                                .content(getCommunityGroupUrl(umGroupID)))
                                .andExpect(status().isOk());

                community = context.reloadEntity(community);

                assertEquals(umGroupID, community.getGroupID());
        }

        @Test
        public void updateCommunityGroupAdmin() throws Exception {

                context.turnOffAuthorisationSystem();

                context.restoreAuthSystemState();

                getClient(adminAuthToken).perform(
                                put(getCommunityCommunityGroupUrl(community.getID()))
                                                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                                .content(getCommunityGroupUrl(umGroupID)))
                                .andExpect(status().isOk());

                community = context.reloadEntity(community);
                assertEquals(umGroupID, community.getGroupID());
        }

        private String getCommunityCommunityGroupUrl(UUID uuid) {
                return "/api/core/communities/" + uuid + "/communityGroup";
        }

        private String getCommunityGroupUrl(int id) {
                return REST_SERVER_URL + "/api/core/communities/" + id;
        }
}
