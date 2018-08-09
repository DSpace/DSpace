/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
importClass(Packages.java.lang.Class);
importClass(Packages.java.lang.ClassLoader);

importClass(Packages.org.dspace.app.xmlui.utils.FlowscriptUtils);
importClass(Packages.org.dspace.app.xmlui.aspect.submission.FlowUtils);
importClass(Packages.org.dspace.app.xmlui.cocoon.HttpServletRequestCocoonWrapper);
importClass(Packages.org.apache.cocoon.environment.http.HttpEnvironment);
importClass(Packages.org.dspace.app.xmlui.aspect.submission.StepAndPage);
importClass(Packages.org.apache.cocoon.servlet.multipart.Part);

importClass(Packages.org.dspace.handle.service.HandleService);
importClass(Packages.org.dspace.handle.factory.HandleServiceFactory);
importClass(Packages.org.dspace.workflow.WorkflowService);
importClass(Packages.org.dspace.core.Constants);
importClass(Packages.org.dspace.workflow.WorkflowItem);
importClass(Packages.org.dspace.content.WorkspaceItem);
importClass(Packages.org.dspace.content.service.WorkspaceItemService);
importClass(Packages.org.dspace.content.factory.ContentServiceFactory);
importClass(Packages.org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem);
importClass(Packages.org.dspace.authorize.service.AuthorizeService);
importClass(Packages.org.dspace.authorize.factory.AuthorizeServiceFactory);
importClass(Packages.org.dspace.core.ConfigurationManager);
importClass(Packages.org.dspace.license.service.CreativeCommonsService);
importClass(Packages.org.dspace.workflow.factory.WorkflowServiceFactory);
importClass(Packages.org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory);


importClass(Packages.org.dspace.app.xmlui.utils.ContextUtil);

importClass(Packages.org.dspace.app.util.SubmissionConfig);
importClass(Packages.org.dspace.app.util.SubmissionConfigReader);
importClass(Packages.org.dspace.app.util.SubmissionInfo);

importClass(Packages.org.dspace.submit.AbstractProcessingStep);

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
 * a new context this object should never be stored and instead always accessed 
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
	// non-ascii characters such as those found in foreign languages they will 
	// come through corrupted if they are not obtained through the cocoon request
	// object. However, since the dspace-api is built to accept only HttpServletRequest
	// a wrapper class HttpServletRequestCocoonWrapper has bee built to translate
	// the cocoon request back into a servlet request. This is not a fully complete 
	// translation as some methods are unimplemented. But it is enough for our 
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

function getHandleService(handle) {
    var dso = HandleServiceFactory.getInstance().getHandleService().resolveToObject(getDSContext(), handle);
    return dso;
}

function getAuthorizeService() {
    return AuthorizeServiceFactory.getInstance().getAuthorizeService();
}

function getWorkspaceItemService() {
    return ContentServiceFactory.getInstance().getWorkspaceItemService();
}

function getItemService() {
    return ContentServiceFactory.getInstance().getItemService();
}

function getWorkflowItemService()
{
    return XmlWorkflowServiceFactory.getInstance().getWorkflowItemService();
}

function getWorkflowFactory()
{
    return XmlWorkflowServiceFactory.getInstance().getWorkflowFactory();
}


/**
 * Return the SubmissionInfo for the current submission
 */
function getSubmissionInfo(workspaceID)
{
    return FlowUtils.obtainSubmissionInfo(getObjectModel(), workspaceID);
}

/**
 * Return an array of all step and page numbers as StepAndPage objects.
 * This returns all steps within the current submission process,
 * including non-interactive steps which do not appear in 
 * the Progress Bar.
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
 * Send the given page and DO NOT wait for the flow to be continued. Execution will
 * proceed as normal. Use this method to add a flow=true parameter. This allows the
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
               var dso = getHandleService(handle);
               // Check that the dso is a collection
               if (dso != null && dso.getType() == Constants.COLLECTION)
               {
                   // Check that the user is able to submit
                   if (getAuthorizeService().authorizeActionBoolean(getDSContext(), dso, Constants.ADD))
                   {
                       // Construct a new workspace for this submission.
                       var workspace = getWorkspaceItemService().create(getDSContext(), dso, true);
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
       var workspace = getWorkspaceItemService().find(getDSContext(), workspaceID);
       
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
               sendPageAndWait("handle/"+handle+"/submit/resumeStep",
                   {"id":workspaceID,"step":"0.0"});

               if (cocoon.request.get("submit_resume"))
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


           } while (1 == 1)


       }
   }

}

/**
 * This is the master flow control for submissions. This method decides
 * which steps are executed in which order. Each step is called and does
 * not return until its exit conditions have been met. Then the standard
 * navigation buttons are checked and the step is either decreased or
 * increased accordingly. Special cases like jumping or saving are also
 * handled here.
 *
 * Parameters:
 *     collectionHandle - the handle of the collection we are submitting to
 *     workspaceID - the in progress submission's Workspace ID
 *     stepAndPage - the Step and Page number to start on (e.g. (1,1))
 */
