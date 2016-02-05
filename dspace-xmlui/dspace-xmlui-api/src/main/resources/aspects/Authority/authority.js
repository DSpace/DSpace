importClass(Packages.org.dspace.authorize.AuthorizeManager);
importClass(Packages.org.dspace.app.xmlui.utils.ContextUtil);

importClass(Packages.org.dspace.core.Constants);
importClass(Packages.org.dspace.harvest.HarvestedCollection);
importClass(Packages.org.dspace.app.util.Util);

importClass(Packages.java.util.Set);

importClass(Packages.org.dspace.app.xmlui.utils.FlowscriptUtils);


importClass(Packages.org.dspace.app.xmlui.aspect.authority.concept.FlowConceptUtils);
importClass(Packages.org.dspace.app.xmlui.aspect.authority.term.FlowTermUtils);
importClass(Packages.org.dspace.app.xmlui.aspect.authority.scheme.FlowSchemeUtils);
importClass(Packages.org.dspace.app.xmlui.aspect.authority.AuthorityUtils);

importClass(Packages.java.lang.System);
importClass(Packages.org.dspace.core.ConfigurationManager);

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

/*
 * This function cycles through the submit parameters until it finds one that starts with "prefix".
 * It then returns everything after the prefix for the first match. If none are found, null is returned.
 * For example, if one of the submit variables is named "submit_add_2123", calling this function with a
 * prefix variable of "submit_add_" will return 2123.
 */
function extractSubmitSuffix(prefix) {

    var names = cocoon.request.getParameterNames();
    while (names.hasMoreElements())
    {
        var name = names.nextElement();
        if (name.startsWith(prefix))
        {
            var extractedSuffix = name.substring(prefix.length);
            return extractedSuffix;
        }
    }
    return null;
}

/*
 * Return whether the currently authenticated Concept is authorized to
 * perform the given action over/on the the given object.
 */
function isAuthorized(objectType, objectID, action) {

    // Note: it's okay to instantiate a DSpace object here because
    // under all cases this method will exit and the objects sent
    // for garbage collecting before and continuations and are used.

    var object = null;
    switch (objectType) {
        case Constants.BITSTREAM:
            object = Bitstream.find(getDSContext(),objectID);
            break;
        case Constants.BUNDLE:
            object = Bundle.find(getDSContext(),objectID);
            break;
        case Constants.ITEM:
            object = Item.find(getDSContext(),objectID);
            break;
        case Constants.COLLECTION:
            object = Collection.find(getDSContext(),objectID);
            break;
        case Constants.COMMUNITY:
            object = Community.find(getDSContext(),objectID);
            break;
        case Constants.GROUP:
            object = Group.find(getDSContext(),objectID);
            break;
        case Constants.EPERSON:
            object = Concept.find(getDSContext(),objectID);
            break;
    }

    // If we couldn't find the object then return false
    if (object == null)
        return false;

    return AuthorizeManager.authorizeActionBoolean(getDSContext(),object,action);
}

/**
 * Assert that the currently authenticated concept is able to perform
 * the given action over the given object. If they are not then an
 * error page is returned and this function will NEVER return.
 */
function assertAuthorized(objectType, objectID, action) {

    if ( ! isAuthorized(objectType, objectID, action)) {
        sendPage("admin/not-authorized");
        cocoon.exit();
    }
}

/**
 * Return whether the currently authenticated concept is an
 * administrator.
 */
function isAdministrator() {
    return AuthorizeManager.isAdmin(getDSContext());
}

/**
 * Assert that the currently authenticated concept is an administrator.
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
 * Entry Point flows for managing scheme
 *********************/


/**
 * Start managing term
 */
function startManageScheme()
{
    assertAdministrator();

    doManageScheme();

    // This should never return, but just in case it does then point
    // the user to the home page.
    cocoon.redirectTo(cocoon.request.getContextPath());
    getDSContext().complete();
    cocoon.exit();
}

/**
 * Start editing an individual community
 */
function startEditSchemeMetadata()
{
    var schemeId = cocoon.request.get("schemeID");
    doEditSchemeMetadata(schemeId);

    cocoon.redirectTo(cocoon.request.getContextPath()+"/scheme/"+schemeId,true);
    getDSContext().complete();
    cocoon.exit();
}

/**
 * Start editing an individual community
 */
