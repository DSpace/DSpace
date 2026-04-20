/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.I18nUtil;
import org.dspace.discovery.IndexingService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.handle.service.HandleService;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.services.ConfigurationService;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test item link and section data REST endpoints for duplicate detection.
 * @see DuplicateDetectionTest (dspace-api) for lower level integration tests.
 *
 * @author Kim Shepherd
 */
public class DuplicateDetectionRestIT extends AbstractControllerIntegrationTest {

    @Autowired
    ConfigurationService configurationService;
    @Autowired
    ItemService itemService;
    @Autowired
    IndexingService indexingService;
    @Autowired
    CollectionService collectionService;
    @Autowired
    HandleService handleService;
    @Autowired
    WorkspaceItemService workspaceItemService;
    @Autowired
    XmlWorkflowItemService workflowItemService;
    @Autowired
    IdentifierService identifierService;
    @Autowired
    AuthorizeService authorizeService;
    @Autowired
    XmlWorkflowService workflowService;
    @Autowired
    EPersonService ePersonService;

    private Collection col;
    private Collection simpleCol;
    private final String item1IssueDate = "2011-10-17";
    private final String item1Subject = "ExtraEntry 1";
    private final String item1Title = "Public item I";
    private final String item1Author = "Smith, Donald";
    private final String item2Subject = "ExtraEntry 2";
    private final String item2IssueDate = "2012-10-17";
    private EPerson anotherEPerson;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Temporarily enable duplicate detection and set comparison value distance to 1
        configurationService.setProperty("duplicate.enable", true);
        configurationService.setProperty("duplicate.comparison.distance", 1);
        configurationService.setProperty("duplicate.comparison.normalise.lowercase", true);
        configurationService.setProperty("duplicate.comparison.normalise.whitespace", true);
        configurationService.setProperty("duplicate.comparison.solr.field", "deduplication_keyword");
        configurationService.setProperty("duplicate.comparison.metadata.field", new String[]{"dc.title"});
        configurationService.setProperty("duplicate.preview.metadata.field",
                new String[]{"dc.date.issued", "dc.subject"});

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

        col = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Test Collection")
                .withWorkflowGroup(1, admin)
                .build();
        simpleCol = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Test Collection without Workflow")
                .build();
        eperson.setFirstName(context, "first");
        eperson.setLastName(context, "last");

        anotherEPerson = EPersonBuilder.createEPerson(context)
                .withEmail("test-another-user@email.com")
                .withNameInMetadata("first", "last")
                .withCanLogin(true)
                .withLanguage(I18nUtil.getDefaultLocale().getLanguage())
                .withPassword(password)
                .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void searchDuplicatesBySearchMethodTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        context.turnOffAuthorisationSystem();

        // Ingest three example items with slightly different titles
        // item2 is 1 edit distance from item1 and item3
        // item1 and item3 are 2 edit distance from each other
        WorkspaceItem workspaceItem1 = WorkspaceItemBuilder.createWorkspaceItem(context, simpleCol)
                .withTitle(item1Title)
                .withSubject(item1Subject)
                .withIssueDate(item1IssueDate)
                .withAuthor(item1Author)
                .withSubmitter(eperson)
                .build();
        WorkspaceItem workspaceItem2 = WorkspaceItemBuilder.createWorkspaceItem(context, simpleCol)
                .withTitle("Public item II")
                .withIssueDate(item2IssueDate)
                .withAuthor("Smith, Donald X.")
                .withSubject(item2Subject)
                .withSubmitter(eperson)
                .build();
        WorkspaceItem workspaceItem3 = WorkspaceItemBuilder.createWorkspaceItem(context, simpleCol)
                .withTitle(item1Title)
                .withTitle("Public item III")
                .withIssueDate("2013-10-17")
                .withAuthor("Smith, Donald Y.")
                .withSubject("ExtraEntry 3")
                .withSubmitter(eperson)
                .build();

        XmlWorkflowItem wfi1 = workflowService.start(context, workspaceItem1);
        XmlWorkflowItem wfi2 = workflowService.start(context, workspaceItem2);
        Item item1 = wfi1.getItem();
        Item item2 = wfi2.getItem();

        context.restoreAuthSystemState();

