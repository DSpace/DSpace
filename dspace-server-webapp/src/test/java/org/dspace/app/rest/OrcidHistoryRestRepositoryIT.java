/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.orcid.OrcidHistory;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.OrcidHistoryBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.junit.Test;

/**
 * Integration test class for the orcid history endpoint
 *
 * @author Mykhaylo Boychuk - 4Science
 */
public class OrcidHistoryRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAllTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/cris/orcidHistories"))
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

        getClient(tokenResearcher).perform(get("/api/cris/orcidHistories/" + orcidHistory.getID().toString()))
                                  .andExpect(status().isOk());

        getClient(tokenAdmin).perform(get("/api/cris/orcidHistories/" + orcidHistory.getID().toString()))
                             .andExpect(status().isOk());
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

        getClient(tokenEperson).perform(get("/api/cris/orcidHistories/" + orcidHistory.getID().toString()))
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

        Item itemPublication = ItemBuilder.createItem(context, col2)
                                          .withAuthor("Josiah, Carberry")
                                          .withTitle("A Methodology for the Emulation of Architecture")
                                          .withIssueDate("2013-08-03").build();

        OrcidHistory orcidHistory = OrcidHistoryBuilder.createOrcidHistory(context, itemPerson, itemPublication)
                                                       .withResponseMessage("<xml><work>...</work>")
                                                       .withPutCode("123456")
                                                       .withStatus(201).build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/cris/orcidHistories/" + orcidHistory.getID().toString()))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneNotFoundTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/cris/orcidHistories/" + Integer.MAX_VALUE))
                             .andExpect(status().isNotFound());
    }
}
