/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

importClass(Packages.org.dspace.authorize.AuthorizeManager);
importClass(Packages.org.dspace.core.Constants);
importClass(Packages.org.dspace.content.Bitstream);
importClass(Packages.org.dspace.content.Bundle);
importClass(Packages.org.dspace.content.Item);
importClass(Packages.org.dspace.content.Collection);
importClass(Packages.org.dspace.content.Community);
importClass(Packages.org.dspace.harvest.HarvestedCollection);
importClass(Packages.org.dspace.eperson.EPerson);
importClass(Packages.org.dspace.eperson.Group);
importClass(Packages.org.dspace.app.util.Util);

importClass(Packages.java.util.Set);

importClass(Packages.org.dspace.app.xmlui.utils.FlowscriptUtils);
importClass(Packages.org.dspace.app.xmlui.utils.ContextUtil);


importClass(Packages.java.lang.System);
importClass(Packages.org.dspace.core.ConfigurationManager);

importClass(Packages.javax.mail.internet.AddressException);


importClass(Packages.org.dspace.core.ConfigurationManager);
importClass(Packages.org.dspace.core.Context);
importClass(Packages.org.dspace.content.Collection);
importClass(Packages.org.dspace.authorize.AuthorizeException);

importClass(Packages.org.dspace.app.xmlui.utils.AuthenticationUtil);


importClass(Packages.java.lang.String);
importClass(Packages.org.dspace.app.xmlui.aspect.shoppingcart.FlowShoppingcartUtils);
importClass(Packages.org.dspace.app.xmlui.aspect.shoppingcart.FlowVoucherUtils);
/**
 * This class defines the workflows for three flows within the EPerson aspect.
 *
 * FIXME: add more documentation
 *
 * @author Scott Phillips
 */

/** These functions should be common to all Manakin Flow scripts */
function getObjectModel()
{
    return FlowscriptUtils.getObjectModel(cocoon);
}

function getDSContext()
{
    return ContextUtil.obtainContext(getObjectModel());
}

function getShoppingcart()
{
    return getDSContext().getShoppingcart();
}

/**
 * Send the current page and wait for the flow to be continued. This method will
 * preform two usefull actions: set the flow parameter & add result information.
 *
 * The flow parameter is used by the sitemap to seperate requests comming from a
 * flow script from just normal urls.
 *
 * The result object could potentialy contain a notice message and a list of
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
 * Start managing epeople
 */
function startManageShoppingcart()
{
    assertAdministrator();

    doManageShoppingcart();

    // This should never return, but just in case it does then point
    // the user to the home page.
    cocoon.redirectTo(cocoon.request.getContextPath());
    getDSContext().complete();
    cocoon.exit();
}

/**

 /********************
 * shopping cart flows
 ********************/

/**
 * Manage shopping cart, allow users to create new, edit exiting,
 * or remove shopping cart. The user may also search or browse
 * for shopping cart.
 *
 * The is typically used as an entry point flow.
 */
function doManageShoppingcart()
{
    assertAdministrator();

    var query = "";
    var page = 0;
    var handle = -1;
    var highlightID=-1;
    var result;
    do {

        sendPageAndWait("admin/shoppingcart/main",{"page":page,"query":query,"highlightID":highlightID},result);
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
            // Add a new shopping cart
            assertAdministrator();

            result = doAddSimplerProperty();

            if (result != null && result.getParameter("shoppingcart_id"))
                highlightID = result.getParameter("shoppingcart_id");
        }
        else if (cocoon.request.get("submit_edit") && cocoon.request.get("shoppingcart_id"))
        {
            // Edit an exting shopping cart
            assertAdministrator();

            var shoppingcart_id = cocoon.request.get("shoppingcart_id");
            result = doEditShoppingcart(shoppingcart_id);
            highlightID = shoppingcart_id;

        }
        else if (cocoon.request.get("submit_delete") && cocoon.request.get("select_shoppingcart"))
        {
            // Delete a set of Simple Properties
            assertAdministrator();

            var simpleproperty_ids = cocoon.request.getParameterValues("select_shoppingcart");
            result = doDeleteSimpleProperties(simpleproperty_ids);
            highlightID = -1;
        }
        else if (cocoon.request.get("submit_return"))
        {
            // Not implemented in the UI, but should be incase someone links to us.
            return;
        }

    } while (true) // only way to exit is to hit the submit_back button.
}


