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
 * Processing action for mapping items into the "Equitable Access Policy"
 * community, if the "local.equitableAccessSubmission" metadata value is
 * "Yes". The item will be mapped into every collection of the
 * Community pointed to by the "equitable_access_policy.community.handle"
 * configuration property.
 */
public class EquitableAccessCollectionMappingAction extends ProcessingAction {
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(
        EquitableAccessCollectionMappingAction.class);

    protected ConfigurationService configurationService;
    protected HandleService handleService;
    protected CollectionService collectionService;

    /**
     * Configuration property containing the handle for the Equitable Access community
     */
    public static String EQUITABLE_ACCESS_POLICY_COMMUNITY_HANDLE_PROPERTY = "equitable_access_policy.community.handle";

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
        if (isEquitableAccessSubmission(item)) {
            log.info("Submitting item '{}' for Equitable Access", item.getID());
            mapToEquitableAccess(c, item);
        } else {
            log.info("Item '{}' not submitted for Equitable Access", item.getID());
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
     * Equitable Access community.
     *
     * @param context the current Context
     * @param item the Item to map
     */
    protected void mapToEquitableAccess(Context context, Item item) throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        try {
            String communityHandle = configurationService.getProperty(
                                        EQUITABLE_ACCESS_POLICY_COMMUNITY_HANDLE_PROPERTY);

            if (communityHandle == null) {
                log.warn("Property '{}' not set, skipping equitable access mapping for item '{}",
                    EQUITABLE_ACCESS_POLICY_COMMUNITY_HANDLE_PROPERTY,
                    item.getHandle());
                return;
            }

            Community community = (Community) handleService.resolveToObject(context, communityHandle);
            if (community == null) {
                log.warn("Skipping equitable access mapping for item '{}'. No community for handle '{}'",
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
     * Returns true if the given Item should be mapped into the Equitable Access
     * collection, false otherwise.
     *
     * @param item the Item to test
     * @return true if the given Item should be mapped into the Equitable Access
     * collection, false otherwise.
     */
    protected boolean isEquitableAccessSubmission(Item item) {
        List<MetadataValue> valueList = itemService.getMetadata(
            item, "local", "equitableAccessSubmission", Item.ANY, Item.ANY);
        if (!valueList.isEmpty()) {
            MetadataValue first = valueList.get(0);
            String value = first.getValue();
            boolean isEquitableAccessSubmission = "Yes".equals(value);
            log.debug("value={}, isEquitableAccessSubmission={}", value, isEquitableAccessSubmission);
            return isEquitableAccessSubmission;
        }

        return false;
    }
}
