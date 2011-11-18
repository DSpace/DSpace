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
importClass(Packages.org.dspace.app.xmlui.aspect.administrative.FlowItemUtils);
importClass(Packages.org.datadryad.app.xmlui.aspect.ame.AMEUtils);

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
function startAME(){
	var itemID = cocoon.request.get("itemID");
	
	assertEditItem(itemID);

    doAME(itemID);

	var item = Item.find(getDSContext(),itemID);

	cocoon.redirectTo(cocoon.request.getContextPath()+"/handle/"+item.getHandle(),true);
	getDSContext().complete();
	item = null;
	cocoon.exit();
}

function doAME(itemID){
    assertEditItem(itemID);
    var result;
    do {
        sendPageAndWait("item/ame/edit",{"itemID":itemID}, result);
        
        if (cocoon.request.get("submit_return")){
            return null;
        }
        else if (cocoon.request.get("submit_remove")) {
        	result = AMEUtils.processRemove(getDSContext(),itemID,cocoon.request);
        }
		else if (cocoon.request.get("submit_update")) {
			result = AMEUtils.processUpdate(getDSContext(),itemID,cocoon.request); 
		}
        else {
        	result = AMEUtils.processAdd(getDSContext(),itemID,cocoon.request);
		}    
    } while (true);
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