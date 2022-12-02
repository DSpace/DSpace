package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.core.MediaType;

import org.dspace.app.rest.exception.EtdUnitNameNotProvidedException;
import org.dspace.app.rest.matcher.EtdUnitMatcher;
import org.dspace.app.rest.matcher.HalMatcher;
import org.dspace.app.rest.model.EtdUnitRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.EtdUnitBuilder;
import org.dspace.content.Collection;
import org.dspace.content.EtdUnit;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.EtdUnitService;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration test for EtdUnitRestRepository
 */
public class EtdUnitRestRepositoryIT extends AbstractControllerIntegrationTest {
        @Autowired
        private ConfigurationService configurationService;

        @Before
        public void setup() {
        }

        /**
         * Populates the database with sample etdunits
         *
         * @throws SQLException if a database error occurs.
         */
        private void createSampleEtdUnits() throws SQLException {
                createEtdUnit("testEtdUnit1", Collections.EMPTY_LIST);
                createEtdUnit("testEtdUnit2", Collections.EMPTY_LIST);
        }

        /**
         * Creates a etdunit with the given name and collection members using
         * EtdUnitBuilder
         *
         * @param name              the name of the EtdUnit to create
         * @param collectionMembers an (possibly empty) list of Collections to associate
         *                          with
         *                          the etdunit.
         * @return the create EtdUnit
         * @throws SQLException if a database error occurs.
         */
        private EtdUnit createEtdUnit(String name, List<Collection> collectionMembers) throws SQLException {
                Context localContext = new Context();
                localContext.turnOffAuthorisationSystem();
                EtdUnitBuilder etdunitBuilder = EtdUnitBuilder.createEtdUnit(localContext).withName(name);
                for (Collection collectionMember : collectionMembers) {
                        etdunitBuilder.addCollection(collectionMember);

                }
                EtdUnit etdunit = etdunitBuilder.build();

                localContext.complete();
                localContext.restoreAuthSystemState();
                return etdunit;
        }

        @Test
        public void createTest()
                        throws Exception {
                // hold the id of the created etdunit
                AtomicReference<UUID> idRef = new AtomicReference<>();
                try {
                        ObjectMapper mapper = new ObjectMapper();
                        EtdUnitRest etdunitRest = new EtdUnitRest();
                        String etdunitName = "testEtdUnit1";
                        etdunitRest.setName(etdunitName);

                        String authToken = getAuthToken(admin.getEmail(), password);

                        // Add etdunit
                        getClient(authToken).perform(post("/api/core/etdunits")
                                        .content(mapper.writeValueAsBytes(etdunitRest)).contentType(contentType)
                                        .param("projection", "full"))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$", EtdUnitMatcher.matchFullEmbeds()))
                                        .andDo(result -> idRef
                                                        .set(UUID.fromString(
                                                                        read(result.getResponse().getContentAsString(),
                                                                                        "$.id"))));

                        // Verify etdunit exists in list of all etdunits
                        getClient(authToken).perform(get("/api/core/etdunits"))
                                        // The status has to be 200 OK
                                        .andExpect(status().isOk())
                                        .andExpect(content().contentType(contentType))
                                        .andExpect(jsonPath("$._embedded.etdunits", Matchers.contains(
                                                        EtdUnitMatcher.matchEtdUnitWithName(etdunitName))));

                        // Verify etdunit can be found by id
                        EtdUnitService etdunitService = ContentServiceFactory.getInstance().getEtdUnitService();
                        EtdUnit etdunit = etdunitService.find(context, idRef.get());

                        assertEquals(etdunitName, etdunit.getName());

                } finally {
                        // remove the created etdunit if any
                        EtdUnitBuilder.deleteEtdUnit(idRef.get());
                }
        }

