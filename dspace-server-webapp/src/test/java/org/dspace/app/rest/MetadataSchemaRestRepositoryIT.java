/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static org.dspace.app.rest.matcher.MetadataschemaMatcher.matchEntry;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.builder.MetadataSchemaBuilder;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.matcher.MetadataschemaMatcher;
import org.dspace.app.rest.model.MetadataSchemaRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.MetadataSchema;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for the {@link org.dspace.app.rest.repository.MetadataSchemaRestRepository}
 * This class will include all the tests for the logic with regards to the
 * {@link org.dspace.app.rest.repository.MetadataSchemaRestRepository}
 */
public class MetadataSchemaRestRepositoryIT extends AbstractControllerIntegrationTest {

    private static final String TEST_NAME = "testSchemaName";
    private static final String TEST_NAMESPACE = "testSchemaNameSpace";

    private static final String TEST_NAME_UPDATED = "testSchemaNameUpdated";
    private static final String TEST_NAMESPACE_UPDATED = "testSchemaNameSpaceUpdated";

    @Autowired
    ConverterService converter;

    @Test
    public void findAll() throws Exception {

        context.turnOffAuthorisationSystem();
        MetadataSchema metadataSchema = MetadataSchemaBuilder.createMetadataSchema(context, "ATest", "ANamespace")
            .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/metadataschemas"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.metadataschemas", Matchers.hasItem(
                        matchEntry()
                )))
                .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/metadataschemas")))
                .andExpect(jsonPath("$.page.size", is(20)));
    }

    @Test
    public void findOne() throws Exception {

        context.turnOffAuthorisationSystem();
        MetadataSchema metadataSchema = MetadataSchemaBuilder.createMetadataSchema(context, "ATest", "ANamespace")
                .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/metadataschemas/" + metadataSchema.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(
                        matchEntry(metadataSchema)
                )));
    }

    @Test
    public void createSuccess() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataSchema metadataSchema = MetadataSchemaBuilder.createMetadataSchema(context, "ATest", "ANamespace")
                                                             .build();
        context.restoreAuthSystemState();

        MetadataSchemaRest metadataSchemaRest = converter.toRest(metadataSchema, Projection.DEFAULT);
        metadataSchemaRest.setPrefix(TEST_NAME);
        metadataSchemaRest.setNamespace(TEST_NAMESPACE);

        String authToken = getAuthToken(admin.getEmail(), password);
        AtomicReference<Integer> idRef = new AtomicReference<>();


        getClient(authToken)
                .perform(post("/api/core/metadataschemas")
                        .content(new ObjectMapper().writeValueAsBytes(metadataSchemaRest))
                        .contentType(contentType))
                .andExpect(status().isCreated())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        getClient().perform(get("/api/core/metadataschemas/" + idRef.get()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", MetadataschemaMatcher.matchEntry(TEST_NAME, TEST_NAMESPACE)));
    }

    @Test
    public void createUnauthorizedTest()
            throws Exception {
        MetadataSchemaRest metadataSchemaRest = new MetadataSchemaRest();
        metadataSchemaRest.setPrefix(TEST_NAME);
        metadataSchemaRest.setNamespace(TEST_NAMESPACE);

        getClient()
                .perform(post("/api/core/metadataschemas")
                        .content(new ObjectMapper().writeValueAsBytes(metadataSchemaRest))
                        .contentType(contentType))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteSuccess() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataSchema metadataSchema = MetadataSchemaBuilder.createMetadataSchema(context, "ATest", "A namespace")
                                                             .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/metadataschemas/" + metadataSchema.getID()))
                   .andExpect(status().isOk());


        getClient(getAuthToken(admin.getEmail(), password))
                .perform(delete("/api/core/metadataschemas/" + metadataSchema.getID()))
                .andExpect(status().isNoContent());

        getClient().perform(get("/api/core/metadataschemas/" + metadataSchema.getID()))
                   .andExpect(status().isNotFound());

    }

    @Test
    public void deleteUnauthorized() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataSchema metadataSchema = MetadataSchemaBuilder.createMetadataSchema(context, TEST_NAME, TEST_NAMESPACE)
                                                             .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/metadataschemas/" + metadataSchema.getID())).andExpect(status().isOk());

        getClient()
                .perform(delete("/api/core/metadataschemas/" + metadataSchema.getID()))
                .andExpect(status().isUnauthorized());

        getClient().perform(get("/api/core/metadataschemas/" + metadataSchema.getID())).andExpect(status().isOk());

    }

    @Test
    public void deleteNonExisting() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataSchema metadataSchema = MetadataSchemaBuilder.createMetadataSchema(context, "A name", "A namespace")
                                                             .build();

        context.restoreAuthSystemState();

        Integer id = metadataSchema.getID();

        getClient(getAuthToken(admin.getEmail(), password))
            .perform(delete("/api/core/metadataschemas/" + id))
            .andExpect(status().isNoContent());

        getClient(getAuthToken(admin.getEmail(), password))
                .perform(delete("/api/core/metadataschemas/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    public void update() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataSchema metadataSchema = MetadataSchemaBuilder.createMetadataSchema(context, TEST_NAME, TEST_NAMESPACE)
                                                             .build();

        context.restoreAuthSystemState();

        MetadataSchemaRest metadataSchemaRest = new MetadataSchemaRest();
        metadataSchemaRest.setId(metadataSchema.getID());
        metadataSchemaRest.setPrefix(TEST_NAME_UPDATED);
        metadataSchemaRest.setNamespace(TEST_NAMESPACE_UPDATED);

        getClient(getAuthToken(admin.getEmail(), password))
                .perform(put("/api/core/metadataschemas/" + metadataSchema.getID())
                        .content(new ObjectMapper().writeValueAsBytes(metadataSchemaRest))
                        .contentType(contentType))
                .andExpect(status().isOk());

        getClient().perform(get("/api/core/metadataschemas/" + metadataSchema.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", MetadataschemaMatcher
                       .matchEntry(TEST_NAME_UPDATED, TEST_NAMESPACE_UPDATED)));
    }

    @Test
    public void updateUnauthorized() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataSchema metadataSchema = MetadataSchemaBuilder.createMetadataSchema(context, TEST_NAME, TEST_NAMESPACE)
                                                             .build();

        context.restoreAuthSystemState();

        MetadataSchemaRest metadataSchemaRest = new MetadataSchemaRest();
        metadataSchemaRest.setId(metadataSchema.getID());
        metadataSchemaRest.setPrefix(TEST_NAME_UPDATED);
        metadataSchemaRest.setNamespace(TEST_NAMESPACE_UPDATED);

        getClient()
                .perform(put("/api/core/metadataschemas/" + metadataSchema.getID())
                        .content(new ObjectMapper().writeValueAsBytes(metadataSchemaRest))
                        .contentType(contentType))
                .andExpect(status().isUnauthorized());

        getClient().perform(get("/api/core/metadataschemas/" + metadataSchema.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", MetadataschemaMatcher
                       .matchEntry(TEST_NAME, TEST_NAMESPACE)));
    }

    @Test
    public void updateWrongRights() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataSchema metadataSchema = MetadataSchemaBuilder.createMetadataSchema(context, TEST_NAME, TEST_NAMESPACE)
                                                             .build();

        context.restoreAuthSystemState();

        MetadataSchemaRest metadataSchemaRest = new MetadataSchemaRest();
        metadataSchemaRest.setId(metadataSchema.getID());
        metadataSchemaRest.setPrefix(TEST_NAME_UPDATED);
        metadataSchemaRest.setNamespace(TEST_NAMESPACE_UPDATED);

        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(put("/api/core/metadataschemas/" + metadataSchema.getID())
                         .content(new ObjectMapper().writeValueAsBytes(metadataSchemaRest))
                         .contentType(contentType))
            .andExpect(status().isForbidden());

        getClient().perform(get("/api/core/metadataschemas/" + metadataSchema.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", MetadataschemaMatcher
                       .matchEntry(TEST_NAME, TEST_NAMESPACE)));
    }

}
