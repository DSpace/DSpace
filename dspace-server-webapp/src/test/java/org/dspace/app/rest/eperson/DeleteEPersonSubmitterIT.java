/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.eperson;

import static com.jayway.jsonpath.JsonPath.read;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.requestitem.RequestItemAuthor;
import org.dspace.app.requestitem.RequestItemAuthorExtractor;
import org.dspace.app.requestitem.RequestItemHelpdeskStrategy;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.versioning.Version;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersioningService;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

/**
 * Class to test interaction between EPerson deletion and tasks present in the workflow
 */
public class DeleteEPersonSubmitterIT extends AbstractControllerIntegrationTest {

    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance()
                                                                               .getWorkspaceItemService();
    protected XmlWorkflowService xmlWorkflowService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService();
    protected XmlWorkflowItemService xmlWorkflowItemService = XmlWorkflowServiceFactory.getInstance()
                                                                                       .getXmlWorkflowItemService();
    protected VersioningService versioningService = VersionServiceFactory.getInstance().getVersionService();

    protected RequestItemAuthorExtractor requestItemAuthorExtractor =
            DSpaceServicesFactory.getInstance()
                                 .getServiceManager()
                                 .getServiceByName(RequestItemHelpdeskStrategy.class.getName(),
                                         RequestItemAuthorExtractor.class);


    private EPerson submitter;
    private EPerson submitterForVersion1;
    private EPerson submitterForVersion2;
    private EPerson workflowUser;

    private static final Logger log = LogManager.getLogger();

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        submitter = EPersonBuilder.createEPerson(context).withEmail("submitter@example.org").build();
        workflowUser = EPersonBuilder.createEPerson(context).withEmail("workflowUser@example.org").build();
        submitterForVersion1 = EPersonBuilder.createEPerson(context).withEmail("submitterForVersion1@example.org")
                                             .build();
        submitterForVersion2 = EPersonBuilder.createEPerson(context).withEmail("submitterForVersion2@example.org")
                                             .build();