        @Test
        public void createUnauthauthorizedTest()
                        throws Exception {
                ObjectMapper mapper = new ObjectMapper();
                EtdUnitRest etdunitRest = new EtdUnitRest();
                String etdunitName = "testEtdUnitUnauth1";

                etdunitRest.setName(etdunitName);

                getClient().perform(post("/api/core/etdunits")
                                .content(mapper.writeValueAsBytes(etdunitRest)).contentType(contentType))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        public void createForbiddenTest()
                        throws Exception {

                ObjectMapper mapper = new ObjectMapper();
                EtdUnitRest etdunitRest = new EtdUnitRest();
                String etdunitName = "testEtdUnitForbidden1";

                etdunitRest.setName(etdunitName);

                String authToken = getAuthToken(eperson.getEmail(), password);
                getClient(authToken).perform(post("/api/core/etdunits")
                                .content(mapper.writeValueAsBytes(etdunitRest)).contentType(contentType))
                                .andExpect(status().isForbidden());
        }

        @Test
        public void createUnprocessableTest()
                        throws Exception {
                String authToken = getAuthToken(admin.getEmail(), password);
                getClient(authToken)
                                .perform(
                                                post("/api/core/etdunits").content("").contentType(contentType))
                                .andExpect(status().isUnprocessableEntity())
                                .andExpect(status().reason(containsString("Unprocessable")));
        }

        @Test
        public void createWithoutNameTest() throws Exception {
                ObjectMapper mapper = new ObjectMapper();
                EtdUnitRest etdunitRest = new EtdUnitRest(); // no name set

                String authToken = getAuthToken(admin.getEmail(), password);

                // enable Polish locale
                configurationService.setProperty("webui.supported.locales", "en, pl");

                // make request using Polish locale
                ResultActions resultActions = getClient(authToken)
                                .perform(
                                                post("/api/core/etdunits")
                                                                .header("Accept-Language", "pl") // request Polish
                                                                                                 // response
                                                                .content(mapper.writeValueAsBytes(etdunitRest))
                                                                .contentType(contentType));
                resultActions.andExpect(status().isUnprocessableEntity())
                                .andExpect(status().reason(is(
                                                I18nUtil.getMessage(EtdUnitNameNotProvidedException.MESSAGE_KEY,
                                                                new Locale("pl")))))
                                .andExpect(status().reason(startsWith("[PL]"))); // verify it did not fall back to
                                                                                 // default locale

                // make request using default locale
                getClient(authToken)
                                .perform(
                                                post("/api/core/etdunits")
                                                                .content(mapper.writeValueAsBytes(etdunitRest))
                                                                .contentType(contentType))
                                .andExpect(status().isUnprocessableEntity())
                                .andExpect(status().reason(is(
                                                I18nUtil.getMessage(EtdUnitNameNotProvidedException.MESSAGE_KEY))))
                                .andExpect(status().reason(not(startsWith("[PL]"))));
        }

        @Test
        public void findAllTest() throws Exception {
                createSampleEtdUnits();

                String token = getAuthToken(admin.getEmail(), password);

                // When we call the root endpoint
                getClient(token).perform(get("/api/core/etdunits"))
                                // The status has to be 200 OK
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(contentType))
                                // The array of etdunits should have a size 2
                                .andExpect(jsonPath("$._embedded.etdunits", hasSize(2)))
                                // The created etdunits should be listed
                                .andExpect(jsonPath("$._embedded.etdunits", Matchers.containsInAnyOrder(
                                                EtdUnitMatcher.matchEtdUnitWithName("testEtdUnit1"),
                                                EtdUnitMatcher.matchEtdUnitWithName("testEtdUnit2"))));
        }

        @Test
        public void findAllUnauthorizedTest()
                        throws Exception {
                getClient().perform(get("/api/core/etdunits"))
                                // The status has to be 403 Not Authorized
                                .andExpect(status().isUnauthorized());
        }

        @Test
        public void findAllForbiddenTest() throws Exception {
                String tokenEperson = getAuthToken(eperson.getEmail(), password);
                getClient(tokenEperson).perform(get("/api/core/etdunits"))
                                .andExpect(status().isForbidden());
        }