function submissionControl(collectionHandle, workspaceID, initStepAndPage) 
{
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

  		cocoon.log.debug("Current step & page=" + state.stepAndPage, null);
  		cocoon.log.debug("Current ERROR Fields=" + getErrorFields(), null);

    	//----------------------------------------------------------
    	// #1: Actually load the next page in the process
    	//-----------------------------------------------------------
    	//split out step and page (e.g. 1.2 is page 2 of step 1)
		var step = state.stepAndPage.getStep();
		var page = state.stepAndPage.getPage();

	    //Set the current page we've reached in current step
	    FlowUtils.setPageReached(getDSContext(),workspaceID, step, page);

	  	//Load this step's configuration
	  	var stepConfig = submissionInfo.getSubmissionConfig().getStep(step);

    	//Pass it all the info it needs, including any response/error flags
    	//in case an error occurred
    	response_flag = doNextPage(collectionHandle, workspaceID, stepConfig, state.stepAndPage, response_flag); 

    	var maxStep = FlowUtils.getMaximumStepReached(getDSContext(),workspaceID);
        var maxPage = FlowUtils.getMaximumPageReached(getDSContext(),workspaceID);
        var maxStepAndPage = new StepAndPage(maxStep,maxPage);

    	//----------------------------------------------------------
    	// #2: Determine which page/step the user should be sent to next
    	//-----------------------------------------------------------
        // User clicked "Next->" button (or a Non-interactive Step - i.e. no UI)
        // Only step forward to next page if no errors on this page
        if ((cocoon.request.get(AbstractProcessingStep.NEXT_BUTTON) || !stepHasUI(stepConfig))  && (response_flag==AbstractProcessingStep.STATUS_COMPLETE))
        {
           	state.progressIterator++;

           	var totalSteps = stepsInSubmission.length;
           	var inWorkflow = submissionInfo.isInWorkflow();

           	//check if we've completed the submission
           	if(state.progressIterator >= totalSteps)
           	{
           		if(inWorkflow==false)
           		{
           		  	//Submission is completed!
           			cocoon.log.debug("Submission Completed!");

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
           		cocoon.log.debug("Next Step & Page=" + state.stepAndPage);
        	}
        }//User clicked "<- Previous" button
        else if (cocoon.request.get(AbstractProcessingStep.PREVIOUS_BUTTON) && 
            (response_flag==AbstractProcessingStep.STATUS_COMPLETE || maxStepAndPage.equals(state.stepAndPage)))
        {  
            var stepBack = true;

            //Need to find the previous step which HAS a user interface.
            while(stepBack)
            {
            	state.progressIterator--;
            	if(state.progressIterator<0)
            	    stepBack = false;

            	state.stepAndPage = stepsInSubmission[state.progressIterator];

            	var prevStep = state.stepAndPage.getStep();
            	var prevStepConfig = submissionInfo.getSubmissionConfig().getStep(prevStep);

            	if(!stepHasUI(prevStepConfig))
            		stepBack = true;
            	else
            		stepBack = false;
     		}

            cocoon.log.debug("Previous Step & Page=" + state.stepAndPage);
        }
        // User clicked "Save/Cancel" Button
        else if (cocoon.request.get(AbstractProcessingStep.CANCEL_BUTTON))
        {
        	var inWorkflow = submissionInfo.isInWorkflow();
        	if (inWorkflow && response_flag==AbstractProcessingStep.STATUS_COMPLETE)
        	{
        		var contextPath = cocoon.request.getContextPath();
        		cocoon.redirectTo(contextPath+"/submissions",true);
        		cocoon.exit();
        	}
        	else if (!inWorkflow)
        	{
        			submitStepSaveOrRemove(collectionHandle,workspaceID,step,page);
        	}
        }
        
        //User clicked on Progress Bar:
        // only check for a 'step_jump' (i.e. click on progress bar)
        // if there are no errors to be resolved
        if(response_flag==AbstractProcessingStep.STATUS_COMPLETE || maxStepAndPage.equals(state.stepAndPage))
        {
	        var names = cocoon.request.getParameterNames();
	        while(names.hasMoreElements())
	        {
	            var name = names.nextElement(); 
	            if (name.startsWith(AbstractProcessingStep.PROGRESS_BAR_PREFIX))
	            {
	                var newStepAndPage = name.substring(AbstractProcessingStep.PROGRESS_BAR_PREFIX.length());
	                newStepAndPage = new StepAndPage(newStepAndPage);

	                //only allow a jump to a page user has already been to
	                if (newStepAndPage.isSet() && (newStepAndPage.compareTo(maxStepAndPage) <= 0))
	                {
	                   state.stepAndPage = newStepAndPage;

					   cocoon.log.debug("Jump To Step & Page=" + state.stepAndPage);

	                   //reset progress iterator
	                   for(var i=0; i<stepsInSubmission.length; i++)
	                   {
	                   		if(state.stepAndPage.equals(stepsInSubmission[i]))
	                   		{
	                   			state.progressIterator = i;
	                   			break;
	                   		}
	                   }
	                }
	            }//end if submit_jump pressed
	        }//end while more elements
        }//end if no errors
    } while ( 1 == 1)

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
    var step = stepAndPage.getStep();
	var page = stepAndPage.getPage();
  
  	//-------------------------------------
 	// #1: Check if this step has a UI
 	//-------------------------------------
	//if this step has an XML-UI, then call the generic step transformer
 	//(otherwise, this is just a processing step)
 	if(stepHasUI(stepConfig))
 	{
 		//prepend URI with the handle of the collection, and go there!
 		sendPageAndWait("handle/"+collectionHandle+ "/submit/continue",{"id":workspaceID,"step":String(stepAndPage),"transformer":stepConfig.getXMLUIClassName(),"error":String(response_flag),"error_fields":getErrorFields()});
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
    response_flag = stepClass.doProcessing(getDSContext(), getHttpRequest(), getHttpResponse(), submissionInfo);

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
 * separate attributes: [name]-path and [name]-inputstream. The first
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
    var maxStepAndPage = new StepAndPage(maxStep,maxPage);
    
    var currStepAndPage = new StepAndPage(step,page);

    if (maxStepAndPage.compareTo(currStepAndPage) > 0)
    {
 	   FlowUtils.setBackPageReached(getDSContext(),workspaceID, step, page);
    }

    sendPageAndWait("handle/"+collectionHandle+"/submit/saveOrRemoveStep",{"id":workspaceID,"step":String(step),"page":String(page)});
    
    FlowUtils.processSaveOrRemove(getDSContext(), workspaceID, cocoon.request);
    
    if (cocoon.request.get("submit_save"))
    {
       // Already saved...
       var contextPath = cocoon.request.getContextPath();
       cocoon.redirectTo(contextPath+"/submissions",true);
       cocoon.exit();
    }
    else if (cocoon.request.get("submit_remove"))
    {
        sendPage("handle/"+collectionHandle+"/submit/removedStep");
        cocoon.exit(); // We're done, Stop execution.
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

function doWorkflowEditMetadata() {
    var contextPath = cocoon.request.getContextPath();
    var workflowItemId = cocoon.request.get("workflowID");
    // Get the collection handle for this item.
    var handle = cocoon.parameters["handle"];

    //    submissionControl(handle, "X"+workflowItemId, null);
    if (workflowItemId.startsWith("X")) {
        workflowItemId = workflowItemId.replace("X", "");
        var coll = getWorkflowItemService().find(getDSContext(), workflowItemId).getCollection();
        var workflow = getWorkflowFactory().getWorkflow(coll);
        var step = workflow.getStep(cocoon.request.get("stepID"));
        var action = step.getActionConfig(cocoon.request.get("actionID"));
        submissionControl(handle, "X"+workflowItemId, null);
        cocoon.redirectTo(contextPath+"/handle/"+handle+"/xmlworkflow?"+"workflowID="+workflowItemId+"&stepID="+step.getId()+"&actionID="+action.getId(), true);
        getDSContext().complete();
        cocoon.exit();
    } else {
        workflowItemId = workflowItemId.replace("W", "");
        submissionControl(handle, "W"+workflowItemId, null);
        cocoon.redirectTo(contextPath+"/handle/"+handle+"/workflow?workflowID="+workflowItemId, true);
        getDSContext().complete();
        cocoon.exit();
    }
}
