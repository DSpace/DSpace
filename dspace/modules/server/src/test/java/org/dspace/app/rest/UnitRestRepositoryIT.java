package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.test.AbstractControllerIntegrationTest.REST_SERVER_URL;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.exception.UnitNameNotProvidedException;
import org.dspace.app.rest.matcher.HalMatcher;
import org.dspace.app.rest.matcher.UnitMatcher;
import org.dspace.app.rest.model.UnitRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.UnitBuilder;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.Group;
import org.dspace.eperson.Unit;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.UnitService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Integration test for UnitRestRepository
 */
public class UnitRestRepositoryIT extends AbstractControllerIntegrationTest {
    @Autowired
    private ConfigurationService configurationService;

    @Before
    public void setup() {
    }

    /**
     * Populates the database with sample units
     *
     * @throws SQLException if a database error occurs.
     */
    private void createSampleUnits() throws SQLException {
        createUnit("testUnit1", Collections.EMPTY_LIST);
        createUnit("testUnit2", Collections.EMPTY_LIST);
    }

    /**
     * Creates a unit with the given name and group members using UnitBuilder
     *
     * @param name the name of the Unit to create
     * @param groupMembers an (possibly empty) list of Groups to associate with
     * the unit.
     * @return the create Unit
     * @throws SQLException if a database error occurs.
     */
    private Unit createUnit(String name, List<Group> groupMembers) throws SQLException {
        Context localContext = new Context();
        localContext.turnOffAuthorisationSystem();
        UnitBuilder unitBuilder = UnitBuilder.createUnit(localContext).withName(name);
        for (Group groupMember : groupMembers) {
            unitBuilder.addGroup(groupMember);

        }
        Unit unit = unitBuilder.build();

        localContext.complete();
        localContext.restoreAuthSystemState();
        return unit;
    }

    @Test
    public void createTest()
        throws Exception {
        // hold the id of the created unit
        AtomicReference<UUID> idRef = new AtomicReference<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            UnitRest unitRest = new UnitRest();
            String unitName = "testUnit1";
            unitRest.setName(unitName);

            String authToken = getAuthToken(admin.getEmail(), password);

            // Add unit
            getClient(authToken).perform(post("/api/eperson/units")
                    .content(mapper.writeValueAsBytes(unitRest)).contentType(contentType)
                    .param("projection", "full"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$", UnitMatcher.matchFullEmbeds()))
                    .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id"))))
            ;

            // Verify unit exists in list of all units
            getClient(authToken).perform(get("/api/eperson/units"))
                       //The status has to be 200 OK
                       .andExpect(status().isOk())
                       .andExpect(content().contentType(contentType))
                       .andExpect(jsonPath("$._embedded.units", Matchers.contains(
                               UnitMatcher.matchUnitWithName(unitName)
                       )))
            ;

            // Verify unit can be found by id
            UnitService unitService = EPersonServiceFactory.getInstance().getUnitService();
            Unit unit = unitService.find(context, idRef.get());

            assertEquals(unitName, unit.getName());

        } finally {
            // remove the created unit if any
            UnitBuilder.deleteUnit(idRef.get());
        }
    }

    @Test
    public void createUnauthauthorizedTest()
            throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        UnitRest unitRest = new UnitRest();
        String unitName = "testUnitUnauth1";

        unitRest.setName(unitName);

        getClient().perform(post("/api/eperson/units")
                .content(mapper.writeValueAsBytes(unitRest)).contentType(contentType))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void createForbiddenTest()
            throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        UnitRest unitRest = new UnitRest();
        String unitName = "testUnitForbidden1";

        unitRest.setName(unitName);

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(post("/api/eperson/units")
                .content(mapper.writeValueAsBytes(unitRest)).contentType(contentType))
                .andExpect(status().isForbidden());
    }

