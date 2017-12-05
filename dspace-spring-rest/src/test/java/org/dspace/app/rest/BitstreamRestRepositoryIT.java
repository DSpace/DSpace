/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.builder.BitstreamBuilder;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.matcher.BitstreamFormatMatcher;
import org.dspace.app.rest.matcher.BitstreamMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class BitstreamRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private BitstreamService bitstreamService;

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

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald")
                .withSubject("ExtraEntry")
                .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream")
                    .withDescription("description")
                    .withMimeType("text/plain")
                    .build();
        }

        //Add a bitstream to an item
        Bitstream bitstream1 = null;
        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream1")
                    .withDescription("description123")
                    .withMimeType("text/plain")
                    .build();
        }

        getClient().perform(get("/api/core/bitstreams/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.bitstreams", Matchers.containsInAnyOrder(
                        BitstreamMatcher.matchBitstreamEntry(bitstream),
                        BitstreamMatcher.matchBitstreamEntry(bitstream1)
                )))

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

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald")
                .withSubject("ExtraEntry")
                .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream")
                    .withDescription("descr")
                    .withMimeType("text/plain")
                    .build();
        }

        //Add a bitstream to an item
        Bitstream bitstream1 = null;
        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream1")
                    .withDescription("desscrip1")
                    .withMimeType("text/plain")
                    .build();
        }

        getClient().perform(get("/api/core/bitstreams/")
                .param("size","1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.bitstreams", Matchers.contains(
                        BitstreamMatcher.matchBitstreamEntry(bitstream))
                ))
                .andExpect(jsonPath("$._embedded.bitstreams", Matchers.not(
                        Matchers.contains(
                                BitstreamMatcher.matchBitstreamEntry(bitstream1))
                        )
                ))

        ;

        getClient().perform(get("/api/core/bitstreams/")
                .param("size","1")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.bitstreams", Matchers.contains(
                        BitstreamMatcher.matchBitstreamEntry(bitstream1)
                )))
                .andExpect(jsonPath("$._embedded.bitstreams", Matchers.not(
                        Matchers.contains(
                                BitstreamMatcher.matchBitstreamEntry(bitstream)
                        )
                )))

        ;
    }

    //TODO Re-enable test after https://jira.duraspace.org/browse/DS-3774 is fixed
    @Ignore
    @Test
    public void findAllWithDeletedTest() throws Exception{
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

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald")
                .build();

        String bitstreamContent = "This is an archived bitstream";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream")
                    .withMimeType("text/plain")
                    .build();
        }

        //Add a bitstream to an item
        bitstreamContent = "This is a deleted bitstream";
        Bitstream bitstream1 = null;
        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream1")
                    .withMimeType("text/plain")
                    .build();
        }

        //Delete the last bitstream
        bitstreamService.delete(context, bitstream1);
        context.commit();

        getClient().perform(get("/api/core/bitstreams/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.bitstreams", contains(
                        BitstreamMatcher.matchBitstreamEntry(bitstream)
                )))

        ;
    }

    @Test
    public void findOneBitstreamTest() throws Exception {

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

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald")
                .withSubject("ExtraEntry")
                .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream")
                    .withDescription("Description")
                    .withMimeType("text/plain")
                    .build();
        }

        //Add a bitstream to an item
        Bitstream bitstream1 = null;
        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream1")
                    .withDescription("Description1")
                    .withMimeType("text/plain")
                    .build();
        }

        getClient().perform(get("/api/core/bitstreams/"+bitstream.getID()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", BitstreamMatcher.matchBitstreamEntry(bitstream)))
                .andExpect(jsonPath("$", not(BitstreamMatcher.matchBitstreamEntry(bitstream1))))
        ;

    }

    @Test
    public void findOneBitstreamRelsTest() throws Exception {

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

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald")
                .withSubject("ExtraEntry")
                .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream")
                    .withDescription("Description")
                    .withMimeType("text/plain")
                    .build();
        }

        //Add a bitstream to an item
        Bitstream bitstream1 = null;
        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream1")
                    .withDescription("Description1234")
                    .withMimeType("text/plain")
                    .build();
        }

        getClient().perform(get("/api/core/bitstreams/"+bitstream.getID()+"/format"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", BitstreamFormatMatcher.matchBitstreamFormatMimeType("text/plain")))
        ;


        getClient().perform(get("/api/core/bitstreams/"+bitstream.getID()+"/self"))
                .andExpect(status().isOk())
        ;


        getClient().perform(get("/api/core/bitstreams/"+bitstream.getID()+"/content"))
                .andExpect(status().isOk())
                .andExpect(content().string("ThisIsSomeDummyText"))
        ;

    }

    @Test
    public void findOneWrongUUID() throws Exception {

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

        getClient().perform(get("/api/core/bitstreams/"+ UUID.randomUUID()))
                .andExpect(status().isNotFound())
        ;

    }

}