/**
 * Edit an exiting shopping cart, all standard metadata elements are presented for editing. The same
 * validation is required as is in adding a new eperson: unique email, first name, and last name
 */
function doEditShoppingcart(shoppingcart_id)
{
    // We can't assert any privleges at this point, the user could be a collection
    // admin or a supper admin. Instead we protect each operation.
    var result;
    do {
        sendPageAndWait("admin/shoppingcart/edit",{"shoppingcart_id":shoppingcart_id},result);
        result == null;

        if (cocoon.request.get("submit_save"))
        {
            // Attempt to save the changes.
            assertAdministrator();
            result = FlowShoppingcartUtils.processEditShoppingcart(getDSContext(),cocoon.request,getObjectModel(),shoppingcart_id);
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            // Cancel out and return to where ever the user came from.
            return null;
        }

    } while (result == null || !result.getContinue())

    return result;
}



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
 * Return wheather the currently authenticated EPerson is authorized to
 * preform the given action over/on the the given object.
 */
function isAuthorized(objectType, objectID, action) {

    // Note: it's okay to instantiate a DSpace object here because
    // under all cases this method will exit and the objects send
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
            object = EPerson.find(getDSContext(),objectID);
            break;
    }

    // If we couldn't find the object then return false
    if (object == null)
        return false;

    return AuthorizeManager.authorizeActionBoolean(getDSContext(),object,action);
}

/**
 * Return weather the currently authenticated eperson can edit the identified item.
 */
