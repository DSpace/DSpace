/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.RegistrationRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.dao.RegistrationDataDAO;
import org.dspace.services.ConfigurationService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class RegistrationRestControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    private RegistrationDataDAO registrationDataDAO;

    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void registrationFlowTest() throws Exception {
        List<RegistrationData> registrationData = registrationDataDAO.findAll(context, RegistrationData.class);
        assertTrue(registrationData.isEmpty());

        String token = getAuthToken(eperson.getEmail(), password);
        String t = ";;";
        ObjectMapper mapper = new ObjectMapper();
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(eperson.getEmail());
        getClient().perform(post("/api/eperson/registrations")
                                .content(mapper.writeValueAsBytes(registrationRest))
                                .contentType(contentType))
                            .andExpect(status().isCreated());
        registrationData = registrationDataDAO.findAll(context, RegistrationData.class);
        assertTrue(registrationData.size() == 1);
        assertTrue(StringUtils.equalsIgnoreCase(registrationData.get(0).getEmail(), eperson.getEmail()));

        String newEmail = "newEPersonTest@gmail.com";
        registrationRest.setEmail(newEmail);
        getClient().perform(post("/api/eperson/registrations")
                                .content(mapper.writeValueAsBytes(registrationRest))
                                .contentType(contentType))
                   .andExpect(status().isCreated());
        registrationData = registrationDataDAO.findAll(context, RegistrationData.class);
        assertTrue(registrationData.size() == 2);
        assertTrue(StringUtils.equalsIgnoreCase(registrationData.get(0).getEmail(), newEmail) ||
                       StringUtils.equalsIgnoreCase(registrationData.get(1).getEmail(), newEmail));
        configurationService.setProperty("user.registration", false);

        newEmail = "newEPersonTestTwo@gmail.com";
        registrationRest.setEmail(newEmail);
        getClient().perform(post("/api/eperson/registrations")
                                .content(mapper.writeValueAsBytes(registrationRest))
                                .contentType(contentType))
                   .andExpect(status().is(500));

        assertTrue(registrationData.size() == 2);
        assertTrue(!StringUtils.equalsIgnoreCase(registrationData.get(0).getEmail(), newEmail) &&
                       !StringUtils.equalsIgnoreCase(registrationData.get(1).getEmail(), newEmail));
    }
}
