/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.workflow;

import java.util.Map;

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
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

/**
 * Unclaim all the selected workflows. This action returns these
 * tasks to the general pool for other users to select from.
 * 
 * 
 * @author Scott Phillips
 */
public class UnclaimTasksAction extends AbstractAction
{
    private static final Logger log = Logger.getLogger(UnclaimTasksAction.class);

    /**
     * @param redirector
     *            un-used.
     * @param resolver
     * @param objectModel
     *            Cocoon's object model
     */
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Context context = ContextUtil.obtainContext(objectModel);
        
    	// Or the user selected a checkbox full of workflow IDs
    	String[] workflowIDs = request.getParameterValues("workflowID");
    	if (workflowIDs != null)
    	{
    		for (String workflowID : workflowIDs)
    		{
    			WorkflowItem workflowItem = WorkflowItem.find(context, Integer.valueOf(workflowID));
    			//workflowItem.get
    			
    			int state = workflowItem.getState();
    			// only claim tasks that are in the pool.
    			if ( state == WorkflowManager.WFSTATE_STEP1 || 
    				 state == WorkflowManager.WFSTATE_STEP2 || 
    				 state == WorkflowManager.WFSTATE_STEP3 )
    			{
                    log.info(LogManager.getHeader(context, "unclaim_workflow", "workflow_id=" + workflowItem.getID()));
    				WorkflowManager.unclaim(context, workflowItem, context.getCurrentUser());
    			}
    		}
    		context.commit();
    	}
    	
    	return null;
    }

}
