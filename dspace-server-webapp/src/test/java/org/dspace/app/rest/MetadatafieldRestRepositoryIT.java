/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.matcher.MetadataFieldMatcher;
import org.dspace.app.rest.model.MetadataFieldRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.MetadataFieldBuilder;
import org.dspace.builder.MetadataSchemaBuilder;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataFieldServiceImpl;
import org.dspace.content.MetadataSchema;
import org.dspace.content.factory.ContentServiceFactory;
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

    public static final String METADATAFIELDS_ENDPOINT = "/api/core/metadatafields/";
    private static final String SEARCH_BYFIELDNAME_ENDPOINT = METADATAFIELDS_ENDPOINT + "search/byFieldName";

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
    public void findByFieldName_schema() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataSchema schema = MetadataSchemaBuilder.createMetadataSchema(context, "ASchema",
            "http://www.dspace.org/ns/aschema").build();

        MetadataField metadataField = MetadataFieldBuilder
            .createMetadataField(context, schema, "AnElement", "AQualifier", "AScopeNote").build();

        context.restoreAuthSystemState();

        getClient().perform(get(SEARCH_BYFIELDNAME_ENDPOINT)
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
    public void findByFieldName_element() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataSchema schema = MetadataSchemaBuilder.createMetadataSchema(context, "ASchema",
            "http://www.dspace.org/ns/aschema").build();
        MetadataSchema schema2 = MetadataSchemaBuilder.createMetadataSchema(context, "ASchema2",
            "http://www.dspace.org/ns/aschema2").build();

        MetadataField metadataField = MetadataFieldBuilder
            .createMetadataField(context, schema, "AnElement", "AQualifier", "AScopeNote").build();

        MetadataField metadataField2 = MetadataFieldBuilder
            .createMetadataField(context, schema2, "AnElement", "AQualifier2", "AScopeNote2").build();

        context.restoreAuthSystemState();

        getClient().perform(get(SEARCH_BYFIELDNAME_ENDPOINT)
            .param("element", "AnElement"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField))
                                      ))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField2))
                                      ))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    public void findByFieldName_elementAndQualifier() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataSchema schema = MetadataSchemaBuilder.createMetadataSchema(context, "ASchema",
            "http://www.dspace.org/ns/aschema").build();
        MetadataSchema schema2 = MetadataSchemaBuilder.createMetadataSchema(context, "ASchema2",
            "http://www.dspace.org/ns/aschema2").build();

        MetadataField metadataField = MetadataFieldBuilder
            .createMetadataField(context, schema, "AnElement1", "AQualifier", "AScopeNote").build();

        MetadataField metadataField2 = MetadataFieldBuilder
            .createMetadataField(context, schema2, "AnElement2", "AQualifier", "AScopeNote2").build();

        MetadataField metadataField3 = MetadataFieldBuilder
            .createMetadataField(context, schema, "AnElement2", "AQualifier", "AScopeNote2").build();

        context.restoreAuthSystemState();

        getClient().perform(get(SEARCH_BYFIELDNAME_ENDPOINT)
            .param("element", "AnElement2")
            .param("qualifier", "AQualifier"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField2))
                                      ))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField3))
                                      ))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.not(hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField))
                                                                                 )))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    public void findByFieldName_schemaAndQualifier() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataSchema schema = MetadataSchemaBuilder.createMetadataSchema(context, "ASchema",
            "http://www.dspace.org/ns/aschema").build();
        MetadataSchema schema2 = MetadataSchemaBuilder.createMetadataSchema(context, "ASchema2",
            "http://www.dspace.org/ns/aschema2").build();

        MetadataField metadataField = MetadataFieldBuilder
            .createMetadataField(context, schema, "AnElement1", "AQualifier", "AScopeNote").build();

        MetadataField metadataField2 = MetadataFieldBuilder
            .createMetadataField(context, schema2, "AnElement2", "AQualifier", "AScopeNote2").build();

        MetadataField metadataField3 = MetadataFieldBuilder
            .createMetadataField(context, schema, "AnElement3", "AQualifier", "AScopeNote3").build();

        context.restoreAuthSystemState();

        getClient().perform(get(SEARCH_BYFIELDNAME_ENDPOINT)
            .param("schema", schema.getName())
            .param("qualifier", "AQualifier"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField))
                                      ))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField3))
                                      ))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.not(hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField2))
                                                                                 )))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    public void findByFieldName_schemaElementAndQualifier() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataSchema schema = MetadataSchemaBuilder.createMetadataSchema(context, "ASchema",
            "http://www.dspace.org/ns/aschema").build();
        MetadataSchema schema2 = MetadataSchemaBuilder.createMetadataSchema(context, "ASchema2",
            "http://www.dspace.org/ns/aschema2").build();

        MetadataField metadataField = MetadataFieldBuilder
            .createMetadataField(context, schema, "AnElement1", "AQualifier", "AScopeNote").build();

        MetadataField metadataField2 = MetadataFieldBuilder
            .createMetadataField(context, schema2, "AnElement2", "AQualifier", "AScopeNote2").build();

        MetadataField metadataField3 = MetadataFieldBuilder
            .createMetadataField(context, schema, "AnElement3", "AQualifier", "AScopeNote3").build();

        context.restoreAuthSystemState();

        getClient().perform(get(SEARCH_BYFIELDNAME_ENDPOINT)
            .param("schema", schema.getName())
            .param("element", metadataField3.getElement())
            .param("qualifier", metadataField3.getQualifier()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.not(hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField))
                                                                                 )))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.not(hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField2))
                                                                                 )))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField3))
                                      ))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void findByFieldName_query() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataSchema schema = MetadataSchemaBuilder.createMetadataSchema(context, "ASchema",
            "http://www.dspace.org/ns/aschema").build();
        MetadataSchema schema2 = MetadataSchemaBuilder.createMetadataSchema(context, "ASchema2",
            "http://www.dspace.org/ns/aschema2").build();

        MetadataField metadataField = MetadataFieldBuilder
            .createMetadataField(context, schema, "AnElement1", "AQualifier", "AScopeNote").build();

        MetadataField metadataField2 = MetadataFieldBuilder
            .createMetadataField(context, schema2, "AnElement2", "AQualifier", "AScopeNote2").build();

        MetadataField metadataField3 = MetadataFieldBuilder
            .createMetadataField(context, schema, "AnElement3", "AQualifier", "AScopeNote2").build();

        context.restoreAuthSystemState();

        getClient().perform(get(SEARCH_BYFIELDNAME_ENDPOINT)
            .param("query", schema.getName()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField))
                                      ))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField3))
                                      ))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField2))
                                      ))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(3)));

        getClient().perform(get(SEARCH_BYFIELDNAME_ENDPOINT)
            .param("query", schema.getName() + ".AnElement3"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.not(hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField))
                                                                                 )))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField3))
                                      ))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.not(hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField2))
                                                                                 )))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(1)));

        getClient().perform(get(SEARCH_BYFIELDNAME_ENDPOINT)
            .param("query", "AnElement3.AQual"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.not(hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField))
                                                                                 )))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField3))
                                      ))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.not(hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField2))
                                                                                 )))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void findByFieldName_query_noQualifier() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataSchema schema = MetadataSchemaBuilder.createMetadataSchema(context, "ASchema",
            "http://www.dspace.org/ns/aschema").build();
        MetadataSchema schema2 = MetadataSchemaBuilder.createMetadataSchema(context, "test",
            "http://www.dspace.org/ns/aschema2").build();

        MetadataField metadataField = MetadataFieldBuilder
            .createMetadataField(context, schema, "AnElement1", null, "AScopeNote").build();

        MetadataField metadataField2 = MetadataFieldBuilder
            .createMetadataField(context, schema2, "AnElement2", null, "AScopeNote2").build();

        MetadataField metadataField3 = MetadataFieldBuilder
            .createMetadataField(context, schema, "test", null, "AScopeNote2").build();

        context.restoreAuthSystemState();

        getClient().perform(get(SEARCH_BYFIELDNAME_ENDPOINT)
            .param("query", "test"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.not(hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField))
                                                                                 )))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField3))
                                      ))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField2))
                                      ))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    public void findByFieldName_invalidQuery() throws Exception {
        getClient().perform(get(SEARCH_BYFIELDNAME_ENDPOINT)
            .param("query", "schema.element.qualifier.morestuff"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void findByFieldName_exactName() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataSchema schema = MetadataSchemaBuilder.createMetadataSchema(context, "ASchema",
            "http://www.dspace.org/ns/aschema").build();
        MetadataSchema schema2 = MetadataSchemaBuilder.createMetadataSchema(context, "test",
            "http://www.dspace.org/ns/aschema2").build();

        MetadataField metadataField = MetadataFieldBuilder
            .createMetadataField(context, schema, "AnElement1", null, "AScopeNote").build();

        MetadataField metadataField2 = MetadataFieldBuilder
            .createMetadataField(context, schema2, "AnElement2", null, "AScopeNote2").build();

        MetadataField metadataField3 = MetadataFieldBuilder
            .createMetadataField(context, schema, "test", null, "AScopeNote2").build();

        context.restoreAuthSystemState();

        getClient().perform(get(SEARCH_BYFIELDNAME_ENDPOINT)
            .param("exactName", metadataField.toString('.')))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField))
                                                                                 ))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.not(hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField3))
                                      )))
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.not(hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField2))
                                      )))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void findByFieldName_exactName_NoResult() throws Exception {
        getClient().perform(get(SEARCH_BYFIELDNAME_ENDPOINT)
            .param("exactName", "not.valid.mdstring"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findByFieldName_exactName_combinedDiscoveryQueryParams_query() throws Exception {
        context.turnOffAuthorisationSystem();
        MetadataSchema schema = MetadataSchemaBuilder.createMetadataSchema(context, "ASchema",
            "http://www.dspace.org/ns/aschema").build();
        MetadataField metadataField = MetadataFieldBuilder
            .createMetadataField(context, schema, "AnElement1", null, "AScopeNote").build();
        context.restoreAuthSystemState();

        getClient().perform(get(SEARCH_BYFIELDNAME_ENDPOINT)
            .param("exactName", metadataField.toString('.'))
            .param("query", "query"))
                   .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void findByFieldName_exactName_combinedDiscoveryQueryParams_schema() throws Exception {
        context.turnOffAuthorisationSystem();
        MetadataSchema schema = MetadataSchemaBuilder.createMetadataSchema(context, "ASchema",
            "http://www.dspace.org/ns/aschema").build();
        MetadataField metadataField = MetadataFieldBuilder
            .createMetadataField(context, schema, "AnElement1", null, "AScopeNote").build();
        context.restoreAuthSystemState();

        getClient().perform(get(SEARCH_BYFIELDNAME_ENDPOINT)
            .param("exactName", metadataField.toString('.'))
            .param("schema", "schema"))
                   .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void findByFieldName_exactName_combinedDiscoveryQueryParams_element() throws Exception {
        context.turnOffAuthorisationSystem();
        MetadataSchema schema = MetadataSchemaBuilder.createMetadataSchema(context, "ASchema",
            "http://www.dspace.org/ns/aschema").build();
        MetadataField metadataField = MetadataFieldBuilder
            .createMetadataField(context, schema, "AnElement1", null, "AScopeNote").build();
        context.restoreAuthSystemState();

        getClient().perform(get(SEARCH_BYFIELDNAME_ENDPOINT)
            .param("exactName", metadataField.toString('.'))
            .param("element", "element"))
                   .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void findByFieldName_exactName_combinedDiscoveryQueryParams_qualifier() throws Exception {
        context.turnOffAuthorisationSystem();
        MetadataSchema schema = MetadataSchemaBuilder.createMetadataSchema(context, "ASchema",
            "http://www.dspace.org/ns/aschema").build();
        MetadataField metadataField = MetadataFieldBuilder
            .createMetadataField(context, schema, "AnElement1", null, "AScopeNote").build();
        context.restoreAuthSystemState();

        getClient().perform(get(SEARCH_BYFIELDNAME_ENDPOINT)
            .param("exactName", metadataField.toString('.'))
            .param("qualifier", "qualifier"))
                   .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void createSuccess() throws Exception {

        MetadataFieldRest metadataFieldRest = new MetadataFieldRest();
        metadataFieldRest.setElement("testElementForCreate");
        metadataFieldRest.setQualifier("testQualifierForCreate");
        metadataFieldRest.setScopeNote(SCOPE_NOTE);

        String authToken = getAuthToken(admin.getEmail(), password);
        AtomicReference<Integer> idRef = new AtomicReference<>();
        try {
            assertThat(metadataFieldService.findByElement(context, metadataSchema, ELEMENT, QUALIFIER), nullValue());

            getClient(authToken)
                .perform(post("/api/core/metadatafields")
                    .param("schemaId", metadataSchema.getID() + "")
                    .param("projection", "full")
                    .content(new ObjectMapper().writeValueAsBytes(metadataFieldRest))
                    .contentType(contentType))
                .andExpect(status().isCreated())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(authToken).perform(get("/api/core/metadatafields/" + idRef.get()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", MetadataFieldMatcher.matchMetadataFieldByKeys(
                                    metadataSchema.getName(), "testElementForCreate", "testQualifierForCreate")));
        } finally {
            MetadataFieldBuilder.deleteMetadataField(idRef.get());
        }
    }

    @Test
    public void createBlankQualifier() throws Exception {

        MetadataFieldRest metadataFieldRest = new MetadataFieldRest();
        metadataFieldRest.setElement(ELEMENT);
        metadataFieldRest.setQualifier("");
        metadataFieldRest.setScopeNote(SCOPE_NOTE);

        String authToken = getAuthToken(admin.getEmail(), password);
        Integer id = null;
        try {
            assertThat(metadataFieldService.findByElement(context, metadataSchema, ELEMENT, null), nullValue());

            id = read(
                    getClient(authToken)
                            .perform(post("/api/core/metadatafields")
                                    .param("schemaId", metadataSchema.getID() + "")
                                    .content(new ObjectMapper().writeValueAsBytes(metadataFieldRest))
                                    .contentType(contentType))
                            .andExpect(status().isCreated())
                            .andReturn().getResponse().getContentAsString(),
                    "$.id"
            );

            getClient(authToken).perform(get("/api/core/metadatafields/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", MetadataFieldMatcher.matchMetadataFieldByKeys(
                            metadataSchema.getName(), ELEMENT, null)));
        } finally {
            if (id != null) {
                MetadataFieldBuilder.deleteMetadataField(id);
            }
        }
    }

    @Test
    public void create_checkAddedToIndex() throws Exception {

        MetadataFieldRest metadataFieldRest = new MetadataFieldRest();
        metadataFieldRest.setElement("testElementForCreate");
        metadataFieldRest.setQualifier("testQualifierForCreate");
        metadataFieldRest.setScopeNote(SCOPE_NOTE);

        String authToken = getAuthToken(admin.getEmail(), password);
        AtomicReference<Integer> idRef = new AtomicReference<>();
        try {
            assertThat(metadataFieldService.findByElement(context, metadataSchema, ELEMENT, QUALIFIER), nullValue());

            getClient(authToken)
                .perform(post("/api/core/metadatafields")
                    .param("schemaId", metadataSchema.getID() + "")
                    .param("projection", "full")
                    .content(new ObjectMapper().writeValueAsBytes(metadataFieldRest))
                    .contentType(contentType))
                .andExpect(status().isCreated())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(authToken).perform(get("/api/core/metadatafields/" + idRef.get()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", MetadataFieldMatcher.matchMetadataFieldByKeys(
                                    metadataSchema.getName(), "testElementForCreate", "testQualifierForCreate")));

            // new metadata field found in index
            getClient().perform(get(SEARCH_BYFIELDNAME_ENDPOINT)
                .param("schema", metadataSchema.getName())
                .param("element", metadataFieldRest.getElement())
                .param("qualifier", metadataFieldRest.getQualifier()))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItem(
                           MetadataFieldMatcher.matchMetadataFieldByKeys(metadataSchema.getName(),
                               metadataFieldRest.getElement(), metadataFieldRest.getQualifier()))
                                          ))
                       .andExpect(jsonPath("$.page.totalElements", is(1)));
        } finally {
            MetadataFieldBuilder.deleteMetadataField(idRef.get());
        }
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
    public void delete_checkDeletedFromIndex() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataSchema schema = MetadataSchemaBuilder.createMetadataSchema(context, "ASchema",
            "http://www.dspace.org/ns/aschema").build();

        MetadataField metadataField = MetadataFieldBuilder.createMetadataField(context, schema, ELEMENT, QUALIFIER,
            SCOPE_NOTE).build();

        context.restoreAuthSystemState();

        Integer id = metadataField.getID();

        getClient().perform(get(SEARCH_BYFIELDNAME_ENDPOINT)
            .param("schema", schema.getName())
            .param("element", metadataField.getElement())
            .param("qualifier", metadataField.getQualifier()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItem(
                       MetadataFieldMatcher.matchMetadataField(metadataField))
                                      ));

        getClient(getAuthToken(admin.getEmail(), password))
            .perform(delete("/api/core/metadatafields/" + id))
            .andExpect(status().isNoContent());

        assertThat(metadataFieldService.find(context, id), nullValue());

        // deleted metadata field not found in index
        getClient().perform(get(SEARCH_BYFIELDNAME_ENDPOINT)
            .param("schema", schema.getName())
            .param("element", metadataField.getElement())
            .param("qualifier", metadataField.getQualifier()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)));
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
    public void update_checkUpdatedInIndex() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataField metadataField = MetadataFieldBuilder.createMetadataField(context, ELEMENT, QUALIFIER, SCOPE_NOTE)
                                                          .build();

        getClient().perform(get(SEARCH_BYFIELDNAME_ENDPOINT)
            .param("schema", metadataSchema.getName())
            .param("element", metadataField.getElement())
            .param("qualifier", metadataField.getQualifier()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItem(
                       MetadataFieldMatcher.matchMetadataFieldByKeys(metadataSchema.getName(),
                           metadataField.getElement(), metadataField.getQualifier()))
                                      ))
                   .andExpect(jsonPath("$.page.totalElements", is(1)));

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

        // new metadata field found in index
        getClient().perform(get(SEARCH_BYFIELDNAME_ENDPOINT)
            .param("schema", metadataSchema.getName())
            .param("element", ELEMENT_UPDATED)
            .param("qualifier", QUALIFIER_UPDATED))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItem(
                       MetadataFieldMatcher.matchMetadataFieldByKeys(metadataSchema.getName(),
                           ELEMENT_UPDATED, QUALIFIER_UPDATED))
                                      ))
                   .andExpect(jsonPath("$.page.totalElements", is(1)));

        // original metadata field not found in index
        getClient().perform(get(SEARCH_BYFIELDNAME_ENDPOINT)
            .param("schema", metadataSchema.getName())
            .param("element", metadataField.getElement())
            .param("qualifier", metadataField.getQualifier()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)));
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

    @Test
    public void findAllPaginationTest() throws Exception {
        List<MetadataField> alphabeticMdFields =
            ContentServiceFactory.getInstance()
                                 .getMetadataFieldService()
                                 .findAll(context).stream()
                                 .sorted(Comparator.comparing(mdf -> mdf.toString('.')))
                                 .collect(Collectors.toList());
        int numberOfMdFields = alphabeticMdFields.size();

        // If we return 3 fields per page, determine number of pages we expect
        int pageSize = 3;
        int numberOfPages = (int) Math.ceil((double) numberOfMdFields / pageSize);

        // Check first page
        getClient().perform(get("/api/core/metadatafields")
                   .param("size", String.valueOf(pageSize))
                   .param("page", "0"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   // Metadata fields are returned alphabetically. So, look for the first 3 alphabetically
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItems(
                              MetadataFieldMatcher.matchMetadataField(alphabeticMdFields.get(0)),
                              MetadataFieldMatcher.matchMetadataField(alphabeticMdFields.get(1)),
                              MetadataFieldMatcher.matchMetadataField(alphabeticMdFields.get(2))
                              )))
                   .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadatafields?"),
                           Matchers.containsString("page=0"), Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadatafields?"),
                           Matchers.containsString("page=0"), Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadatafields?"),
                           Matchers.containsString("page=1"), Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadatafields?"),
                           Matchers.containsString("page=" + (numberOfPages - 1)),
                           Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$.page.totalElements", is(numberOfMdFields)))
                   .andExpect(jsonPath("$.page.totalPages", is(numberOfPages)))
                   .andExpect(jsonPath("$.page.size", is(pageSize)));

        // Check second page
        getClient().perform(get("/api/core/metadatafields")
                   .param("size", String.valueOf(pageSize))
                   .param("page", "1"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   // Metadata fields are returned alphabetically. So, look for the next 3 alphabetically
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItems(
                              MetadataFieldMatcher.matchMetadataField(alphabeticMdFields.get(3)),
                              MetadataFieldMatcher.matchMetadataField(alphabeticMdFields.get(4)),
                              MetadataFieldMatcher.matchMetadataField(alphabeticMdFields.get(5))
                              )))
                   .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadatafields?"),
                           Matchers.containsString("page=0"), Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$._links.prev.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadatafields?"),
                           Matchers.containsString("page=0"), Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadatafields?"),
                           Matchers.containsString("page=1"), Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadatafields?"),
                           Matchers.containsString("page=2"), Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadatafields?"),
                           Matchers.containsString("page=" + (numberOfPages - 1)),
                           Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$.page.totalElements", is(numberOfMdFields)))
                   .andExpect(jsonPath("$.page.totalPages", is(numberOfPages)))
                   .andExpect(jsonPath("$.page.size", is(pageSize)));

        // Check last page
        getClient().perform(get("/api/core/metadatafields")
                   .param("size", String.valueOf(pageSize))
                   .param("page", String.valueOf(numberOfPages - 1)))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   // Metadata fields are returned alphabetically.
                   // So, on the last page we'll just ensure it *at least* includes the last field alphabetically
                   .andExpect(jsonPath("$._embedded.metadatafields", Matchers.hasItems(
                              MetadataFieldMatcher.matchMetadataField(
                                  alphabeticMdFields.get(alphabeticMdFields.size() - 1))
                              )))
                   .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadatafields?"),
                           Matchers.containsString("page=0"), Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$._links.prev.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadatafields?"),
                           Matchers.containsString("page=" + (numberOfPages - 2)),
                           Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadatafields?"),
                           Matchers.containsString("page=" + (numberOfPages - 1)),
                           Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                           Matchers.containsString("/api/core/metadatafields?"),
                           Matchers.containsString("page=" + (numberOfPages - 1)),
                           Matchers.containsString("size=" + pageSize))))
                   .andExpect(jsonPath("$.page.totalElements", is(numberOfMdFields)))
                   .andExpect(jsonPath("$.page.totalPages", is(numberOfPages)))
                   .andExpect(jsonPath("$.page.size", is(pageSize)));
    }

}
