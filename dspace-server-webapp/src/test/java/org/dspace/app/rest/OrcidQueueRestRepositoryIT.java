/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.orcid.OrcidQueue;
import org.dspace.app.rest.matcher.OrcidQueueMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.OrcidQueueBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test class for the orcid queue endpoint
 *
 * @author Mykhaylo Boychuk - 4Science
 */
public class OrcidQueueRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Test
    public void findByOwnerTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Person")
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Publication")
                                           .withName("Collection 2").build();

        Collection col3 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Project")
                                           .withName("Collection 3").build();

        Item itemPerson = ItemBuilder.createItem(context, col1)
                                     .withPersonIdentifierFirstName("Josiah")
                                     .withPersonIdentifierLastName("Carberry")
                                     .build();

        itemService.addMetadata(context, itemPerson, "person", "identifier", "orcid", "en", "0000-0002-1825-0097");


        Item itemPublication = ItemBuilder.createItem(context, col2)
                                          .withAuthor("Josiah, Carberry")
                                          .withTitle("A Methodology for the Emulation of Architecture")
                                          .withIssueDate("2013-08-03")
                                          .build();

        Item itemPublication2 = ItemBuilder.createItem(context, col2)
                                           .withAuthor("Josiah, Carberry")
                                           .withTitle("The Impact of Interactive Epistemologies on Cryptography")
                                           .withIssueDate("2013-02-17")
                                           .build();

        Item itemProject = ItemBuilder.createItem(context, col3)
                                      .withTitle("Title Project")
                                      .build();

        itemService.addMetadata(context, itemPerson, "crisevent", "description", "keywords", null, "psychoceramics");
        itemService.addMetadata(context, itemPerson, "dc", "identifier", "scopus", null, "7004769520");

        itemService.addMetadata(context, itemPublication, "cris", "owner", null, null, itemPerson.getID().toString());
        itemService.addMetadata(context, itemPublication2, "cris", "owner", null, null, itemPerson.getID().toString());
        itemService.addMetadata(context, itemProject, "cris", "owner", null, null, itemPerson.getID().toString());

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, itemPerson, itemPublication).build();
        OrcidQueue orcidQueue2 = OrcidQueueBuilder.createOrcidQueue(context, itemPerson, itemPublication2).build();
        OrcidQueue orcidQueue3 = OrcidQueueBuilder.createOrcidQueue(context, itemPerson, itemProject).build();

        context.restoreAuthSystemState();
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenAdmin).perform(get("/api/cris/orcidQueue/search/findByOwner")
                             .param("ownerId", itemPerson.getID().toString()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._embedded.orcidQueues", Matchers.containsInAnyOrder(
                                     OrcidQueueMatcher.matchOrcidQueue(orcidQueue, "Publication"),
                                     OrcidQueueMatcher.matchOrcidQueue(orcidQueue2, "Publication"),
                                     OrcidQueueMatcher.matchOrcidQueue(orcidQueue3, "Project")
                                     )))
                             .andExpect(jsonPath("$.page.totalElements", is(3)));
    }

    @Test
    public void findByOwnerTwoResercherTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Person")
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Publication")
                                           .withName("Collection 2").build();

        Item itemPerson1 = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("Josiah")
                                      .withPersonIdentifierLastName("Carberry")
                                      .build();

        Item itemPerson2 = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("Laura")
                                      .withPersonIdentifierLastName("Shulz")
                                      .build();

        itemService.addMetadata(context, itemPerson1, "person", "identifier", "orcid", "en", "0000-0002-1825-0097");
        itemService.addMetadata(context, itemPerson2, "person", "identifier", "orcid", "en", "0000-0002-1825-0033");


        Item itemPublication = ItemBuilder.createItem(context, col2)
                                          .withAuthor("Josiah, Carberry")
                                          .withTitle("A Methodology for the Emulation of Architecture")
                                          .withIssueDate("2013-08-03").build();

        Item itemPublication2 = ItemBuilder.createItem(context, col2)
                                           .withAuthor("Laura, Shulz")
                                           .withTitle("Bulk and surface plasmons in artificially structured materials")
                                           .withIssueDate("2015-05-21").build();

        Item itemPublication3 = ItemBuilder.createItem(context, col2)
                                           .withAuthor("Laura, Shulz")
                                           .withAuthor("Josiah, Carberry")
                                           .withTitle("Developing Thin Clients Using Amphibious Epistemologies")
                                           .withIssueDate("2012-11-21").build();

        itemService.addMetadata(context, itemPublication, "cris", "owner", null, null, itemPerson1.getID().toString());
        itemService.addMetadata(context, itemPublication2, "cris", "owner", null, null, itemPerson2.getID().toString());
        itemService.addMetadata(context, itemPublication3, "cris", "owner", null, null, itemPerson1.getID().toString());
        itemService.addMetadata(context, itemPublication3, "cris", "owner", null, null, itemPerson2.getID().toString());

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, itemPerson1, itemPublication).build();
        OrcidQueue orcidQueue2 = OrcidQueueBuilder.createOrcidQueue(context, itemPerson2, itemPublication2).build();
        OrcidQueue orcidQueue3 = OrcidQueueBuilder.createOrcidQueue(context, itemPerson1, itemPublication3).build();
        OrcidQueue orcidQueue4 = OrcidQueueBuilder.createOrcidQueue(context, itemPerson2, itemPublication3).build();

        context.restoreAuthSystemState();
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenAdmin).perform(get("/api/cris/orcidQueue/search/findByOwner")
                             .param("ownerId", itemPerson1.getID().toString()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._embedded.orcidQueues", Matchers.containsInAnyOrder(
                                        OrcidQueueMatcher.matchOrcidQueue(orcidQueue, "Publication"),
                                        OrcidQueueMatcher.matchOrcidQueue(orcidQueue3, "Publication")
                                        )))
                             .andExpect(jsonPath("$._embedded.orcidQueues", Matchers.not(Matchers.containsInAnyOrder(
                                        OrcidQueueMatcher.matchOrcidQueue(orcidQueue2, "Publication"),
                                        OrcidQueueMatcher.matchOrcidQueue(orcidQueue4, "Publication")
                                        ))))
                             .andExpect(jsonPath("$.page.totalElements", is(2)));

        getClient(tokenAdmin).perform(get("/api/cris/orcidQueue/search/findByOwner")
                             .param("ownerId", itemPerson2.getID().toString()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._embedded.orcidQueues", Matchers.containsInAnyOrder(
                                     OrcidQueueMatcher.matchOrcidQueue(orcidQueue2, "Publication"),
                                     OrcidQueueMatcher.matchOrcidQueue(orcidQueue4, "Publication")
                                     )))
                             .andExpect(jsonPath("$._embedded.orcidQueues", Matchers.not(Matchers.containsInAnyOrder(
                                     OrcidQueueMatcher.matchOrcidQueue(orcidQueue, "Publication"),
                                     OrcidQueueMatcher.matchOrcidQueue(orcidQueue3, "Publication")
                                     ))))
                             .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Person")
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Publication")
                                           .withName("Collection 2").build();

        Item itemPerson1 = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("Josiah")
                                      .withPersonIdentifierLastName("Carberry")
                                      .build();

        Item itemPerson2 = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("Laura")
                                      .withPersonIdentifierLastName("Shulz")
                                      .build();

        itemService.addMetadata(context, itemPerson1, "person", "identifier", "orcid", "en", "0000-0002-1825-0097");
        itemService.addMetadata(context, itemPerson2, "person", "identifier", "orcid", "en", "0000-0002-1825-0033");


        Item itemPublication = ItemBuilder.createItem(context, col2)
                                          .withAuthor("Josiah, Carberry")
                                          .withTitle("A Methodology for the Emulation of Architecture")
                                          .withIssueDate("2013-08-03").build();

        Item itemPublication2 = ItemBuilder.createItem(context, col2)
                                           .withAuthor("Laura, Shulz")
                                           .withTitle("Bulk and surface plasmons in artificially structured materials")
                                           .withIssueDate("2015-05-21").build();

        itemService.addMetadata(context, itemPublication, "cris", "owner", null, null, itemPerson1.getID().toString());
        itemService.addMetadata(context, itemPublication2, "cris", "owner", null, null, itemPerson2.getID().toString());

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, itemPerson1, itemPublication).build();
        OrcidQueue orcidQueue2 = OrcidQueueBuilder.createOrcidQueue(context, itemPerson2, itemPublication2).build();

        context.restoreAuthSystemState();
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenAdmin).perform(get("/api/cris/orcidQueue/" + orcidQueue.getID().toString()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", is(
                                        OrcidQueueMatcher.matchOrcidQueue(orcidQueue, "Publication"))))
                             .andExpect(jsonPath("$._links.self.href", Matchers
                                       .containsString("/api/cris/orcidQueue/" + orcidQueue.getID())));

        getClient(tokenAdmin).perform(get("/api/cris/orcidQueue/" + orcidQueue2.getID().toString()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", is(
                                        OrcidQueueMatcher.matchOrcidQueue(orcidQueue2, "Publication"))))
                             .andExpect(jsonPath("$._links.self.href", Matchers
                                       .containsString("/api/cris/orcidQueue/" + orcidQueue2.getID())));
    }

    @Test
    public void deleteOneTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Person")
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Publication")
                                           .withName("Collection 2").build();

        Item itemPerson1 = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("Josiah")
                                      .withPersonIdentifierLastName("Carberry")
                                      .build();

        itemService.addMetadata(context, itemPerson1, "person", "identifier", "orcid", "en", "0000-0002-1825-0097");

        Item itemPublication = ItemBuilder.createItem(context, col2)
                                          .withAuthor("Josiah, Carberry")
                                          .withTitle("A Methodology for the Emulation of Architecture")
                                          .withIssueDate("2013-08-03").build();

        itemService.addMetadata(context, itemPublication, "cris", "owner", null, null, itemPerson1.getID().toString());

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, itemPerson1, itemPublication).build();

        context.restoreAuthSystemState();
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenAdmin).perform(delete("/api/cris/orcidQueue/" + orcidQueue.getID().toString()))
                             .andExpect(status().isNoContent());

        getClient(tokenAdmin).perform(get("/api/cris/orcidQueue/" + orcidQueue.getID().toString()))
                             .andExpect(status().isNotFound());
    }
}
