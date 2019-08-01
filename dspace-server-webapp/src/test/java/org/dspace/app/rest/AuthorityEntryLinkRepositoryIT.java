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

import java.util.List;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Test;

/**
 * Test suite for the AuthorityEntryValue endpoint
 * @author Giuseppe Digilio (giuseppe.digilio at 4science.it)
 *
 */
public class AuthorityEntryLinkRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    /**
     * The workspaceitem endpoint must provide proper pagination
     *
     * @throws Exception
     */
    public void entriesTest() throws Exception {
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
                                  .withTitle("Author1")
                                  .withIssueDate("2017-10-17")
                                  .withAuthor("Smith, Donald")
                                  .withRelationshipType("Person")
                                  .build();

        Item author2 = ItemBuilder.createItem(context, col2)
                                  .withTitle("Author2")
                                  .withIssueDate("2016-02-13")
                                  .withAuthor("Smith, Maria")
                                  .withRelationshipType("Person")
                                  .build();

        Item author3 = ItemBuilder.createItem(context, col2)
                                  .withTitle("Author3")
                                  .withIssueDate("2016-02-13")
                                  .withAuthor("Maybe, Maybe")
                                  .withRelationshipType("Person")
                                  .build();

        Item orgUnit1 = ItemBuilder.createItem(context, col3)
                                   .withTitle("OrgUnit1")
                                   .withAuthor("Testy, TEst")
                                   .withIssueDate("2015-01-01")
                                   .withRelationshipType("OrgUnit")
                                   .build();

        Item project1 = ItemBuilder.createItem(context, col3)
                                   .withTitle("Project1")
                                   .withAuthor("Testy, TEst")
                                   .withIssueDate("2015-01-01")
                                   .withRelationshipType("Project")
                                   .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
        List<String> list = configService.getPropertyKeys("choices.plugin.");
        String wosUser = configService.getProperty("submission.lookup.webofknowledge.user");
        String wosPassword = configService.getProperty("submission.lookup.webofknowledge.password");
        String grobid = configService.getProperty("metadata.extraction.grobid.url");

        getClient(token).perform(get("/api/integration/authorities/AuthorAuthority/entries")
                .param("query", "Author1")
                .param("metadata", "dc.contributor.author")
                .param("uuid", col1.getID().toString())
                .param("size", "10"))
            //We expect a 200 OK status
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size", is(10)))
            .andExpect(jsonPath("$.page.totalElements", is(1)));
    }
}