function startEditScheme()
{
    //var schemeId = cocoon.request.get("scheme");
    // var schemeId = cocoon.parameters["scheme"];
    var schemeId = cocoon.request.get("schemeID");
    doEditScheme(schemeId);

    cocoon.redirectTo(cocoon.request.getContextPath()+"/scheme/"+schemeId,true);
    getDSContext().complete();
    cocoon.exit();
}

function doManageScheme()
{
    assertAdministrator();

    var query = "";
    var page = 0;
    var highlightID = -1;
    var result;
    do {

        sendPageAndWait("admin/scheme/main",{"page":page,"query":escape(query),"highlightID":highlightID},result);
        result = null;

        // Update the page parameter if supplied.
        if (cocoon.request.get("page"))
            page = cocoon.request.get("page");

        if (cocoon.request.get("submit_search"))
        {
            // Grab the new query and reset the page parameter
            query = cocoon.request.get("query");
            page = 0
            highlightID = -1;
        }
        else if (cocoon.request.get("submit_add"))
        {
            // Add a new metadatascheme
            assertAdministrator();

            result = doAddScheme();

            if (result != null && result.getParameter("schemeId"))
                highlightID = result.getParameter("schemeId");
        }
        else if (cocoon.request.get("submit_edit") && cocoon.request.get("schemeId"))
        {
            // Edit an existing metadatascheme
            assertAdministrator();

            var schemeId = cocoon.request.get("schemeId");
            result = doEditScheme(schemeId);
            highlightID = schemeId;

        }
        else if (cocoon.request.get("submit_delete") && cocoon.request.get("select_schemes"))
        {
            // Delete a set of metadatascheme
            assertAdministrator();

            var schemeIds = cocoon.request.getParameterValues("select_schemes");
            result = doDeleteScheme(schemeIds);
            highlightID = -1;
        }
        else if (cocoon.request.get("submit_return"))
        {
            // Not implemented in the UI, but should be incase someone links to us.
            return;
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            // Not implemented in the UI, but should be incase someone links to us.
            return;
        }
    } while (true) // only way to exit is to hit the submit_back button.
}



/**
 * Add a new metadatascheme, the user is presented with a form to create a new metadatascheme. They will
 * repeat this form until the user has supplied a unique email address, first name, and
 * last name.
 */
function doAddScheme()
{
    assertAdministrator();

    var result;
    var errorMessage;
    do {
        sendPageAndWait("admin/scheme/add",{"errors":errorMessage},result);
        result = null;

        if (cocoon.request.get("submit_save"))
        {
            // Save the new metadatascheme, assuming they have met all the requirements.
            assertAdministrator();

            result = FlowSchemeUtils.processAddScheme(getDSContext(),cocoon.request,getObjectModel());
            errorMessage = result.getErrorString();
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            // The user can cancel at any time.
            return null;
        }

    } while (result == null || !result.getContinue())

    return result;
}

function doDeleteScheme(schemeIds)
{

    /**
     * Confirm and delete the list of given epeople. The user will be presented with a list of selected epeople,
     * and if they click the confirm delete button then the epeople will be deleted.
     */

    assertAdministrator();

    sendPageAndWait("admin/scheme/delete",{"schemeIds":schemeIds.join(',')});

    if (cocoon.request.get("submit_confirm"))
    {
        // The user has confirmed, actually perform the deletion.
        assertAdministrator();

        var result = FlowSchemeUtils.processDeleteScheme(getDSContext(),schemeIds);
        return result;
    }
    return null;
}
/**
 * Edit an existing metadatascheme, all standard metadata elements are presented for editing. The same
 * validation is required as is in adding a new metadatascheme: unique email, first name, and last name
 */
function doEditScheme(schemeId)
{
    // We can't assert any privileges at this point, the user could be a collection
    // admin or a supper admin. Instead we protect each operation.
    var result;

    do {
        sendPageAndWait("admin/scheme/edit",{"schemeId":schemeId},result);
        result == null;

        if (cocoon.request.get("submit_save"))
        {
            // Attempt to save the changes.
            assertAdministrator();
            result = FlowSchemeUtils.processEditScheme(getDSContext(),cocoon.request,getObjectModel(),schemeId);
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            // Cancel out and return to wherever the user came from.
            return null;
        }
        else if (cocoon.request.get("submit_delete") && cocoon.request.get("conceptIds"))
        {
            // Delete this user
            assertAdministrator();
            var conceptIds = new Array();
            conceptIds = cocoon.request.get("conceptIds");
            result = FlowSchemeUtils.doDeleteConceptFromScheme(getDSContext(),cocoon.request,getObjectModel(),conceptIds);

            // No matter what just bail out to the group list.
            return null;
        }

    } while (result == null || !result.getContinue())

    return result;
}


