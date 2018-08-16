/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.matcher.EPersonMatcher;
import org.dspace.app.rest.matcher.EPersonMetadataMatcher;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.MetadataEntryRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;



public class EPersonRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void createTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // we should check how to get it from Spring
        ObjectMapper mapper = new ObjectMapper();
        EPersonRest data = new EPersonRest();
        data.setEmail("createtest@fake-email.com");
        data.setCanLogIn(true);
        MetadataEntryRest surname = new MetadataEntryRest();
        surname.setKey("eperson.lastname");
        surname.setValue("Doe");
        MetadataEntryRest firstname = new MetadataEntryRest();
        firstname.setKey("eperson.firstname");
        firstname.setValue("John");
        data.setMetadata(Arrays.asList(surname, firstname));

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
                               hasJsonPath("$.metadata", Matchers.containsInAnyOrder(
                                   EPersonMetadataMatcher.matchFirstName("John"),
                                   EPersonMetadataMatcher.matchLastName("Doe")
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
                       EPersonMatcher.matchDefaultTestEPerson(),
                       EPersonMatcher.matchDefaultTestEPerson()
                   )))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(3)))
        ;
    }

    @Test
    public void findAllUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson newUser = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

        getClient().perform(get("/api/eperson/eperson"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findAllForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson newUser = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/eperson"))
                            .andExpect(status().isForbidden());
    }

    @Test
    public void findAllPaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

        String authToken = getAuthToken(admin.getEmail(), password);
        // using size = 2 the first page will contains our test user and admin
        getClient(authToken).perform(get("/api/eperson/eperson")
                                .param("size", "2"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.epersons", Matchers.containsInAnyOrder(
                           EPersonMatcher.matchDefaultTestEPerson(),
                           EPersonMatcher.matchDefaultTestEPerson()
                   )))
                   .andExpect(jsonPath("$._embedded.epersons", Matchers.not(
                       Matchers.contains(
                           EPersonMatcher.matchEPersonEntry(ePerson)
                       )
                   )))
                   .andExpect(jsonPath("$.page.size", is(2)))
                   .andExpect(jsonPath("$.page.totalElements", is(3)))
        ;

        // using size = 2 the first page will contains our test user and admin
        getClient(authToken).perform(get("/api/eperson/eperson")
                                .param("size", "2")
                                .param("page", "1"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.epersons", Matchers.contains(
                       EPersonMatcher.matchEPersonEntry(ePerson)
                   )))
                   .andExpect(jsonPath("$.page.size", is(2)))
                   .andExpect(jsonPath("$.page.totalElements", is(3)))
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

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/" + ePerson2.getID()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", is(
                       EPersonMatcher.matchEPersonEntry(ePerson2)
                   )))
                   .andExpect(jsonPath("$", Matchers.not(
                       is(
                           EPersonMatcher.matchEPersonEntry(ePerson)
                       )
                   )));

    }

    @Test
    public void findOneRelsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

        EPerson ePerson2 = EPersonBuilder.createEPerson(context)
                                         .withNameInMetadata("Jane", "Smith")
                                         .withEmail("janesmith@fake-email.com")
                                         .build();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/" + ePerson2.getID()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", is(
                       EPersonMatcher.matchEPersonEntry(ePerson2)
                   )))
                   .andExpect(jsonPath("$", Matchers.not(
                       is(
                           EPersonMatcher.matchEPersonEntry(ePerson)
                       )
                   )))
                   .andExpect(jsonPath("$._links.self.href",
                                       Matchers.containsString("/api/eperson/epersons/" + ePerson2.getID())));
    }


    @Test
    public void findOneTestWrongUUID() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@fake-email.com")
                                        .build();

        EPerson ePerson2 = EPersonBuilder.createEPerson(context)
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
                .andExpect(status().isNotFound());
    }

    @Test
    public void findByEmailUnprocessable() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/search/byEmail"))
                .andExpect(status().isUnprocessableEntity());
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
                .andExpect(status().isUnprocessableEntity());
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

    @Ignore
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

}
