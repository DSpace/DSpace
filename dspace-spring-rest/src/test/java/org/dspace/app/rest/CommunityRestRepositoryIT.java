/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.matcher.CommunityMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CommunityRestRepositoryIT extends AbstractControllerIntegrationTest{

    @Test
    public void findAllTest() throws Exception{
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        getClient().perform(get("/api/core/communities"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.communities", Matchers.containsInAnyOrder(
                        CommunityMatcher.matchCommunityEntry(parentCommunity.getName(), parentCommunity.getID(), parentCommunity.getHandle()),
                        CommunityMatcher.matchCommunityEntry(child1.getName(), child1.getID(), child1.getHandle())
                )))
                .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/communities")))
        ;
    }

    @Test
    public void findAllPaginationTest() throws Exception{
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        getClient().perform(get("/api/core/communities")
                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.communities", Matchers.contains(
                        CommunityMatcher.matchCommunityEntry(parentCommunity.getName(), parentCommunity.getID(), parentCommunity.getHandle())
                )))
                .andExpect(jsonPath("$._embedded.communities", Matchers.not(
                        Matchers.contains(
                                CommunityMatcher.matchCommunityEntry(child1.getName(), child1.getID(), child1.getHandle())
                        )
                )))
                .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/communities")))
        ;

        getClient().perform(get("/api/core/communities")
                .param("size", "1")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.communities", Matchers.contains(
                        CommunityMatcher.matchCommunityEntry(child1.getName(), child1.getID(), child1.getHandle())
                )))
                .andExpect(jsonPath("$._embedded.communities", Matchers.not(
                        Matchers.contains(
                                CommunityMatcher.matchCommunityEntry(parentCommunity.getName(), parentCommunity.getID(), parentCommunity.getHandle())
                        )
                )))
                .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/communities")))
        ;
    }

    @Test
    public void findOneTest() throws Exception{
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        getClient().perform(get("/api/core/communities/" + parentCommunity.getID().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", Matchers.is(
                        CommunityMatcher.matchCommunityEntry(parentCommunity.getName(), parentCommunity.getID(), parentCommunity.getHandle())
                )))
                .andExpect(jsonPath("$", Matchers.not(
                        Matchers.is(
                                CommunityMatcher.matchCommunityEntry(child1.getName(), child1.getID(), child1.getHandle())
                        )
                )))
                .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/communities")))
        ;
    }

    @Test
    public void findOneRelsTest() throws Exception{
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .withLogo("ThisIsSomeDummyText")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        getClient().perform(get("/api/core/communities/" + parentCommunity.getID().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", Matchers.is(
                        CommunityMatcher.matchCommunityEntry(parentCommunity.getName(), parentCommunity.getID(), parentCommunity.getHandle())
                )))
                .andExpect(jsonPath("$", Matchers.not(
                        Matchers.is(
                                CommunityMatcher.matchCommunityEntry(child1.getName(), child1.getID(), child1.getHandle())
                        )
                )))
                .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/communities/" + parentCommunity.getID().toString())))
                .andExpect(jsonPath("$._links.logo.href", Matchers.containsString("/api/core/communities/" + parentCommunity.getID().toString() + "/logo")))
                .andExpect(jsonPath("$._links.collections.href", Matchers.containsString("/api/core/communities/" + parentCommunity.getID().toString() + "/collections")))
        ;

        getClient().perform(get("/api/core/communities/" + parentCommunity.getID().toString() + "/logo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType));

        getClient().perform(get("/api/core/communities/" + child1.getID().toString() + "/logo"))
                .andExpect(status().isOk());

        getClient().perform(get("/api/core/communities/" + parentCommunity.getID().toString() + "/collections"))
                .andExpect(status().isOk());

        getClient().perform(get("/api/core/communities/" + child1.getID().toString() + "/collections"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType));
    }

    //TODO /api/core/communities/search/top

    //TODO /api/core/communities/search/subCommunities

}
