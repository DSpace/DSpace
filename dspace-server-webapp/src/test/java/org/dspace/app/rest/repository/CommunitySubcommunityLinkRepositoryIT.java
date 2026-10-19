/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.CommunityMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Community;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for {@link CommunitySubcommunityLinkRepository}
 */
public class CommunitySubcommunityLinkRepositoryIT extends AbstractControllerIntegrationTest {

    Community parentCommunity;
    Community subCommunity1;
    Community subCommunity2;
    Community subCommunity3;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .build();
        subCommunity1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
            .withName("Sub community 1")
            .build();
        subCommunity2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
            .withName("Sub community 2")
            .build();
        subCommunity3 = CommunityBuilder.createSubCommunity(context, parentCommunity)
            .withName("Sub community 3")
            .build();
        context.commit();
        context.restoreAuthSystemState();
    }

    @Test
    public void getSubCommunities_sortTitleASC() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/core/communities/" + parentCommunity.getID() + "/subcommunities")
                                          .param("sort", "dc.title,ASC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.subcommunities", Matchers.contains(
                CommunityMatcher.matchCommunity(subCommunity1),
                CommunityMatcher.matchCommunity(subCommunity2),
                CommunityMatcher.matchCommunity(subCommunity3)
            )));
    }

    @Test
    public void getSubCommunities_sortTitleDESC() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/core/communities/" + parentCommunity.getID() + "/subcommunities")
                                          .param("sort", "dc.title,DESC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.subcommunities", Matchers.contains(
                CommunityMatcher.matchCommunity(subCommunity3),
                CommunityMatcher.matchCommunity(subCommunity2),
                CommunityMatcher.matchCommunity(subCommunity1)
            )));
    }

}
