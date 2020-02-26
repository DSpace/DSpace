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
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.builder.MetadataFieldBuilder;
import org.dspace.app.rest.builder.MetadataSchemaBuilder;
import org.dspace.app.rest.matcher.MetadataFieldMatcher;
import org.dspace.app.rest.model.MetadataFieldRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataFieldServiceImpl;
import org.dspace.content.MetadataSchema;
import org.dspace.content.service.MetadataSchemaService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for the {@link org.dspace.app.rest.repository.MetadataFieldRestRepository}
 * This class will include all the tests for the logic with regards to the
 * {@link org.dspace.app.rest.repository.MetadataFieldRestRepository}
 */
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
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/metadatafields")
                                .param("size", String.valueOf(100)))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItems(
                       MetadataFieldMatcher.matchMetadataFieldByKeys("dc", "title", null),
                       MetadataFieldMatcher.matchMetadataFieldByKeys("dc", "date", "issued"))
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
        context.restoreAuthSystemState();

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
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/metadatafields/search/bySchema")
                                .param("schema", "dc")
                                .param("size", String.valueOf(100)))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItems(
                       MetadataFieldMatcher.matchMetadataFieldByKeys("dc", "title", null),
                       MetadataFieldMatcher.matchMetadataFieldByKeys("dc", "date", "issued"))
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
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void createSuccess() throws Exception {

        MetadataFieldRest metadataFieldRest = new MetadataFieldRest();
        metadataFieldRest.setElement("testElementForCreate");
        metadataFieldRest.setQualifier("testQualifierForCreate");
        metadataFieldRest.setScopeNote(SCOPE_NOTE);

        String authToken = getAuthToken(admin.getEmail(), password);
        AtomicReference<Integer> idRef = new AtomicReference<>();

        assertThat(metadataFieldService.findByElement(context, metadataSchema, ELEMENT, QUALIFIER), nullValue());

        getClient(authToken)
            .perform(post("/api/core/metadatafields")
                         .param("schemaId", metadataSchema.getID() + "")
                         .content(new ObjectMapper().writeValueAsBytes(metadataFieldRest))
                         .contentType(contentType))
            .andExpect(status().isCreated())
            .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        getClient(authToken).perform(get("/api/core/metadatafields/" + idRef.get()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", MetadataFieldMatcher.matchMetadataFieldByKeys(
                                metadataSchema.getName(), "testElementForCreate", "testQualifierForCreate")));
    }

    @Test
    public void createUnauthorized() throws Exception {

        MetadataFieldRest metadataFieldRest = new MetadataFieldRest();
        metadataFieldRest.setElement(ELEMENT);
        metadataFieldRest.setQualifier(QUALIFIER);
        metadataFieldRest.setScopeNote(SCOPE_NOTE);

        getClient()
            .perform(post("/api/core/metadatafields")
                         .param("schemaId", metadataSchema.getID() + "")
                         .content(new ObjectMapper().writeValueAsBytes(metadataFieldRest))
                         .contentType(contentType))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void createUnauthorizedEPersonNoAdminRights() throws Exception {

        MetadataFieldRest metadataFieldRest = new MetadataFieldRest();
        metadataFieldRest.setElement(ELEMENT);
        metadataFieldRest.setQualifier(QUALIFIER);
        metadataFieldRest.setScopeNote(SCOPE_NOTE);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token)
            .perform(post("/api/core/metadatafields")
                         .param("schemaId", metadataSchema.getID() + "")
                         .content(new ObjectMapper().writeValueAsBytes(metadataFieldRest))
                         .contentType(contentType))
            .andExpect(status().isForbidden());
    }

    @Test
    public void deleteSuccess() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataField metadataField = MetadataFieldBuilder.createMetadataField(context, ELEMENT, QUALIFIER, SCOPE_NOTE)
                                                          .build();
        context.restoreAuthSystemState();


        getClient().perform(get("/api/core/metadatafields/" + metadataField.getID()))
                   .andExpect(status().isOk());
        getClient(getAuthToken(admin.getEmail(), password))
            .perform(delete("/api/core/metadatafields/" + metadataField.getID()))
            .andExpect(status().isNoContent());

        getClient().perform(get("/api/core/metadatafields/" + metadataField.getID()))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void deleteUnauthorized() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataField metadataField = MetadataFieldBuilder.createMetadataField(context, ELEMENT, QUALIFIER, SCOPE_NOTE)
                                                          .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/metadatafields/" + metadataField.getID()))
                   .andExpect(status().isOk());
        getClient()
            .perform(delete("/api/core/metadatafields/" + metadataField.getID()))
            .andExpect(status().isUnauthorized());

        getClient().perform(get("/api/core/metadatafields/" + metadataField.getID()))
                   .andExpect(status().isOk());
    }

    @Test
    public void deleteUnauthorizedEPersonNoAdminRights() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataField metadataField = MetadataFieldBuilder.createMetadataField(context, ELEMENT, QUALIFIER, SCOPE_NOTE)
                                                          .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);


        getClient().perform(get("/api/core/metadatafields/" + metadataField.getID()))
                   .andExpect(status().isOk());
        getClient(token)
            .perform(delete("/api/core/metadatafields/" + metadataField.getID()))
            .andExpect(status().isForbidden());

        getClient().perform(get("/api/core/metadatafields/" + metadataField.getID()))
                   .andExpect(status().isOk());
    }


    @Test
    public void deleteNonExisting() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataField metadataField = MetadataFieldBuilder.createMetadataField(context, ELEMENT, QUALIFIER, SCOPE_NOTE)
                                                          .build();

        context.restoreAuthSystemState();

        Integer id = metadataField.getID();
        getClient(getAuthToken(admin.getEmail(), password))
            .perform(delete("/api/core/metadatafields/" + id))
            .andExpect(status().isNoContent());

        assertThat(metadataFieldService.find(context, id), nullValue());

        getClient(getAuthToken(admin.getEmail(), password))
            .perform(delete("/api/core/metadatafields/" + id))
            .andExpect(status().isNotFound());
    }

    @Test
    public void update() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataField metadataField = MetadataFieldBuilder.createMetadataField(context, ELEMENT, QUALIFIER, SCOPE_NOTE)
                                                          .build();

        context.restoreAuthSystemState();

        MetadataFieldRest metadataFieldRest = new MetadataFieldRest();
        metadataFieldRest.setId(metadataField.getID());
        metadataFieldRest.setElement(ELEMENT_UPDATED);
        metadataFieldRest.setQualifier(QUALIFIER_UPDATED);
        metadataFieldRest.setScopeNote(SCOPE_NOTE_UPDATED);

        getClient(getAuthToken(admin.getEmail(), password))
            .perform(put("/api/core/metadatafields/" + metadataField.getID())
                         .content(new ObjectMapper().writeValueAsBytes(metadataFieldRest))
                         .contentType(contentType))
            .andExpect(status().isOk());

        getClient().perform(get("/api/core/metadatafields/" + metadataField.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", MetadataFieldMatcher.matchMetadataFieldByKeys(
                       metadataSchema.getName(), ELEMENT_UPDATED, QUALIFIER_UPDATED)
                   ));
    }

    @Test
    public void updateUnauthorized() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataField metadataField = MetadataFieldBuilder.createMetadataField(context, ELEMENT, QUALIFIER, SCOPE_NOTE)
                                                          .build();

        context.restoreAuthSystemState();

        MetadataFieldRest metadataFieldRest = new MetadataFieldRest();
        metadataFieldRest.setId(metadataField.getID());
        metadataFieldRest.setElement(ELEMENT_UPDATED);
        metadataFieldRest.setQualifier(QUALIFIER_UPDATED);
        metadataFieldRest.setScopeNote(SCOPE_NOTE_UPDATED);

        getClient()
            .perform(put("/api/core/metadatafields/" + metadataField.getID())
                         .content(new ObjectMapper().writeValueAsBytes(metadataFieldRest))
                         .contentType(contentType))
            .andExpect(status().isUnauthorized());

        getClient().perform(get("/api/core/metadatafields/" + metadataField.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", MetadataFieldMatcher.matchMetadataFieldByKeys(
                       metadataSchema.getName(), ELEMENT, QUALIFIER)
                   ));


    }

    @Test
    public void updateWrongRights() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataField metadataField = MetadataFieldBuilder.createMetadataField(context, ELEMENT, QUALIFIER, SCOPE_NOTE)
                                                          .build();

        context.restoreAuthSystemState();

        MetadataFieldRest metadataFieldRest = new MetadataFieldRest();
        metadataFieldRest.setId(metadataField.getID());
        metadataFieldRest.setElement(ELEMENT_UPDATED);
        metadataFieldRest.setQualifier(QUALIFIER_UPDATED);
        metadataFieldRest.setScopeNote(SCOPE_NOTE_UPDATED);

        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(put("/api/core/metadatafields/" + metadataField.getID())
                         .content(new ObjectMapper().writeValueAsBytes(metadataFieldRest))
                         .contentType(contentType))
            .andExpect(status().isForbidden());

        getClient().perform(get("/api/core/metadatafields/" + metadataField.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", MetadataFieldMatcher.matchMetadataFieldByKeys(
                       metadataSchema.getName(), ELEMENT, QUALIFIER)
                   ));


    }


}
