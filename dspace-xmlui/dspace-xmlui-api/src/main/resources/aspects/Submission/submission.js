/*
 * submission.js
 *
 * Version: $Revision: 1.2 $
 *
 * Date: $Date: 2006/06/02 21:37:32 $
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

importClass(Packages.org.apache.cocoon.components.CocoonComponentManager);

importClass(Packages.org.dspace.handle.HandleManager);
importClass(Packages.org.dspace.core.Constants);
importClass(Packages.org.dspace.workflow.WorkflowItem);
importClass(Packages.org.dspace.content.WorkspaceItem);
importClass(Packages.org.dspace.authorize.AuthorizeManager);
importClass(Packages.org.dspace.license.CreativeCommons);

importClass(Packages.org.dspace.app.xmlui.utils.ContextUtil);
importClass(Packages.org.dspace.app.xmlui.aspect.submission.FlowUtils);


/**
 * Simple access method to access the current cocoon object model.
 */
function getObjectModel() 
{
  return CocoonComponentManager.getCurrentEnvironment().getObjectModel();
}

/**
 * Return the DSpace context for this request since each HTTP request generates
 * a new context this object should never be stored and instead allways accessed 
 * through this method so you are ensured that it is the correct one.
 */
function getDSContext()
{
	return ContextUtil.obtainContext(getObjectModel());
}

/**
 * Send the current page and wait for the flow to be continued. Use this method to add
 * a flow=true parameter. This allows the sitemap to filter out all flow related requests
 * from non flow related requests.
 */
function sendPageAndWait(uri,bizData)
{
    if (bizData == null)
        bizData = {};
        
    // just to remember where we came from.
    bizData["flow"] = "true";
    cocoon.sendPageAndWait(uri,bizData);
}

/**
 * Send the given page and DO NOT wait for the flow to be continued. Excution will 
 * proccede as normal. Use this method to add a flow=true parameter. This allows the 
 * sitemap to filter out all flow related requests from non flow related requests.
 */
function sendPage(uri,bizData)
{
    if (bizData == null)
        bizData = {};
    
    // just to remember where we came from.
    bizData["flow"] = "true";
    cocoon.sendPage(uri,bizData);
}







/**
 * Submission starting point.
 *
 * This is the entry point for all submission related flows, either resuming an old 
 * submission or starting a new one. If a new submission is being resumed then the 
 * workspace id should be passed as an HTTP parameter.
 */
function doSubmission() 
{
   var workspaceID = cocoon.request.get("workspaceID");
   
   if (workspaceID == null)
   {
       var handle = cocoon.parameters["handle"];
       if (handle == null)
           handle = cocoon.request.get("handle");
       
       var collectionSelected = false;
       do {
           if (handle != null)
           {
               var dso = HandleManager.resolveToObject(getDSContext(), handle);
               
               // Check that the dso is a collection
               if (dso != null && dso.getType() == Constants.COLLECTION)
               {
                   // Check that the user is able to submit
                   if (AuthorizeManager.authorizeActionBoolean(getDSContext(), dso, Constants.ADD))
                   {
                       // Construct a new workspace for this submission.
                       var workspace = WorkspaceItem.create(getDSContext(), dso, true);
                       workspaceID = workspace.getID();
                       
                       collectionSelected = true;
                        
                       break; // We don't need to ask them for a collection again.   
                   }
               }
           }
           
           sendPageAndWait("submit/selectCollectionStep", { "handle" : handle } );
       
           handle = cocoon.request.get("handle");
                     
       } while (collectionSelected == false)
      
       // Hand off to the master thingy....
       submissionControl(handle,"S"+workspaceID);
       
   }
   else
   {
       // Resume a previous submission
       
       // First check that the id is valid.
       var submitterID = WorkspaceItem.find(getDSContext(), workspaceID).getSubmitter().getID()
       var currentID = getDSContext().getCurrentUser().getID();
       if (submitterID == currentID)
       {
           // Get the collection handle for this item.
           var handle = WorkspaceItem.find(getDSContext(), workspaceID).getCollection().getHandle();
           
           // Record that this is a submission id, not a workflow id.
           workspaceID = "S"+workspaceID;
           do {
               sendPageAndWait("handle/"+handle+"/submit/resumeStep",{"id":workspaceID,"step":"0"});
               
               if (cocoon.request.get("submit_resume"))
               {
                   submissionControl(handle,workspaceID);
               }
               else if (cocoon.request.get("submit_cancel"))
               {
                   var contextPath = cocoon.request.getContextPath();
                   cocoon.redirectTo(contextPath+"/submissions",true);
                   getDSContext().complete();
                   cocoon.exit();
               }
               
               
           } while (1 == 1)
           
   
       }
   }

}

