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
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.OrcidQueueBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.eperson.EPerson;
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
    public void findAllTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/cris/orcidqueues"))
                            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void findByOwnerTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson researcher = EPersonBuilder.createEPerson(context)
                                           .withNameInMetadata("Josiah", "Carberry")
                                           .withEmail("josiah.Carberry@example.com")
                                           .withPassword(password).build();

        EPerson researcher2 = EPersonBuilder.createEPerson(context)
                                           .withNameInMetadata("Laura", "Shulz")
                                           .withEmail("laura.shulz@example.com")
                                           .withPassword(password).build();

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
                                     .withCrisOwner(researcher.getFullName(), researcher.getID().toString())
                                     .build();

        Item itemPerson2 = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("Laura")
                                      .withPersonIdentifierLastName("Shulz")
                                      .withCrisOwner(researcher2.getFullName(), researcher2.getID().toString())
                                      .build();

        itemService.addMetadata(context, itemPerson, "person", "identifier", "orcid", "en", "0000-0002-1825-0097");
        itemService.addMetadata(context, itemPerson2, "person", "identifier", "orcid", "en", "0000-0002-1826-4497");


        Item itemPublication = ItemBuilder.createItem(context, col2)
                                          .withAuthor("Josiah, Carberry")
                                          .withTitle("A Methodology for the Emulation of Architecture")
                                          .withIssueDate("2013-08-03")
                                          .build();

        Item itemPublication2 = ItemBuilder.createItem(context, col2)
                                           .withAuthor("Laura, Shulz")
                                           .withTitle("The Impact of Interactive Epistemologies on Cryptography")
                                           .withIssueDate("2013-02-17")
                                           .build();

        Item itemProject = ItemBuilder.createItem(context, col3)
                                      .withTitle("Title Project")
                                      .build();

        itemService.addMetadata(context, itemPerson, "crisevent", "description", "keywords", null, "psychoceramics");
        itemService.addMetadata(context, itemPerson, "dc", "identifier", "scopus", null, "7004769520");

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, itemPerson, itemPublication).build();
        OrcidQueue orcidQueue2 = OrcidQueueBuilder.createOrcidQueue(context, itemPerson2, itemPublication2).build();
        OrcidQueue orcidQueue3 = OrcidQueueBuilder.createOrcidQueue(context, itemPerson, itemProject).build();

        context.restoreAuthSystemState();
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenResearcher = getAuthToken(researcher.getEmail(), password);
        String tokenResearcher2 = getAuthToken(researcher2.getEmail(), password);

        getClient(tokenResearcher).perform(get("/api/cris/orcidqueue/search/findByOwner")
                                  .param("ownerId", itemPerson.getID().toString()))
                                  .andExpect(status().isOk())
                                  .andExpect(jsonPath("$._embedded.orcidqueues", Matchers.containsInAnyOrder(
                                             OrcidQueueMatcher.matchOrcidQueue(orcidQueue, "Publication"),
                                             OrcidQueueMatcher.matchOrcidQueue(orcidQueue3, "Project")
                                             )))
                                  .andExpect(jsonPath("$.page.totalElements", is(2)));

        getClient(tokenResearcher2).perform(get("/api/cris/orcidqueue/search/findByOwner")
                                   .param("ownerId", itemPerson2.getID().toString()))
                                   .andExpect(status().isOk())
                                   .andExpect(jsonPath("$._embedded.orcidqueues", Matchers.contains(
                                              OrcidQueueMatcher.matchOrcidQueue(orcidQueue2, "Publication")
                                              )))
                                   .andExpect(jsonPath("$.page.totalElements", is(1)))
                                   .andExpect(jsonPath("$._links.self.href", Matchers
                                             .containsString("/api/cris/orcidqueue/search/findByOwner")));

        getClient(tokenAdmin).perform(get("/api/cris/orcidqueue/search/findByOwner")
                             .param("ownerId", itemPerson.getID().toString()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._embedded.orcidqueues", Matchers.containsInAnyOrder(
                                        OrcidQueueMatcher.matchOrcidQueue(orcidQueue, "Publication"),
                                        OrcidQueueMatcher.matchOrcidQueue(orcidQueue3, "Project")
                                        )))
                             .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    public void findByOwnerForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson researcher = EPersonBuilder.createEPerson(context)
                                           .withNameInMetadata("Josiah", "Carberry")
                                           .withEmail("josiah.Carberry@example.com")
                                           .withPassword(password).build();

        EPerson researcher2 = EPersonBuilder.createEPerson(context)
                                           .withNameInMetadata("Laura", "Shulz")
                                           .withEmail("laura.shulz@example.com")
                                           .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Person")
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Publication")
                                           .withName("Collection 2").build();

        Item itemPerson = ItemBuilder.createItem(context, col1)
                                     .withPersonIdentifierFirstName("Josiah")
                                     .withPersonIdentifierLastName("Carberry")
                                     .withCrisOwner(researcher.getFullName(), researcher.getID().toString())
                                     .build();

        Item itemPerson2 = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("Laura")
                                      .withPersonIdentifierLastName("Shulz")
                                      .withCrisOwner(researcher2.getFullName(), researcher2.getID().toString())
                                      .build();

        itemService.addMetadata(context, itemPerson, "person", "identifier", "orcid", "en", "0000-0002-1825-0097");
        itemService.addMetadata(context, itemPerson2, "person", "identifier", "orcid", "en", "0000-0002-1826-4497");


        Item itemPublication = ItemBuilder.createItem(context, col2)
                                          .withAuthor("Josiah, Carberry")
                                          .withTitle("A Methodology for the Emulation of Architecture")
                                          .withIssueDate("2013-08-03")
                                          .build();

        Item itemPublication2 = ItemBuilder.createItem(context, col2)
                                           .withAuthor("Laura, Shulz")
                                           .withTitle("The Impact of Interactive Epistemologies on Cryptography")
                                           .withIssueDate("2013-02-17")
                                           .build();

        itemService.addMetadata(context, itemPerson, "crisevent", "description", "keywords", null, "psychoceramics");
        itemService.addMetadata(context, itemPerson, "dc", "identifier", "scopus", null, "7004769520");

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, itemPerson, itemPublication).build();
        OrcidQueue orcidQueue2 = OrcidQueueBuilder.createOrcidQueue(context, itemPerson2, itemPublication2).build();

        context.restoreAuthSystemState();
        String tokenResearcher2 = getAuthToken(researcher2.getEmail(), password);

        getClient(tokenResearcher2).perform(get("/api/cris/orcidqueue/search/findByOwner")
                                   .param("ownerId", itemPerson.getID().toString()))
                                   .andExpect(status().isForbidden());
    }

    @Test
    public void findByOwnerUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson researcher = EPersonBuilder.createEPerson(context)
                                           .withNameInMetadata("Josiah", "Carberry")
                                           .withEmail("josiah.Carberry@example.com")
                                           .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Person")
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Publication")
                                           .withName("Collection 2").build();

        Item itemPerson = ItemBuilder.createItem(context, col1)
                                     .withPersonIdentifierFirstName("Josiah")
                                     .withPersonIdentifierLastName("Carberry")
                                     .withCrisOwner(researcher.getFullName(), researcher.getID().toString())
                                     .build();

        itemService.addMetadata(context, itemPerson, "person", "identifier", "orcid", "en", "0000-0002-1825-0097");

        Item itemPublication = ItemBuilder.createItem(context, col2)
                                          .withAuthor("Josiah, Carberry")
                                          .withTitle("A Methodology for the Emulation of Architecture")
                                          .withIssueDate("2013-08-03")
                                          .build();

        itemService.addMetadata(context, itemPerson, "crisevent", "description", "keywords", null, "psychoceramics");
        itemService.addMetadata(context, itemPerson, "dc", "identifier", "scopus", null, "7004769520");

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, itemPerson, itemPublication).build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/cris/orcidqueue/search/findByOwner")
                   .param("ownerId", itemPerson.getID().toString()))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson researcher = EPersonBuilder.createEPerson(context)
                                           .withNameInMetadata("Josiah", "Carberry")
                                           .withEmail("josiah.Carberry@example.com")
                                           .withPassword(password).build();

        EPerson researcher2 = EPersonBuilder.createEPerson(context)
                                            .withNameInMetadata("Laura", "Shulz")
                                            .withEmail("Laura.Shulz@example.com")
                                            .withPassword(password).build();

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
                                      .withCrisOwner(researcher.getFullName(), researcher.getID().toString())
                                      .build();

        Item itemPerson2 = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("Laura")
                                      .withPersonIdentifierLastName("Shulz")
                                      .withCrisOwner(researcher2.getFullName(), researcher2.getID().toString())
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

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, itemPerson1, itemPublication).build();
        OrcidQueue orcidQueue2 = OrcidQueueBuilder.createOrcidQueue(context, itemPerson2, itemPublication2).build();

        context.restoreAuthSystemState();
        String tokenResearcher = getAuthToken(researcher.getEmail(), password);
        String tokenResearcher2 = getAuthToken(researcher2.getEmail(), password);

        getClient(tokenResearcher).perform(get("/api/cris/orcidqueues/" + orcidQueue.getID().toString()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", is(
                                        OrcidQueueMatcher.matchOrcidQueue(orcidQueue, "Publication"))))
                             .andExpect(jsonPath("$._links.self.href", Matchers
                                       .containsString("/api/cris/orcidqueues/" + orcidQueue.getID())))
                             .andExpect(jsonPath("$._links.owner.href", Matchers
                                       .containsString("/api/core/items/" + itemPerson1.getID())))
                             .andExpect(jsonPath("$._links.entity.href", Matchers
                                       .containsString("/api/core/items/" + itemPublication.getID())));

        getClient(tokenResearcher2).perform(get("/api/cris/orcidqueues/" + orcidQueue2.getID().toString()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", is(
                                        OrcidQueueMatcher.matchOrcidQueue(orcidQueue2, "Publication"))))
                             .andExpect(jsonPath("$._links.self.href", Matchers
                                       .containsString("/api/cris/orcidqueues/" + orcidQueue2.getID())))
                             .andExpect(jsonPath("$._links.owner.href", Matchers
                                       .containsString("/api/core/items/" + itemPerson2.getID())))
                             .andExpect(jsonPath("$._links.entity.href", Matchers
                                       .containsString("/api/core/items/" + itemPublication2.getID())));
    }

    @Test
    public void findOneForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson researcher = EPersonBuilder.createEPerson(context)
                                           .withNameInMetadata("Josiah", "Carberry")
                                           .withEmail("josiah.Carberry@example.com")
                                           .withPassword(password).build();

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
                                      .withCrisOwner(researcher.getFullName(), researcher.getID().toString())
                                      .build();

        itemService.addMetadata(context, itemPerson1, "person", "identifier", "orcid", "en", "0000-0002-1825-0097");

        Item itemPublication = ItemBuilder.createItem(context, col2)
                                          .withAuthor("Josiah, Carberry")
                                          .withTitle("A Methodology for the Emulation of Architecture")
                                          .withIssueDate("2013-08-03").build();

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, itemPerson1, itemPublication).build();

        context.restoreAuthSystemState();
        String tokenEperson = getAuthToken(eperson.getEmail(), password);

        getClient(tokenEperson).perform(get("/api/cris/orcidqueues/" + orcidQueue.getID().toString()))
                               .andExpect(status().isForbidden());
    }

    @Test
    public void findOneUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson researcher = EPersonBuilder.createEPerson(context)
                                           .withNameInMetadata("Josiah", "Carberry")
                                           .withEmail("josiah.Carberry@example.com")
                                           .withPassword(password).build();

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
                                      .withCrisOwner(researcher.getFullName(), researcher.getID().toString())
                                      .build();

        itemService.addMetadata(context, itemPerson1, "person", "identifier", "orcid", "en", "0000-0002-1825-0097");

        Item itemPublication = ItemBuilder.createItem(context, col2)
                                          .withAuthor("Josiah, Carberry")
                                          .withTitle("A Methodology for the Emulation of Architecture")
                                          .withIssueDate("2013-08-03").build();

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, itemPerson1, itemPublication).build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/cris/orcidqueue/" + orcidQueue.getID().toString()))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneNotFoundTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/cris/orcidQueues/" + Integer.MAX_VALUE))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void deleteOneTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson researcher = EPersonBuilder.createEPerson(context)
                                           .withNameInMetadata("Josiah", "Carberry")
                                           .withEmail("josiah.Carberry@example.com")
                                           .withPassword(password).build();

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
                                      .withCrisOwner(researcher.getFullName(), researcher.getID().toString())
                                      .build();

        itemService.addMetadata(context, itemPerson1, "person", "identifier", "orcid", "en", "0000-0002-1825-0097");

        Item itemPublication = ItemBuilder.createItem(context, col2)
                                          .withAuthor("Josiah, Carberry")
                                          .withTitle("A Methodology for the Emulation of Architecture")
                                          .withIssueDate("2013-08-03").build();

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, itemPerson1, itemPublication).build();

        context.restoreAuthSystemState();
        String tokenResearcher = getAuthToken(researcher.getEmail(), password);

        getClient(tokenResearcher).perform(delete("/api/cris/orcidqueues/" + orcidQueue.getID()))
                                  .andExpect(status().isNoContent());

        getClient(tokenResearcher).perform(get("/api/cris/orcidqueues/" + orcidQueue.getID()))
                                  .andExpect(status().isNotFound());
    }

    @Test
    public void deleteOneForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson researcher = EPersonBuilder.createEPerson(context)
                                           .withNameInMetadata("Josiah", "Carberry")
                                           .withEmail("josiah.Carberry@example.com")
                                           .withPassword(password).build();

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
                                      .withCrisOwner(researcher.getFullName(), researcher.getID().toString())
                                      .build();

        itemService.addMetadata(context, itemPerson1, "person", "identifier", "orcid", "en", "0000-0002-1825-0097");

        Item itemPublication = ItemBuilder.createItem(context, col2)
                                          .withAuthor("Josiah, Carberry")
                                          .withTitle("A Methodology for the Emulation of Architecture")
                                          .withIssueDate("2013-08-03").build();

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, itemPerson1, itemPublication).build();

        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        String tokenResearcher = getAuthToken(researcher.getEmail(), password);

        getClient(tokenEPerson).perform(delete("/api/cris/orcidqueues/" + orcidQueue.getID()))
                               .andExpect(status().isForbidden());

        getClient(tokenResearcher).perform(get("/api/cris/orcidqueues/" + orcidQueue.getID()))
                                  .andExpect(status().isOk())
                                  .andExpect(jsonPath("$", is(
                                          OrcidQueueMatcher.matchOrcidQueue(orcidQueue, "Publication"))));
    }

    @Test
    public void deleteOneUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson researcher = EPersonBuilder.createEPerson(context)
                                           .withNameInMetadata("Josiah", "Carberry")
                                           .withEmail("josiah.Carberry@example.com")
                                           .withPassword(password).build();

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
                                      .withCrisOwner(researcher.getFullName(), researcher.getID().toString())
                                      .build();

        itemService.addMetadata(context, itemPerson1, "person", "identifier", "orcid", "en", "0000-0002-1825-0097");

        Item itemPublication = ItemBuilder.createItem(context, col2)
                                          .withAuthor("Josiah, Carberry")
                                          .withTitle("A Methodology for the Emulation of Architecture")
                                          .withIssueDate("2013-08-03").build();

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, itemPerson1, itemPublication).build();

        context.restoreAuthSystemState();
        String tokenResearcher = getAuthToken(researcher.getEmail(), password);

        getClient().perform(delete("/api/cris/orcidqueues/" + orcidQueue.getID()))
                   .andExpect(status().isUnauthorized());

        getClient(tokenResearcher).perform(get("/api/cris/orcidqueues/" + orcidQueue.getID()))
                                  .andExpect(status().isOk())
                                  .andExpect(jsonPath("$", is(
                                          OrcidQueueMatcher.matchOrcidQueue(orcidQueue, "Publication"))));
    }

    @Test
    public void deleteOneNotFoundTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(delete("/api/cris/orcidqueues/" + Integer.MAX_VALUE))
                             .andExpect(status().isNotFound());
    }
}