function doManageConceptsInScheme(){
    assertAdministrator();
    var schemeId = cocoon.parameters["scheme"];
    var result;
    do {

        sendPageAndWait("admin/scheme/"+schemeId+"/concept/main",{"schemeId":schemeId},result);
        result = null;


        if (cocoon.request.get("submit_add"))
        {
            // Add a new metadatascheme
            assertAdministrator();

            result = doAddConcept2Scheme(schemeId);

        }
        else if (cocoon.request.get("submit_edit") && cocoon.request.get("conceptId"))
        {
            // Edit an existing metadatascheme
            assertAdministrator();
            var conceptId = cocoon.request.get("conceptId");
            result = doEditConcept(conceptId);

        }
        else if (cocoon.request.get("submit_delete") && cocoon.request.get("select_conceptIds"))
        {
            // Delete a set of metadatascheme
            assertAdministrator();

            var conceptIds = cocoon.request.getParameterValues("select_conceptIds");
            result = doDeleteConceptFromScheme(conceptIds,schemeId);

        }
        else if (cocoon.request.get("submit_return"))
        {
            // Not implemented in the UI, but should be incase someone links to us.
            return;
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            // Not implemented in the UI, but should be incase someone links to us.
            return;
        }
    } while (true) // only way to exit is to hit the submit_back button.
}
function doAddConcept2Scheme(schemeId){
    // We can't assert any privileges at this point, the user could be a collection
    // admin or a supper admin. Instead we protect each operation.
    assertAdministrator();

    var result;
    var errorMessage;
    do {
        sendPageAndWait("/admin/scheme/"+schemeId+"/concept/add",{"schemeId":schemeId,"errors":errorMessage},result);
        result = null;

        if (cocoon.request.get("submit_save"))
        {
            // Save the new term, assuming they have met all the requirements.
            assertAdministrator();

            result = FlowConceptUtils.processAddConcept(getDSContext(),schemeId, cocoon.request,getObjectModel());
            errorMessage = result.getErrorString();
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            // The user can cancel at any time.
            return null;
        }

    } while (result == null || !result.getContinue())

    return result;
}


/*********************
 * Entry Point flows for managing concept
 *********************/


/**
 * Start managing term
 */
function startManageConcept()
{
    assertAdministrator();
    var schemeId = cocoon.request.get("schemeID");
    doManageConcept();

    // This should never return, but just in case it does then point
    // the user to the home page.

    cocoon.redirectTo(cocoon.request.getContextPath()+"/scheme/"+schemeId,true);
    getDSContext().complete();
    cocoon.exit();
}



/**
 * Start editing an individual community
 */
function startEditConcept()
{
    var conceptId = cocoon.request.get("conceptID");
    doEditConcept(conceptId);

    cocoon.redirectTo(cocoon.request.getContextPath()+"/concept/"+conceptId,true);
    getDSContext().complete();
    cocoon.exit();
}
/**
 * Start editing an individual community
 */
