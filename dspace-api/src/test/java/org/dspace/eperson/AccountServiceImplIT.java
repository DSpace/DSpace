/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.MetadataFieldBuilder;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.AccountService;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.RegistrationDataService;
import org.junit.Before;
import org.junit.Test;

public class AccountServiceImplIT extends AbstractIntegrationTestWithDatabase {

    public static final String ORCID_NETID = "vins01";
    public static final String ORCID_EMAIL = "vins-01@fake.mail";
    public static final String CUSTOM_METADATA_VALUE = "vins01-customID";

    AccountService accountService =
        EPersonServiceFactory.getInstance().getAccountService();

    EPersonService ePersonService =
        EPersonServiceFactory.getInstance().getEPersonService();

    RegistrationDataService registrationDataService =
        EPersonServiceFactory.getInstance().getRegistrationDataService();
    ;

    EPerson tokenPerson;
    RegistrationData orcidToken;
    MetadataField metadataField;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        tokenPerson =
            EPersonBuilder.createEPerson(context)
                          .withNameInMetadata("Vincenzo", "Mecca")
                          .withEmail(null)
                          .withNetId(null)
                          .withCanLogin(true)
                          .build();

        metadataField =
            MetadataFieldBuilder.createMetadataField(context, "identifier", "custom", null)
                                .build();

        orcidToken =
            registrationDataService.create(context, ORCID_NETID, RegistrationTypeEnum.ORCID);
        orcidToken.setEmail(ORCID_EMAIL);

        registrationDataService.addMetadata(context, orcidToken, metadataField, CUSTOM_METADATA_VALUE);
        registrationDataService.update(context, orcidToken);