        getClient(token).perform(get("/api/submission/duplicates/search/findByItem?uuid=" + item1.getID()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                // Valid duplicates array
                .andExpect(jsonPath("$._embedded.potentialDuplicateResources", Matchers.hasSize(1)))
                // UUID of only array member matches item2 ID
                .andExpect(jsonPath("$._embedded.potentialDuplicateResources[0].uuid")
                        .value(item2.getID().toString()))
                // First item has subject and issue date metadata populated as expected
                .andExpect(jsonPath("$._embedded.potentialDuplicateResources[0]" +
                        ".metadata['dc.subject'][0].value")
                        .value(item2Subject))
                .andExpect(jsonPath("$._embedded.potentialDuplicateResources[0]" +
                        ".metadata['dc.date.issued'][0].value")
                        .value(item2IssueDate))
                // Does NOT have other metadata e.g. author, title
                .andExpect(jsonPath("$._embedded.potentialDuplicateResources[0]" +
                        ".metadata['dc.contributor.author']").doesNotExist())
                .andExpect(jsonPath("$._embedded.potentialDuplicateResources[0]" +
                        ".metadata['dc.title']").doesNotExist());
    }

    /**
     * Duplicates should be accessible via section data. Data should update as comparison value (title) is changed.
     *
     * @throws Exception
     */
    @Test
    public void submissionSectionDataTest() throws Exception {
        // Test publication
        context.turnOffAuthorisationSystem();

        Collection workspaceCollection =
                CollectionBuilder.createCollection(context, parentCommunity, "123456789/test-duplicate-detection")
                        .withName("Test Collection Workspace").build();

        // Ingest three example items with slightly different titles
        // item2 is 1 edit distance from item1 and item3
        // item1 and item3 are 2 edit distance from each other
        Item item1 = ItemBuilder.createItem(context, col)
                .withTitle("Submission section test I") // Public item I
                .withIssueDate(item1IssueDate)
                .withAuthor(item1Author)
                .withSubject(item1Subject)
                .build();
        Item item2 = ItemBuilder.createItem(context, col)
                .withTitle("Submission section test II")
                .withIssueDate(item2IssueDate)
                .withAuthor("Smith, Donald X.")
                .withSubject(item2Subject)
                .build();
        Item item3 = ItemBuilder.createItem(context, col)
                .withTitle("Submission section test III")
                .withIssueDate("2013-10-17")
                .withAuthor("Smith, Donald Y.")
                .withSubject("ExtraEntry 3")
                .build();
        // Create a new workspace item with a similar title to Item 1 (1 edit distance). Reuse other items
        // metadata for the rest, as it is not relevant.
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, workspaceCollection)
                .withTitle("Submission section test 1")
                .withSubject(item2Subject)
                .withIssueDate(item2IssueDate)
                .withAuthor(item1Author)
                .withSubmitter(eperson)
                .build();
        String submitterToken = getAuthToken(eperson.getEmail(), password);
        context.restoreAuthSystemState();

        getClient(submitterToken).perform(get("/api/submission/workspaceitems/" + workspaceItem.getID()))
                .andExpect(status().isOk())
                // The duplicates section is present
                .andExpect(jsonPath("$.sections.duplicates").exists())
                // There is a potentialDuplicates array in the section data of size 1
                .andExpect(jsonPath("$.sections.duplicates.potentialDuplicates", Matchers.hasSize(1)))
                // The UUID of the first duplicate matches item 1 (which is 1 distance from this new title)
                .andExpect(jsonPath("$.sections.duplicates.potentialDuplicates[0].uuid")
                        .value(item1.getID().toString()))
                // Metadata for subject and issue date is populated as expected
                .andExpect(jsonPath("$.sections.duplicates.potentialDuplicates[0]" +
                        ".metadata['dc.subject'][0].value")
                        .value(item1Subject))
                .andExpect(jsonPath("$.sections.duplicates.potentialDuplicates[0]" +
                        ".metadata['dc.date.issued'][0].value")
                        .value(item1IssueDate))
                // Metadata for other metadata fields has not been copied across, as expected
                .andExpect(jsonPath("$.sections.duplicates.potentialDuplicates[0]" +
                        ".metadata['dc.contributor.author']").doesNotExist())
                .andExpect(jsonPath("$.sections.duplicates.potentialDuplicates[0]" +
                        ".metadata['dc.title']").doesNotExist());

        List<Operation> updateOperations = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "Submission section test II");
        updateOperations.add(new ReplaceOperation("/sections/traditionalpageone/dc.title/0", value));
        String patchBody = getPatchContent(updateOperations);
        getClient(submitterToken).perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist());

        // Now there should be 3 results
        getClient(submitterToken).perform(get("/api/submission/workspaceitems/" + workspaceItem.getID()))
                .andExpect(status().isOk())
                // The duplicates section is present
                .andExpect(jsonPath("$.sections.duplicates").exists())
                // There is a potentialDuplicates array in the section data (even if empty)
                .andExpect(jsonPath("$.sections.duplicates.potentialDuplicates", Matchers.hasSize(3)));

        // Now, change the title to something completely different
        updateOperations = new ArrayList<>();
        value.put("value", "Research article");
        updateOperations.add(new ReplaceOperation("/sections/traditionalpageone/dc.title/0", value));
        patchBody = getPatchContent(updateOperations);
        getClient(submitterToken).perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist());

        // Now there should be NO results
        getClient(submitterToken).perform(get("/api/submission/workspaceitems/" + workspaceItem.getID()))
                .andExpect(status().isOk())
                // The duplicates section is present
                .andExpect(jsonPath("$.sections.duplicates").exists())
                // There is a potentialDuplicates array in the section data (even if empty)
                .andExpect(jsonPath("$.sections.duplicates.potentialDuplicates", Matchers.hasSize(0)));
    }

    /**
     * If there is a potential duplicate that is also in submission (workspace item), it will
     * ONLY be shown if the current user is the submitter / item owner.
     *
     * @throws Exception
     */
    @Test
    public void submissionSectionWorkspaceItemVisibilityTest() throws Exception {
        // Test publication
        context.turnOffAuthorisationSystem();
        // Create a new collection with handle that maps to the test-duplicate-detection submission config
        col = CollectionBuilder.createCollection(context, parentCommunity, "123456789/test-duplicate-detection")
                .withName("Test Collection with Duplicate Detection")
                .withWorkflowGroup(1, admin)
                .build();
        // Create a new workspace item with a similar title to Item 1 (1 edit distance). Reuse other items
        // metadata for the rest, as it is not relevant.
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, col)
                .withTitle("Unique title")
                .withSubject(item1Subject)
                .withIssueDate(item1IssueDate)
                .withAuthor(item1Author)
                .withSubmitter(eperson)
                .build();
        WorkspaceItem workspaceItem2 = WorkspaceItemBuilder.createWorkspaceItem(context, col)
                .withTitle("Unique title")
                .withSubject(item2Subject)
                .withIssueDate(item2IssueDate)
                .withAuthor(item1Author)
                .withSubmitter(eperson)
                .build();
        WorkspaceItem workspaceItem3 = WorkspaceItemBuilder.createWorkspaceItem(context, col)
                .withTitle("Unique title")
                .withSubject("asdf")
                .withIssueDate("2000-01-01")
                .withAuthor("asdfasf")
                .withSubmitter(admin)
                .build();
        String submitterToken = getAuthToken(eperson.getEmail(), password);

        context.restoreAuthSystemState();

        // Even though there are 3 items with the same title, this 'eperson' user should only see 1 duplicate
        // as workspaceItem3 is owned by a different submitter, and self-references are skipped
        getClient(submitterToken).perform(get("/api/submission/workspaceitems/" + workspaceItem.getID()))
                .andExpect(status().isOk())
                // The duplicates section is present
                .andExpect(jsonPath("$.sections.duplicates").exists())
                // There is a potentialDuplicates array in the section data of size 1
                .andExpect(jsonPath("$.sections.duplicates.potentialDuplicates", Matchers.hasSize(1)))
                // The UUID of the first duplicate matches item 1 (which is 1 distance from this new title)
                .andExpect(jsonPath("$.sections.duplicates.potentialDuplicates[0].uuid")
                        .value(workspaceItem2.getItem().getID().toString()));
    }

    /**
     * If there is a potential duplicate that is also in workflow, it will
     * ONLY be shown if the current user is in a workflow group for step 1, 2, or 3, or is an admin, or otherwise
     * has READ permission
     *
     * @throws Exception
     */
    @Test
    public void submissionSectionWorkflowItemVisibilityTest() throws Exception {

        context.turnOffAuthorisationSystem();
        // Create a new collection with handle that maps to the test-duplicate-detection submission config
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection workflowCol = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Test Collection with Duplicate Detection")
                .withWorkflowGroup("reviewer", admin)
                .build();

        XmlWorkflowItem workflowItem1 = WorkflowItemBuilder.createWorkflowItem(context, workflowCol)
                .withTitle("Unique title")
                .withSubmitter(anotherEPerson)
                .build();
        XmlWorkflowItem workflowItem2 = WorkflowItemBuilder.createWorkflowItem(context, workflowCol)
                .withTitle("Unique title")
                .withSubmitter(eperson)
                .build();
        context.restoreAuthSystemState();

        context.setCurrentUser(admin);
        String reviewerToken = getAuthToken(admin.getEmail(), password);

        // The reviewer should be able to see the workflow item as a potential duplicate of the test item
        getClient(reviewerToken).perform(get("/api/submission/duplicates/search/findByItem?uuid="
                        + workflowItem1.getItem().getID()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                // Valid duplicates array
                .andExpect(jsonPath("$._embedded.potentialDuplicateResources", Matchers.hasSize(1)))
                // UUID of only array member matches the new workflow item ID
                .andExpect(jsonPath("$._embedded.potentialDuplicateResources[0].uuid")
                        .value(workflowItem2.getItem().getID().toString()));

        // Another random user will NOT see this
        getClient(getAuthToken(anotherEPerson.getEmail(), password))
                .perform(get("/api/submission/duplicates/search/findByItem?uuid="
                        + workflowItem1.getItem().getID()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                // Valid duplicates array
                .andExpect(jsonPath("$._embedded.potentialDuplicateResources").doesNotExist());
    }

}
