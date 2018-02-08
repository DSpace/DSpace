/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.documentation;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.relaxedLinks;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.RootRestResourceController;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;


/**
 * Documentation test for the {@link RootRestResourceController}
 */
public class RootControllerDocumentationTest extends AbstractControllerIntegrationTest {

    protected String getRestCategory() {
        return RestModel.CORE;
    }

    @Test
    public void listDefinedEndpoint() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/api"))
                //The status has to be 200 OK
                .andExpect(status().isOk())

                .andDo(document("root",

                        relaxedLinks(
                        linkWithRel("core:bitstreamformats").description("The <<bitstreamformats,Bitstream formats>> in this repository"),
                        linkWithRel("core:bitstreams").description("Endpoint to retrieve all <<bitstreams.adoc#bitstreams,bitstreams>> in this repository"),
                        linkWithRel("core:items").description("Endpoint to retrieve all <<items.adoc#item-list,items>> in this repository"),
                        linkWithRel("discover:browses").description("Endpoint to <<browses,browse the content>> of this repository"),
                        linkWithRel("curies").description("Curies for documentation")
                        ),

                        relaxedResponseFields(
                                fieldWithPath("_links").description("<<root-links,Links>> to other resources"))
                ))
                ;
    }

}