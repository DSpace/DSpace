package org.dspace.xmlworkflow.state.actions;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import java.util.List;

import edu.umd.lib.dspace.xmlworkflow.state.actions.DataCommunityCollectionMappingAction;
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

public class DataCommunityCollectionMappingIT extends AbstractIntegrationTestWithDatabase {
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

        // Data Community community/collection item may be mapped to
        Community dataCommunityCommunity = CommunityBuilder.createCommunity(context)
                                                .withName("UMD Data Community").build();
        CollectionBuilder.createCollection(context, dataCommunityCommunity)
                .withName("UMD Data Collection").build();

        // The WorkspaceItem for the workflow
        wsi = WorkspaceItemBuilder.createWorkspaceItem(context, baseCollection)
            .withTitle("Test item")
            .withIssueDate("2019-03-06")
            .build();

        // The actual Item
        item = wsi.getItem();

        // Use handle of dataCommunityCommunity object as DATA_COMMUNITY_HANDLE_PROPERTY value
        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        configurationService.setProperty(
            DataCommunityCollectionMappingAction.DATA_COMMUNITY_HANDLE_PROPERTY,
            dataCommunityCommunity.getHandle()
        );

        context.restoreAuthSystemState();
    }

    @Test
    public void itemNotAdded_WhenHandlePropertyNotConfigured() throws Exception {
        // Clear the DATA_COMMUNITY_HANDLE_PROPERTY
        DSpaceConfigurationService configService =
            (DSpaceConfigurationService) DSpaceServicesFactory.getInstance().getConfigurationService();
        configService.clearConfig(
            DataCommunityCollectionMappingAction.DATA_COMMUNITY_HANDLE_PROPERTY);

        submitItemWithType("Dataset");
        assertThat(getCollectionNames(item), not(hasItem("UMD Data Collection")));
    }

    @Test
    public void itemNotAdded_WhenCommunityDoesNotExit() throws Exception {
        DSpaceConfigurationService configService =
            (DSpaceConfigurationService) DSpaceServicesFactory.getInstance().getConfigurationService();
        configService.setProperty(
            DataCommunityCollectionMappingAction.DATA_COMMUNITY_HANDLE_PROPERTY,
            "community_does_not_exist"
        );

        submitItemWithType("Dataset");
        assertThat(getCollectionNames(item), not(hasItem("UMD Data Collection")));
    }


    @Test
    public void itemAdded_WhenItemType_IsDataset() throws Exception {
        submitItemWithType("Dataset");
        assertThat(getCollectionNames(item), hasItem("UMD Data Collection"));
    }

    @Test
    public void itemAdded_WhenItemType_IsSoftware() throws Exception {
        submitItemWithType("Software");
        assertThat(getCollectionNames(item), hasItem("UMD Data Collection"));
    }

    @Test
    public void itemNotAdded_WhenItemType_AnythingElse() throws Exception {
        submitItemWithType("Article");
        assertThat(getCollectionNames(item), not(hasItem("UMD Data Collection")));
    }

    @Test
    public void itemNotAdded_WhenItemType_IsNull() throws Exception {
        submitItemWithType(null);
        assertThat(getCollectionNames(item), not(hasItem("UMD Data Collection")));
    }

    @Test
    public void itemNotAdded_WhenItemType_IsEmptyString() throws Exception {
        submitItemWithType("");
        assertThat(getCollectionNames(item), not(hasItem("UMD Data Collection")));
    }

    @Test
    public void itemNotAdded_WhenSubmissionValue_IsSomeOtherString() throws Exception {
        submitItemWithType("random string");
        assertThat(getCollectionNames(item), not(hasItem("UMD Data Collection")));
    }

    /**
     * Sets the item type value on the item, and activates workflow
     *
     * @param itemType the String representing the item type, drawn from the
     * umd_common_types "stored-value" entries in dspace/config/submission-forms.xml
     */
    protected void submitItemWithType(String itemType) throws Exception {
        ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        itemService.setMetadataSingleValue(
            context, item, "dc", "type", null, null, itemType);

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