function canEditItem(itemID)
{
    var item = Item.find(getDSContext(),itemID);

    return item.canEdit();
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
 * Return whether the currently authenticated eperson can edit this collection
 */
function canEditCollection(collectionID)
{
    var collection = Collection.find(getDSContext(),collectionID);

    if (collection == null) {
        return isAdministrator();
    }
    return collection.canEditBoolean();
}

/**
 * Assert that the currently authenticated eperson can edit this collection. If they
 * can not then this method will never return.
 */
function assertEditCollection(collectionID) {

    if ( ! canEditCollection(collectionID)) {
        sendPage("admin/not-authorized");
        cocoon.exit();
    }
}


/**
 * Return whether the currently authenticated eperson can administrate this collection
 */
function canAdminCollection(collectionID)
{
    var collection = Collection.find(getDSContext(),collectionID);

    if (collection == null) {
        return isAdministrator();
    }
    return AuthorizeManager.authorizeActionBoolean(getDSContext(), collection, Constants.ADMIN);
}

/**
 * Assert that the currently authenticated eperson can administrate this collection. If they
 * can not then this method will never return.
 */
function assertAdminCollection(collectionID) {

    if ( ! canAdminCollection(collectionID)) {
        sendPage("admin/not-authorized");
        cocoon.exit();
    }
}


/**
 * Return whether the currently authenticated eperson can edit this community.
 */
function canEditCommunity(communityID)
{
    if (communityID == -1) {
        return isAdministrator();
    }

    var community = Community.find(getDSContext(),communityID);

    if (community == null) {
        return isAdministrator();
    }
    return community.canEditBoolean();
}

/**
 * Assert that the currently authenticated eperson can edit this community. If they can
 * not then this method will never return.
 */
function assertEditCommunity(communityID) {

    if ( ! canEditCommunity(communityID)) {
        sendPage("admin/not-authorized");
        cocoon.exit();
    }
}

/**
 * Return whether the currently authenticated eperson can administrate this community
 */
function canAdminCommunity(communityID)
{
    var community = Community.find(getDSContext(),communityID);

    if (community == null) {
        return isAdministrator();
    }
    return AuthorizeManager.authorizeActionBoolean(getDSContext(), community, Constants.ADMIN);
}

/**
 * Assert that the currently authenticated eperson can administrate this community. If they
 * can not then this method will never return.
 */
function assertAdminCommunity(communityID) {

    if ( ! canAdminCommunity(communityID)) {
        sendPage("admin/not-authorized");
        cocoon.exit();
    }
}

/**
 * Assert that the currently authenticated eperson can edit the given group. If they can
 * not then this method will never return.
 */
function assertEditGroup(groupID)
{
    // Check authorizations
    if (groupID == -1)
    {
        // only system admin can create "top level" group
        assertAdministrator();
    }
    else
    {
        assertAuthorized(Constants.GROUP, groupID, Constants.WRITE);
    }
}

/**
 * Return whether the currently authenticated eperson is an
 * administrator.
 */
function isAdministrator() {
    return AuthorizeManager.isCuratorOrAdmin(getDSContext());
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



function doManageShoppingcartCommunity(dsoID){
    assertAdministrator();

    var query = "";
    var page = 0;
    var highlightID=-1;
    var typeID= 4;
    var result;
    do {

        sendPageAndWait("admin/shoppingcart/community/main",{"page":page,"query":query,"typeID":typeID,"dsoID":dsoID,"highlightID":highlightID},result);
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
            // Add a new shopping cart
            assertAdministrator();

            result = doAddSimplerPropertyDSO(typeID,dsoID);

            if (result != null && result.getParameter("shoppingcart_id"))
                highlightID = result.getParameter("shoppingcart_id");
        }
        else if (cocoon.request.get("submit_edit") && cocoon.request.get("shoppingcart_id"))
        {
            // Edit an exting shopping cart
            assertAdministrator();

            var shoppingcart_id = cocoon.request.get("shoppingcart_id");
            result = doEditShoppingcart(shoppingcart_id);
            highlightID = shoppingcart_id;

        }
        else if (cocoon.request.get("submit_delete") && cocoon.request.get("select_shoppingcart"))
        {
            // Delete a set of Simple Properties
            assertAdministrator();

            var shoppingcart_ids = cocoon.request.getParameterValues("select_shoppingcart");
            result = doDeleteSimpleProperties(shoppingcart_ids);
            highlightID = -1;
        }
        else if (cocoon.request.get("submit_return"))
        {
            // Not implemented in the UI, but should be incase someone links to us.
            return;
        }

    } while (true) // only way to exit is to hit the submit_back button.
}




function doManageShoppingcartCollection(dsoID){
    assertAdministrator();

    var query = "";
    var page = 0;
    var highlightID=-1;
    var typeID= 3;
    var result;
    do {

        sendPageAndWait("shoppingcart/collection/main",{"page":page,"query":query,"typeID":typeID,"dsoID":dsoID,"highlightID":highlightID},result);
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
            // Add a new shopping cart
            assertAdministrator();

            result = doAddSimplerPropertyDSO(typeID,dsoID);

            if (result != null && result.getParameter("shoppingcart_id"))
                highlightID = result.getParameter("shoppingcart_id");
        }
        else if (cocoon.request.get("submit_edit") && cocoon.request.get("shoppingcart_id"))
        {
            // Edit an exting shoppingcart
            assertAdministrator();

            var shoppingcart_id = cocoon.request.get("shoppingcart_id");
            result = doEditShoppingcart(shoppingcart_id);
            highlightID = shoppingcart_id;

        }
        else if (cocoon.request.get("submit_delete") && cocoon.request.get("select_shoppingcart"))
        {
            // Delete a set of Simple Properties
            assertAdministrator();

            var shoppingcart_ids = cocoon.request.getParameterValues("select_shoppingcart");
            result = doDeleteSimpleProperties(shoppingcart_ids);
            highlightID = -1;
        }
        else if (cocoon.request.get("submit_return"))
        {
            // Not implemented in the UI, but should be incase someone links to us.
            return;
        }

    } while (true) // only way to exit is to hit the submit_back button.
}
/**
 * Start managing Site
 */
