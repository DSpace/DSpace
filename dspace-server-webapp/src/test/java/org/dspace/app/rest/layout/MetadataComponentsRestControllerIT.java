/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.layout;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;

import org.dspace.app.rest.builder.CrisLayoutBoxBuilder;
import org.dspace.app.rest.builder.CrisLayoutFieldBuilder;
import org.dspace.app.rest.builder.EntityTypeBuilder;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.EntityType;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutField;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This test class verify the REST Services for the Layout Metadata Component functionality
 * (endpoint /api/layout/boxmetadataconfiguration/<:string>)
 * 
 * @author Danilo Di Nuzzo (danilo dot dinuzzo at 4science dot it)
 *
 */
public class MetadataComponentsRestControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    private MetadataSchemaService mdss;

    @Autowired
    private MetadataFieldService mfss;

    @Test
    public void getMetadataComponent() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // get metadata field
        MetadataSchema schema = mdss.find(context, "dc");
        MetadataField isbn = mfss.findByElement(context, schema, "identifier", "isbn");
        MetadataField uri = mfss.findByElement(context, schema, "identifier", "uri");
        MetadataField abs = mfss.findByElement(context, schema, "description", "abstract");
        MetadataField provenance = mfss.findByElement(context, schema, "description", "provenance");
        MetadataField sponsorship = mfss.findByElement(context, schema, "description", "sponsorship");
        MetadataField extent = mfss.findByElement(context, schema, "format", "extent");
        // Create boxes
        CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                .withShortname("box-shortname-one")
                .build();
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                .withShortname("box-shortname-two")
                .build();
        CrisLayoutFieldBuilder.createMetadataField(context, isbn, 0, 0)
                .withLabel("LABEL ISBN")
                .withRendering("RENDERIGN ISBN")
                .withStyle("STYLE")
                .withBox(box)
                .build();
        CrisLayoutFieldBuilder.createMetadataField(context, uri, 0, 1)
                .withLabel("LABEL URI")
                .withRendering("RENDERIGN URI")
                .withStyle("STYLE")
                .withBox(box)
                .build();
        CrisLayoutFieldBuilder.createMetadataField(context, abs, 1, 0)
                .withLabel("LABEL ABS")
                .withRendering("RENDERIGN ABS")
                .withStyle("STYLE")
                .withBox(box)
                .build();
        CrisLayoutFieldBuilder.createMetadataField(context, provenance, 1, 1)
                .withLabel("LABEL PROVENANCE")
                .withRendering("RENDERIGN PROVENANCE")
                .withStyle("STYLE")
                .withBox(box)
                .build();
        CrisLayoutFieldBuilder.createMetadataField(context, sponsorship, 1, 2)
                .withLabel("LABEL SPRONSORSHIP")
                .withRendering("RENDERIGN SPRONSORSHIP")
                .withStyle("STYLE")
                .withBox(box)
                .build();
        CrisLayoutFieldBuilder.createMetadataField(context, extent, 2, 0)
                .withLabel("LABEL EXTENT")
                .withRendering("RENDERIGN EXTENT")
                .withStyle("STYLE")
                .withBox(box)
                .build();
        CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                .withShortname("box-shortname-three")
                .build();
        context.restoreAuthSystemState();
        // Test WS endpoint
        getClient().perform(get("/api/layout/boxmetadataconfigurations/" + box.getID()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.id", Matchers.is(box.getID())))
            .andExpect(jsonPath("$.rows.length()", Matchers.is(3)))
            .andExpect(jsonPath("$.rows[0].fields.length()", Matchers.is(2)))
            .andExpect(jsonPath("$.rows[1].fields.length()", Matchers.is(3)))
            .andExpect(jsonPath("$.rows[2].fields.length()", Matchers.is(1)));
    }

    @Test
    public void patchAddMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                .withShortname("box-shortname-test")
                                                .build();

        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);

        List<Operation> operations = new ArrayList<Operation>();
        List<Map<String, String>> metadataValues = new ArrayList<Map<String, String>>();
        Map<String, String> values = new HashMap<String, String>();
        values.put("metadata", "orgunit.identifier.name");
        values.put("label", "Department Name");
        values.put("rendering", "browselink");
        values.put("fieldType", "metadata");
        metadataValues.add(values);
        operations.add(new AddOperation("/rows/0/fields/0", metadataValues));

        String patchBody = getPatchContent(operations);
        getClient(authToken).perform(patch("/api/layout/boxmetadataconfigurations/" + box.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].fields.length()", Matchers.is(1)))
                .andExpect(jsonPath("$", Matchers.allOf(
                        hasJsonPath("$.id", is(box.getID())), // now the configuration id is a number (box id)
                        hasJsonPath("$.type", is("boxmetadataconfiguration")),
                        hasJsonPath("$.rows[0].fields[0].metadata", is("orgunit.identifier.name")),
                        hasJsonPath("$.rows[0].fields[0].label", is("Department Name")),
                        hasJsonPath("$.rows[0].fields[0].fieldType", is("METADATA")),
                        hasJsonPath("$.rows[0].fields[0].rendering", is("browselink"))
                       )));

    }

    @Test
    public void patchRemoveMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        MetadataSchema schema = mdss.find(context, "orgunit");
        MetadataField contributor = mfss.findByElement(context, schema, "identifier", "name");

        CrisLayoutField fieldContributor = CrisLayoutFieldBuilder.createMetadataField(context, contributor, 0, 0)
                                                                 .withLabel("Author")
                                                                 .withRendering("")
                                                                 .withStyle("STYLE")
                                                                 .build();

        schema = mdss.find(context, "person");
        MetadataField firstName = mfss.findByElement(context, schema, "givenName", null);
        MetadataField lastName = mfss.findByElement(context, schema, "familyName", null);

        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                .withShortname("box-shortname-test")
                .addField(fieldContributor)
                .build();

        CrisLayoutFieldBuilder.createMetadataField(context, firstName, 0, 1)
                .withLabel("Author")
                .withRendering("")
                .withStyle("STYLE")
                .withBox(box)
                .build();

        CrisLayoutFieldBuilder.createMetadataField(context, lastName, 0, 2)
                .withLabel("Author")
                .withRendering("")
                .withStyle("STYLE")
                .withBox(box)
                .build();

        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);

        List<Operation> operations = new ArrayList<Operation>();
        operations.add(new RemoveOperation("/rows/0/fields/0"));

        String patchBody = getPatchContent(operations);
        getClient(authToken).perform(patch("/api/layout/boxmetadataconfigurations/" + box.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk());

        getClient(authToken).perform(get("/api/layout/boxmetadataconfigurations/" + box.getID()))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$.id", Matchers.is(box.getID())))
                            .andExpect(jsonPath("$.rows.length()", Matchers.is(1)))
                            .andExpect(jsonPath("$.rows[0].fields.length()", Matchers.is(1)));
    }

    @Test
    public void patchAddMetadataNotExistTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                .withShortname("box-shortname-test")
                                                .build();

        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);

        List<Operation> operations = new ArrayList<Operation>();
        List<Map<String, String>> metadataValues = new ArrayList<Map<String, String>>();
        Map<String, String> values = new HashMap<String, String>();
        values.put("metadata", "dc.not.exist");
        values.put("label", "wrong metadata");
        values.put("fieldType", "metadata");
        metadataValues.add(values);
        operations.add(new AddOperation("/rows/0/fields/0", metadataValues));

        String patchBody = getPatchContent(operations);
        getClient(authToken).perform(patch("/api/layout/boxmetadataconfigurations/" + box.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void patchAddBistreamFieldTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                .withShortname("box-shortname-test")
                                                .build();

        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);

        List<Operation> operations = new ArrayList<Operation>();
        List<Map<String, Object>> metadataValues = new ArrayList<Map<String, Object>>();
        Map<String, Object> values = new HashMap<String, Object>();
        Map<String, String> bitstream = new HashMap<String, String>();
        values.put("label", "Department Logo");
        values.put("rendering", "thumbnail");
        values.put("fieldType", "bitstream");
        values.put("bitstream", bitstream);
        bitstream.put("metadataField", "dc.type");
        bitstream.put("metadataValue", "Logo");
        bitstream.put("bundle", "ORIGINAL");
        metadataValues.add(values);
        operations.add(new AddOperation("/rows/0/fields/0", metadataValues));

        String patchBody = getPatchContent(operations);
        getClient(authToken).perform(patch("/api/layout/boxmetadataconfigurations/" + box.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].fields.length()", Matchers.is(1)))
                .andExpect(jsonPath("$", Matchers.allOf(
                        hasJsonPath("$.id", is(box.getID())), // now the configuration id is a number (box id)
                        hasJsonPath("$.type", is("boxmetadataconfiguration")),
                        hasNoJsonPath("$.rows[0].fields[0].metadata"),
                        hasJsonPath("$.rows[0].fields[0].bitstream.bundle", is("ORIGINAL")),
                        hasJsonPath("$.rows[0].fields[0].bitstream.metadataField", is("dc.type")),
                        hasJsonPath("$.rows[0].fields[0].bitstream.metadataValue", is("Logo")),
                        hasJsonPath("$.rows[0].fields[0].label", is("Department Logo")),
                        hasJsonPath("$.rows[0].fields[0].fieldType", is("BITSTREAM")),
                        hasJsonPath("$.rows[0].fields[0].rendering", is("thumbnail"))
                       )));

    }

}