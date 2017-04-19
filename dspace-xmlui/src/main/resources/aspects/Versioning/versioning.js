/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

importClass(Packages.java.lang.Class);
importClass(Packages.java.lang.ClassLoader);
importClass(Packages.java.util.UUID)

importClass(Packages.org.dspace.app.xmlui.utils.FlowscriptUtils);

importClass(Packages.org.dspace.app.xmlui.aspect.administrative.FlowResult);

importClass(Packages.org.apache.cocoon.environment.http.HttpEnvironment);
importClass(Packages.org.apache.cocoon.servlet.multipart.Part);
importClass(Packages.org.dspace.content.Item);
importClass(Packages.org.dspace.content.factory.ContentServiceFactory)

importClass(Packages.org.dspace.core.Constants);

importClass(Packages.org.dspace.app.xmlui.utils.ContextUtil);
importClass(Packages.org.dspace.app.xmlui.cocoon.HttpServletRequestCocoonWrapper);
importClass(Packages.org.dspace.app.xmlui.aspect.versioning.VersionManager);

importClass(Packages.org.dspace.submit.AbstractProcessingStep);



/* Global variable which stores a comma-separated list of all fields
 * which errored out during processing of the last step.
 */
var ERROR_FIELDS = null;

/**
 * Simple access method to access the current Cocoon object model.
 */
function getObjectModel()
{
  return FlowscriptUtils.getObjectModel(cocoon);
}

