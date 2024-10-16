/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.repository.RegistrationRestRepository.TOKEN_QUERY_PARAM;
import static org.dspace.app.rest.repository.RegistrationRestRepository.TYPE_FORGOT;
import static org.dspace.app.rest.repository.RegistrationRestRepository.TYPE_QUERY_PARAM;
import static org.dspace.app.rest.repository.RegistrationRestRepository.TYPE_REGISTER;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.matcher.RegistrationMatcher;
import org.dspace.app.rest.model.RegistrationRest;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.repository.RegistrationRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.EPersonBuilder;
import org.dspace.core.Email;
import org.dspace.eperson.CaptchaServiceImpl;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.InvalidReCaptchaException;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.RegistrationTypeEnum;
import org.dspace.eperson.dao.RegistrationDataDAO;
import org.dspace.eperson.service.CaptchaService;
import org.dspace.eperson.service.RegistrationDataService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

public class RegistrationRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private CaptchaServiceImpl captchaService;
    @Autowired
    private RegistrationDataDAO registrationDataDAO;
    @Autowired
    private RegistrationDataService registrationDataService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private RegistrationRestRepository registrationRestRepository;
    private static MockedStatic<Email> emailMockedStatic;

    @After
    public void tearDown() throws Exception {
        Iterator<RegistrationData> iterator = registrationDataDAO.findAll(context, RegistrationData.class).iterator();
        while (iterator.hasNext()) {
            RegistrationData registrationData = iterator.next();
            registrationDataDAO.delete(context, registrationData);
        }
    }

    @BeforeClass
    public static void init() throws Exception {
        emailMockedStatic = Mockito.mockStatic(Email.class);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        emailMockedStatic.close();
    }

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
        ObjectMapper mapper = new ObjectMapper();
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(email);
        getClient().perform(post("/api/eperson/registrations")
                                .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
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
                                    .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                                    .content(mapper.writeValueAsBytes(registrationRest))
                                    .contentType(contentType))
                       .andExpect(status().isCreated());
            registrationDataList = registrationDataDAO.findAll(context, RegistrationData.class);
            assertEquals(1, registrationDataList.size());
            assertTrue(StringUtils.equalsIgnoreCase(registrationDataList.get(0).getEmail(), eperson.getEmail()));

            String newEmail = "newEPersonTest@gmail.com";
            registrationRest.setEmail(newEmail);
            getClient().perform(post("/api/eperson/registrations")
                                    .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
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
                                    .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
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
    public void testRegisterDomainRegistered() throws Exception {
        List<RegistrationData> registrationDataList = registrationDataDAO.findAll(context, RegistrationData.class);
        try {
            configurationService.setProperty("authentication-password.domain.valid", "test.com");
            RegistrationRest registrationRest = new RegistrationRest();
            String email = "testPerson@test.com";
            registrationRest.setEmail(email);

            ObjectMapper mapper = new ObjectMapper();
            getClient().perform(post("/api/eperson/registrations")
                                    .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                                    .content(mapper.writeValueAsBytes(registrationRest))
                                    .contentType(contentType))
                       .andExpect(status().isCreated());
            registrationDataList = registrationDataDAO.findAll(context, RegistrationData.class);
            assertEquals(1, registrationDataList.size());
            assertTrue(StringUtils.equalsIgnoreCase(registrationDataList.get(0).getEmail(), email));
        } finally {
            Iterator<RegistrationData> iterator = registrationDataList.iterator();
            while (iterator.hasNext()) {
                RegistrationData registrationData = iterator.next();
                registrationDataDAO.delete(context, registrationData);
            }
        }
    }

    @Test
    public void testRegisterDomainNotRegistered() throws Exception {
        List<RegistrationData> registrationDataList;
        try {
            configurationService.setProperty("authentication-password.domain.valid", "test.com");
            RegistrationRest registrationRest = new RegistrationRest();
            String email = "testPerson@bladibla.com";
            registrationRest.setEmail(email);

            ObjectMapper mapper = new ObjectMapper();
            getClient().perform(post("/api/eperson/registrations")
                                    .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                                    .content(mapper.writeValueAsBytes(registrationRest))
                                    .contentType(contentType))
                       .andExpect(status().isUnprocessableEntity());
        } finally {
            registrationDataList = registrationDataDAO.findAll(context, RegistrationData.class);
            Iterator<RegistrationData> iterator = registrationDataList.iterator();
            while (iterator.hasNext()) {
                RegistrationData registrationData = iterator.next();
                registrationDataDAO.delete(context, registrationData);
            }
        }
    }

    @Test
    public void testRegisterMailAddressRegistered() throws Exception {
        List<RegistrationData> registrationDataList = registrationDataDAO.findAll(context, RegistrationData.class);
        try {
            context.turnOffAuthorisationSystem();
            String email = "test@gmail.com";
            EPersonBuilder.createEPerson(context)
                          .withEmail(email)
                          .withCanLogin(true)
                          .build();
            context.restoreAuthSystemState();

            RegistrationRest registrationRest = new RegistrationRest();
            registrationRest.setEmail(email);

            ObjectMapper mapper = new ObjectMapper();
            getClient().perform(post("/api/eperson/registrations")
                                    .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                                    .content(mapper.writeValueAsBytes(registrationRest))
                                    .contentType(contentType))
                       .andExpect(status().isCreated());
            registrationDataList = registrationDataDAO.findAll(context, RegistrationData.class);
            assertEquals(1, registrationDataList.size());
            assertTrue(StringUtils.equalsIgnoreCase(registrationDataList.get(0).getEmail(), email));
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
                                    .param(TYPE_QUERY_PARAM, TYPE_FORGOT)
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

    @Test
    public void testUnauthorizedForgotPasswordTest() throws Exception {
        configurationService.setProperty("user.registration", false);
        configurationService.setProperty("user.forgot-password", false);

        List<RegistrationData> registrationDataList = registrationDataDAO.findAll(context, RegistrationData.class);
        try {
            assertEquals(0, registrationDataList.size());

            ObjectMapper mapper = new ObjectMapper();
            RegistrationRest registrationRest = new RegistrationRest();
            registrationRest.setEmail(eperson.getEmail());
            getClient().perform(post("/api/eperson/registrations")
                                    .param(TYPE_QUERY_PARAM, TYPE_FORGOT)
                                    .content(mapper.writeValueAsBytes(registrationRest))
                                    .contentType(contentType))
                       .andExpect(status().isUnauthorized());
            registrationDataList = registrationDataDAO.findAll(context, RegistrationData.class);
            assertEquals(0, registrationDataList.size());
        } finally {
            Iterator<RegistrationData> iterator = registrationDataList.iterator();
            while (iterator.hasNext()) {
                RegistrationData registrationData = iterator.next();
                registrationDataDAO.delete(context, registrationData);
            }
        }
    }

    @Test
    public void registrationFlowWithNoHeaderCaptchaTokenTest() throws Exception {
        String originVerification = configurationService.getProperty("registration.verification.enabled");
        String originSecret = configurationService.getProperty("google.recaptcha.key.secret");
        String originVresion = configurationService.getProperty("google.recaptcha.version");
        reloadCaptchaProperties("true", "test-secret", "v2");

        ObjectMapper mapper = new ObjectMapper();
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(eperson.getEmail());

        // when reCAPTCHA enabled and request doesn't contain "X-Recaptcha-Token” header
        getClient().perform(post("/api/eperson/registrations")
                            .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                   .content(mapper.writeValueAsBytes(registrationRest))
                   .contentType(contentType))
                   .andExpect(status().isForbidden());

        reloadCaptchaProperties(originVerification, originSecret, originVresion);
    }

    @Test
    public void registrationFlowWithInvalidCaptchaTokenTest() throws Exception {
        String originVerification = configurationService.getProperty("registration.verification.enabled");
        String originSecret = configurationService.getProperty("google.recaptcha.key.secret");
        String originVresion = configurationService.getProperty("google.recaptcha.version");
        reloadCaptchaProperties("true", "test-secret", "v2");

        ObjectMapper mapper = new ObjectMapper();
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(eperson.getEmail());

        String captchaToken = "invalid-captcha-Token";
        // when reCAPTCHA enabled and request contains Invalid "X-Recaptcha-Token” header
        getClient().perform(post("/api/eperson/registrations")
                   .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                   .header("X-Recaptcha-Token", captchaToken)
                   .content(mapper.writeValueAsBytes(registrationRest))
                   .contentType(contentType))
                   .andExpect(status().isForbidden());

        reloadCaptchaProperties(originVerification, originSecret, originVresion);
    }

    @Test
    public void registrationFlowWithValidCaptchaTokenTest() throws Exception {
        String originVerification = configurationService.getProperty("registration.verification.enabled");
        String originSecret = configurationService.getProperty("google.recaptcha.key.secret");
        String originVresion = configurationService.getProperty("google.recaptcha.version");
        reloadCaptchaProperties("true", "test-secret", "v2");

        String captchaToken = "123456";
        String captchaToken1 = "12345676866";

        CaptchaService captchaServiceMock = mock(CaptchaService.class);

        registrationRestRepository.setCaptchaService(captchaServiceMock);

        doThrow(new InvalidReCaptchaException("Invalid captcha token"))
            .when(captchaServiceMock).processResponse(any(), any());

        doNothing().when(captchaServiceMock).processResponse(eq(captchaToken), eq("register_email"));

        List<RegistrationData> registrationDataList = registrationDataDAO.findAll(context, RegistrationData.class);
        assertEquals(0, registrationDataList.size());

        ObjectMapper mapper = new ObjectMapper();
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(eperson.getEmail());
        try {
            // will throw InvalidReCaptchaException because 'X-Recaptcha-Token' not equal captchaToken
            getClient().perform(post("/api/eperson/registrations")
                       .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                       .header("X-Recaptcha-Token", captchaToken1)
                       .content(mapper.writeValueAsBytes(registrationRest))
                       .contentType(contentType))
                       .andExpect(status().isForbidden());

            getClient().perform(post("/api/eperson/registrations")
                       .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                       .header("X-Recaptcha-Token", captchaToken)
                       .content(mapper.writeValueAsBytes(registrationRest))
                       .contentType(contentType))
                       .andExpect(status().isCreated());

            registrationDataList = registrationDataDAO.findAll(context, RegistrationData.class);
            assertEquals(1, registrationDataList.size());
            assertTrue(StringUtils.equalsIgnoreCase(registrationDataList.get(0).getEmail(), eperson.getEmail()));

            String newEmail = "newEPersonTest@gmail.com";
            registrationRest.setEmail(newEmail);
            getClient().perform(post("/api/eperson/registrations")
                       .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
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
                       .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                       .header("X-Recaptcha-Token", captchaToken)
                       .content(mapper.writeValueAsBytes(registrationRest))
                       .contentType(contentType))
                       .andExpect(status().is(HttpServletResponse.SC_UNAUTHORIZED));

            assertEquals(2, registrationDataList.size());
            assertTrue(!StringUtils.equalsIgnoreCase(registrationDataList.get(0).getEmail(), newEmail) &&
                       !StringUtils.equalsIgnoreCase(registrationDataList.get(1).getEmail(), newEmail));
        } finally {
            registrationRestRepository.setCaptchaService(captchaService);
            Iterator<RegistrationData> iterator = registrationDataList.iterator();
            while (iterator.hasNext()) {
                RegistrationData registrationData = iterator.next();
                registrationDataDAO.delete(context, registrationData);
            }
            reloadCaptchaProperties(originVerification, originSecret, originVresion);
        }
    }

    private void reloadCaptchaProperties(String verification, String secret, String version) {
        configurationService.setProperty("registration.verification.enabled", verification);
        configurationService.setProperty("google.recaptcha.key.secret", secret);
        configurationService.setProperty("google.recaptcha.version", version);
        captchaService.init();
    }

    @Test
    public void accountEndpoint_WithoutAccountTypeParam() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(eperson.getEmail());
        getClient().perform(post("/api/eperson/registrations")
            .content(mapper.writeValueAsBytes(registrationRest))
            .contentType(contentType))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void accountEndpoint_WrongAccountTypeParam() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(eperson.getEmail());
        getClient().perform(post("/api/eperson/registrations")
            .param(TYPE_QUERY_PARAM, "nonValidValue")
            .content(mapper.writeValueAsBytes(registrationRest))
            .contentType(contentType))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void givenRegistrationData_whenPatchInvalidValue_thenUnprocessableEntityResponse()
        throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(eperson.getEmail());
        registrationRest.setUser(eperson.getID());

        Email spy = Mockito.spy(Email.class);
        doNothing().when(spy).send();

        emailMockedStatic.when(() -> Email.getEmail(any())).thenReturn(spy);

        // given RegistrationData with email
        getClient().perform(post("/api/eperson/registrations")
                                .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                                .content(mapper.writeValueAsBytes(registrationRest))
                                .contentType(contentType))
                   .andExpect(status().isCreated());

        RegistrationData registrationData =
            registrationDataService.findByEmail(context, registrationRest.getEmail());

        assertThat(registrationData, notNullValue());
        assertThat(registrationData.getToken(), not(emptyOrNullString()));

        String token = registrationData.getToken();
        String newMail = null;
        String patchContent = getPatchContent(
            List.of(new ReplaceOperation("/email", newMail))
        );

        // when patch for replace email
        getClient().perform(patch("/api/eperson/registrations/" + registrationData.getID())
                                .param(TOKEN_QUERY_PARAM, token)
                                .content(patchContent)
                                .contentType(contentType))
                   // then succesful response returned
                   .andExpect(status().isBadRequest());

        newMail = "test@email.com";
        patchContent = getPatchContent(
            List.of(new AddOperation("/email", newMail))
        );

        getClient().perform(patch("/api/eperson/registrations/" + registrationData.getID())
                                .param(TOKEN_QUERY_PARAM, token)
                                .content(patchContent)
                                .contentType(contentType))
                   // then succesful response returned
                   .andExpect(status().isUnprocessableEntity());

        newMail = "invalidemail!!!!";
        patchContent = getPatchContent(
            List.of(new ReplaceOperation("/email", newMail))
        );

        getClient().perform(patch("/api/eperson/registrations/" + registrationData.getID())
                                .param(TOKEN_QUERY_PARAM, token)
                                .content(patchContent)
                                .contentType(contentType))
                   // then succesful response returned
                   .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void givenRegistrationData_whenPatchWithInvalidToken_thenUnprocessableEntityResponse()
        throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(eperson.getEmail());
        registrationRest.setUser(eperson.getID());

        Email spy = Mockito.spy(Email.class);
        doNothing().when(spy).send();

        emailMockedStatic.when(() -> Email.getEmail(any())).thenReturn(spy);

        // given RegistrationData with email
        getClient().perform(post("/api/eperson/registrations")
                                .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                                .content(mapper.writeValueAsBytes(registrationRest))
                                .contentType(contentType))
                   .andExpect(status().isCreated());

        RegistrationData registrationData =
            registrationDataService.findByEmail(context, registrationRest.getEmail());


        assertThat(registrationData, notNullValue());
        assertThat(registrationData.getToken(), not(emptyOrNullString()));

        String token = null;
        String newMail = "validemail@email.com";
        String patchContent = getPatchContent(
            List.of(new ReplaceOperation("/email", newMail))
        );

        // when patch for replace email
        getClient().perform(patch("/api/eperson/registrations/" + registrationData.getID())
                                .param(TOKEN_QUERY_PARAM, token)
                                .content(patchContent)
                                .contentType(contentType))
                   // then succesful response returned
                   .andExpect(status().isUnauthorized());

        token = "notexistingtoken";

        // when patch for replace email
        getClient().perform(patch("/api/eperson/registrations/" + registrationData.getID())
                                .param(TOKEN_QUERY_PARAM, token)
                                .content(patchContent)
                                .contentType(contentType))
                   // then succesful response returned
                   .andExpect(status().isUnauthorized());

        context.turnOffAuthorisationSystem();
        registrationData = context.reloadEntity(registrationData);
        registrationDataService.markAsExpired(context, registrationData);
        context.commit();
        context.restoreAuthSystemState();

        registrationData = context.reloadEntity(registrationData);

        assertThat(registrationData.getExpires(), notNullValue());

        token = registrationData.getToken();
        newMail = "validemail@email.com";
        patchContent = getPatchContent(
            List.of(new ReplaceOperation("/email", newMail))
        );

        // when patch for replace email
        getClient().perform(patch("/api/eperson/registrations/" + registrationData.getID())
                                .param(TOKEN_QUERY_PARAM, token)
                                .content(patchContent)
                                .contentType(contentType))
                   // then succesful response returned
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void givenRegistrationDataWithEmail_whenPatchForReplaceEmail_thenSuccessfullResponse()
        throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(eperson.getEmail());
        registrationRest.setUser(eperson.getID());

        // given RegistrationData with email
        getClient().perform(post("/api/eperson/registrations")
                                .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                                .content(mapper.writeValueAsBytes(registrationRest))
                                .contentType(contentType))
                   .andExpect(status().isCreated());

        RegistrationData registrationData =
            registrationDataService.findByEmail(context, registrationRest.getEmail());

        assertThat(registrationData, notNullValue());
        assertThat(registrationData.getToken(), not(emptyOrNullString()));

        String token = registrationData.getToken();
        String newMail = "vincenzo.mecca@4science.com";
        String patchContent = getPatchContent(
            List.of(new ReplaceOperation("/email", newMail))
        );

        // when patch for replace email
        getClient().perform(patch("/api/eperson/registrations/" + registrationData.getID())
                                .param(TOKEN_QUERY_PARAM, token)
                                .content(patchContent)
                                .contentType(contentType))
                   // then succesful response returned
                   .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void givenRegistrationDataWithoutEmail_whenPatchForAddEmail_thenSuccessfullResponse()
        throws Exception {

        RegistrationData registrationData =
            createNewRegistrationData("0000-1111-2222-3333", RegistrationTypeEnum.ORCID);

        assertThat(registrationData, notNullValue());
        assertThat(registrationData.getToken(), not(emptyOrNullString()));

        String token = registrationData.getToken();
        String newMail = "vincenzo.mecca@4science.com";
        String patchContent = getPatchContent(
            List.of(new AddOperation("/email", newMail))
        );

        // when patch for replace email
        getClient().perform(patch("/api/eperson/registrations/" + registrationData.getID())
                                .param(TOKEN_QUERY_PARAM, token)
                                .content(patchContent)
                                .contentType(contentType))
                   // then succesful response returned
                   .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void givenRegistrationDataWithEmail_whenPatchForReplaceEmail_thenNewRegistrationDataCreated()
        throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(eperson.getEmail());
        registrationRest.setUser(eperson.getID());

        // given RegistrationData with email
        getClient().perform(post("/api/eperson/registrations")
                                .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                                .content(mapper.writeValueAsBytes(registrationRest))
                                .contentType(contentType))
                   .andExpect(status().isCreated());

        RegistrationData registrationData =
            registrationDataService.findByEmail(context, registrationRest.getEmail());

        assertThat(registrationData, notNullValue());
        assertThat(registrationData.getToken(), not(emptyOrNullString()));

        String token = registrationData.getToken();
        String newMail = "vincenzo.mecca@4science.com";
        String patchContent = getPatchContent(
            List.of(new ReplaceOperation("/email", newMail))
        );

        // when patch for replace email
        getClient().perform(patch("/api/eperson/registrations/" + registrationData.getID())
                                .param(TOKEN_QUERY_PARAM, token)
                                .content(patchContent)
                                .contentType(contentType))
                   .andExpect(status().is2xxSuccessful());

        // then email updated with new registration
        RegistrationData newRegistration = registrationDataService.findByEmail(context, newMail);
        assertThat(newRegistration, notNullValue());
        assertThat(newRegistration.getToken(), not(emptyOrNullString()));
        assertThat(newRegistration.getEmail(), equalTo(newMail));

        assertThat(newRegistration.getEmail(), not(equalTo(registrationData.getEmail())));
        assertThat(newRegistration.getToken(), not(equalTo(registrationData.getToken())));

        registrationData = context.reloadEntity(registrationData);
        assertThat(registrationData, nullValue());
    }

    @Test
    public void givenRegistrationDataWithoutEmail_whenPatchForReplaceEmail_thenNewRegistrationDataCreated()
        throws Exception {
        RegistrationData registrationData =
            createNewRegistrationData("0000-1111-2222-3333", RegistrationTypeEnum.ORCID);

        assertThat(registrationData.getToken(), not(emptyOrNullString()));

        String token = registrationData.getToken();
        String newMail = "vincenzo.mecca@4science.com";
        String patchContent = getPatchContent(
            List.of(new AddOperation("/email", newMail))
        );

        // when patch for replace email
        getClient().perform(patch("/api/eperson/registrations/" + registrationData.getID())
                                .param(TOKEN_QUERY_PARAM, token)
                                .content(patchContent)
                                .contentType(contentType))
                   .andExpect(status().is2xxSuccessful());

        // then email updated with new registration
        RegistrationData newRegistration = registrationDataService.findByEmail(context, newMail);
        assertThat(newRegistration, notNullValue());
        assertThat(newRegistration.getToken(), not(emptyOrNullString()));
        assertThat(newRegistration.getEmail(), equalTo(newMail));

        assertThat(newRegistration.getEmail(), not(equalTo(registrationData.getEmail())));
        assertThat(newRegistration.getToken(), not(equalTo(registrationData.getToken())));

        registrationData = context.reloadEntity(registrationData);
        assertThat(registrationData, nullValue());
    }

    @Test
    public void givenRegistrationDataWithoutEmail_whenPatchForAddEmail_thenExternalLoginSent() throws Exception {
        RegistrationData registrationData =
            createNewRegistrationData("0000-1111-2222-3333", RegistrationTypeEnum.ORCID);

        assertThat(registrationData, notNullValue());
        assertThat(registrationData.getToken(), not(emptyOrNullString()));

        String token = registrationData.getToken();
        String newMail = "vincenzo.mecca@4science.com";
        String patchContent = getPatchContent(
            List.of(new AddOperation("/email", newMail))
        );

        Email spy = Mockito.spy(Email.class);
        doNothing().when(spy).send();

        emailMockedStatic.when(() -> Email.getEmail(any())).thenReturn(spy);

        // when patch for replace email
        getClient().perform(patch("/api/eperson/registrations/" + registrationData.getID())
                                .param(TOKEN_QUERY_PARAM, token)
                                .content(patchContent)
                                .contentType(contentType))
                   .andExpect(status().is2xxSuccessful());

        // then verification email sent
        verify(spy, times(1)).addRecipient(newMail);
        verify(spy).addArgument(
            ArgumentMatchers.contains(
                RegistrationTypeEnum.ORCID.getLink()
            )
        );
        verify(spy, times(1)).send();
    }

    @Test
    public void givenRegistrationDataWithEmail_whenPatchForNewEmail_thenExternalLoginSent() throws Exception {
        RegistrationData registrationData =
            createNewRegistrationData("0000-1111-2222-3333", RegistrationTypeEnum.ORCID);

        String token = registrationData.getToken();
        String newMail = "vincenzo.mecca@orcid.com";
        String patchContent = getPatchContent(
            List.of(new AddOperation("/email", newMail))
        );

        Email spy = Mockito.spy(Email.class);
        doNothing().when(spy).send();

        emailMockedStatic.when(() -> Email.getEmail(any())).thenReturn(spy);

        // when patch for replace email
        getClient().perform(patch("/api/eperson/registrations/" + registrationData.getID())
                                .param(TOKEN_QUERY_PARAM, token)
                                .content(patchContent)
                                .contentType(contentType))
                   .andExpect(status().is2xxSuccessful());

        verify(spy, times(1)).addRecipient(newMail);
        verify(spy).addArgument(
            ArgumentMatchers.contains(
                registrationData.getRegistrationType().getLink()
            )
        );
        verify(spy, times(1)).send();

        registrationData = registrationDataService.findByEmail(context, newMail);

        assertThat(registrationData, notNullValue());
        assertThat(registrationData.getToken(), not(emptyOrNullString()));

        token = registrationData.getToken();
        newMail = "vincenzo.mecca@4science.com";
        patchContent = getPatchContent(
            List.of(new ReplaceOperation("/email", newMail))
        );

        spy = Mockito.spy(Email.class);
        doNothing().when(spy).send();

        emailMockedStatic.when(() -> Email.getEmail(any())).thenReturn(spy);

        // when patch for replace email
        getClient().perform(patch("/api/eperson/registrations/" + registrationData.getID())
                                .param(TOKEN_QUERY_PARAM, token)
                                .content(patchContent)
                                .contentType(contentType))
                   .andExpect(status().is2xxSuccessful());

        // then verification email sent
        verify(spy, times(1)).addRecipient(newMail);
        verify(spy).addArgument(
            ArgumentMatchers.contains(
                registrationData.getRegistrationType().getLink()
            )
        );
        verify(spy, times(1)).send();
    }

    @Test
    public void givenRegistrationDataWithEmail_whenPatchForExistingEPersonEmail_thenReviewAccountLinkSent()
        throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(eperson.getEmail());
        registrationRest.setNetId("0000-0000-0000-0000");

        // given RegistrationData with email
        getClient().perform(post("/api/eperson/registrations")
                                .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                                .content(mapper.writeValueAsBytes(registrationRest))
                                .contentType(contentType))
                   .andExpect(status().isCreated());

        RegistrationData registrationData =
            registrationDataService.findByEmail(context, registrationRest.getEmail());

        assertThat(registrationData, notNullValue());
        assertThat(registrationData.getToken(), not(emptyOrNullString()));

        context.turnOffAuthorisationSystem();
        final EPerson vins =
            EPersonBuilder.createEPerson(context)
                          .withEmail("vincenzo.mecca@4science.com")
                          .withNameInMetadata("Vincenzo", "Mecca")
                          .withOrcid("0101-0101-0101-0101")
                          .build();
        context.restoreAuthSystemState();

        String token = registrationData.getToken();
        String vinsEmail = vins.getEmail();
        String patchContent = getPatchContent(
            List.of(new ReplaceOperation("/email", vins.getEmail()))
        );

        Email spy = Mockito.spy(Email.class);
        doNothing().when(spy).send();

        emailMockedStatic.when(() -> Email.getEmail(any())).thenReturn(spy);

        // when patch for replace email
        getClient().perform(patch("/api/eperson/registrations/" + registrationData.getID())
                                .param(TOKEN_QUERY_PARAM, token)
                                .content(patchContent)
                                .contentType(contentType))
                   .andExpect(status().is2xxSuccessful());

        // then verification email sent
        verify(spy, times(1)).addRecipient(vinsEmail);
        verify(spy).addArgument(
            ArgumentMatchers.contains(
                RegistrationTypeEnum.VALIDATION_ORCID.getLink()
            )
        );
        verify(spy, times(1)).send();
    }

    @Test
    public void givenRegistrationDataWithoutEmail_whenPatchForExistingAccount_thenReviewAccountSent() throws Exception {
        RegistrationData registrationData =
            createNewRegistrationData("0000-1111-2222-3333", RegistrationTypeEnum.ORCID);

        assertThat(registrationData, notNullValue());
        assertThat(registrationData.getToken(), not(emptyOrNullString()));

        context.turnOffAuthorisationSystem();
        final EPerson vins =
            EPersonBuilder.createEPerson(context)
                          .withEmail("vincenzo.mecca@4science.com")
                          .withNameInMetadata("Vincenzo", "Mecca")
                          .withOrcid("0101-0101-0101-0101")
                          .build();
        context.commit();
        context.restoreAuthSystemState();

        String token = registrationData.getToken();
        String vinsEmail = vins.getEmail();
        String patchContent = getPatchContent(
            List.of(new AddOperation("/email", vins.getEmail()))
        );

        Email spy = Mockito.spy(Email.class);
        doNothing().when(spy).send();

        emailMockedStatic.when(() -> Email.getEmail(any())).thenReturn(spy);

        // when patch for replace email
        getClient().perform(patch("/api/eperson/registrations/" + registrationData.getID())
                                .param(TOKEN_QUERY_PARAM, token)
                                .content(patchContent)
                                .contentType(contentType))
                   .andExpect(status().is2xxSuccessful());

        // then verification email sent
        verify(spy, times(1)).addRecipient(vinsEmail);
        verify(spy).addArgument(
            ArgumentMatchers.contains(
                RegistrationTypeEnum.VALIDATION_ORCID.getLink()
            )
        );
        verify(spy, times(1)).send();
    }


    private RegistrationData createNewRegistrationData(
        String netId, RegistrationTypeEnum type
    ) throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        RegistrationData registrationData =
            registrationDataService.create(context, netId, type);
        context.commit();
        context.restoreAuthSystemState();
        return registrationData;
    }

}
