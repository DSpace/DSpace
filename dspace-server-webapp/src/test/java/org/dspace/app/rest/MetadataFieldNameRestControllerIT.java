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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.builder.MetadataFieldBuilder;
import org.dspace.app.rest.builder.MetadataSchemaBuilder;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.junit.Test;

/**
 * Integration tests for the {@link org.dspace.app.rest.MetadataFieldNameRestController} controlled endpoints
 *
 * @author Maria Verdonck (Atmire) on 17/07/2020
 */
public class MetadataFieldNameRestControllerIT extends AbstractEntityIntegrationTest {

    private static final String GETBYNAME_METADATAFIELDS_ENDPOINT =
        MetadatafieldRestRepositoryIT.METADATAFIELDS_ENDPOINT + "name/";

    @Test
    public void testGetMetadataFieldByName_ExistingName() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataSchema schema = MetadataSchemaBuilder.createMetadataSchema(context, "ASchema",
            "http://www.dspace.org/ns/aschema").build();

        MetadataField metadataField = MetadataFieldBuilder
            .createMetadataField(context, schema, "AnElement1", "AQualifier", "AScopeNote").build();

        context.restoreAuthSystemState();
        getClient().perform(get(GETBYNAME_METADATAFIELDS_ENDPOINT + metadataField.toString('.')))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.id", is(metadataField.getID())))
                   .andExpect(jsonPath("$.element", is(metadataField.getElement())));
    }


    @Test
    public void testGetMetadataFieldByName_ExistingName_NoQualifier() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataSchema schema = MetadataSchemaBuilder.createMetadataSchema(context, "ASchema",
            "http://www.dspace.org/ns/aschema").build();

        MetadataField metadataField = MetadataFieldBuilder
            .createMetadataField(context, schema, "AnElement1", null, "AScopeNote").build();

        context.restoreAuthSystemState();
        getClient().perform(get(GETBYNAME_METADATAFIELDS_ENDPOINT + metadataField.toString('.')))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.id", is(metadataField.getID())))
                   .andExpect(jsonPath("$.element", is(metadataField.getElement())))
                   .andExpect(jsonPath("$.qualifier", is(metadataField.getQualifier())));
    }

    @Test
    public void testGetMetadataFieldByName_NonExistentName() throws Exception {
        String nonExistentName = "nonExistentName";
        getClient().perform(get(GETBYNAME_METADATAFIELDS_ENDPOINT + nonExistentName))
                   .andExpect(status().isNotFound());
    }
}
