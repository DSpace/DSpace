/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.matcher.EPersonMatcher;
import org.dspace.app.rest.matcher.HalMatcher;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.test.MetadataPatchSuite;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.hamcrest.Matchers;
import org.junit.Test;


public class EPersonRestRepositoryIT extends AbstractControllerIntegrationTest {


    @Test
    public void createTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // we should check how to get it from Spring
        ObjectMapper mapper = new ObjectMapper();
        EPersonRest data = new EPersonRest();
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

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(post("/api/eperson/epersons")
                                        .content(mapper.writeValueAsBytes(data))
                                        .contentType(contentType))
                            .andExpect(status().isCreated())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$", Matchers.allOf(
                               hasJsonPath("$.uuid", not(empty())),
                               // is it what you expect? EPerson.getName() returns the email...
                               //hasJsonPath("$.name", is("Doe John")),
                               hasJsonPath("$.email", is("createtest@fake-email.com")),
                               hasJsonPath("$.type", is("eperson")),
                               hasJsonPath("$.canLogIn", is(true)),
                               hasJsonPath("$.requireCertificate", is(false)),
                               hasJsonPath("$._links.self.href", not(empty())),
                               hasJsonPath("$.metadata", Matchers.allOf(
                                       matchMetadata("eperson.firstname", "John"),
                                       matchMetadata("eperson.lastname", "Doe")
                               )))));
        // TODO cleanup the context!!!
    }

    @Test
    public void findAllTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson newUser = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

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
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

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
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

        EPerson ePerson2 = EPersonBuilder.createEPerson(context)
                                         .withNameInMetadata("Jane", "Smith")
                                         .withEmail("janesmith@fake-email.com")
                                         .build();

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
    public void readEpersonAuthorizationTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson1 = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

        EPerson ePerson2 = EPersonBuilder.createEPerson(context)
                                         .withNameInMetadata("Jane", "Smith")
                                         .withEmail("janesmith@fake-email.com")
                                         .build();


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

        // Verify an unprivilegd user can access information about himself/herself
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
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

        EPerson testEPerson2 = EPersonBuilder.createEPerson(context)
                                         .withNameInMetadata("Jane", "Smith")
                                         .withEmail("janesmith@fake-email.com")
                                         .build();

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
        .andExpect(jsonPath("$._links.byName", Matchers.notNullValue()));
    }

    @Test
    public void findByEmail() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

        // create a second eperson to put the previous one in a no special position (is not the first as we have default
        // epersons is not the latest created)
        EPerson ePerson2 = EPersonBuilder.createEPerson(context)
                                         .withNameInMetadata("Jane", "Smith")
                                         .withEmail("janesmith@fake-email.com")
                                         .build();

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
    public void findByEmailUnprocessable() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/search/byEmail"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void findByName() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

        EPerson ePerson2 = EPersonBuilder.createEPerson(context)
                                         .withNameInMetadata("Jane", "Smith")
                                         .withEmail("janesmith@fake-email.com")
                                         .build();

        EPerson ePerson3 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Tom", "Doe")
                .withEmail("tomdoe@fake-email.com")
                .build();

        EPerson ePerson4 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Dirk", "Doe-Postfix")
                .withEmail("dirkdoepostfix@fake-email.com")
                .build();

        EPerson ePerson5 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Harry", "Prefix-Doe")
                .withEmail("harrydoeprefix@fake-email.com")
                .build();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/search/byName")
                    .param("q", ePerson.getLastName()))
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
        getClient(authToken).perform(get("/api/eperson/epersons/search/byName")
                .param("q", ePerson.getLastName().toLowerCase()))
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
    public void findByNameUndefined() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/search/byName")
                .param("q", "Doe, John"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(contentType))
        .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findByNameUnprocessable() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/search/byName"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteOne() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

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
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

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
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

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
    public void deleteViolatingConstraints() throws Exception {
        // We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Sample", "Submitter")
                .withEmail("submitter@fake-email.com")
                .build();

        // force the use of the new user for subsequent operation
        context.setCurrentUser(ePerson);

        // ** GIVEN **
        // 1. A community with a logo
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").withLogo("logo_community")
                                          .build();

        // 2. A collection with a logo
        Collection col = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection")
                                          .withLogo("logo_collection").build();


        // 3. Create an item that will prevent the deletation of the eperson account (it is the submitter)
        Item item = ItemBuilder.createItem(context, col).build();

        String token = getAuthToken(admin.getEmail(), password);

        // 422 error when trying to DELETE the eperson=submitter
        getClient(token).perform(delete("/api/eperson/epersons/" + ePerson.getID()))
                   .andExpect(status().is(422));

        // Verify the eperson is still here
        getClient(token).perform(get("/api/eperson/epersons/" + ePerson.getID()))
                .andExpect(status().isOk());
    }

    @Test
    public void patchByForbiddenUser() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

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
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

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
                                        .withEmail("Johndoe@fake-email.com")
                                        .withNetId("testId")
                                        .build();

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
                        .andExpect(jsonPath("$.email", Matchers.is("johndoe@fake-email.com")))
                        .andExpect(jsonPath("$.canLogIn", Matchers.is(false)));

    }

    @Test
    public void patchUsingStringForBoolean() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .withNetId("testId")
                                        .build();

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
                        .andExpect(jsonPath("$.email", Matchers.is("johndoe@fake-email.com")))
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
                        .andExpect(jsonPath("$.email", Matchers.is("johndoe@fake-email.com")))
                        .andExpect(jsonPath("$.canLogIn", Matchers.is(false)));

    }

    @Test
    public void replaceOnNonExistentValue() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

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
                        .andExpect(jsonPath("$.email", Matchers.is("johndoe@fake-email.com")))
                        .andExpect(jsonPath("$.netid", Matchers.nullValue()));

    }

    @Test
    public void patchNetIdMissingValue() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .withNetId("testId")
                                        .build();

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
                        .andExpect(jsonPath("$.email", Matchers.is("johndoe@fake-email.com")))
                        .andExpect(jsonPath("$.canLogIn", Matchers.is(false)));


    }

    @Test
    public void patchCanLogin() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

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
                        .andExpect(jsonPath("$.email", Matchers.is("johndoe@fake-email.com")))
                        .andExpect(jsonPath("$.netid", Matchers.nullValue()));


    }

    @Test
    public void patchCanLoginMissingValue() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

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
                        .andExpect(jsonPath("$.email", Matchers.is("johndoe@fake-email.com")))
                        .andExpect(jsonPath("$.requireCertificate", Matchers.is(false)));

    }

    @Test
    public void patchRequireCertificate() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

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
                        .andExpect(jsonPath("$.email", Matchers.is("johndoe@fake-email.com")))
                        .andExpect(jsonPath("$.netid", Matchers.nullValue()));

    }

    @Test
    public void patchRequireCertificateMissingValue() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

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
                        .andExpect(jsonPath("$.email", Matchers.is("johndoe@fake-email.com")))
                        .andExpect(jsonPath("$.canLogIn", Matchers.is(false)));


    }

    @Test
    public void patchPassword() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .withPassword(password)
                                        .build();

        String newPassword = "newpassword";

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/password", newPassword);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(admin.getEmail(), password);

        // updates password
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        // login with new password
        token = getAuthToken(ePerson.getEmail(), newPassword);
        getClient(token).perform(get("/api/"))
                        .andExpect(status().isOk());

    }

    @Test
    public void patchPasswordIsForbidden() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson1 = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .withPassword(password)
                                        .build();

        EPerson ePerson2 = EPersonBuilder.createEPerson(context)
                                          .withNameInMetadata("Jane", "Doe")
                                          .withEmail("Janedoe@fake-email.com")
                                          .withPassword(password)
                                          .build();

        String newPassword = "newpassword";

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/password", newPassword);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        // eperson one
        String token = getAuthToken(ePerson1.getEmail(), password);

        // should not be able to update the password of eperson two
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson2.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isForbidden());

        // login with old password
        token = getAuthToken(ePerson2.getEmail(), password);
        getClient(token).perform(get("/api/"))
                        .andExpect(status().isOk());

    }
    @Test
    public void patchPasswordForNonAdminUser() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .withPassword(password)
                                        .build();

        String newPassword = "newpassword";

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/password", newPassword);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(ePerson.getEmail(), password);

        // updates password
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        // login with new password
        token = getAuthToken(ePerson.getEmail(), newPassword);
        getClient(token).perform(get("/api/"))
                        .andExpect(status().isOk());

    }

    @Test
    public void patchPasswordReplaceOnNonExistentValue() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("John", "Doe")
                .withEmail("Johndoe@fake-email.com")
                .build();

        String newPassword = "newpassword";

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/password", newPassword);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(admin.getEmail(), password);

        // replace of password should fail
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void patchCanLoginNonAdminUser() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("CanLogin@fake-email.com")
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
                                        .withEmail("CanLogin@fake-email.com")
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

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .withPassword("testpass79bC")
                                        .build();

        String token = getAuthToken(admin.getEmail(), password);

        String newPassword = "newpass";

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/password", newPassword);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        // initialize passwd
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());


        List<Operation> ops2 = new ArrayList<Operation>();
        ReplaceOperation replaceOperation2 = new ReplaceOperation("/password", null);
        ops2.add(replaceOperation2);
        patchBody = getPatchContent(ops2);

        // should return bad request
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isBadRequest());

        // login with original password
        token = getAuthToken(ePerson.getEmail(), newPassword);
        getClient(token).perform(get("/api/"))
                        .andExpect(status().isOk());

    }

    @Test
    public void patchEmail() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .withPassword(password)
                                        .build();

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
        getClient(token).perform(get("/api/"))
                        .andExpect(status().isOk());

    }

    @Test
    public void patchEmailNonAdminUser() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
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
                                        .withEmail("Johndoe@fake-email.com")
                                        .withPassword(password)
                                        .build();

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

        // login with original password
        token = getAuthToken(ePerson.getEmail(), password);
        getClient(token).perform(get("/api/"))
                        .andExpect(status().isOk());

    }

    @Test
    public void patchMultipleOperationsWithSuccess() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .withNetId("testId")
                                        .build();

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
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

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
        EPerson ePerson = EPersonBuilder.createEPerson(context).withEmail("user@test.com").build();
        context.restoreAuthSystemState();
        String token = getAuthToken(asUser.getEmail(), password);

        new MetadataPatchSuite().runWith(getClient(token), "/api/eperson/epersons/" + ePerson.getID(), expectedStatus);
    }

}
