package org.dspace.app.xmlui.aspect.submission.workflow;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.workflow.PoolTask;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;
import org.dspace.workflow.WorkflowRequirementsManager;

import java.util.Map;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 13-aug-2010
 * Time: 15:30:53
 */
public class UnclaimTasksAction extends AbstractAction
{

    private static final Logger log = Logger.getLogger(UnclaimTasksAction.class);

    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters) throws Exception {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Context context = ContextUtil.obtainContext(objectModel);

    	// Or the user selected a checkbox full of workflow IDs
    	String[] workflowIDs = request.getParameterValues("workflowandstepID");
    	if (workflowIDs != null)
    	{
    		for (String workflowID : workflowIDs)
    		{
                WorkflowItem workflowItem = WorkflowItem.find(context, Integer.valueOf(workflowID.split(":")[0]));

                PoolTask pooledTask = PoolTask.findByWorkflowIdAndEPerson(context, workflowItem.getID(), context.getCurrentUser().getID());
                WorkflowManager.deletePooledTask(context, workflowItem, pooledTask);

                WorkflowRequirementsManager.removeClaimedUser(context, workflowItem, context.getCurrentUser(), workflowID.split(":")[1]);
                log.info(LogManager.getHeader(context, "unclaim_workflow", "workflow_id=" + workflowItem.getID()));

            }
        }
        return null;
    }
}
