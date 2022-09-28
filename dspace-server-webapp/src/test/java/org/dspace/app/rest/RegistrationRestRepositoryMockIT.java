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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.RegistrationRest;
import org.dspace.app.rest.repository.RegistrationRestRepository;
import org.dspace.eperson.CaptchaServiceImpl;
import org.dspace.eperson.InvalidReCaptchaException;
import org.dspace.eperson.RegistrationData;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class RegistrationRestRepositoryMockIT extends RegistrationRestRepositoryIT {

    @Mock
    private RegistrationRestRepository registrationRestRepository;

    @InjectMocks
    private CaptchaServiceImpl captchaService;

    @Test
    public void registrationFlowWithValidCaptchaTokenTest() throws Exception {

        String captchaToken = "123456";
        String captchaToken1 = "12345676866";

        configurationService.setProperty("registration.verification.enabled", "true");

        doThrow(new InvalidReCaptchaException("Invalid captcha token"))
        .when(captchaService).processResponse(any(), any());

        doNothing().when(captchaService).processResponse(eq(captchaToken), eq("register_email"));

        List<RegistrationData> registrationDataList = registrationDataDAO.findAll(context, RegistrationData.class);
        assertEquals(0, registrationDataList.size());

        ObjectMapper mapper = new ObjectMapper();
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail("test-email@test.com");

        try {
            // will throw InvalidReCaptchaException because 'X-Recaptcha-Token' not equal captchaToken
            getClient().perform(post("/api/eperson/registrations")
                       .header("X-Recaptcha-Token", captchaToken1)
                       .content(mapper.writeValueAsBytes(registrationRest))
                       .contentType(contentType))
                       .andExpect(status().isForbidden());

            getClient().perform(post("/api/eperson/registrations")
                       .header("X-Recaptcha-Token", captchaToken)
                       .content(mapper.writeValueAsBytes(registrationRest))
                       .contentType(contentType))
                       .andExpect(status().isCreated());

            registrationDataList = registrationDataDAO.findAll(context, RegistrationData.class);
            assertEquals(1, registrationDataList.size());
            assertTrue(StringUtils.equalsIgnoreCase(registrationDataList.get(0).getEmail(), "test-email@test.com"));

            String newEmail = "newEPersonTest@gmail.com";
            registrationRest.setEmail(newEmail);
            getClient().perform(post("/api/eperson/registrations")
                       .header("X-Recaptcha-Token", captchaToken)
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
                       .header("X-Recaptcha-Token", captchaToken)
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

}