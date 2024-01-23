/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
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
import org.dspace.eperson.factory.EPersonServiceFactory;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private Collection col;
    private Collection simpleCol;
    private final String item1IssueDate = "2011-10-17";
    private final String item1Subject = "ExtraEntry 1";
    private final String item1Title = "Public item I";
    private final String item1Author = "Smith, Donald";
    private final String item2Subject = "ExtraEntry 2";
    private final String item2IssueDate = "2012-10-17";
    private EPerson anotherEPerson;

    private static Logger log = LogManager.getLogger();

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Temporarily enable duplicate detection and set signature distance to 1
        configurationService.setProperty("duplicate.enable", true);
        configurationService.setProperty("duplicate.signature.distance", 1);
        configurationService.setProperty("duplicate.signature.normalise.lowercase", true);
        configurationService.setProperty("duplicate.signature.normalise.whitespace", true);
        configurationService.setProperty("duplicate.signature.field", "item_signature");
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
        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
        anotherEPerson = ePersonService.findByEmail(context, "test-another-user@email.com");
        if (anotherEPerson == null) {
            anotherEPerson = ePersonService.create(context);
            anotherEPerson.setFirstName(context, "first");
            anotherEPerson.setLastName(context, "last");
            anotherEPerson.setEmail("test-another-user@email.com");
            anotherEPerson.setCanLogIn(true);
            anotherEPerson.setLanguage(context, I18nUtil.getDefaultLocale().getLanguage());
            ePersonService.setPassword(anotherEPerson, password);
            // actually save the eperson to unit testing DB
            ePersonService.update(context, anotherEPerson);
        }
        //context.setDispatcher("noindex");
        context.restoreAuthSystemState();
    }

    @Test
    public void itemsContainDuplicatesLinkTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        log.error("EPERSON FULL NAME IS " + eperson.getFullName());

        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem1 = WorkspaceItemBuilder.createWorkspaceItem(context, simpleCol)
                .withTitle(item1Title)
                .withSubject(item1Subject)
                .withIssueDate(item1IssueDate)
                .withAuthor(item1Author)
                .withSubmitter(eperson)
                .build();
        XmlWorkflowItem wfi1 = workflowService.start(context, workspaceItem1);
        Item item1 = wfi1.getItem();
        context.restoreAuthSystemState();

        getClient(token).perform(get("/api/core/items/" + item1.getID()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._links.duplicates").exists());
    }

    @Test
    public void searchDuplicatesByLinkTest() throws Exception {
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
        log.error("EPERSON FULL NAME IS " + eperson.getFullName());
        XmlWorkflowItem wfi1 = workflowService.start(context, workspaceItem1);
        XmlWorkflowItem wfi2 = workflowService.start(context, workspaceItem2);
        Item item1 = wfi1.getItem();
        Item item2 = wfi2.getItem();

        context.restoreAuthSystemState();

        getClient(token).perform(get("/api/core/items/" + item1.getID() + "/duplicates"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                // Valid duplicates array
                .andExpect(jsonPath("$._embedded.duplicates", Matchers.hasSize(1)))
                // UUID of only array member matches item2 ID
                .andExpect(jsonPath("$._embedded.duplicates[0].uuid").value(item2.getID().toString()))
                // First item has subject and issue date metadata populated as expected
                .andExpect(jsonPath("$._embedded.duplicates[0].metadata['dc.subject'][0].value")
                        .value(item2Subject))
                .andExpect(jsonPath("$._embedded.duplicates[0].metadata['dc.date.issued'][0].value")
                        .value(item2IssueDate))
                // Does NOT have other metadata e.g. author, title
                .andExpect(jsonPath("$._embedded.duplicates[0].metadata['dc.contributor.author']").doesNotExist())
                .andExpect(jsonPath("$._embedded.duplicates[0].metadata['dc.title']").doesNotExist());
    }

    /**
     * Duplicates should be accessible via section data. Data should update as item signature (title) is changed.
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
                .withTitle(item1Title) // Public item I
                .withIssueDate(item1IssueDate)
                .withAuthor(item1Author)
                .withSubject(item1Subject)
                .build();
        Item item2 = ItemBuilder.createItem(context, col)
                .withTitle("Public item II")
                .withIssueDate(item2IssueDate)
                .withAuthor("Smith, Donald X.")
                .withSubject(item2Subject)
                .build();
        Item item3 = ItemBuilder.createItem(context, col)
                .withTitle("Public item III")
                .withIssueDate("2013-10-17")
                .withAuthor("Smith, Donald Y.")
                .withSubject("ExtraEntry 3")
                .build();
        // Create a new workspace item with a similar title to Item 1 (1 edit distance). Reuse other items
        // metadata for the rest, as it is not relevant.
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, workspaceCollection)
                .withTitle("Public item X")
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
                .andExpect(jsonPath("$.sections.duplicates.potentialDuplicates[0].metadata['dc.subject'][0].value")
                        .value(item1Subject))
                .andExpect(jsonPath("$.sections.duplicates.potentialDuplicates[0].metadata['dc.date.issued'][0].value")
                        .value(item1IssueDate))
                // Metadata for other metadata fields has not been copied across, as expected
                .andExpect(jsonPath("$.sections.duplicates.potentialDuplicates[0]" +
                        ".metadata['dc.contributor.author']").doesNotExist())
                .andExpect(jsonPath("$.sections.duplicates.potentialDuplicates[0]" +
                        ".metadata['dc.title']").doesNotExist());

        // Try to add ISBN (type bound to book and book chapter) - this should not work and instead we'll get
        // no JSON path for that field, because this item has no type yet
        List<Operation> updateOperations = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "Public item II");
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
        // Create a new collection with handle that maps to teh test-duplicate-detection submission config
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
     * ONLY be shown if the current user is in a worflow group for step 1, 2, or 3, or is an admin, or otherwise
     * has READ permission
     *
     * @throws Exception
     */
    @Test
    public void submissionSectionWorkflowItemVisibilityTest() throws Exception {

        context.turnOffAuthorisationSystem();
        // Create a new collection with handle that maps to teh test-duplicate-detection submission config
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
        getClient(reviewerToken).perform(get("/api/core/items/" + workflowItem1.getItem().getID() + "/duplicates"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                // Valid duplicates array
                .andExpect(jsonPath("$._embedded.duplicates", Matchers.hasSize(1)))
                // UUID of only array member matches the new workflow item ID
                .andExpect(jsonPath("$._embedded.duplicates[0].uuid").value(workflowItem2.getItem().getID().toString()));

        // Another random user will NOT see this
        getClient(getAuthToken(anotherEPerson.getEmail(), password))
                .perform(get("/api/core/items/" + workflowItem1.getItem().getID() + "/duplicates"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                // Valid duplicates array
                .andExpect(jsonPath("$._embedded.duplicates", Matchers.hasSize(0)));
    }

}