    @Test
    public void createUnprocessableTest()
            throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(
                post("/api/eperson/units").content("").contentType(contentType)
            )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(status().reason(containsString("Unprocessable")));
    }

    @Test
    public void createWithoutNameTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        UnitRest unitRest = new UnitRest(); // no name set

        String authToken = getAuthToken(admin.getEmail(), password);

        // enable Polish locale
        configurationService.setProperty("webui.supported.locales", "en, pl");

        // make request using Polish locale
        ResultActions resultActions = getClient(authToken)
            .perform(
                post("/api/eperson/units")
                    .header("Accept-Language", "pl") // request Polish response
                    .content(mapper.writeValueAsBytes(unitRest))
                    .contentType(contentType)
            );
            resultActions.andExpect(status().isUnprocessableEntity())
            .andExpect(status().reason(is(
                I18nUtil.getMessage(UnitNameNotProvidedException.MESSAGE_KEY, new Locale("pl"))
            )))
            .andExpect(status().reason(startsWith("[PL]"))); // verify it did not fall back to default locale

        // make request using default locale
        getClient(authToken)
            .perform(
                post("/api/eperson/units")
                    .content(mapper.writeValueAsBytes(unitRest))
                    .contentType(contentType)
            )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(status().reason(is(
                I18nUtil.getMessage(UnitNameNotProvidedException.MESSAGE_KEY)
            )))
            .andExpect(status().reason(not(startsWith("[PL]"))));
    }

    @Test
    public void findAllTest() throws Exception {
        createSampleUnits();

        String token = getAuthToken(admin.getEmail(), password);

        //When we call the root endpoint
        getClient(token).perform(get("/api/eperson/units"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   //The array of units should have a size 2
                   .andExpect(jsonPath("$._embedded.units", hasSize(2)))
                   // The created units should be listed
                   .andExpect(jsonPath("$._embedded.units", Matchers.containsInAnyOrder(
                       UnitMatcher.matchUnitWithName("testUnit1"),
                       UnitMatcher.matchUnitWithName("testUnit2")
                   )))
        ;
    }

    @Test
    public void findAllUnauthorizedTest()
            throws Exception {
        getClient().perform(get("/api/eperson/units"))
                   //The status has to be 403 Not Authorized
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findAllForbiddenTest() throws Exception {
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/eperson/units"))
                   .andExpect(status().isForbidden());
    }

    @Test
    public void findAllPaginationTest() throws Exception {
        createSampleUnits();

        String token = getAuthToken(admin.getEmail(), password);

        //When we call the root endpoint
        getClient(token).perform(get("/api/eperson/units"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(2)))
                   .andExpect(jsonPath("$.page.totalPages", is(1)))
                   .andExpect(jsonPath("$.page.number", is(0)));
    }


    @Test
    public void findOneTest() throws Exception {
        Unit unit = createUnit("findOneTest Unit", Collections.EMPTY_LIST);

        String token = getAuthToken(admin.getEmail(), password);

        // When full projection is requested, response should include expected properties, links, and embeds.
        String generatedUnitId = unit.getID().toString();
        String unitIdCall = "/api/eperson/units/" + generatedUnitId;
        getClient(token).perform(get(unitIdCall).param("projection", "full"))
                    //The status has to be 200 OK
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(contentType))
                    .andExpect(jsonPath("$", UnitMatcher.matchFullEmbeds()))
                    .andExpect(jsonPath("$", UnitMatcher.matchLinks(unit.getID())))
                    .andExpect(jsonPath("$", Matchers.is(
                        UnitMatcher.matchUnitEntry(unit.getID(), unit.getName())
                   )))
        ;

        getClient(token).perform(get("/api/eperson/units"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.totalElements", is(1)))
        ;

        // When no projection is requested, response should include expected properties, links, and no embeds.
        getClient(token).perform(get("/api/eperson/unit/"  + generatedUnitId))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
        ;
    }

    @Test
    public void findOneForbiddenTest() throws Exception {
        // Individual units are only viewable by admins
        Unit unit = createUnit("findOneForbidden", Collections.EMPTY_LIST);

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/eperson/units/" + unit.getID()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void findOneTestWrongUUID() throws Exception {
        // Random UUID should not exist
        String unitIdCall = "/api/eperson/units/" + UUID.randomUUID();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get(unitIdCall))
                   //The status has to be 404 Not Found
                   .andExpect(status().isNotFound())
        ;
    }

    @Test
    public void searchMethodsExist() throws Exception {
        createSampleUnits();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/units"))
                            .andExpect(jsonPath("$._links.search.href", Matchers.notNullValue()));

        getClient(authToken).perform(get("/api/eperson/units/search"))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$._links.byMetadata", Matchers.notNullValue()));
    }

    @Test
    public void findByMetadata() throws Exception {
        Unit unit1 = createUnit("Test unit", Collections.EMPTY_LIST);
        Unit unit2 = createUnit("Test unit 2", Collections.EMPTY_LIST);
        Unit unit3 = createUnit("Test unit 3", Collections.EMPTY_LIST);
        Unit unit4 = createUnit("Test other unit", Collections.EMPTY_LIST);

        // Search by name
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/units/search/byMetadata")
                                             .param("query", unit1.getName()))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$._embedded.units", Matchers.containsInAnyOrder(
                                    UnitMatcher.matchUnitEntry(unit1.getID(), unit1.getName()),
                                    UnitMatcher.matchUnitEntry(unit2.getID(), unit2.getName()),
                                    UnitMatcher.matchUnitEntry(unit3.getID(), unit3.getName())
                            )))
                            .andExpect(jsonPath("$.page.totalElements", is(3)));

        // Search by name (case insensitive)
        getClient(authToken).perform(get("/api/eperson/units/search/byMetadata")
                                             .param("query", unit1.getName().toLowerCase()))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$._embedded.units", Matchers.containsInAnyOrder(
                                    UnitMatcher.matchUnitEntry(unit1.getID(), unit1.getName()),
                                    UnitMatcher.matchUnitEntry(unit2.getID(), unit2.getName()),
                                    UnitMatcher.matchUnitEntry(unit3.getID(), unit3.getName())
                            )))
                            .andExpect(jsonPath("$.page.totalElements", is(3)));

        // Search by id
        getClient(authToken).perform(get("/api/eperson/units/search/byMetadata")
                                             .param("query", String.valueOf(unit1.getID())))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$._embedded.units", Matchers.contains(
                                    UnitMatcher.matchUnitEntry(unit1.getID(), unit1.getName())
                            )))
                            .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void findByMetadataUnauthorized() throws Exception {
        Unit unit1 = createUnit("Test unit", Collections.EMPTY_LIST);

        getClient().perform(get("/api/eperson/units/search/byMetadata")
                   .param("query", String.valueOf(unit1.getID())))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findByMetadataForbidden() throws Exception {
        Unit unit1 = createUnit("Test unit", Collections.EMPTY_LIST);

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/units/search/byMetadata")
                            .param("query", String.valueOf(unit1.getID())))
                            .andExpect(status().isForbidden());
    }

    @Test
    public void findByMetadataUndefined() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/units/search/byMetadata")
                                             .param("query", "Non-existing Unit"))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findByMetadataMissingParameter() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/units/search/byMetadata"))
                            .andExpect(status().isBadRequest());
    }

    @Test
    public void patchUnitName() throws Exception {
        Unit unit = createUnit("Unit", Collections.EMPTY_LIST);

        String token = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/name", "new name");
        ops.add(replaceOperation);
        String requestBody = getPatchContent(ops);
        getClient(token)
                .perform(patch("/api/eperson/units/" + unit.getID()).content(requestBody)
                                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk());
        getClient(token)
                .perform(get("/api/eperson/units/" + unit.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(
                        UnitMatcher.matchUnitEntry(unit.getID(), "new name"))
                ));
    }

    @Test
    public void addGroupTest() throws Exception {
        UnitService unitService = EPersonServiceFactory.getInstance().getUnitService();

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        Group adminGroup = groupService.findByName(context, "Administrator");

        Unit unit = createUnit("addGroupTest", Collections.EMPTY_LIST);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(
                post("/api/eperson/units/" + unit.getID() + "/groups")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(REST_SERVER_URL + "eperson/groups/" + adminGroup.getID()
                        )
        ).andExpect(status().isNoContent());
        unit = context.reloadEntity(unit);
        adminGroup = context.reloadEntity(adminGroup);
        assertTrue(unitService.isMember(unit, adminGroup));
    }

    @Test
    public void addMultipleGroupsTest() throws Exception {
        UnitService unitService = EPersonServiceFactory.getInstance().getUnitService();

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        Group adminGroup = groupService.findByName(context, "Administrator");

        context.turnOffAuthorisationSystem();
        Group testGroup = GroupBuilder.createGroup(context).withName("Test Group").build();
        context.restoreAuthSystemState();

        Unit unit = createUnit("addMultipleGroupsTest", Collections.EMPTY_LIST);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(
                post("/api/eperson/units/" + unit.getID() + "/groups")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(REST_SERVER_URL + "eperson/groups/" + adminGroup.getID() + "/\n"
                                + REST_SERVER_URL + "eperson/groups/" + testGroup.getID()
                        )
        ).andExpect(status().isNoContent());

        unit = context.reloadEntity(unit);
        adminGroup = context.reloadEntity(adminGroup);
        testGroup = context.reloadEntity(testGroup);
        assertTrue(unitService.isMember(unit, adminGroup));
        assertTrue(unitService.isMember(unit, testGroup));

        // Need to delete unit manually, to prevent a constraint violation
        // when deleting the testGroup
        // Ideally this would be handled by
        // AbstractBuilder/AbstractBuilderCleanupUtil
        // but it is not clear how to implement that.
        UnitBuilder.deleteUnit(unit.getID());
    }

    @Test
    public void addGroupForbiddenTest() throws Exception {
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        Group adminGroup = groupService.findByName(context, "Administrator");

        Unit unit = createUnit("addGroupTest", Collections.EMPTY_LIST);

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(
                post("/api/eperson/units/" + unit.getID() + "/groups")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(REST_SERVER_URL + "eperson/groups/" + adminGroup.getID()
                        )
        ).andExpect(status().isForbidden());
    }

    @Test
    public void addGroupUnauthorizedTest() throws Exception {
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        Group adminGroup = groupService.findByName(context, "Administrator");

        Unit unit = createUnit("addGroupTest", Collections.EMPTY_LIST);

        getClient().perform(
                post("/api/eperson/units/" + unit.getID() + "/groups")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(REST_SERVER_URL + "eperson/groups/" + adminGroup.getID()
                        )
        ).andExpect(status().isUnauthorized());
    }

    @Test
    public void addGroup_UnitNotFoundTest() throws Exception {
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        Group adminGroup = groupService.findByName(context, "Administrator");

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(
                post("/api/eperson/units/" + UUID.randomUUID() + "/groups")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(REST_SERVER_URL + "eperson/groups/" + adminGroup.getID()
                        )
        ).andExpect(status().isNotFound());
    }

    @Test
    public void addGroupUnprocessableTest() throws Exception {
        Unit unit = createUnit("addGroupTest", Collections.EMPTY_LIST);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(
                post("/api/eperson/units/" + unit.getID() + "/groups")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(REST_SERVER_URL + "eperson/groups/NOT_A_GROUP_UUID"
                        )
        ).andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void removeGroupTest() throws Exception {
        UnitService unitService = EPersonServiceFactory.getInstance().getUnitService();
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        Group adminGroup = groupService.findByName(context, "Administrator");
        List<Group> groups = List.of(adminGroup);
        Unit unit = createUnit("removeGroupTest", groups);

        assertTrue(unitService.isMember(unit, adminGroup));

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(
                delete("/api/eperson/units/" + unit.getID() + "/groups/" + adminGroup.getID())
        ).andExpect(status().isNoContent());

        unit = context.reloadEntity(unit);
        adminGroup = context.reloadEntity(adminGroup);

        assertFalse(unitService.isMember(unit, adminGroup));
    }

    @Test
    public void removeGroupForbiddenTest() throws Exception {
        UnitService unitService = EPersonServiceFactory.getInstance().getUnitService();
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        Group adminGroup = groupService.findByName(context, "Administrator");
        List<Group> groups = List.of(adminGroup);
        Unit unit = createUnit("removeGroupForbiddenTest", groups);

        assertTrue(unitService.isMember(unit, adminGroup));

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(
                delete("/api/eperson/units/" + unit.getID() + "/groups/" + adminGroup.getID())
        ).andExpect(status().isForbidden());

        unit = context.reloadEntity(unit);
        adminGroup = context.reloadEntity(adminGroup);

        assertTrue(unitService.isMember(unit, adminGroup));
    }

    @Test
    public void removeGroupUnauthorizedTest() throws Exception {
        UnitService unitService = EPersonServiceFactory.getInstance().getUnitService();
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        Group adminGroup = groupService.findByName(context, "Administrator");
        List<Group> groups = List.of(adminGroup);
        Unit unit = createUnit("removeGroupUnauthorizedTest", groups);

        assertTrue(unitService.isMember(unit, adminGroup));

        getClient().perform(
                delete("/api/eperson/units/" + unit.getID() + "/groups/" + adminGroup.getID())
        ).andExpect(status().isUnauthorized());

        unit = context.reloadEntity(unit);
        adminGroup = context.reloadEntity(adminGroup);

        assertTrue(unitService.isMember(unit, adminGroup));
    }

    @Test
    public void removeGroup_UnitNotFoundTest() throws Exception {
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        Group adminGroup = groupService.findByName(context, "Administrator");

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(
                delete("/api/eperson/units/" + UUID.randomUUID() + "/groups/" + adminGroup.getID())
        ).andExpect(status().isNotFound());
    }

    @Test
    public void removeGroupUnprocessableTest() throws Exception {
        UnitService unitService = EPersonServiceFactory.getInstance().getUnitService();
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        Group adminGroup = groupService.findByName(context, "Administrator");
        List<Group> groups = List.of(adminGroup);
        Unit unit = createUnit("removeGroupUnprocessableTest", groups);

        assertTrue(unitService.isMember(unit, adminGroup));

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(
                delete("/api/eperson/units/" + unit.getID() + "/groups/" + UUID.randomUUID())
        ).andExpect(status().isUnprocessableEntity());

        unit = context.reloadEntity(unit);
        adminGroup = context.reloadEntity(adminGroup);

        assertTrue(unitService.isMember(unit, adminGroup));
    }

    @Test
    public void deleteUnitTest() throws Exception {
        Unit unit = createUnit("deleteUnitTest", Collections.EMPTY_LIST);

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(
                get("/api/eperson/units/" + unit.getID())
        ).andExpect(status().isOk());

        getClient(authToken).perform(
                delete("/api/eperson/units/" + unit.getID())
        ).andExpect(status().isNoContent());

        getClient(authToken).perform(
                get("/api/eperson/units/" + unit.getID())
        ).andExpect(status().isNotFound());
    }

    @Test
    public void deleteUnitUnauthorizedTest() throws Exception {
        Unit unit = createUnit("deleteGroupUnauthorizedTest", Collections.EMPTY_LIST);

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(
                get("/api/eperson/units/" + unit.getID())
        ).andExpect(status().isOk());

        getClient().perform(
                delete("/api/eperson/units/" + unit.getID())
        ).andExpect(status().isUnauthorized());

        getClient(authToken).perform(
                get("/api/eperson/units/" + unit.getID())
        ).andExpect(status().isOk());
    }

    @Test
    public void deleteUnitForbiddenTest() throws Exception {
        Unit unit = createUnit("deleteUnitForbiddenTest", Collections.EMPTY_LIST);

        String adminToken = getAuthToken(admin.getEmail(), password);
        String authToken = getAuthToken(eperson.getEmail(), password);

        getClient(adminToken).perform(
                get("/api/eperson/units/" + unit.getID())
        ).andExpect(status().isOk());

        getClient(authToken).perform(
                delete("/api/eperson/units/" + unit.getID())
        ).andExpect(status().isForbidden());

        getClient(adminToken).perform(
                get("/api/eperson/units/" + unit.getID())
        ).andExpect(status().isOk());
    }

    @Test
    public void deleteUnitNotFoundTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(
                delete("/api/eperson/units/" + UUID.randomUUID())
        ).andExpect(status().isNotFound());
    }

    @Test
    public void findByMetadataPaginationTest() throws Exception {
        createUnit("Test unit", Collections.EMPTY_LIST);
        createUnit("Test unit 2", Collections.EMPTY_LIST);
        createUnit("Test unit 3", Collections.EMPTY_LIST);
        createUnit("Test unit 4", Collections.EMPTY_LIST);
        createUnit("Test other unit", Collections.EMPTY_LIST);

        String authTokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(authTokenAdmin).perform(get("/api/eperson/units/search/byMetadata")
                .param("query", "unit")
                .param("page", "0")
                .param("size", "2"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.units", Matchers.everyItem(
                        hasJsonPath("$.type", is("unit")))
                        ))
                .andExpect(jsonPath("$._embedded.units").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.number", is(0)))
                .andExpect(jsonPath("$.page.totalPages", is(3)))
                .andExpect(jsonPath("$.page.totalElements", is(5)));

        getClient(authTokenAdmin).perform(get("/api/eperson/units/search/byMetadata")
                .param("query", "unit")
                .param("page", "1")
                .param("size", "2"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.units", Matchers.everyItem(
                        hasJsonPath("$.type", is("unit")))
                        ))
                .andExpect(jsonPath("$._embedded.units").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.number", is(1)))
                .andExpect(jsonPath("$.page.totalPages", is(3)))
                .andExpect(jsonPath("$.page.totalElements", is(5)));

        getClient(authTokenAdmin).perform(get("/api/eperson/units/search/byMetadata")
                .param("query", "unit")
                .param("page", "2")
                .param("size", "2"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.units", Matchers.everyItem(
                        hasJsonPath("$.type", is("unit")))
                        ))
                .andExpect(jsonPath("$._embedded.units").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.number", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(3)))
                .andExpect(jsonPath("$.page.totalElements", is(5)));
    }

    @Test
    public void patchFacultyOnly() throws Exception {
        Unit unit = createUnit("patchFacultyOnly Unit", Collections.EMPTY_LIST);
        assertFalse(unit.getFacultyOnly());

        List<Operation> ops = new ArrayList<>();
        // Boolean operations should accept either string or boolean as value. Try boolean.
        ReplaceOperation replaceOperation = new ReplaceOperation("/facultyOnly", true);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(admin.getEmail(), password);

        // updates facultyOnly to true
        getClient(token).perform(patch("/api/eperson/units/" + unit.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.facultyOnly", Matchers.is(true)))
                        .andExpect(jsonPath("$", Matchers.is(
                                UnitMatcher.matchUnitEntry(unit.getID(), unit.getName()))));
    }

    @Test
    public void patchFacultyOnlyMissingValue() throws Exception {
        Unit unit = createUnit("patchFacultyOnlyMissingValue Unit", Collections.EMPTY_LIST);
        assertFalse(unit.getFacultyOnly());

        String token = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/facultyOnly", true);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        // initialize to true
        getClient(token).perform(patch("/api/eperson/units/" + unit.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.facultyOnly", Matchers.is(true)));

        unit = context.reloadEntity(unit);
        assertTrue(unit.getFacultyOnly());

        List<Operation> ops2 = new ArrayList<>();
        ReplaceOperation replaceOperation2 = new ReplaceOperation("/facultyOnly",null);
        ops2.add(replaceOperation2);
        patchBody = getPatchContent(ops2);

        // should return bad request
        getClient(token).perform(patch("/api/eperson/units/" + unit.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isBadRequest());

        // value should still be true.
        getClient(token).perform(get("/api/eperson/units/" + unit.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.facultyOnly", Matchers.is(true)))
                        .andExpect(jsonPath("$", Matchers.is(
                                UnitMatcher.matchUnitEntry(unit.getID(), unit.getName()))));
    }
}
