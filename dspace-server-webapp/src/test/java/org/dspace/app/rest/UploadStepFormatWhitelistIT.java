/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Integration tests for the (opt-in) bitstream format upload whitelist enforced by the
 * submission {@link org.dspace.app.rest.submit.step.UploadStep}.
 */
public class UploadStepFormatWhitelistIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @After
    public void resetWhitelistConfig() {
        configurationService.setProperty("bitstream.format.upload.whitelist.enabled", null);
        configurationService.setProperty("bitstream.format.upload.whitelist.min-support-level", null);
    }

    /**
     * With the whitelist enabled, a file whose format resolves to "Unknown" (an unregistered
     * extension such as .exe) is rejected: the submission reports a format error and no
     * bitstream is added.
     */
    @Test
    public void uploadDisallowedFormatIsRejected() throws Exception {
        configurationService.setProperty("bitstream.format.upload.whitelist.enabled", true);

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").build();
        Collection col = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection").build();
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col)
                                                  .withTitle("Test WorkspaceItem")
                                                  .withIssueDate("2024-01-01")
                                                  .build();
        context.restoreAuthSystemState();

        MockMultipartFile exeFile = new MockMultipartFile("file", "malware.exe",
            "application/octet-stream", "not really an executable".getBytes());

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(multipart("/api/submission/workspaceitems/" + witem.getID()).file(exeFile))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.errors[?(@.message=='error.validation.upload.format-not-allowed')]", hasSize(1)))
            .andExpect(jsonPath("$.sections.upload.files", hasSize(0)));
    }

    /**
     * With the whitelist enabled, a file whose format is KNOWN (e.g. a .pdf) is accepted.
     */
    @Test
    public void uploadAllowedFormatIsAccepted() throws Exception {
        configurationService.setProperty("bitstream.format.upload.whitelist.enabled", true);

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").build();
        Collection col = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection").build();
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col)
                                                  .withTitle("Test WorkspaceItem")
                                                  .withIssueDate("2024-01-01")
                                                  .build();
        context.restoreAuthSystemState();

        MockMultipartFile pdfFile = new MockMultipartFile("file", "document.pdf",
            "application/pdf", "%PDF-1.4 fake pdf".getBytes());

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(multipart("/api/submission/workspaceitems/" + witem.getID()).file(pdfFile))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.errors[?(@.message=='error.validation.upload.format-not-allowed')]", hasSize(0)))
            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.title'][0].value", is("document.pdf")));
    }

    /**
     * With the whitelist disabled (the default), the same "Unknown" file is accepted.
     */
    @Test
    public void uploadDisallowedFormatAcceptedWhenWhitelistDisabled() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").build();
        Collection col = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection").build();
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col)
                                                  .withTitle("Test WorkspaceItem")
                                                  .withIssueDate("2024-01-01")
                                                  .build();
        context.restoreAuthSystemState();

        MockMultipartFile exeFile = new MockMultipartFile("file", "malware.exe",
            "application/octet-stream", "not really an executable".getBytes());

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(multipart("/api/submission/workspaceitems/" + witem.getID()).file(exeFile))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.errors[?(@.message=='error.validation.upload.format-not-allowed')]", hasSize(0)))
            .andExpect(jsonPath("$.sections.upload.files", hasSize(1)));
    }
}
