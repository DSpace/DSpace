/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MediaType;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.WorkspaceItemBuilder;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.WorkspaceItem;
import org.junit.Test;

/**
 * Class to the methods from the CCLicenseAddPatchOperation
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

        String adminToken = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation addOperation = new AddOperation("/sections/cclicense/uri", "license-uri");

        ops.add(addOperation);
        String patchBody = getPatchContent(ops);


        getClient(adminToken).perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                                              .content(patchBody)
                                              .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk());
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

        String adminToken = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation addOperation = new AddOperation("/sections/cclicense/uri", "invalid-license-uri");

        ops.add(addOperation);
        String patchBody = getPatchContent(ops);


        getClient(adminToken).perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                                              .content(patchBody)
                                              .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isInternalServerError());
    }
}
