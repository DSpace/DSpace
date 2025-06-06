/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.junit.Test;

public class DiscoverRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void testFindAdminAuthorizedCollection() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("Test Community")
                                              .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                                                .withName("Test Collection")
                                                .build();

        EPerson communityAdmin = EPersonBuilder.createEPerson(context)
                                               .withEmail("commadmin@example.com")
                                               .withPassword(password)
                                               .build();

        Group commAdminGroup = GroupBuilder.createCommunityAdminGroup(context, community)
                                           .addMember(communityAdmin)
                                           .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(communityAdmin.getEmail(), password);

        getClient(token).perform(get("/api/discover/search/objects")
                            .param("dsoTypes", "Collection"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]"
                        + "._embedded.indexableObject.name",
                        is(collection.getName())));
    }

    @Test
    public void testFindAdminAuthorizedCommunity() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("Test Community")
                                              .build();

        EPerson communityAdmin = EPersonBuilder.createEPerson(context)
                                               .withEmail("commadmin@example.com")
                                               .withPassword(password)
                                               .build();

        Group commAdminGroup = GroupBuilder.createCommunityAdminGroup(context, community)
                                           .addMember(communityAdmin)
                                           .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(communityAdmin.getEmail(), password);

        getClient(token).perform(get("/api/discover/search/objects")
                            .param("dsoTypes", "Community"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]"
                        + "._embedded.indexableObject.name",
                        is(community.getName())));
    }

    @Test
    public void testUnauthorizedUserCannotSeeCollectionsOrCommunities() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("Test Community")
                                              .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                                                .withName("Test Collection")
                                                .build();

        EPerson normalUser = EPersonBuilder.createEPerson(context)
                                           .withEmail("user@example.com")
                                           .withPassword(password)
                                           .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(normalUser.getEmail(), password);

        getClient(token).perform(get("/api/discover/search/objects")
                            .param("dsoTypes", "Collection"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.collections").doesNotExist());

        getClient(token).perform(get("/api/discover/search/objects")
                            .param("dsoTypes", "Community"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.communities").doesNotExist());
    }
}


