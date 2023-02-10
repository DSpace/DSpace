/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.alerts.AllowSessionsEnum;
import org.dspace.alerts.SystemWideAlert;
import org.dspace.app.rest.model.SystemWideAlertRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.SystemWideAlertBuilder;
import org.junit.Test;

/**
 * Test class to test the operations in the SystemWideAlertRestRepository
 */
public class SystemWideAlertRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAllTest() throws Exception {
        // Create two alert entries in the db to fully test the findAll method
        // Note: It is not possible to create two alerts through the REST API
        context.turnOffAuthorisationSystem();
        Date countdownDate = new Date();
        SystemWideAlert systemWideAlert1 = SystemWideAlertBuilder.createSystemWideAlert(context, "Test alert 1")
                                                                 .withAllowSessions(
                                                                         AllowSessionsEnum.ALLOW_CURRENT_SESSIONS_ONLY)
                                                                 .withCountdownDate(countdownDate)
                                                                 .isActive(true)
                                                                 .build();

        SystemWideAlert systemWideAlert2 = SystemWideAlertBuilder.createSystemWideAlert(context, "Test alert 2")
                                                                 .withAllowSessions(
                                                                         AllowSessionsEnum.ALLOW_ADMIN_SESSIONS_ONLY)
                                                                 .withCountdownDate(null)
                                                                 .isActive(false)
                                                                 .build();
        context.restoreAuthSystemState();

        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(get("/api/system/systemwidealerts/"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.systemwidealerts", containsInAnyOrder(
                           allOf(
                                   hasJsonPath("$.alertId", is(systemWideAlert1.getID())),
                                   hasJsonPath("$.message", is(systemWideAlert1.getMessage())),
                                   hasJsonPath("$.allowSessions", is(systemWideAlert1.getAllowSessions().getValue())),
                                   hasJsonPath("$.countdownTo",
                                               startsWith(sdf.format(systemWideAlert1.getCountdownTo()))),
                                   hasJsonPath("$.active", is(systemWideAlert1.isActive()))
                           ),
                           allOf(
                                   hasJsonPath("$.alertId", is(systemWideAlert2.getID())),
                                   hasJsonPath("$.message", is(systemWideAlert2.getMessage())),
                                   hasJsonPath("$.allowSessions", is(systemWideAlert2.getAllowSessions().getValue())),
                                   hasJsonPath("$.countdownTo", is(systemWideAlert2.getCountdownTo())),
                                   hasJsonPath("$.active", is(systemWideAlert2.isActive()))
                           )
                   )));
    }

    @Test
    public void findAllUnauthorizedTest() throws Exception {
        // Create two alert entries in the db to fully test the findAll method
        // Note: It is not possible to create two alerts through the REST API
        context.turnOffAuthorisationSystem();
        Date countdownDate = new Date();
        SystemWideAlert systemWideAlert1 = SystemWideAlertBuilder.createSystemWideAlert(context, "Test alert 1")
                                                                 .withAllowSessions(
                                                                         AllowSessionsEnum.ALLOW_CURRENT_SESSIONS_ONLY)
                                                                 .withCountdownDate(countdownDate)
                                                                 .isActive(true)
                                                                 .build();

        SystemWideAlert systemWideAlert2 = SystemWideAlertBuilder.createSystemWideAlert(context, "Test alert 2")
                                                                 .withAllowSessions(
                                                                         AllowSessionsEnum.ALLOW_ADMIN_SESSIONS_ONLY)
                                                                 .withCountdownDate(null)
                                                                 .isActive(false)
                                                                 .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/api/system/systemwidealerts/"))
                   .andExpect(status().isUnauthorized());

    }

    @Test
    public void findAllForbiddenTest() throws Exception {
        // Create two alert entries in the db to fully test the findAll method
        // Note: It is not possible to create two alerts through the REST API
        context.turnOffAuthorisationSystem();
        Date countdownDate = new Date();
        SystemWideAlert systemWideAlert1 = SystemWideAlertBuilder.createSystemWideAlert(context, "Test alert 1")
                                                                 .withAllowSessions(
                                                                         AllowSessionsEnum.ALLOW_CURRENT_SESSIONS_ONLY)
                                                                 .withCountdownDate(countdownDate)
                                                                 .isActive(true)
                                                                 .build();

        SystemWideAlert systemWideAlert2 = SystemWideAlertBuilder.createSystemWideAlert(context, "Test alert 2")
                                                                 .withAllowSessions(
                                                                         AllowSessionsEnum.ALLOW_ADMIN_SESSIONS_ONLY)
                                                                 .withCountdownDate(null)
                                                                 .isActive(false)
                                                                 .build();
        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/system/systemwidealerts/"))
                            .andExpect(status().isForbidden());

    }

    @Test
    public void findOneTest() throws Exception {
        // Create two alert entries in the db to fully test the findOne method
        // Note: It is not possible to create two alerts through the REST API
        context.turnOffAuthorisationSystem();
        Date countdownDate = new Date();
        SystemWideAlert systemWideAlert1 = SystemWideAlertBuilder.createSystemWideAlert(context, "Test alert 1")
                                                                 .withAllowSessions(
                                                                         AllowSessionsEnum.ALLOW_CURRENT_SESSIONS_ONLY)
                                                                 .withCountdownDate(countdownDate)
                                                                 .isActive(true)
                                                                 .build();
        SystemWideAlert systemWideAlert2 = SystemWideAlertBuilder.createSystemWideAlert(context, "Test alert 2")
                                                                 .withAllowSessions(
                                                                         AllowSessionsEnum.ALLOW_ADMIN_SESSIONS_ONLY)
                                                                 .withCountdownDate(null)
                                                                 .isActive(false)
                                                                 .build();
        context.restoreAuthSystemState();

        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        String authToken = getAuthToken(admin.getEmail(), password);

        // When the alert is active and the user is not an admin, the user will be able to see the alert
        getClient(authToken).perform(get("/api/system/systemwidealerts/" + systemWideAlert1.getID()))
                            .andExpect(status().isOk())
                            .andExpect(
                                    jsonPath("$", allOf(
                                             hasJsonPath("$.alertId", is(systemWideAlert1.getID())),
                                             hasJsonPath("$.message", is(systemWideAlert1.getMessage())),
                                             hasJsonPath("$.allowSessions",
                                                         is(systemWideAlert1.getAllowSessions().getValue())),
                                             hasJsonPath("$.countdownTo",
                                                         startsWith(sdf.format(systemWideAlert1.getCountdownTo()))),
                                             hasJsonPath("$.active", is(systemWideAlert1.isActive()))
                                             )
                                    ));

    }


    @Test
    public void findOneUnauthorizedTest() throws Exception {
        // Create two alert entries in the db to fully test the findOne method
        // Note: It is not possible to create two alerts through the REST API
        context.turnOffAuthorisationSystem();
        Date countdownDate = new Date();
        SystemWideAlert systemWideAlert1 = SystemWideAlertBuilder.createSystemWideAlert(context, "Test alert 1")
                                                                 .withAllowSessions(
                                                                         AllowSessionsEnum.ALLOW_CURRENT_SESSIONS_ONLY)
                                                                 .withCountdownDate(countdownDate)
                                                                 .isActive(true)
                                                                 .build();

        SystemWideAlert systemWideAlert2 = SystemWideAlertBuilder.createSystemWideAlert(context, "Test alert 2")
                                                                 .withAllowSessions(
                                                                         AllowSessionsEnum.ALLOW_ADMIN_SESSIONS_ONLY)
                                                                 .withCountdownDate(null)
                                                                 .isActive(false)
                                                                 .build();
        context.restoreAuthSystemState();

        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        // When the alert is active and the user is not an admin, the user will be able to see the alert
        getClient().perform(get("/api/system/systemwidealerts/" + systemWideAlert1.getID()))
                            .andExpect(status().isOk())
                            .andExpect(
                                    jsonPath("$", allOf(
                                             hasJsonPath("$.alertId", is(systemWideAlert1.getID())),
                                             hasJsonPath("$.message", is(systemWideAlert1.getMessage())),
                                             hasJsonPath("$.allowSessions",
                                                         is(systemWideAlert1.getAllowSessions().getValue())),
                                             hasJsonPath("$.countdownTo",
                                                         startsWith(sdf.format(systemWideAlert1.getCountdownTo()))),
                                             hasJsonPath("$.active", is(systemWideAlert1.isActive()))
                                             )
                                    ));

        // When the alert is inactive and the user is not an admin, the user will not be able to see the presence of the
        // alert and a 404 will be returned by the findOne endpoint
        getClient().perform(get("/api/system/systemwidealerts/" + systemWideAlert2.getID()))
                   .andExpect(status().isNotFound());

    }

    @Test
    public void findOneForbiddenTest() throws Exception {
        // Create two alert entries in the db to fully test the findOne method
        // Note: It is not possible to create two alerts through the REST API
        context.turnOffAuthorisationSystem();
        Date countdownDate = new Date();
        SystemWideAlert systemWideAlert1 = SystemWideAlertBuilder.createSystemWideAlert(context, "Test alert 1")
                                                                 .withAllowSessions(
                                                                         AllowSessionsEnum.ALLOW_CURRENT_SESSIONS_ONLY)
                                                                 .withCountdownDate(countdownDate)
                                                                 .isActive(true)
                                                                 .build();

        SystemWideAlert systemWideAlert2 = SystemWideAlertBuilder.createSystemWideAlert(context, "Test alert 2")
                                                                 .withAllowSessions(
                                                                         AllowSessionsEnum.ALLOW_ADMIN_SESSIONS_ONLY)
                                                                 .withCountdownDate(null)
                                                                 .isActive(false)
                                                                 .build();
        context.restoreAuthSystemState();

        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String authToken = getAuthToken(eperson.getEmail(), password);


        getClient(authToken).perform(get("/api/system/systemwidealerts/" + systemWideAlert1.getID()))
                            .andExpect(status().isOk())
                            .andExpect(
                                    jsonPath("$", allOf(
                                             hasJsonPath("$.alertId", is(systemWideAlert1.getID())),
                                             hasJsonPath("$.message", is(systemWideAlert1.getMessage())),
                                             hasJsonPath("$.allowSessions",
                                                         is(systemWideAlert1.getAllowSessions().getValue())),
                                             hasJsonPath("$.countdownTo",
                                                         startsWith(sdf.format(systemWideAlert1.getCountdownTo()))),
                                             hasJsonPath("$.active", is(systemWideAlert1.isActive()))
                                             )
                                    ));

        // When the alert is inactive and the user is not an admin, the user will not be able to see the presence of the
        // alert and a 404 will be returned by the findOne endpoint
        getClient(authToken).perform(get("/api/system/systemwidealerts/" + systemWideAlert2.getID()))
                            .andExpect(status().isNotFound());

    }

    @Test
    public void findAllActiveTest() throws Exception {
        // Create three alert entries in the db to fully test the findActive search method
        // Note: It is not possible to create two alerts through the REST API
        context.turnOffAuthorisationSystem();
        Date countdownDate = new Date();
        SystemWideAlert systemWideAlert1 = SystemWideAlertBuilder.createSystemWideAlert(context, "Test alert 1")
                                                                 .withAllowSessions(
                                                                         AllowSessionsEnum.ALLOW_CURRENT_SESSIONS_ONLY)
                                                                 .withCountdownDate(countdownDate)
                                                                 .isActive(true)
                                                                 .build();

        SystemWideAlert systemWideAlert2 = SystemWideAlertBuilder.createSystemWideAlert(context, "Test alert 2")
                                                                 .withAllowSessions(
                                                                         AllowSessionsEnum.ALLOW_ADMIN_SESSIONS_ONLY)
                                                                 .withCountdownDate(null)
                                                                 .isActive(false)
                                                                 .build();

        SystemWideAlert systemWideAlert3 = SystemWideAlertBuilder.createSystemWideAlert(context, "Test alert 3")
                                                                 .withAllowSessions(
                                                                         AllowSessionsEnum.ALLOW_ADMIN_SESSIONS_ONLY)
                                                                 .withCountdownDate(null)
                                                                 .isActive(true)
                                                                 .build();
        context.restoreAuthSystemState();

        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        getClient().perform(get("/api/system/systemwidealerts/search/active"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$._embedded.systemwidealerts", containsInAnyOrder(
                                    allOf(
                                    hasJsonPath("$.alertId", is(systemWideAlert1.getID())),
                                    hasJsonPath("$.message", is(systemWideAlert1.getMessage())),
                                    hasJsonPath("$.allowSessions", is(systemWideAlert1.getAllowSessions().getValue())),
                                    hasJsonPath("$.countdownTo",
                                                startsWith(sdf.format(systemWideAlert1.getCountdownTo()))),
                                    hasJsonPath("$.active", is(systemWideAlert1.isActive()))
                                    ),
                                    allOf(
                                    hasJsonPath("$.alertId", is(systemWideAlert3.getID())),
                                    hasJsonPath("$.message", is(systemWideAlert3.getMessage())),
                                    hasJsonPath("$.allowSessions", is(systemWideAlert3.getAllowSessions().getValue())),
                                    hasJsonPath("$.countdownTo", is(systemWideAlert3.getCountdownTo())),
                                    hasJsonPath("$.active", is(systemWideAlert3.isActive()))
                                    )
                            )));

    }

    @Test
    public void createTest() throws Exception {
        SystemWideAlertRest systemWideAlertRest = new SystemWideAlertRest();
        systemWideAlertRest.setMessage("Alert test message");
        systemWideAlertRest.setCountdownTo(new Date());
        systemWideAlertRest.setAllowSessions(AllowSessionsEnum.ALLOW_CURRENT_SESSIONS_ONLY.getValue());
        systemWideAlertRest.setActive(true);

        ObjectMapper mapper = new ObjectMapper();

        String authToken = getAuthToken(admin.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<>();


        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        getClient(authToken).perform(post("/api/system/systemwidealerts/")
                                             .content(mapper.writeValueAsBytes(systemWideAlertRest))
                                             .contentType(contentType))
                            .andExpect(status().isCreated())
                            .andExpect(
                                    jsonPath("$", allOf(
                                         hasJsonPath("$.alertId"),
                                         hasJsonPath("$.message", is(systemWideAlertRest.getMessage())),
                                         hasJsonPath("$.allowSessions",
                                                     is(systemWideAlertRest.getAllowSessions())),
                                         hasJsonPath("$.countdownTo",
                                                     startsWith(sdf.format(systemWideAlertRest.getCountdownTo()))),
                                         hasJsonPath("$.active", is(systemWideAlertRest.isActive()))
                                             )
                                    ))
                            .andDo(result -> idRef
                                    .set((read(result.getResponse().getContentAsString(), "$.alertId"))));

        getClient(authToken).perform(get("/api/system/systemwidealerts/" + idRef.get()))
                   .andExpect(status().isOk())
                   .andExpect(
                           jsonPath("$", allOf(
                                            hasJsonPath("$.alertId", is(idRef.get())),
                                            hasJsonPath("$.message", is(systemWideAlertRest.getMessage())),
                                            hasJsonPath("$.allowSessions", is(systemWideAlertRest.getAllowSessions())),
                                            hasJsonPath("$.countdownTo",
                                                        startsWith(sdf.format(systemWideAlertRest.getCountdownTo()))),
                                            hasJsonPath("$.active", is(systemWideAlertRest.isActive()))
                                    )
                           ));

    }

    @Test
    public void createForbiddenTest() throws Exception {

        SystemWideAlertRest systemWideAlertRest = new SystemWideAlertRest();
        systemWideAlertRest.setMessage("Alert test message");
        systemWideAlertRest.setCountdownTo(new Date());
        systemWideAlertRest.setAllowSessions(AllowSessionsEnum.ALLOW_CURRENT_SESSIONS_ONLY.getValue());
        systemWideAlertRest.setActive(true);

        ObjectMapper mapper = new ObjectMapper();

        String authToken = getAuthToken(eperson.getEmail(), password);


        getClient(authToken).perform(post("/api/system/systemwidealerts/")
                                             .content(mapper.writeValueAsBytes(systemWideAlertRest))
                                             .contentType(contentType))
                            .andExpect(status().isForbidden());
    }

    @Test
    public void createUnAuthorizedTest() throws Exception {

        SystemWideAlertRest systemWideAlertRest = new SystemWideAlertRest();
        systemWideAlertRest.setMessage("Alert test message");
        systemWideAlertRest.setCountdownTo(new Date());
        systemWideAlertRest.setAllowSessions(AllowSessionsEnum.ALLOW_CURRENT_SESSIONS_ONLY.getValue());
        systemWideAlertRest.setActive(true);

        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/system/systemwidealerts/")
                                    .content(mapper.writeValueAsBytes(systemWideAlertRest))
                                    .contentType(contentType))
                   .andExpect(status().isUnauthorized());

    }


    @Test
    public void createWhenAlreadyExistsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        SystemWideAlert systemWideAlert = SystemWideAlertBuilder.createSystemWideAlert(context, "Test alert")
                                                                .withAllowSessions(
                                                                        AllowSessionsEnum.ALLOW_ADMIN_SESSIONS_ONLY)
                                                                .withCountdownDate(null)
                                                                .isActive(false)
                                                                .build();

        context.restoreAuthSystemState();

        SystemWideAlertRest systemWideAlertRest = new SystemWideAlertRest();
        systemWideAlertRest.setMessage("Alert test message");
        systemWideAlertRest.setCountdownTo(new Date());
        systemWideAlertRest.setAllowSessions(AllowSessionsEnum.ALLOW_CURRENT_SESSIONS_ONLY.getValue());
        systemWideAlertRest.setActive(true);

        ObjectMapper mapper = new ObjectMapper();

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(post("/api/system/systemwidealerts/")
                                             .content(mapper.writeValueAsBytes(systemWideAlertRest))
                                             .contentType(contentType))
                            .andExpect(status().isBadRequest());

    }

    @Test
    public void putTest() throws Exception {
        context.turnOffAuthorisationSystem();
        SystemWideAlert systemWideAlert = SystemWideAlertBuilder.createSystemWideAlert(context, "Alert test message")
                                                                .withAllowSessions(
                                                                        AllowSessionsEnum.ALLOW_ADMIN_SESSIONS_ONLY)
                                                                .withCountdownDate(null)
                                                                .isActive(false)
                                                                .build();
        context.restoreAuthSystemState();

        SystemWideAlertRest systemWideAlertRest = new SystemWideAlertRest();
        systemWideAlertRest.setAlertId(systemWideAlert.getID());
        systemWideAlertRest.setMessage("Updated alert test message");
        systemWideAlertRest.setCountdownTo(new Date());
        systemWideAlertRest.setAllowSessions(AllowSessionsEnum.ALLOW_CURRENT_SESSIONS_ONLY.getValue());
        systemWideAlertRest.setActive(true);

        ObjectMapper mapper = new ObjectMapper();

        String authToken = getAuthToken(admin.getEmail(), password);


        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        getClient(authToken).perform(put("/api/system/systemwidealerts/" + systemWideAlert.getID())
                                             .content(mapper.writeValueAsBytes(systemWideAlertRest))
                                             .contentType(contentType))
                            .andExpect(status().isOk())
                            .andExpect(
                                    jsonPath("$", allOf(
                                             hasJsonPath("$.alertId"),
                                             hasJsonPath("$.message", is(systemWideAlertRest.getMessage())),
                                             hasJsonPath("$.allowSessions",
                                                         is(systemWideAlertRest.getAllowSessions())),
                                             hasJsonPath("$.countdownTo",
                                                         startsWith(sdf.format(systemWideAlertRest.getCountdownTo()))),
                                             hasJsonPath("$.active", is(systemWideAlertRest.isActive()))
                                             )
                                    ));

        getClient(authToken).perform(get("/api/system/systemwidealerts/" + systemWideAlert.getID()))
                   .andExpect(status().isOk())
                   .andExpect(
                           jsonPath("$", allOf(
                                            hasJsonPath("$.alertId", is(systemWideAlert.getID())),
                                            hasJsonPath("$.message", is(systemWideAlertRest.getMessage())),
                                            hasJsonPath("$.allowSessions", is(systemWideAlertRest.getAllowSessions())),
                                            hasJsonPath("$.countdownTo",
                                                        startsWith(sdf.format(systemWideAlertRest.getCountdownTo()))),
                                            hasJsonPath("$.active", is(systemWideAlertRest.isActive()))
                                    )
                           ));


    }


}

