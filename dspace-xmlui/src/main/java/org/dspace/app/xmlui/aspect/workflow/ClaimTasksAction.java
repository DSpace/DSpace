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
import org.dspace.workflowbasic.BasicWorkflowItem;
import org.dspace.workflowbasic.BasicWorkflowServiceImpl;
import org.dspace.workflowbasic.factory.BasicWorkflowServiceFactory;
import org.dspace.workflowbasic.service.BasicWorkflowItemService;
import org.dspace.workflowbasic.service.BasicWorkflowService;

/**
 * Claim all the selected workflows. This action is used by the 
 * submission page, when the user clicks the claim tasks button.
 * 
 * @author Scott Phillips
 */
public class ClaimTasksAction extends AbstractAction
{

	protected BasicWorkflowService basicWorkflowService = BasicWorkflowServiceFactory.getInstance().getBasicWorkflowService();
	protected BasicWorkflowItemService basicWorkflowItemService = BasicWorkflowServiceFactory.getInstance().getBasicWorkflowItemService();

    /**
     * Claim-tasks action.
     *
     * @param redirector unused.
     * @param resolver unused.
     * @param objectModel
     *            Cocoon's object model.
     * @param source unused.
     * @param parameters unused.
     * @return null.
     * @throws java.lang.Exception passed through.
     */
    @Override
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
    			BasicWorkflowItem workflowItem = basicWorkflowItemService.find(context, Integer.valueOf(workflowID));
    			
    			int state = workflowItem.getState();
    			// Only unclaim tasks that are already claimed.
    			if ( state == BasicWorkflowServiceImpl.WFSTATE_STEP1POOL ||
    				 state == BasicWorkflowServiceImpl.WFSTATE_STEP2POOL ||
    				 state == BasicWorkflowServiceImpl.WFSTATE_STEP3POOL)
    			{
					basicWorkflowService.claim(context, workflowItem, context.getCurrentUser());
    			}
    		}
    	}
    	
    	return null;
    }

}
