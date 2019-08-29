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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.junit.Test;

/**
 * Test suite for the AuthorityEntryValue endpoint
 * @author Giuseppe Digilio (giuseppe.digilio at 4science.it)
 *
 */
public class AuthorityEntryLinkRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    /**
     * The authorities endpoint must provide proper pagination
     *
     * @throws Exception
     */
    public void authorAuthorityEntriesTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item author1 = ItemBuilder.createItem(context, col1)
            .withTitle("Smith, Donald")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald")
            .withRelationshipType("Person")
            .build();

        Item author2 = ItemBuilder.createItem(context, col2)
            .withTitle("Smith, Maria")
            .withIssueDate("2016-02-13")
            .withAuthor("Smith, Maria")
            .withRelationshipType("Person")
            .build();

        Item author3 = ItemBuilder.createItem(context, col2)
            .withTitle("Smith, Joe")
            .withIssueDate("2016-12-13")
            .withAuthor("Smith, Joe")
            .withRelationshipType("Person")
            .build();

        Item author4 = ItemBuilder.createItem(context, col2)
            .withTitle("Smith, Jack")
            .withIssueDate("2016-09-20")
            .withAuthor("Smith, Jack")
            .withRelationshipType("Person")
            .build();

        Item author5 = ItemBuilder.createItem(context, col2)
            .withTitle("Smith, J.")
            .withIssueDate("2016-01-13")
            .withAuthor("Smith, J.")
            .withRelationshipType("Person")
            .build();

        Item author6 = ItemBuilder.createItem(context, col1)
            .withTitle("Foe, Donald")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald")
            .withRelationshipType("Person")
            .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/integration/authorities/AuthorAuthority/entries")
            .param("query", "Smith")
            .param("metadata", "dc.contributor.author")
            .param("uuid", col1.getID().toString())
            .param("size", "10"))
            //We expect a 200 OK status
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size", is(10)))
            .andExpect(jsonPath("$.page.totalElements", is(5)))
            .andExpect(jsonPath("$._embedded.authorityEntries[0].value", is("Smith, Donald")))
            .andExpect(jsonPath("$._embedded.authorityEntries[1].value", is("Smith, Maria")))
            .andExpect(jsonPath("$._embedded.authorityEntries[2].value", is("Smith, Jack")))
            .andExpect(jsonPath("$._embedded.authorityEntries[3].value", is("Smith, Joe")))
            .andExpect(jsonPath("$._embedded.authorityEntries[4].value", is("Smith, J.")));

        getClient(token).perform(get("/api/integration/authorities/AuthorAuthority/entries")
            .param("query", "Smith, J")
            .param("metadata", "dc.contributor.author")
            .param("uuid", col1.getID().toString())
            .param("size", "10"))
            //We expect a 200 OK status
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size", is(10)))
            .andExpect(jsonPath("$.page.totalElements", is(3)))
            .andExpect(jsonPath("$._embedded.authorityEntries[0].value", is("Smith, J.")))
            .andExpect(jsonPath("$._embedded.authorityEntries[1].value", is("Smith, Jack")))
            .andExpect(jsonPath("$._embedded.authorityEntries[2].value", is("Smith, Joe")));

        getClient(token).perform(get("/api/integration/authorities/AuthorAuthority/entries")
            .param("query", "Smith, J.")
            .param("metadata", "dc.contributor.author")
            .param("uuid", col1.getID().toString())
            .param("size", "10"))
            //We expect a 200 OK status
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size", is(10)))
            .andExpect(jsonPath("$.page.totalElements", is(1)))
            .andExpect(jsonPath("$._embedded.authorityEntries[0].value", is("Smith, J.")));

        getClient(token).perform(get("/api/integration/authorities/AuthorAuthority/entries")
            .param("query", "Donald")
            .param("metadata", "dc.contributor.author")
            .param("uuid", col1.getID().toString())
            .param("size", "10"))
            //We expect a 200 OK status
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size", is(10)))
            .andExpect(jsonPath("$.page.totalElements", is(2)))
            .andExpect(jsonPath("$._embedded.authorityEntries[0].value", is("Smith, Donald")))
            .andExpect(jsonPath("$._embedded.authorityEntries[1].value", is("Foe, Donald")));
    }

    @Test
    /**
     * The authorities endpoint must provide proper pagination
     *
     * @throws Exception
     */
    public void journalAuthorityEntriesTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item journal = ItemBuilder.createItem(context, col1)
            .withTitle("Science Technological journal")
            .withIssueDate("2017-10-17")
            .withAuthor("Science Technological journal")
            .withRelationshipType("Journal")
            .build();

        Item journal1 = ItemBuilder.createItem(context, col1)
            .withTitle("Science China-Earth Sciences")
            .withIssueDate("2017-10-17")
            .withAuthor("Science China-Earth Sciences")
            .withRelationshipType("Journal")
            .build();

        Item journal2 = ItemBuilder.createItem(context, col1)
            .withTitle("Science")
            .withIssueDate("2017-10-17")
            .withAuthor("Science")
            .withRelationshipType("Journal")
            .build();

        Item journal3 = ItemBuilder.createItem(context, col1)
            .withTitle("Ocean Science")
            .withIssueDate("2017-10-17")
            .withAuthor("Ocean Science")
            .withRelationshipType("Journal")
            .build();

        Item journal4 = ItemBuilder.createItem(context, col1)
            .withTitle("Management science")
            .withIssueDate("2017-10-17")
            .withAuthor("Management science")
            .withRelationshipType("Journal")
            .build();

        Item journal5 = ItemBuilder.createItem(context, col1)
            .withTitle("Service Science")
            .withIssueDate("2017-10-17")
            .withAuthor("Service Science")
            .withRelationshipType("Journal")
            .build();

        Item journal6 = ItemBuilder.createItem(context, col1)
            .withTitle("Annals of science")
            .withIssueDate("2017-10-17")
            .withAuthor("Annals of science")
            .withRelationshipType("Journal")
            .build();

        Item journal7 = ItemBuilder.createItem(context, col1)
            .withTitle("Bioresource technology journal")
            .withIssueDate("2017-10-17")
            .withAuthor("Bioresource technology journal")
            .withRelationshipType("Journal")
            .build();

        Item journal8 = ItemBuilder.createItem(context, col1)
            .withTitle("Energy economics")
            .withIssueDate("2017-10-17")
            .withAuthor("Energy economics")
            .withRelationshipType("Journal")
            .build();

        Item journal9 = ItemBuilder.createItem(context, col1)
            .withTitle("Nuclear Energy-journal Of The British Nuclear Energy Society")
            .withIssueDate("2017-10-17")
            .withAuthor("Nuclear Energy-journal Of The British Nuclear Energy Society")
            .withRelationshipType("Journal")
            .build();

        Item journal10 = ItemBuilder.createItem(context, col1)
            .withTitle("Journal of High Energy Astrophysics")
            .withIssueDate("2017-10-17")
            .withAuthor("Journal of High Energy Astrophysics")
            .withRelationshipType("Journal")
            .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/integration/authorities/JournalAuthority/entries")
            .param("query", "Journal")
            .param("metadata", "dc.relation.journal")
            .param("uuid", col1.getID().toString())
            .param("size", "10"))
            //We expect a 200 OK status
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size", is(10)))
            .andExpect(jsonPath("$.page.totalElements", is(4)))
            .andExpect(jsonPath("$._embedded.authorityEntries[0].value", is("Science Technological journal")))
            .andExpect(jsonPath("$._embedded.authorityEntries[1].value", is("Bioresource technology journal")))
            .andExpect(jsonPath("$._embedded.authorityEntries[2].value", is("Journal of High Energy Astrophysics")))
            .andExpect(jsonPath("$._embedded.authorityEntries[3].value",
                    is("Nuclear Energy-journal Of The British Nuclear Energy Society")));

        getClient(token).perform(get("/api/integration/authorities/JournalAuthority/entries")
            .param("query", "science")
            .param("metadata", "dc.relation.journal")
            .param("uuid", col1.getID().toString())
            .param("size", "10"))
            //We expect a 200 OK status
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size", is(10)))
            .andExpect(jsonPath("$.page.totalElements", is(7)))
            .andExpect(jsonPath("$._embedded.authorityEntries[0].value", is("Science")))
            .andExpect(jsonPath("$._embedded.authorityEntries[1].value", is("Science China-Earth Sciences")))
            .andExpect(jsonPath("$._embedded.authorityEntries[2].value", is("Management science")))
            .andExpect(jsonPath("$._embedded.authorityEntries[3].value", is("Ocean Science")))
            .andExpect(jsonPath("$._embedded.authorityEntries[4].value", is("Service Science")))
            .andExpect(jsonPath("$._embedded.authorityEntries[5].value", is("Annals of science")))
            .andExpect(jsonPath("$._embedded.authorityEntries[6].value", is("Science Technological journal")));

        getClient(token).perform(get("/api/integration/authorities/JournalAuthority/entries")
            .param("query", "Annals of science")
            .param("metadata", "dc.relation.journal")
            .param("uuid", col1.getID().toString())
            .param("size", "10"))
            //We expect a 200 OK status
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size", is(10)))
            .andExpect(jsonPath("$.page.totalElements", is(1)))
            .andExpect(jsonPath("$._embedded.authorityEntries[0].value", is("Annals of science")));
    }
}