/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.matcher.RegistrationMatcher;
import org.dspace.app.rest.model.RegistrationRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.dao.RegistrationDataDAO;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class RegistrationRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private RegistrationDataDAO registrationDataDAO;

    @Test
    public void findByTokenTestExistingUserTest() throws Exception {
        String email = eperson.getEmail();
        createTokenForEmail(email);
        RegistrationData registrationData = registrationDataDAO.findByEmail(context, email);

        getClient().perform(get("/api/eperson/registrations/search/findByToken")
                                .param("token", registrationData.getToken()))
                   .andExpect(status().isOk())
                   .andExpect(
                       jsonPath("$", Matchers.is(RegistrationMatcher.matchRegistration(email, eperson.getID()))));

        email = "newUser@testnewuser.com";
        createTokenForEmail(email);
        registrationData = registrationDataDAO.findByEmail(context, email);

        getClient().perform(get("/api/eperson/registrations/search/findByToken")
                                .param("token", registrationData.getToken()))
                   .andExpect(status().isOk())
                   .andExpect(
                       jsonPath("$", Matchers.is(RegistrationMatcher.matchRegistration(email, null))));

        registrationDataDAO.delete(context, registrationData);

    }

    @Test
    public void findByTokenTestNewUserTest() throws Exception {
        String email = "newUser@testnewuser.com";
        createTokenForEmail(email);
        RegistrationData registrationData = registrationDataDAO.findByEmail(context, email);

        getClient().perform(get("/api/eperson/registrations/search/findByToken")
                                .param("token", registrationData.getToken()))
                   .andExpect(status().isOk())
                   .andExpect(
                       jsonPath("$", Matchers.is(RegistrationMatcher.matchRegistration(email, null))));

        registrationDataDAO.delete(context, registrationData);
    }

    @Test
    public void findByTokenNotExistingTokenTest() throws Exception {
        getClient().perform(get("/api/eperson/registration/search/findByToken")
                                .param("token", "ThisTokenDoesNotExist"))
                   .andExpect(status().isNotFound());
    }

    private void createTokenForEmail(String email) throws Exception {
        List<RegistrationData> registrationDatas;
        ObjectMapper mapper = new ObjectMapper();
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(email);
        getClient().perform(post("/api/eperson/registrations")
                                .content(mapper.writeValueAsBytes(registrationRest))
                                .contentType(contentType))
                   .andExpect(status().isCreated());
    }
}
