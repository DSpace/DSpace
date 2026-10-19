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

import org.dspace.app.rest.matcher.CollectionMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for {@link CommunityCollectionLinkRepository}
 */
public class CommunityCollectionLinkRepositoryIT extends AbstractControllerIntegrationTest {

    Community parentCommunity;
    Collection collection1;
    Collection collection2;
    Collection collection3;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .build();
        collection1 = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection 1")
            .build();
        collection2 = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection 2")
            .build();
        collection3 = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection 3")
            .build();
        context.commit();
        context.restoreAuthSystemState();
    }

    @Test
    public void getCollections_sortTitleASC() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/core/communities/" + parentCommunity.getID() + "/collections")
                                          .param("sort", "dc.title,ASC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections", Matchers.contains(
                CollectionMatcher.matchCollection(collection1),
                CollectionMatcher.matchCollection(collection2),
                CollectionMatcher.matchCollection(collection3)
            )));
    }

    @Test
    public void getCollections_sortTitleDESC() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/core/communities/" + parentCommunity.getID() + "/collections")
                                          .param("sort", "dc.title,DESC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections", Matchers.contains(
                CollectionMatcher.matchCollection(collection3),
                CollectionMatcher.matchCollection(collection2),
                CollectionMatcher.matchCollection(collection1)
            )));
    }

}
