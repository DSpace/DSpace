/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.WorkspaceItemBuilder;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Constants;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class MetadataSuggestionRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private AuthorizeService authorizeService;

    WorkspaceItem workspaceItem = null;

    @Before
    public void setup() throws SQLException {
        context.setCurrentUser(admin);

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();


        //2. Three workspace items in two different collections
        workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                                            .withTitle("Workspace Item 1")
                                            .withIssueDate("2017-10-17")
                                            .build();
    }


    @Test
    public void findAllTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/integration/metadatasuggestion")
                                    .param("workspaceitem", String.valueOf(workspaceItem.getID())))
                        .andExpect(status().isOk());
    }

    @Test
    public void findAllTestPagination() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/integration/metadatasuggestion")
                                     .param("workspaceitem", String.valueOf(workspaceItem.getID()))
                                     .param("size", "1"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.metadatasuggestions[0].id", Matchers.is("mock")));

        getClient(token).perform(get("/api/integration/metadatasuggestion")
                                        .param("workspaceitem", String.valueOf(workspaceItem.getID()))
                                        .param("size", "1")
                                        .param("page", "1"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.metadatasuggestions[0].id", Matchers.is("mock2")));
    }


    @Test
    public void findAllTestForbidden() throws Exception {

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/integration/metadatasuggestion")
                                     .param("workspaceitem", String.valueOf(workspaceItem.getID())))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void findAllTestUnauthorized() throws Exception {

        getClient().perform(get("/api/integration/metadatasuggestion")
                                     .param("workspaceitem", String.valueOf(workspaceItem.getID())))
                        .andExpect(status().isUnauthorized());
    }

    @Test
    public void findAllTestUserWithWriteRights() throws Exception {

        context.turnOffAuthorisationSystem();
        authorizeService.addPolicy(context, workspaceItem.getItem(), Constants.WRITE, eperson);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/integration/metadatasuggestion")
                                .param("workspaceitem", String.valueOf(workspaceItem.getID())))
                   .andExpect(status().isOk());
    }

    @Test
    public void findAllNoWorkspaceItem() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/integration/metadatasuggestion"))
                        .andExpect(status().isBadRequest());
    }

    @Test
    public void findAllWrongWorkspaceItemId() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/integration/metadatasuggestion")
                                .param("workspaceitem", "123123123"))
                        .andExpect(status().isBadRequest());
    }

}
