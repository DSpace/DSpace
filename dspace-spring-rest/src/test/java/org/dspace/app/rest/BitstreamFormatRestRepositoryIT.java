/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import org.dspace.app.rest.builder.BitstreamFormatBuilder;
import org.dspace.app.rest.matcher.BitstreamFormatMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.BitstreamFormat;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Jonas Van Goolen - (jonas@atmire.com)
 */

public class BitstreamFormatRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAllPaginationTest() throws Exception {
        getClient().perform(get("/api/core/bitstreamformats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$._links.self.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.self.href", endsWith("/api/core/bitstreamformats")))
        ;
    }

    @Test
    @Ignore
    public void unknownFormatRequiredByDefault() throws Exception {
        getClient().perform(get("/api/core/bitstreamformats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(1)))
                .andExpect(jsonPath("$._links.self.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.self.href", endsWith("/api/core/bitstreamformats")))
                .andExpect(jsonPath("$._embedded.bitstreamformats", Matchers.is(
                        BitstreamFormatMatcher.matchBitstreamFormatMimeType("Unknown")
                )));
    }

    @Test
    @Ignore
    public void findAllMimeTypeCheck() throws Exception {
        context.turnOffAuthorisationSystem();
        BitstreamFormat bitstreamFormat = BitstreamFormatBuilder.createBitstreamFormat(context)
                .withMimeType("application/octet-stream")
                .withDescription("Description")
                .build();
        getClient().perform(get("/api/core/bitstreamformats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.totalElements", is(2)))
                .andExpect(jsonPath("$._embedded.bitstreamformats", Matchers.contains(
                        BitstreamFormatMatcher.matchBitstreamFormat(bitstreamFormat.getMIMEType(), bitstreamFormat.getDescription())
                )))
        ;

    }

    @Test
    @Ignore
    public void findOne() throws Exception {
        context.turnOffAuthorisationSystem();
        BitstreamFormat bitstreamFormat = BitstreamFormatBuilder.createBitstreamFormat(context)
                .withMimeType("application/octet-stream")
                .withDescription("Description")
                .build();

        getClient().perform(get("/api/core/bitstreamformats/" + bitstreamFormat.getID()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.description", is(bitstreamFormat.getDescription())))
                .andExpect(jsonPath("$.mimetype", is(bitstreamFormat.getMIMEType())))
                .andExpect(jsonPath("$.type", is("bitstreamformat")))
                .andExpect(jsonPath("$._links.self.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.self.href", endsWith("/api/core/bitstreamformats/"+bitstreamFormat.getID())))
        ;
    }
}
