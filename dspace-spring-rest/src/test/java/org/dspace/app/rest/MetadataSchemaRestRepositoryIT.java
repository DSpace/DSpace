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
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.builder.MetadataSchemaBuilder;
import org.dspace.app.rest.model.MetadataSchemaRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.content.service.MetadataSchemaService;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class MetadataSchemaRestRepositoryIT extends AbstractControllerIntegrationTest {

    private static final String TEST_NAME = "testSchemaName";
    private static final String TEST_NAMESPACE = "testSchemaNameSpace";

    private static final String TEST_NAME_UPDATED = "testSchemaNameUpdated";
    private static final String TEST_NAMESPACE_UPDATED = "testSchemaNameSpaceUpdated";

    @Autowired
    private MetadataSchemaService metadataSchemaService;

    @Test
    public void findAll() throws Exception {

        context.turnOffAuthorisationSystem();
        MetadataSchema metadataSchema = MetadataSchemaBuilder.createMetadataSchema(context, "ATest", "ANamespace")
                .build();

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

        getClient().perform(get("/api/core/metadataschemas/" + metadataSchema.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(
                        matchEntry(metadataSchema)
                )));
    }

    @Test
    public void createSuccess() throws Exception {

        MetadataSchemaRest metadataSchemaRest = new MetadataSchemaRest();
        metadataSchemaRest.setPrefix(TEST_NAME);
        metadataSchemaRest.setNamespace(TEST_NAMESPACE);

        String authToken = getAuthToken(admin.getEmail(), password);
        AtomicReference<Integer> idRef = new AtomicReference<>();

        try {

            assertThat(metadataSchemaService.find(context, TEST_NAME), nullValue());

            getClient(authToken)
                    .perform(post("/api/core/metadataschemas")
                            .content(new ObjectMapper().writeValueAsBytes(metadataSchemaRest))
                            .contentType(contentType))
                    .andExpect(status().isCreated())
                    .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            MetadataSchema metadataSchema = metadataSchemaService.find(context, idRef.get());
            assertThat(metadataSchema, notNullValue());

            assertEquals(metadataSchema.getID(), idRef.get());
            assertEquals(metadataSchema.getName(), TEST_NAME);
            assertEquals(metadataSchema.getNamespace(), TEST_NAMESPACE);

        } finally {
            deleteMetadataSchemaIfExists(TEST_NAME);
        }
    }

    @Test
    public void createUnauthauthorizedTest()
            throws Exception {

        MetadataSchemaRest metadataSchemaRest = new MetadataSchemaRest();
        metadataSchemaRest.setPrefix(TEST_NAME);
        metadataSchemaRest.setNamespace(TEST_NAMESPACE);

        try {
            getClient()
                    .perform(post("/api/core/metadataschemas")
                            .content(new ObjectMapper().writeValueAsBytes(metadataSchemaRest))
                            .contentType(contentType))
                    .andExpect(status().isUnauthorized());

        } finally {
            deleteMetadataSchemaIfExists(TEST_NAME);
        }
    }

    @Test
    public void deleteSuccess() throws Exception {

        MetadataSchema metadataSchema = createMetadataSchema();

        try {

            assertThat(metadataSchemaService.find(context, metadataSchema.getID()), notNullValue());

            getClient(getAuthToken(admin.getEmail(), password))
                    .perform(delete("/api/core/metadataschemas/" + metadataSchema.getID()))
                    .andExpect(status().isNoContent());

            assertThat(metadataSchemaService.find(context, metadataSchema.getID()), nullValue());

        } finally {
            deleteMetadataSchemaIfExists(TEST_NAME);
        }
    }

    @Test
    public void deleteUnauthorized() throws Exception {

        MetadataSchema metadataSchema = createMetadataSchema();

        try {

            assertThat(metadataSchemaService.find(context, metadataSchema.getID()), notNullValue());

            getClient()
                    .perform(delete("/api/core/metadataschemas/" + metadataSchema.getID()))
                    .andExpect(status().isUnauthorized());

            assertThat(metadataSchemaService.find(context, metadataSchema.getID()), notNullValue());

        } finally {
            deleteMetadataSchemaIfExists(TEST_NAME);
        }
    }

    @Test
    public void deleteNonExisting() throws Exception {

        MetadataSchema metadataSchema = createMetadataSchema();
        deleteMetadataSchemaIfExists(TEST_NAME);

        Integer id = metadataSchema.getID();
        assertThat(metadataSchemaService.find(context, id), nullValue());

        getClient(getAuthToken(admin.getEmail(), password))
                .perform(delete("/api/core/metadataschemas/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    public void update() throws Exception {

        MetadataSchema metadataSchema = createMetadataSchema();

        MetadataSchemaRest metadataSchemaRest = new MetadataSchemaRest();
        metadataSchemaRest.setId(metadataSchema.getID());
        metadataSchemaRest.setPrefix(TEST_NAME_UPDATED);
        metadataSchemaRest.setNamespace(TEST_NAMESPACE_UPDATED);

        try {
            getClient(getAuthToken(admin.getEmail(), password))
                    .perform(put("/api/core/metadataschemas/" + metadataSchema.getID())
                            .content(new ObjectMapper().writeValueAsBytes(metadataSchemaRest))
                            .contentType(contentType))
                    .andExpect(status().isOk());

            metadataSchema = metadataSchemaService.find(context, metadataSchema.getID());

            assertEquals(TEST_NAME_UPDATED, metadataSchema.getName());
            assertEquals(TEST_NAMESPACE_UPDATED, metadataSchema.getNamespace());
        } finally {
            deleteMetadataSchemaIfExists(metadataSchema);
        }
    }

    @Test
    public void updateUnauthorized() throws Exception {

        MetadataSchema metadataSchema = createMetadataSchema();

        MetadataSchemaRest metadataSchemaRest = new MetadataSchemaRest();
        metadataSchemaRest.setId(metadataSchema.getID());
        metadataSchemaRest.setPrefix(TEST_NAME_UPDATED);
        metadataSchemaRest.setNamespace(TEST_NAMESPACE_UPDATED);

        try {
            getClient()
                    .perform(put("/api/core/metadataschemas/" + metadataSchema.getID())
                            .content(new ObjectMapper().writeValueAsBytes(metadataSchemaRest))
                            .contentType(contentType))
                    .andExpect(status().isUnauthorized());

            metadataSchema = metadataSchemaService.find(context, metadataSchema.getID());

            assertEquals(TEST_NAME, metadataSchema.getName());
            assertEquals(TEST_NAMESPACE, metadataSchema.getNamespace());
        } finally {
            deleteMetadataSchemaIfExists(metadataSchema);
        }
    }

    private MetadataSchema createMetadataSchema() throws SQLException, AuthorizeException, NonUniqueMetadataException {
        context.turnOffAuthorisationSystem();
        MetadataSchema metadataSchema = metadataSchemaService.create(context, TEST_NAME, TEST_NAMESPACE);
        context.commit();
        return metadataSchema;
    }

    private void deleteMetadataSchemaIfExists(String name) throws SQLException, AuthorizeException {

        deleteMetadataSchemaIfExists(metadataSchemaService.find(context, name));
    }

    private void deleteMetadataSchemaIfExists(MetadataSchema metadataSchema) throws SQLException, AuthorizeException {

        if (metadataSchema != null) {
            context.turnOffAuthorisationSystem();
            metadataSchemaService.delete(context, metadataSchema);
            context.commit();
        }
    }
}
