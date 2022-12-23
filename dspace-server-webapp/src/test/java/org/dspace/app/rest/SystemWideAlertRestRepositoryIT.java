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

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.alerts.AllowSessionsEnum;
import org.dspace.alerts.SystemWideAlert;
import org.dspace.alerts.service.SystemWideAlertService;
import org.dspace.app.rest.model.SystemWideAlertRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test class to test the operations in the SystemWideAlertRestRepository
 */
public class SystemWideAlertRestRepositoryIT extends AbstractControllerIntegrationTest {

    private static final Logger log = LogManager.getLogger(SystemWideAlertRestRepositoryIT.class);

    @Autowired
    private SystemWideAlertService systemWideAlertService;

    @After
    public void destroy() throws Exception {
        context.turnOffAuthorisationSystem();
        systemWideAlertService.findAll(context).stream().forEach(systemWideAlert -> {
            try {
                systemWideAlertService.delete(context, systemWideAlert);
            } catch (SQLException | IOException | AuthorizeException e) {
                log.error(e);
            }
        });
        context.restoreAuthSystemState();

        super.destroy();
    }


    @Test
    public void findAllTest() throws Exception {
        Date countdownDate = new Date();
        SystemWideAlert systemWideAlert1 = systemWideAlertService.create(context, "Test alert 1",
                                                                         AllowSessionsEnum.ALLOW_CURRENT_SESSIONS_ONLY, countdownDate,
                                                                         true);
        SystemWideAlert systemWideAlert2 = systemWideAlertService.create(context, "Test alert 2",
                                                                         AllowSessionsEnum.ALLOW_ADMIN_SESSIONS_ONLY, null,
                                                                         false);
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        getClient().perform(get("/api/system/systemwidealerts/"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.systemwidealerts", containsInAnyOrder(
                           allOf(
                                   hasJsonPath("$.alertId", is(systemWideAlert1.getID())),
                                   hasJsonPath("$.message", is(systemWideAlert1.getMessage())),
                                   hasJsonPath("$.allowSessions", is(systemWideAlert1.getAllowSessions())),
                                   hasJsonPath("$.countdownTo",
                                               startsWith(sdf.format(systemWideAlert1.getCountdownTo()))),
                                   hasJsonPath("$.active", is(systemWideAlert1.isActive()))
                           ),
                           allOf(
                                   hasJsonPath("$.alertId", is(systemWideAlert2.getID())),
                                   hasJsonPath("$.message", is(systemWideAlert2.getMessage())),
                                   hasJsonPath("$.allowSessions", is(systemWideAlert2.getAllowSessions())),
                                   hasJsonPath("$.countdownTo", is(systemWideAlert2.getCountdownTo())),
                                   hasJsonPath("$.active", is(systemWideAlert2.isActive()))
                           )
                   )));


    }

    @Test
    public void findOneTest() throws Exception {

        Date countdownDate = new Date();
        SystemWideAlert systemWideAlert1 = systemWideAlertService.create(context, "Test alert 1",
                                                                         AllowSessionsEnum.ALLOW_CURRENT_SESSIONS_ONLY, countdownDate,
                                                                         true);
        SystemWideAlert systemWideAlert2 = systemWideAlertService.create(context, "Test alert 2",
                                                                         AllowSessionsEnum.ALLOW_ADMIN_SESSIONS_ONLY, null,
                                                                         false);
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        getClient().perform(get("/api/system/systemwidealerts/" + systemWideAlert1.getID()))
                   .andExpect(status().isOk())
                   .andExpect(
                           jsonPath("$", allOf(
                                            hasJsonPath("$.alertId", is(systemWideAlert1.getID())),
                                            hasJsonPath("$.message", is(systemWideAlert1.getMessage())),
                                            hasJsonPath("$.allowSessions", is(systemWideAlert1.getAllowSessions())),
                                            hasJsonPath("$.countdownTo",
                                                        startsWith(sdf.format(systemWideAlert1.getCountdownTo()))),
                                            hasJsonPath("$.active", is(systemWideAlert1.isActive()))
                                    )
                           ));

    }

    @Test
    public void createTest() throws Exception {
        SystemWideAlertRest systemWideAlertRest = new SystemWideAlertRest();
        systemWideAlertRest.setMessage("Alert test message");
        systemWideAlertRest.setCountdownTo(new Date());
        systemWideAlertRest.setAllowSessions(1);
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

        getClient().perform(get("/api/system/systemwidealerts/" + idRef.get()))
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
        systemWideAlertRest.setAllowSessions(1);
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
        systemWideAlertRest.setAllowSessions(1);
        systemWideAlertRest.setActive(true);

        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/system/systemwidealerts/")
                                    .content(mapper.writeValueAsBytes(systemWideAlertRest))
                                    .contentType(contentType))
                   .andExpect(status().isUnauthorized());

    }


    @Test
    public void createWhenAlreadyExistsTest() throws Exception {

        SystemWideAlert systemWideAlert = systemWideAlertService.create(context, "Test alert",
                                                                        AllowSessionsEnum.ALLOW_ADMIN_SESSIONS_ONLY, null,
                                                                        false);

        SystemWideAlertRest systemWideAlertRest = new SystemWideAlertRest();
        systemWideAlertRest.setMessage("Alert test message");
        systemWideAlertRest.setCountdownTo(new Date());
        systemWideAlertRest.setAllowSessions(1);
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

        SystemWideAlert systemWideAlert = systemWideAlertService.create(context, "Alert test message",
                                                                        AllowSessionsEnum.ALLOW_ADMIN_SESSIONS_ONLY, null,
                                                                        false);

        SystemWideAlertRest systemWideAlertRest = new SystemWideAlertRest();
        systemWideAlertRest.setAlertId(systemWideAlert.getID());
        systemWideAlertRest.setMessage("Updated alert test message");
        systemWideAlertRest.setCountdownTo(new Date());
        systemWideAlertRest.setAllowSessions(1);
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

        getClient().perform(get("/api/system/systemwidealerts/" + systemWideAlert.getID()))
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

