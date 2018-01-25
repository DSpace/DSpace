/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.matcher.EPersonMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.eperson.EPerson;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EPersonRestRepositoryIT extends AbstractControllerIntegrationTest{

    @Test
    public void findAllTest() throws Exception{
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("John", "Doe")
                .withEmail("Johndoe@gmail.com")
                .build();

        getClient().perform(get("/api/eperson/eperson"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.epersons", Matchers.containsInAnyOrder(
                        EPersonMatcher.matchEPersonEntry(ePerson),
                        EPersonMatcher.matchDefaultTestEPerson()
                )))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(2)))
        ;
    }

    @Test
    public void findAllPaginationTest() throws Exception{
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("John", "Doe")
                .withEmail("Johndoe@gmail.com")
                .build();

        getClient().perform(get("/api/eperson/eperson")
                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.epersons", Matchers.contains(
                        EPersonMatcher.matchDefaultTestEPerson()
                )))
                .andExpect(jsonPath("$._embedded.epersons", Matchers.not(
                        Matchers.contains(
                                EPersonMatcher.matchEPersonEntry(ePerson)
                        )
                )))
                .andExpect(jsonPath("$.page.size", is(1)))
                .andExpect(jsonPath("$.page.totalElements", is(2)))
        ;

        getClient().perform(get("/api/eperson/eperson")
                .param("size", "1")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.epersons", Matchers.contains(
                        EPersonMatcher.matchEPersonEntry(ePerson)
                )))
                .andExpect(jsonPath("$.page.size", is(1)))
                .andExpect(jsonPath("$.page.totalElements", is(2)))
        ;
    }


    @Test
    public void findOneTest() throws Exception{
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("John", "Doe")
                .withEmail("Johndoe@gmail.com")
                .build();

        EPerson ePerson2 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Jane", "Smith")
                .withEmail("janesmith@gmail.com")
                .build();

        getClient().perform(get("/api/eperson/epersons/" + ePerson2.getID()))
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
    public void findOneRelsTest() throws Exception{
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("John", "Doe")
                .withEmail("Johndoe@gmail.com")
                .build();

        EPerson ePerson2 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Jane", "Smith")
                .withEmail("janesmith@gmail.com")
                .build();

        getClient().perform(get("/api/eperson/epersons/" + ePerson2.getID()))
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
