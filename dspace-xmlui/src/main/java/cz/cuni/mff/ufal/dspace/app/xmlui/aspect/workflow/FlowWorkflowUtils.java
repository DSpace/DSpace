/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.workflow;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.workflow.FlowUtils;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

/**
 * Utility methods to processes additional actions on worflow item. These methods are used
 * exclusively from the workflow flow scripts.
 *
 * @author Michal Josífko
 * modified for LINDAT/CLARIN
 */
public class FlowWorkflowUtils {

    private static final Logger log = Logger.getLogger(FlowWorkflowUtils.class);

    /**
     * Move workflow item to another collection
     *
     * @param context The current DSpace content
     * @param id The unique ID of the current workflow item
     * @param id The unique ID of the target collection
     * @param request The current request object
     */
    public static boolean processMoveWorkflowItem(Context context, String workflowID, String collectionID) throws SQLException, UIException, ServletException, AuthorizeException, IOException
    {
        boolean reclaimed = false;
        WorkflowItem wfi = FlowUtils.findWorkflow(context, workflowID);
        Collection sourceCollection = wfi.getCollection();
        Item item = wfi.getItem();
        Collection targetCollection = Collection.find(context, Integer.parseInt(collectionID));
        EPerson e = context.getCurrentUser();

        // change collection in workflow item
        if(!wfi.getCollection().equals(targetCollection)) {
            wfi.setCollection(targetCollection);
            wfi.getItem().clearMetadata("local", "branding", null, null);
            wfi.getItem().addMetadata("local", "branding", null, null, targetCollection.getPrincipalCommunity().getName());
            wfi.update();
        }

        // unclaim the task
        WorkflowManager.unclaim(context, wfi, e);
        try {
            // try to reclaim the task back
            FlowUtils.authorizeWorkflowItem(context, workflowID);
            WorkflowManager.claim(context, wfi, e);
            reclaimed = true;
        }
        catch(AuthorizeException ae) {
            // unable to reclaim the task - unsufficient privileges on new collection
            reclaimed = false;
        }

        context.commit();

        // Workflow item moved.  Log this information
        log.info(LogManager.getHeader(context, "move_workflow_item", "workflow_item_id=" + wfi.getID()
                + ", item_id=" + item.getID()
                + ", source collection_id=" + sourceCollection.getID()
                + ", target collection_id=" + targetCollection.getID()
                + ", eperson_id=" + e.getID()));

        return reclaimed;
    }
}
