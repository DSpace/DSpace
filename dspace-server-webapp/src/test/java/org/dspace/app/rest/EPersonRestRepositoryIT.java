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
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadataDoesNotExist;
import static org.dspace.app.rest.repository.RegistrationRestRepository.TYPE_QUERY_PARAM;
import static org.dspace.app.rest.repository.RegistrationRestRepository.TYPE_REGISTER;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.exception.EPersonNameNotProvidedException;
import org.dspace.app.rest.exception.RESTEmptyWorkflowGroupException;
import org.dspace.app.rest.jackson.IgnoreJacksonWriteOnlyAccess;
import org.dspace.app.rest.matcher.EPersonMatcher;
import org.dspace.app.rest.matcher.GroupMatcher;
import org.dspace.app.rest.matcher.HalMatcher;
import org.dspace.app.rest.matcher.MetadataMatcher;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.RegistrationRest;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.test.MetadataPatchSuite;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.PasswordHash;
import org.dspace.eperson.service.AccountService;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.RegistrationDataService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class EPersonRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private RegistrationDataService registrationDataService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void createTest() throws Exception {
        // we should check how to get it from Spring
        ObjectMapper mapper = new ObjectMapper();
        EPersonRest data = new EPersonRest();
        EPersonRest dataFull = new EPersonRest();
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
        dataFull.setEmail("createtestFull@example.com");
        dataFull.setCanLogIn(true);
        dataFull.setMetadata(metadataRest);

        AtomicReference<UUID> idRef = new AtomicReference<UUID>();
        AtomicReference<UUID> idRefNoEmbeds = new AtomicReference<UUID>();

        String authToken = getAuthToken(admin.getEmail(), password);

        try {
        getClient(authToken).perform(post("/api/eperson/epersons")
                                        .content(mapper.writeValueAsBytes(data))
                                        .contentType(contentType)
                            .param("projection", "full"))
                            .andExpect(status().isCreated())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$", EPersonMatcher.matchFullEmbeds()))
                            .andExpect(jsonPath("$", Matchers.allOf(
                               hasJsonPath("$.uuid", not(empty())),
                               // is it what you expect? EPerson.getName() returns the email...
                               //hasJsonPath("$.name", is("Doe John")),
                               hasJsonPath("$.email", is("createtest@example.com")),
                               hasJsonPath("$.type", is("eperson")),
                               hasJsonPath("$.canLogIn", is(true)),
                               hasJsonPath("$.requireCertificate", is(false)),
                               hasJsonPath("$._links.self.href", not(empty())),
                               hasJsonPath("$.metadata", Matchers.allOf(
                                       matchMetadata("eperson.firstname", "John"),
                                       matchMetadata("eperson.lastname", "Doe"),
                                       matchMetadataDoesNotExist("dc.identifier.uri")
                               )))))
                            .andDo(result -> idRef
                                    .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id"))));

        getClient(authToken).perform(post("/api/eperson/epersons")
                .content(mapper.writeValueAsBytes(dataFull))
                .contentType(contentType))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
                .andDo(result -> idRefNoEmbeds
                        .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id"))));;

        } finally {
            EPersonBuilder.deleteEPerson(idRef.get());
            EPersonBuilder.deleteEPerson(idRefNoEmbeds.get());
        }
    }

    @Test
    public void createAnonAccessDeniedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // we should check how to get it from Spring
        ObjectMapper mapper = new ObjectMapper();
        EPersonRest data = new EPersonRest();
        EPersonRest dataFull = new EPersonRest();
        MetadataRest metadataRest = new MetadataRest();
        data.setEmail("createtest@fake-email.com");
        data.setCanLogIn(true);
        MetadataValueRest surname = new MetadataValueRest();
        surname.setValue("Doe");
        metadataRest.put("eperson.lastname", surname);
        MetadataValueRest firstname = new MetadataValueRest();
        firstname.setValue("John");
        metadataRest.put("eperson.firstname", firstname);
        data.setMetadata(metadataRest);
        dataFull.setEmail("createtestFull@fake-email.com");
        dataFull.setCanLogIn(true);
        dataFull.setMetadata(metadataRest);

        context.restoreAuthSystemState();

        getClient().perform(post("/api/eperson/epersons")
                                         .content(mapper.writeValueAsBytes(data))
                                         .contentType(contentType)
                                         .param("projection", "full"))
                            .andExpect(status().isUnauthorized());
        getClient().perform(get("/api/eperson/epersons/search/byEmail")
                                .param("email", data.getEmail()))
                   .andExpect(status().isNoContent());
    }

    @Test
    public void testCreateWithInvalidPassword() throws Exception {

        context.turnOffAuthorisationSystem();
        accountService.sendRegistrationInfo(context, "test@fake-email.com");
        String token = registrationDataService.findByEmail(context, "test@fake-email.com").getToken();
        context.restoreAuthSystemState();

        String ePersonData = "{" +
            "   \"metadata\":{" +
            "      \"eperson.firstname\":[{\"value\":\"John\"}]," +
            "      \"eperson.lastname\":[{\"value\":\"Doe\"}]" +
            "   }," +
            "   \"email\":\"test@fake-email.com\"," +
            "   \"password\":\"1234\"," +
            "   \"type\":\"eperson\"" +
            "}";

        try {

            getClient().perform(post("/api/eperson/epersons")
                .content(ePersonData)
                .contentType(contentType)
                .param("token", token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(status().reason(is("New password is invalid. "
                    + "Valid passwords must be at least 8 characters long!")));
        } finally {
            context.turnOffAuthorisationSystem();
            registrationDataService.deleteByToken(context, token);
            context.restoreAuthSystemState();
        }

    }

    @Test
    public void findAllTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson newUser = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/eperson"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.epersons", Matchers.containsInAnyOrder(
                       EPersonMatcher.matchEPersonEntry(newUser),
                       EPersonMatcher.matchEPersonOnEmail(admin.getEmail()),
                       EPersonMatcher.matchEPersonOnEmail(eperson.getEmail())
                   )))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(3)))
        ;

        getClient().perform(get("/api/eperson/epersons"))
                   .andExpect(status().isUnauthorized())
        ;
    }

    @Test
    public void findAllUnauthorizedTest() throws Exception {
        // Access endpoint without being authenticated
        getClient().perform(get("/api/eperson/eperson"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findAllForbiddenTest() throws Exception {
        String authToken = getAuthToken(eperson.getEmail(), password);
        // Access endpoint logged in as an unprivileged user
        getClient(authToken).perform(get("/api/eperson/eperson"))
                            .andExpect(status().isForbidden());
    }

    @Test
    public void findAllPaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson testEPerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        // NOTE: /eperson/epersons endpoint returns users sorted by email
        // using size = 2 the first page will contain our new test user and default 'admin' ONLY
        getClient(authToken).perform(get("/api/eperson/epersons")
                                .param("size", "2"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.epersons", Matchers.containsInAnyOrder(
                           EPersonMatcher.matchEPersonEntry(testEPerson),
                           EPersonMatcher.matchEPersonOnEmail(admin.getEmail())
                   )))
                   .andExpect(jsonPath("$._embedded.epersons", Matchers.not(
                       Matchers.contains(
                           EPersonMatcher.matchEPersonOnEmail(eperson.getEmail())
                       )
                   )))
                   .andExpect(jsonPath("$.page.size", is(2)))
                   .andExpect(jsonPath("$.page.totalElements", is(3)))
        ;

        // using size = 2 the *second* page will contains our default 'eperson' ONLY
        getClient(authToken).perform(get("/api/eperson/epersons")
                                .param("size", "2")
                                .param("page", "1"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.epersons", Matchers.contains(
                       EPersonMatcher.matchEPersonOnEmail(eperson.getEmail())
                   )))
                   .andExpect(jsonPath("$._embedded.epersons", Matchers.hasSize(1)))
                   .andExpect(jsonPath("$.page.size", is(2)))
                   .andExpect(jsonPath("$.page.totalElements", is(3)))
        ;

        getClient().perform(get("/api/eperson/epersons"))
                   .andExpect(status().isUnauthorized())
        ;
    }


    @Test
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .build();

        EPerson ePerson2 = EPersonBuilder.createEPerson(context)
                                         .withNameInMetadata("Jane", "Smith")
                                         .withEmail("janesmith@example.com")
                                         .build();

        context.restoreAuthSystemState();

        // When full projection is requested, response should include expected properties, links, and embeds.
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/" + ePerson2.getID()).param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", EPersonMatcher.matchFullEmbeds()))
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", is(
                       EPersonMatcher.matchEPersonEntry(ePerson2)
                   )))
                   .andExpect(jsonPath("$", Matchers.not(
                       is(
                           EPersonMatcher.matchEPersonEntry(ePerson)
                       )
                   )))
        ;

        // When no projection is requested, response should include expected properties, links, and no embeds.
        getClient(authToken).perform(get("/api/eperson/epersons/" + ePerson2.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
        ;

    }

    @Test
    public void findOneForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Mik", "Reck")
                .withEmail("MikReck@email.com")
                .withPassword("qwerty01")
                .build();

        EPerson ePerson2 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Bob", "Smith")
                .withEmail("bobsmith@example.com")
                .build();

        context.restoreAuthSystemState();

        String tokenEperson1 = getAuthToken(ePerson1.getEmail(), "qwerty01");
        getClient(tokenEperson1).perform(get("/api/eperson/epersons/" + ePerson2.getID()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void readEpersonAuthorizationTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson1 = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .build();

        EPerson ePerson2 = EPersonBuilder.createEPerson(context)
                                         .withNameInMetadata("Jane", "Smith")
                                         .withEmail("janesmith@example.com")
                                         .build();

        context.restoreAuthSystemState();

        // Verify admin can access information about any user (and only one user is included in response)
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/" + ePerson2.getID()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", is(
                       EPersonMatcher.matchEPersonEntry(ePerson2)
                   )))
                   .andExpect(jsonPath("$", Matchers.not(
                       is(
                           EPersonMatcher.matchEPersonEntry(ePerson1)
                       )
                   )))
                   .andExpect(jsonPath("$._links.self.href",
                                       Matchers.containsString("/api/eperson/epersons/" + ePerson2.getID())));


        // Verify an unprivileged user cannot access information about a *different* user
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(get("/api/eperson/epersons/" + ePerson2.getID()))
                               .andExpect(status().isForbidden());

        // Verify an unprivileged user can access their own information
        getClient(epersonToken).perform(get("/api/eperson/epersons/" + eperson.getID()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", is(
                        EPersonMatcher.matchEPersonOnEmail(eperson.getEmail())
                )))
                .andExpect(jsonPath("$._links.self.href",
                                    Matchers.containsString("/api/eperson/epersons/" + eperson.getID())));

    }

    @Test
    public void findOneTestWrongUUID() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson testEPerson1 = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .build();

        EPerson testEPerson2 = EPersonBuilder.createEPerson(context)
                                         .withNameInMetadata("Jane", "Smith")
                                         .withEmail("janesmith@example.com")
                                         .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/" + UUID.randomUUID()))
                   .andExpect(status().isNotFound());

    }

    @Test
    public void searchMethodsExist() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons"))
                .andExpect(jsonPath("$._links.search.href", Matchers.notNullValue()));

        getClient(authToken).perform(get("/api/eperson/epersons/search"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(contentType))
        .andExpect(jsonPath("$._links.byEmail", Matchers.notNullValue()))
        .andExpect(jsonPath("$._links.byMetadata", Matchers.notNullValue()));
    }

    @Test
    public void findByEmail() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .build();

        // create a second eperson to put the previous one in a no special position (is not the first as we have default
        // epersons is not the latest created)
        EPerson ePerson2 = EPersonBuilder.createEPerson(context)
                                         .withNameInMetadata("Jane", "Smith")
                                         .withEmail("janesmith@example.com")
                                         .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/search/byEmail")
                                             .param("email", ePerson.getEmail()))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$", is(
                                    EPersonMatcher.matchEPersonEntry(ePerson)
                            )));

        // it must be case-insensitive
        getClient(authToken).perform(get("/api/eperson/epersons/search/byEmail")
                                             .param("email", ePerson.getEmail().toUpperCase()))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$", is(
                                    EPersonMatcher.matchEPersonEntry(ePerson)
                            )));
    }

    @Test
    public void findByEmailUndefined() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/search/byEmail")
                                             .param("email", "undefined@undefined.com"))
                            .andExpect(status().isNoContent());
    }

    @Test
    public void findByEmailMissingParameter() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/search/byEmail"))
                            .andExpect(status().isBadRequest());
    }

    @Test
    public void findByMetadataUsingLastName() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .build();

        EPerson ePerson2 = EPersonBuilder.createEPerson(context)
                                         .withNameInMetadata("Jane", "Smith")
                                         .withEmail("janesmith@example.com")
                                         .build();

        EPerson ePerson3 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Tom", "Doe")
                .withEmail("tomdoe@example.com")
                .build();

        EPerson ePerson4 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Dirk", "Doe-Postfix")
                .withEmail("dirkdoepostfix@example.com")
                .build();

        EPerson ePerson5 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Harry", "Prefix-Doe")
                .withEmail("harrydoeprefix@example.com")
                .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/search/byMetadata")
                    .param("query", ePerson.getLastName()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(contentType))
                    .andExpect(jsonPath("$._embedded.epersons", Matchers.containsInAnyOrder(
                            EPersonMatcher.matchEPersonEntry(ePerson),
                            EPersonMatcher.matchEPersonEntry(ePerson3),
                            EPersonMatcher.matchEPersonEntry(ePerson4),
                            EPersonMatcher.matchEPersonEntry(ePerson5)
                    )))
                    .andExpect(jsonPath("$.page.totalElements", is(4)));

        // it must be case insensitive
        getClient(authToken).perform(get("/api/eperson/epersons/search/byMetadata")
                .param("query", ePerson.getLastName().toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.epersons", Matchers.containsInAnyOrder(
                        EPersonMatcher.matchEPersonEntry(ePerson),
                        EPersonMatcher.matchEPersonEntry(ePerson3),
                        EPersonMatcher.matchEPersonEntry(ePerson4),
                        EPersonMatcher.matchEPersonEntry(ePerson5)
                )))
                .andExpect(jsonPath("$.page.totalElements", is(4)));
    }

    @Test
    public void findByMetadataUsingFirstName() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .build();

        EPerson ePerson2 = EPersonBuilder.createEPerson(context)
                                         .withNameInMetadata("Jane", "Smith")
                                         .withEmail("janesmith@example.com")
                                         .build();

        EPerson ePerson3 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("John", "Smith")
                .withEmail("tomdoe@example.com")
                .build();

        EPerson ePerson4 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("John-Postfix", "Smath")
                .withEmail("dirkdoepostfix@example.com")
                .build();

        EPerson ePerson5 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Prefix-John", "Smoth")
                .withEmail("harrydoeprefix@example.com")
                .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/search/byMetadata")
                    .param("query", ePerson.getFirstName()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(contentType))
                    .andExpect(jsonPath("$._embedded.epersons", Matchers.containsInAnyOrder(
                            EPersonMatcher.matchEPersonEntry(ePerson),
                            EPersonMatcher.matchEPersonEntry(ePerson3),
                            EPersonMatcher.matchEPersonEntry(ePerson4),
                            EPersonMatcher.matchEPersonEntry(ePerson5)
                    )))
                    .andExpect(jsonPath("$.page.totalElements", is(4)));

        // it must be case insensitive
        getClient(authToken).perform(get("/api/eperson/epersons/search/byMetadata")
                .param("query", ePerson.getFirstName().toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.epersons", Matchers.containsInAnyOrder(
                        EPersonMatcher.matchEPersonEntry(ePerson),
                        EPersonMatcher.matchEPersonEntry(ePerson3),
                        EPersonMatcher.matchEPersonEntry(ePerson4),
                        EPersonMatcher.matchEPersonEntry(ePerson5)
                )))
                .andExpect(jsonPath("$.page.totalElements", is(4)));
    }

    @Test
    public void findByMetadataUsingEmail() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .build();

        EPerson ePerson2 = EPersonBuilder.createEPerson(context)
                                         .withNameInMetadata("Jane", "Smith")
                                         .withEmail("janesmith@example.com")
                                         .build();

        EPerson ePerson3 = EPersonBuilder.createEPerson(context)
                                         .withNameInMetadata("Tom", "Doe")
                                         .withEmail("tomdoe@example.com")
                                         .build();

        EPerson ePerson4 = EPersonBuilder.createEPerson(context)
                                         .withNameInMetadata("Dirk", "Doe-Postfix")
                                         .withEmail("dirkdoepostfix@example.com")
                                         .build();

        EPerson ePerson5 = EPersonBuilder.createEPerson(context)
                                         .withNameInMetadata("Harry", "Prefix-Doe")
                                         .withEmail("harrydoeprefix@example.com")
                                         .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/search/byMetadata")
                                             .param("query", ePerson.getEmail()))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$._embedded.epersons", Matchers.contains(
                                    EPersonMatcher.matchEPersonEntry(ePerson)
                            )))
                            .andExpect(jsonPath("$.page.totalElements", is(1)));

        // it must be case insensitive
        getClient(authToken).perform(get("/api/eperson/epersons/search/byMetadata")
                                             .param("query", ePerson.getEmail().toLowerCase()))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$._embedded.epersons", Matchers.contains(
                                    EPersonMatcher.matchEPersonEntry(ePerson)
                            )))
                            .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void findByMetadataUsingUuid() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .build();

        EPerson ePerson2 = EPersonBuilder.createEPerson(context)
                                         .withNameInMetadata("Jane", "Smith")
                                         .withEmail("janesmith@example.com")
                                         .build();

        EPerson ePerson3 = EPersonBuilder.createEPerson(context)
                                         .withNameInMetadata("Tom", "Doe")
                                         .withEmail("tomdoe@example.com")
                                         .build();

        EPerson ePerson4 = EPersonBuilder.createEPerson(context)
                                         .withNameInMetadata("Dirk", "Doe-Postfix")
                                         .withEmail("dirkdoepostfix@example.com")
                                         .build();

        EPerson ePerson5 = EPersonBuilder.createEPerson(context)
                                         .withNameInMetadata("Harry", "Prefix-Doe")
                                         .withEmail("harrydoeprefix@example.com")
                                         .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/search/byMetadata")
                                             .param("query", String.valueOf(ePerson.getID())))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$._embedded.epersons", Matchers.contains(
                                    EPersonMatcher.matchEPersonEntry(ePerson)
                            )))
                            .andExpect(jsonPath("$.page.totalElements", is(1)));

        // it must be case insensitive
        getClient(authToken).perform(get("/api/eperson/epersons/search/byMetadata")
                                             .param("query", String.valueOf(ePerson.getID()).toLowerCase()))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$._embedded.epersons", Matchers.contains(
                                    EPersonMatcher.matchEPersonEntry(ePerson)
                            )))
                            .andExpect(jsonPath("$.page.totalElements", is(1)));
    }


    @Test
    public void findByMetadataUnauthorized() throws Exception {
        getClient().perform(get("/api/eperson/epersons/search/byMetadata")
                                             .param("query", "Doe, John"))
                            .andExpect(status().isUnauthorized());
    }

    @Test
    public void findByMetadataForbidden() throws Exception {
        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/search/byMetadata")
                                             .param("query", "Doe, John"))
                            .andExpect(status().isForbidden());
    }

    @Test
    public void findByMetadataUndefined() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/search/byMetadata")
                .param("query", "Doe, John"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(contentType))
        .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findByMetadataMissingParameter() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/search/byMetadata"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteOne() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        // Delete
        getClient(token).perform(delete("/api/eperson/epersons/" + ePerson.getID()))
                .andExpect(status().is(204));

        // Verify 404 after delete
        getClient().perform(get("/api/eperson/epersons/" + ePerson.getID()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void deleteUnauthorized() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .build();

        context.restoreAuthSystemState();

        // login as a basic user
        String token = getAuthToken(eperson.getEmail(), password);

        // Delete using an unauthorized user
        getClient(token).perform(delete("/api/eperson/epersons/" + ePerson.getID()))
                .andExpect(status().isForbidden());

        // login as admin
        String adminToken = getAuthToken(admin.getEmail(), password);

        // Verify the eperson is still here
        getClient(adminToken).perform(get("/api/eperson/epersons/" + ePerson.getID()))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteForbidden() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .build();

        context.restoreAuthSystemState();

        // Delete as anonymous user
        getClient().perform(delete("/api/eperson/epersons/" + ePerson.getID()))
                .andExpect(status().isUnauthorized());

        // login as admin
        String adminToken = getAuthToken(admin.getEmail(), password);

        // Verify the eperson is still here
        getClient(adminToken).perform(get("/api/eperson/epersons/" + ePerson.getID()))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteViolatingWorkFlowConstraints() throws Exception {
        // We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Sample", "Submitter")
                .withEmail("submitter@example.com")
                .build();

        // force the use of the new user for subsequent operation
        context.setCurrentUser(ePerson);

        // ** GIVEN **
        // 1. A community with a logo
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").withLogo("logo_community")
                                          .build();

        // 2. A collection with a logo
        Collection col = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection")
                                          .withLogo("logo_collection")
                                          .withWorkflowGroup(1, ePerson)
                                          .build();


        // 3. Create an item that will prevent the deletion of the eperson account (it is the submitter)
        WorkflowItemBuilder.createWorkflowItem(context, col);
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        // 422 error when trying to DELETE the eperson=submitter
        getClient(token).perform(delete("/api/eperson/epersons/" + ePerson.getID()))
                   .andExpect(status().isUnprocessableEntity());

        // Verify the eperson is still here
        getClient(token).perform(get("/api/eperson/epersons/" + ePerson.getID()))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteLastPersonInWorkflowGroup() throws Exception {
        // set up workflow group with ePerson as only member
        context.turnOffAuthorisationSystem();
        EPerson ePerson = EPersonBuilder
            .createEPerson(context)
            .withEmail("eperson@example.com")
            .withNameInMetadata("Sample", "EPerson")
            .build();
        Community community = CommunityBuilder
            .createCommunity(context)
            .build();
        Collection collection = CollectionBuilder
            .createCollection(context, community)
            .withWorkflowGroup(1, ePerson)
            .build();
        Group workflowGroup = collection.getWorkflowStep1(context);
        context.restoreAuthSystemState();

        // enable Polish locale
        configurationService.setProperty("webui.supported.locales", "en, pl");

        // generate expectations
        String key = RESTEmptyWorkflowGroupException.MESSAGE_KEY;
        String[] values = {
            ePerson.getID().toString(),
            workflowGroup.getID().toString(),
        };
        MessageFormat defaultFmt = new MessageFormat(I18nUtil.getMessage(key));
        MessageFormat plFmt = new MessageFormat(I18nUtil.getMessage(key, new Locale("pl")));

        // make request using Polish locale
        getClient(getAuthToken(admin.getEmail(), password))
            .perform(
                delete("/api/eperson/epersons/" + ePerson.getID())
                    .header("Accept-Language", "pl") // request Polish response
            )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(status().reason(is(plFmt.format(values))))
            .andExpect(status().reason(startsWith("[PL]"))); // verify it did not fall back to default locale

        // make request using default locale
        getClient(getAuthToken(admin.getEmail(), password))
            .perform(delete("/api/eperson/epersons/" + ePerson.getID()))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(status().reason(is(defaultFmt.format(values))))
            .andExpect(status().reason(not(startsWith("[PL]"))));
    }

    @Test
    public void patchByForbiddenUser() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/netid", "newNetId");
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(eperson.getEmail(), password);

        // should be forbidden.
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                   .andExpect(status().isForbidden());

        token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/eperson/epersons/" + ePerson.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.netid", Matchers.is(nullValue())));

    }

    @Test
    public void patchByAnonymousUser() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/netid", "newNetId");
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);
        // should be unauthorized.
        getClient().perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isUnauthorized());

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/eperson/epersons/" + ePerson.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.netid", Matchers.is(nullValue())));

    }

    @Test
    public void patchNetId() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .withNetId("testId")
                                        .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/netid", "newNetId");
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(admin.getEmail(), password);

        // update net id
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.netid", Matchers.is("newNetId")))
                        .andExpect(jsonPath("$.email", Matchers.is("johndoe@example.com")))
                        .andExpect(jsonPath("$.canLogIn", Matchers.is(false)));

    }

    @Test
    public void patchUsingStringForBoolean() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .withNetId("testId")
                                        .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();

        // String should be converted to boolean.
        ReplaceOperation replaceOperation = new ReplaceOperation("/canLogin", "true");
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        // updatecan login
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.netid", Matchers.is("testId")))
                        .andExpect(jsonPath("$.email", Matchers.is("johndoe@example.com")))
                        .andExpect(jsonPath("$.canLogIn", Matchers.is(true)));

        // String should be converted to boolean.
        replaceOperation = new ReplaceOperation("/canLogin", "false");
        ops.set(0, replaceOperation);
        patchBody = getPatchContent(ops);

        // update can login
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.netid", Matchers.is("testId")))
                        .andExpect(jsonPath("$.email", Matchers.is("johndoe@example.com")))
                        .andExpect(jsonPath("$.canLogIn", Matchers.is(false)));

    }

    @Test
    public void replaceOnNonExistentValue() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/netid", "newNetId");
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(admin.getEmail(), password);

        // update of netId should fail.
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isBadRequest());

        getClient(token).perform(get("/api/eperson/epersons/" + ePerson.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.email", Matchers.is("johndoe@example.com")))
                        .andExpect(jsonPath("$.netid", Matchers.nullValue()));

    }

    @Test
    public void patchNetIdMissingValue() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .withNetId("testId")
                                        .build();

        context.restoreAuthSystemState();

        String newId = "newId";

        String token = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/netid", newId);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        // initialize to true
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.netid", Matchers.is(newId)));


        List<Operation> ops2 = new ArrayList<Operation>();
        ReplaceOperation replaceOperation2 = new ReplaceOperation("/netid", null);
        ops2.add(replaceOperation2);
        patchBody = getPatchContent(ops2);

        // should return bad request
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isBadRequest());

        // value should be unchanged.
        getClient(token).perform(get("/api/eperson/epersons/" + ePerson.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.netid", Matchers.is(newId)))
                        .andExpect(jsonPath("$.email", Matchers.is("johndoe@example.com")))
                        .andExpect(jsonPath("$.canLogIn", Matchers.is(false)));


    }

    @Test
    public void patchCanLogin() throws Exception {

        context.turnOffAuthorisationSystem();

        // Create a new EPerson and ensure canLogin is set to "false" initially
        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .withCanLogin(false)
                                        .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/canLogin", true);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(admin.getEmail(), password);

        // updates canLogIn to true
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.canLogIn", Matchers.is(true)))
                        .andExpect(jsonPath("$.email", Matchers.is("johndoe@example.com")))
                        .andExpect(jsonPath("$.netid", Matchers.nullValue()))
                        // Verify CSRF token has NOT been changed (as neither the cookie nor header are sent back)
                        // This is included in this single test as a simple proof that CSRF tokens don't change on
                        // basic requests. Additional tests regarding CSRF tokens are in AuthenticationRestControllerIT
                        .andExpect(cookie().doesNotExist("DSPACE-XSRF-COOKIE"))
                        .andExpect(header().doesNotExist("DSPACE-XSRF-TOKEN"));


    }

    @Test
    public void patchCanLoginMissingValue() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/canLogin", true);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        // initialize to true
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.canLogIn", Matchers.is(true)));;


        List<Operation> ops2 = new ArrayList<Operation>();
        ReplaceOperation replaceOperation2 = new ReplaceOperation("/canLogin", null);
        ops2.add(replaceOperation2);
        patchBody = getPatchContent(ops2);

        // should return bad request
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isBadRequest());

        // value should still be true.
        getClient(token).perform(get("/api/eperson/epersons/" + ePerson.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.canLogIn", Matchers.is(true)))
                        .andExpect(jsonPath("$.email", Matchers.is("johndoe@example.com")))
                        .andExpect(jsonPath("$.requireCertificate", Matchers.is(false)));

    }

    @Test
    public void patchRequireCertificate() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        // Boolean operations should accept either string or boolean as value. Try boolean.
        ReplaceOperation replaceOperation = new ReplaceOperation("/certificate", false);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(admin.getEmail(), password);

        // updates requireCertificate to false
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.requireCertificate", Matchers.is(false)))
                        .andExpect(jsonPath("$.email", Matchers.is("johndoe@example.com")))
                        .andExpect(jsonPath("$.netid", Matchers.nullValue()));

    }

    @Test
    public void patchRequireCertificateMissingValue() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/certificate", true);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        // initialize to true
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.requireCertificate", Matchers.is(true)));;

        List<Operation> ops2 = new ArrayList<Operation>();
        ReplaceOperation replaceOperation2 = new ReplaceOperation("/certificate",null);
        ops2.add(replaceOperation2);
        patchBody = getPatchContent(ops2);

        // should return bad request
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isBadRequest());

        // value should still be true.
        getClient(token).perform(get("/api/eperson/epersons/" + ePerson.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.requireCertificate", Matchers.is(true)))
                        .andExpect(jsonPath("$.email", Matchers.is("johndoe@example.com")))
                        .andExpect(jsonPath("$.canLogIn", Matchers.is(false)));


    }

    @Test
    public void patchPassword() throws Exception {
        configurationService.setProperty("authentication-password.regex-validation.pattern", "");
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .withPassword(password)
                                        .build();

        context.restoreAuthSystemState();

        String newPassword = "newpassword";
        String patchBody = buildPasswordAddOperationPatchBody(newPassword, password);

        String token = getAuthToken(admin.getEmail(), password);

        // updates password
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        // login with new password
        token = getAuthToken(ePerson.getEmail(), newPassword);
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        // can't login with old password
        token = getAuthToken(ePerson.getEmail(), password);
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void patchPasswordIsForbidden() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson1 = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .withPassword(password)
                                        .build();

        EPerson ePerson2 = EPersonBuilder.createEPerson(context)
                                          .withNameInMetadata("Jane", "Doe")
                                          .withEmail("Janedoe@example.com")
                                          .withPassword(password)
                                          .build();

        context.restoreAuthSystemState();

        String newPassword = "newpassword";
        String patchBody = buildPasswordAddOperationPatchBody(newPassword, password);

        // eperson one
        String token = getAuthToken(ePerson1.getEmail(), password);

        // should not be able to update the password of eperson two
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson2.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isForbidden());

        // login with old password
        token = getAuthToken(ePerson2.getEmail(), password);
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        // can't login with new password
        token = getAuthToken(ePerson2.getEmail(), newPassword);
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void patchPasswordForNonAdminUser() throws Exception {
        configurationService.setProperty("authentication-password.regex-validation.pattern", "");
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .withPassword(password)
                                        .build();

        context.restoreAuthSystemState();

        String newPassword = "newpassword";
        String patchBody = buildPasswordAddOperationPatchBody(newPassword, password);

        String token = getAuthToken(ePerson.getEmail(), password);

        // updates password
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        // login with new password
        token = getAuthToken(ePerson.getEmail(), newPassword);
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        // can't login with old password
        token = getAuthToken(ePerson.getEmail(), password);
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void patchPasswordReplaceOnNonExistentValue() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("John", "Doe")
                .withEmail("Johndoe@example.com")
                .build();

        context.restoreAuthSystemState();

        String newPassword = "newpassword";
        String patchBody = buildPasswordAddOperationPatchBody(newPassword, null);
        String token = getAuthToken(admin.getEmail(), password);

        // replace of password should fail
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isBadRequest());

        // can't login with new password
        token = getAuthToken(ePerson.getEmail(), newPassword);
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void patchCanLoginNonAdminUser() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("CanLogin@example.com")
                                        .withPassword(password)
                                        .build();

        context.restoreAuthSystemState();

        ReplaceOperation replaceOperation = new ReplaceOperation("/canLogin", true);
        List<Operation> ops = new ArrayList<Operation>();
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(ePerson.getEmail(), password);

        // should return
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isForbidden());

    }

    @Test
    public void patchCertificateNonAdminUser() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("CanLogin@example.com")
                                        .withPassword(password)
                                        .build();

        context.restoreAuthSystemState();

        ReplaceOperation replaceOperation = new ReplaceOperation("/certificate", true);
        List<Operation> ops = new ArrayList<Operation>();
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(ePerson.getEmail(), password);

        // should return
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isForbidden());

    }

    @Test
    public void patchPasswordMissingValue() throws Exception {
        context.turnOffAuthorisationSystem();

        String originalPw = "testpass79bC";

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .withPassword(originalPw)
                                        .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        String patchBody = buildPasswordAddOperationPatchBody(null, null);

        // adding null pw should return bad request
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isBadRequest());

        // login with original password
        token = getAuthToken(ePerson.getEmail(), originalPw);
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        // can't login with null password
        token = getAuthToken(ePerson.getEmail(), null);
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void patchPasswordNotInitialised() throws Exception {
        configurationService.setProperty("authentication-password.regex-validation.pattern", "");
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("userNotInitialised@example.com")
                                        .withCanLogin(true)
                                        .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        String newPassword = "newpass";
        String patchBody = buildPasswordAddOperationPatchBody(newPassword, null);

        // initialize password with add operation, not set during creation
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        // login with new password => succeeds
        token = getAuthToken(ePerson.getEmail(), newPassword);
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        // can't login with old password
        token = getAuthToken(ePerson.getEmail(), password);
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void patchEmail() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .withPassword(password)
                                        .build();

        context.restoreAuthSystemState();

        String newEmail = "janedoe@real-email.com";

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/email", newEmail);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(admin.getEmail(), password);

        // updates email
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(jsonPath("$.email", Matchers.is(newEmail)))
                        .andExpect(status().isOk());

        // login with new email address
        token = getAuthToken(newEmail, password);
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

    }

    @Test
    public void patchEmailNonAdminUser() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .withPassword(password)
                                        .build();

        context.restoreAuthSystemState();

        String newEmail = "new@email.com";

        ReplaceOperation replaceOperation = new ReplaceOperation("/email", newEmail);
        List<Operation> ops = new ArrayList<Operation>();
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(ePerson.getEmail(), password);

        // returns 403
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isForbidden());

    }

    @Test
    public void patchEmailMissingValue() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .withPassword(password)
                                        .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation2 = new ReplaceOperation("/email", null);
        ops.add(replaceOperation2);
        String patchBody = getPatchContent(ops);

        // should return bad request
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isBadRequest());

        // login with original email
        token = getAuthToken(ePerson.getEmail(), password);
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void patchMultipleOperationsWithSuccess() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .withNetId("testId")
                                        .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();

        ReplaceOperation replaceOperation1 = new ReplaceOperation("/canLogin", true);
        ops.add(replaceOperation1);
        ReplaceOperation replaceOperation2 = new ReplaceOperation("/netid", "multitestId");
        ops.add(replaceOperation2);
        ReplaceOperation replaceOperation3 = new ReplaceOperation("/certificate", true);
        ops.add(replaceOperation3);
        String patchBody = getPatchContent(ops);

        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.canLogIn", Matchers.is(true)))
                        .andExpect(jsonPath("$.netid", Matchers.is("multitestId")))
                        .andExpect(jsonPath("$.requireCertificate", Matchers.is(true)));

    }

    @Test
    public void patchMultipleOperationsWithFailure() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);


        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation0 = new ReplaceOperation("/canLogin", true);
        ops.add(replaceOperation0);
        String patchBody = getPatchContent(ops);

        // Initialized canLogIn value is true.
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.canLogIn", Matchers.is(true)));

        // The first operation in the series sets canLogIn to be false.
        ReplaceOperation replaceOperation1 = new ReplaceOperation("/canLogin", false);
        ops.add(replaceOperation1);
        ReplaceOperation replaceOperation2 = new ReplaceOperation("/certificate", true);
        ops.add(replaceOperation2);
        // This will fail. The path does not exist.
        ReplaceOperation replaceOperation3 = new ReplaceOperation("/nonexistentPath", "somevalue");
        ops.add(replaceOperation3);
        ReplaceOperation replaceOperation4 = new ReplaceOperation("/certificate", false);
        ops.add(replaceOperation4);
        patchBody = getPatchContent(ops);

        // The 3rd operation should result in bad request
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isBadRequest());

        // The value of canLogIn should equal the previously initialized value.
        getClient(token).perform(get("/api/eperson/epersons/" + ePerson.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.canLogIn", Matchers.is(true)));

    }

    @Test
    public void patchEPersonMetadataAuthorized() throws Exception {
        runPatchMetadataTests(admin, 200);
    }

    @Test
    public void patchEPersonMetadataUnauthorized() throws Exception {
        runPatchMetadataTests(eperson, 403);
    }

    private void runPatchMetadataTests(EPerson asUser, int expectedStatus) throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson ePerson = EPersonBuilder.createEPerson(context).withEmail("user@example.com").build();
        context.restoreAuthSystemState();
        String token = getAuthToken(asUser.getEmail(), password);

        new MetadataPatchSuite().runWith(getClient(token), "/api/eperson/epersons/" + ePerson.getID(), expectedStatus);
    }

    @Test
    public void patchMetadataByAdmin() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .build();

        String newName = "JohnReplace";

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/metadata/eperson.firstname", newName);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(admin.getEmail(), password);

        // should be allowed, and eperson.firstname should be replaced.
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.metadata", Matchers.allOf(
                            MetadataMatcher.matchMetadata("eperson.firstname", newName))));

        // The replacement of the eperson.firstname value is persisted
        getClient(token).perform(get("/api/eperson/epersons/" + ePerson.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", hasJsonPath("$.metadata", allOf(
                            matchMetadata("eperson.firstname", newName)))));
    }

    @Test
    public void patchOwnMetadataByNonAdminUser() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .withPassword(password)
                                        .build();

        String newName = "JohnReplace";

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/metadata/eperson.firstname", newName);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(ePerson.getEmail(), password);

        // replace operation on eperson.firstname by owning user
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                                     .content(patchBody)
                                     .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.metadata", Matchers.allOf(
                            MetadataMatcher.matchMetadata("eperson.firstname", newName))));

        // The replacement of the eperson.firstname value is persisted
        getClient(token).perform(get("/api/eperson/epersons/" + ePerson.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", hasJsonPath("$.metadata", allOf(
                            matchMetadata("eperson.firstname", newName)))));
    }

    @Test
    public void patchNotOwnMetadataByNonAdminUser() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .withPassword(password)
                                        .build();

        EPerson ePerson2 = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("Jane", "Smith")
                                        .withEmail("Janesmith@example.com")
                                        .withPassword(password)
                                        .build();

        String newName = "JohnReplace";

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/metadata/eperson.firstname", newName);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String token2 = getAuthToken(ePerson2.getEmail(), password);
        String token = getAuthToken(ePerson.getEmail(), password);

        // attempts to replace eperson.firstname, not allowed, only allowed by admin or owning user
        getClient(token2).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                                     .content(patchBody)
                                     .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isForbidden());

        // No replacement of the eperson.firstname
        getClient(token).perform(get("/api/eperson/epersons/" + ePerson.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", hasJsonPath("$.metadata", allOf(
                            matchMetadata("eperson.firstname", "John")))));
    }

    @Test
    public void newlyCreatedAccountHasNoGroups() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Mik", "Reck")
                .withEmail("MikReck@email.com")
                .withPassword("qwerty01")
                .build();

        context.restoreAuthSystemState();

        String tokenEperson1 = getAuthToken(ePerson1.getEmail(), "qwerty01");
        // by contract the groups embedded in the eperson only contains direct explicit membership,
        // so the anonymous group is not listed
        getClient(tokenEperson1).perform(get("/api/eperson/epersons/" + ePerson1.getID())
                .param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", Matchers.allOf(
                        hasJsonPath("$._embedded.groups._embedded.groups.length()", is(0)),
                        hasJsonPath("$._embedded.groups.page.totalElements", is(0))
                )));
    }

    /**
     * Test that epersons/<:uuid>/groups endpoint returns the direct groups of the epersons
     * @throws Exception
     */
    @Test
    public void getDirectEpersonGroups() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .withPassword(password)
                                        .build();

        Group parentGroup1 = GroupBuilder.createGroup(context)
                                        .withName("Test Parent Group 1")
                                        .build();

        Group childGroup1 = GroupBuilder.createGroup(context)
                                       .withName("Test Child Group 1")
                                       .withParent(parentGroup1)
                                       .addMember(ePerson)
                                       .build();

        Group parentGroup2 = GroupBuilder.createGroup(context)
                                         .withName("Test Parent Group 2")
                                         .build();

        Group childGroup2 = GroupBuilder.createGroup(context)
                                        .withName("Test Child Group 2")
                                        .withParent(parentGroup2)
                                        .addMember(ePerson)
                                        .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/" + ePerson.getID() + "/groups"))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$._embedded.groups", containsInAnyOrder(
                                    GroupMatcher.matchGroupWithName(childGroup1.getName()),
                                    GroupMatcher.matchGroupWithName(childGroup2.getName()))))
                            .andExpect(jsonPath("$._embedded.groups", Matchers.not(
                                    containsInAnyOrder(
                                            GroupMatcher.matchGroupWithName(parentGroup1.getName()),
                                            GroupMatcher.matchGroupWithName(parentGroup2.getName()))))
                            );

    }

    @Test
    public void patchReplacePasswordWithToken() throws Exception {
        configurationService.setProperty("authentication-password.regex-validation.pattern", "");
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .withPassword(password)
                                        .build();

        String newPassword = "newpassword";

        context.restoreAuthSystemState();

        String patchBody = buildPasswordAddOperationPatchBody(newPassword, null);

        accountService.sendRegistrationInfo(context, ePerson.getEmail());
        String tokenForEPerson = registrationDataService.findByEmail(context, ePerson.getEmail()).getToken();
        PasswordHash oldPassword = ePersonService.getPasswordHash(ePerson);
        // updates password
        getClient().perform(patch("/api/eperson/epersons/" + ePerson.getID())
                                     .content(patchBody)
                                     .contentType(MediaType.APPLICATION_JSON_PATCH_JSON)
                                     .param("token", tokenForEPerson))
                        .andExpect(status().isOk());

        PasswordHash newPasswordHash = ePersonService.getPasswordHash(ePerson);
        assertNotEquals(oldPassword, newPasswordHash);
        assertTrue(registrationDataService.findByEmail(context, ePerson.getEmail()) == null);

        assertNull(registrationDataService.findByToken(context, tokenForEPerson));
    }


    @Test
    public void patchReplacePasswordWithRandomTokenPatchFail() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .withPassword(password)
                                        .build();

        String newPassword = "newpassword";

        context.restoreAuthSystemState();

        String patchBody = buildPasswordAddOperationPatchBody(newPassword, null);

        accountService.sendRegistrationInfo(context, ePerson.getEmail());
        String tokenForEPerson = registrationDataService.findByEmail(context, ePerson.getEmail()).getToken();
        PasswordHash oldPassword = ePersonService.getPasswordHash(ePerson);
        // updates password
        getClient().perform(patch("/api/eperson/epersons/" + ePerson.getID())
                                     .content(patchBody)
                                     .contentType(MediaType.APPLICATION_JSON_PATCH_JSON)
                                     .param("token", "RandomToken"))
                        .andExpect(status().isUnauthorized());

        PasswordHash newPasswordHash = ePersonService.getPasswordHash(ePerson);
        assertEquals(oldPassword.getHashString(),newPasswordHash.getHashString());
        assertNotNull(registrationDataService.findByEmail(context, ePerson.getEmail()));
        assertEquals(registrationDataService.findByEmail(context, ePerson.getEmail()).getToken(), tokenForEPerson);

        context.turnOffAuthorisationSystem();
        registrationDataService.deleteByToken(context, tokenForEPerson);
        context.restoreAuthSystemState();
    }

    @Test
    public void patchReplacePasswordWithOtherUserTokenFail() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .withPassword(password)
                                        .build();


        EPerson ePersonTwo = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("Smith", "Donald")
                                        .withEmail("donaldSmith@fake-email.com")
                                        .withPassword(password)
                                        .build();

        String newPassword = "newpassword";

        context.restoreAuthSystemState();

        String patchBody = buildPasswordAddOperationPatchBody(newPassword, null);

        accountService.sendRegistrationInfo(context, ePerson.getEmail());
        accountService.sendRegistrationInfo(context, ePersonTwo.getEmail());
        String tokenForEPerson = registrationDataService.findByEmail(context, ePerson.getEmail()).getToken();
        String tokenForEPersonTwo = registrationDataService.findByEmail(context, ePersonTwo.getEmail()).getToken();

        PasswordHash oldPassword = ePersonService.getPasswordHash(ePerson);
        // updates password
        getClient().perform(patch("/api/eperson/epersons/" + ePerson.getID())
                                     .content(patchBody)
                                     .contentType(MediaType.APPLICATION_JSON_PATCH_JSON)
                                     .param("token", tokenForEPersonTwo))
                        .andExpect(status().isUnauthorized());

        PasswordHash newPasswordHash = ePersonService.getPasswordHash(ePerson);
        assertEquals(oldPassword.getHashString(),newPasswordHash.getHashString());
        assertNotNull(registrationDataService.findByEmail(context, ePerson.getEmail()));

        context.turnOffAuthorisationSystem();
        registrationDataService.deleteByToken(context, tokenForEPerson);
        registrationDataService.deleteByToken(context, tokenForEPersonTwo);
        context.restoreAuthSystemState();
    }

    @Test
    public void patchReplaceEmailWithTokenFail() throws Exception {
        context.turnOffAuthorisationSystem();

        String originalEmail = "johndoe@fake-email.com";
        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail(originalEmail)
                                        .withPassword(password)
                                        .build();

        String newEmail = "johnyandmaria@fake-email.com";

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/email", newEmail);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);
        accountService.sendRegistrationInfo(context, ePerson.getEmail());
        String tokenForEPerson = registrationDataService.findByEmail(context, ePerson.getEmail()).getToken();
        PasswordHash oldPassword = ePersonService.getPasswordHash(ePerson);
        // updates password
        getClient().perform(patch("/api/eperson/epersons/" + ePerson.getID())
                                     .content(patchBody)
                                     .contentType(MediaType.APPLICATION_JSON_PATCH_JSON)
                                     .param("token", tokenForEPerson))
                        .andExpect(status().isUnauthorized());

        PasswordHash newPasswordHash = ePersonService.getPasswordHash(ePerson);
        assertEquals(oldPassword.getHashString(),newPasswordHash.getHashString());
        assertNotNull(registrationDataService.findByEmail(context, ePerson.getEmail()));
        assertEquals(ePerson.getEmail(), originalEmail);

        context.turnOffAuthorisationSystem();
        registrationDataService.delete(context, registrationDataService.findByEmail(context, ePerson.getEmail()));
        registrationDataService.deleteByToken(context, tokenForEPerson);
        context.restoreAuthSystemState();

    }

    @Test
    public void registerNewAccountPatchUpdatePasswordRandomUserUuidFail() throws Exception {
        context.turnOffAuthorisationSystem();

        ObjectMapper mapper = new ObjectMapper();
        String newRegisterEmail = "new-register@fake-email.com";
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(newRegisterEmail);
        getClient().perform(post("/api/eperson/registrations")
                            .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsBytes(registrationRest)))
                   .andExpect(status().isCreated());

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .withPassword(password)
                                        .build();

        String newPassword = "newpassword";

        context.restoreAuthSystemState();

        String patchBody = buildPasswordAddOperationPatchBody(newPassword, null);

        accountService.sendRegistrationInfo(context, ePerson.getEmail());
        String newRegisterToken = registrationDataService.findByEmail(context, newRegisterEmail).getToken();
        PasswordHash oldPassword = ePersonService.getPasswordHash(ePerson);
        try {
            // updates password
            getClient().perform(patch("/api/eperson/epersons/" + ePerson.getID())
                                         .content(patchBody)
                                         .contentType(MediaType.APPLICATION_JSON_PATCH_JSON)
                                         .param("token", newRegisterToken))
                            .andExpect(status().isUnauthorized());

            PasswordHash newPasswordHash = ePersonService.getPasswordHash(ePerson);
            assertTrue(StringUtils.equalsIgnoreCase(oldPassword.getHashString(),newPasswordHash.getHashString()));
            assertFalse(registrationDataService.findByEmail(context, ePerson.getEmail()) == null);
            assertFalse(registrationDataService.findByEmail(context, newRegisterEmail) == null);
        } finally {
            context.turnOffAuthorisationSystem();
            registrationDataService.delete(context, registrationDataService.findByEmail(context, ePerson.getEmail()));
            registrationDataService.deleteByToken(context, newRegisterToken);
            context.restoreAuthSystemState();
        }
    }

    @Test
    public void postEPersonWithTokenWithoutEmailProperty() throws Exception {
        configurationService.setProperty("authentication-password.regex-validation.pattern", "");
        ObjectMapper mapper = new ObjectMapper();

        String newRegisterEmail = "new-register@fake-email.com";
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(newRegisterEmail);
        getClient().perform(post("/api/eperson/registrations")
                                .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsBytes(registrationRest)))
                   .andExpect(status().isCreated());
        String newRegisterToken = registrationDataService.findByEmail(context, newRegisterEmail).getToken();

        EPersonRest ePersonRest = new EPersonRest();
        MetadataRest metadataRest = new MetadataRest();
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
                                // is it what you expect? EPerson.getName() returns the email...
                                //hasJsonPath("$.name", is("Doe John")),
                                hasJsonPath("$.type", is("eperson")),
                                hasJsonPath("$._links.self.href", not(empty())),
                                hasJsonPath("$.metadata", Matchers.allOf(
                                    matchMetadata("eperson.firstname", "John"),
                                    matchMetadata("eperson.lastname", "Doe")
                                )))))
                                .andDo(result -> idRef
                                    .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id"))));



            String epersonUuid = String.valueOf(idRef.get());
            EPerson createdEPerson = ePersonService.find(context, UUID.fromString(epersonUuid));
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
    public void postEPersonWithTokenWithEmailProperty() throws Exception {
        configurationService.setProperty("authentication-password.regex-validation.pattern", "");
        ObjectMapper mapper = new ObjectMapper();

        String newRegisterEmail = "new-register@fake-email.com";
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(newRegisterEmail);
        getClient().perform(post("/api/eperson/registrations")
                                .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
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
                                                      // is it what you expect? EPerson.getName() returns the email...
                                                      //hasJsonPath("$.name", is("Doe John")),
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
    public void postEPersonWithTokenWithEmailAndSelfRegisteredProperty() throws Exception {
        configurationService.setProperty("authentication-password.regex-validation.pattern", "");
        ObjectMapper mapper = new ObjectMapper();

        String newRegisterEmail = "new-register@fake-email.com";
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(newRegisterEmail);
        getClient().perform(post("/api/eperson/registrations")
                                .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
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
        ePersonRest.setSelfRegistered(true);
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
                                                      // is it what you expect? EPerson.getName() returns the email...
                                                      //hasJsonPath("$.name", is("Doe John")),
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
    public void postEPersonWithTokenWithTwoTokensDifferentEmailProperty() throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        String newRegisterEmail = "new-register@fake-email.com";
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(newRegisterEmail);
        getClient().perform(post("/api/eperson/registrations")
                                .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsBytes(registrationRest)))
                   .andExpect(status().isCreated());
        String newRegisterToken = registrationDataService.findByEmail(context, newRegisterEmail).getToken();

        String newRegisterEmailTwo = "new-register-two@fake-email.com";
        RegistrationRest registrationRestTwo = new RegistrationRest();
        registrationRestTwo.setEmail(newRegisterEmailTwo);
        getClient().perform(post("/api/eperson/registrations")
                                .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsBytes(registrationRestTwo)))
                   .andExpect(status().isCreated());
        String newRegisterTokenTwo = registrationDataService.findByEmail(context, newRegisterEmailTwo).getToken();


        EPersonRest ePersonRest = new EPersonRest();
        MetadataRest metadataRest = new MetadataRest();
        ePersonRest.setEmail(newRegisterEmailTwo);
        ePersonRest.setCanLogIn(true);
        MetadataValueRest surname = new MetadataValueRest();
        surname.setValue("Doe");
        metadataRest.put("eperson.lastname", surname);
        MetadataValueRest firstname = new MetadataValueRest();
        firstname.setValue("John");
        metadataRest.put("eperson.firstname", firstname);
        ePersonRest.setMetadata(metadataRest);
        ePersonRest.setPassword("somePassword");

        mapper.setAnnotationIntrospector(new IgnoreJacksonWriteOnlyAccess());

        try {
            getClient().perform(post("/api/eperson/epersons")
                                                               .param("token", newRegisterToken)
                                                               .content(mapper.writeValueAsBytes(ePersonRest))
                                                               .contentType(MediaType.APPLICATION_JSON))
                                                  .andExpect(status().isBadRequest());

            EPerson createdEPerson = ePersonService.findByEmail(context, newRegisterEmailTwo);
            assertNull(createdEPerson);
            assertNotNull(registrationDataService.findByToken(context, newRegisterToken));
            assertNotNull(registrationDataService.findByToken(context, newRegisterTokenTwo));
        } finally {
            context.turnOffAuthorisationSystem();
            registrationDataService.deleteByToken(context, newRegisterToken);
            registrationDataService.deleteByToken(context, newRegisterTokenTwo);
            context.restoreAuthSystemState();

        }
    }

    @Test
    public void postEPersonWithRandomTokenWithEmailProperty() throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        String newRegisterEmail = "new-register@fake-email.com";
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(newRegisterEmail);
        getClient().perform(post("/api/eperson/registrations")
                                .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
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

        mapper.setAnnotationIntrospector(new IgnoreJacksonWriteOnlyAccess());

        try {
            getClient().perform(post("/api/eperson/epersons")
                                         .param("token", "randomToken")
                                         .content(mapper.writeValueAsBytes(ePersonRest))
                                         .contentType(MediaType.APPLICATION_JSON))
                            .andExpect(status().isBadRequest());

            EPerson createdEPerson = ePersonService.findByEmail(context, newRegisterEmail);
            assertNull(createdEPerson);
            assertNotNull(registrationDataService.findByToken(context, newRegisterToken));
        } finally {
            context.turnOffAuthorisationSystem();
            registrationDataService.deleteByToken(context, newRegisterToken);
            context.restoreAuthSystemState();
        }

    }

    @Test
    public void postEPersonWithTokenWithEmailAndSelfRegisteredFalseProperty() throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        String newRegisterEmail = "new-register@fake-email.com";
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(newRegisterEmail);
        getClient().perform(post("/api/eperson/registrations")
                                .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
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
        ePersonRest.setSelfRegistered(false);

        mapper.setAnnotationIntrospector(new IgnoreJacksonWriteOnlyAccess());

        try {
            getClient().perform(post("/api/eperson/epersons")
                                                               .param("token", newRegisterToken)
                                                               .content(mapper.writeValueAsBytes(ePersonRest))
                                                               .contentType(MediaType.APPLICATION_JSON))
                                                  .andExpect(status().isBadRequest());

            EPerson createdEPerson = ePersonService.findByEmail(context, newRegisterEmail);
            assertNull(createdEPerson);
            assertNotNull(registrationDataService.findByToken(context, newRegisterToken));
        } finally {
            context.turnOffAuthorisationSystem();
            registrationDataService.deleteByToken(context, newRegisterToken);
            context.restoreAuthSystemState();
        }

    }

    @Test
    public void postEPersonWithTokenWithoutLastNameProperty() throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        String newRegisterEmail = "new-register@fake-email.com";
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(newRegisterEmail);
        getClient().perform(post("/api/eperson/registrations")
                                .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsBytes(registrationRest)))
                   .andExpect(status().isCreated());
        String newRegisterToken = registrationDataService.findByEmail(context, newRegisterEmail).getToken();


        EPersonRest ePersonRest = new EPersonRest();
        MetadataRest metadataRest = new MetadataRest();
        ePersonRest.setEmail(newRegisterEmail);
        ePersonRest.setCanLogIn(true);
        MetadataValueRest firstname = new MetadataValueRest();
        firstname.setValue("John");
        metadataRest.put("eperson.firstname", firstname);
        ePersonRest.setMetadata(metadataRest);
        ePersonRest.setPassword("somePassword");
        ePersonRest.setSelfRegistered(true);

        mapper.setAnnotationIntrospector(new IgnoreJacksonWriteOnlyAccess());

        // enable Polish locale
        configurationService.setProperty("webui.supported.locales", "en, pl");

        try {
            // make request using Polish locale
            getClient().perform(post("/api/eperson/epersons")
                                        .header("Accept-Language", "pl") // request Polish response
                                        .param("token", newRegisterToken)
                                        .content(mapper.writeValueAsBytes(ePersonRest))
                                        .contentType(MediaType.APPLICATION_JSON))
                            .andExpect(status().isUnprocessableEntity())
                            .andExpect(status().reason(is(
                                // find message in dspace-server-webapp/src/test/resources/Messages_pl.properties
                                I18nUtil.getMessage(EPersonNameNotProvidedException.MESSAGE_KEY, new Locale("pl"))
                            )))
                            .andExpect(status().reason(startsWith("[PL]"))); // verify default locale was NOT used

            // make request using default locale
            getClient().perform(post("/api/eperson/epersons")
                                        .param("token", newRegisterToken)
                                        .content(mapper.writeValueAsBytes(ePersonRest))
                                        .contentType(MediaType.APPLICATION_JSON))
                            .andExpect(status().isUnprocessableEntity())
                            .andExpect(status().reason(is(
                                I18nUtil.getMessage(EPersonNameNotProvidedException.MESSAGE_KEY)
                            )))
                            .andExpect(status().reason(not(startsWith("[PL]"))));

            EPerson createdEPerson = ePersonService.findByEmail(context, newRegisterEmail);
            assertNull(createdEPerson);
            assertNotNull(registrationDataService.findByToken(context, newRegisterToken));
        } finally {
            context.turnOffAuthorisationSystem();
            registrationDataService.deleteByToken(context, newRegisterToken);
            context.restoreAuthSystemState();
        }

    }

    @Test
    public void postEPersonWithTokenWithoutFirstNameProperty() throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        String newRegisterEmail = "new-register@fake-email.com";
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(newRegisterEmail);
        getClient().perform(post("/api/eperson/registrations")
                                .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
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
        ePersonRest.setMetadata(metadataRest);
        ePersonRest.setPassword("somePassword");
        ePersonRest.setSelfRegistered(true);

        mapper.setAnnotationIntrospector(new IgnoreJacksonWriteOnlyAccess());

        // enable Polish locale
        configurationService.setProperty("webui.supported.locales", "en, pl");

        try {
            // make request using Polish locale
            getClient().perform(post("/api/eperson/epersons")
                                        .header("Accept-Language", "pl") // request Polish response
                                        .param("token", newRegisterToken)
                                        .content(mapper.writeValueAsBytes(ePersonRest))
                                        .contentType(MediaType.APPLICATION_JSON))
                            .andExpect(status().isUnprocessableEntity())
                            .andExpect(status().reason(is(
                                // find message in dspace-server-webapp/src/test/resources/Messages_pl.properties
                                I18nUtil.getMessage(EPersonNameNotProvidedException.MESSAGE_KEY, new Locale("pl"))
                            )))
                            .andExpect(status().reason(startsWith("[PL]"))); // verify default locale was NOT used

            // make request using default locale
            getClient().perform(post("/api/eperson/epersons")
                                        .param("token", newRegisterToken)
                                        .content(mapper.writeValueAsBytes(ePersonRest))
                                        .contentType(MediaType.APPLICATION_JSON))
                            .andExpect(status().isUnprocessableEntity())
                            .andExpect(status().reason(is(
                                // find message in dspace-server-webapp/src/test/resources/Messages_pl.properties
                                I18nUtil.getMessage(EPersonNameNotProvidedException.MESSAGE_KEY)
                            )))
                            .andExpect(status().reason(not(startsWith("[PL]"))));

            EPerson createdEPerson = ePersonService.findByEmail(context, newRegisterEmail);
            assertNull(createdEPerson);
            assertNotNull(registrationDataService.findByToken(context, newRegisterToken));
        } finally {
            context.turnOffAuthorisationSystem();
            registrationDataService.deleteByToken(context, newRegisterToken);
            context.restoreAuthSystemState();
        }

    }

    @Test
    public void postEPersonWithTokenWithoutPasswordProperty() throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        String newRegisterEmail = "new-register@fake-email.com";
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(newRegisterEmail);
        getClient().perform(post("/api/eperson/registrations")
                                .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
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

        mapper.setAnnotationIntrospector(new IgnoreJacksonWriteOnlyAccess());

        try {
            getClient().perform(post("/api/eperson/epersons")
                                         .param("token", newRegisterToken)
                                         .content(mapper.writeValueAsBytes(ePersonRest))
                                         .contentType(MediaType.APPLICATION_JSON))
                            .andExpect(status().isBadRequest());

            EPerson createdEPerson = ePersonService.findByEmail(context, newRegisterEmail);
            assertNull(createdEPerson);
            assertNotNull(registrationDataService.findByToken(context, newRegisterToken));
        } finally {
            context.turnOffAuthorisationSystem();
            registrationDataService.deleteByToken(context, newRegisterToken);
            context.restoreAuthSystemState();
        }

    }

    @Test
    public void postEPersonWithWrongToken() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        String newEmail = "new-email@fake-email.com";

        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(eperson.getEmail());
        getClient().perform(post("/api/eperson/registrations")
                                .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsBytes(registrationRest)))
                   .andExpect(status().isCreated());
        String forgotPasswordToken = registrationDataService.findByEmail(context, eperson.getEmail()).getToken();


        EPersonRest ePersonRest = new EPersonRest();
        MetadataRest metadataRest = new MetadataRest();
        ePersonRest.setCanLogIn(true);
        MetadataValueRest surname = new MetadataValueRest();
        surname.setValue("Doe");
        metadataRest.put("eperson.lastname", surname);
        MetadataValueRest firstname = new MetadataValueRest();
        firstname.setValue("John");
        metadataRest.put("eperson.firstname", firstname);
        ePersonRest.setMetadata(metadataRest);
        ePersonRest.setPassword("somePassword");
        ePersonRest.setSelfRegistered(true);

        mapper.setAnnotationIntrospector(new IgnoreJacksonWriteOnlyAccess());

        try {
            getClient().perform(post("/api/eperson/epersons")
                                         .param("token", forgotPasswordToken)
                                         .content(mapper.writeValueAsBytes(ePersonRest))
                                         .contentType(MediaType.APPLICATION_JSON))
                            .andExpect(status().isBadRequest());

            EPerson createdEPerson = ePersonService.findByEmail(context, newEmail);
            assertNull(createdEPerson);
            assertNotNull(registrationDataService.findByToken(context, forgotPasswordToken));
        } finally {
            context.turnOffAuthorisationSystem();
            registrationDataService.deleteByToken(context, forgotPasswordToken);
            context.restoreAuthSystemState();
        }


    }

    @Test
    public void postEPersonWithTokenWithEmailPropertyAnonUser() throws Exception {
        configurationService.setProperty("authentication-password.regex-validation.pattern", "");
        ObjectMapper mapper = new ObjectMapper();

        String newRegisterEmail = "new-register@fake-email.com";
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(newRegisterEmail);
        getClient().perform(post("/api/eperson/registrations")
                                .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
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

        mapper.setAnnotationIntrospector(new IgnoreJacksonWriteOnlyAccess());

        AtomicReference<UUID> idRef = new AtomicReference<UUID>();

        try {
            getClient().perform(post("/api/eperson/epersons")
                                                               .param("token", newRegisterToken)
                                                               .content(mapper.writeValueAsBytes(ePersonRest))
                                                               .contentType(MediaType.APPLICATION_JSON))
                                                  .andExpect(status().isCreated())
                                                  .andExpect(jsonPath("$", Matchers.allOf(
                                                      hasJsonPath("$.uuid", not(empty())),
                                                      // is it what you expect? EPerson.getName() returns the email...
                                                      //hasJsonPath("$.name", is("Doe John")),
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
    public void findByMetadataByCommAdminAndByColAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson adminChild1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Oliver", "Rossi")
                .withEmail("adminChild1@example.com")
                .withPassword(password)
                .build();
        EPerson adminCol1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("James", "Rossi")
                .withEmail("adminCol1@example.com")
                .withPassword(password)
                .build();
        EPerson colSubmitter = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Carl", "Rossi")
                .withEmail("colSubmitter@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .withAdminGroup(eperson)
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .withAdminGroup(adminChild1)
                                           .build();

        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withAdminGroup(adminCol1)
                                           .withSubmitterGroup(colSubmitter)
                                           .build();

        context.restoreAuthSystemState();

        String tokenAdminComm = getAuthToken(adminChild1.getEmail(), password);
        String tokenAdminCol = getAuthToken(adminCol1.getEmail(), password);
        String tokencolSubmitter = getAuthToken(colSubmitter.getEmail(), password);

        getClient(tokenAdminComm).perform(get("/api/eperson/epersons/search/byMetadata")
                 .param("query", "Rossi"))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$._embedded.epersons", Matchers.containsInAnyOrder(
                            EPersonMatcher.matchEPersonEntry(adminChild1),
                            EPersonMatcher.matchEPersonEntry(adminCol1),
                            EPersonMatcher.matchEPersonEntry(colSubmitter)
                            )))
                 .andExpect(jsonPath("$.page.totalElements", is(3)));

        getClient(tokenAdminCol).perform(get("/api/eperson/epersons/search/byMetadata")
                 .param("query", "Rossi"))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$._embedded.epersons", Matchers.containsInAnyOrder(
                            EPersonMatcher.matchEPersonEntry(adminChild1),
                            EPersonMatcher.matchEPersonEntry(adminCol1),
                            EPersonMatcher.matchEPersonEntry(colSubmitter)
                            )))
                 .andExpect(jsonPath("$.page.totalElements", is(3)));

        getClient(tokencolSubmitter).perform(get("/api/eperson/epersons/search/byMetadata")
                .param("query", "Rossi"))
        .andExpect(status().isForbidden());
    }

    @Test
    public void findByMetadataByCommAdminAndByColAdminWithoutAuthorizationsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        List<String> confPropsCollectionAdmins = new LinkedList<>();
        confPropsCollectionAdmins.add("core.authorization.collection-admin.policies");
        confPropsCollectionAdmins.add("core.authorization.collection-admin.workflows");
        confPropsCollectionAdmins.add("core.authorization.collection-admin.submitters");
        confPropsCollectionAdmins.add("core.authorization.collection-admin.admin-group");

        List<String> confPropsCommunityAdmins = new LinkedList<>();
        confPropsCommunityAdmins.add("core.authorization.community-admin.policies");
        confPropsCommunityAdmins.add("core.authorization.community-admin.admin-group");
        confPropsCommunityAdmins.add("core.authorization.community-admin.collection.policies");
        confPropsCommunityAdmins.add("core.authorization.community-admin.collection.workflows");
        confPropsCommunityAdmins.add("core.authorization.community-admin.collection.submitters");
        confPropsCommunityAdmins.add("core.authorization.community-admin.collection.admin-group");

        EPerson adminChild1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Oliver", "Rossi")
                .withEmail("adminChild1@example.com")
                .withPassword(password)
                .build();
        EPerson adminCol = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("James", "Rossi")
                .withEmail("adminCol1@example.com")
                .withPassword(password)
                .build();
        EPerson col1Submitter = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Carl", "Rossi")
                .withEmail("col1Submitter@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .withAdminGroup(eperson)
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .withAdminGroup(adminChild1)
                                           .build();

        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withAdminGroup(adminCol)
                                           .withSubmitterGroup(col1Submitter)
                                           .build();

        context.restoreAuthSystemState();

        String tokenAdminCol = getAuthToken(adminCol.getEmail(), password);
        String tokenAdminComm = getAuthToken(adminChild1.getEmail(), password);

        for (String prop : confPropsCollectionAdmins) {
            getClient(tokenAdminCol).perform(get("/api/eperson/epersons/search/byMetadata")
                    .param("query", "Rossi"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(contentType))
                    .andExpect(jsonPath("$._embedded.epersons", Matchers.containsInAnyOrder(
                               EPersonMatcher.matchEPersonEntry(adminChild1),
                               EPersonMatcher.matchEPersonEntry(adminCol),
                               EPersonMatcher.matchEPersonEntry(col1Submitter)
                               )))
                    .andExpect(jsonPath("$.page.totalElements", is(3)));

            configurationService.setProperty(prop, false);
        }

        getClient(tokenAdminCol).perform(get("/api/eperson/epersons/search/byMetadata")
                .param("query", "Rossi"))
                .andExpect(status().isForbidden());

        for (String prop : confPropsCommunityAdmins) {
            getClient(tokenAdminComm).perform(get("/api/eperson/epersons/search/byMetadata")
                    .param("query", "Rossi"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(contentType))
                    .andExpect(jsonPath("$._embedded.epersons", Matchers.containsInAnyOrder(
                               EPersonMatcher.matchEPersonEntry(adminChild1),
                               EPersonMatcher.matchEPersonEntry(adminCol),
                               EPersonMatcher.matchEPersonEntry(col1Submitter)
                               )))
                    .andExpect(jsonPath("$.page.totalElements", is(3)));

            configurationService.setProperty(prop, false);
        }

        getClient(tokenAdminComm).perform(get("/api/eperson/epersons/search/byMetadata")
                .param("query", "Rossi"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void discoverableNestedLinkTest() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._links",Matchers.allOf(
                                hasJsonPath("$.epersons.href",
                                         is("http://localhost/api/eperson/epersons")),
                                hasJsonPath("$.eperson-registration.href",
                                         is("http://localhost/api/eperson/registrations"))
                        )));
    }

    @Test
    public void findByMetadataUsingFirstNamePaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson ePerson = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("John", "Doe")
                .withEmail("Johndoe@example.com").build();

        EPerson ePerson2 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Jane", "Smith")
                .withEmail("janesmith@example.com").build();

        EPerson ePerson3 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("John", "Smith")
                .withEmail("tomdoe@example.com")
                .build();

        EPerson ePerson4 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("John-Postfix", "Smath")
                .withEmail("dirkdoepostfix@example.com")
                .build();

        EPerson ePerson5 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Prefix-John", "Smoth")
                .withEmail("harrydoeprefix@example.com")
                .build();

        EPerson ePerson6 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("John", "Boychuk")
                .withEmail("johnboychuk@example.com")
                .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/search/byMetadata")
                 .param("query", ePerson.getFirstName())
                 .param("page", "0")
                 .param("size", "2"))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$._embedded.epersons", Matchers.everyItem(
                         hasJsonPath("$.type", is("eperson")))
                         ))
                 .andExpect(jsonPath("$._embedded.epersons").value(Matchers.hasSize(2)))
                 .andExpect(jsonPath("$.page.size", is(2)))
                 .andExpect(jsonPath("$.page.number", is(0)))
                 .andExpect(jsonPath("$.page.totalPages", is(3)))
                 .andExpect(jsonPath("$.page.totalElements", is(5)));

        getClient(authToken).perform(get("/api/eperson/epersons/search/byMetadata")
                .param("query", ePerson.getFirstName())
                .param("page", "1")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.epersons", Matchers.everyItem(
                        hasJsonPath("$.type", is("eperson")))
                        ))
                .andExpect(jsonPath("$._embedded.epersons").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.number", is(1)))
                .andExpect(jsonPath("$.page.totalPages", is(3)))
                .andExpect(jsonPath("$.page.totalElements", is(5)));

        getClient(authToken).perform(get("/api/eperson/epersons/search/byMetadata")
                .param("query", ePerson.getFirstName())
                .param("page", "2")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.epersons", Matchers.everyItem(
                        hasJsonPath("$.type", is("eperson")))
                        ))
                .andExpect(jsonPath("$._embedded.epersons").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.number", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(3)))
                .andExpect(jsonPath("$.page.totalElements", is(5)));

        getClient(authToken).perform(get("/api/eperson/epersons/search/byMetadata")
                .param("query", ePerson.getFirstName())
                .param("page", "3")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.epersons").doesNotExist())
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.number", is(3)))
                .andExpect(jsonPath("$.page.totalPages", is(3)))
                .andExpect(jsonPath("$.page.totalElements", is(5)));

    }

    @Test
    public void validatePasswordRobustnessContainingAtLeastAnUpperCaseCharUnprocessableTest() throws Exception {
        configurationService.setProperty("authentication-password.regex-validation.pattern", "^(?=.*[A-Z])");

        ObjectMapper mapper = new ObjectMapper();

        String newRegisterEmail = "new-register@fake-email.com";
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(newRegisterEmail);

        getClient().perform(post("/api/eperson/registrations")
                   .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(mapper.writeValueAsBytes(registrationRest)))
                   .andExpect(status().isCreated());

        String newRegisterToken = registrationDataService.findByEmail(context, newRegisterEmail).getToken();

        EPersonRest ePersonRest = new EPersonRest();
        MetadataRest metadataRest = new MetadataRest();
        ePersonRest.setCanLogIn(true);
        MetadataValueRest surname = new MetadataValueRest();
        surname.setValue("Misha");
        metadataRest.put("eperson.lastname", surname);
        MetadataValueRest firstname = new MetadataValueRest();
        firstname.setValue("Boychuk");
        metadataRest.put("eperson.firstname", firstname);
        ePersonRest.setMetadata(metadataRest);
        ePersonRest.setPassword("lowercasepassword");

        mapper.setAnnotationIntrospector(new IgnoreJacksonWriteOnlyAccess());

        try {
            getClient().perform(post("/api/eperson/epersons")
                       .param("token", newRegisterToken)
                       .content(mapper.writeValueAsBytes(ePersonRest))
                       .contentType(MediaType.APPLICATION_JSON))
                       .andExpect(status().isUnprocessableEntity());

        } finally {
            context.turnOffAuthorisationSystem();
            registrationDataService.deleteByToken(context, newRegisterToken);
            context.restoreAuthSystemState();
        }
    }

    @Test
    public void validatePasswordRobustnessContainingAtLeastAnUpperCaseCharTest() throws Exception {
        configurationService.setProperty("authentication-password.regex-validation.pattern", "^(?=.*[A-Z])");

        ObjectMapper mapper = new ObjectMapper();

        String newRegisterEmail = "new-register@fake-email.com";
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(newRegisterEmail);

        getClient().perform(post("/api/eperson/registrations")
                   .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(mapper.writeValueAsBytes(registrationRest)))
                   .andExpect(status().isCreated());

        String newRegisterToken = registrationDataService.findByEmail(context, newRegisterEmail).getToken();

        EPersonRest ePersonRest = new EPersonRest();
        MetadataRest metadataRest = new MetadataRest();
        ePersonRest.setCanLogIn(true);
        MetadataValueRest surname = new MetadataValueRest();
        surname.setValue("Boychuk");
        metadataRest.put("eperson.lastname", surname);
        MetadataValueRest firstname = new MetadataValueRest();
        firstname.setValue("Misha");
        metadataRest.put("eperson.firstname", firstname);
        ePersonRest.setMetadata(metadataRest);
        ePersonRest.setPassword("Lowercasepassword");
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
                               hasJsonPath("$.type", is("eperson")),
                               hasJsonPath("$._links.self.href", not(empty())),
                               hasJsonPath("$.metadata", Matchers.allOf(
                                      matchMetadata("eperson.firstname", "Misha"),
                                      matchMetadata("eperson.lastname", "Boychuk"))))))
                 .andDo(result -> idRef.set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id"))));

            EPerson createdEPerson = ePersonService.find(context, UUID.fromString(String.valueOf(idRef.get())));
            assertTrue(ePersonService.checkPassword(context, createdEPerson, "Lowercasepassword"));
            assertNull(registrationDataService.findByToken(context, newRegisterToken));
        } finally {
            context.turnOffAuthorisationSystem();
            registrationDataService.deleteByToken(context, newRegisterToken);
            context.restoreAuthSystemState();
            EPersonBuilder.deleteEPerson(idRef.get());
        }
    }

    @Test
    public void validatePasswordRobustnessContainingAtLeastAnUppercaseCharPatchUnprocessableTest() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .withPassword("TestPassword")
                                        .build();

        context.restoreAuthSystemState();

        configurationService.setProperty("authentication-password.regex-validation.pattern", "^(?=.*[A-Z])");

        String newPassword = "newpassword";
        String patchBody = buildPasswordAddOperationPatchBody(newPassword, "TestPassword");

        String token = getAuthToken(admin.getEmail(), password);

        // updates password
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isUnprocessableEntity());

        // can't login with new password
        token = getAuthToken(ePerson.getEmail(), newPassword);
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));

        // login with origin password
        token = getAuthToken(ePerson.getEmail(), "TestPassword");
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void validatePasswordRobustnessContainingAtLeastAnUppercaseCharPatchTest() throws Exception {
        configurationService.setProperty("authentication-password.regex-validation.pattern", "^(?=.*[A-Z])");
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .withPassword("TestPassword")
                                        .build();

        context.restoreAuthSystemState();

        String newPassword = "Newpassword";
        String patchBody = buildPasswordAddOperationPatchBody(newPassword, "TestPassword");

        String token = getAuthToken(admin.getEmail(), password);

        // updates password
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        // login with new password
        token = getAuthToken(ePerson.getEmail(), newPassword);
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        // can't login with old password
        token = getAuthToken(ePerson.getEmail(), "TestPassword");
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void patchChangePassword() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("John", "Doe")
            .withEmail("Johndoe@example.com")
            .withPassword(password)
            .build();

        context.restoreAuthSystemState();

        String newPassword = "newpassword";
        String patchBody = buildPasswordAddOperationPatchBody(newPassword, password);

        String token = getAuthToken(admin.getEmail(), password);

        // updates password
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk());

        // login with new password
        token = getAuthToken(ePerson.getEmail(), newPassword);
        getClient(token).perform(get("/api/authn/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.okay", is(true)))
            .andExpect(jsonPath("$.authenticated", is(true)))
            .andExpect(jsonPath("$.type", is("status")));

        // can't login with old password
        token = getAuthToken(ePerson.getEmail(), password);
        getClient(token).perform(get("/api/authn/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.okay", is(true)))
            .andExpect(jsonPath("$.authenticated", is(false)))
            .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void patchChangePasswordWithWrongCurrentPassword() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("John", "Doe")
            .withEmail("Johndoe@example.com")
            .withPassword(password)
            .build();

        context.restoreAuthSystemState();

        String newPassword = "newpassword";
        String wrongPassword = "wrong_password";
        String patchBody = buildPasswordAddOperationPatchBody(newPassword, wrongPassword);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    public void patchChangePasswordWithNoCurrentPassword() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("John", "Doe")
            .withEmail("Johndoe@example.com")
            .withPassword(password)
            .build();

        context.restoreAuthSystemState();

        String newPassword = "newpassword";
        String patchBody = buildPasswordAddOperationPatchBody(newPassword, null);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isForbidden());
    }

    private String buildPasswordAddOperationPatchBody(String password, String currentPassword) {

        Map<String, String> value = new HashMap<>();
        if (password != null) {
            value.put("new_password", password);
        }
        if (currentPassword != null) {
            value.put("current_password", currentPassword);
        }

        return getPatchContent(List.of(new AddOperation("/password", value)));

    }

}
