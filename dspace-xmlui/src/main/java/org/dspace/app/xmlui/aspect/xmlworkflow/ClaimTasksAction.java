/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.xmlworkflow;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.WorkflowFactory;
import org.dspace.xmlworkflow.XmlWorkflowManager;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

import java.util.Map;

/**
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class ClaimTasksAction extends AbstractAction {
    @Override
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters) throws Exception {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Context context = ContextUtil.obtainContext(objectModel);

    	// Or the user selected a checkbox full of workflow IDs
    	int[] workflowIDs = Util.getIntParameters(request, "workflowID");
    	if (workflowIDs != null)
    	{
            for (int workflowID : workflowIDs)
            {
                PoolTask poolTask = PoolTask.findByWorkflowIdAndEPerson(context, workflowID, context.getCurrentUser().getID());
                XmlWorkflowItem workflowItem = XmlWorkflowItem.find(context, workflowID);
                Workflow workflow = WorkflowFactory.getWorkflow(workflowItem.getCollection());

                WorkflowActionConfig currentAction = workflow.getStep(poolTask.getStepID()).getActionConfig(poolTask.getActionID());
                XmlWorkflowManager.doState(context, context.getCurrentUser(), request, workflowID, workflow, currentAction);
            }
            context.commit();
        }


        return null;
    }
}
