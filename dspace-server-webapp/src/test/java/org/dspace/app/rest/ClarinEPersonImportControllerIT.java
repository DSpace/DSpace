/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.model.ClarinUserRegistrationRest;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.ClarinUserRegistrationBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.content.clarin.ClarinUserRegistration;
import org.dspace.content.service.clarin.ClarinUserRegistrationService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test to test the /api/clarin/import/* endpoints
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
public class ClarinEPersonImportControllerIT  extends AbstractControllerIntegrationTest {

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private ClarinUserRegistrationService clarinUserRegistrationService;

    @Test
    public void createEpersonTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        EPersonRest data = new EPersonRest();
        MetadataRest metadataRest = new MetadataRest();
        data.setEmail("createtest@example.com");
        data.setCanLogIn(true);
        MetadataValueRest surname = new MetadataValueRest();
        surname.setValue("Doe");
        metadataRest.put("eperson.lastname", surname);
        MetadataValueRest firstname = new MetadataValueRest();
        firstname.setValue("John");
        metadataRest.put("eperson.firstname", firstname);
        data.setMetadata(metadataRest);

        AtomicReference<UUID> idRef = new AtomicReference<UUID>();

        String authToken = getAuthToken(admin.getEmail(), password);

        try {
            getClient(authToken).perform(post("/api/clarin/import/eperson")
                            .content(mapper.writeValueAsBytes(data))
                            .contentType(contentType)
                            .param("projection", "full")
                            .param("selfRegistered", "true")
                            .param("lastActive", "2018-02-10T13:21:29.733"))
                    .andExpect(status().isOk())
                    .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id"))));

            EPerson createdEperson = ePersonService.find(context, idRef.get());

            assertEquals(getStringFromDate(createdEperson.getLastActive()), "2018-02-10T13:21:29.733");
            assertTrue(createdEperson.getSelfRegistered());
            assertEquals(createdEperson.getEmail(),"createtest@example.com");
            assertTrue(createdEperson.canLogIn());
            assertFalse(createdEperson.getRequireCertificate());
            assertEquals(createdEperson.getFirstName(), "John");
            assertEquals(createdEperson.getLastName(), "Doe");

        } finally {
            EPersonBuilder.deleteEPerson(idRef.get());
        }
    }

    @Test
    public void createEpersonDifferentLastActiveFormatTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        EPersonRest data = new EPersonRest();
        MetadataRest metadataRest = new MetadataRest();
        data.setEmail("createtest@example.com");
        data.setCanLogIn(true);
        MetadataValueRest surname = new MetadataValueRest();
        surname.setValue("Doe");
        metadataRest.put("eperson.lastname", surname);
        MetadataValueRest firstname = new MetadataValueRest();
        firstname.setValue("John");
        metadataRest.put("eperson.firstname", firstname);
        data.setMetadata(metadataRest);

        AtomicReference<UUID> idRef = new AtomicReference<UUID>();

        String authToken = getAuthToken(admin.getEmail(), password);

        try {
            getClient(authToken).perform(post("/api/clarin/import/eperson")
                            .content(mapper.writeValueAsBytes(data))
                            .contentType(contentType)
                            .param("projection", "full")
                            .param("selfRegistered", "true")
                            .param("lastActive", "2018-02-10T13:21:29.733"))
                    .andExpect(status().isOk())
                    .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id"))));

            EPerson createdEperson = ePersonService.find(context, idRef.get());

            assertEquals(getStringFromDate(createdEperson.getLastActive()), "2018-02-10T13:21:29.733");
            assertTrue(createdEperson.getSelfRegistered());
            assertEquals(createdEperson.getEmail(),"createtest@example.com");
            assertTrue(createdEperson.canLogIn());
            assertFalse(createdEperson.getRequireCertificate());
            assertEquals(createdEperson.getFirstName(), "John");
            assertEquals(createdEperson.getLastName(), "Doe");

        } finally {
            EPersonBuilder.deleteEPerson(idRef.get());
        }
    }

    @Test
    public void createUserRegistrationTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        context.turnOffAuthorisationSystem();
        EPerson ePerson = EPersonBuilder.createEPerson(context)
                .withEmail("eperson3@mail.com")
                .withPassword("qwerty03")
                .build();
        context.restoreAuthSystemState();
        ClarinUserRegistrationRest userRegistrationRest = new ClarinUserRegistrationRest();
        userRegistrationRest.setConfirmation(true);
        userRegistrationRest.setEmail("test@test.edu");
        userRegistrationRest.setePersonID(ePerson.getID());
        userRegistrationRest.setOrganization("Test");

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();

        String authToken = getAuthToken(admin.getEmail(), password);

        try {
            getClient(authToken).perform(post("/api/clarin/import/userregistration")
                            .content(mapper.writeValueAsBytes(userRegistrationRest))
                            .contentType(contentType))
                    .andExpect(status().isOk())
                    .andDo(result -> idRef
                            .set(read(result.getResponse().getContentAsString(), "$.id")));
            //control
            ClarinUserRegistration clarinUserRegistration = clarinUserRegistrationService.find(context, idRef.get());
            assertTrue(clarinUserRegistration.isConfirmation());
            assertEquals(clarinUserRegistration.getEmail(), "test@test.edu");
            assertEquals(clarinUserRegistration.getPersonID(), ePerson.getID());
            assertEquals(clarinUserRegistration.getOrganization(), "Test");
        } finally {
            ClarinUserRegistrationBuilder.deleteClarinUserRegistration(idRef.get());
        }
    }

    private String getStringFromDate(Date value) throws ParseException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        return df.format(value);
    }
}
