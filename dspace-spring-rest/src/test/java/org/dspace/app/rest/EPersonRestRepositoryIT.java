/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.matcher.EPersonMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;

public class EPersonRestRepositoryIT extends AbstractControllerIntegrationTest{

    EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    @Test
    @Ignore
    public void findAllTest() throws Exception{
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("John", "Doe")
                .withEmail("Johndoe@gmail.com")
                .build();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/eperson/eperson"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.epersons", Matchers.containsInAnyOrder(
                        EPersonMatcher.matchEPersonEntry(ePerson),
                        EPersonMatcher.matchDefaultTestEPerson()
                )))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(2)))
        ;

        getClient().perform(get("/api/eperson/eperson"))
                .andExpect(status().isForbidden())
        ;
    }

    @Test
    @Ignore
    public void findAllPaginationTest() throws Exception{
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("John", "Doe")
                .withEmail("Johndoe@gmail.com")
                .build();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/eperson/epersons")
                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.epersons", Matchers.contains(
                        EPersonMatcher.matchDefaultTestEPerson()
                )))
                .andExpect(jsonPath("$._embedded.epersons", Matchers.not(
                        Matchers.contains(
                                EPersonMatcher.matchEPersonEntry(admin)
                        )
                )))
                .andExpect(jsonPath("$.page.size", is(1)))
                .andExpect(jsonPath("$.page.totalElements", is(3)))
        ;


        getClient(token).perform(get("/api/eperson/epersons")
                .param("size", "1")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.epersons", Matchers.contains(
                        EPersonMatcher.matchEPersonEntry(admin)
                )))
                .andExpect(jsonPath("$.page.size", is(1)))
                .andExpect(jsonPath("$.page.totalElements", is(3)))
        ;

        getClient().perform(get("/api/eperson/epersons"))
                .andExpect(status().isForbidden())
        ;
    }


    @Test
    @Ignore
    public void findOneTest() throws Exception{
        context.turnOffAuthorisationSystem();

        ePersonService.setPassword(eperson, password);

        //Only works with admin
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/eperson/epersons/" + admin.getID()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", is(
                        EPersonMatcher.matchEPersonEntry(admin)
                )));

    }

    @Test
    @Ignore
    public void findOneRelsTest() throws Exception{
        context.turnOffAuthorisationSystem();


        EPerson ePerson2 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Jane", "Smith")
                .withEmail("janesmith@gmail.com")
                .build();

        //Admin can access everyone
        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/eperson/epersons/" + ePerson2.getID()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", is(
                        EPersonMatcher.matchEPersonEntry(ePerson2)
                )))
                .andExpect(jsonPath("$", Matchers.not(
                        is(
                                EPersonMatcher.matchEPersonEntry(eperson)
                        )
                )))
                .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/eperson/epersons/" + ePerson2.getID())));


        //EPerson can only access himself
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get("/api/eperson/epersons/" + eperson.getID()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", is(
                        EPersonMatcher.matchEPersonEntry(ePerson2)
                )))
                .andExpect(jsonPath("$", Matchers.not(
                        is(
                                EPersonMatcher.matchEPersonEntry(eperson)
                        )
                )))
                .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/eperson/epersons/" + ePerson2.getID())));


    }


    @Test
    public void findOneTestWrongUUID() throws Exception{
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("John", "Doe")
                .withEmail("Johndoe@gmail.com")
                .build();

        EPerson ePerson2 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Jane", "Smith")
                .withEmail("janesmith@gmail.com")
                .build();

        getClient().perform(get("/api/eperson/epersons/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());

    }
}
