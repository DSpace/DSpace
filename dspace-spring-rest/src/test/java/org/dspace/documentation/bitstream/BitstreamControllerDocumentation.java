/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.documentation.bitstream;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.relaxedLinks;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.builder.BitstreamBuilder;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.test.AbstractDocumentationTest;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.junit.Test;


/**
 * Documentation test for the {@link org.dspace.app.rest.repository.BitstreamRestRepository}
 */
public class BitstreamControllerDocumentation extends AbstractDocumentationTest {

    protected String getRestCategory() {
        return RestModel.CORE;
    }


    @Test
    public void findOneBitstreamDocumentation() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream")
                    .build();
        }

        getClient(this.getRestDocumentation()).perform(get("/api/core/bitstreams/" + bitstream.getID()))
                .andExpect(status().isOk())
                .andDo(
                        document("bitstream",

                                relaxedLinks(
                                        linkWithRel("c:format").description("Link to the <<bitstreamformats.adoc#bitstreamformats,format>> of the bitstream"),
                                        linkWithRel("self").description("Link to <<bitstreams.adoc#bitstream, this>> page"),
                                        linkWithRel("c:content").description("Link to the <<contents.adoc#content, content>> of the bitstream"),
                                        linkWithRel("curies").description("Curies for documentation")
                                ),

                                relaxedResponseFields(
                                        fieldWithPath("uuid").description("The UUID of the selected bitstream"),
                                        fieldWithPath("name").description("The name of the selected bitstream"),
                                        fieldWithPath("metadata").description("The metadata of the selected bitstream"),
                                        fieldWithPath("sizeBytes").description("The size of this bitstream"),
                                        fieldWithPath("checkSum").description("The checksum object for this bitstream"),
                                        fieldWithPath("type").description("The type namely bitstream"),
                                        fieldWithPath("_links").description("The links displayed in this bitstream endpoint"),
                                        fieldWithPath("_embedded").description("The embedded bitstream format for this bitstream")
                        )))
                ;


    }

    @Test
    public void findAllBitstreamsDocumentation() throws Exception {
        context.turnOffAuthorisationSystem();

//** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream")
                    .build();
        }

        Bitstream bitstream1 = null;
        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream")
                    .build();
        }
        Bitstream bitstream2 = null;
        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream")
                    .build();
        }

        //When we call the root endpoint
        getClient(this.getRestDocumentation()).perform(get("/api/core/bitstreams")
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