function startManageShoppingcartSite()
{
    assertAdministrator();
    var siteID = cocoon.request.get("siteID");

    doManageShoppingcartSite(siteID);

    // This should never return, but just in case it does then point
    // the user to the home page.
    cocoon.redirectTo(cocoon.request.getContextPath());
    getDSContext().complete();
    cocoon.exit();
}

function doManageShoppingcartSite(dsoID){
    assertAdministrator();

    var query = "";
    var page = 0;
    var highlightID=-1;
    var typeID= 2;
    var result;
    do {

        sendPageAndWait("admin/shoppingcart/site/main",{"page":page,"query":query,"typeID":typeID,"dsoID":dsoID,"highlightID":highlightID},result);
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
        else if (cocoon.request.get("submit_edit") && cocoon.request.get("shoppingcart_id"))
        {
            // Edit an exting shopping cart
            assertAdministrator();

            var shoppingcart_id = cocoon.request.get("shoppingcart_id");
            result = doEditShoppingcart(shoppingcart_id);
            highlightID = shoppingcart_id;

        }
        else if (cocoon.request.get("submit_return"))
        {
            // Not implemented in the UI, but should be incase someone links to us.
            return;
        }

    } while (true) // only way to exit is to hit the submit_back button.
}



/**
 * Start managing Voucher
 */
function startManageVoucher()
{
    assertAdministrator();

    doManageVoucher();

    // This should never return, but just in case it does then point
    // the user to the home page.
    cocoon.redirectTo(cocoon.request.getContextPath());
    getDSContext().complete();
    cocoon.exit();
}

function doManageVoucher()
{
    assertAdministrator();

    var query = "";
    var page = 0;
    var handle = -1;
    var highlightID=-1;
    var result;
    do {

        sendPageAndWait("admin/voucher/main",{"page":page,"query":query,"highlightID":highlightID},result);
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
            // Add a new shopping cart
            assertAdministrator();

            result = doAddVoucher();

            if (result != null && result.getParameter("voucher_id"))
                highlightID = result.getParameter("voucher_id");
        }
        else if (cocoon.request.get("submit_edit") && cocoon.request.get("voucher_id"))
        {
            // Edit an exting shopping cart
            assertAdministrator();

            var voucher_id = cocoon.request.get("voucher_id");
            result = doEditVoucher(voucher_id);
            highlightID = voucher_id;

        }
        else if (cocoon.request.get("submit_delete") && cocoon.request.get("select_voucher"))
        {
            // Delete a set of voucher
            assertAdministrator();

            var voucher_ids = cocoon.request.getParameterValues("select_voucher");
            result = doDeleteVoucher(voucher_ids);
            highlightID = -1;
        }
        else if (cocoon.request.get("submit_return"))
        {
            // Not implemented in the UI, but should be incase someone links to us.
            return;
        }

    } while (true) // only way to exit is to hit the submit_back button.
}

function doAddVoucher()
{
    assertAdministrator();

    var result;
    do {
        sendPageAndWait("admin/voucher/add",{},result);
        result = null;

        if (cocoon.request.get("submit_save"))
        {
            // Save the new eperson, assuming they have meet all the requirements.
            assertAdministrator();

            result = FlowVoucherUtils.processAddVoucher(getDSContext(),cocoon.request,getObjectModel());
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            // The user can cancel at any time.
            return null;
        }

    } while (result == null || !result.getContinue())

    return result;
}

/**
 * Edit an exiting shopping cart, all standard metadata elements are presented for editing. The same
 * validation is required as is in adding a new eperson: unique email, first name, and last name
 */
function doEditVoucher(voucher_id)
{
    // We can't assert any privleges at this point, the user could be a collection
    // admin or a supper admin. Instead we protect each operation.
    var result;
    do {
        sendPageAndWait("admin/voucher/edit",{"voucher_id":voucher_id},result);
        result == null;

        if (cocoon.request.get("submit_save"))
        {
            // Attempt to save the changes.
            assertAdministrator();
            result = FlowVoucherUtils.processEditVoucher(getDSContext(),cocoon.request,getObjectModel(),voucher_id);
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            // Cancel out and return to where ever the user came from.
            return null;
        }

    } while (result == null || !result.getContinue())

    return result;
}