        context.restoreAuthSystemState();
    }


    @Test
    public void testMergedORCIDRegistration() throws SQLException, AuthorizeException {

        // set current logged-in eperson
        context.setCurrentUser(tokenPerson);

        // try to update account details with the ORCID token
        EPerson updatedEperson =
            accountService.mergeRegistration(
                context, tokenPerson.getID(), orcidToken.getToken(),
                List.of()
            );

        // updates value with the one inside the ORCID token
        assertThat(updatedEperson, notNullValue());
        assertThat(updatedEperson.getEmail(), is(ORCID_EMAIL));
        assertThat(updatedEperson.getNetid(), is(ORCID_NETID));

        String customMetadataFound =
            ePersonService.getMetadataFirstValue(
                updatedEperson, metadataField.getMetadataSchema().getName(), metadataField.getElement(),
                metadataField.getQualifier(), Item.ANY
            );

        // updates the metadata with the one set in the ORCID token
        assertThat(customMetadataFound, is(CUSTOM_METADATA_VALUE));
        // deletes the token
        assertThat(registrationDataService.findByToken(context, orcidToken.getToken()), nullValue());
    }

    @Test
    public void testMergedORCIDRegistrationWithOverwrittenMetadata() throws SQLException, AuthorizeException {

        // set current logged-in eperson
        context.setCurrentUser(tokenPerson);

        registrationDataService.addMetadata(
            context, orcidToken, "eperson", "firstname", null, "Vins"
        );
        registrationDataService.addMetadata(
            context, orcidToken, "eperson", "lastname", null, "4Science"
        );
        registrationDataService.update(context, orcidToken);

        // try to update account details with the ORCID token
        EPerson updatedEperson =
            accountService.mergeRegistration(context, tokenPerson.getID(), orcidToken.getToken(),
                                             List.of("eperson.firstname", "eperson.lastname"));

        // updates value with the one inside the ORCID token
        assertThat(updatedEperson, notNullValue());
        assertThat(updatedEperson.getEmail(), is(ORCID_EMAIL));
        assertThat(updatedEperson.getNetid(), is(ORCID_NETID));
        // overwrites values with the one from the token
        assertThat(updatedEperson.getFirstName(), is("Vins"));
        assertThat(updatedEperson.getLastName(), is("4Science"));

        String customMetadataFound =
            ePersonService.getMetadataFirstValue(
                updatedEperson, metadataField.getMetadataSchema().getName(), metadataField.getElement(),
                metadataField.getQualifier(), Item.ANY
            );

        // updates the metadata with the one set in the ORCID token
        assertThat(customMetadataFound, is(CUSTOM_METADATA_VALUE));
        // deletes the token
        assertThat(registrationDataService.findByToken(context, orcidToken.getToken()), nullValue());
    }


    @Test
    public void testCannotMergedORCIDRegistrationWithDifferentLoggedEperson() {

        // set current logged-in admin
        context.setCurrentUser(admin);

        // try to update eperson details with the ORCID token while logged in as admin
        assertThrows(
            AuthorizeException.class,
            () -> accountService.mergeRegistration(context, tokenPerson.getID(), orcidToken.getToken(), List.of())
        );
    }

    @Test
    public void testCreateUserWithRegistration() throws SQLException, AuthorizeException, IOException {

        // set current logged-in eperson
        context.setCurrentUser(null);

        context.turnOffAuthorisationSystem();
        // create an orcid validation token
        RegistrationData orcidRegistration =
            registrationDataService.create(context, ORCID_NETID, RegistrationTypeEnum.VALIDATION_ORCID);
        registrationDataService.addMetadata(
            context, orcidRegistration, "eperson", "firstname", null, "Vincenzo"
        );
        registrationDataService.addMetadata(
            context, orcidRegistration, "eperson", "lastname", null, "Mecca"
        );
        orcidRegistration.setEmail(ORCID_EMAIL);
        registrationDataService.update(context, orcidRegistration);

        context.commit();
        context.restoreAuthSystemState();

        EPerson createdEPerson = null;
        try {

            // try to create a new account during orcid registration
            createdEPerson =
                accountService.mergeRegistration(context, null, orcidRegistration.getToken(), List.of());

            // updates value with the one inside the validation token
            assertThat(createdEPerson, notNullValue());
            assertThat(createdEPerson.getFirstName(), is("Vincenzo"));
            assertThat(createdEPerson.getLastName(), is("Mecca"));
            assertThat(createdEPerson.getEmail(), is(ORCID_EMAIL));
            assertThat(createdEPerson.getNetid(), is(ORCID_NETID));

            // deletes the token
            assertThat(registrationDataService.findByToken(context, orcidRegistration.getToken()), nullValue());
        } finally {
            context.turnOffAuthorisationSystem();
            ePersonService.delete(context, context.reloadEntity(createdEPerson));
            RegistrationData found = context.reloadEntity(orcidRegistration);
            if (found != null) {
                registrationDataService.delete(context, found);
            }
            context.restoreAuthSystemState();
        }

    }


    @Test
    public void testInvalidMergeWithoutValidToken() throws SQLException, AuthorizeException {

        // create a register token
        RegistrationData anyToken =
            registrationDataService.create(context, ORCID_NETID, RegistrationTypeEnum.REGISTER);

        try {

            assertThrows(
                AuthorizeException.class,
                () -> accountService.mergeRegistration(context, null, anyToken.getToken(), List.of())
            );

            // sets as forgot token
            anyToken.setRegistrationType(RegistrationTypeEnum.FORGOT);
            registrationDataService.update(context, anyToken);

            assertThrows(
                AuthorizeException.class,
                () -> accountService.mergeRegistration(context, null, anyToken.getToken(), List.of())
            );

            // sets as change_password token
            anyToken.setRegistrationType(RegistrationTypeEnum.CHANGE_PASSWORD);
            registrationDataService.update(context, anyToken);

            assertThrows(
                AuthorizeException.class,
                () -> accountService.mergeRegistration(context, null, anyToken.getToken(), List.of())
            );

            // sets as invitation token
            anyToken.setRegistrationType(RegistrationTypeEnum.INVITATION);
            registrationDataService.update(context, anyToken);

            assertThrows(
                AuthorizeException.class,
                () -> accountService.mergeRegistration(context, null, anyToken.getToken(), List.of())
            );

        } finally {
            registrationDataService.delete(context, context.reloadEntity(anyToken));
        }

    }

}