function startEditConceptMetadata()
{
    var conceptId = cocoon.request.get("conceptID");
    doEditConceptMetadata(conceptId);

    cocoon.redirectTo(cocoon.request.getContextPath()+"/concept/"+conceptId,true);
    getDSContext().complete();
    cocoon.exit();
}
function doManageConcept()
{
    assertAdministrator();

    var query = "";
    var page = 0;
    var highlightID = -1;
    var result;
    var schemeId = cocoon.request.get("schemeID");
    do {

        sendPageAndWait("admin/concept/main",{"page":page,"query":escape(query),"highlightID":highlightID,"schemeId":schemeId},result);
        result = null;

        // Update the page parameter if supplied.
        if (cocoon.request.get("page"))
            page = cocoon.request.get("page");

        if (cocoon.request.get("submit_search"))
        {
            // Grab the new query and reset the page parameter
            query = cocoon.request.get("query");
            page = 0
            highlightID = -1;
        }
        else if (cocoon.request.get("submit_add"))
        {
            // Add a new concept
            assertAdministrator();

            result = doAddConcept(schemeId);

            if (result != null && result.getParameter("conceptId"))
                highlightID = result.getParameter("conceptId");
        }
        else if (cocoon.request.get("submit_edit") && cocoon.request.get("conceptId"))
        {
            // Edit an existing concept
            assertAdministrator();

            var conceptId = cocoon.request.get("conceptId");
            result = doEditConcept(conceptId);
            highlightID = conceptId;

        }
        else if (cocoon.request.get("submit_delete") && cocoon.request.get("select_concepts"))
        {
            // Delete a set of concept
            assertAdministrator();

            var conceptIds = cocoon.request.getParameterValues("select_concepts");
            result = doDeleteConcept(schemeId,conceptIds);
            highlightID = -1;
        }
        else if (cocoon.request.get("submit_return"))
        {
            // Not implemented in the UI, but should be incase someone links to us.
            return;
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            // Not implemented in the UI, but should be incase someone links to us.
            return;
        }
        else if (cocoon.request.get("submit_edit_concept")&&cocoon.request.get("conceptID"))
        {
            // Jump to a specific EPerson
            var conceptID = cocoon.request.get("conceptID");
            result = doEditConcept(conceptID);

            if (result != null)
                result.setContinue(false);
        }

    } while (true) // only way to exit is to hit the submit_back button.
}



/**
 * Add a new concept, the user is presented with a form to create a new concept. They will
 * repeat this form until the user has supplied a unique email address, first name, and
 * last name.
 */
function doAddConcept(schemeId)
{
    assertAdministrator();

    var result;
    var errorMessage;
    do {
        sendPageAndWait("admin/concept/add",{"errors":errorMessage},result);
        result = null;

        if (cocoon.request.get("submit_save"))
        {
            // Save the new concept, assuming they have met all the requirements.
            assertAdministrator();

            result = FlowConceptUtils.processAddConcept(getDSContext(),schemeId,cocoon.request,getObjectModel());
            var conceptId = result.getParameter("ConceptID");
            errorMessage = result.getErrorString();

        }
        else if (cocoon.request.get("submit_cancel"))
        {
            // The user can cancel at any time.
            return null;
        }

    } while (result == null || !result.getContinue())

    return result;
}

function doDeleteConcept(schemeId,conceptIds)
{

    /**
     * Confirm and delete the list of given epeople. The user will be presented with a list of selected epeople,
     * and if they click the confirm delete button then the epeople will be deleted.
     */

    assertAdministrator();

    sendPageAndWait("admin/concept/delete",{"conceptIds":conceptIds.join(','),"schemeId":schemeId});

    if (cocoon.request.get("submit_confirm"))
    {
        // The user has confirmed, actually perform the deletion.
        assertAdministrator();
        var result = FlowConceptUtils.processDeleteConcept(getDSContext(),conceptIds);
        return result;
    }
    return null;
}
/**
 * Edit an existing concept, all standard metadata elements are presented for editing. The same
 * validation is required as is in adding a new concept: unique email, first name, and last name
 */
function doEditConcept(conceptId)
{
    // We can't assert any privileges at this point, the user could be a collection
    // admin or a supper admin. Instead we protect each operation.
    var result;

    do {
        sendPageAndWait("admin/concept/edit",{"conceptId":conceptId},result);
        result == null;

        if (cocoon.request.get("submit_save"))
        {
            // Attempt to save the changes.
            assertAdministrator();
            result = FlowConceptUtils.processEditConcept(getDSContext(),cocoon.request,getObjectModel(),conceptId);
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            // Cancel out and return to wherever the user came from.
            return null;
        }
        else if (cocoon.request.get("submit_delete") && cocoon.request.get("conceptIds"))
        {
            // Delete this user
            assertAdministrator();
            var conceptIds = new Array();
            conceptIds[0] = cocoon.request.get("conceptId");
            result = FlowConceptUtils.doDeleteTermFromConcept(getDSContext(),cocoon.request,getObjectModel(),conceptIds);

            // No matter what just bail out to the group list.
            return null;
        }
        var names = cocoon.request.getParameterNames();
        while (names.hasMoreElements())
        {
            var name = names.nextElement();
            var match = null;

            if ((match = name.match(/submit_add_concept_(\d+)/)) != null)
            {
                // Delete this user
                assertAdministrator();
                var addConceptId = match[1];
                FlowConceptUtils.addConcept2Concept(getDSContext(), conceptId,addConceptId);

            }
        }

    } while (result == null || !result.getContinue())

    return result;
}


