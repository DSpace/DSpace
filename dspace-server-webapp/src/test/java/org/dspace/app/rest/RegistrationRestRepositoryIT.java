/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.matcher.RegistrationMatcher;
import org.dspace.app.rest.model.RegistrationRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.dao.RegistrationDataDAO;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class RegistrationRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private RegistrationDataDAO registrationDataDAO;

    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void findByTokenTestExistingUserTest() throws Exception {
        String email = eperson.getEmail();
        createTokenForEmail(email);
        RegistrationData registrationData = registrationDataDAO.findByEmail(context, email);

        try {
            getClient().perform(get("/api/eperson/registrations/search/findByToken")
                                    .param("token", registrationData.getToken()))
                       .andExpect(status().isOk())
                       .andExpect(
                           jsonPath("$", Matchers.is(RegistrationMatcher.matchRegistration(email, eperson.getID()))));

            registrationDataDAO.delete(context, registrationData);

            email = "newUser@testnewuser.com";
            createTokenForEmail(email);
            registrationData = registrationDataDAO.findByEmail(context, email);

            getClient().perform(get("/api/eperson/registrations/search/findByToken")
                                    .param("token", registrationData.getToken()))
                       .andExpect(status().isOk())
                       .andExpect(
                           jsonPath("$", Matchers.is(RegistrationMatcher.matchRegistration(email, null))));
        } finally {
            registrationDataDAO.delete(context, registrationData);
        }


    }

    @Test
    public void findByTokenTestNewUserTest() throws Exception {
        String email = "newUser@testnewuser.com";
        createTokenForEmail(email);
        RegistrationData registrationData = registrationDataDAO.findByEmail(context, email);

        try {
            getClient().perform(get("/api/eperson/registrations/search/findByToken")
                                    .param("token", registrationData.getToken()))
                       .andExpect(status().isOk())
                       .andExpect(
                           jsonPath("$", Matchers.is(RegistrationMatcher.matchRegistration(email, null))));
        } finally {
            registrationDataDAO.delete(context, registrationData);
        }

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

    @Test
    public void registrationFlowTest() throws Exception {
        List<RegistrationData> registrationDataList = registrationDataDAO.findAll(context, RegistrationData.class);
        assertEquals(0, registrationDataList.size());

        ObjectMapper mapper = new ObjectMapper();
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(eperson.getEmail());

        try {
            getClient().perform(post("/api/eperson/registrations")
                                    .content(mapper.writeValueAsBytes(registrationRest))
                                    .contentType(contentType))
                       .andExpect(status().isCreated());
            registrationDataList = registrationDataDAO.findAll(context, RegistrationData.class);
            assertEquals(1, registrationDataList.size());
            assertTrue(StringUtils.equalsIgnoreCase(registrationDataList.get(0).getEmail(), eperson.getEmail()));

            String newEmail = "newEPersonTest@gmail.com";
            registrationRest.setEmail(newEmail);
            getClient().perform(post("/api/eperson/registrations")
                                    .content(mapper.writeValueAsBytes(registrationRest))
                                    .contentType(contentType))
                       .andExpect(status().isCreated());
            registrationDataList = registrationDataDAO.findAll(context, RegistrationData.class);
            assertTrue(registrationDataList.size() == 2);
            assertTrue(StringUtils.equalsIgnoreCase(registrationDataList.get(0).getEmail(), newEmail) ||
                           StringUtils.equalsIgnoreCase(registrationDataList.get(1).getEmail(), newEmail));
            configurationService.setProperty("user.registration", false);

            newEmail = "newEPersonTestTwo@gmail.com";
            registrationRest.setEmail(newEmail);
            getClient().perform(post("/api/eperson/registrations")
                                    .content(mapper.writeValueAsBytes(registrationRest))
                                    .contentType(contentType))
                       .andExpect(status().is(HttpServletResponse.SC_UNAUTHORIZED));

            assertEquals(2, registrationDataList.size());
            assertTrue(!StringUtils.equalsIgnoreCase(registrationDataList.get(0).getEmail(), newEmail) &&
                           !StringUtils.equalsIgnoreCase(registrationDataList.get(1).getEmail(), newEmail));
        } finally {
            Iterator<RegistrationData> iterator = registrationDataList.iterator();
            while (iterator.hasNext()) {
                RegistrationData registrationData = iterator.next();
                registrationDataDAO.delete(context, registrationData);
            }
        }
    }

    @Test
    public void forgotPasswordTest() throws Exception {
        configurationService.setProperty("user.registration", false);

        List<RegistrationData> registrationDataList = registrationDataDAO.findAll(context, RegistrationData.class);
        try {
            assertEquals(0, registrationDataList.size());

            ObjectMapper mapper = new ObjectMapper();
            RegistrationRest registrationRest = new RegistrationRest();
            registrationRest.setEmail(eperson.getEmail());
            getClient().perform(post("/api/eperson/registrations")
                                    .content(mapper.writeValueAsBytes(registrationRest))
                                    .contentType(contentType))
                       .andExpect(status().isCreated());
            registrationDataList = registrationDataDAO.findAll(context, RegistrationData.class);
            assertEquals(1, registrationDataList.size());
            assertTrue(StringUtils.equalsIgnoreCase(registrationDataList.get(0).getEmail(), eperson.getEmail()));
        } finally {
            Iterator<RegistrationData> iterator = registrationDataList.iterator();
            while (iterator.hasNext()) {
                RegistrationData registrationData = iterator.next();
                registrationDataDAO.delete(context, registrationData);
            }
        }
    }

}
