/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;
import static com.jayway.jsonpath.JsonPath.read;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.jackson.IgnoreJacksonWriteOnlyAccess;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.RegistrationRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.RegistrationDataService;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
* @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
*/
public class EPersonRestRepositoryInviationIT extends AbstractControllerIntegrationTest {

    @Autowired
    private GroupService groupService;

    @Autowired
    private RegistrationDataService registrationDataService;

    @Autowired
    private EPersonService ePersonService;

    @Test
    public void adminInvitedEPersonToGroupsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group group = GroupBuilder.createGroup(context)
                                  .withName("Test group 1").build();

        Group group2 = GroupBuilder.createGroup(context)
                                   .withName("Test group 2").build();
        context.restoreAuthSystemState();
        ObjectMapper mapper = new ObjectMapper();

        String newRegisterEmail = "new-register@fake-email.com";

        List<UUID> groups = new ArrayList<UUID>();
        groups.add(group.getID());
        groups.add(group2.getID());

        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(newRegisterEmail);
        registrationRest.setGroups(groups);

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(post("/api/eperson/registrations")
                             .contentType(MediaType.APPLICATION_JSON)
                             .content(mapper.writeValueAsBytes(registrationRest)))
                             .andExpect(status().isCreated());

        String newRegisterToken = registrationDataService.findByEmail(context, newRegisterEmail).getToken();

        EPersonRest ePersonRest = new EPersonRest();
        MetadataRest metadataRest = new MetadataRest();
        ePersonRest.setEmail(newRegisterEmail);
        ePersonRest.setCanLogIn(true);
        MetadataValueRest surname = new MetadataValueRest();
        surname.setValue("Doe");
        metadataRest.put("eperson.lastname", surname);
        MetadataValueRest firstname = new MetadataValueRest();
        firstname.setValue("John");
        metadataRest.put("eperson.firstname", firstname);
        ePersonRest.setMetadata(metadataRest);
        ePersonRest.setPassword("somePassword");
        AtomicReference<UUID> idRef = new AtomicReference<UUID>();

        mapper.setAnnotationIntrospector(new IgnoreJacksonWriteOnlyAccess());
        try {
            getClient().perform(post("/api/eperson/epersons")
                       .param("token", newRegisterToken)
                       .content(mapper.writeValueAsBytes(ePersonRest))
                       .contentType(MediaType.APPLICATION_JSON))
                       .andExpect(status().isCreated())
                       .andExpect(jsonPath("$", Matchers.allOf(
                               hasJsonPath("$.uuid", not(empty())),
                               hasJsonPath("$.email", is(newRegisterEmail)),
                               hasJsonPath("$.type", is("eperson")),
                               hasJsonPath("$._links.self.href", not(empty())),
                               hasJsonPath("$.metadata", Matchers.allOf(
                                           matchMetadata("eperson.firstname", "John"),
                                           matchMetadata("eperson.lastname", "Doe")
                                           ))))).andDo(result -> idRef
                       .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id"))));

            String epersonUuid = String.valueOf(idRef.get());
            EPerson createdEPerson = ePersonService.find(context, UUID.fromString(epersonUuid));
            group = context.reloadEntity(group);
            group2 = context.reloadEntity(group2);
            assertTrue(groupService.isMember(context, createdEPerson, group));
            assertTrue(groupService.isMember(context, createdEPerson, group2));
            assertTrue(ePersonService.checkPassword(context, createdEPerson, "somePassword"));
            assertNull(registrationDataService.findByToken(context, newRegisterToken));

        } finally {
            context.turnOffAuthorisationSystem();
            registrationDataService.deleteByToken(context, newRegisterToken);
            context.restoreAuthSystemState();
            EPersonBuilder.deleteEPerson(idRef.get());
        }
    }

    @Test
    public void simpleUserInvitedEPersonToGroupsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group group = GroupBuilder.createGroup(context)
                                  .withName("Test group 1").build();

        Group group2 = GroupBuilder.createGroup(context)
                                   .withName("Test group 2").build();
        context.restoreAuthSystemState();
        ObjectMapper mapper = new ObjectMapper();

        String newRegisterEmail = "new-register@fake-email.com";

