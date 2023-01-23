package org.dspace.xmlworkflow.state.actions;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import java.util.List;

import edu.umd.lib.dspace.xmlworkflow.state.actions.EquitableAccessCollectionMappingAction;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.junit.Before;
import org.junit.Test;

public class EquitableAccessCollectionMappingIT extends AbstractIntegrationTestWithDatabase {
    protected XmlWorkflowService xmlWorkflowService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService();

    private WorkspaceItem wsi;
    private Item item;


    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        // Base community/collection item will be submitted to
        Community baseCommunity = CommunityBuilder.createCommunity(context)
                                    .withName("Base Community").build();
        Collection baseCollection = CollectionBuilder.createCollection(context, baseCommunity)
                                        .withName("Base Collection")
                                        .build();

        // Equitable access community/collection item may be mapped to
        Community equitableAccessCommunity = CommunityBuilder.createCommunity(context)
                                                .withName("Equitable Access").build();
        CollectionBuilder.createCollection(context, equitableAccessCommunity)
                .withName("Equitable Access Collection").build();

        // The WorkspaceItem for the workflow
        wsi = WorkspaceItemBuilder.createWorkspaceItem(context, baseCollection)
            .withTitle("Test item")
            .withIssueDate("2019-03-06")
            .build();

        // The actual Item
        item = wsi.getItem();

        // Use handle of equitableAccessCommunity object as EQUITABLE_ACCESS_POLICY_HANDLE_PROPERTY value
        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        configurationService.setProperty(
            EquitableAccessCollectionMappingAction.EQUITABLE_ACCESS_POLICY_COMMUNITY_HANDLE_PROPERTY,
            equitableAccessCommunity.getHandle()
        );

        context.restoreAuthSystemState();
    }

    @Test
    public void itemNotAdded_WhenHandlePropertyNotConfigured() throws Exception {
        // Clear the EQUITABLE_ACCESS_POLICY_HANDLE_PROPERTY
        DSpaceConfigurationService configService =
            (DSpaceConfigurationService) DSpaceServicesFactory.getInstance().getConfigurationService();
        configService.clearConfig(
            EquitableAccessCollectionMappingAction.EQUITABLE_ACCESS_POLICY_COMMUNITY_HANDLE_PROPERTY);

        submitItemWithEquitableAccessPolicy("Yes");
        assertThat(getCollectionNames(item), not(hasItem("Equitable Access Collection")));
    }

    @Test
    public void itemNotAdded_WhenCommunityDoesNotExit() throws Exception {
        DSpaceConfigurationService configService =
            (DSpaceConfigurationService) DSpaceServicesFactory.getInstance().getConfigurationService();
        configService.setProperty(
            EquitableAccessCollectionMappingAction.EQUITABLE_ACCESS_POLICY_COMMUNITY_HANDLE_PROPERTY,
            "community_does_not_exist"
        );

        submitItemWithEquitableAccessPolicy("Yes");
        assertThat(getCollectionNames(item), not(hasItem("Equitable Access Collection")));
    }


    @Test
    public void itemAdded_WhenSubmissionValue_IsYes() throws Exception {
        submitItemWithEquitableAccessPolicy("Yes");
        assertThat(getCollectionNames(item), hasItem("Equitable Access Collection"));
    }

    @Test
    public void itemNotAdded_WhenSubmissionValue_IsNo() throws Exception {
        submitItemWithEquitableAccessPolicy("No");
        assertThat(getCollectionNames(item), not(hasItem("Equitable Access Collection")));
    }

    @Test
    public void itemNotAdded_WhenSubmissionValue_IsNull() throws Exception {
        submitItemWithEquitableAccessPolicy(null);
        assertThat(getCollectionNames(item), not(hasItem("Equitable Access Collection")));
    }

    @Test
    public void itemNotAdded_WhenSubmissionValue_IsEmptyString() throws Exception {
        submitItemWithEquitableAccessPolicy("");
        assertThat(getCollectionNames(item), not(hasItem("Equitable Access Collection")));
    }

    @Test
    public void itemNotAdded_WhenSubmissionValue_IsSomeOtherString() throws Exception {
        submitItemWithEquitableAccessPolicy("random string");
        assertThat(getCollectionNames(item), not(hasItem("Equitable Access Collection")));
    }

    /**
     * Sets the Equitable Access submission value on the item, and activates workflow
     *
     * @param submissionValue the String representing the user response on the submission form.
     */
    protected void submitItemWithEquitableAccessPolicy(String submissionValue) throws Exception {
        ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        itemService.setMetadataSingleValue(
            context, item, "local", "equitableAccessSubmission", null, null, submissionValue);

        xmlWorkflowService.startWithoutNotify(context, wsi);
    }

    /**
     * Returns a List of collection names (from "getCollections()") for the
     * given Item.
     *
     * @param item the Item to return the collection names of
     */
    protected List<String> getCollectionNames(Item item) {
        return item.getCollections().stream().map(c -> c.getName()).collect(toList());
    }
}