/**
 * This is the master flow control for submissions. This method decides
 * which steps are excuted in which order. Each step is called and does
 * not return until it's exit conditions have been meet. Then the standard
 * navigation buttons are checked and the step is either decreased or
 * increased accordingly. Special cases like jumping or saveing are also
 * handled here.
 *
 * FIXME: Note, this should probably read the order of the steps from a
 * configuration file. This will allow for more customized submission 
 * workflows.
 */
function submissionControl(handle, workspaceID) 
{
    
    // First find out how many describe pages there are:
    var numberOfDescribePages = 
       FlowUtils.getNumberOfDescribePages(getDSContext(),workspaceID);
    
    var step = 0;
    
    do { 
        // Loop forever, exit cases such as save, remove, or completed
        // will call cocoon.exit() stoping excecution.
        FlowUtils.setStepReached(getDSContext(),workspaceID,step);
    
        if ( step >= 1 && step <= numberOfDescribePages )
        {
            submitStepDescribe(handle,workspaceID,step);
        } 
        else 
        {
            switch (step)
            {
            case 0:
                submitStepInitial(handle,workspaceID,step);
                break;
            case numberOfDescribePages+1:
                submitStepUpload(handle,workspaceID,step);
                break;
            case numberOfDescribePages+2:
                submitStepReview(handle,workspaceID,step);
                break;
            case numberOfDescribePages+3:
                submitStepCCLicense(handle,workspaceID,step);
                break;
            case numberOfDescribePages+4:
                submitStepLicense(handle,workspaceID,step);
                break;
            }
        }
        
        if (cocoon.request.get("submit_next"))
        {
            step++;
        }
        else if (cocoon.request.get("submit_previous"))
        {
            step--;
        }
        else if (cocoon.request.get("submit_save"))
        {
            submitStepSaveOrRemove(handle,workspaceID,step);
        }
        
        var names = cocoon.request.getParameterNames();
        while (names.hasMoreElements())
        {
            var maxStep = FlowUtils.getMaximumStepReached(getDSContext(),workspaceID);
            var name = names.nextElement(); 
            if (name.startsWith("submit_jump_"))
            {
                var newStep = name.substring("submit_jump_".length);
                newStep = parseInt(newStep);
                if (newStep >= 0 && newStep <= maxStep)
                {
                   step = newStep;     
                }
            }
        }
        
    } while ( 1 == 1)

}

/**
 * This step is used when ever the user clicks save/cancel during the submission 
 * processes. We ask them if they would like to save the submission or remove it.
 */
function submitStepSaveOrRemove(handle,workspaceID,step)
{
    sendPageAndWait("handle/"+handle+"/submit/saveOrRemoveStep",{"id":workspaceID,"step":String(step)});
    
    FlowUtils.processSaveOrRemove(getDSContext(),workspaceID,cocoon.request);
    
    if (cocoon.request.get("submit_save"))
    {
       // Allready saved, just take them back to dspace home.
       var contextPath = cocoon.request.getContextPath();
       cocoon.redirectTo(contextPath+"/submissions",true);
       getDSContext().complete();
       cocoon.exit();
    }
    else if (cocoon.request.get("submit_remove"))
    {
        sendPage("handle/"+handle+"/submit/removedStep");
        cocoon.exit(); // We're done, Stop excecution.
    }
    
    // go back to submission control and continue.
}

/**
 * This step presents a set of initial questions to the user, such as mulitple titles,
 * or if it has been published before. Since there are no error conditions, the page 
 * only has checkboxes on it we can just return after processing.
 */
function submitStepInitial(handle,workspaceID,step)
{
    sendPageAndWait("handle/"+handle+"/submit/initialQuestionsStep",{"id":workspaceID,"step":String(step)});
    
    FlowUtils.processInitialQuestions(getDSContext(),workspaceID,cocoon.request);
}

/**
 * This step presents the user with a list of metadata fields to describe the item. There 
 * may be multiple describe steps in the overall submissions workflow, so when the user 
 * clicks next it will go up to the submissionControl method and may come right back here 
 * with another describe page.
 */