        context.restoreAuthSystemState();

    }


    /**
     * This test verifies that when the submitter Eperson is deleted, the delete
     * succeeds and the item will have 'null' as submitter.
     *
     * @throws Exception
     */
    @Test
    public void testArchivedItemSubmitterDelete() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(submitter)
                                                .withTitle("Test Item")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        Item installItem = installItemService.installItem(context, wsi);

        assertDeletionOfEperson(submitter);

        assertNull(retrieveItemSubmitter(installItem.getID()));

        // Don't depend on external configuration; set up helpdesk as needed.
        final String HELPDESK_EMAIL = "dspace-help@example.com";
        final String HELPDESK_NAME = "Help Desk";
        ConfigurationService configurationService
                = DSpaceServicesFactory.getInstance().getConfigurationService();
        configurationService.setProperty("mail.helpdesk", HELPDESK_EMAIL);
        configurationService.setProperty("mail.helpdesk.name", HELPDESK_NAME);
        configurationService.setProperty("request.item.helpdesk.override", "true");

        // Test it.
        Item item = itemService.find(context, installItem.getID());
        List<RequestItemAuthor> requestItemAuthor = requestItemAuthorExtractor.getRequestItemAuthor(context, item);

        assertEquals(HELPDESK_NAME, requestItemAuthor.get(0).getFullName());
        assertEquals(HELPDESK_EMAIL, requestItemAuthor.get(0).getEmail());
    }

    /**
     * This test verifies that when the submitter Eperson is deleted, the delete succeeds and the item will have
     * 'null' as submitter
     *
     * @throws Exception
     */
    @Test
    public void testWIthdrawnItemSubmitterDelete() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(submitter)
                                                .withTitle("Test Item")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        Item item = installItemService.installItem(context, wsi);

        List<Operation> opsToWithDraw = new ArrayList<>();
        ReplaceOperation replaceOperationToWithDraw = new ReplaceOperation("/withdrawn", true);
        opsToWithDraw.add(replaceOperationToWithDraw);
        String patchBodyToWithdraw = getPatchContent(opsToWithDraw);

        // withdraw item
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(patch("/api/core/items/" + item.getID())
                                         .content(patchBodyToWithdraw)
                                         .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.withdrawn", Matchers.is(true)))
                        .andExpect(jsonPath("$.inArchive", Matchers.is(false)));


        assertDeletionOfEperson(submitter);

        assertNull(retrieveItemSubmitter(item.getID()));

        List<Operation> opsToReinstate = new ArrayList<>();
        ReplaceOperation replaceOperationToReinstate = new ReplaceOperation("/withdrawn", false);
        opsToReinstate.add(replaceOperationToReinstate);
        String patchBodyToReinstate = getPatchContent(opsToReinstate);

        // reinstate item
        getClient(token).perform(patch("/api/core/items/" + item.getID())
                                         .content(patchBodyToReinstate)
                                         .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.withdrawn", Matchers.is(false)))
                        .andExpect(jsonPath("$.inArchive", Matchers.is(true)));

        assertNull(retrieveItemSubmitter(item.getID()));


        // withdraw item again
        getClient(token).perform(patch("/api/core/items/" + item.getID())
                                         .content(patchBodyToWithdraw)
                                         .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.withdrawn", Matchers.is(true)))
                        .andExpect(jsonPath("$.inArchive", Matchers.is(false)));

        assertNull(retrieveItemSubmitter(item.getID()));

    }

    @Test
    public void testVersionItemSubmitterDelete() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(submitter)
                                                .withTitle("Test Item")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        Item item = installItemService.installItem(context, wsi);

        context.setCurrentUser(submitter);
        //TODO: Replace this with a REST call when possible
        Version version1 = versioningService.createNewVersion(context, item);
        Integer version1ID = version1.getID();
        WorkspaceItem version1WorkspaceItem = workspaceItemService.findByItem(context, version1.getItem());
        installItemService.installItem(context, version1WorkspaceItem);

        assertDeletionOfEperson(submitter);
        assertNull(retrieveItemSubmitter(item.getID()));

        Item version1Item = retrieveVersionItem(version1ID);
        assertNull(retrieveItemSubmitter(version1Item.getID()));


        context.setCurrentUser(submitterForVersion1);

        Version version2 = versioningService.createNewVersion(context, item);
        Integer version2ID = version2.getID();
        WorkspaceItem version2WorkspaceItem = workspaceItemService.findByItem(context, version2.getItem());
        installItemService.installItem(context, version2WorkspaceItem);
        Item version2Item = retrieveVersionItem(version2ID);
        assertEquals(submitterForVersion1.getID(), retrieveItemSubmitter(version2Item.getID()).getID());

        context.setCurrentUser(submitterForVersion2);
        Version version3 = versioningService.createNewVersion(context, version2Item);
        Integer version3ID = version3.getID();
        assertDeletionOfEperson(submitterForVersion2);

        getClient(token).perform(get("/api/versioning/versions/" + version3ID + "/item"))
                        .andExpect(status().isNoContent());


        // Clean up versions
        cleanupVersion(version1ID);
        cleanupVersion(version2ID);
        cleanupVersion(version3ID);

    }


    @Test
    public void testWorkspaceItemSubmitterDelete() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(submitter)
                                                .withTitle("Test Item")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();


        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/workspaceitems/" + wsi.getID()))
                        .andExpect(status().isOk());

        assertDeletionOfEperson(submitter);

        getClient(token).perform(get("/api/submission/workspaceitems/" + wsi.getID()))
                        .andExpect(status().isNotFound());
    }

    @Test
    public void testWorkflowItemSubmitterDelete() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .withWorkflowGroup(1, workflowUser)
                                                 .build();

        XmlWorkflowItem workflowItem = WorkflowItemBuilder.createWorkflowItem(context, collection)
                                                   .withSubmitter(submitter)
                                                   .withTitle("Test Item")
                                                   .withIssueDate("2019-03-06")
                                                   .withSubject("ExtraEntry")
                                                   .build();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/workflow/workflowitems/" + workflowItem.getID()))
                        .andExpect(status().isOk());

        assertDeletionOfEperson(submitter);

        getClient(token).perform(get("/api/workflow/workflowitems/" + workflowItem.getID()))
                        .andExpect(status().isOk());

    }


    private void assertDeletionOfEperson(EPerson ePerson) throws SQLException {
        try {
            String token = getAuthToken(admin.getEmail(), password);
            getClient(token).perform(delete("/api/eperson/epersons/" + ePerson.getID()))
                            .andExpect(status().isNoContent());

        } catch (Exception ex) {
            log.error("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() + ": ", ex);
            fail("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() +
                         ": " + ex.getMessage());
        }
        EPerson ePersonCheck = ePersonService.find(context, ePerson.getID());
        assertNull(ePersonCheck);
    }

    /**
     * TODO: THis method currently retrieves the submitter through the itemService. A method should be added to retrieve
     * TODO: this through the REST API
     */
    private EPerson retrieveItemSubmitter(UUID itemID) throws Exception {

        Item item = itemService.find(context, itemID);
        return item.getSubmitter();

    }

    private Item retrieveVersionItem(int id) throws Exception {
        AtomicReference<String> idRef = new AtomicReference<>();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/versioning/versions/" + id + "/item"))
                        .andExpect(status().isOk())
                        .andDo(result -> idRef
                                .set(read(result.getResponse().getContentAsString(), "$.uuid")));

        return itemService.find(context, UUID.fromString(idRef.get()));
    }

    private void cleanupVersion(int id) throws SQLException {
        Version version = versioningService.getVersion(context, id);
        versioningService.delete(context, version);

    }


}
