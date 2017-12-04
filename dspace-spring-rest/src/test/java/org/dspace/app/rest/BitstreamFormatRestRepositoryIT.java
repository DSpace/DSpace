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
    public void findAllMimeTypeCheck() throws Exception {
        context.turnOffAuthorisationSystem();
        BitstreamFormat bitstreamFormat = BitstreamFormatBuilder.createBitstreamFormat(context)
                .withMimeType("application/octet-stream")
                .withDescription("Description")
                .build();
        getClient().perform(get("/api/core/bitstreamformats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.totalElements", is(1)))
                .andExpect(jsonPath("$._embedded.bitstreamformats", Matchers.contains(
                        BitstreamFormatMatcher.matchBitstreamFormat(bitstreamFormat.getMIMEType(), bitstreamFormat.getDescription())
                )))
        ;

    }

    @Test
    @Ignore
    public void findOne() throws Exception {
        getClient().perform(get("/api/core/bitstreamformats/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._links.self.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.self.href", endsWith("/api/core/bitstreamformats/1")))
        ;
    }
}