function submitStepDescribe(handle,workspaceID, step)
{
    var errors;
    var complete = false;
    do {
        
        sendPageAndWait("handle/"+handle+"/submit/describeStep",{"id":workspaceID, "step":String(step), "errors":errors});
        
        // Processes the page and store the new errors in a temprorary variable and only 
        // show them to the user if they have clicked next or previous.
        var newerrors = FlowUtils.processDescribeItem(getDSContext(),workspaceID,step,cocoon.request);

        if (cocoon.request.get("submit_next"))
        {
            if (newerrors != null) 
            {
                // The user want's to go on, but the page is in error.
                errors = newerrors;   
            }
            else
            {
                complete = true;
            }
        }
        else if (cocoon.request.get("submit_previous"))
        {
            complete = true;
        }
        else if (cocoon.request.get("submit_save"))
        {
            if (newerrors != null)
            {
                // Can not leave the page until the errors have been resolved.
                errors = newerrors;
            }
            else
            {
                complete = true;
            }
        }
        
        // Jump submit button to another potion of the submission processes.
        var names = cocoon.request.getParameterNames();
        while (names.hasMoreElements())
        {
            var name = names.nextElement(); 
            if (name.startsWith("submit_jump_"))
            {
                if (newerrors != null)
                {
                    // Can not leave the page until the errors have been resolved.
                    errors = newerrors;
                }
                else
                {
                    complete = true;
                }
            }
        }
        
    } while (complete == false)
}

/**
 * This step allows the user to upload files to the submission item. The user is 
 * also presented with a list of existing files on the submission and may choose one of 
 * them to edit metadata about. In this case mini editFileStep is used, note this not a full 
 * step because it is not called by the submission master control.
 */
function submitStepUpload(handle,workspaceID,step) 
{
    var finished = false;
    var errors;
    do {
        sendPageAndWait("handle/"+handle+"/submit/uploadStep",{"id":workspaceID,"step":String(step),"errors":errors});
        
	    var newerrors = FlowUtils.processUpload(getDSContext(),workspaceID,cocoon.request);
        
        if (cocoon.request.get("submit_next"))
        {
            if (newerrors != null)
            {
                // The user may not procced until the errors are fixed.
                errors = newerrors;
            }
            else
            {
                finished = true;
            }
        }
        else if (cocoon.request.get("submit_previous"))
        {
            finished = true;
        }
        else if (cocoon.request.get("submit_save"))
        {
            finished = true;
        }
        
        var names = cocoon.request.getParameterNames();
        while (names.hasMoreElements())
        {
            var name = names.nextElement(); 
            if (name.startsWith("submit_edit_"))
            {
                var bitstreamID = name.substring("submit_edit_".length);
                sendPageAndWait("handle/"+handle+"/submit/editFileStep",{"id":workspaceID,"step":String(step),"bitstreamID":bitstreamID});  
                
                if (cocoon.request.get("submit_save"))
                {
                    FlowUtils.processEditFile(getDSContext(),bitstreamID,cocoon.request);  
                }           
            }
        }
        
        var names = cocoon.request.getParameterNames();
        while (names.hasMoreElements())
        {
            var name = names.nextElement(); 
            if (name.startsWith("submit_jump_"))
            {
                if (newerrors != null)
                {
                    // may not jump until the errors are resolved.
                    errors = newerrors;
                }
                else
                {
                    finished = true;
                }
            }
        }
        
    } while (finished == false)

}

/**
 * This steps presents all the data that the user has entered up to
 * this point and gives them the option to jump back to each section
 * to edit any of the entered metadata.
 */
function submitStepReview(handle,workspaceID,step) 
{
    sendPageAndWait("handle/"+handle+"/submit/reviewStep",{"id":workspaceID,"step":String(step)});
}

/**
 * This optional step allows the user to add a creative commons
 * license to the item. If they want to add one then they will be taken 
 * away from this website to the creative commons site where they will 
 * answer a few questions and select a license. Once finished at the 
 * creative commons website they will be directed back here.
 */
function submitStepCCLicense(handle,workspaceID,step)
{
    // step only preforms an action if Creative commons is enabled, 
    // other wise skip to the next step.
    if (CreativeCommons.isEnabled())
    {
        var finished = false;
        do {
        
            sendPageAndWait("handle/"+handle+"/submit/ccLicenseStep",{"id":workspaceID,"step":String(step)});
            
            FlowUtils.processCCLicense(getDSContext(),workspaceID,cocoon.request);
            
            if (cocoon.request.get("submit_previous"))
            {
                finished = true;
            }
            else if (cocoon.request.get("submit_save"))
            {
                finished = true;
            }
            else if (cocoon.request.get("submit_next"))
            {
                finished = true;
            }
            
        } while (finished == false)
    }
}

