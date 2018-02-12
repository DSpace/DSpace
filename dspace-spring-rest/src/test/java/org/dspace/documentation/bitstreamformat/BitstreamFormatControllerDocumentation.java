/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.documentation.bitstreamformat;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.relaxedLinks;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.test.AbstractDocumentationTest;
import org.junit.Ignore;
import org.junit.Test;


/**
 * Documentation test for the {@link org.dspace.app.rest.repository.BitstreamFormatRestRepository}
 */
public class BitstreamFormatControllerDocumentation extends AbstractDocumentationTest {

    protected String getRestCategory() {
        return RestModel.CORE;
    }

    @Test
    public void findAllBitstreamFormatsDocumentation() throws Exception {
        context.turnOffAuthorisationSystem();

        //When we call the root endpoint
        getClient(this.getRestDocumentation()).perform(get("/api/core/bitstreamformats")
                .param("size", "2"))
                //The status has to be 200 OK
                .andExpect(status().isOk())

                .andDo(document("bitstreamformats",

                        relaxedLinks(
                                linkWithRel("first").description("Link to the first page of bitstreamformats"),
                                linkWithRel("self").description("Link to <<bitstreamformats.adoc#bitstreamformat-list, this>> page"),
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

    @Test
    @Ignore
    public void findOneBitstreamFormatDocumentation() throws Exception {
        context.turnOffAuthorisationSystem();

        //When we call the root endpoint
        getClient(this.getRestDocumentation()).perform(get("/api/core/bitstreamformats/1"))
                //The status has to be 200 OK
                .andExpect(status().isOk())

                .andDo(document("bitstreamformat",

                        relaxedLinks(
                                linkWithRel("self").description("Link to <<bitstreamformats.adoc#bitstreamformat, this>> page")
                        ),

                        relaxedResponseFields(
                                fieldWithPath("shortDescription").description("The short description of the bitstream format"),
                                fieldWithPath("description").description("Full description of the bitstream format"),
                                fieldWithPath("mimetype").description("The mimetype of the bitstream format"),
                                fieldWithPath("supportLevel").description("The support level of the bitstream format"),
                                fieldWithPath("internal").description("Indicates whether the bitstream format is internal or not"),
                                fieldWithPath("extensions").description("Describes the extensions for this bitstream format"),
                                fieldWithPath("type").description("The type of this object"),
                                fieldWithPath("_links").description("The links that are displayed on this page, mostly with regards to pagination if applicable"))
                ))
        ;
    }
}
