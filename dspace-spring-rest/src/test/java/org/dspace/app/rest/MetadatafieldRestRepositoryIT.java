/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.builder.MetadataFieldBuilder;
import org.dspace.app.rest.matcher.MetadataFieldMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.MetadataField;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;

public class MetadatafieldRestRepositoryIT extends AbstractControllerIntegrationTest {


    @Test
    public void findAll() throws Exception {

        context.turnOffAuthorisationSystem();
        MetadataField metadataField = MetadataFieldBuilder
            .createMetadataField(context, "AnElement", "AQualifier", "AScopeNote").build();

        getClient().perform(get("/api/core/metadatafields"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItem(
                       MetadataFieldMatcher.matchMetadataField()
                   )))
                   .andExpect(jsonPath("$._links.first.href", Matchers.containsString("/api/core/metadatafields")))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/metadatafields")))
                   .andExpect(jsonPath("$._links.next.href", Matchers.containsString("/api/core/metadatafields")))
                   .andExpect(jsonPath("$._links.last.href", Matchers.containsString("/api/core/metadatafields")))

                   .andExpect(jsonPath("$.page.size", is(20)));
    }

    @Test
    @Ignore
    public void findOne() throws Exception {

        context.turnOffAuthorisationSystem();
        MetadataField metadataField = MetadataFieldBuilder
            .createMetadataField(context, "AnElement", "AQualifier", "AScopeNote").build();

        getClient().perform(get("/api/core/metadatafields/" + metadataField.getID()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField)
                   )))

                   .andExpect(jsonPath("$.page.size", is(20)));
    }
}