/**
 * This step is the last step in the traditional submission workflow. At this step the user is required
 * to accept the distribution license.
 */
function submitStepLicense(handle,workspaceID,step) 
{
    var finished=false;
    var errors
    do {
        sendPageAndWait("handle/"+handle+"/submit/licenseStep",{"id":workspaceID,"step":String(step),"errors":errors});
   
        var newerrors = FlowUtils.processLicense(getDSContext(),workspaceID,cocoon.request);
        
        if (cocoon.request.get("submit_remove"))
        {
            // Remove this submission
            FlowUtils.processSaveOrRemove(getDSContext(),workspaceID,cocoon.request);
            
            sendPage("handle/"+handle+"/submit/removed");
        
            cocoon.exit(); // We're done, Stop excecution.
        }
        if (cocoon.request.get("submit_previous"))
        {
            finished = true;
        }
        else if (cocoon.request.get("submit_save"))
        {
            finished = true;
        }
        else if (cocoon.request.get("submit_complete"))
        {
            if (newerrors != null)
            {
                // Submission can not be completed until
                // a license is granted.
                errors = newerrors;
            }
            else
            {
                FlowUtils.processCompleteSubmission(getDSContext(),workspaceID,cocoon.request);
                sendPage("handle/"+handle+"/submit/completedStep",{"handle":handle});
                cocoon.exit(); // We're done, Stop excecution.
            }
        }
        
        var names = cocoon.request.getParameterNames();
        while (names.hasMoreElements())
        {
            var name = names.nextElement(); 
            if (name.startsWith("submit_jump_"))
            {
                finished = true;
            }
        }
    }
    while (finished == false)
}








/**
 * This is the starting point for all workflow tasks. The id of the workflow
 * is expected to be passed in as a request parameter. The user will be able
 * to view the item and preform the nessesary actions on the task such as: 
 * accept, reject, or edit the item's metadata. 
 */
function doWorkflow() 
{
    var workflowID = cocoon.request.get("workflowID");
    
    if (workflowID == null)
    {
        throw "Unable to find workflow, no workflow id supplied.";
    }
    
    // Get the collection handle for this item.
    var handle = WorkflowItem.find(getDSContext(), workflowID).getCollection().getHandle();
    
    // Specify that we are working with workflows.
    workflowID = "W"+workflowID;
    
    do
    {
        sendPageAndWait("handle/"+handle+"/workflow/performTaskStep",{"id":workflowID,"step":"0"});
        
        if (cocoon.request.get("submit_leave"))
        {
            // Just exit workflow with out doing anything
            var contextPath = cocoon.request.getContextPath();
            cocoon.redirectTo(contextPath+"/submissions",true);
            getDSContext().complete();
            cocoon.exit();
        }
        else if (cocoon.request.get("submit_approve"))
        {
            // Approve this task and exit the workflow
            var archived = FlowUtils.processApproveTask(getDSContext(),workflowID);
            
            var contextPath = cocoon.request.getContextPath();
            cocoon.redirectTo(contextPath+"/submissions",true);
            getDSContext().complete();
            cocoon.exit();
        }
        else if (cocoon.request.get("submit_return"))
        {
            // Return this task to the pool and exit the workflow
            FlowUtils.processUnclaimTask(getDSContext(),workflowID);
            
            var contextPath = cocoon.request.getContextPath();
            cocoon.redirectTo(contextPath+"/submissions",true);
            getDSContext().complete();
            cocoon.exit();
        }
        else if (cocoon.request.get("submit_take_task"))
        {
            // Take the task and stay on this workflow
            FlowUtils.processClaimTask(getDSContext(),workflowID);
            
        }
        else if (cocoon.request.get("submit_reject"))
        {
            var rejected = workflowStepReject(handle,workflowID);
            
            if (rejected == true)
            {
                // the user really rejected the item
                var contextPath = cocoon.request.getContextPath();
                cocoon.redirectTo(contextPath+"/submissions",true);
                getDSContext().complete();
                cocoon.exit();
            }
        }
        else if (cocoon.request.get("submit_edit"))
        {
            // Shuttle the user into the edit workflow
            workflowEditMetadataControl(handle,workflowID);
        }
        
    } while (1==1)
    

}

/**
 * This step is used when the user wants to reject a workflow item, at this step they 
 * are asked to enter a reason for the rejection.
 */
