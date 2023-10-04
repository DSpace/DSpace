/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.MetadataMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.EPersonBuilder;
import org.dspace.content.MetadataField;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Email;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.RegistrationTypeEnum;
import org.dspace.eperson.dto.RegistrationDataChanges;
import org.dspace.eperson.dto.RegistrationDataPatch;
import org.dspace.eperson.service.AccountService;
import org.dspace.eperson.service.RegistrationDataService;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class EPersonRegistrationRestControllerIT extends AbstractControllerIntegrationTest {

    private static MockedStatic<Email> emailMockedStatic;

    @Autowired
    private AccountService accountService;
    @Autowired
    private RegistrationDataService registrationDataService;
    @Autowired
    private MetadataFieldService metadataFieldService;

    private RegistrationData orcidRegistration;
    private MetadataField orcidMf;
    private MetadataField firstNameMf;
    private MetadataField lastNameMf;
    private EPerson customEPerson;
    private String customPassword;


    @BeforeClass
    public static void init() throws Exception {
        emailMockedStatic = Mockito.mockStatic(Email.class);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        emailMockedStatic.close();
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        orcidRegistration =
            registrationDataService.create(context, "0000-0000-0000-0000", RegistrationTypeEnum.ORCID);

        orcidMf =
            metadataFieldService.findByElement(context, "eperson", "orcid", null);
        firstNameMf =
            metadataFieldService.findByElement(context, "eperson", "firstname", null);
        lastNameMf =
            metadataFieldService.findByElement(context, "eperson", "lastname", null);

        registrationDataService.addMetadata(
            context, orcidRegistration, orcidMf, "0000-0000-0000-0000"
        );
        registrationDataService.addMetadata(
            context, orcidRegistration, firstNameMf, "Vincenzo"
        );
        registrationDataService.addMetadata(
            context, orcidRegistration, lastNameMf, "Mecca"
        );

        registrationDataService.update(context, orcidRegistration);

        customPassword = "vins-01";
        customEPerson =
            EPersonBuilder.createEPerson(context)
                          .withEmail("vincenzo.mecca@4science.com")
                          .withNameInMetadata("Vins", "4Science")
                          .withPassword(customPassword)
                          .withCanLogin(true)
                          .build();

        context.restoreAuthSystemState();
    }


    @Test
    public void givenOrcidToken_whenPostForMerge_thenUnauthorized() throws Exception {

        getClient().perform(
            post("/api/eperson/epersons/" + customEPerson.getID())
                .param("token", orcidRegistration.getToken())
                .param("override", "eperson.firtname,eperson.lastname,eperson.orcid")
        ).andExpect(status().isUnauthorized());

    }

    @Test
    public void givenExpiredToken_whenPostForMerge_thenUnauthorized() throws Exception {

        context.turnOffAuthorisationSystem();
        registrationDataService.markAsExpired(context, orcidRegistration);
        context.restoreAuthSystemState();

        getClient().perform(
            post("/api/eperson/epersons/" + customEPerson.getID())
                .param("token", orcidRegistration.getToken())
                .param("override", "eperson.firtname,eperson.lastname,eperson.orcid")
        ).andExpect(status().isUnauthorized());

    }

    @Test
    public void givenExpiredToken_whenPostAuthForMerge_thenForbidden() throws Exception {

        context.turnOffAuthorisationSystem();
        registrationDataService.markAsExpired(context, orcidRegistration);
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenAdmin).perform(
            post("/api/eperson/epersons/" + customEPerson.getID())
                .param("token", orcidRegistration.getToken())
                .param("override", "eperson.firtname,eperson.lastname,eperson.orcid")
        ).andExpect(status().isForbidden());

    }

    @Test
    public void givenValidationRegistration_whenPostAuthDiffersFromIdPathParam_thenForbidden() throws Exception {

        context.turnOffAuthorisationSystem();
        RegistrationData validationRegistration =
            registrationDataService.create(context, "0000-0000-0000-0000", RegistrationTypeEnum.VALIDATION_ORCID);
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenAdmin).perform(
            post("/api/eperson/epersons/" + customEPerson.getID())
                .param("token", validationRegistration.getToken())
        ).andExpect(status().isForbidden());

    }

    @Test
    public void givenValidationRegistration_whenPostWithoutOverride_thenCreated() throws Exception {

        Email spy = Mockito.spy(Email.class);
        doNothing().when(spy).send();

        emailMockedStatic.when(() -> Email.getEmail(any())).thenReturn(spy);

        context.turnOffAuthorisationSystem();
        RegistrationDataChanges changes =
            new RegistrationDataChanges("vincenzo.mecca@4science.com", RegistrationTypeEnum.VALIDATION_ORCID);
        RegistrationData validationRegistration =
            this.accountService.renewRegistrationForEmail(
                context, new RegistrationDataPatch(orcidRegistration, changes)
            );
        context.restoreAuthSystemState();

        String customToken = getAuthToken(customEPerson.getEmail(), customPassword);

        getClient(customToken).perform(
            post("/api/eperson/epersons/" + customEPerson.getID())
                .param("token", validationRegistration.getToken())
        ).andExpect(status().isCreated());

    }

    @Test
    public void givenValidationRegistration_whenPostWithOverride_thenCreated() throws Exception {

        Email spy = Mockito.spy(Email.class);
        doNothing().when(spy).send();

        emailMockedStatic.when(() -> Email.getEmail(any())).thenReturn(spy);

        context.turnOffAuthorisationSystem();
        RegistrationDataChanges changes =
            new RegistrationDataChanges("vincenzo.mecca@4science.com", RegistrationTypeEnum.VALIDATION_ORCID);
        RegistrationData validationRegistration =
            this.accountService.renewRegistrationForEmail(
                context, new RegistrationDataPatch(orcidRegistration, changes)
            );
        context.restoreAuthSystemState();

        String customToken = getAuthToken(customEPerson.getEmail(), customPassword);

        getClient(customToken).perform(
            post("/api/eperson/epersons/" + customEPerson.getID())
                .param("token", validationRegistration.getToken())
                .param("override", "eperson.firstname,eperson.lastname")
        ).andExpect(status().isCreated());

    }

    @Test
    public void givenValidationRegistration_whenPostWithoutOverride_thenOnlyNewMetadataAdded() throws Exception {

        Email spy = Mockito.spy(Email.class);
        doNothing().when(spy).send();

        emailMockedStatic.when(() -> Email.getEmail(any())).thenReturn(spy);

        context.turnOffAuthorisationSystem();
        RegistrationDataChanges changes =
            new RegistrationDataChanges("vincenzo.mecca@4science.com", RegistrationTypeEnum.VALIDATION_ORCID);
        RegistrationData validationRegistration =
            this.accountService.renewRegistrationForEmail(
                context, new RegistrationDataPatch(orcidRegistration, changes)
            );
        context.restoreAuthSystemState();

        String customToken = getAuthToken(customEPerson.getEmail(), customPassword);

        getClient(customToken).perform(
            post("/api/eperson/epersons/" + customEPerson.getID())
                .param("token", validationRegistration.getToken())
        ).andExpect(status().isCreated())
        .andExpect(
            jsonPath("$.netid", equalTo("0000-0000-0000-0000"))
        )
        .andExpect(
            jsonPath("$.metadata",
                 Matchers.allOf(
                     MetadataMatcher.matchMetadata("eperson.firstname", "Vins"),
                     MetadataMatcher.matchMetadata("eperson.lastname", "4Science"),
                     MetadataMatcher.matchMetadata("eperson.orcid", "0000-0000-0000-0000")
                 )
            )
        );

    }

    @Test
    public void givenValidationRegistration_whenPostWithOverride_thenMetadataReplaced() throws Exception {

        Email spy = Mockito.spy(Email.class);
        doNothing().when(spy).send();

        emailMockedStatic.when(() -> Email.getEmail(any())).thenReturn(spy);

        context.turnOffAuthorisationSystem();
        RegistrationDataChanges changes =
            new RegistrationDataChanges("vincenzo.mecca@4science.com", RegistrationTypeEnum.VALIDATION_ORCID);
        RegistrationData validationRegistration =
            this.accountService.renewRegistrationForEmail(
                context, new RegistrationDataPatch(orcidRegistration, changes)
            );
        context.restoreAuthSystemState();

        String customToken = getAuthToken(customEPerson.getEmail(), customPassword);

        getClient(customToken).perform(
              post("/api/eperson/epersons/" + customEPerson.getID())
                  .param("token", validationRegistration.getToken())
                  .param("override", "eperson.firstname,eperson.lastname")
          ).andExpect(status().isCreated())
          .andExpect(
              jsonPath("$.netid", equalTo("0000-0000-0000-0000"))
          )
          .andExpect(
              jsonPath("$.metadata",
                       Matchers.allOf(
                           MetadataMatcher.matchMetadata("eperson.firstname", "Vincenzo"),
                           MetadataMatcher.matchMetadata("eperson.lastname", "Mecca"),
                           MetadataMatcher.matchMetadata("eperson.orcid", "0000-0000-0000-0000")
                       )
              )
          );

    }

    @Test
    public void givenValidationRegistration_whenPostWithOverrideAndMetadataNotFound_thenBadRequest() throws Exception {

        Email spy = Mockito.spy(Email.class);
        doNothing().when(spy).send();

        emailMockedStatic.when(() -> Email.getEmail(any())).thenReturn(spy);

        context.turnOffAuthorisationSystem();
        RegistrationDataChanges changes =
            new RegistrationDataChanges("vincenzo.mecca@4science.com", RegistrationTypeEnum.VALIDATION_ORCID);
        RegistrationData validationRegistration =
            this.accountService.renewRegistrationForEmail(
                context, new RegistrationDataPatch(orcidRegistration, changes)
            );
        context.restoreAuthSystemState();

        String customToken = getAuthToken(customEPerson.getEmail(), customPassword);

        getClient(customToken).perform(
                                  post("/api/eperson/epersons/" + customEPerson.getID())
                                      .param("token", validationRegistration.getToken())
                                      .param("override", "eperson.phone")
                              ).andExpect(status().isBadRequest());

        context.turnOffAuthorisationSystem();
        MetadataField phoneMf =
            metadataFieldService.findByElement(context, "eperson", "phone", null);

        registrationDataService.addMetadata(
            context, validationRegistration, phoneMf, "1234567890"
        );
        context.restoreAuthSystemState();

        getClient(customToken).perform(
            post("/api/eperson/epersons/" + customEPerson.getID())
                .param("token", validationRegistration.getToken())
                .param("override", "eperson.phone")
        ).andExpect(status().isBadRequest());

    }

}
