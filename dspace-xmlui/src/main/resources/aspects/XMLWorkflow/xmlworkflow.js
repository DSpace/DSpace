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
importClass(Packages.org.apache.cocoon.environment.http.HttpEnvironment);
importClass(Packages.org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem);
importClass(Packages.org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory);

importClass(Packages.org.dspace.app.xmlui.utils.ContextUtil);
importClass(Packages.org.dspace.app.xmlui.cocoon.HttpServletRequestCocoonWrapper);

/* Global variable which stores a comma-separated list of all fields
 * which errored out during processing of the last step.
 */
var ERROR_FIELDS = null;


function getWorkflowFactory()
{
    return XmlWorkflowServiceFactory.getInstance().getWorkflowFactory();
}

function getWorkflowItemService()
{
    return XmlWorkflowServiceFactory.getInstance().getWorkflowItemService();
}


function getWorkflowService()
{
    return XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService();
}


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
 * This is the starting point for all workflow tasks. The id of the workflow
 * is expected to be passed in as a request parameter. The user will be able
 * to view the item and perform the necessary actions on the task such as:
 * accept, reject, or edit the item's metadata.
 */
function doWorkflow()
{
    var workflowItemId = cocoon.request.get("workflowID");
    // Get the collection handle for this item.
    var coll = getWorkflowItemService().find(getDSContext(), workflowItemId).getCollection();
    var handle = coll.getHandle();
    var workflow = getWorkflowFactory().getWorkflow(coll);
    var step = workflow.getStep(cocoon.request.get("stepID"));


    if (workflowItemId == null)
    {
        throw "Unable to find workflow, no workflow id supplied.";
    }else if(step == null){
        throw "Unable to find step, no step id supplied.";
    }
    var action = step.getActionConfig(cocoon.request.get("actionID"));


    do{
        sendPageAndWait("handle/"+handle+"/xmlworkflow/getTask",{"workflowID":workflowItemId,"stepID":step.getId(),"actionID":action.getId()});

        if (cocoon.request.get("submit_edit"))
        {
            var contextPath = cocoon.request.getContextPath();
            cocoon.redirectTo(contextPath+"/handle/"+handle+"/workflow_edit_metadata?"+"workflowID=X"+workflowItemId+"&stepID="+step.getId()+"&actionID="+action.getId(), true);
            getDSContext().complete();
            cocoon.exit();
//            submissionControl(handle, "X"+workflowItemId, null);
            //Check if we have pressed the submit_full_item_info button
        }else if((cocoon.request.get("submit_full_item_info") && !cocoon.request.get("submit_full_item_info").equals("true")) || cocoon.request.get("submit_simple_item_info")){
            //Don't do anything just go back to the start of the loop
        }else{
            try{
                action = getWorkflowService().doState(getDSContext(), getDSContext().getCurrentUser(), getHttpRequest(), workflowItemId, workflow, action);
            }catch(exception){
                sendPage("handle/"+handle+"/xmlworkflow/workflowexception",{"error":exception.toString()});
                cocoon.exit();
            }
            if(action == null){
                var contextPath = cocoon.request.getContextPath();
                cocoon.redirectTo(contextPath+"/submissions",true);
                getDSContext().complete();
                cocoon.exit();
            }
        }

    }while(true);

}
