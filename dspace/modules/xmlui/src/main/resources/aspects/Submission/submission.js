/*
 * submission.js
 *
 * Version: $Revision: 4568 $
 *
 * Date: $Date: 2009-11-30 16:37:13 +0000 (Mon, 30 Nov 2009) $
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

importClass(Packages.java.lang.Class);
importClass(Packages.java.lang.ClassLoader);

importClass(Packages.org.dspace.app.xmlui.utils.FlowscriptUtils);
importClass(Packages.org.apache.cocoon.environment.http.HttpEnvironment);
importClass(Packages.org.apache.cocoon.servlet.multipart.Part);

importClass(Packages.org.dspace.handle.HandleManager);
importClass(Packages.org.dspace.core.Constants);
importClass(Packages.org.dspace.workflow.WorkflowItem);
importClass(Packages.org.dspace.workflow.WorkflowManager);
importClass(Packages.org.dspace.workflow.WorkflowFactory);
importClass(Packages.org.dspace.content.WorkspaceItem);
importClass(Packages.org.dspace.authorize.AuthorizeManager);
importClass(Packages.org.dspace.license.CreativeCommons);

importClass(Packages.org.dspace.app.xmlui.utils.ContextUtil);
importClass(Packages.org.dspace.app.xmlui.cocoon.HttpServletRequestCocoonWrapper);
importClass(Packages.org.dspace.app.xmlui.aspect.submission.FlowUtils);

importClass(Packages.org.dspace.app.util.SubmissionConfig);
importClass(Packages.org.dspace.app.util.SubmissionConfigReader);
importClass(Packages.org.dspace.app.util.SubmissionInfo);

importClass(Packages.org.dspace.submit.AbstractProcessingStep);
importClass(Packages.org.dspace.content.Collection);
/**
 *
 * This file has been altered to support the dryad submission
 */

/* Global variable which stores a comma-separated list of all fields 
 * which errored out during processing of the last step.
 */
var ERROR_FIELDS = null;

/**
 * Simple access method to access the current cocoon object model.
 */