function getItemService()
{
    return ContentServiceFactory.getInstance().getItemService();
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

	// Cocoon's request object handles form encoding, thus if the user enters
	// non-ASCII characters such as those found in foreign languages they will
	// come through corrupted if they are not obtained through the Cocoon request
	// object. However, since the dspace-api is built to accept only HttpServletRequest
	// a wrapper class HttpServletRequestCocoonWrapper has been built to translate
	// the Cocoon request back into a servlet request. This is not a fully complete
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
 * Start editing an individual item.
 */
function startCreateNewVersionItem(){
	var itemID = UUID.fromString((cocoon.request.get("itemID")));

	assertEditItem(itemID);

    var result= new FlowResult();
	do{
        result = doCreateNewVersion(itemID, result);
    }while(result!=null);

	var item = getItemService().find(getDSContext(),itemID);

    //Send us back to the item page if we cancel !
    cocoon.redirectTo(cocoon.request.getContextPath() + "/handle/" + item.getHandle(), true);
    getDSContext().complete();
	item = null;
    cocoon.exit();
}

/*
 * Move this item to another collection
 */
function doCreateNewVersion(itemID, result){
    assertEditItem(itemID);
    do {
        sendPageAndWait("item/version/create",{"itemID":itemID, "summary":result.getParameter("summary")}, result);
        
        if (cocoon.request.get("submit_cancel")){
            return null;
        }
        else if (cocoon.request.get("submit_version")){
            var summary = cocoon.request.get("summary");
            assertEditItem(itemID);
            result = VersionManager.processCreateNewVersion(getDSContext(),itemID, summary);

            var wsid = result.getParameter("wsid");
            getDSContext().complete();
            cocoon.redirectTo(cocoon.request.getContextPath()+"/submit?workspaceID=" + wsid,true);
	        cocoon.exit();
        }
        else if (cocoon.request.get("submit_update_version")){
            var summary = cocoon.request.get("summary");
            assertEditItem(itemID);
            result = VersionManager.processUpdateVersion(getDSContext(),itemID, summary);
        }
    } while (result == null || !result.getContinue());

    return result;
}

/**
 * Start editing an individual item.
 */
function startVersionHistoryItem(){
	var itemID = UUID.fromString((cocoon.request.get("itemID")));

	assertEditItem(itemID);

    var result= new FlowResult();
	do{
        result=doVersionHistoryItem(itemID, result);        
    }while(result!=null);
}

function doVersionHistoryItem(itemID, result){
    //var result;
	do {
		sendPageAndWait("item/versionhistory/show",{"itemID":itemID},result);
		assertEditItem(itemID);
        result = null;

        if (cocoon.request.get("submit_cancel")){
            //Pressed the cancel button, redirect us to the item page
            var item = getItemService().find(getDSContext(),itemID);

           	cocoon.redirectTo(cocoon.request.getContextPath()+"/handle/"+item.getHandle(),true);
           	getDSContext().complete();
           	item = null;
           	cocoon.exit();
        }
        else if (cocoon.request.get("submit_delete") && cocoon.request.get("remove")){
			var versionIDs = cocoon.request.getParameterValues("remove");
			result = doDeleteVersions(itemID, versionIDs);
            if(result != null){
                if(result.getParameter("itemID") == null){
                    // We have removed everything, redirect us to the home page !
                    cocoon.redirectTo(cocoon.request.getContextPath(), true);
                    getDSContext().complete();
                    cocoon.exit();
                }else{
                    // Perhaps we have a new item (if we deleted the current version)
                    itemID = result.getParameter("itemID");
                }
            }

		}
        else if (cocoon.request.get("submit_restore") && cocoon.request.get("versionID")){
		    var versionID = cocoon.request.get("versionID");
            itemID = UUID.fromString(cocoon.request.get("itemID"));

		    result = doRestoreVersion(itemID, versionID);
		}
        else if (cocoon.request.get("submit_update") && cocoon.request.get("versionID")){
		    var versionID = cocoon.request.get("versionID");
            itemID = UUID.fromString((cocoon.request.get("itemID")));
		    result = doUpdateVersion(itemID, versionID);
		}
	} while (true)
}

/**
 * Confirm and delete the given version(s)
 */
function doDeleteVersions(itemID, versionIDs){

    sendPageAndWait("item/versionhistory/delete",{"itemID":itemID,"versionIDs":versionIDs.join(',')});

    if (cocoon.request.get("submit_cancel")){
        return null;
    }
    else if (cocoon.request.get("submit_confirm")){
        return VersionManager.processDeleteVersions(getDSContext(), itemID, versionIDs);
    }
    return null;
}


/**
 * Restore the given version
 */
function doRestoreVersion(itemID, versionID){
    var result;
    do {
        sendPageAndWait("item/versionhistory/restore", {"itemID":itemID,"versionID":versionID}, result);
        result = null;
        if (cocoon.request.get("submit_cancel"))
            return null;

        else if (cocoon.request.get("submit_restore")){
            var summary = cocoon.request.get("summary");
            result = VersionManager.processRestoreVersion(getDSContext(),versionID, summary);
        }


    } while (result == null || ! result.getContinue())
    return result;
}


/**
 * Update the given version
 */
function doUpdateVersion(itemID, versionID){
    var result;
    do {
        sendPageAndWait("item/versionhistory/update", {"itemID":itemID,"versionID":versionID}, result);
        result = null;
        if (cocoon.request.get("submit_cancel")){
            return null;
        }
        else if (cocoon.request.get("submit_update")){
            var summary = cocoon.request.get("summary");
            result = VersionManager.processUpdateVersion(getDSContext(),itemID, summary);
        }


    } while (result == null || ! result.getContinue())
    return result;
}


/**
 * Assert that the currently authenticated eperson can edit this item, if they can
 * not then this method will never return.
 */
function assertEditItem(itemID) {

	if ( ! canEditItem(itemID)) {
		sendPage("admin/not-authorized");
		cocoon.exit();
	}
}

/**
 * Return weather the currently authenticated eperson can edit the identified item.
 */
function canEditItem(itemID)
{
    // Navigation already deals with loading the right operation. return always true.
    return true;
}




function sendPageAndWait(uri,bizData,result)
{
    if (bizData == null)
        bizData = {};

    if (result != null)
    {
        var outcome = result.getOutcome();
        var header = result.getHeader();
        var message = result.getMessage();
        var characters = result.getCharacters();

        if (message != null || characters != null)
        {
            bizData["notice"]     = "true";
            bizData["outcome"]    = outcome;
            bizData["header"]     = header;
            bizData["message"]    = message;
            bizData["characters"] = characters;
        }

        var errors = result.getErrorString();
        if (errors != null)
        {
            bizData["errors"] = errors;
        }
    }

    // just to remember where we came from.
    bizData["flow"] = "true";
    cocoon.sendPageAndWait(uri,bizData);
}

/**
 * Send the given page and DO NOT wait for the flow to be continued. Execution will
 * proceed as normal. This method will preform two useful actions: set the flow
 * parameter & add result information.
 *
 * The flow parameter is used by the sitemap to separate requests comming from a
 * flow script from just normal urls.
 *
 * The result object could potentially contain a notice message and a list of
 * errors. If either of these are present then they are added to the sitemap's
 * parameters.
 */
function sendPage(uri,bizData,result)
{
    if (bizData == null)
        bizData = {};

    if (result != null)
    {
        var outcome = result.getOutcome();
        var header = result.getHeader();
        var message = result.getMessage();
        var characters = result.getCharacters();

        if (message != null || characters != null)
        {
            bizData["notice"]     = "true";
            bizData["outcome"]    = outcome;
            bizData["header"]     = header;
            bizData["message"]    = message;
            bizData["characters"] = characters;
        }

        var errors = result.getErrorString();
        if (errors != null)
        {
            bizData["errors"] = errors;
        }
    }

    // just to remember where we came from.
    bizData["flow"] = "true";
    cocoon.sendPage(uri,bizData);
}
