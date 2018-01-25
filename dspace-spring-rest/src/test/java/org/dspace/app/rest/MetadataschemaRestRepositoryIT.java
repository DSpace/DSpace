/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import org.dspace.app.rest.builder.MetadataSchemaBuilder;
import org.dspace.app.rest.matcher.MetadataschemaMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.MetadataSchema;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.is;

public class MetadataschemaRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAll() throws Exception{

        context.turnOffAuthorisationSystem();
        MetadataSchema metadataSchema = MetadataSchemaBuilder.createMetadataSchema(context, "ATest", "ANamespace").build();

        getClient().perform(get("/api/core/metadataschemas"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.metadataschemas", Matchers.hasItem(
                        MetadataschemaMatcher.matchEntry()
                )))
                .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/metadataschemas")))
                .andExpect(jsonPath("$.page.size", is(20)));
    }


    //TODO This test fails, reactivate when endpoint is fixed
    @Test
    @Ignore
    public void findOne() throws Exception{

        context.turnOffAuthorisationSystem();
        MetadataSchema metadataSchema = MetadataSchemaBuilder.createMetadataSchema(context, "ATest", "ANamespace").build();

        getClient().perform(get("/api/core/metadataschemas/" + metadataSchema.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(
                        MetadataschemaMatcher.matchEntry(metadataSchema)
                )));
    }
}