function getObjectModel() 
{
  return FlowscriptUtils.getObjectModel(cocoon);
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
 * Return the HTTP Request object for this request
 */
function getHttpRequest()
{
	//return getObjectModel().get(HttpEnvironment.HTTP_REQUEST_OBJECT)
	
	// Cocoon's request object handles form encoding, thus if the users enters 
	// non-ascii characters such as those found in foriegn languages they will 
	// come through corruped if they are not obtained through the cocoon request
	// object. However, since the dspace-api is built to accept only HttpServletRequest
	// a wrapper class HttpServletRequestCocoonWrapper has bee built to translate
	// the cocoon request back into a servlet request. This is not a fully complete 
	// translation as some methods are unimplemeted. But it is enough for our 
	// purposes here.
	return new HttpServletRequestCocoonWrapper(getObjectModel());
}

/**
 * Return the HTTP Response object for the response
 * (used for compatibility with DSpace configurable submission system)
 */
function getHttpResponse()
{
	return getObjectModel().get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
}

/**
 * Return the SubmissionInfo for the current submission
 */
function getSubmissionInfo(workspaceID)
{
	return FlowUtils.obtainSubmissionInfo(getObjectModel(), workspaceID);
}

/**
 * Return an array of all step and page numbers as Java Double
 * objects (format: #.#). 
 * This returns all steps within the current submission process,
 * including non-interactive steps which do not appear in 
 * the Progress Bar
 */
function getSubmissionSteps(submissionInfo)
{
	return FlowUtils.getListOfAllSteps(getHttpRequest(), submissionInfo);
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


function doSubmissionOverview()
{
    var workItemID = cocoon.request.get("workspaceID");
    //Check for a workflow item
    if(workItemID == null){
        workItemID = "W" + cocoon.request.get("workflowID");
    }else
        workItemID = "S" + workItemID;


    do {
        //Send user to the overviewpage & await further steps.
        sendPageAndWait("submit/overviewStep",{"id":workItemID});

        var redirUrl = FlowUtils.processOverviewStep(getDSContext(), cocoon.request, cocoon.response, workItemID);
        if(redirUrl != null){
            cocoon.redirectTo(redirUrl,true);
            cocoon.exit();
        }
    }while (true);

}

function doSubmissionCheckout()
{
    var workItemID = cocoon.request.get("workspaceID");
    //Check for a workflow item
    if(workItemID == null){
        workItemID = "W" + cocoon.request.get("workflowID");
    }else
        workItemID = "S" + workItemID;


    do {
        //Send user to the overviewpage & await further steps.
        sendPageAndWait("submit/checkout",{"id":workItemID});

        var redirUrl = FlowUtils.processCheckoutStep(getDSContext(), cocoon.request, cocoon.response, workItemID);
        if(redirUrl != null){
            cocoon.redirectTo(redirUrl,true);
            cocoon.exit();
        }
    }while (true);

}

function doDepositConfirmed()
{
    do {
        //Send user to the overviewpage & await further steps.
        sendPageAndWait("submit/depositConfirmedStep",{"id":cocoon.request.get("itemID")});

        var redirUrl = FlowUtils.processDepositConfirmedStep(getDSContext(), cocoon.request, cocoon.response, workItemID);
        if(redirUrl != null){
            cocoon.redirectTo(redirUrl,true);
            cocoon.exit();
        }
    }while (true);

}

function doEditMetadata()
{
    var wfItemID = cocoon.request.get("wfItemID");

    var result;
    do{
        sendPageAndWait("submit/submit-edit-metadata", {"wfItemID": wfItemID}, result);
        result = null;
        if(cocoon.request.get("submit_return") != null){
            //Send us back to our overview
            var redirUrl = cocoon.request.getContextPath() + "/submit-overview?workflowID=" + wfItemID;
            cocoon.redirectTo(redirUrl, true);
            cocoon.exit();
        }
        if(cocoon.request.get("submit_add") != null){
            //Add a new metadata field
            result = FlowUtils.doAddMetadata(getDSContext(), wfItemID, cocoon.request);
        }
        else{
            //Process our edit
            result = FlowUtils.doEditMetadata(getDSContext(), wfItemID, cocoon.request);
        }
    }while (true);
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
   var step = cocoon.request.get("step"); //retrieve step number
   
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
       //(specify "S" for submission item, for FlowUtils.findSubmission())
       submissionControl(handle,"S"+workspaceID, step);
       
   }
   else
   {
       // Resume a previous submission
       var workspace = WorkspaceItem.find(getDSContext(), workspaceID);
       var skipOverview = cocoon.request.get("skipOverview");

       // First check that the id is valid.
       var submitterID = workspace.getSubmitter().getID()
       var currentID = getDSContext().getCurrentUser().getID();
       if (submitterID == currentID)
       {
           // Get the collection handle for this item.
           var handle = workspace.getCollection().getHandle();
           
           // Record that this is a submission id, not a workflow id.
           //(specify "S" for submission item, for FlowUtils.findSubmission())
           workspaceID = "S"+workspaceID;
           do {

               if(!skipOverview)
                   sendPageAndWait("handle/"+handle+"/submit/resumeStep",{"id":workspaceID,"step":"0"});
               
               if (cocoon.request.get("submit_resume") || skipOverview)
               {
                   submissionControl(handle,workspaceID, step);
               }
               else if (cocoon.request.get("submit_cancel"))
               {
                   var contextPath = cocoon.request.getContextPath();
                   cocoon.redirectTo(contextPath+"/submissions",true);
                   getDSContext().complete();
                   cocoon.exit();
               }
               else if (cocoon.request.get("submit_continue_reviewer"))
               {
                   FlowUtils.completePublicationSubmission(getDSContext(), workspace, cocoon.request);
                   getDSContext().commit();
                   //Redirect the user to the page where he get's an overview of all the items that have been sent in the reviewing process
                   showCompleteConfirmation(handle);
               }
               
               
           } while (1 == 1)
           
   
       }
   }

}

function doSubmissionControl(){
    submissionControl(cocoon.request.get("collHandle"), cocoon.request.get("workitemID"), null);

}

/**
 * This is the master flow control for submissions. This method decides
 * which steps are excuted in which order. Each step is called and does
 * not return until it's exit conditions have been meet. Then the standard
 * navigation buttons are checked and the step is either decreased or
 * increased accordingly. Special cases like jumping or saveing are also
 * handled here.
 *
 * Parameters:
 *     collectionHandle - the handle of the collection we are submitting to
 *     workspaceID - the in progress submission's Workspace ID
 *     stepAndPage - the Step and Page number to start on (e.g. "1.1")
 */
function submissionControl(collectionHandle, workspaceID, initStepAndPage)
{
  cocoon.log.info("starting submissionControl ch=" + collectionHandle + " wID=" + workspaceID + " initStep=" + initStepAndPage, null);

	//load initial submission information
	var submissionInfo = getSubmissionInfo(workspaceID);
	
	//Initialize a Cocoon Local Page to save current state information
	//(This lets us handle when users click the browser "back button"
	// by caching the state of that previous page, etc.)
	var state = cocoon.createPageLocal(); 
    state.progressIterator = 0;  //initialize our progress indicator
    
    //this is array of all the steps/pages in current submission process
    //it's used to step back and forth between pages!
    var stepsInSubmission = getSubmissionSteps(submissionInfo);
    
	
    //if we didn't have a page passed in, go to first page in process
    if(initStepAndPage==null)
    	state.stepAndPage = stepsInSubmission[0];
    else
	    state.stepAndPage = initStepAndPage;
		
    var response_flag = 0;
    
    do { 
     	// Loop forever, exit cases such as save, remove, or completed
        // will call cocoon.exit() stopping execution.
        
  		cocoon.log.info("Current step & page=" + state.stepAndPage, null);
  		cocoon.log.info("Current ERROR Fields=" + getErrorFields(), null);
    	
    	//----------------------------------------------------------
    	// #1: Actually load the next page in the process
    	//-----------------------------------------------------------
    	//split out step and page (e.g. 1.2 is page 2 of step 1)
	 	var fields = String(state.stepAndPage).split(".");
		var step = fields[0];
		var page = fields[1];

        //Make sure we skip any steps that are not accessible
        var possibleNewStepConfig = submissionInfo.getSubmissionConfig().getStep(step);
        cocoon.log.info("after getSubmissionConfig().getStep()");
        while(!FlowUtils.isStepAccessible(getDSContext(), possibleNewStepConfig.getProcessingClassName(), submissionInfo)){
            state.progressIterator++;
            state.stepAndPage = stepsInSubmission[state.progressIterator];
            step = String(state.stepAndPage).split(".")[0];
            possibleNewStepConfig = submissionInfo.getSubmissionConfig().getStep(step);
            cocoon.log.info("Skipped step & page=" + state.stepAndPage + " reason: not accessible");
        }
      cocoon.log.info("Setting page reached to step=" + step + " page=" + page);
        //Set the current page we've reached in current step
	    FlowUtils.setPageReached(getDSContext(),workspaceID, step, page);
	    cocoon.log.info("After setPageReached");
	  	//Load this step's configuration
	  	var stepConfig = submissionInfo.getSubmissionConfig().getStep(step);
	  	cocoon.log.info("After getStep");
	  		  		
    	//Pass it all the info it needs, including any response/error flags
    	//in case an error occurred
    	response_flag = doNextPage(collectionHandle, workspaceID, stepConfig, state.stepAndPage, response_flag); 
    	cocoon.log.info("After doNextPage");
    	var maxStep = FlowUtils.getMaximumStepReached(getDSContext(),workspaceID);
        var maxPage = FlowUtils.getMaximumPageReached(getDSContext(),workspaceID);
        var maxStepAndPage = parseFloat(maxStep + "." + maxPage);
    	
    	//----------------------------------------------------------
    	// #2: Determine which page/step the user should be sent to next
    	//-----------------------------------------------------------
        // User clicked "Next->" button (or a Non-interactive Step - i.e. no UI)
        // Only step forward to next page if no errors on this page
        if ((cocoon.request.get(AbstractProcessingStep.NEXT_BUTTON) || !stepHasUI(stepConfig))  && (response_flag==AbstractProcessingStep.STATUS_COMPLETE))
        {
            cocoon.log.info("User clicked next for a non-interactive step");
           	state.progressIterator++;
           	
           	var totalSteps = stepsInSubmission.length;
           	var inWorkflow = submissionInfo.isInWorkflow();
  	
           	//check if we've completed the submission
           	if(state.progressIterator >= totalSteps)
           	{
           		if(inWorkflow==false)
           		{
           		  	//Submission is completed!
           			cocoon.log.info("Submission Completed!");
           			
           			showCompleteConfirmation(collectionHandle);
           		}
           		else
           		{   //since in Workflow just break out of loop to return to Workflow process
           			break;
           		}
           	}
           	else
           	{
           		state.stepAndPage = stepsInSubmission[state.progressIterator];
                var possibleNewStep = String(state.stepAndPage).split(".")[0];
                possibleNewStepConfig = submissionInfo.getSubmissionConfig().getStep(possibleNewStep);
                //Should our page not be accessible for some reason skip it & go to the next one
                while(!FlowUtils.isStepAccessible(getDSContext(), possibleNewStepConfig.getProcessingClassName(), submissionInfo)){
                    state.progressIterator++;
                    state.stepAndPage = stepsInSubmission[state.progressIterator];
                    possibleNewStep = String(state.stepAndPage).split(".")[0];
                    possibleNewStepConfig = submissionInfo.getSubmissionConfig().getStep(possibleNewStep);
                    cocoon.log.info("Skipped step & page=" + state.stepAndPage + " reason: not accessible");
                }
           		cocoon.log.info("Next Step & Page=" + state.stepAndPage);
        	}
        }//User clicked "<- Previous" button
        else if (cocoon.request.get(AbstractProcessingStep.PREVIOUS_BUTTON) && 
        			(response_flag==AbstractProcessingStep.STATUS_COMPLETE || state.stepAndPage == maxStepAndPage))
        {  
            cocoon.log.info("User clicked previous");
            var stepBack = true;
            
            //Need to find the previous step which HAS a user interface.
            while(stepBack)
            {

                cocoon.log.error("state.progressIterator=" + state.progressIterator);

            	state.progressIterator--;
            	if(state.progressIterator<0)
            	    stepBack = false;
            	   
            	state.stepAndPage = stepsInSubmission[state.progressIterator];


                cocoon.log.error("Previous Step & Page=" + state.stepAndPage);


                var prevStep = String(state.stepAndPage).split(".")[0];
                var prevStepConfig = submissionInfo.getSubmissionConfig().getStep(prevStep);


                cocoon.log.error("step has UI ? " + stepHasUI(prevStepConfig));
                cocoon.log.error("is step accessible ? " + FlowUtils.isStepAccessible(getDSContext(), prevStepConfig.getProcessingClassName(), submissionInfo));

                //Make sure that the step has a UI & that the step is accessible
                if(!stepHasUI(prevStepConfig) || !FlowUtils.isStepAccessible(getDSContext(), prevStepConfig.getProcessingClassName(), submissionInfo)){
                    stepBack = true;
                }
                else{
                    stepBack = false;
                }

            }

            
            cocoon.log.info("Previous Step & Page=" + state.stepAndPage);
        }
        // User clicked "Save/Cancel" Button
        else if (cocoon.request.get(AbstractProcessingStep.CANCEL_BUTTON))
        {
          cocoon.log.info("User clicked save/cancel");
        	var inWorkflow = submissionInfo.isInWorkflow();
        	if (inWorkflow && response_flag==AbstractProcessingStep.STATUS_COMPLETE)
        	{
        		var contextPath = cocoon.request.getContextPath();
        		cocoon.redirectTo(contextPath+"/submissions",true);
        		coocon.exit();
        	}
        	else if (!inWorkflow)
        	{
        			submitStepSaveOrRemove(collectionHandle,workspaceID,step,page);
        	}
        }
        
        //User clicked on Progress Bar:
        // only check for a 'step_jump' (i.e. click on progress bar)
        // if there are no errors to be resolved
        if(response_flag==AbstractProcessingStep.STATUS_COMPLETE || state.stepAndPage == maxStepAndPage)
        {
          cocoon.log.info("response_flag is complete and stepAndPage is max");
	        var names = cocoon.request.getParameterNames();
	        while(names.hasMoreElements())
	        {
	            var name = names.nextElement(); 
	            if (name.startsWith(AbstractProcessingStep.PROGRESS_BAR_PREFIX))
	            {
	                var newStepAndPage = name.substring(AbstractProcessingStep.PROGRESS_BAR_PREFIX.length());
	                newStepAndPage = parseFloat(newStepAndPage);
	                
	                //only allow a jump to a page user has already been to
	                if (newStepAndPage >= 0 && newStepAndPage <= maxStepAndPage)
	                {
	                   state.stepAndPage = newStepAndPage;
	                   
					   cocoon.log.info("Jump To Step & Page=" + state.stepAndPage);
	                   
	                   //reset progress iterator
	                   for(var i=0; i<stepsInSubmission.length; i++)
	                   {
	                   		if(state.stepAndPage==stepsInSubmission[i])
	                   		{
	                   			state.progressIterator = i;
	                   			break;
	                   		}
	                   }
	                }
	            }//end if submit_jump pressed
	        }//end while more elements
        }//end if no errors
        cocoon.log.info("looping...");
    } while ( 1 == 1)
  cocoon.log.info("submissionControl over");
}

/**
 * This function actually starts the next page in a step,
 * loading it's UI and then doing its processing!
 *
 * Parameters:
 *     collectionHandle - the handle of the collection we are submitting to
 *     workspaceID - the in progress submission's Workspace ID
 *     stepConfig - the SubmissionStepConfig representing the current step config
 *     stepAndPage - the current Step and Page number (e.g. "1.1")
 *     response_flag - any response or errors from previous processing
 */
function doNextPage(collectionHandle, workspaceID, stepConfig, stepAndPage, response_flag)
{
  	//split out step and page (e.g. 1.2 is page 2 of step 1)
	var fields = String(stepAndPage).split(".");
    var step = fields[0];
	var page = fields[1];
  
  	//-------------------------------------
 	// #1: Check if this step has a UI
 	//-------------------------------------
	//if this step has an XML-UI, then call the generic step transformer
 	//(otherwise, this is just a processing step)
 	if(stepHasUI(stepConfig))
 	{
 	  cocoon.log.info("calling sendPageAndWait with collection handle=" + collectionHandle + " workspaceID=" + workspaceID + " transformer=" + stepConfig.getXMLUIClassName());
 		//prepend URI with the handle of the collection, and go there!
 		sendPageAndWait("handle/"+collectionHandle+ "/submit/continue",{"id":workspaceID,"step":String(stepAndPage),"transformer":stepConfig.getXMLUIClassName(),"error":String(response_flag),"error_fields":getErrorFields()});
 		cocoon.log.info("after sendPageAndWait");
    }
        
    //-------------------------------------
    // #2: Perform step processing
    //-------------------------------------
    //perform step processing (this returns null if no errors, otherwise an error string)
    response_flag = processPage(workspaceID, stepConfig, page);
     
    return response_flag;
}


/**
 * This function calls the step processing code, which will process
 * all user inputs for this step, or just perform backend processing
 * (for non-interactive steps).
 *
 * This function returns the response_flag which is returned by the
 * step class's doProcessing() method.  An error flag of 
 * AbstractProcessingStep.STATUS_COMPLETE (value = 0) means no errors!
 *
 * Parameters:
 *     workspaceID - the in progress submission's Workspace ID
 *     stepConfig - the SubmissionStepConfig for the current step
 *     page - the current page number we are on in the step
 */
function processPage(workspaceID, stepConfig, page)
{
	//retrieve submission info 
	//(we cannot pass the submission info to this function, since
	// often this processing takes place as part of a new request 
	// and the DSpace Context is changed on each request) 
	var submissionInfo = getSubmissionInfo(workspaceID);
    var handle = submissionInfo.getCollectionHandle();
	var response_flag = null;

	//---------------------------------------------
    // #1: Get a reference to Step Processing class
    //---------------------------------------------
	//get name of processing class for this step
	var processingClassName = stepConfig.getProcessingClassName();
    
	//retrieve an instance of the processing class
	var loader = submissionInfo.getClass().getClassLoader();
	var processingClass = loader.loadClass(processingClassName);
	
 	// this processing class *must* be a valid AbstractProcessingStep, 
	// or else we'll have problems very shortly
	var stepClass = processingClass.newInstance();
	
	//------------------------------------------------
    // #2: Perform step processing & check for errors
    //------------------------------------------------
    //Check if this	request is a file upload
	//(if so, Cocoon automatically uploads the file, 
	// so we need to let the Processing class know that)
	var contentType = getHttpRequest().getContentType();
	if ((contentType != null)
                && (contentType.indexOf("multipart/form-data") != -1))
    {
    	//load info about uploaded file, so that it can be
    	//saved properly by the step's doProcessing() method below
    	loadFileUploadInfo();
    }	
    
	//before beginning processing, let this step know what page to process
	//(this is important for multi-page steps!)	
	stepClass.setCurrentPage(getHttpRequest(), page);	
		
	//call the step's doProcessing() method
    try{
	    response_flag = stepClass.doProcessing(getDSContext(), getHttpRequest(), getHttpResponse(), submissionInfo);
    }catch(exception){
        sendPage("handle/"+handle+"/workflow_new/workflowexception",{"error":exception.toString()});
        cocoon.exit();

    }

	//if this is a non-interactive step,
	//we cannot do much with errors/responses other than logging them!
    if((!stepHasUI(stepConfig)) && (response_flag!=AbstractProcessingStep.STATUS_COMPLETE))
    {
    	//check to see if there is a description of this response/error in Messages!
    	var error = stepClass.getErrorMessage(response_flag);
    	
    	//if no error message defined, create a dummy one
    	if(error==null)
    	{
			error = "The doProcessing() method for " + processingClass.getName() + 
      						" returned an error flag = " + response_flag + ". " +
      						"It is recommended to define a custom error message for this error flag using the addErrorMessage() method for this class!";
        }
        		
    	cocoon.log.error(error, null); //log as an error to Cocoon
    	
    	//clear error flag, so that processing can continue
    	response_flag = AbstractProcessingStep.STATUS_COMPLETE;  
    	//clear any error fields as well
		saveErrorFields(null);
    	
    }//else if there is a UI, but still there were errors!
    else if(response_flag!=AbstractProcessingStep.STATUS_COMPLETE)
	{
		//save error fields to global ERROR_FIELDS variable,
		//for step-specific post-processing
		saveErrorFields(stepClass.getErrorFields(getHttpRequest()));
	}
	else //otherwise, no errors at all
	{
		//clear any previously set error fields
		saveErrorFields(null);
	}
	
    return response_flag;
}

/**
 * This function loads information about a file automatically
 * uploaded by Cocoon.  A file will only be automatically uploaded
 * by Cocoon if 'enable-uploads' is set to 'true' in the web.xml
 * (which is the default setting for Manakin).
 *
 * The uploaded files will be added to the Request object as two
 * seperate attributes: [name]-path and [name]-inputstream. The first
 * attribute contains the full path to the uploaded file on the client's
 * Operating System. The second attribute contains an inputstream to the
 * file. These two attributes will be created for any file uploaded.
 */
function loadFileUploadInfo()
{
	//determine the parameter which is the uploaded file
	var paramNames = cocoon.request.getParameterNames();
	while(paramNames.hasMoreElements())
	{
		var fileParam = paramNames.nextElement();
		
		var fileObject = cocoon.request.get(fileParam);
        
        //check if this is actually a file
		if (!(fileObject instanceof Part)) 
		{	
			continue;
		}
		
		//load uploaded file information
		if (fileObject != null && fileObject.getSize() > 0)
		{
			//Now, save information to HTTP request which
			//the step processing class will use to actually 
			//save the file as a DSpace bitstream object.
			
			//save original filename to request attribute
			getHttpRequest().setAttribute(fileParam + "-path", fileObject.getUploadName());
			
			//save inputstream of file contents to request attribute
			getHttpRequest().setAttribute(fileParam + "-inputstream", fileObject.getInputStream());
		}	
		
    }
}

/**
 * Save the error fields returned by the last step processed.
 * 
 * The errorFields parameter is the List of strings returned by a
 * call to AbstractProcessingStep.getErrorFields()
 */
function saveErrorFields(errorFields)
{
	if(errorFields==null || errorFields.size()==0)
	{
		ERROR_FIELDS=null;
	}
	else
	{	
        ERROR_FIELDS="";
		//iterate through the fields
		var i = errorFields.iterator();
	
		//build comma-separated list of error fields
		while(i.hasNext())
		{
			var field = i.next();
			
			if(ERROR_FIELDS==null || ERROR_FIELDS.length==0)
			{
				ERROR_FIELDS = field;
			}
			else
			{
				ERROR_FIELDS = ERROR_FIELDS + "," + field;
			}	
		}
	}	
}

/**
 * Get the error fields returned by the last step processed.
 * 
 * This method returns a comma-separated list of field names
 */
function getErrorFields()
{
	return ERROR_FIELDS;
}


/**
 * Return whether or not the step (specified by the step configuration)
 * has a User Interface.
 * 
 * This method returns true (if step has UI) or false (if no UI / non-interactive step)
 * Parameters:
 *     stepConfig - the SubmissionStepConfig representing the current step config
 */
function stepHasUI(stepConfig)
{
	//check if this step has an XML-UI Transformer class specified
 	var xmlUIClassName = stepConfig.getXMLUIClassName();
 
	//if this step specifies an XMLUI class, then it has a User Interface
 	if((xmlUIClassName!=null) && (xmlUIClassName.length()>0))
 	{
 		return true;
 	}
 	else
 	{
 		return false;
 	}
}

/**
 * This step is used when ever the user clicks save/cancel during the submission 
 * processes. We ask them if they would like to save the submission or remove it.
 */
function submitStepSaveOrRemove(collectionHandle,workspaceID,step,page)
{
	// we need to update the reached step to prevent smart user to skip file upload 
    // or keep empty required metadata using the resume
    var maxStep = FlowUtils.getMaximumStepReached(getDSContext(),workspaceID);
    var maxPage = FlowUtils.getMaximumPageReached(getDSContext(),workspaceID);
    var maxStepAndPage = parseFloat(maxStep + "." + maxPage);
    
    var currStepAndPage = parseFloat(step + "." + page);
 	   
    if (maxStepAndPage > currStepAndPage)
    {
 	   FlowUtils.setBackPageReached(getDSContext(),workspaceID, step, page);
    }

    sendPageAndWait("handle/"+collectionHandle+"/submit/saveOrRemoveStep",{"id":workspaceID,"step":String(step),"page":String(page)});
    
    FlowUtils.processSaveOrRemove(getDSContext(), workspaceID, cocoon.request);
    
    if (cocoon.request.get("submit_save"))
    {
       // Allready saved...
       var contextPath = cocoon.request.getContextPath();
       cocoon.redirectTo(contextPath+"/submissions",true);
       cocoon.exit();
    }
    else if (cocoon.request.get("submit_remove"))
    {
        sendPage("handle/"+collectionHandle+"/submit/removedStep");
        cocoon.exit(); // We're done, Stop excecution.
    }
    
    // go back to submission control and continue.
}

/**
 * This method simply displays
 * the "submission completed" confirmation page
 */
function showCompleteConfirmation(handle)
{
	//forward to completion page & exit cocoon
	sendPage("handle/"+handle+"/submit/completedStep",{"handle":handle});
    cocoon.exit(); // We're done, Stop execution.
}

/**
 * This is the starting point for all workflow tasks. The id of the workflow
 * is expected to be passed in as a request parameter. The user will be able
 * to view the item and preform the nessesary actions on the task such as: 
 * accept, reject, or edit the item's metadata. 
 */
function doWorkflow() 
{
    var workflowItemId = cocoon.request.get("workflowID");
    // Get the collection handle for this item.
    var workflowItem = WorkflowItem.find(getDSContext(), workflowItemId);
    var coll = workflowItem.getCollection();
    var handle = coll.getHandle();
    var workflow = WorkflowFactory.getWorkflow(coll);
    var step = workflow.getStep(cocoon.request.get("stepID"));

    var contextPath = cocoon.request.getContextPath();
    if (workflowItemId == null)
    {
        throw "Unable to find workflow, no workflow id supplied.";
    }else if(step == null){
        throw "Unable to find step, no step id supplied.";
    }
    var action = step.getActionConfig(cocoon.request.get("actionID"));

    do{
        sendPageAndWait("handle/"+handle+"/workflow_new/getTask",{"workflow_item_id":workflowItemId,"step_id":step.getId(),"action_id":action.getId()});

        if(cocoon.request.get("submit_adddataset")){
            //Add another data file to our datapublication
            //First retrieve our publication
            var redirUrl = FlowUtils.addDataset(getDSContext(), cocoon.request, workflowItem.getItem(), true);
            if(redirUrl != null){
                //Redirect us to our dataset
                var contextPath = cocoon.request.getContextPath();
                cocoon.redirectTo(redirUrl,true);
                getDSContext().complete();
                cocoon.exit();
            }else{
                //This should never occur
                sendPage("handle/"+handle+"/workflow_new/workflowexception",{"error":"Unknown error"});
            }
        }
        else
        if (cocoon.request.get("submit_edit"))
        {
        	//User is editing this submission:
            //	Send user through the Submission Control
            cocoon.redirectTo(cocoon.request.getContextPath() + "/submit-overview?workflowID=" + workflowItemId, true);
            getDSContext().complete();
            cocoon.exit();
        }else if((cocoon.request.get("submit_full_item_info") && !cocoon.request.get("submit_full_item_info").equals("true")) || cocoon.request.get("submit_simple_item_info")){
            //Don't do anything just go back to the start of the loop
        }
        else if (cocoon.request.get("skip_payment")||cocoon.request.get("submit_next")!=null)
        {
            // enter the reauthorization payment step and exit the workflow
            var approved = FlowUtils.processReAuthorization(getDSContext(),workflowItemId,action,cocoon.request);
            var contextPath = cocoon.request.getContextPath();
            if(approved){

                cocoon.redirectTo(contextPath+"/my-tasks",true);
            }
            else
            {     //has an error when submit the reauthorization form, need to resubmit
                var collection = Collection.find(getDSContext(),coll.getID());
                cocoon.redirectTo(contextPath+"/handle/"+collection.getHandle()+"/workflow_new?workflowID="+workflowItemId+"&stepID=reAuthorizationPaymentStep&actionID=reAuthorizationPaymentAction&encountError=true",true);

            }
            getDSContext().complete();
            cocoon.exit();
        }
        else if (cocoon.request.get("submit-credit")){
            var step = cocoon.request.get("stepID");
            var action =  cocoon.request.get("actionID");
            cocoon.redirectTo(contextPath+"/handle/"+handle+"/workflow?workflowID="+workflowItemId+"&stepID="+step+"&actionID="+action+"&submit-credit=true&credit=true",true);
        }
        else if (cocoon.request.get("submit-voucher"))
        {
            cocoon.redirectTo(contextPath+"/handle/"+handle+"/workflow_new?workflowID="+workflowItemId+"&stepID=reAuthorizationPaymentStep&actionID=reAuthorizationPaymentAction&submit-voucher=true&voucher="+cocoon.request.get("voucher"),true);
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            //cancel perform the reauthorizationpaymentaction
            var contextPath = cocoon.request.getContextPath();
            cocoon.redirectTo(contextPath+"/my-tasks",true);
            getDSContext().complete();
            cocoon.exit();
        }
        else{
            try{
                action = WorkflowManager.doState(getDSContext(), getDSContext().getCurrentUser(), getHttpRequest(), workflowItemId, workflow, action);
            }catch(exception){
                sendPage("handle/"+handle+"/workflow_new/workflowexception",{"error":exception.toString()});
                cocoon.exit();
            }
            if(action == null){
                var contextPath = cocoon.request.getContextPath();
                var itemID = workflowItem.getItem().getID();
                cocoon.redirectTo(contextPath+"/my-tasks?itemID=" + itemID,true);
                getDSContext().complete();
                cocoon.exit();
            }
        }

    }while(true);

}

/**
 * This step is used when the user wants to reject a workflow item, at this step they 
 * are asked to enter a reason for the rejection.
 */
function workflowStepReject(handle,workflowID)
{
    var error_fields;
    do {
        
        sendPageAndWait("handle/"+handle+"/workflow/rejectTaskStep",{"id":workflowID, "step":"0", "error_fields":error_fields});

        if (cocoon.request.get("submit_reject"))
        {
            error_fields = FlowUtils.processRejectTask(getDSContext(),workflowID,cocoon.request);
            
            if (error_fields == null)
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


