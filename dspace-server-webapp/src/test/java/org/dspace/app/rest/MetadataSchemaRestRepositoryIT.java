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
import org.dspace.app.rest.converter.MetadataSchemaConverter;
import org.dspace.app.rest.matcher.HalMatcher;
import org.dspace.app.rest.matcher.MetadataschemaMatcher;
import org.dspace.app.rest.model.MetadataSchemaRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.MetadataSchemaBuilder;
import org.dspace.content.MetadataSchema;
import org.dspace.content.factory.ContentServiceFactory;
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
    private MetadataSchemaConverter metadataSchemaConverter;
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

        MetadataSchemaRest metadataSchemaRest = metadataSchemaConverter.convert(metadataSchema, Projection.DEFAULT);
        metadataSchemaRest.setPrefix(TEST_NAME);
        metadataSchemaRest.setNamespace(TEST_NAMESPACE);

        String authToken = getAuthToken(admin.getEmail(), password);
        AtomicReference<Integer> idRef = new AtomicReference<>();


        try {
            getClient(authToken)
                    .perform(post("/api/core/metadataschemas")
                            .content(new ObjectMapper().writeValueAsBytes(metadataSchemaRest))
                            .contentType(contentType))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
                    .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient().perform(get("/api/core/metadataschemas/" + idRef.get()))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$", MetadataschemaMatcher.matchEntry(TEST_NAME, TEST_NAMESPACE)));
        } finally {
            MetadataSchemaBuilder.deleteMetadataSchema(idRef.get());
        }
    }

    @Test
    public void createUnprocessableEntity_prefixContainingInvalidCharacters() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataSchema metadataSchema = MetadataSchemaBuilder.createMetadataSchema(context, "ATest", "ANamespace")
            .build();
        context.restoreAuthSystemState();

        MetadataSchemaRest metadataSchemaRest = metadataSchemaConverter.convert(metadataSchema, Projection.DEFAULT);
        metadataSchemaRest.setPrefix("test.SchemaName");
        metadataSchemaRest.setNamespace(TEST_NAMESPACE);

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken)
            .perform(post("/api/core/metadataschemas")
                         .content(new ObjectMapper().writeValueAsBytes(metadataSchemaRest))
                         .contentType(contentType))
            .andExpect(status().isUnprocessableEntity());

        metadataSchemaRest.setPrefix("test,SchemaName");
        getClient(authToken)
            .perform(post("/api/core/metadataschemas")
                         .content(new ObjectMapper().writeValueAsBytes(metadataSchemaRest))
                         .contentType(contentType))
            .andExpect(status().isUnprocessableEntity());

        metadataSchemaRest.setPrefix("test SchemaName");
        getClient(authToken)
            .perform(post("/api/core/metadataschemas")
                         .content(new ObjectMapper().writeValueAsBytes(metadataSchemaRest))
                         .contentType(contentType))
            .andExpect(status().isUnprocessableEntity());
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
        metadataSchemaRest.setPrefix(TEST_NAME);
        metadataSchemaRest.setNamespace(TEST_NAMESPACE_UPDATED);

        getClient(getAuthToken(admin.getEmail(), password))
                .perform(put("/api/core/metadataschemas/" + metadataSchema.getID())
                        .content(new ObjectMapper().writeValueAsBytes(metadataSchemaRest))
                        .contentType(contentType))
                .andExpect(status().isOk());

        getClient().perform(get("/api/core/metadataschemas/" + metadataSchema.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", MetadataschemaMatcher
                       .matchEntry(TEST_NAME, TEST_NAMESPACE_UPDATED)));
    }

    @Test
    public void update_schemaNameShouldThrowError() throws Exception {
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
            .andExpect(status().isUnprocessableEntity());

        getClient().perform(get("/api/core/metadataschemas/" + metadataSchema.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", MetadataschemaMatcher
                .matchEntry(TEST_NAME, TEST_NAMESPACE)));
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

    @Test
    public void findAllPaginationTest() throws Exception {

        // Determine number of schemas from database
        int numberOfSchema = ContentServiceFactory.getInstance()
                                                   .getMetadataSchemaService().findAll(context).size();
        // If we return 6 schema per page, determine number of pages we expect
        int pageSize = 6;
        int numberOfPages = (int) Math.ceil((double) numberOfSchema / pageSize);

        // In these tests we just validate the first 3 pages, as we currently have at least that many schema
        getClient().perform(get("/api/core/metadataschemas")
                   .param("size", String.valueOf(pageSize))
                   .param("page", "0"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.metadataschemas", Matchers.hasItem(matchEntry())))
                   .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadataschemas?"),
                           Matchers.containsString("page=0"), Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadataschemas?"),
                           Matchers.containsString("page=0"), Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadataschemas?"),
                           Matchers.containsString("page=1"), Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadataschemas?"),
                           Matchers.containsString("page=" + (numberOfPages - 1)),
                           Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$.page.totalElements", is(numberOfSchema)))
                   .andExpect(jsonPath("$.page.totalPages", is(numberOfPages)))
                   .andExpect(jsonPath("$.page.size", is(pageSize)));

        getClient().perform(get("/api/core/metadataschemas")
                   .param("size", String.valueOf(pageSize))
                   .param("page", "1"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.metadataschemas", Matchers.hasItem(matchEntry())))
                   .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadataschemas?"),
                           Matchers.containsString("page=0"), Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$._links.prev.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadataschemas?"),
                           Matchers.containsString("page=0"), Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadataschemas?"),
                           Matchers.containsString("page=1"), Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadataschemas?"),
                           Matchers.containsString("page=2"), Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadataschemas?"),
                           Matchers.containsString("page=" + (numberOfPages - 1)),
                           Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$.page.totalElements", is(numberOfSchema)))
                   .andExpect(jsonPath("$.page.totalPages", is(numberOfPages)))
                   .andExpect(jsonPath("$.page.size", is(pageSize)));

        getClient().perform(get("/api/core/metadataschemas")
                   .param("size", String.valueOf(pageSize))
                   .param("page", "2"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.metadataschemas", Matchers.hasItem(matchEntry())))
                   .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadataschemas?"),
                           Matchers.containsString("page=0"), Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$._links.prev.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadataschemas?"),
                           Matchers.containsString("page=1"), Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadataschemas?"),
                           Matchers.containsString("page=2"), Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadataschemas?"),
                           Matchers.containsString("page=" + (numberOfPages - 1)),
                           Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$.page.totalElements", is(numberOfSchema)))
                   .andExpect(jsonPath("$.page.totalPages", is(numberOfPages)))
                   .andExpect(jsonPath("$.page.size", is(pageSize)));

    }

}
