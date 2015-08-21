/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

importClass(Packages.org.dspace.app.xmlui.utils.FlowscriptUtils);
importClass(Packages.org.dspace.app.xmlui.utils.ContextUtil);
importClass(Packages.cz.cuni.mff.ufal.UFALLicenceAgreement);
importClass(Packages.cz.cuni.mff.ufal.UFALLicenceAgreementAgreed);

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
 * Send the current page and wait for the flow to be continued. This method will
 * perform two useful actions: set the flow parameter & add result information.
 *
 * The flow parameter is used by the sitemap to separate requests comming from a
 * flow script from just normal urls.
 *
 * The result object could potentially contain a notice message and a list of
 * errors. If either of these are present then they are added to the sitemap's
 * parameters.
 */
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
 * proceed as normal. This method will perform two useful actions: set the flow
 * parameter & add result information.
 *
 * The flow parameter is used by the sitemap to separate requests coming from a
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

/*********************
 * Entry Point flows
 *********************/


/**
 * Start managing epeople
 */
function startLicenseAgree()
{
    var handleUrl = "handle/" + cocoon.parameters.handle;
    var signatureNeeded = UFALLicenceAgreement.signatureNeeded(getObjectModel());
    var everythingFine = false;
    if(signatureNeeded) {
        everythingFine = doLicenseAgree(handleUrl);
    }

    if(!everythingFine) {
        cocoon.redirectTo(cocoon.request.getContextPath() + "/" + handleUrl, true);
        getDSContext().complete();
    }
}

function doLicenseAgree(handleUrl){
    var result;
    var err;
    do{
        var allzip = false;
        if(cocoon.request.get("allzip")){
            allzip = cocoon.request.get("allzip");
        }
        var bitId = -1;
        if(cocoon.request.get("bitstreamId")){
            bitId = cocoon.request.get("bitstreamId");
        }
        sendPageAndWait(handleUrl + "/ufal-licence-agreement", {"allzip":allzip, "bitstreamId":bitId}, result);
        
        result = UFALLicenceAgreement.validate(getObjectModel());

        if(cocoon.request.get("confirm_license") && result.getContinue()){
            result = null;
            var agreement = new UFALLicenceAgreementAgreed();
            var returnURL = agreement.agree(getDSContext(), getObjectModel(),
                java.lang.Boolean.parseBoolean(allzip), java.lang.Integer.parseInt(bitId));
            if(returnURL != null) {
                cocoon.redirectTo(returnURL, true);
                return true;
            }
            return false;
        }

    }while(true)
}

function assertEperson(){
    if(getDSContext().getCurrentUser() != null){
        return true;
    }
    cocoon.redirectTo(cocoon.request.getContextPath(), true);
    cocoon.exit();
}
