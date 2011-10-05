/*
 * ClaimTasksAction.java
 *
 * Version: $Revision: 3705 $
 *
 * Date: $Date: 2009-04-11 17:02:24 +0000 (Sat, 11 Apr 2009) $
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

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.workflow.*;
import org.dspace.workflow.actions.Action;
import org.dspace.workflow.actions.WorkflowActionConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Claim all the selected workflows. This action is used by the 
 * submission page, when the user clicks the claim tasks botton.
 * 
 * @author Scott Phillips
 *
 * The user interface for the claim task, this has been adjusted to support the dryad data model 
 */
public class ClaimTasksAction extends AbstractAction
{
    private static final Logger log = Logger.getLogger(ClaimTasksAction.class);

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
    	String[] selectedCheckboxes = request.getParameterValues("workflowandstepID");
        Map<Integer, WorkflowItem> workflowItems = new HashMap<Integer, WorkflowItem>();
        for (int i = 0; i < selectedCheckboxes.length; i++) {
            String checkboxVal = selectedCheckboxes[i];
            try{
                WorkflowItem wf = WorkflowItem.find(context, Integer.parseInt(checkboxVal.split(":")[0]));
                //Retrieve the data package for this workflowItem
                if(DryadWorkflowUtils.isDataPackage(wf)){
                    //Add it to the list
                    workflowItems.put(wf.getID(), wf);
                }else{
                    //Retrieve our data package & add that to the list
                    Item dataPackage = DryadWorkflowUtils.getDataPackage(context, wf.getItem());
                    //Retrieve our workflowitem for the data package
                    WorkflowItem dataPackageWf = WorkflowItem.findByItemId(context, dataPackage.getID());
                    workflowItems.put(dataPackageWf.getID(), dataPackageWf);
                }

            }catch (Exception e){
                log.error("Error while claiming task", e);                
            }
        }
        Map<Integer, PoolTask> tasksByWorkflowItemId = new HashMap<Integer, PoolTask>();
        List<PoolTask> tasks = PoolTask.findByEperson(context, context.getCurrentUser().getID());
        //Now that we have all our tasks put em in a map with as a key the workflowitem id so we can easely match on them
        for (PoolTask poolTask : tasks) {
            tasksByWorkflowItemId.put(poolTask.getWorkflowItemID(), poolTask);
        }


        //We have retrieved all our data packages we want to accept, loop over em & accept em
        for(int wfId : workflowItems.keySet()){
            WorkflowItem wf = workflowItems.get(wfId);
            //Retrieve our task
            PoolTask poolTask = tasksByWorkflowItemId.get(wfId);

            if(poolTask == null)
                continue;

            Workflow workflow = WorkflowFactory.getWorkflow(wf.getCollection());
            WorkflowActionConfig action = workflow.getStep(poolTask.getStepID()).getActionConfig(poolTask.getActionID());
            //Execute our action
            WorkflowManager.doState(context, context.getCurrentUser(), request, wfId, workflow, action);
        }
        //Commit everything & we are done
        context.commit();
        /*if (workflowIDs != null)
    	{
    		for (String workflowID : workflowIDs)
    		{
    			WorkflowItem workflowItem = WorkflowItem.find(context, Integer.valueOf(workflowID));
    			
    			int state = workflowItem.getState();
    			// Only unclaim tasks that are allready claimed.
    			if ( state == WorkflowManager.WFSTATE_STEP1POOL || 
    				 state == WorkflowManager.WFSTATE_STEP2POOL || 
    				 state == WorkflowManager.WFSTATE_STEP3POOL)
    			{
    				WorkflowManager.claim(context, workflowItem, context.getCurrentUser());
    			}
    		}

    	}
    	*/
    	return null;
    }

}
