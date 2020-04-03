/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.matcher.EPersonMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class LoginAsEPersonIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private GroupService groupService;

    @Before
    public void setup() {
        context.turnOffAuthorisationSystem();
        configurationService.setProperty("webui.user.assumelogin", true);
        context.restoreAuthSystemState();
    }

    @Test
    public void loggedInUserRetrievalTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authn/status")
                                    .param("projection", "full"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eperson", EPersonMatcher.matchEPersonOnEmail(admin.getEmail())));


    }
    @Test
    public void loggedInAsOtherUserRetrievalTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authn/status")
                                    .param("projection", "full")
                                    .header("X-On-Behalf-Of", eperson.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.eperson",
                                            EPersonMatcher.matchEPersonOnEmail(eperson.getEmail())));


    }

    @Test
    public void loggedInAsOtherUserNotAUuidInHeaderBadRequestRetrievalTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authn/status")
                                     .param("projection", "full")
                                     .header("X-On-Behalf-Of", "not-a-uuid"))
                        .andExpect(status().isBadRequest());


    }

    @Test
    public void loggedInAsOtherUserWrongUuidInHeaderBadRequestRetrievalTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authn/status")
                                     .param("projection", "full")
                                     .header("X-On-Behalf-Of", UUID.randomUUID()))
                        .andExpect(status().isBadRequest());


    }

    @Test
    public void loggedInAsOtherUserNoPermissionForbiddenRetrievalTest() throws Exception {


        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authn/status")
                                     .param("projection", "full")
                                     .header("X-On-Behalf-Of", eperson.getID()))
                        .andExpect(status().isForbidden());


    }


    @Test
    public void loggedInUserPropertyFalseTest() throws Exception {
        context.turnOffAuthorisationSystem();
        configurationService.setProperty("webui.user.assumelogin", false);
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authn/status")
                                     .header("X-On-Behalf-Of", eperson.getID()))
                        .andExpect(status().isBadRequest());

        context.turnOffAuthorisationSystem();
        configurationService.setProperty("webui.user.assumelogin", true);
        context.restoreAuthSystemState();
    }


    @Test
    public void loggedInUserOtherAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson testEperson = EPersonBuilder.createEPerson(context).withEmail("loginasuseradmin@test.com").build();


        Group adminGroup = groupService.findByName(context, Group.ADMIN);
        groupService.addMember(context, adminGroup, testEperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authn/status")
                                     .header("X-On-Behalf-Of", testEperson.getID()))
                        .andExpect(status().isBadRequest());




    }

}