        List<UUID> groups = new ArrayList<UUID>();
        groups.add(group.getID());
        groups.add(group2.getID());

        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(newRegisterEmail);
        registrationRest.setGroups(groups);

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(post("/api/eperson/registrations")
                               .contentType(MediaType.APPLICATION_JSON)
                               .content(mapper.writeValueAsBytes(registrationRest)))
                               .andExpect(status().isForbidden());

    }

    @Test
    public void anonymousUserInvitedEPersonToGroupsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group group = GroupBuilder.createGroup(context)
                                  .withName("Test group 1").build();

        Group group2 = GroupBuilder.createGroup(context)
                                   .withName("Test group 2").build();
        context.restoreAuthSystemState();
        ObjectMapper mapper = new ObjectMapper();

        String newRegisterEmail = "new-register@fake-email.com";

        List<UUID> groups = new ArrayList<UUID>();
        groups.add(group.getID());
        groups.add(group2.getID());

        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(newRegisterEmail);
        registrationRest.setGroups(groups);

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(post("/api/eperson/registrations")
                               .contentType(MediaType.APPLICATION_JSON)
                               .content(mapper.writeValueAsBytes(registrationRest)))
                               .andExpect(status().isForbidden());

    }

    @Test
    public void adminInvitedEPersonToGroupNotExistTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group group = GroupBuilder.createGroup(context)
                                  .withName("Test group 1").build();

        context.restoreAuthSystemState();
        ObjectMapper mapper = new ObjectMapper();

        String newRegisterEmail = "new-register@fake-email.com";

        List<UUID> groups = new ArrayList<UUID>();
        groups.add(group.getID());
        groups.add(UUID.randomUUID());

        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(newRegisterEmail);
        registrationRest.setGroups(groups);

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(post("/api/eperson/registrations")
                               .contentType(MediaType.APPLICATION_JSON)
                               .content(mapper.writeValueAsBytes(registrationRest)))
                               .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void removeOneGroupTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group group = GroupBuilder.createGroup(context)
                                  .withName("Test group 1").build();

        Group group2 = GroupBuilder.createGroup(context)
                                   .withName("Test group 2").build();
        context.restoreAuthSystemState();
        ObjectMapper mapper = new ObjectMapper();

        String newRegisterEmail = "new-register@fake-email.com";

        List<UUID> groups = new ArrayList<UUID>();
        groups.add(group.getID());
        groups.add(group2.getID());

        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(newRegisterEmail);
        registrationRest.setGroups(groups);

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(post("/api/eperson/registrations")
                             .contentType(MediaType.APPLICATION_JSON)
                             .content(mapper.writeValueAsBytes(registrationRest)))
                             .andExpect(status().isCreated());

        String newRegisterToken = registrationDataService.findByEmail(context, newRegisterEmail).getToken();

        groupService.delete(context, group);

        EPersonRest ePersonRest = new EPersonRest();
        MetadataRest metadataRest = new MetadataRest();
        ePersonRest.setEmail(newRegisterEmail);
        ePersonRest.setCanLogIn(true);
        MetadataValueRest surname = new MetadataValueRest();
        surname.setValue("Doe");
        metadataRest.put("eperson.lastname", surname);
        MetadataValueRest firstname = new MetadataValueRest();
        firstname.setValue("John");
        metadataRest.put("eperson.firstname", firstname);
        ePersonRest.setMetadata(metadataRest);
        ePersonRest.setPassword("somePassword");
        AtomicReference<UUID> idRef = new AtomicReference<UUID>();

        mapper.setAnnotationIntrospector(new IgnoreJacksonWriteOnlyAccess());
        try {
            getClient().perform(post("/api/eperson/epersons")
                       .param("token", newRegisterToken)
                       .content(mapper.writeValueAsBytes(ePersonRest))
                       .contentType(MediaType.APPLICATION_JSON))
                       .andExpect(status().isCreated())
                       .andExpect(jsonPath("$", Matchers.allOf(
                               hasJsonPath("$.uuid", not(empty())),
                               hasJsonPath("$.email", is(newRegisterEmail)),
                               hasJsonPath("$.type", is("eperson")),
                               hasJsonPath("$._links.self.href", not(empty())),
                               hasJsonPath("$.metadata", Matchers.allOf(
                                           matchMetadata("eperson.firstname", "John"),
                                           matchMetadata("eperson.lastname", "Doe")
                                           ))))).andDo(result -> idRef
                       .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id"))));

            String epersonUuid = String.valueOf(idRef.get());
            EPerson createdEPerson = ePersonService.find(context, UUID.fromString(epersonUuid));
            group = context.reloadEntity(group);
            group2 = context.reloadEntity(group2);
            assertFalse(groupService.isMember(context, createdEPerson, group));
            assertTrue(groupService.isMember(context, createdEPerson, group2));
            assertTrue(ePersonService.checkPassword(context, createdEPerson, "somePassword"));
            assertNull(registrationDataService.findByToken(context, newRegisterToken));

        } finally {
            context.turnOffAuthorisationSystem();
            registrationDataService.deleteByToken(context, newRegisterToken);
            context.restoreAuthSystemState();
            EPersonBuilder.deleteEPerson(idRef.get());
        }
    }

}