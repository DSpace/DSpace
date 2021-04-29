/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.orcid;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.orcid.OrcidHistory;
import org.dspace.app.orcid.OrcidQueue;
import org.dspace.app.rest.matcher.OrcidHistoryMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.OrcidHistoryBuilder;
import org.dspace.builder.OrcidQueueBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.eperson.EPerson;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.http.MediaType;

/**
 * Integration test class for the orcid history endpoint
 *
 * @author Mykhaylo Boychuk - 4Science
 */
public class OrcidHistoryRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Test
    public void findAllTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/cris/orcidhistories"))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson researcher = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Josiah", "Carberry")
            .withEmail("josiah.Carberry@example.com")
            .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Person")
            .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Collection 2").build();

        Item itemPerson = ItemBuilder.createItem(context, col1)
            .withPersonIdentifierFirstName("Josiah")
            .withPersonIdentifierLastName("Carberry")
            .withCrisOwner(researcher.getFullName(), researcher.getID().toString())
            .build();

        Item itemPublication = ItemBuilder.createItem(context, col2)
            .withAuthor("Josiah, Carberry")
            .withTitle("A Methodology for the Emulation of Architecture")
            .withIssueDate("2013-08-03").build();

        OrcidHistory orcidHistory = OrcidHistoryBuilder.createOrcidHistory(context, itemPerson, itemPublication)
            .withResponseMessage("<xml><work>...</work>")
            .withPutCode("123456")
            .withStatus(201).build();

        context.restoreAuthSystemState();

        String tokenResearcher = getAuthToken(researcher.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenResearcher).perform(get("/api/cris/orcidhistories/" + orcidHistory.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", is(OrcidHistoryMatcher.matchOrcidHistory(
                orcidHistory, 201, "123456", "<xml><work>...</work>"))))
            .andExpect(jsonPath("$._links.self.href", Matchers
                .containsString("/api/cris/orcidhistories/" + orcidHistory.getID())));

        getClient(tokenAdmin).perform(get("/api/cris/orcidhistories/" + orcidHistory.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", is(OrcidHistoryMatcher.matchOrcidHistory(
                orcidHistory, 201, "123456", "<xml><work>...</work>"))))
            .andExpect(jsonPath("$._links.self.href", Matchers
                .containsString("/api/cris/orcidhistories/" + orcidHistory.getID())));
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
            .withEntityType("Person")
            .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Collection 2").build();

        Item itemPerson = ItemBuilder.createItem(context, col1)
            .withPersonIdentifierFirstName("Josiah")
            .withPersonIdentifierLastName("Carberry")
            .withCrisOwner(researcher.getFullName(), researcher.getID().toString())
            .build();

        Item itemPublication = ItemBuilder.createItem(context, col2)
            .withAuthor("Josiah, Carberry")
            .withTitle("A Methodology for the Emulation of Architecture")
            .withIssueDate("2013-08-03").build();

        OrcidHistory orcidHistory = OrcidHistoryBuilder.createOrcidHistory(context, itemPerson, itemPublication)
            .withResponseMessage("<xml><work>...</work>")
            .withPutCode("123456")
            .withStatus(201).build();

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);

        getClient(tokenEperson).perform(get("/api/cris/orcidhistories/" + orcidHistory.getID().toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    public void findOneisUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson researcher = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Josiah", "Carberry")
            .withEmail("josiah.Carberry@example.com")
            .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Person")
            .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Collection 2").build();

        Item itemPerson = ItemBuilder.createItem(context, col1)
            .withPersonIdentifierFirstName("Josiah")
            .withPersonIdentifierLastName("Carberry")
            .withCrisOwner(researcher.getFullName(), researcher.getID().toString())
            .build();

        Item itemPublication = ItemBuilder.createItem(context, col2)
            .withAuthor("Josiah, Carberry")
            .withTitle("A Methodology for the Emulation of Architecture")
            .withIssueDate("2013-08-03").build();

        OrcidHistory orcidHistory = OrcidHistoryBuilder.createOrcidHistory(context, itemPerson, itemPublication)
            .withResponseMessage("<xml><work>...</work>")
            .withPutCode("123456")
            .withStatus(201).build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/cris/orcidhistories/" + orcidHistory.getID().toString()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneNotFoundTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/cris/orcidhistories/" + Integer.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    public void createForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson researcher = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Josiah", "Carberry")
            .withEmail("josiah.Carberry@example.com")
            .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Person")
            .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Collection 2").build();

        Item itemPerson = ItemBuilder.createItem(context, col1)
            .withPersonIdentifierFirstName("Josiah")
            .withPersonIdentifierLastName("Carberry")
            .withCrisOwner(researcher.getFullName(), researcher.getID().toString()).build();

        itemService.addMetadata(context, itemPerson, "person", "identifier", "orcid", "en", "0000-0002-1825-0097");
        itemService.addMetadata(context, itemPerson, "cris", "orcid", "access-token", "en",
            "f5af9f51-07e6-4332-8f1a-c0c11c1e3728");

        Item itemPublication = ItemBuilder.createItem(context, col2).withAuthor("Josiah, Carberry")
            .withTitle("A Methodology for the Emulation of Architecture")
            .withIssueDate("2013-08-03").build();

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, itemPerson, itemPublication).build();

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(post("/api/cris/orcidhistories")
            .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
            .content("/api/cris/orcidqueues/" + orcidQueue.getID()))
            .andExpect(status().isForbidden());
    }

    @Test
    public void createUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson researcher = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Josiah", "Carberry")
            .withEmail("josiah.Carberry@example.com")
            .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Person")
            .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Collection 2").build();

        Item itemPerson = ItemBuilder.createItem(context, col1)
            .withPersonIdentifierFirstName("Josiah")
            .withPersonIdentifierLastName("Carberry")
            .withCrisOwner(researcher.getFullName(), researcher.getID().toString()).build();

        itemService.addMetadata(context, itemPerson, "person", "identifier", "orcid", "en", "0000-0002-1825-0097");
        itemService.addMetadata(context, itemPerson, "cris", "orcid", "access-token", "en",
            "f5af9f51-07e6-4332-8f1a-c0c11c1e3728");

        Item itemPublication = ItemBuilder.createItem(context, col2).withAuthor("Josiah, Carberry")
            .withTitle("A Methodology for the Emulation of Architecture")
            .withIssueDate("2013-08-03").build();

        OrcidQueue orcidQueue = OrcidQueueBuilder.createOrcidQueue(context, itemPerson, itemPublication).build();

        context.restoreAuthSystemState();

        getClient().perform(post("/api/cris/orcidhistories")
            .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
            .content("/api/cris/orcidqueues/" + orcidQueue.getID()))
            .andExpect(status().isUnauthorized());

    }

}
