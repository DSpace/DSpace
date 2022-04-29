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
import javax.ws.rs.core.MediaType;

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
 * Since the CC Licenses are obtained from the CC License API, a mock service has been implemented
 * This mock service will return a fixed set of CC Licenses using a similar structure to the ones obtained from the
 * CC License API.
 * Refer to {@link org.dspace.license.MockCCLicenseConnectorServiceImpl} for more information
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
        AddOperation addOperation = new AddOperation("/sections/cclicense/uri",
                                                     "http://creativecommons.org/licenses/by-nc-sa/4.0/");

        ops.add(addOperation);
        String patchBody = getPatchContent(ops);


        getClient(adminToken).perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                                              .content(patchBody)
                                              .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.cclicense", allOf(
                                     hasJsonPath("$.uri", is("http://creativecommons.org/licenses/by-nc-sa/4.0/")),
                                     hasJsonPath("$.rights",
                                                 is("Attribution-NonCommercial-ShareAlike 4.0 International")),
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
        AddOperation addOperation = new AddOperation("/sections/cclicense/uri", "invalid-license-uri");

        ops.add(addOperation);
        String patchBody = getPatchContent(ops);


        getClient(adminToken).perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                                              .content(patchBody)
                                              .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isInternalServerError());

        getClient(adminToken).perform(get("/api/submission/workspaceitems/" + workspaceItem.getID())
                                              .content(patchBody)
                                              .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.cclicense", allOf(
                                     hasJsonPath("$.uri", nullValue()),
                                     hasJsonPath("$.rights",nullValue()),
                                     hasJsonPath("$.file", nullValue())
                             )));


    }
}