function workflowStepReject(handle,workflowID)
{
    var errors;
    do {
        
        sendPageAndWait("handle/"+handle+"/workflow/rejectTaskStep",{"id":workflowID, "step":"0", "errors":errors});

        if (cocoon.request.get("submit_reject"))
        {
            errors = FlowUtils.processRejectTask(getDSContext(),workflowID,cocoon.request);
            
            if (errors == null)
            {
                // Only exit if rejection succeded, otherwise ask for a reason again.
                return true;
            }      
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            // just go back to the view workflow screen.
            return false;
        }
    } while (1 == 1)
    
}


/**
 * This flow is used during a workflow task when the user selects to edit an item's metadata.
 * The edit processes is very similar to the submission processes, in fact most of the steps 
 * are the same with just minor modifications. This control flow is quite similar but is
 * truncated in the number of steps it hase.
 */
function workflowEditMetadataControl(handle, workflowID) 
{
    
    // First find out how many describe pages there are:
    var numberOfDescribePages = 
      FlowUtils.getNumberOfDescribePages(getDSContext(),workflowID);
    
    var complete = false;
    var step = 0;
    do { 
    
        if ( step >= 1 && step <= numberOfDescribePages )
        {
            complete = workflowStepDescribe(handle,workflowID,step);
        } 
        else 
        {
            switch (step)
            {
            case 0:
                complete = workflowStepInitial(handle,workflowID,step);
                break;
            case numberOfDescribePages+1:
                complete = workflowStepUpload(handle,workflowID,step);
                break;
            case numberOfDescribePages+2:
                complete = workflowStepReview(handle,workflowID,step);
                break;
            }
        }
        
        if (cocoon.request.get("submit_next"))
        {
            step++;
        }
        else if (cocoon.request.get("submit_previous"))
        {
            step--;
        }
        else if (cocoon.request.get("submit_save"))
        {
            complete = true;
        }
        
        var names = cocoon.request.getParameterNames();
        while (names.hasMoreElements())
        {
            var name = names.nextElement(); 
            if (name.startsWith("submit_jump_"))
            {
                var newStep = name.substring("submit_jump_".length);
                newStep = parseInt(newStep);
                if (newStep >= 0 && newStep <= numberOfDescribePages+2)
                {
                   step = newStep;     
                }
            }
        }
        
    } while ( complete == false)
}

/**
 * This step is the same as the submission version.
 */
function workflowStepInitial(handle,workflowID,step)
{
    // Just use the same implementation as submission.
    submitStepInitial(handle,workflowID,step);
    return false;
}

/**
 * This step is the same as the submission version.
 */
function workflowStepDescribe(handle,workflowID, step)
{
    // Just us the same implementation as submission
    submitStepDescribe(handle,workflowID,step);
    return false;
}

/**
 * This step is simplar to the submission version but
 * it does not allow the user to upload any files. The user
 * may only edit metadata on existing files.
 */
function workflowStepUpload(handle,workflowID,step) 
{
    // The same implementation as submission but without the ability to upload.
    
    var finished = false;
    var errors;
    do {
        sendPageAndWait("handle/"+handle+"/submit/uploadStep",{"id":workflowID,"step":String(step),"errors":errors});

        if (cocoon.request.get("submit_next"))
        {
            finished = true;
        }
        else if (cocoon.request.get("submit_previous"))
        {
            finished = true;
        }
        else if (cocoon.request.get("submit_save"))
        {
            finished = true;
        }
        
        var names = cocoon.request.getParameterNames();
        while (names.hasMoreElements())
        {
            var name = names.nextElement(); 
            if (name.startsWith("submit_edit_"))
            {
                var bitstreamID = name.substring("submit_edit_".length);
                sendPageAndWait("handle/"+handle+"/submit/editFileStep",{"id":workflowID,"step":String(step),"bitstreamID":bitstreamID});  
                
                if (cocoon.request.get("submit_save"))
                {
                    FlowUtils.processEditFile(getDSContext(),bitstreamID,cocoon.request);  
                }           
            }
        }
    } while (finished == false)
    
    return false;
}


/**
 * This step is the same as the submission version, with 
 * one exception. When the user click's next instead of 
 * progressing to another step they are returned to the 
 * task preview step.
 */
function workflowStepReview(handle,workflowID,step) 
{
    // Just use the same implementation as submission,
    // but if they try to go to the next page then set
    // the exit paramater so instead they go back to
    // the workflow view.
    submitStepReview(handle,workflowID,step);
    
    if (cocoon.request.get("submit_next"))
    {
       return true;
    }
    
    return false; 
}


