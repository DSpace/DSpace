/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
importClass(Packages.org.dspace.authorize.service.AuthorizeService);
importClass(Packages.org.dspace.authorize.factory.AuthorizeServiceFactory);
importClass(Packages.org.dspace.content.factory.ContentServiceFactory);

importClass(Packages.org.dspace.app.xmlui.utils.FlowscriptUtils);
importClass(Packages.org.dspace.app.xmlui.utils.ContextUtil);
importClass(Packages.org.dspace.sword.client.DSpaceSwordClient);
importClass(Packages.org.dspace.app.xmlui.aspect.swordclient.SelectTargetAction);
importClass(Packages.org.dspace.app.xmlui.aspect.swordclient.SelectCollectionAction);
importClass(Packages.org.dspace.app.xmlui.aspect.swordclient.SelectPackagingAction);
importClass(Packages.org.dspace.app.xmlui.aspect.swordclient.DepositAction);
importClass(Packages.org.dspace.content.Item);

importClass(Packages.java.util.UUID);

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

    // Comment - Ugh ! In Cocoon terms 'sendPage' should be used when there is no 'flow' but Manakin sets this misleading parameter
    // to force it through another part of the sitemap - Robin.
    bizData["flow"] = "true";
    cocoon.sendPage(uri,bizData);
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

function getAuthorizeService()
{
    return AuthorizeServiceFactory.getInstance().getAuthorizeService();
}

function getItemService()
{
    return ContentServiceFactory.getInstance().getItemService();
}


/**
 * Return whether the currently authenticated eperson is an
 * administrator.
 */
function isAdministrator() {
	return getAuthorizeService().isAdmin(getDSContext());
}

/**
 * Assert that the currently authenticated eperson is an administrator.
 * If they are not then an error page is returned and this function
 * will NEVER return.
 */
function assertAdministrator() {

	if ( ! isAdministrator()) {
		sendPage("admin/not-authorized");
		cocoon.exit();
	}
}

 /*********************
  * Entry Point flows
  *********************/

function startSwordDeposit()
{
    assertAdministrator();

    var itemID = UUID.fromString(cocoon.request.get("itemID"));
    var item = getItemService().find(getDSContext(),itemID);
    var handle = item.getHandle();
    var DSClient = new DSpaceSwordClient();
    var result  = null;

    // The URL of the service document (or 'sub' service document).
    var serviceDoc;
    // The URL of the selected collection.
    var location;
    // The available file types. An intersection of what the client and server can support.
    var fileTypes;
    // The available package formats. An intersection of what the client and server can support.
    var packageFormats;

    // Retrieve the high level service document

    result = getServiceDoc(handle, DSClient);
    serviceDoc =  result.getParameter("serviceDoc");

    // Select a collection in which to deposit.

    result = getCollection(handle, serviceDoc, DSClient);
    location = result.getParameter("location");
    serviceDoc = result.getParameter("serviceDoc");
    fileTypes =  result.getParameter("fileTypes");
    packageFormats = result.getParameter("packageFormats");

    // Ask the user to select which file type and package format combo they want.

    getPackageType(handle, fileTypes, packageFormats, location, serviceDoc, DSClient);

    // Now send the item.

    sendItem(handle, DSClient);

}

function getServiceDoc(handle, DSClient)
{
    var selectTargetAction;
    var result;

    do {
        sendPageAndWait("swordclient/select-target", {handle: handle}, result);

        if (cocoon.request.get("submit_next"))
        {
            selectTargetAction = new SelectTargetAction();
            result = selectTargetAction.processSelectTarget(getDSContext(), cocoon.request, DSClient);
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            cocoon.redirectTo(cocoon.request.getContextPath() + "/handle/" + handle, true);
            getDSContext().complete();
            cocoon.exit();
        }
    } while (result == null || !result.getContinue());

    return result;
}


function getCollection(handle, serviceDoc, DSClient)
{
    var selectCollectionAction;
    var selectTargetAction;
    var result;

    do {
        cocoon.request.setAttribute("serviceDoc", serviceDoc);
        sendPageAndWait("swordclient/select-collection", {handle: handle}, result);

        if (cocoon.request.get("deposit"))
        {
            // A collection, or rather the location of one, has been selected, so let's
            // see what file type and package format combos are available.

            // We have a new request so need to attach the service doc again.
            cocoon.request.setAttribute("serviceDoc", serviceDoc);

            selectCollectionAction = new SelectCollectionAction();
            result = selectCollectionAction.processSelectCollection(getDSContext(), cocoon.request, DSClient);
        }
        else if (cocoon.request.get("sub-service"))
        {
            // The user has opted to drill down into a 'sub' service document
            // Note : The 'result' from this action should never have 'continue=true'.

            selectTargetAction = new SelectTargetAction();
            result = selectTargetAction.processSelectSubTarget(getDSContext(), cocoon.request, DSClient);

            // Reset serviceDoc so that when we loop round we display the contents of the new service document.
            serviceDoc =  result.getParameter("serviceDoc");
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            cocoon.redirectTo(cocoon.request.getContextPath() + "/handle/" + handle, true);
            getDSContext().complete();
            cocoon.exit();
        }
    } while (result == null || !result.getContinue());

    return result;

}

function getPackageType(handle, fileTypes, packageFormats, location, serviceDoc, DSClient)
{
    var selectPackagingAction;
    var result;

    do {
        cocoon.request.setAttribute("fileTypes", fileTypes);
        cocoon.request.setAttribute("packageFormats", packageFormats);
        cocoon.request.setAttribute("location", location);
        cocoon.request.setAttribute("serviceDoc", serviceDoc);
        sendPageAndWait("swordclient/select-packaging", {handle: handle}, result);

        if (cocoon.request.get("submit_next"))
        {
            // Update the Sword Client with the selected file type and package format

            selectPackagingAction = new SelectPackagingAction();
            result = selectPackagingAction.processSelectPackaging(getDSContext(), cocoon.request, DSClient);
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            cocoon.redirectTo(cocoon.request.getContextPath() + "/handle/" + handle, true);
            getDSContext().complete();
            cocoon.exit();
        }
    } while (result == null || !result.getContinue());

}

function sendItem(handle, DSClient)
{
    var depositAction;
    var result;

    depositAction = new DepositAction();
    result = depositAction.processDeposit(getDSContext(), handle, DSClient);
    sendPage("swordclient/deposit-response", {handle: handle}, result);

}


