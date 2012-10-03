package org.dspace.app.xmlui.aspect.discovery;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.workflow.ClaimedTask;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;
import org.dspace.workflow.WorkflowRequirementsManager;

import java.util.List;
import java.util.Map;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 15-sep-2011
 * Time: 9:37:23
 */
public class BackToPoolAction extends AbstractAction {

    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters) throws Exception {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Context context = ContextUtil.obtainContext(objectModel);

        context.turnOffAuthorisationSystem();
        //Retrieve the reason
        WorkflowItem wfItem = WorkflowItem.findByItemId(context, Util.getIntParameter(request, "itemID"));
        String reason = request.getParameter("reason");

        if(wfItem != null){
            //Reject the item
            List<ClaimedTask> claimedTasks = ClaimedTask.findByWorkflowId(context, wfItem.getID());

            //Remove all the claimed tasks
            for (ClaimedTask claimedTask : claimedTasks) {
                WorkflowManager.deleteClaimedTask(context, wfItem, claimedTask);
                WorkflowRequirementsManager.removeClaimedUser(context, wfItem, context.getCurrentUser(), claimedTask.getStepID());
                wfItem.update();
            }
            context.commit();
        }
        context.restoreAuthSystemState();
        return null;
    }
}