function doEditConceptMetadata(conceptId)
{
    // We can't assert any privileges at this point, the user could be a collection
    // admin or a supper admin. Instead we protect each operation.
    var result;

    do {
        sendPageAndWait("admin/concept/editMetadata",{"conceptId":conceptId},result);
        result == null;

        if (cocoon.request.get("submit_update"))
        {
            // Attempt to save the changes.
            assertAdministrator();
            result = FlowConceptUtils.processEditConceptMetadata(getDSContext(),conceptId,cocoon.request);
        }
        else if (cocoon.request.get("submit_return"))
        {
            // Cancel out and return to wherever the user came from.
            return null;
        }
        else if (cocoon.request.get("submit_delete"))
        {
            // Delete this user
            assertAdministrator();
            result = FlowConceptUtils.doDeleteMetadataFromConcept(getDSContext(),conceptId,cocoon.request);

        }
        else if (cocoon.request.get("submit_add"))
        {
            // Delete this user
            assertAdministrator();
            result = FlowConceptUtils.doAddMetadataToConcept(getDSContext(),conceptId,cocoon.request);

        }

    } while (result == null || !result.getContinue())

    return result;
}
function doEditSchemeMetadata(schemeId)
{
    // We can't assert any privileges at this point, the user could be a collection
    // admin or a supper admin. Instead we protect each operation.
    var result;

    do {
        sendPageAndWait("admin/scheme/editMetadata",{"schemeId":schemeId},result);
        result == null;

        if (cocoon.request.get("submit_update"))
        {
            // Attempt to save the changes.
            assertAdministrator();
            result = FlowSchemeUtils.processEditSchemeMetadata(getDSContext(),schemeId,cocoon.request);
        }
        else if (cocoon.request.get("submit_return"))
        {
            // Cancel out and return to wherever the user came from.
            return null;
        }
        else if (cocoon.request.get("submit_delete"))
        {
            // Delete this user
            assertAdministrator();
            result = FlowSchemeUtils.doDeleteMetadataFromScheme(getDSContext(),schemeId,cocoon.request);

        }
        else if (cocoon.request.get("submit_add"))
        {
            // Delete this user
            assertAdministrator();
            result = FlowSchemeUtils.doAddMetadataToScheme(getDSContext(),schemeId,cocoon.request);

        }

    } while (result == null || !result.getContinue())

    return result;
}
function doEditTermMetadata(termId)
{
    // We can't assert any privileges at this point, the user could be a collection
    // admin or a supper admin. Instead we protect each operation.
    var result;

    do {
        sendPageAndWait("admin/term/editMetadata",{"termId":termId},result);
        result == null;

        if (cocoon.request.get("submit_update"))
        {
            // Attempt to save the changes.
            assertAdministrator();
            result = FlowTermUtils.processEditTermMetadata(getDSContext(),termId,cocoon.request);
        }
        else if (cocoon.request.get("submit_return"))
        {
            // Cancel out and return to wherever the user came from.
            return null;
        }
        else if (cocoon.request.get("submit_delete"))
        {
            // Delete this user
            assertAdministrator();
            result = FlowTermUtils.doDeleteMetadataFromTerm(getDSContext(),termId,cocoon.request);

        }
        else if (cocoon.request.get("submit_add"))
        {
            // Delete this user
            assertAdministrator();
            result = FlowTermUtils.doAddMetadataToTerm(getDSContext(),termId,cocoon.request);

        }

    } while (result == null || !result.getContinue())

    return result;
}



function doManageConceptsInConcept(){
    assertAdministrator();
    var conceptId = cocoon.parameters["concept"];
    var result;
    do {

        sendPageAndWait("admin/concept/"+conceptId+"/term/main",{"conceptId":conceptId},result);
        result = null;


        if (cocoon.request.get("submit_add"))
        {
            // Add a new concept
            assertAdministrator();

            result = doAddConcept2Concept(conceptId);

        }
        else if (cocoon.request.get("submit_edit") && cocoon.request.get("conceptId"))
        {
            // Edit an existing concept
            assertAdministrator();
            var conceptId = cocoon.request.get("conceptId");
            result = doEditConcept(conceptId);

        }
        else if (cocoon.request.get("submit_delete") && cocoon.request.get("select_conceptIds"))
        {
            // Delete a set of concept
            assertAdministrator();

            var conceptIds = cocoon.request.getParameterValues("select_conceptIds");
            result = doDeleteTermFromConcept(conceptIds,conceptId);

        }
        else if (cocoon.request.get("submit_return"))
        {
            // Not implemented in the UI, but should be incase someone links to us.
            return;
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            // Not implemented in the UI, but should be incase someone links to us.
            return;
        }

    } while (true) // only way to exit is to hit the submit_back button.
}


