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
import org.dspace.xmlworkflow.XmlWorkflowFactoryImpl;
import org.dspace.xmlworkflow.XmlWorkflowServiceImpl;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;

import java.util.Map;

/**
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class ClaimTasksAction extends AbstractAction {

    protected PoolTaskService poolTaskService = XmlWorkflowServiceFactory.getInstance().getPoolTaskService();
    protected XmlWorkflowItemService xmlWorkflowItemService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowItemService();
    protected XmlWorkflowFactory workflowFactory = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory();
    protected XmlWorkflowService xmlWorkflowService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService();

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
                XmlWorkflowItem workflowItem = xmlWorkflowItemService.find(context, workflowID);
                PoolTask poolTask = poolTaskService.findByWorkflowIdAndEPerson(context, workflowItem, context.getCurrentUser());
                Workflow workflow = workflowFactory.getWorkflow(workflowItem.getCollection());

                WorkflowActionConfig currentAction = workflow.getStep(poolTask.getStepID()).getActionConfig(poolTask.getActionID());
                xmlWorkflowService.doState(context, context.getCurrentUser(), request, workflowID, workflow, currentAction);
            }
        }


        return null;
    }
}
