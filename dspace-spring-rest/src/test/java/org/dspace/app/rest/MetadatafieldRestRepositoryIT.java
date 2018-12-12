/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
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
import org.dspace.app.rest.builder.MetadataFieldBuilder;
import org.dspace.app.rest.builder.MetadataSchemaBuilder;
import org.dspace.app.rest.matcher.MetadataFieldMatcher;
import org.dspace.app.rest.model.MetadataFieldRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataFieldServiceImpl;
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.content.service.MetadataSchemaService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class MetadatafieldRestRepositoryIT extends AbstractControllerIntegrationTest {

    private static final String ELEMENT = "test element";
    private static final String QUALIFIER = "test qualifier";
    private static final String SCOPE_NOTE = "test scope_note";

    private static final String ELEMENT_UPDATED = "test element updated";
    private static final String QUALIFIER_UPDATED = "test qualifier updated";
    private static final String SCOPE_NOTE_UPDATED = "test scope_note updated";

    private MetadataSchema metadataSchema;

    @Autowired
    private MetadataSchemaService metadataSchemaService;

    @Autowired
    private MetadataFieldServiceImpl metadataFieldService;

    @Before
    public void setup() throws Exception {
        metadataSchema = metadataSchemaService.findAll(context).get(0);
    }

    @Test
    public void findAll() throws Exception {

        context.turnOffAuthorisationSystem();
        MetadataField metadataField = MetadataFieldBuilder
            .createMetadataField(context, "AnElement", "AQualifier", "AScopeNote").build();

        getClient().perform(get("/api/core/metadatafields")
                        .param("size", String.valueOf(100)))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItems(
                           MetadataFieldMatcher.matchMetadataFieldByKeys("dc","title", null),
                           MetadataFieldMatcher.matchMetadataFieldByKeys("dc","date", "issued"))
                   ))
                   .andExpect(jsonPath("$._links.first.href", Matchers.containsString("/api/core/metadatafields")))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/metadatafields")))
                   .andExpect(jsonPath("$._links.next.href", Matchers.containsString("/api/core/metadatafields")))
                   .andExpect(jsonPath("$._links.last.href", Matchers.containsString("/api/core/metadatafields")))

                   .andExpect(jsonPath("$.page.size", is(100)));
    }

    @Test
    public void findOne() throws Exception {

        context.turnOffAuthorisationSystem();
        MetadataField metadataField = MetadataFieldBuilder
            .createMetadataField(context, "AnElement", "AQualifier", "AScopeNote").build();

        getClient().perform(get("/api/core/metadatafields/" + metadataField.getID()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.is(
                       MetadataFieldMatcher.matchMetadataField(metadataField)
                   )));
    }

    @Test
    public void searchMethodsExist() throws Exception {
        getClient().perform(get("/api/core/metadatafields"))
                   .andExpect(jsonPath("$._links.search.href", notNullValue()));

        getClient().perform(get("/api/core/metadatafields/search"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._links.bySchema", notNullValue()));
    }

    @Test
    public void findBySchema() throws Exception {

        context.turnOffAuthorisationSystem();
        MetadataSchema schema = MetadataSchemaBuilder.createMetadataSchema(context, "ASchema",
                "http://www.dspace.org/ns/aschema").build();

        MetadataField metadataField = MetadataFieldBuilder
            .createMetadataField(context, schema, "AnElement", "AQualifier", "AScopeNote").build();

        getClient().perform(get("/api/core/metadatafields/search/bySchema")
                        .param("schema", "dc")
                        .param("size", String.valueOf(100)))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItems(
                       MetadataFieldMatcher.matchMetadataFieldByKeys("dc","title", null),
                       MetadataFieldMatcher.matchMetadataFieldByKeys("dc","date", "issued"))
                   ))
                   .andExpect(jsonPath("$.page.size", is(100)));

        getClient().perform(get("/api/core/metadatafields/search/bySchema")
                .param("schema", schema.getName()))
           .andExpect(status().isOk())
           .andExpect(content().contentType(contentType))
           .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItem(
                   MetadataFieldMatcher.matchMetadataField(metadataField))
           ))
           .andExpect(jsonPath("$.page.size", is(20)))
           .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void findByUndefinedSchema() throws Exception {

        getClient().perform(get("/api/core/metadatafields/search/bySchema")
                        .param("schema", "undefined"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findByNullSchema() throws Exception {

        getClient().perform(get("/api/core/metadatafields/search/bySchema"))
                   .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void createSuccess() throws Exception {

        MetadataFieldRest metadataFieldRest = new MetadataFieldRest();
        metadataFieldRest.setElement(ELEMENT);
        metadataFieldRest.setQualifier(QUALIFIER);
        metadataFieldRest.setScopeNote(SCOPE_NOTE);

        String authToken = getAuthToken(admin.getEmail(), password);
        AtomicReference<Integer> idRef = new AtomicReference<>();

        try {
            assertThat(metadataFieldService.findByElement(context, metadataSchema, ELEMENT, QUALIFIER), nullValue());

            getClient(authToken)
                    .perform(post("/api/core/metadatafields")
                            .param("schemaId", metadataSchema.getID() + "")
                            .content(new ObjectMapper().writeValueAsBytes(metadataFieldRest))
                            .contentType(contentType))
                    .andExpect(status().isCreated())
                    .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            MetadataField metadataField = metadataFieldService.find(context, idRef.get());
            assertThat(metadataField, notNullValue());

            assertEquals(metadataField.getMetadataSchema(), metadataSchema);
            assertEquals(metadataField.getElement(), ELEMENT);
            assertEquals(metadataField.getQualifier(), QUALIFIER);
            assertEquals(metadataField.getScopeNote(), SCOPE_NOTE);

        } finally {
            deleteMetadataFieldIfExists();
        }
    }

    @Test
    public void createUnauthauthorized() throws Exception {

        MetadataFieldRest metadataFieldRest = new MetadataFieldRest();
        metadataFieldRest.setElement(ELEMENT);
        metadataFieldRest.setQualifier(QUALIFIER);
        metadataFieldRest.setScopeNote(SCOPE_NOTE);

        try {
            getClient()
                    .perform(post("/api/core/metadatafields")
                            .param("schemaId", metadataSchema.getID() + "")
                            .content(new ObjectMapper().writeValueAsBytes(metadataFieldRest))
                            .contentType(contentType))
                    .andExpect(status().isUnauthorized());
        } finally {
            deleteMetadataFieldIfExists();
        }
    }

    @Test
    public void deleteSuccess() throws Exception {

        MetadataField metadataField = createMetadataField();

        try {

            assertThat(metadataFieldService.find(context, metadataField.getID()), notNullValue());

            getClient(getAuthToken(admin.getEmail(), password))
                    .perform(delete("/api/core/metadatafields/" + metadataField.getID()))
                    .andExpect(status().isNoContent());

            assertThat(metadataFieldService.find(context, metadataField.getID()), nullValue());

        } finally {
            deleteMetadataFieldIfExists();
        }
    }

    @Test
    public void deleteUnauthorized() throws Exception {

        MetadataField metadataField = createMetadataField();

        try {

            assertThat(metadataFieldService.find(context, metadataField.getID()), notNullValue());

            getClient()
                    .perform(delete("/api/core/metadatafields/" + metadataField.getID()))
                    .andExpect(status().isUnauthorized());

            assertThat(metadataFieldService.find(context, metadataField.getID()), notNullValue());

        } finally {
            deleteMetadataFieldIfExists();
        }
    }

    @Test
    public void deleteNonExisting() throws Exception {

        MetadataField metadataField = createMetadataField();
        deleteMetadataFieldIfExists();

        Integer id = metadataField.getID();
        assertThat(metadataFieldService.find(context, id), nullValue());

        getClient(getAuthToken(admin.getEmail(), password))
                .perform(delete("/api/core/metadatafields/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    public void update() throws Exception {

        MetadataField metadataField = createMetadataField();

        MetadataFieldRest metadataFieldRest = new MetadataFieldRest();
        metadataFieldRest.setId(metadataField.getID());
        metadataFieldRest.setElement(ELEMENT_UPDATED);
        metadataFieldRest.setQualifier(QUALIFIER_UPDATED);
        metadataFieldRest.setScopeNote(SCOPE_NOTE_UPDATED);

        try {
            getClient(getAuthToken(admin.getEmail(), password))
                    .perform(put("/api/core/metadatafields/" + metadataField.getID())
                            .content(new ObjectMapper().writeValueAsBytes(metadataFieldRest))
                            .contentType(contentType))
                    .andExpect(status().isOk());

            metadataField = metadataFieldService.find(context, metadataField.getID());

            assertEquals(ELEMENT_UPDATED, metadataField.getElement());
            assertEquals(QUALIFIER_UPDATED, metadataField.getQualifier());
            assertEquals(SCOPE_NOTE_UPDATED, metadataField.getScopeNote());
        } finally {
            deleteMetadataFieldIfExists(metadataField);
        }
    }

    @Test
    public void updateUnauthorized() throws Exception {

        MetadataField metadataField = createMetadataField();

        MetadataFieldRest metadataFieldRest = new MetadataFieldRest();
        metadataFieldRest.setId(metadataField.getID());
        metadataFieldRest.setElement(ELEMENT_UPDATED);
        metadataFieldRest.setQualifier(QUALIFIER_UPDATED);
        metadataFieldRest.setScopeNote(SCOPE_NOTE_UPDATED);

        try {
            getClient()
                    .perform(put("/api/core/metadatafields/" + metadataField.getID())
                            .content(new ObjectMapper().writeValueAsBytes(metadataFieldRest))
                            .contentType(contentType))
                    .andExpect(status().isUnauthorized());

            metadataField = metadataFieldService.find(context, metadataField.getID());

            assertEquals(ELEMENT, metadataField.getElement());
            assertEquals(QUALIFIER, metadataField.getQualifier());
            assertEquals(SCOPE_NOTE, metadataField.getScopeNote());
        } finally {
            deleteMetadataFieldIfExists(metadataField);
        }
    }

    private MetadataField createMetadataField() throws AuthorizeException, SQLException, NonUniqueMetadataException {
        context.turnOffAuthorisationSystem();
        MetadataField metadataField = metadataFieldService.create(
                context, metadataSchema, ELEMENT, QUALIFIER, SCOPE_NOTE
        );
        context.commit();
        return metadataField;
    }

    private void deleteMetadataFieldIfExists() throws SQLException, AuthorizeException {

        deleteMetadataFieldIfExists(metadataFieldService.findByElement(context, metadataSchema, ELEMENT, QUALIFIER));
    }

    private void deleteMetadataFieldIfExists(MetadataField metadataField) throws SQLException, AuthorizeException {
        if (metadataField != null) {
            context.turnOffAuthorisationSystem();
            metadataFieldService.delete(context, metadataField);
            context.commit();
        }
    }
}