function doDeleteTermFromConcept(conceptId,termIds)
{

    /**
     * Confirm and delete the list of given epeople. The user will be presented with a list of selected epeople,
     * and if they click the confirm delete button then the epeople will be deleted.
     */

    assertAdministrator();

    sendPageAndWait("admin/concept/"+conceptId+"/concept/delete",{"conceptId":conceptId,"termId":termIds.join(',')});

    if (cocoon.request.get("submit_confirm"))
    {
        // The user has confirmed, actually perform the deletion.
        assertAdministrator();

        var result = FlowConceptUtils.doDeleteTermFromConcept(getDSContext(),cocoon.request,getObjectModel(),termIds);
        return result;
    }
    return null;
}


/*********************
 * Entry Point flows for managing term
 *********************/


/**
 * Start managing term
 */
function startManageTerm()
{
    assertAdministrator();

    var conceptId = cocoon.request.get("conceptID");
    doManageTerm();

    // This should never return, but just in case it does then point
    // the user to the home page.
    cocoon.redirectTo(cocoon.request.getContextPath()+"/concept/"+conceptId,true);
    getDSContext().complete();
    cocoon.exit();
}



/**
 * Start editing an individual community
 */
function startEditTerm()
{
    var termId = cocoon.request.get("termID");
    doEditTerm(termId);

    cocoon.redirectTo(cocoon.request.getContextPath()+"/term/"+termId,true);
    getDSContext().complete();
    cocoon.exit();
}  /**
 * Start editing an individual community
 */
function startEditTermMetadata()
{
    var termId = cocoon.request.get("termID");
    doEditTermMetadata(termId);

    cocoon.redirectTo(cocoon.request.getContextPath()+"/term/"+termId,true);
    getDSContext().complete();
    cocoon.exit();
}

function doManageTerm()
{
    assertAdministrator();
    var conceptId = cocoon.request.get("conceptID");
    var query = "";
    var page = 0;
    var highlightID = -1;
    var result;
    do {

        sendPageAndWait("admin/term/main",{"page":page,"query":escape(query),"highlightID":highlightID,"conceptId":conceptId},result);
        result = null;

        // Update the page parameter if supplied.
        if (cocoon.request.get("page"))
            page = cocoon.request.get("page");

        if (cocoon.request.get("submit_search"))
        {
            // Grab the new query and reset the page parameter
            query = cocoon.request.get("query");
            page = 0
            highlightID = -1;
        }
        else if (cocoon.request.get("submit_add"))
        {
            // Add a new term
            assertAdministrator();
            result = doAddTerm(conceptId,cocoon.request);

            if (result != null && result.getParameter("termId"))
                highlightID = result.getParameter("termId");
        }
        else if (cocoon.request.get("submit_edit") && cocoon.request.get("termId"))
        {
            // Edit an existing term
            assertAdministrator();

            var termId = cocoon.request.get("termId");
            result = doEditTerm(termId);
            highlightID = termId;

        }
        else if (cocoon.request.get("submit_delete") && cocoon.request.get("select_terms"))
        {
            // Delete a set of term
            assertAdministrator();

            var termIds = cocoon.request.getParameterValues("select_terms");
            result = doDeleteTerm(conceptId,termIds);
            highlightID = -1;
        }
        else if (cocoon.request.get("submit_return"))
        {
            // Not implemented in the UI, but should be incase someone links to us.
            return;
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            // Not implemented in the UI, but should be incase someone links to us.
            return;
        }
    } while (true) // only way to exit is to hit the submit_back button.
}



/**
 * Add a new term, the user is presented with a form to create a new term. They will
 * repeat this form until the user has supplied a unique email address, first name, and
 * last name.
 */
