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
import org.dspace.app.rest.matcher.CollectionMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CollectionRestRepositoryIT extends AbstractControllerIntegrationTest{


    @Test
    public void findAllTest() throws Exception {

        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = new CommunityBuilder().createCommunity(context)
                                .withName("Parent Community")
                                .build();
        Community child1 = new CommunityBuilder().createSubCommunity(context, parentCommunity)
                                .withName("Sub Community")
                                .build();
        Community child2 = new CommunityBuilder().createSubCommunity(context, parentCommunity)
                                .withName("Sub Community Two")
                                .build();
        Collection col1 = new CollectionBuilder().createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = new CollectionBuilder().createCollection(context, child2).withName("Collection 2").build();



        getClient().perform(get("/api/core/collections"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                        CollectionMatcher.matchCollectionEntry(col1.getName(), col1.getID(), col1.getHandle()),
                        CollectionMatcher.matchCollectionEntry(col2.getName(), col2.getID(), col2.getHandle())
                )));
    }

    @Test
    public void findOneCollectionTest() throws Exception {

        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = new CommunityBuilder().createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = new CommunityBuilder().createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Community child2 = new CommunityBuilder().createSubCommunity(context, parentCommunity)
                .withName("Sub Community Two")
                .build();
        Collection col1 = new CollectionBuilder().createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = new CollectionBuilder().createCollection(context, child2).withName("Collection 2").build();



        getClient().perform(get("/api/core/collections/" + col1.getID()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", Matchers.is(
                        CollectionMatcher.matchCollectionEntry(col1.getName(), col1.getID(), col1.getHandle())
                )))
                .andExpect(jsonPath("$", Matchers.not(
                        Matchers.is(
                                CollectionMatcher.matchCollectionEntry(col2.getName(), col2.getID(), col2.getHandle())
                ))));
    }

    @Test
    public void findOneCollectionRelsTest() throws Exception {

        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = new CommunityBuilder().createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = new CommunityBuilder().createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Community child2 = new CommunityBuilder().createSubCommunity(context, parentCommunity)
                .withName("Sub Community Two")
                .build();
        Collection col1 = new CollectionBuilder().createCollection(context, child1).withName("Collection 1").withLogo("TestingContentForLogo").build();
        Collection col2 = new CollectionBuilder().createCollection(context, child2).withName("Collection 2").build();

        getClient().perform(get("/api/core/collections/" + col1.getID()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", Matchers.is(
                        CollectionMatcher.matchCollectionEntry(col1.getName(), col1.getID(), col1.getHandle())
                )))
                .andExpect(jsonPath("$", Matchers.not(
                        Matchers.is(
                                CollectionMatcher.matchCollectionEntry(col2.getName(), col2.getID(), col2.getHandle())
                        )))
                )
        ;

        getClient().perform(get("/api/core/collections/" + col1.getID() + "/logo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._links.format.href", Matchers.containsString("/api/core/bitstreams"))).andExpect(jsonPath("$._links.format.href", Matchers.containsString("/format")))
                .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/bitstreams")))
                .andExpect(jsonPath("$._links.content.href", Matchers.containsString("/api/core/bitstreams"))).andExpect(jsonPath("$._links.content.href", Matchers.containsString("/content")))
                ;

    }

    //TODO Search doesn't exist yet for this endpoint. Write tests when it does.
}
