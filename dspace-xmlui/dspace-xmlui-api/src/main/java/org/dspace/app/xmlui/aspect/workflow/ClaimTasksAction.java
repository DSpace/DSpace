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
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

/**
 * Claim all the selected workflows. This action is used by the 
 * submission page, when the user clicks the claim tasks button.
 * 
 * @author Scott Phillips
 */
public class ClaimTasksAction extends AbstractAction
{

    /**
     * @param redirector
     * @param resolver
     * @param objectModel
     *            Cocoon's object model
     * @param source
     * @param parameters
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
    			
    			int state = workflowItem.getState();
    			// Only unclaim tasks that are already claimed.
    			if ( state == WorkflowManager.WFSTATE_STEP1POOL || 
    				 state == WorkflowManager.WFSTATE_STEP2POOL || 
    				 state == WorkflowManager.WFSTATE_STEP3POOL)
    			{
    				WorkflowManager.claim(context, workflowItem, context.getCurrentUser());
    			}
    		}

    		context.commit();
    	}
    	
    	return null;
    }

}
