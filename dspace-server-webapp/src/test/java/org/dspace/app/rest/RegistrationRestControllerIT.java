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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.RegistrationRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.dao.RegistrationDataDAO;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class RegistrationRestControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    private RegistrationDataDAO registrationDataDAO;

    @Autowired
    private ConfigurationService configurationService;


    @Before
    public void setup() throws SQLException {
        CollectionUtils.emptyIfNull(registrationDataDAO.findAll(context, RegistrationData.class)).stream()
                       .forEach(registrationData -> {
                           try {
                               registrationDataDAO.delete(context, registrationData);
                           } catch (SQLException e) {
                               throw new RuntimeException(e);
                           }
                       });
    }

    @Test
    public void registrationFlowTest() throws Exception {
        List<RegistrationData> registrationDataList = registrationDataDAO.findAll(context, RegistrationData.class);
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
                   .andExpect(status().is(401));

        assertEquals(2, registrationDataList.size());
        assertTrue(!StringUtils.equalsIgnoreCase(registrationDataList.get(0).getEmail(), newEmail) &&
                       !StringUtils.equalsIgnoreCase(registrationDataList.get(1).getEmail(), newEmail));

        Iterator<RegistrationData> iterator = registrationDataList.iterator();
        while (iterator.hasNext()) {
            RegistrationData registrationData = iterator.next();
            registrationDataDAO.delete(context, registrationData);
        }
    }

    @Test
    public void forgotPasswordTest() throws Exception {
        context.turnOffAuthorisationSystem();
        configurationService.setProperty("user.registration", false);
        context.restoreAuthSystemState();

        List<RegistrationData> registrationDataList = registrationDataDAO.findAll(context, RegistrationData.class);
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
        Iterator<RegistrationData> iterator = registrationDataList.iterator();
        while (iterator.hasNext()) {
            RegistrationData registrationData = iterator.next();
            registrationDataDAO.delete(context, registrationData);
        }
    }
}
