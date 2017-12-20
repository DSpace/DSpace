/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.documentation.bitstream;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.builder.BitstreamBuilder;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.junit.Test;

import java.io.InputStream;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.relaxedLinks;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Documentation test for the {@link org.dspace.app.rest.repository.BitstreamRestRepository}
 */
public class BitstreamControllerDocumentationTest extends AbstractControllerIntegrationTest {

    protected String getRestCategory() {
        return RestModel.CORE;
    }


    @Test
    public void findOneBitstreamDocumentation() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = new CommunityBuilder().createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = new CommunityBuilder().createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = new CollectionBuilder().createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = new CollectionBuilder().createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = new ItemBuilder().createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = new BitstreamBuilder().
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream")
                    .build();
        }

        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()))
                .andExpect(status().isOk())
                .andDo(
                        document("bitstream",

                                relaxedLinks(
                                        linkWithRel("core:format").description("Link to the format of the bitstream"),
                                        linkWithRel("self").description("Link to <<bitstreams.adoc, this>> page"),
                                        linkWithRel("core:content").description("Link to the content of the bitstream"),
                                        linkWithRel("curies").description("Curies for documentation")
                                ),

                                relaxedResponseFields(
                                        fieldWithPath("_embedded").description("The embedded bitstream format for this bitstream")
                        )))
                ;


    }

    @Test
    public void findAllBitstreamsDocumentation() throws Exception {
        context.turnOffAuthorisationSystem();

//** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = new CommunityBuilder().createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = new CommunityBuilder().createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = new CollectionBuilder().createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = new CollectionBuilder().createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = new ItemBuilder().createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = new BitstreamBuilder().
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream")
                    .build();
        }

        Bitstream bitstream1 = null;
        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = new BitstreamBuilder().
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream")
                    .build();
        }
        Bitstream bitstream2 = null;
        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = new BitstreamBuilder().
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream")
                    .build();
        }

        //When we call the root endpoint
        getClient().perform(get("/api/core/bitstreams")
                .param("size", "2"))
                //The status has to be 200 OK
                .andExpect(status().isOk())

                .andDo(document("bitstreams",

                        relaxedLinks(
                                linkWithRel("first").description("Link to the first page of bitstreams"),
                                linkWithRel("self").description("Link to <<bitstreams.adoc#bitstream-list, this>> page"),
                                linkWithRel("next").description("Link to the next page"),
                                linkWithRel("last").description("Link to the last page"),
                                linkWithRel("curies").description("Curies for documentation")
                        ),

                        relaxedResponseFields(
                                fieldWithPath("_embedded").description("The bitstreams that are in the repository, displayed with pagination"),
                                fieldWithPath("_links").description("The links that are displayed on this page, mostly with regards to pagination if applicable"),
                                fieldWithPath("page").description("A page object that contains information about the amount of bitstreams shown and in total"))
                ))
        ;
    }

}