function doAddTerm(conceptId,request)
{
    assertAdministrator();

    var result;
    var errorMessage;
    do {
        sendPageAndWait("admin/term/add",{"errors":errorMessage,"conceptId":conceptId},result);
        result = null;
        if (cocoon.request.get("submit_save"))
        {
            // Save the new term, assuming they have met all the requirements.
            assertAdministrator();

            result = FlowTermUtils.processAddTerm(getDSContext(),conceptId,cocoon.request,getObjectModel());

            var termId = result.getParameter("termID");
            errorMessage = result.getErrorString();
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            // The user can cancel at any time.
            return null;
        }

    } while (result == null || !result.getContinue())

    return result;
}

function doDeleteTerm(conceptId,termIds)
{

    /**
     * Confirm and delete the list of given epeople. The user will be presented with a list of selected epeople,
     * and if they click the confirm delete button then the epeople will be deleted.
     */
    var errorMessage;
    assertAdministrator();
    do{
        sendPageAndWait("admin/term/delete",{"termIds":termIds.join(','),"conceptId":conceptId,"errors":errorMessage});

        if (cocoon.request.get("submit_confirm"))
        {
            // The user has confirmed, actually perform the deletion.
            assertAdministrator();
            var result = FlowTermUtils.processDeleteTerm(getDSContext(),termIds);
            errorMessage = result.getErrorString();
            if(errorMessage==null){
                return result;
            }
            else
            {
                result =null;
            }
        }
    } while (result == null || !result.getContinue())

    return null;
}
/**
 * Edit an existing term, all standard metadata elements are presented for editing. The same
 * validation is required as is in adding a new term: unique email, first name, and last name
 */
function doEditTerm(termId)
{
    // We can't assert any privileges at this point, the user could be a collection
    // admin or a supper admin. Instead we protect each operation.
    var result;

    do {
        sendPageAndWait("admin/term/edit",{"termId":termId},result);
        result == null;

        if (cocoon.request.get("submit_save"))
        {
            // Attempt to save the changes.
            assertAdministrator();
            result = FlowTermUtils.processEditTerm(getDSContext(),cocoon.request,getObjectModel(),termId);
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            // Cancel out and return to wherever the user came from.
            return null;
        }
        else if (cocoon.request.get("submit_delete") && cocoon.request.get("termIds"))
        {
            // Delete this user
            assertAdministrator();
            var termIds = new Array();
            termIds[0] = cocoon.request.get("termId");
            result = FlowTermUtils.processDeleteTerm(getDSContext(),termIds);

            // No matter what just bail out to the group list.
            return null;
        }
        var names = cocoon.request.getParameterNames();
        while (names.hasMoreElements())
        {
            var name = names.nextElement();
            var match = null;

            if ((match = name.match(/submit_add_term_(\d+)/)) != null)
            {
                // Delete this user
                assertAdministrator();
                var addTermId = match[1];
                FlowTermUtils.addTerm2Concept(getDSContext(), termId,addTermId);

            }
        }

    } while (result == null || !result.getContinue())

    return result;
}

/**
 * Start editing an individual concept 2 concept relation
 */
function startAddConceptRelation()
{
    var conceptId = cocoon.request.get("conceptID");
    doAddConceptRelation(conceptId);

    cocoon.redirectTo(cocoon.request.getContextPath()+"/concept/"+conceptId,true);
    getDSContext().complete();
    cocoon.exit();
}

function doAddConceptRelation(conceptId)
{
    // We can't assert any privileges at this point, the user could be a collection
    // admin or a supper admin. Instead we protect each operation.
    var result;
    var errorMessage;
    var query = "";
    var page = 0;
    var highlightID = -1;
    do {
        sendPageAndWait("admin/concept/addRelation",{"conceptId":conceptId,"query":query,"errors":errorMessage,"page":page,"query":escape(query),"highlightID":highlightID},result);
        result == null;

        // Update the page parameter if supplied.
        if (cocoon.request.get("page"))
            page = cocoon.request.get("page");


        if (cocoon.request.get("submit_add"))
        {
            // Attempt to save the changes.
            assertAdministrator();
            result = FlowConceptUtils.doAddConceptToConcept(getDSContext(),conceptId,cocoon.request);
            errorMessage = result.getErrorString();
        }
        else if (cocoon.request.get("submit_return"))
        {
            // Cancel out and return to wherever the user came from.
            return null;
        }
        if (cocoon.request.get("submit_search"))
        {
            // Grab the new query and reset the page parameter
            query = cocoon.request.get("query");
            page = 0
            highlightID = -1;
        }

    } while (result == null || !result.getContinue())

    return result;
}
