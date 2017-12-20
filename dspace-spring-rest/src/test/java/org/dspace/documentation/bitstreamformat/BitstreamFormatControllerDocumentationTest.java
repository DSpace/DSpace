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
 * Documentation test for the {@link org.dspace.app.rest.repository.BitstreamFormatRestRepository}
 */
public class BitstreamFormatControllerDocumentationTest extends AbstractControllerIntegrationTest {

    protected String getRestCategory() {
        return RestModel.CORE;
    }

    @Test
    public void findAllBitstreamFormatsDocumentation() throws Exception {
        context.turnOffAuthorisationSystem();

        //When we call the root endpoint
        getClient().perform(get("/api/core/bitstreamformats")
                .param("size", "2"))
                //The status has to be 200 OK
                .andExpect(status().isOk())

                .andDo(document("formats",

                        relaxedLinks(
                                linkWithRel("first").description("Link to the first page of bitstreamformats"),
                                linkWithRel("self").description("Link to <<formats.adoc#bitstreamformat-list, this>> page"),
                                linkWithRel("next").description("Link to the next page"),
                                linkWithRel("last").description("Link to the last page"),
                                linkWithRel("curies").description("Curies for documentation")
                        ),

                        relaxedResponseFields(
                                fieldWithPath("_embedded").description("The bitstreamformats that are in the repository, displayed with pagination"),
                                fieldWithPath("_links").description("The links that are displayed on this page, mostly with regards to pagination if applicable"),
                                fieldWithPath("page").description("A page object that contains information about the amount of bitstreamformats shown and in total"))
                ))
        ;
    }

}
