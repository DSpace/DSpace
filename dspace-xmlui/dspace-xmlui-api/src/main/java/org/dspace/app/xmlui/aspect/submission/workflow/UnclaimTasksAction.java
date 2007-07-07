/*
 * UnclaimTasksAction.java
 *
 * Version: $Revision: 1.1 $
 *
 * Date: $Date: 2006/04/06 15:15:59 $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.app.xmlui.aspect.submission.workflow;

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
 * Unclaim all the selected workflows. This action returns these
 * tasks to the general pool for other users to select from.
 * 
 * 
 * @author Scott Phillips
 */
public class UnclaimTasksAction extends AbstractAction
{

    /**
     * @param pattern
     *            un-used.
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
    				WorkflowManager.unclaim(context, workflowItem, context.getCurrentUser());
    			}
    		}
    		context.commit();
    	}
    	
    	return null;
    }

}
