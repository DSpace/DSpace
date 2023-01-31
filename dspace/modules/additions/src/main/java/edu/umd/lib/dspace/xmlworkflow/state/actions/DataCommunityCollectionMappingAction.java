package edu.umd.lib.dspace.xmlworkflow.state.actions;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.processingaction.ProcessingAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * Processing action for mapping items into the "UMD Data Community"
 * community, if the item type is "Dataset" or "Software"
 * The item will be mapped into every collection of the Community pointed to by
 * the "data.community.handle" configuration property.
 */
public class DataCommunityCollectionMappingAction extends ProcessingAction {
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(
        DataCommunityCollectionMappingAction.class);

    protected ConfigurationService configurationService;
    protected HandleService handleService;
    protected CollectionService collectionService;

    /**
     * Configuration property containing the handle for the Data Community community
     */
    public static String DATA_COMMUNITY_HANDLE_PROPERTY = "data.community.handle";

    @Override
    public void activate(Context c, XmlWorkflowItem wf)
            throws SQLException, IOException, AuthorizeException, WorkflowException {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        handleService = HandleServiceFactory.getInstance().getHandleService();
        collectionService = ContentServiceFactory.getInstance().getCollectionService();
    }

    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
            throws SQLException, AuthorizeException, IOException, WorkflowException {
        Item item = wfi.getItem();
        if (isDataCommunityItem(item)) {
            log.info("Submitting item '{}' for Data Community", item.getID());
            mapToDataCommunity(c, item);
        } else {
            log.info("Item '{}' not submitted for Data Community", item.getID());
        }
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME,
                ActionResult.OUTCOME_COMPLETE);
    }

    @Override
    public List<String> getOptions() {
        return Collections.emptyList();
    }

    /**
     * Maps the given Item into the collections contained in the
     * Data Community community.
     *
     * @param context the current Context
     * @param item the Item to map
     */
    protected void mapToDataCommunity(Context context, Item item) throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        try {
            String communityHandle = configurationService.getProperty(
                DATA_COMMUNITY_HANDLE_PROPERTY);

            if (communityHandle == null) {
                log.warn("Property '{}' not set, skipping data community mapping for item '{}",
                    DATA_COMMUNITY_HANDLE_PROPERTY,
                    item.getHandle());
                return;
            }

            Community community = (Community) handleService.resolveToObject(context, communityHandle);
            if (community == null) {
                log.warn("Skipping data community mapping for item '{}'. No community for handle '{}'",
                    item, communityHandle
                );
                return;
            }

            List<Collection> collections = community.getCollections();
            for (Collection collection : collections) {
                log.info("Mapping item '{}' to collection '{}'", item.getID(), collection.getID());
                collectionService.addItem(context, collection, item);
                collectionService.update(context, collection);
                itemService.update(context, item);
            }
        } finally {
            context.restoreAuthSystemState();
        }
    }

    /**
     * Returns true if the given Item should be mapped into the Data Community
     * collection, false otherwise.
     *
     * @param item the Item to test
     * @return true if the given Item should be mapped into the Data Community
     * collection, false otherwise.
     */
    protected boolean isDataCommunityItem(Item item) {
        List<MetadataValue> valueList = itemService.getMetadata(
            item, "dc", "type", Item.ANY, Item.ANY);
        if (!valueList.isEmpty()) {
            MetadataValue first = valueList.get(0);
            String value = first.getValue();
            boolean isDataCommunityItem = "Software".equals(value) || "Dataset".equals(value);
            log.debug("value={}, isDataCommunityItem={}", value, isDataCommunityItem);
            return isDataCommunityItem;
        }

        return false;
    }
}
