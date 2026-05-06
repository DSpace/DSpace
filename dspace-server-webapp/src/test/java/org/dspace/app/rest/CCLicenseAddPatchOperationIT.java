/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.MediaType;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.WorkspaceItem;
import org.junit.Test;
/**
 * Class to test the methods from the CCLicenseAddPatchOperation
 * CC License now grabs from a set csv and index.rdf file
 *
 */
public class CCLicenseAddPatchOperationIT extends AbstractControllerIntegrationTest {

    @Test
    public void patchSubmissionCCLicense() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                .withName("Community")
                .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                .withName("Collection")
                .build();

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle("Workspace Item")
                .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<>();
        ops.add(new AddOperation("/sections/cclicense/uri",
                "https://creativecommons.org/licenses/by-nc-sa/4.0/"));

        getClient(adminToken).perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                        .content(getPatchContent(ops))
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.cclicense", allOf(
                        hasJsonPath("$.uri", is("https://creativecommons.org/licenses/by-nc-sa/4.0/")),
                        hasJsonPath("$.rights",
                                is("Attribution-NonCommercial-ShareAlike 4.0 International")),
                        hasJsonPath("$.file.name", is("license_rdf"))
                )));
    }

    @Test
    public void patchSubmissionCCLicense30WithJurisdiction() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                .withName("Community")
                .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                .withName("Collection")
                .build();

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle("Workspace Item")
                .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<>();
        ops.add(new AddOperation("/sections/cclicense/uri",
                "https://creativecommons.org/licenses/by/3.0/de/"));

        getClient(adminToken).perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                        .content(getPatchContent(ops))
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.cclicense", allOf(
                        hasJsonPath("$.uri", is("https://creativecommons.org/licenses/by/3.0/de/")),
                        hasJsonPath("$.rights", is("Namensnennung 3.0 Deutschland")),
                        hasJsonPath("$.file.name", is("license_rdf"))
                )));
    }

    @Test
    public void patchSubmissionCC0License() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                .withName("Community")
                .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                .withName("Collection")
                .build();

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle("Workspace Item")
                .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<>();
        ops.add(new AddOperation("/sections/cclicense/uri",
                "https://creativecommons.org/publicdomain/zero/1.0/"));

        getClient(adminToken).perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                        .content(getPatchContent(ops))
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.cclicense", allOf(
                        hasJsonPath("$.uri", is("https://creativecommons.org/publicdomain/zero/1.0/")),
                        hasJsonPath("$.rights", is("CC0 1.0 Universal")),
                        hasJsonPath("$.file.name", is("license_rdf"))
                )));
    }

    @Test
    public void patchSubmissionCCLicenseInvalid() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                .withName("Community")
                .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                .withName("Collection")
                .build();

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle("Workspace Item")
                .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<>();
        ops.add(new AddOperation("/sections/cclicense/uri", "invalid-license-uri"));

        getClient(adminToken).perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                        .content(getPatchContent(ops))
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isInternalServerError());

        getClient(adminToken).perform(get("/api/submission/workspaceitems/" + workspaceItem.getID())
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.cclicense", allOf(
                        hasJsonPath("$.uri", nullValue()),
                        hasJsonPath("$.rights", nullValue()),
                        hasJsonPath("$.file", nullValue())
                )));
    }
}