        @Test
        public void findAllPaginationTest() throws Exception {
                createSampleEtdUnits();

                String token = getAuthToken(admin.getEmail(), password);

                // When we call the root endpoint
                getClient(token).perform(get("/api/core/etdunits"))
                                // The status has to be 200 OK
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(contentType))
                                .andExpect(jsonPath("$.page.size", is(20)))
                                .andExpect(jsonPath("$.page.totalElements", is(2)))
                                .andExpect(jsonPath("$.page.totalPages", is(1)))
                                .andExpect(jsonPath("$.page.number", is(0)));
        }

        @Test
        public void findOneTest() throws Exception {
                EtdUnit etdunit = createEtdUnit("findOneTest EtdUnit", Collections.EMPTY_LIST);

                String token = getAuthToken(admin.getEmail(), password);

                // When full projection is requested, response should include expected
                // properties, links, and embeds.
                String generatedEtdUnitId = etdunit.getID().toString();
                String etdunitIdCall = "/api/core/etdunits/" + generatedEtdUnitId;
                getClient(token).perform(get(etdunitIdCall).param("projection", "full"))
                                // The status has to be 200 OK
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(contentType))
                                .andExpect(jsonPath("$", EtdUnitMatcher.matchFullEmbeds()))
                                .andExpect(jsonPath("$", EtdUnitMatcher.matchLinks(etdunit.getID())))
                                .andExpect(jsonPath("$", Matchers.is(
                                                EtdUnitMatcher.matchEtdUnitEntry(etdunit.getID(), etdunit.getName()))));

                getClient(token).perform(get("/api/core/etdunits"))
                                // The status has to be 200 OK
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(contentType))
                                .andExpect(jsonPath("$.page.totalElements", is(1)));

                // When no projection is requested, response should include expected properties,
                // links, and no embeds.
                getClient(token).perform(get("/api/core/etdunit/" + generatedEtdUnitId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()));
        }

        @Test
        public void findOneForbiddenTest() throws Exception {
                // Individual etdunits are only viewable by admins
                EtdUnit etdunit = createEtdUnit("findOneForbidden", Collections.EMPTY_LIST);

                String tokenEperson = getAuthToken(eperson.getEmail(), password);
                getClient(tokenEperson).perform(get("/api/core/etdunits/" + etdunit.getID()))
                                .andExpect(status().isForbidden());
        }

        @Test
        public void findOneTestWrongUUID() throws Exception {
                // Random UUID should not exist
                String etdunitIdCall = "/api/core/etdunits/" + UUID.randomUUID();

                String token = getAuthToken(admin.getEmail(), password);
                getClient(token).perform(get(etdunitIdCall))
                                // The status has to be 404 Not Found
                                .andExpect(status().isNotFound());
        }

        @Test
        public void searchMethodsExist() throws Exception {
                createSampleEtdUnits();

                String authToken = getAuthToken(admin.getEmail(), password);
                getClient(authToken).perform(get("/api/core/etdunits"))
                                .andExpect(jsonPath("$._links.search.href", Matchers.notNullValue()));

                getClient(authToken).perform(get("/api/core/etdunits/search"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(contentType))
                                .andExpect(jsonPath("$._links.byMetadata", Matchers.notNullValue()));
        }

        @Test
        public void findByMetadata() throws Exception {
                EtdUnit etdunit1 = createEtdUnit("Test etdunit", Collections.EMPTY_LIST);
                EtdUnit etdunit2 = createEtdUnit("Test etdunit 2", Collections.EMPTY_LIST);
                EtdUnit etdunit3 = createEtdUnit("Test etdunit 3", Collections.EMPTY_LIST);
                EtdUnit etdunit4 = createEtdUnit("Test other etdunit", Collections.EMPTY_LIST);

                // Search by name
                String authToken = getAuthToken(admin.getEmail(), password);
                getClient(authToken).perform(get("/api/core/etdunits/search/byMetadata")
                                .param("query", etdunit1.getName()))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(contentType))
                                .andExpect(jsonPath("$._embedded.etdunits", Matchers.containsInAnyOrder(
                                                EtdUnitMatcher.matchEtdUnitEntry(etdunit1.getID(), etdunit1.getName()),
                                                EtdUnitMatcher.matchEtdUnitEntry(etdunit2.getID(), etdunit2.getName()),
                                                EtdUnitMatcher.matchEtdUnitEntry(etdunit3.getID(),
                                                                etdunit3.getName()))))
                                .andExpect(jsonPath("$.page.totalElements", is(3)));

                // Search by name (case insensitive)
                getClient(authToken).perform(get("/api/core/etdunits/search/byMetadata")
                                .param("query", etdunit1.getName().toLowerCase()))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(contentType))
                                .andExpect(jsonPath("$._embedded.etdunits", Matchers.containsInAnyOrder(
                                                EtdUnitMatcher.matchEtdUnitEntry(etdunit1.getID(), etdunit1.getName()),
                                                EtdUnitMatcher.matchEtdUnitEntry(etdunit2.getID(), etdunit2.getName()),
                                                EtdUnitMatcher.matchEtdUnitEntry(etdunit3.getID(),
                                                                etdunit3.getName()))))
                                .andExpect(jsonPath("$.page.totalElements", is(3)));

                // Search by id
                getClient(authToken).perform(get("/api/core/etdunits/search/byMetadata")
                                .param("query", String.valueOf(etdunit1.getID())))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(contentType))
                                .andExpect(jsonPath("$._embedded.etdunits", Matchers.contains(
                                                EtdUnitMatcher.matchEtdUnitEntry(etdunit1.getID(),
                                                                etdunit1.getName()))))
                                .andExpect(jsonPath("$.page.totalElements", is(1)));
        }

        @Test
        public void findByMetadataUnauthorized() throws Exception {
                EtdUnit etdunit1 = createEtdUnit("Test etdunit", Collections.EMPTY_LIST);

                getClient().perform(get("/api/core/etdunits/search/byMetadata")
                                .param("query", String.valueOf(etdunit1.getID())))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        public void findByMetadataForbidden() throws Exception {
                EtdUnit etdunit1 = createEtdUnit("Test etdunit", Collections.EMPTY_LIST);

                String authToken = getAuthToken(eperson.getEmail(), password);
                getClient(authToken).perform(get("/api/core/etdunits/search/byMetadata")
                                .param("query", String.valueOf(etdunit1.getID())))
                                .andExpect(status().isForbidden());
        }

        @Test
        public void findByMetadataUndefined() throws Exception {
                String authToken = getAuthToken(admin.getEmail(), password);
                getClient(authToken).perform(get("/api/core/etdunits/search/byMetadata")
                                .param("query", "Non-existing EtdUnit"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(contentType))
                                .andExpect(jsonPath("$.page.totalElements", is(0)));
        }

        @Test
        public void findByMetadataMissingParameter() throws Exception {
                String authToken = getAuthToken(admin.getEmail(), password);
                getClient(authToken).perform(get("/api/core/etdunits/search/byMetadata"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        public void patchEtdUnitName() throws Exception {
                EtdUnit etdunit = createEtdUnit("EtdUnit", Collections.EMPTY_LIST);

                String token = getAuthToken(admin.getEmail(), password);

                List<Operation> ops = new ArrayList<>();
                ReplaceOperation replaceOperation = new ReplaceOperation("/name", "new name");
                ops.add(replaceOperation);
                String requestBody = getPatchContent(ops);
                getClient(token)
                                .perform(patch("/api/core/etdunits/" + etdunit.getID()).content(requestBody)
                                                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                                .andExpect(status().isOk());
                getClient(token)
                                .perform(get("/api/core/etdunits/" + etdunit.getID()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", Matchers.is(
                                                EtdUnitMatcher.matchEtdUnitEntry(etdunit.getID(), "new name"))));
        }

        @Test
        public void addCollectionTest() throws Exception {
                EtdUnitService etdunitService = ContentServiceFactory.getInstance().getEtdUnitService();

                CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
                context.turnOffAuthorisationSystem();
                Collection collection1 = CollectionBuilder.createCollection(context, null).withName("Test Collection1")
                                .build();
                Collection collection2 = CollectionBuilder.createCollection(context, null).withName("Test Collection2")
                                .build();
                context.restoreAuthSystemState();

                EtdUnit etdunit = createEtdUnit("addCollectionTest", Collections.EMPTY_LIST);

                String authToken = getAuthToken(admin.getEmail(), password);
                getClient(authToken).perform(
                                post("/api/core/etdunits/" + etdunit.getID() + "/collections")
                                                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                                .content(REST_SERVER_URL + "etdunit/collections/"
                                                                + collection1.getID()))
                                .andExpect(status().isNoContent());
                etdunit = context.reloadEntity(etdunit);
                collection1 = context.reloadEntity(collection1);
                assertTrue(etdunitService.isMember(etdunit, collection1));
        }

        @Test
        public void addMultipleCollectionsTest() throws Exception {
                EtdUnitService etdunitService = ContentServiceFactory.getInstance().getEtdUnitService();

                CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
                context.turnOffAuthorisationSystem();
                Collection collection1 = CollectionBuilder.createCollection(context, null).withName("Test Collection1")
                                .build();
                Collection collection2 = CollectionBuilder.createCollection(context, null).withName("Test Collection2")
                                .build();
                context.restoreAuthSystemState();

                EtdUnit etdunit = createEtdUnit("addMultipleCollectionsTest", Collections.EMPTY_LIST);

                String authToken = getAuthToken(admin.getEmail(), password);
                getClient(authToken).perform(
                                post("/api/core/etdunits/" + etdunit.getID() + "/collections")
                                                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                                .content(REST_SERVER_URL + "etdunit/collections/" + collection1.getID()
                                                                + "/\n"
                                                                + REST_SERVER_URL + "etdunit/collections/"
                                                                + collection2.getID()))
                                .andExpect(status().isNoContent());

                etdunit = context.reloadEntity(etdunit);
                collection1 = context.reloadEntity(collection1);
                collection2 = context.reloadEntity(collection2);
                assertTrue(etdunitService.isMember(etdunit, collection1));
                assertTrue(etdunitService.isMember(etdunit, collection2));

                // Need to delete etdunit manually, to prevent a constraint violation
                // when deleting the collection2
                // Ideally this would be handled by
                // AbstractBuilder/AbstractBuilderCleanupUtil
                // but it is not clear how to implement that.
                EtdUnitBuilder.deleteEtdUnit(etdunit.getID());
        }

        @Test
        public void addCollectionForbiddenTest() throws Exception {
                CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
                context.turnOffAuthorisationSystem();
                Collection collection1 = CollectionBuilder.createCollection(context, null).withName("Test Collection1")
                                .build();
                context.restoreAuthSystemState();
                EtdUnit etdunit = createEtdUnit("addCollectionTest", Collections.EMPTY_LIST);

                String authToken = getAuthToken(eperson.getEmail(), password);
                getClient(authToken).perform(
                                post("/api/core/etdunits/" + etdunit.getID() + "/collections")
                                                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                                .content(REST_SERVER_URL + "etdunit/collections/"
                                                                + collection1.getID()))
                                .andExpect(status().isForbidden());
        }

        @Test
        public void addCollectionUnauthorizedTest() throws Exception {
                CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
                context.turnOffAuthorisationSystem();
                Collection collection1 = CollectionBuilder.createCollection(context, null).withName("Test Collection1")
                                .build();
                context.restoreAuthSystemState();

                EtdUnit etdunit = createEtdUnit("addCollectionTest", Collections.EMPTY_LIST);

                getClient().perform(
                                post("/api/core/etdunits/" + etdunit.getID() + "/collections")
                                                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                                .content(REST_SERVER_URL + "etdunit/collections/"
                                                                + collection1.getID()))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        public void addCollection_EtdUnitNotFoundTest() throws Exception {
                CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
                context.turnOffAuthorisationSystem();
                Collection collection1 = CollectionBuilder.createCollection(context, null).withName("Test Collection1")
                                .build();
                context.restoreAuthSystemState();

                String authToken = getAuthToken(admin.getEmail(), password);
                getClient(authToken).perform(
                                post("/api/core/etdunits/" + UUID.randomUUID() + "/collections")
                                                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                                .content(REST_SERVER_URL + "etdunit/collections/"
                                                                + collection1.getID()))
                                .andExpect(status().isNotFound());
        }

        @Test
        public void addCollectionUnprocessableTest() throws Exception {
                EtdUnit etdunit = createEtdUnit("addCollectionTest", Collections.EMPTY_LIST);

                String authToken = getAuthToken(admin.getEmail(), password);
                getClient(authToken).perform(
                                post("/api/core/etdunits/" + etdunit.getID() + "/collections")
                                                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                                                .content(REST_SERVER_URL + "etdunit/collections/NOT_A_GROUP_UUID"))
                                .andExpect(status().isUnprocessableEntity());
        }

        @Test
        public void removeCollectionTest() throws Exception {
                EtdUnitService etdunitService = ContentServiceFactory.getInstance().getEtdUnitService();
                CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

                context.turnOffAuthorisationSystem();
                Collection collection1 = CollectionBuilder.createCollection(context, null).withName("Test Collection1")
                                .build();
                context.restoreAuthSystemState();
                List<Collection> collections = List.of(collection1);
                EtdUnit etdunit = createEtdUnit("removeCollectionTest", collections);

                assertTrue(etdunitService.isMember(etdunit, collection1));

                String authToken = getAuthToken(admin.getEmail(), password);
                getClient(authToken).perform(
                                delete("/api/core/etdunits/" + etdunit.getID() + "/collections/" + collection1.getID()))
                                .andExpect(status().isNoContent());

                etdunit = context.reloadEntity(etdunit);
                collection1 = context.reloadEntity(collection1);

                assertFalse(etdunitService.isMember(etdunit, collection1));
        }

        @Test
        public void removeCollectionForbiddenTest() throws Exception {
                EtdUnitService etdunitService = ContentServiceFactory.getInstance().getEtdUnitService();
                CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

                context.turnOffAuthorisationSystem();
                Collection collection1 = CollectionBuilder.createCollection(context, null).withName("Test Collection1")
                                .build();
                context.restoreAuthSystemState();
                List<Collection> collections = List.of(collection1);
                EtdUnit etdunit = createEtdUnit("removeCollectionForbiddenTest", collections);

                assertTrue(etdunitService.isMember(etdunit, collection1));

                String authToken = getAuthToken(eperson.getEmail(), password);
                getClient(authToken).perform(
                                delete("/api/core/etdunits/" + etdunit.getID() + "/collections/" + collection1.getID()))
                                .andExpect(status().isForbidden());

                etdunit = context.reloadEntity(etdunit);
                collection1 = context.reloadEntity(collection1);

                assertTrue(etdunitService.isMember(etdunit, collection1));
        }

        @Test
        public void removeCollectionUnauthorizedTest() throws Exception {
                EtdUnitService etdunitService = ContentServiceFactory.getInstance().getEtdUnitService();
                CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

                context.turnOffAuthorisationSystem();
                Collection collection1 = CollectionBuilder.createCollection(context, null).withName("Test Collection1")
                                .build();
                context.restoreAuthSystemState();
                List<Collection> collections = List.of(collection1);
                EtdUnit etdunit = createEtdUnit("removeCollectionUnauthorizedTest", collections);

                assertTrue(etdunitService.isMember(etdunit, collection1));

                getClient().perform(
                                delete("/api/core/etdunits/" + etdunit.getID() + "/collections/" + collection1.getID()))
                                .andExpect(status().isUnauthorized());

                etdunit = context.reloadEntity(etdunit);
                collection1 = context.reloadEntity(collection1);

                assertTrue(etdunitService.isMember(etdunit, collection1));
        }

        @Test
        public void removeCollection_EtdUnitNotFoundTest() throws Exception {
                CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
                context.turnOffAuthorisationSystem();
                Collection collection1 = CollectionBuilder.createCollection(context, null).withName("Test Collection1")
                                .build();
                context.restoreAuthSystemState();

                String authToken = getAuthToken(admin.getEmail(), password);

                getClient(authToken).perform(
                                delete("/api/core/etdunits/" + UUID.randomUUID() + "/collections/"
                                                + collection1.getID()))
                                .andExpect(status().isNotFound());
        }

        @Test
        public void removeCollectionUnprocessableTest() throws Exception {
                EtdUnitService etdunitService = ContentServiceFactory.getInstance().getEtdUnitService();
                CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

                context.turnOffAuthorisationSystem();
                Collection collection1 = CollectionBuilder.createCollection(context, null).withName("Test Collection1")
                                .build();
                context.restoreAuthSystemState();
                List<Collection> collections = List.of(collection1);
                EtdUnit etdunit = createEtdUnit("removeCollectionUnprocessableTest", collections);

                assertTrue(etdunitService.isMember(etdunit, collection1));

                String authToken = getAuthToken(admin.getEmail(), password);

                getClient(authToken).perform(
                                delete("/api/core/etdunits/" + etdunit.getID() + "/collections/" + UUID.randomUUID()))
                                .andExpect(status().isUnprocessableEntity());

                etdunit = context.reloadEntity(etdunit);
                collection1 = context.reloadEntity(collection1);

                assertTrue(etdunitService.isMember(etdunit, collection1));
        }

        @Test
        public void deleteEtdUnitTest() throws Exception {
                EtdUnit etdunit = createEtdUnit("deleteEtdUnitTest", Collections.EMPTY_LIST);

                String authToken = getAuthToken(admin.getEmail(), password);

                getClient(authToken).perform(
                                get("/api/core/etdunits/" + etdunit.getID())).andExpect(status().isOk());

                getClient(authToken).perform(
                                delete("/api/core/etdunits/" + etdunit.getID())).andExpect(status().isNoContent());

                getClient(authToken).perform(
                                get("/api/core/etdunits/" + etdunit.getID())).andExpect(status().isNotFound());
        }

        @Test
        public void deleteEtdUnitUnauthorizedTest() throws Exception {
                EtdUnit etdunit = createEtdUnit("deleteCollectionUnauthorizedTest", Collections.EMPTY_LIST);

                String authToken = getAuthToken(admin.getEmail(), password);

                getClient(authToken).perform(
                                get("/api/core/etdunits/" + etdunit.getID())).andExpect(status().isOk());

                getClient().perform(
                                delete("/api/core/etdunits/" + etdunit.getID())).andExpect(status().isUnauthorized());

                getClient(authToken).perform(
                                get("/api/core/etdunits/" + etdunit.getID())).andExpect(status().isOk());
        }

        @Test
        public void deleteEtdUnitForbiddenTest() throws Exception {
                EtdUnit etdunit = createEtdUnit("deleteEtdUnitForbiddenTest", Collections.EMPTY_LIST);

                String adminToken = getAuthToken(admin.getEmail(), password);
                String authToken = getAuthToken(eperson.getEmail(), password);

                getClient(adminToken).perform(
                                get("/api/core/etdunits/" + etdunit.getID())).andExpect(status().isOk());

                getClient(authToken).perform(
                                delete("/api/core/etdunits/" + etdunit.getID())).andExpect(status().isForbidden());

                getClient(adminToken).perform(
                                get("/api/core/etdunits/" + etdunit.getID())).andExpect(status().isOk());
        }

        @Test
        public void deleteEtdUnitNotFoundTest() throws Exception {
                String authToken = getAuthToken(admin.getEmail(), password);

                getClient(authToken).perform(
                                delete("/api/core/etdunits/" + UUID.randomUUID())).andExpect(status().isNotFound());
        }

        @Test
        public void findByMetadataPaginationTest() throws Exception {
                createEtdUnit("Test etdunit", Collections.EMPTY_LIST);
                createEtdUnit("Test etdunit 2", Collections.EMPTY_LIST);
                createEtdUnit("Test etdunit 3", Collections.EMPTY_LIST);
                createEtdUnit("Test etdunit 4", Collections.EMPTY_LIST);
                createEtdUnit("Test other etdunit", Collections.EMPTY_LIST);

                String authTokenAdmin = getAuthToken(admin.getEmail(), password);
                getClient(authTokenAdmin).perform(get("/api/core/etdunits/search/byMetadata")
                                .param("query", "etdunit")
                                .param("page", "0")
                                .param("size", "2"))
                                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                                .andExpect(jsonPath("$._embedded.etdunits", Matchers.everyItem(
                                                hasJsonPath("$.type", is("etdunit")))))
                                .andExpect(jsonPath("$._embedded.etdunits").value(Matchers.hasSize(2)))
                                .andExpect(jsonPath("$.page.size", is(2)))
                                .andExpect(jsonPath("$.page.number", is(0)))
                                .andExpect(jsonPath("$.page.totalPages", is(3)))
                                .andExpect(jsonPath("$.page.totalElements", is(5)));

                getClient(authTokenAdmin).perform(get("/api/core/etdunits/search/byMetadata")
                                .param("query", "etdunit")
                                .param("page", "1")
                                .param("size", "2"))
                                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                                .andExpect(jsonPath("$._embedded.etdunits", Matchers.everyItem(
                                                hasJsonPath("$.type", is("etdunit")))))
                                .andExpect(jsonPath("$._embedded.etdunits").value(Matchers.hasSize(2)))
                                .andExpect(jsonPath("$.page.size", is(2)))
                                .andExpect(jsonPath("$.page.number", is(1)))
                                .andExpect(jsonPath("$.page.totalPages", is(3)))
                                .andExpect(jsonPath("$.page.totalElements", is(5)));

                getClient(authTokenAdmin).perform(get("/api/core/etdunits/search/byMetadata")
                                .param("query", "etdunit")
                                .param("page", "2")
                                .param("size", "2"))
                                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                                .andExpect(jsonPath("$._embedded.etdunits", Matchers.everyItem(
                                                hasJsonPath("$.type", is("etdunit")))))
                                .andExpect(jsonPath("$._embedded.etdunits").value(Matchers.hasSize(1)))
                                .andExpect(jsonPath("$.page.size", is(2)))
                                .andExpect(jsonPath("$.page.number", is(2)))
                                .andExpect(jsonPath("$.page.totalPages", is(3)))
                                .andExpect(jsonPath("$.page.totalElements", is(5)));
        }

}
