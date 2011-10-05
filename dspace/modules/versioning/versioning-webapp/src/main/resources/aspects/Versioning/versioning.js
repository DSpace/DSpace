/*
 * versioning.js
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

importClass(Packages.org.dspace.app.xmlui.aspect.administrative.FlowResult);

importClass(Packages.org.apache.cocoon.environment.http.HttpEnvironment);
importClass(Packages.org.apache.cocoon.servlet.multipart.Part);
importClass(Packages.org.dspace.content.Item);

importClass(Packages.org.dspace.handle.HandleManager);
importClass(Packages.org.dspace.core.Constants);
importClass(Packages.org.dspace.authorize.AuthorizeManager);
importClass(Packages.org.dspace.license.CreativeCommons);

importClass(Packages.org.dspace.app.xmlui.utils.ContextUtil);
importClass(Packages.org.dspace.app.xmlui.cocoon.HttpServletRequestCocoonWrapper);
importClass(Packages.org.dspace.app.xmlui.aspect.versioning.VersionManager);

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
 * Start editing an individual item.
 */
function startCreateNewVersionItem(){
	var itemID = cocoon.request.get("itemID");

	assertEditItem(itemID);

    var result= new FlowResult();
	do{
        result=doCreateNewVersion(itemID, result);
    }while(result!=null);

	var item = Item.find(getDSContext(),itemID);

	cocoon.redirectTo(cocoon.request.getContextPath()+"/handle/"+item.getHandle(),true);
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
	var itemID = cocoon.request.get("itemID");

	assertEditItem(itemID);

    var result= new FlowResult();
	do{
        result=doVersionHistoryItem(itemID, result);        
    }while(result!=null);

	var item = Item.find(getDSContext(),itemID);

	cocoon.redirectTo(cocoon.request.getContextPath()+"/handle/"+item.getHandle(),true);
	getDSContext().complete();
	item = null;
	cocoon.exit();
}

function doVersionHistoryItem(itemID, result){
    //var result;
	do {
		sendPageAndWait("item/versionhistory/show",{"itemID":itemID},result);
		assertEditItem(itemID);
        result = null;

        if (cocoon.request.get("submit_cancel")){
            return null;
        }
        else if (cocoon.request.get("submit_delete") && cocoon.request.get("remove")){
			var versionIDs = cocoon.request.getParameterValues("remove");
			result = doDeleteVersions(itemID, versionIDs);
		}
        else if (cocoon.request.get("submit_restore") && cocoon.request.get("versionID")){
		    var versionID = cocoon.request.get("versionID");
            itemID = cocoon.request.get("itemID");

		    result = doRestoreVersion(itemID, versionID);
		}
        else if (cocoon.request.get("submit_update") && cocoon.request.get("versionID")){
		    var versionID = cocoon.request.get("versionID");
            itemID = cocoon.request.get("itemID");
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
        var result = VersionManager.processDeleteVersions(getDSContext(),versionIDs);
        return result;
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
	var item = Item.find(getDSContext(),itemID);
	return item.canEdit();
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
 * Send the given page and DO NOT wait for the flow to be continued. Excution will
 * proccede as normal. This method will preform two usefull actions: set the flow
 * parameter & add result information.
 *
 * The flow parameter is used by the sitemap to seperate requests comming from a
 * flow script from just normal urls.
 *
 * The result object could potentialy contain a notice message and a list of
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