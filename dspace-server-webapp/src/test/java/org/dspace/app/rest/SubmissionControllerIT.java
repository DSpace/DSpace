/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.MediaType;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test for the SubmissionController
 *
 * @author Milan Majchrak (dspace at dataquest.sk)
 */
public class SubmissionControllerIT extends AbstractControllerIntegrationTest {

    private static final String SUBMITTER_EMAIL = "submitter@example.com";
    @Autowired
    private WorkspaceItemService workspaceItemService;
    @Autowired
    private EPersonService ePersonService;

    WorkspaceItem wsi;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community with one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        //2. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail(SUBMITTER_EMAIL)
                .withPassword("dspace")
                .build();

        // Submitter group - allow deposit a new item without workflow
        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection")
                .withSubmitterGroup(submitter)
                .build();

        wsi = WorkspaceItemBuilder.createWorkspaceItem(context, col)
                .withTitle("Item with custom handle")
                .withIssueDate("2017-10-17")
                .withSubmitter(submitter)
                .build();
        context.restoreAuthSystemState();
    }

    @Test
    public void generateShareTokenAndSetOwnerTest() throws Exception {
        AtomicReference<String> shareLink = new AtomicReference<>();
        EPerson currentUser = context.getCurrentUser();

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/submission/share")
                        .param("workspaceitemid", wsi.getID().toString())
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareLink", is(notNullValue())))
                .andDo(result -> shareLink.set(read(result.getResponse().getContentAsString(), "$.shareLink")));

        // Check that the share token was set on the WorkspaceItem and persisted into the database
        WorkspaceItem updatedWsi = workspaceItemService.find(context, wsi.getID());
        assertThat(wsi.getID(), is(updatedWsi.getID()));
        assertThat(updatedWsi.getSubmitter().getEmail(), is(SUBMITTER_EMAIL));
        assertThat(updatedWsi.getSubmitter().getEmail(), not(currentUser.getEmail()));

        EPerson adminUser = ePersonService.findByEmail(context, admin.getEmail());
        context.setCurrentUser(adminUser);
        // Set workspace item owner to the current user
        getClient(adminToken).perform(get("/api/submission/setOwner")
                .param("shareToken", updatedWsi.getShareToken())
                .param("workspaceitemid", updatedWsi.getID().toString())
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk());

        // Check that the owner of the WorkspaceItem was set to the current user
        // Check the wsi was persisted into the database
        updatedWsi = workspaceItemService.find(context, wsi.getID());
        assertThat(updatedWsi.getSubmitter().getEmail(), is(adminUser.getEmail()));
        assertThat(updatedWsi.getSubmitter().getEmail(), not(SUBMITTER_EMAIL));
    }
}
