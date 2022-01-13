/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */


importClass(Packages.org.dspace.authorize.service.AuthorizeService);
importClass(Packages.org.dspace.authorize.factory.AuthorizeServiceFactory);
importClass(Packages.org.dspace.content.factory.ContentServiceFactory)
importClass(Packages.org.dspace.content.service.DSpaceObjectService)
importClass(Packages.org.dspace.content.service.CommunityService)
importClass(Packages.org.dspace.content.CommunityServiceImpl)
importClass(Packages.java.util.UUID)
importClass(Packages.java.lang.Integer)
importClass(Packages.org.apache.commons.lang.StringUtils)

importClass(Packages.org.dspace.core.Constants);
importClass(Packages.org.dspace.content.Bitstream);
importClass(Packages.org.dspace.content.Bundle);
importClass(Packages.org.dspace.content.Item);
importClass(Packages.org.dspace.content.Collection);
importClass(Packages.org.dspace.content.Community);
importClass(Packages.org.dspace.harvest.service.HarvestedCollectionService);
importClass(Packages.org.dspace.harvest.factory.HarvestServiceFactory);
importClass(Packages.org.dspace.eperson.EPerson);
importClass(Packages.org.dspace.eperson.Group);
importClass(Packages.org.dspace.app.util.Util);

importClass(Packages.org.dspace.workflow.factory.WorkflowServiceFactory);
importClass(Packages.org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory);
importClass(Packages.org.dspace.xmlworkflow.service.XmlWorkflowService);

importClass(Packages.java.util.Set);

importClass(Packages.org.dspace.app.xmlui.utils.FlowscriptUtils);
importClass(Packages.org.dspace.app.xmlui.utils.ContextUtil);
importClass(Packages.org.dspace.app.xmlui.aspect.administrative.FlowEPersonUtils);
importClass(Packages.org.dspace.app.xmlui.aspect.administrative.FlowGroupUtils);
importClass(Packages.org.dspace.app.xmlui.aspect.administrative.FlowRegistryUtils);
importClass(Packages.org.dspace.app.xmlui.aspect.administrative.FlowItemUtils);
importClass(Packages.org.dspace.app.xmlui.aspect.administrative.FlowMapperUtils);
importClass(Packages.org.dspace.app.xmlui.aspect.administrative.FlowAuthorizationUtils);
importClass(Packages.org.dspace.app.xmlui.aspect.administrative.FlowContainerUtils);
importClass(Packages.org.dspace.app.xmlui.aspect.administrative.FlowCurationUtils);
importClass(Packages.org.dspace.app.xmlui.aspect.administrative.FlowMetadataImportUtils);
importClass(Packages.org.dspace.app.xmlui.aspect.administrative.FlowBatchImportUtils);
importClass(Packages.java.lang.System);

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

function getCommunityService()
{
    return ContentServiceFactory.getInstance().getCommunityService();
}

function getCollectionService()
{
    return ContentServiceFactory.getInstance().getCollectionService();
}

function getHarvestedCollectionService()
{
    return HarvestServiceFactory.getInstance().getHarvestedCollectionService();
}

function getItemService()
{
    return ContentServiceFactory.getInstance().getItemService();
}

function getAuthorizeService()
{
    return AuthorizeServiceFactory.getInstance().getAuthorizeService();
}

function getXmlWorkflowFactory()
{
    return XmlWorkflowServiceFactory.getInstance().getWorkflowFactory();
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
 * Return whether the currently authenticated EPerson is authorized to
 * perform the given action over/on the the given object.
 */
function isAuthorized(objectType, objectID, action) {

    // Note: it's okay to instantiate a DSpace object here because
    // under all cases this method will exit and the objects sent
    // for garbage collecting before and continuations and are used.

    var object = ContentServiceFactory.getInstance().getDSpaceObjectService(objectType).find(getDSContext(), objectID);

    // If we couldn't find the object then return false
    if (object == null)
        return false;

    return getAuthorizeService().authorizeActionBoolean(getDSContext(),object,action);
}

/**
 * Assert that the currently authenticated eperson is able to perform
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
 * Return whether the currently authenticated eperson can edit the identified item.
 */
function canEditItem(itemID)
{
	var item = getItemService().find(getDSContext(),itemID);
	
	return getItemService().canEdit(getDSContext(), item);
}

/**
 * Assert that the currently authenticated eperson can edit this item, if they
 * cannot then this method will never return.
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
	var collection = getCollectionService().find(getDSContext(),collectionID);
	
	if (collection == null) {
		return isAdministrator();
	}
	return getCollectionService().canEditBoolean(getDSContext(), collection);
}

/**
 * Assert that the currently authenticated eperson can edit this collection. If they
 * cannot then this method will never return.
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
	var collection = getCollectionService().find(getDSContext(),collectionID);
	
	if (collection == null) {
		return isAdministrator();
	}
	return getAuthorizeService().authorizeActionBoolean(getDSContext(), collection, Constants.ADMIN);
}

/**
 * Assert that the currently authenticated eperson can administrate this collection. If they
 * cannot then this method will never return.
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
	if (communityID == null) {
		return isAdministrator();
	}
	
	var community = getCommunityService().find(getDSContext(),communityID);
	
	if (community == null) {
		return isAdministrator();
	}
	return getCommunityService().canEditBoolean(getDSContext(),community);
}

/**
 * Assert that the currently authenticated eperson can edit this community. If they
 * cannot then this method will never return.
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
	var community = getCommunityService().find(getDSContext(),communityID);

	if (community == null) {
		return isAdministrator();
	}
	return getAuthorizeService().authorizeActionBoolean(getDSContext(), community, Constants.ADMIN);
}

/**
 * Assert that the currently authenticated eperson can administrate this community. If they
 * cannot then this method will never return.
 */
function assertAdminCommunity(communityID) {

	if ( ! canAdminCommunity(communityID)) {
		sendPage("admin/not-authorized");
		cocoon.exit();
	}
}

/**
 * Assert that the currently authenticated eperson can edit the given group. If they
 * cannot then this method will never return.
 */
function assertEditGroup(groupID)
{
	// Check authorizations
	if (groupID == null)
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


/**
 * Start managing epeople
 */
function startManageEPeople()
{
	assertAdministrator();

	doManageEPeople();

	// This should never return, but just in case it does then point
	// the user to the home page.
	cocoon.redirectTo(cocoon.request.getContextPath());
	getDSContext().complete();
	cocoon.exit();
}

/**
 * Start managing groups
 */
function startManageGroups()
{
	assertAdministrator();

	doManageGroups();

	// This should never return, but just in case it does then point
	// the user to the home page.
	cocoon.redirectTo(cocoon.request.getContextPath());
	getDSContext().complete();
	cocoon.exit();
}

/**
 * Start managing the metadata registry
 */
function startManageMetadataRegistry()
{
	assertAdministrator();

	doManageMetadataRegistry();

	// This should never return, but just in case it does then point
	// the user to the home page.
	cocoon.redirectTo(cocoon.request.getContextPath());
	getDSContext().complete();
	cocoon.exit();
}

/**
 * Start managing the format registry
 */
function startManageFormatRegistry()
{
	assertAdministrator();

	doManageFormatRegistry();

	// This should never return, but just in case it does then point
	// the user to the home page.
	cocoon.redirectTo(cocoon.request.getContextPath());
	getDSContext().complete();
	cocoon.exit();
}

/**
 * Start managing items
 */
function startManageItems()
{
	assertAdministrator();

	doManageItems();

	// This should never return, but just in case it does then point
	// the user to the home page.
	cocoon.redirectTo(cocoon.request.getContextPath());
	getDSContext().complete();
	cocoon.exit();
}

/**
 * Start managing authorizations
 */
function startManageAuthorizations()
{
	assertAdministrator();

	doManageAuthorizations();

	// This should never return, but just in case it does then point
	// the user to the home page.
	cocoon.redirectTo(cocoon.request.getContextPath());
	getDSContext().complete();
	cocoon.exit();
}


/**
 * Start editing an individual item.
 */
function startEditItem()
{
	var itemID = UUID.fromString(cocoon.request.get("itemID"));

	assertEditItem(itemID);

	doEditItem(itemID);

	var item = getItemService().find(getDSContext(),itemID);
	cocoon.redirectTo(cocoon.request.getContextPath()+"/handle/"+item.getHandle(),true);
	getDSContext().complete();
	item = null;
	cocoon.exit();
}

/**
 * Start managing items that are mapped to a collection.
 */
function startMapItems()
{
    var collectionID = UUID.fromString(cocoon.request.get("collectionID"));

	// they can edit the collection they are maping items into.
	assertEditCollection(collectionID);

	doMapItems(collectionID);

	var collection = getCollectionService().find(getDSContext(),collectionID);
	cocoon.redirectTo(cocoon.request.getContextPath()+"/handle/"+collection.getHandle(),true);
	getDSContext().complete();
	collection = null;
	cocoon.exit();
}

function startMetadataImport()
{

        assertAdministrator();

	doMetadataImport();

	cocoon.redirectTo(cocoon.request.getContextPath());
        getDSContext().complete();
	cocoon.exit();
}

function startBatchImport()
{

    assertAdministrator();

    doBatchImport();

    cocoon.redirectTo(cocoon.request.getContextPath());
    getDSContext().complete();
    cocoon.exit();
}

/**
 * Start creating a new collection.
 */
function startCreateCollection()
{
	var communityID = UUID.fromString(cocoon.request.get("communityID"));

	assertAuthorized(Constants.COMMUNITY,communityID,Constants.ADD);

	doCreateCollection(communityID);

	// Root level community, cancel out to the global community list.
	cocoon.redirectTo(cocoon.request.getContextPath()+"/community-list",true);
	getDSContext().complete();
	cocoon.exit();
}


/**
 * Start editing an individual collection
 */
function startEditCollection()
{
	var collectionID = UUID.fromString(cocoon.request.get("collectionID"));

	assertEditCollection(collectionID);

	doEditCollection(collectionID);

	// Go back to the collection
	var collection = getCollectionService().find(getDSContext(),collectionID);
	cocoon.redirectTo(cocoon.request.getContextPath()+"/handle/"+collection.getHandle(),true);
	getDSContext().complete();
	collection = null;
	cocoon.exit();
}

/**
 * Start creating a new community
 */
function startCreateCommunity()
{
	var communityID = cocoon.request.get("communityID");

    if(communityID != null) {
        communityID = UUID.fromString(communityID);
    }

	doCreateCommunity(communityID);

	// Root level community, cancel out to the global community list.
	cocoon.redirectTo(cocoon.request.getContextPath()+"/community-list",true);
	getDSContext().complete();
	cocoon.exit();
}

/**
 * Start editing an individual community
 */
function startEditCommunity()
{
    var communityID = UUID.fromString(cocoon.request.get("communityID"));

	assertEditCommunity(communityID);

	doEditCommunity(communityID);

	// Go back to the community
	var community = getCommunityService().find(getDSContext(),communityID);
	cocoon.redirectTo(cocoon.request.getContextPath()+"/handle/"+community.getHandle(),true);
	getDSContext().complete();
	community = null;
	cocoon.exit();
}

/**
 * Start (site-wide) curation of any object
 */
function startCurate()
{
        assertAdministrator();

        doCurate();

        cocoon.redirectTo(cocoon.request.getContextPath());
        getDSContext().complete();
        cocoon.exit();
}




/********************
 * EPerson flows
 ********************/

/**
 * Manage epeople, allow users to create new, edit existing,
 * or remove epeople. The user may also search or browse
 * for epeople.
 *
 * The is typically used as an entry point flow.
 */
function doManageEPeople()
{
    assertAdministrator();

    var query = null;
    var page = 0;
    var highlightID = -1;
    var result;
    do {

        sendPageAndWait("admin/epeople/main",{"page":page,"query":query,"highlightID":highlightID},result);
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
            // Add a new eperson
            assertAdministrator();

            result = doAddEPerson();

            if (result != null && result.getParameter("epersonID"))
              	highlightID = UUID.fromString(result.getParameter("epersonID"));
        }
        else if (cocoon.request.get("submit_edit") && cocoon.request.get("epersonID"))
        {
            // Edit an existing eperson
            assertAdministrator();

            var epersonID = UUID.fromString(cocoon.request.get("epersonID"));
            result = doEditEPerson(epersonID);
            highlightID = epersonID;

        }
        else if (cocoon.request.get("submit_delete") && cocoon.request.get("select_eperson"))
        {
            // Delete a set of epeople
            assertAdministrator();

            var epeopleIDs = cocoon.request.getParameterValues("select_eperson");
            result = doDeleteEPeople(epeopleIDs);
            highlightID = -1;
        }
        else if (cocoon.request.get("submit_return"))
        {
            // Not implemented in the UI, but should be in case someone links to us.
            return;
        }

    } while (true) // only way to exit is to hit the submit_back button.
}

/**
 * Add a new eperson, the user is presented with a form to create a new eperson. They will
 * repeat this form until the user has supplied a unique email address, first name, and
 * last name.
 */
function doAddEPerson()
{
    assertAdministrator();

    var result;
    do {
        sendPageAndWait("admin/epeople/add",{},result);
        result = null;

        if (cocoon.request.get("submit_save"))
        {
            // Save the new eperson, assuming they have met all the requirements.
            assertAdministrator();

            result = FlowEPersonUtils.processAddEPerson(getDSContext(),cocoon.request,getObjectModel());
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
 * Edit an existing eperson, all standard metadata elements are presented for editing. The same
 * validation is required as is in adding a new eperson: unique email, first name, and last name
 */
function doEditEPerson(epersonID)
{
    // We can't assert any privileges at this point, the user could be a collection
    // admin or a supper admin. Instead we protect each operation.
    var result;
    do {
        sendPageAndWait("admin/epeople/edit",{"epersonID":epersonID},result);
        result == null;

        if (cocoon.request.get("submit_save"))
        {
            // Attempt to save the changes.
            assertAdministrator();
            result = FlowEPersonUtils.processEditEPerson(getDSContext(),cocoon.request,getObjectModel(),epersonID);
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            // Cancel out and return to wherever the user came from.
            return null;
        }
        else if (cocoon.request.get("submit_edit_group") && cocoon.request.get("groupID"))
        {
            // Jump to a group that this user is a member.
            assertAdministrator();
            var groupID = UUID.fromString(cocoon.request.get("groupID"));
            result = doEditGroup(groupID);

            // Don't continue after returning from editing a group.
            if (result != null)
                result.setContinue(false);
        }
        else if (cocoon.request.get("submit_delete"))
        {
            // Delete this user
            assertAdministrator();
            var epeopleIDs = new Array();
            epeopleIDs[0] = epersonID;
            result = doDeleteEPeople(epeopleIDs);

            // No matter what just bail out to the group list.
            return null;
        }
        else if (cocoon.request.get("submit_reset_password"))
        {
            // Reset the user's password by sending them the forgot password email.
            assertAdministrator();
            result = FlowEPersonUtils.processResetPassword(getDSContext(),epersonID);

            if (result != null)
                result.setContinue(false);
        }
        else if (cocoon.request.get("submit_login_as"))
        {
        	// Login as this user.
        	assertAdministrator();
        	result = FlowEPersonUtils.processLoginAs(getDSContext(),getObjectModel(),epersonID);

        	if (result != null && result.getOutcome().equals("success"))
        	{
        		// the user is loged in as another user, we can't let them continue on
        		// using this flow because they might not have permissions. So forward
        		// them to the homepage.
			var siteRoot = cocoon.request.getContextPath();
			if (siteRoot == "")
			{
				siteRoot = "/";
			}
        		cocoon.redirectTo(siteRoot,true);
				getDSContext().complete();
				cocoon.exit();
        	}
        }

    } while (result == null || !result.getContinue())

    return result;
}

/**
 * Confirm and delete the list of given epeople. The user will be presented with a list of selected epeople,
 * and if they click the confirm delete button then the epeople will be deleted.
 */
function doDeleteEPeople(epeopleIDs)
{
    assertAdministrator();

    sendPageAndWait("admin/epeople/delete",{"epeopleIDs":epeopleIDs.join(',')});

    if (cocoon.request.get("submit_confirm"))
    {
        // The user has confirmed, actually perform the deletion.
        assertAdministrator();

        var result = FlowEPersonUtils.processDeleteEPeople(getDSContext(),epeopleIDs);
        return result;
    }
    return null;
}

/********************
 * Group flows
 ********************/

/**
 * Manage groups, allow users to create new, edit existing,
 * or remove groups. The user may also search or browse
 * for groups.
 *
 * The is typically used as an entry point flow.
 */
function doManageGroups()
{
    assertAdministrator();

    var highlightID = -1
    var query = "";
    var page = 0;
    var result;
    do {


        sendPageAndWait("admin/group/main",{"query":query,"page":page,"highlightID":highlightID},result);
        assertAdministrator();
		result = null;


         // Update the page parameter if supplied.
        if (cocoon.request.get("page"))
            page = cocoon.request.get("page");

        if (cocoon.request.get("submit_search"))
        {
            // Grab the new query and reset the page parameter
            query = cocoon.request.get("query");
            page = 0
            highlightID = -1
        }
        else if (cocoon.request.get("submit_add"))
        {
            // Just create a blank group then pass it to the group editor.
            result = doEditGroup(null);

            if (result != null && result.getParameter("groupID"))
           		highlightID = UUID.fromString(result.getParameter("groupID"));
        }
        else if (cocoon.request.get("submit_edit") && cocoon.request.get("groupID"))
        {
            // Edit a specific group
			var groupID = UUID.fromString(cocoon.request.get("groupID"));
			result = doEditGroup(groupID);
			highlightID = groupID;
        }
        else if (cocoon.request.get("submit_delete") && cocoon.request.get("select_group"))
        {
            // Delete a set of groups
            var groupIDs = cocoon.request.getParameterValues("select_group");
            result = doDeleteGroups(groupIDs);
            highlightID = -1;
        }
        else if (cocoon.request.get("submit_return"))
        {
            // Not implemented in the UI, but should be in case someone links to us.
            return;
        }

    } while (true) // only way to exit is to hit the submit_back button.
}


/**
 * This flow allows for the full editing of a group, changing the group's name or
 * removing members. Users may also search for epeople / groups to add as members
 * to this group.
 */
function doEditGroup(groupID)
{
    var groupName        = FlowGroupUtils.getName(getDSContext(),groupID);
    var memberEPeopleIDs = FlowGroupUtils.getEPeopleMembers(getDSContext(),groupID);
    var memberGroupIDs   = FlowGroupUtils.getGroupMembers(getDSContext(),groupID);

    assertEditGroup(groupID);

    var highlightEPersonID;
    var highlightGroupID;
    var type = "";
    var query = "";
    var page = 0;
    var result = null;
    do {
        sendPageAndWait("admin/group/edit",{"groupID":groupID,"groupName":groupName,"memberGroupIDs":memberGroupIDs.join(','),"memberEPeopleIDs":memberEPeopleIDs.join(','),"highlightEPersonID":highlightEPersonID,"highlightGroupID":highlightGroupID,"query":query,"page":page,"type":type},result);
        assertEditGroup(groupID);

		result = null;
        highlightEPersonID = null;
        highlightGroupID = null;

        // Update the groupName
		if (cocoon.request.get("group_name"))
			groupName = cocoon.request.get("group_name");

		if (cocoon.request.get("page"))
			page = cocoon.request.get("page");

        if (cocoon.request.get("submit_cancel"))
        {
            // Just return without saving anything.
            return null;
        }
       	else if (cocoon.request.get("submit_save"))
       	{
       		result = FlowGroupUtils.processSaveGroup(getDSContext(),groupID,groupName,memberEPeopleIDs,memberGroupIDs);

       		// In case a group was created, update our id.
       		if (result != null && result.getParameter("groupID"))
           		groupID = UUID.fromString(result.getParameter("groupID"));
       	}
        else if (cocoon.request.get("submit_search_epeople") && cocoon.request.get("query"))
        {
            // Perform a new search for epeople.
            query = cocoon.request.get("query");
            page = 0;
            type = "eperson";
        }
        else if (cocoon.request.get("submit_search_groups") && cocoon.request.get("query"))
        {
            // Perform a new search for groups.
            query = cocoon.request.get("query");
            page = 0;
            type = "group";
        }
        else if (cocoon.request.get("submit_clear"))
        {
            // Perform a new search for epeople.
            query = "";
            page = 0;
            type = "";
        }
        else if (cocoon.request.get("submit_edit_eperson") && cocoon.request.get("epersonID"))
        {
            // Jump to a specific EPerson
            var epersonID = UUID.fromString(cocoon.request.get("epersonID"));
            result = doEditEPerson(epersonID);

            if (result != null)
                result.setContinue(false);
        }
        else if (cocoon.request.get("submit_edit_group") && cocoon.request.get("groupID"))
        {
            // Jump to another group.
            var newGroupID = UUID.fromString(cocoon.request.get("groupID"));
            result = doEditGroup(newGroupID); // ahhh recursion!

            if (result != null)
                result.setContinue(false);
        }

        // Check if there were any add or delete operations.
        var names = cocoon.request.getParameterNames();
        while (names.hasMoreElements())
        {
        	var name = names.nextElement();
        	var match = null;

        	if ((match = name.match(/submit_add_eperson_([^]+)/)) != null)
        	{
        		// Add an eperson
        		var epersonID = UUID.fromString(match[1]);
        		memberEPeopleIDs = FlowGroupUtils.addMember(memberEPeopleIDs,epersonID);
        		highlightEPersonID = epersonID;
        	}

        	if ((match = name.match(/submit_add_group_([^]+)/)) != null)
        	{
        		// Add a group
        		var _groupID = match[1];
        		memberGroupIDs = FlowGroupUtils.addMember(memberGroupIDs,_groupID);
        		highlightGroupID = _groupID;
        	}
        	if ((match = name.match(/submit_remove_eperson_([^]+)/)) != null)
        	{
        		// remove an eperson
        		var epersonID = match[1];
				memberEPeopleIDs = FlowGroupUtils.removeMember(memberEPeopleIDs,epersonID);
				highlightEPersonID = epersonID;
        	}
        	if ((match = name.match(/submit_remove_group_([^]+)/)) != null)
        	{
        		// remove a group
        		var _groupID = match[1];
        		memberGroupIDs = FlowGroupUtils.removeMember(memberGroupIDs,_groupID);
        		highlightGroupID = _groupID;
        	}
        }


    } while (result == null || !result.getContinue())

    return result;
}

/**
 * Confirm that the given groupIDs should be deleted, if confirmed they will be deleted.
 */
function doDeleteGroups(groupIDs)
{
    assertAdministrator();

    sendPageAndWait("admin/group/delete",{"groupIDs":groupIDs.join(',')});

    if (cocoon.request.get("submit_confirm"))
    {
        // The user has confirmed, actually delete these groups
        assertAdministrator();
        var result = FlowGroupUtils.processDeleteGroups(getDSContext(),groupIDs);
        return result;
    }
    return null;
}


/**************************
 * Registries: Metadata flows
 **************************/

/**
 * Manage metadata registries, consisting of all metadata fields grouped into
 * one or more schemas. This flow will list all available schemas for edit, allow
 * the user to add or delete schemas.
 *
 * This is an entry point flow.
 */
function doManageMetadataRegistry()
{
    assertAdministrator();

    var result = null;
    do {
        sendPageAndWait("admin/metadata-registry/main",{},result);
		assertAdministrator();
        result = null;

        if (cocoon.request.get("submit_edit") && cocoon.request.get("schemaID"))
        {
            // Edit a specific schema
            var schemaID = cocoon.request.get("schemaID");
            result = doEditMetadataSchema(schemaID)
        }
        else if (cocoon.request.get("submit_add"))
        {
            // Add a new schema
            var namespace = cocoon.request.get("namespace");
            var name = cocoon.request.get("name");
            result = FlowRegistryUtils.processAddMetadataSchema(getDSContext(), namespace, name);
        }
        else if (cocoon.request.get("submit_delete") && cocoon.request.get("select_schema"))
        {
            // Remove the selected schemas
            var schemaIDs = cocoon.request.getParameterValues("select_schema");
            result = doDeleteMetadataSchemas(schemaIDs)
        }
    } while(true)
}

/**
 * Edit a particular schema, this will list all fields in the schema. When clicking
 * on a field it will be loaded into the top of the page where it can be edited. When
 * the top form is not loaded it can be used to add new fields.
 *
 * The last field operated on is kept as a highlighted field to make it easier to
 * find things in the interface.
 */
function doEditMetadataSchema(schemaID)
{
    assertAdministrator();

    var highlightID = -1 // Field that is highlighted
    var updateID = -1; // Field being updated
    var result = null;
    do {
        sendPageAndWait("admin/metadata-registry/edit-schema",{"schemaID":schemaID,"updateID":updateID,"highlightID":highlightID},result);
		assertAdministrator();
        result = null;

        if (cocoon.request.get("submit_return"))
        {
            // Go back to wherever they came from.
            return null;
        }
        else if (cocoon.request.get("submit_edit") && cocoon.request.get("fieldID"))
        {
            // select an existing field for editing. This will load it into the
            // form for editing.
            updateID = cocoon.request.get("fieldID");
            highlightID = updateID;
        }
        else if (cocoon.request.get("submit_add"))
        {
            // Add a new field
            var element = cocoon.request.get("newElement");
            var qualifier = cocoon.request.get("newQualifier");
            var note = cocoon.request.get("newNote");
            // processes adding field
            result = FlowRegistryUtils.processAddMetadataField(getDSContext(),schemaID,element,qualifier,note);
            highlightID = result.getParameter("fieldID");
        }
        else if (cocoon.request.get("submit_update") && updateID >= 0)
        {
            // Update an existing field
            var element = cocoon.request.get("updateElement");
            var qualifier = cocoon.request.get("updateQualifier");
            var note = cocoon.request.get("updateNote");

            result = FlowRegistryUtils.processEditMetadataField(getDSContext(),schemaID,updateID,element,qualifier,note);

            if (result != null && result.getContinue())
            {
                // If the update was successful then clean the updateID;
                highlightID = updateID;
                updateID = -1;
            }
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            // User cancels out of a field update
            updateID = -1;
            highlightID = -1;
        }
        else if (cocoon.request.get("submit_delete") && cocoon.request.get("select_field"))
        {
            // Delete the selected fields
            var fieldIDs = cocoon.request.getParameterValues("select_field");
            result = doDeleteMetadataFields(fieldIDs);
            updateID = -1;
            highlightID = -1
        }
        else if (cocoon.request.get("submit_move") && cocoon.request.get("select_field"))
        {
            // Attempt to move the selected fields to another schema.
            var fieldIDs = cocoon.request.getParameterValues("select_field");
            result = doMoveMetadataFields(fieldIDs);
            updateID = -1;
            highlightID = -1;
        }

    } while (true)
}

/**
 * Confirm the deletion of the listed metadata fields.
 */
function doDeleteMetadataFields(fieldIDs)
{
    assertAdministrator();

    sendPageAndWait("admin/metadata-registry/delete-fields",{"fieldIDs":fieldIDs.join(',')})
    assertAdministrator();

    if (cocoon.request.get("submit_confirm"))
    {
        // Actually delete the fields
        var result = FlowRegistryUtils.processDeleteMetadataField(getDSContext(),fieldIDs);
        return result;
    }
    return null;
}

/**
 * Move the listed fields to the target schema. A list of all fields will be
 * given a select box to identify the target schema.
 */
function doMoveMetadataFields(fieldIDs)
{
    assertAdministrator();

    sendPageAndWait("admin/metadata-registry/move-fields",{"fieldIDs":fieldIDs.join(',')});
	assertAdministrator();

    if (cocoon.request.get("submit_move") && cocoon.request.get("to_schema"))
    {
        // Actually move the fields
        var schemaID = cocoon.request.get("to_schema");
        var result = FlowRegistryUtils.processMoveMetadataField(getDSContext(), schemaID,fieldIDs);
        return result;
    }
    return null;
}


/**
 * Confirm the deletion of the listed schema
 */
function doDeleteMetadataSchemas(schemaIDs)
{
    assertAdministrator();

    sendPageAndWait("admin/metadata-registry/delete-schemas",{"schemaIDs":schemaIDs.join(',')});
    assertAdministrator();

    if (cocoon.request.get("submit_confirm"))
    {
        // Actually delete the schemas
        var result = FlowRegistryUtils.processDeleteMetadataSchemas(getDSContext(),schemaIDs)
        return result;
    }
    return null;
}

/**************************
 * Registries: Format flows
 **************************/

/**
 * Manage the set of bitstream formats that are available in the system. A
 * list of all available formats is given each one can be editeded or
 * deleteed and new ones may be added.
 */
function doManageFormatRegistry()
{
    assertAdministrator();

    var highlightID = -1 // Field that is highlighted
    var updateID = -1; // Field being updated
    var result = null;
    do {
        sendPageAndWait("admin/format-registry/main",{"updateID":updateID,"highlightID":highlightID},result);
        assertAdministrator();
		result = null;

        if (cocoon.request.get("submit_return"))
        {
            // Go back to wherever they came from.
            return null;
        }
        else if (cocoon.request.get("submit_edit") && cocoon.request.get("formatID"))
        {
            var formatID = cocoon.request.get("formatID");
            result = doEditBitstreamFormat(formatID);

            // Highlight the format that was just edited.
            if (result != null && result.getParameter("formatID"))
                highlightID = result.getParameter("formatID");
            else
                highlightID = formatID;
        }
        else if (cocoon.request.get("submit_add"))
        {
            // Add a new format
            result = doEditBitstreamFormat(-1);

             // Highlight the format that was just created.
            if (result != null && result.getParameter("formatID"))
                highlightID = result.getParameter("formatID");
        }
        else if (cocoon.request.get("submit_delete") && cocoon.request.get("select_format"))
        {
            // Delete the selected formats
            var formatIDs = cocoon.request.getParameterValues("select_format");
            result = doDeleteBitstreamFormats(formatIDs);
            updateID = -1;
            highlightID = -1
        }

    } while (true)
}

/**
 * Edit a single bitstream format specified by formatID. If the formatID is
 * -1 then a new bitstream will be added instead.
 */
function doEditBitstreamFormat(formatID)
{
    assertAdministrator();

    var result = null;
    do {
        sendPageAndWait("admin/format-registry/edit",{"formatID":formatID},result);
		assertAdministrator();
        result = null;


        if (cocoon.request.get("submit_cancel"))
        {
            // Go back to wherever they came from.
            return null;
        }
        else if (cocoon.request.get("submit_save"))
        {
            // Create or update an existing bitstream format
            result = FlowRegistryUtils.processEditBitstreamFormat(getDSContext(), formatID, cocoon.request);
        }

    } while (result == null || !result.getContinue())

    return result;
}

/**
 * Confirm the deletion of the listed bitstream formats
 */
function doDeleteBitstreamFormats(formatIDs)
{
    assertAdministrator();

    sendPageAndWait("admin/format-registry/delete",{"formatIDs":formatIDs.join(',')});
	assertAdministrator();

    if (cocoon.request.get("submit_confirm"))
    {
        // Actually delete the formats
        var result = FlowRegistryUtils.processDeleteBitstreamFormats(getDSContext(),formatIDs)
        return result;
    }
    return null;
}

/**************************
 * Edit Item flows
 **************************/

/**
 * Manage items, this is a flow entry point allowing the user to search for items by
 * their internal id or handle.
 */
function doManageItems()
{
	assertAdministrator();

	var identifier;
	var result;
	do {
		sendPageAndWait("admin/item/find",{"identifier":identifier},result);
   		assertAdministrator();
		result = null;

		if (cocoon.request.get("submit_find") && cocoon.request.get("identifier"))
		{
			// Search for the identifier
			identifier = cocoon.request.get("identifier");
			result = FlowItemUtils.resolveItemIdentifier(getDSContext(),identifier);

			// If an item was found then allow the user to edit the item.
			if (result != null && result.getParameter("itemID"))
			{
				var itemID = UUID.fromString(result.getParameter("itemID"));
				result = doEditItem(itemID);
			}
		}
	} while (true)
}


/**
 * Edit a single item. This method allows the user to switch between the
 * three sections of editing an item: status, bitstreams, and metadata.
 */
function doEditItem(itemID)
{
	// Always go to the status page first
	doEditItemStatus(itemID);

	do {
	    if (cocoon.request.get("submit_return"))
		{
			// go back to wherever we came from.
			return null;
		}
		else if (cocoon.request.get("submit_status"))
		{
			doEditItemStatus(itemID);
		}
		else if (cocoon.request.get("submit_bitstreams"))
		{
			doEditItemBitstreams(itemID);
		}
		else if (cocoon.request.get("submit_metadata"))
		{
			doEditItemMetadata(itemID, null);
		}
		else if (cocoon.request.get("view_item"))
		{
			doViewItem(itemID);
		}
	        else if (cocoon.request.get("submit_curate"))
                {
                        doCurateItem(itemID, cocoon.request.get("curate_task"));
                }
                else
		{
			// This case should never happen but to prevent an infinite loop
			// from occurring let's just return null.
			return null;
		}
	} while (true)
}

/**
*  Just show the item
*/
function doViewItem(itemID){
	do {
		sendPageAndWait("admin/item/view_item",{"itemID":itemID},null);
		assertEditItem(itemID);
		if ( !cocoon.request.get("view_item"))
		{
			// go back to wherever we came from.
			return null;
		}
	} while (true)
}

/**
 * Change the status of an item, withdraw or reinstate, or completely delete it.
 */
function doEditItemStatus(itemID)
{
	assertEditItem(itemID);

	var result;
	do {
        sendPageAndWait("admin/item/status",{"itemID":itemID},result);
		assertEditItem(itemID);
		result = null;

		if (cocoon.request.get("submit_return")  || cocoon.request.get("submit_bitstreams") || cocoon.request.get("submit_metadata") || cocoon.request.get("view_item") || cocoon.request.get("submit_curate") )
		{
			// go back to wherever we came from.
			return null;
		}
		else if (cocoon.request.get("submit_delete"))
		{
			assertAuthorized(Constants.ITEM, itemID, Constants.DELETE);

			// Confirm the item's deletion
			result = doDeleteItem(itemID);

			// If the user actually deleted the item the return back
			// to the manage items page.
			if (result != null)
				return result;
		}
		else if (cocoon.request.get("submit_withdraw"))
		{
			// Confirm the withdrawal of the item
			result = doWithdrawItem(itemID);
		}
		else if (cocoon.request.get("submit_reinstate"))
		{
			// Confirm the reinstatiation of the item
			result = doReinstateItem(itemID);
		}
        else if (cocoon.request.get("submit_private"))
        {

            result = doPrivateItem(itemID);
        }
        else if (cocoon.request.get("submit_public"))
        {

            result = doPublicItem(itemID);
        }
		else if (cocoon.request.get("submit_move"))
		{
			// Move this item somewhere else
			result = doMoveItem(itemID);
		}
		else if (cocoon.request.get("submit_authorization"))
		{
			// Edit the authorizations for this
			// authorization check performed by the doAuthorize methods in FlowAuthorizationUtils
			// assertAdministrator();

			doAuthorizeItem(itemID);
		}

	} while (true)
}

/**
 * Allow the user to manage the item's bitstreams, add, delete, or change a bitstream's metadata.
 */
function doEditItemBitstreams(itemID)
{
	assertEditItem(itemID);

	var result;
	do {
		sendPageAndWait("admin/item/bitstreams",{"itemID":itemID}, result);
		assertEditItem(itemID);
		result = null;

        var submitButton = Util.getSubmitButton(cocoon.request, "submit_return");
		if (cocoon.request.get("submit_return") || cocoon.request.get("submit_status") || cocoon.request.get("submit_bitstreams") || cocoon.request.get("submit_metadata") || cocoon.request.get("view_item") || cocoon.request.get("submit_curate"))
		{
			// go back to wherever we came from.
			return null;
		}
		else if (cocoon.request.get("submit_add"))
		{
		    // Upload a new bitstream
		    assertAuthorized(Constants.ITEM,itemID,Constants.ADD)
		    result = doAddBitstream(itemID);
		}
		else if (cocoon.request.get("submit_edit") && cocoon.request.get("bitstreamID"))
		{
		    // Update the bitstream's metadata
		    var bitstreamID = UUID.fromString(cocoon.request.get("bitstreamID"));
			assertAuthorized(Constants.BITSTREAM,bitstreamID,Constants.WRITE)
		    result = doEditBitstream(itemID, bitstreamID);
		}
		else if (cocoon.request.get("submit_delete") && cocoon.request.get("remove"))
		{
			// Delete the bitstream
			assertAuthorized(Constants.ITEM,itemID,Constants.REMOVE);
			var bitstreamIDs = cocoon.request.getParameterValues("remove");

			result = doDeleteBitstreams(itemID,bitstreamIDs)
		}
        else if (submitButton.equals("submit_update_order") || submitButton.startsWith("submit_order_")){
            result = FlowItemUtils.processReorderBitstream(getDSContext(), itemID, cocoon.request);
        }
	} while (true)
}

/**
 * Allow the user to update, remove, and add new metadata to the item.
 */
function doEditItemMetadata(itemID, templateCollectionID)
{
	assertEditItem(itemID);

	var result;
	do {
		sendPageAndWait("admin/item/metadata",{"itemID":itemID,
                                               "templateCollectionID":templateCollectionID},result);
		assertEditItem(itemID);
		result = null;

		if (cocoon.request.get("submit_return") || cocoon.request.get("submit_status") ||
                    cocoon.request.get("submit_bitstreams") || cocoon.request.get("submit_metadata") ||
                    cocoon.request.get("view_item") || cocoon.request.get("submit_curate"))
		{
			// go back to wherever we came from.
			return null;
		}
		else if (cocoon.request.get("submit_add"))
		{
			// Add a new metadata entry
			result = FlowItemUtils.processAddMetadata(getDSContext(),itemID,cocoon.request);
		}
		else if (cocoon.request.get("submit_update"))
		{
			// Update the item
			result = FlowItemUtils.processEditItem(getDSContext(),itemID,cocoon.request);
		}
	} while (true)
}


/**
 * Curate an Item
 * Can only be performed by someone who is able to edit that Item.
 */
function doCurateItem(itemID, task)
{
    var result;

    do {
           if (cocoon.request.get("select_curate_group"))
	   {
                var select_curate_group = cocoon.request.getParameter("select_curate_group");
                sendPageAndWait("admin/item/curateItem",{"itemID":itemID,"select_curate_group":select_curate_group}, result);
	   } else {
                sendPageAndWait("admin/item/curateItem",{"itemID":itemID}, result);
           }
           assertEditItem(itemID);
           result = null;
           if (!cocoon.request.get("submit_curate_task") && !cocoon.request.get("submit_queue_task") && !cocoon.request.get("select_curate_group"))
           {
                return null;
           }
           else if (cocoon.request.get("submit_curate_task"))
           {
                result = FlowItemUtils.processCurateItem(getDSContext(), itemID, cocoon.request);
           }
           else if (cocoon.request.get("submit_queue_task"))
           {
                result = FlowItemUtils.processQueueItem(getDSContext(), itemID, cocoon.request);
           }
    } while (true);
}

/**
 * Confirm the deletion of this item.
 */
function doDeleteItem(itemID)
{
	assertAuthorized(Constants.ITEM, itemID, Constants.DELETE);

	sendPageAndWait("admin/item/delete",{"itemID":itemID});

	if (cocoon.request.get("submit_confirm"))
	{
		// It's been confirmed, delete the item.
		assertAuthorized(Constants.ITEM, itemID, Constants.DELETE);
		var result = FlowItemUtils.processDeleteItem(getDSContext(),itemID);

		if (result.getContinue()) {
			cocoon.redirectTo(cocoon.request.getContextPath()+"/community-list",true);
			getDSContext().complete();
			cocoon.exit();
		}

		return result;
	}
	return null;
}

/**
 * Confirm the withdrawal of this item.
 */
function doWithdrawItem(itemID)
{
	// authorization check performed directly by the dspace-api
	// assertAdministrator();

	sendPageAndWait("admin/item/withdraw",{"itemID":itemID});

	if (cocoon.request.get("submit_confirm"))
	{
		// Actually withdraw the item
		// authorization check performed directly by the dspace-api
		// assertAdministrator();
		var result = FlowItemUtils.processWithdrawItem(getDSContext(),itemID);
		return result;
	}
	return null;
}

/**
 * Confirm the reinstatiation of this item
 */
function doReinstateItem(itemID)
{
	// authorization check performed directly by the dspace-api
	// assertAdministrator();

	sendPageAndWait("admin/item/reinstate",{"itemID":itemID});

	if (cocoon.request.get("submit_confirm"))
	{
		// Actually reinstate the item
		// authorization check performed directly by the dspace-api
		// assertAdministrator();

		var result = FlowItemUtils.processReinstateItem(getDSContext(),itemID);
		return result;
	}
	return null;
}


function doPrivateItem(itemID)
{
    assertEditItem(itemID);
    sendPageAndWait("admin/item/private",{"itemID":itemID});

    if (cocoon.request.get("submit_confirm"))
    {
        var result = FlowItemUtils.processPrivateItem(getDSContext(),itemID);
        return result;
    }
    return null;
}

function doPublicItem(itemID)
{
    assertEditItem(itemID);
    sendPageAndWait("admin/item/public",{"itemID":itemID});

    if (cocoon.request.get("submit_confirm"))
    {
        var result = FlowItemUtils.processPublicItem(getDSContext(),itemID);
        return result;
    }
    return null;
}



/*
 * Move this item to another collection
 */
function doMoveItem(itemID)
{
    assertEditItem(itemID);

    var result;
    do {
        sendPageAndWait("admin/item/move",{"itemID":itemID});
        result = null;

        if (cocoon.request.get("submit_cancel"))
        {
            return null;
        }
        else if (cocoon.request.get("submit_move"))
        {
            var collectionID = UUID.fromString(cocoon.request.get("collectionID"));
            if (!collectionID)
            {
                continue;
            }

            var inherit = false;
            if (cocoon.request.get("inheritPolicies"))
            {
                inherit = true;
            }

            // Actually move the item
            assertEditItem(itemID);

            result = FlowItemUtils.processMoveItem(getDSContext(), itemID, collectionID, inherit);
        }
    } while (result == null || !result.getContinue());

    return result;
}

/**
 * Allow the user to upload a new bitstream to this item.
 */
function doAddBitstream(itemID)
{
    assertAuthorized(Constants.ITEM,itemID,Constants.ADD);
    var result;
    do {
        sendPageAndWait("admin/item/add-bitstream",{"itemID":itemID},result);
		assertAuthorized(Constants.ITEM,itemID,Constants.ADD);
        result = null;

        if (cocoon.request.get("submit_cancel"))
        {
            // return to whom ever called us
            return null;
        }
        else if (cocoon.request.get("submit_upload"))
        {
            // Upload the file
            result = FlowItemUtils.processAddBitstream(getDSContext(),itemID,cocoon.request);
        }
    } while (result == null || ! result.getContinue())

    return result;
}

/**
 * Allow the user to edit a bitstream's metadata (description & format)
 */
function doEditBitstream(itemID, bitstreamID)
{
    assertAuthorized(Constants.BITSTREAM,bitstreamID,Constants.WRITE);
    var result;
    do {
        sendPageAndWait("admin/item/edit-bitstream",{"itemID":itemID,"bitstreamID":bitstreamID},result);
		assertAuthorized(Constants.BITSTREAM,bitstreamID,Constants.WRITE);
        result = null;

        if (cocoon.request.get("submit_cancel"))
        {
            // return to whom ever called us
            return null;
        }
        else if (cocoon.request.get("submit_save"))
        {
            // Update the metadata
            var primary = cocoon.request.get("primary");
            var description = cocoon.request.get("description");
            var formatID = cocoon.request.get("formatID");
            var userFormat = cocoon.request.get("user_format");
            var bitstreamName = cocoon.request.get("bitstreamName");

            result = FlowItemUtils.processEditBitstream(getDSContext(),itemID,bitstreamID,bitstreamName,primary,description,formatID,userFormat, cocoon.request);
        }
    } while (result == null || ! result.getContinue())

    return result;
}

/**
 * Confirm and delete the given bitstreamIDs
 */
function doDeleteBitstreams(itemID, bitstreamIDs)
{
    assertAuthorized(Constants.ITEM,itemID,Constants.REMOVE);

    sendPageAndWait("admin/item/delete-bitstreams",{"itemID":itemID,"bitstreamIDs":bitstreamIDs.join(',')});

    if (cocoon.request.get("submit_confirm"))
    {
        // The user has confirmed.
        assertAuthorized(Constants.ITEM,itemID,Constants.REMOVE);

        var result = FlowItemUtils.processDeleteBitstreams(getDSContext(),itemID,bitstreamIDs);
        return result;
    }
    return null;

}


/**
 * Manage mapping items from one collection to another.
 */
function doMapItems(collectionID)
{
    assertEditCollection(collectionID);
    var result;

    do
    {
        sendPageAndWait("admin/mapper/main",{"collectionID":collectionID},result);
		assertEditCollection(collectionID);
        result = null;


        if (cocoon.request.get("submit_return"))
        {
            return null;
        }
        if (cocoon.request.get("submit_author"))
        {
            // Search for new items to added
			assertAuthorized(Constants.COLLECTION,collectionID,Constants.ADD);
            result = doMapItemSearch(collectionID);
        }
        else if (cocoon.request.get("submit_browse"))
        {
            // Browse existing mapped items
            result = doMapItemBrowse(collectionID);
        }
    } while (true);
}

/**
 * Manage batch metadata import
 *
 */

function doMetadataImport()
{
    var result;

    assertAdministrator();
    do
    {
        sendPageAndWait("admin/metadataimport/main",{},result);
        result = null;

        if (cocoon.request.get("submit_upload"))
        {
            result = doMetadataImportUpload();

        }

    } while (true);
}

function doMetadataImportUpload()
{
    var result = FlowMetadataImportUtils.processUploadCSV(getDSContext(),cocoon.request);

    assertAdministrator();
    do
    {
        sendPageAndWait("admin/metadataimport/upload",{},result);
        result = null;

        if (cocoon.request.get("submit_return"))
        {
            return null;
        }
        else if (cocoon.request.get("submit_confirm"))
        {

            result = doMetadataImportConfirm();
            return result;
        }
    } while (true);
}

function doMetadataImportConfirm()
{
    var result = FlowMetadataImportUtils.processMetadataImport(getDSContext(),cocoon.request);
    assertAdministrator();
    sendPageAndWait("admin/metadataimport/confirm",{},result);
    return null;
}

/**
 * Manage batch metadata import
 *
 */

function doBatchImport()
{
    var result;

    assertAdministrator();
    do
    {
        sendPageAndWait("admin/batchimport/main",{},result);
        result = null;

        if (cocoon.request.get("submit_upload"))
        {
            result = doBatchImportUpload();

        }

    } while (true);
}

function doBatchImportUpload()
{
    var result = FlowBatchImportUtils.processUploadZIP(getDSContext(),cocoon.request);

    assertAdministrator();
    do
    {
        sendPageAndWait("admin/batchimport/upload",{},result);
        result = null;

        if (cocoon.request.get("submit_return"))
        {
            return null;
        }
        else if (cocoon.request.get("submit_confirm"))
        {

            result = doBatchImportConfirm();
            return result;
        }
    } while (true);
}

function doBatchImportConfirm()
{
    var result = FlowBatchImportUtils.processBatchImport(getDSContext(),cocoon.request);
    assertAdministrator();
    sendPageAndWait("admin/batchimport/confirm",{},result);
    return null;
}

/**
 * Search for new items to map into the collection
 */
function doMapItemSearch(collectionID)
{
 	assertAuthorized(Constants.COLLECTION,collectionID,Constants.ADD);
    var result;
    var query = cocoon.request.get("query");


    sendPageAndWait("admin/mapper/search",{"collectionID":collectionID,"query":query},result);
	assertAuthorized(Constants.COLLECTION,collectionID,Constants.ADD);

    if (cocoon.request.get("submit_cancel"))
    {
        // return back
        return null;
    }
    else if (cocoon.request.get("submit_map") && cocoon.request.get("itemID"))
    {
        // map the selected items
        var itemIDs = cocoon.request.getParameterValues("itemID");
        result = FlowMapperUtils.processMapItems(getDSContext(),collectionID,itemIDs);
        return result;
    }
}


/**
 * Browse items that have been mapped into this collection.
 */
function doMapItemBrowse(collectionID)
{
    var result;

    do {
        sendPageAndWait("admin/mapper/browse",{"collectionID":collectionID});

        if (cocoon.request.get("submit_return"))
        {
            // return back to where they came from.
            return null;
        }
        else if (cocoon.request.get("submit_unmap") && cocoon.request.get("itemID"))
        {
            // Unmap these items.
			assertAuthorized(Constants.COLLECTION,collectionID,Constants.REMOVE);
            var itemIDs = cocoon.request.getParameterValues("itemID");
            result = FlowMapperUtils.processUnmapItems(getDSContext(), collectionID, itemIDs);
        }
    } while (true);
}



/**
 * Authorization Flows
 */

/*
 * Current entry point into the authorization flow. Presents the user with form to look up items,
 * perform wildcard authorizations, or select a collection/community from the list to edit.
 */
function doManageAuthorizations()
{
    // authorization check moved to FlowAuthorizationUtils
    // assertAdministrator();

    var result = null;
    var query = "";

    do {
        sendPageAndWait("admin/authorize/main",{"query":query},result);

        // authorization check moved to FlowAuthorizationUtils
    	// assertAdministrator();

        result = null;

        // if an identifier of some sort was entered into the lookup field
        if (cocoon.request.get("submit_edit") && cocoon.request.get("identifier"))
        {
            var identifier = cocoon.request.get("identifier");
            query = identifier;

            // resolve the identifier to a type, look up its associated object, and act accordingly
            result = FlowAuthorizationUtils.resolveItemIdentifier(getDSContext(),identifier);
            if (result.getParameter("type") == Constants.ITEM && result.getParameter("itemID"))
            {
            	var itemID = UUID.fromString(result.getParameter("itemID"));
                result = doAuthorizeItem(itemID);
            }
            else if (result.getParameter("type") == Constants.COLLECTION && result.getParameter("collectionID"))
            {
            	var collectionID = UUID.fromString(result.getParameter("collectionID"));
                result = doAuthorizeCollection(collectionID);
            }
            else if (result.getParameter("type") == Constants.COMMUNITY && result.getParameter("communityID"))
            {
            	var communityID = UUID.fromString(result.getParameter("communityID"));
                result = doAuthorizeCommunity(communityID);
            }
        }
        // Clicking to edit a collection's authorizations
        else if (cocoon.request.get("submit_edit") && cocoon.request.get("collection_id"))
        {
            var collectionID = UUID.fromString(cocoon.request.get("collection_id"));
            result = doAuthorizeCollection(collectionID);
        }
        // Clicking to edit a community's authorizations
        else if (cocoon.request.get("submit_edit") && cocoon.request.get("community_id"))
        {
            var communityID = UUID.fromString(cocoon.request.get("community_id"));
            result = doAuthorizeCommunity(communityID);
        }
        // Wildcard/advanced authorizations
        else if (cocoon.request.get("submit_wildcard"))
        {
            result = doAdvancedAuthorization();
        }
    }
    while(true);
}


/*
 * Wrapper functions for editing authorization policies for collections and communities
 */
function doAuthorizeCollection(collectionID)
{
    doAuthorizeContainer(Constants.COLLECTION,collectionID);
}
function doAuthorizeCommunity(communityID)
{
    doAuthorizeContainer(Constants.COMMUNITY,communityID);
}

/*
 * Editing authorization policies for collections and communities, collectively referred to as "containers"
 */
function doAuthorizeContainer(containerType, containerID)
{
    // authorization check moved to FlowAuthorizationUtils
    // must be an ADMIN on the container to change its authorizations
    // assertAuthorized(containerType, containerID, Constants.ADMIN);

    var result;
    var highlightID;

    do {
        sendPageAndWait("admin/authorize/container",{"containerType":containerType,"containerID":containerID,"highlightID":highlightID},result);
        // authorization check moved to FlowAuthorizationUtils
    	// assertAuthorized(containerType, containerID, Constants.ADMIN);
        result = null;

        // Cancel out the operation
        if (cocoon.request.get("submit_return")) {
            return null;
        }
        else if (cocoon.request.get("submit_add")) {
            // Create a new policy (pass policyID=-1 to create a new one)
            result = doEditPolicy(containerType,containerID,-1);
            if (result != null && result.getParameter("policyID"))
            	highlightID = result.getParameter("policyID");
        }
        else if (cocoon.request.get("submit_edit") && cocoon.request.get("policy_id")) {
            // Edit an existing policy
            var policyID = cocoon.request.get("policy_id");
            result = doEditPolicy(containerType,containerID,policyID);
            highlightID = policyID;
        }
        else if (cocoon.request.get("submit_delete") && cocoon.request.get("select_policy")) {
            // Delete existing policies
            var policyIDs = cocoon.request.getParameterValues("select_policy");
            result = doDeletePolicies(policyIDs);
            highlightID = null;
        }
        else if (cocoon.request.get("submit_edit_group") && cocoon.request.get("group_id")) {
            // Edit a group from the authorization screen
            var groupID = UUID.fromString(cocoon.request.get("group_id"));
            result = doEditGroup(groupID);
        }
    }
    while (true);
}



/*
 * Editing authorization policies for items
 */
function doAuthorizeItem(itemID)
{
    // authorization check moved to FlowAuthorizationUtils
    // assertAdministrator();

    var result;
    var highlightID;

    do {
        sendPageAndWait("admin/authorize/item",{"itemID":itemID,"highlightID":highlightID},result);
        // authorization check moved to FlowAuthorizationUtils
    	// assertAdministrator();
        result = null;

        var bundleID = extractSubmitSuffix("submit_add_bundle_");
        var bitstreamID = extractSubmitSuffix("submit_add_bitstream_");

        // Cancel out the operation
        if (cocoon.request.get("submit_return")) {
            return null;
        }
        else if (bundleID)
        {
            bundleID = UUID.fromString(bundleID);
            // Create a new policy for a bundle
            result = doEditPolicy(Constants.BUNDLE, bundleID, -1);
            if (result != null && result.getParameter("policyID"))
            	highlightID = result.getParameter("policyID");
        }
        else if (bitstreamID)
        {
            bitstreamID = UUID.fromString(bitstreamID);
            // Create a new policy for a bitstream
             result = doEditPolicy(Constants.BITSTREAM, bitstreamID,-1);
            if (result != null && result.getParameter("policyID"))
            	highlightID = result.getParameter("policyID");
        }
        else if (cocoon.request.get("submit_add_item")) {
            // Create a new policy for the item
            result = doEditPolicy(Constants.ITEM, itemID,-1);
            if (result != null && result.getParameter("policyID"))
            	highlightID = result.getParameter("policyID");
        }
        else if (cocoon.request.get("submit_edit") && cocoon.request.get("policy_id")
                && cocoon.request.get("object_id") && cocoon.request.get("object_type")) {
            // Edit an existing policy
            var policyID = cocoon.request.get("policy_id");
            var objectID = UUID.fromString(cocoon.request.get("object_id"));
            var objectType = cocoon.request.get("object_type");
            result = doEditPolicy(objectType,objectID,policyID);
            highlightID = policyID;
        }
        else if (cocoon.request.get("submit_delete") && cocoon.request.get("select_policy")) {
            // Delete existing policies
            var policyIDs = cocoon.request.getParameterValues("select_policy");
            result = doDeletePolicies(policyIDs);
            highlightID = null;
        }
        else if (cocoon.request.get("submit_edit_group") && cocoon.request.get("group_id")) {
            // Edit a group from the authorization screen
            var groupID = UUID.fromString(cocoon.request.get("group_id"));
            result = doEditGroup(groupID);
        }
    }
    while (true);
}


/*
 * Advanced or "wildcard" authorizations; presents the user with a list of communities and list of groups,
 * together with options for an authorization action and a recepient of that action. The user can select
 * multiple collections and multiple groups to create repository-wide authorizations.
 */
function doAdvancedAuthorization()
{
    assertAdministrator();
    var result;
    var groupIDs;
    var actionID;
    var resourceID;
    var collectionIDs;
    var name;
    var description;
    var startDate;
    var endDate;
    do {

        if(groupIDs!=null)
            cocoon.request.setAttribute("groupIDs", groupIDs);
        if(collectionIDs!=null)
            cocoon.request.setAttribute("collectionIDs", collectionIDs);

        sendPageAndWait("admin/authorize/advanced",{"resource_id":resourceID,"action_id":actionID,"name":name,
                            "description":description, "startDate":startDate, "endDate":endDate},result);
        assertAdministrator();
        result = null;

        // For all of the selected groups...
        groupIDs = cocoon.request.getParameterValues("group_id");
        // ...grant the ability to perform the following action...
        actionID = Integer.parseInt(cocoon.request.get("action_id"));
        // ...for all following object types...
        resourceID = Integer.parseInt(cocoon.request.get("resource_id"));
        // ...across the following collections.
        collectionIDs = cocoon.request.getParameterValues("collection_id");

        name = cocoon.request.get("name");
        description = cocoon.request.get("description");
        startDate = cocoon.request.get("start_date");
        endDate = cocoon.request.get("end_date");

        if (cocoon.request.get("submit_return"))
        {
            return null;
        }
        else if (cocoon.request.get("submit_add"))
        {
            result = FlowAuthorizationUtils.processAdvancedPolicyAdd(getDSContext(),groupIDs,actionID,resourceID,collectionIDs, name, description, startDate, endDate);
        }
        else if (cocoon.request.get("submit_remove_all"))
        {
            result = FlowAuthorizationUtils.processAdvancedPolicyDelete(getDSContext(),resourceID,collectionIDs);
        }
    }
    while(result == null || !result.getContinue());
}


/**
 * Edit a policy, giving the user a choice of actions and the ability to either browse to a desired group via
 * drop-down field, or search for one via search box.
 */
function doEditPolicy(objectType,objectID,policyID)
{
    // authorize check moved to FlowAuthorizationUtils.processEditPolicy
    // assertAdministrator();

    var result;
    var query= "-1";
    var groupID;
    var actionID = -1;
    var page = 0;
    var name;
    var description;
    var startDate;
    var endDate;

    do {
    	/* The page receives parameters for the type and ID of the DSpace object that the policy is assciated with, the
    	 * policy ID, the group search query (if a search was performed), the ID of the currently associated group, the
    	 * current action and the currently viewed page (if a search returned more than one page of results) */
        sendPageAndWait("admin/authorize/edit",{"objectType":objectType,"objectID":objectID,"policyID":policyID,"query":query,"groupID":groupID,"actionID":actionID,"page":page,
                                                "name":name, "description":description, "startDate":startDate, "endDate":endDate},result);
        // authorize check moved to FlowAuthorizationUtils.processEditPolicy
    	// assertAdministrator();
        result = null;

        // Figure out which button was pressed on the group search results page
        var names = cocoon.request.getParameterNames();
        while (names.hasMoreElements())
        {
                var name = names.nextElement();
                var match = null;

	        if ((match = name.match(/submit_group_id_([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/)) != null)
			{
	        	groupID = UUID.fromString(match[1]);
			}

			if (cocoon.request.get("action_id"))
        		actionID = Integer.parseInt(cocoon.request.get("action_id"));
        }

        if (cocoon.request.get("page")) {
        	page = cocoon.request.get("page");
        }

        // perform a search to set a group
        if (cocoon.request.get("submit_search_groups")) {
        	query = cocoon.request.get("query");
        	if (cocoon.request.get("action_id"))
        		actionID = Integer.parseInt(cocoon.request.get("action_id"));
        	page = 0;

			name = cocoon.request.get("name");
			description = cocoon.request.get("description");
			startDate = cocoon.request.get("start_date");
			endDate = cocoon.request.get("end_date");
			
        }
        else if (cocoon.request.get("submit_save"))
        {
            groupID = UUID.fromString(cocoon.request.get("group_id"));
            //if (groupID == null) groupID = -1;
            name = cocoon.request.get("name");
            description = cocoon.request.get("description");
            startDate = cocoon.request.get("start_date");
            endDate = cocoon.request.get("end_date");

            result = FlowAuthorizationUtils.processEditPolicy(getDSContext(),objectType,objectID,policyID,groupID,actionID, name, description, startDate, endDate);
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            return null;
        }
        // TODO; make sure to handle the odd case of the target group getting deleted from inside the authorization flow
        else if (cocoon.request.get("submit_edit_group") && cocoon.request.get("group_id"))
        {
            var editGroupID = UUID.fromString(cocoon.request.get("group_id"));
            result = doEditGroup(editGroupID);

            if (result != null)
                result.setContinue(false);
        }
    }
    while(result == null || !result.getContinue());

    return result;
}


/**
 * Confirm that the given policies should be deleted; if confirmed they will be deleted.
 */
function doDeletePolicies(policyIDs)
{
    // authorization check moved to FlowAuthorizationUtils
    // assertAdministrator();
    sendPageAndWait("admin/authorize/delete",{"policyIDs":policyIDs.join(',')});

    if (cocoon.request.get("submit_confirm"))
    {
        // The user has confirmed, actually delete these policies
    	// authorization check moved to FlowAuthorizationUtils
    	// assertAdministrator();

        var result = FlowAuthorizationUtils.processDeletePolicies(getDSContext(),policyIDs);
        return result;
    }
    return null;

}




/**
 * Community/Collection editing
 */


function doEditCollection(collectionID,newCollectionP)
{
	assertEditCollection(collectionID);

	// If this is a new collection, then go to
	// role assignment first, otherwise start with
	// the metadata
	if (newCollectionP)
		doAssignCollectionRoles(collectionID);
	else
		doEditCollectionMetadata(collectionID);

	do {

		if (cocoon.request.get("submit_return") || cocoon.request.get("submit_save"))
		{
			// go back to wherever we came from.
			return null;
		}
		else if (cocoon.request.get("submit_metadata"))
		{
			// go edit collection metadata
			doEditCollectionMetadata(collectionID)
		}
		else if (cocoon.request.get("submit_roles"))
		{
			// go assign collection roles
			doAssignCollectionRoles(collectionID);
		}
		else if (cocoon.request.get("submit_harvesting"))
		{
			// edit collection harvesting settings
			doEditCollectionHarvesting(collectionID);
		}
		else  if (cocoon.request.get("submit_curate"))
                {
                        doCurateCollection(collectionID, cocoon.request.get("curate_task"));
                }
                else
		{
			// This case should never happen but to prevent an infinite loop
			// from occurring let's just return null.
			return null;
		}
	} while (true)
}


/**
 * Edit metadata of a collection; presenting the user with a form of standard collection metadata,
 * an option add/remove a logo, and set the item template. From here the user can also move on to
 * edit roles and authorizations screen
 */
function doEditCollectionMetadata(collectionID)
{
	assertEditCollection(collectionID);

	var result;

	do {
		sendPageAndWait("admin/collection/editMetadata",{"collectionID":collectionID},result);
		assertEditCollection(collectionID);
		result=null;

		if (cocoon.request.get("submit_return") || cocoon.request.get("submit_metadata") ||
			cocoon.request.get("submit_roles") || cocoon.request.get("submit_harvesting") ||
                        cocoon.request.get("submit_curate"))
		{
			// return to the editCollection function which will determine where to go next.
			return null;
		}
		else if (cocoon.request.get("submit_save"))
		{
			// Save updates
			result = FlowContainerUtils.processEditCollection(getDSContext(), collectionID, false, cocoon.request);
      if (result.getContinue())
         return null;
		}
		else if (cocoon.request.get("submit_delete"))
		{
			// delete collection
			assertAdministrator();

			result = doDeleteCollection(collectionID);
		}
		else if (cocoon.request.get("submit_delete_logo"))
		{
			// Delete the collection's logo
			result = FlowContainerUtils.processEditCollection(getDSContext(), collectionID, true, cocoon.request);
		}
		else if (cocoon.request.get("submit_create_template") || cocoon.request.get("submit_edit_template"))
		{
			// Create or edit the item's template
			var itemID = FlowContainerUtils.getTemplateItemID(getDSContext(), collectionID);
			result = doEditItemMetadata(itemID, collectionID);
		}
		else if (cocoon.request.get("submit_delete_template"))
		{
			// Delete the item's template
	    assertEditCollection(collectionID);

 			result = FlowContainerUtils.processDeleteTemplateItem(getDSContext(), collectionID);
		}

	}while (true);
}

/**
 * Edit the set roles for a collection: admin, workflows, submitters, and default read.
 * Returns to the EditCollection page and selected tab if this is a simple navigation,
 * not a submit.
 */
function setXMLWorkflowRoles(workflow, collectionID, result) {
    var roles = workflow.getRoles().keySet().toArray();
    for (var i = 0; i < roles.length; i++) {
        if (cocoon.request.get("submit_delete_wf_role_" + roles[i])) {
            assertAdminCollection(collectionID);
            result = doDeleteCollectionRole(collectionID, roles[i]);
        }
        if (cocoon.request.get("submit_edit_wf_role_" + roles[i])) {
            assertEditCollection(collectionID);
            var groupID = FlowContainerUtils.getCollectionRole(getDSContext(), collectionID, roles[i]);
            result = doEditGroup(groupID);
        }
        if (cocoon.request.get("submit_create_wf_role_" + roles[i])) {
            assertEditCollection(collectionID);
            var groupID = FlowContainerUtils.getCollectionRole(getDSContext(), collectionID, roles[i]);
            result = doEditGroup(groupID);
        }
    }
    return result;
}
function setOriginalWorkflowRoles(collectionID, result) {
    // WORKFLOW STEPS 1-3
    if (cocoon.request.get("submit_edit_wf_step1") || cocoon.request.get("submit_create_wf_step1")) {
        assertEditCollection(collectionID);
        var groupID = FlowContainerUtils.getCollectionRole(getDSContext(), collectionID, "WF_STEP1");
        result = doEditGroup(groupID);
    }
    else if (cocoon.request.get("submit_delete_wf_step1")) {
        result = doDeleteCollectionRole(collectionID, "WF_STEP1");
    }

    else if (cocoon.request.get("submit_edit_wf_step2") || cocoon.request.get("submit_create_wf_step2")) {
        assertEditCollection(collectionID);
        var groupID = FlowContainerUtils.getCollectionRole(getDSContext(), collectionID, "WF_STEP2");
        result = doEditGroup(groupID);
    }
    else if (cocoon.request.get("submit_delete_wf_step2")) {
        result = doDeleteCollectionRole(collectionID, "WF_STEP2");
    }

    else if (cocoon.request.get("submit_edit_wf_step3") || cocoon.request.get("submit_create_wf_step3")) {
        assertEditCollection(collectionID);
        var groupID = FlowContainerUtils.getCollectionRole(getDSContext(), collectionID, "WF_STEP3");
        result = doEditGroup(groupID);
    }
    else if (cocoon.request.get("submit_delete_wf_step3")) {
        result = doDeleteCollectionRole(collectionID, "WF_STEP3");
    }
    return result;
}
function doAssignCollectionRoles(collectionID)
{
	assertEditCollection(collectionID);

	var result;
	var workflow = null;

	do {
		sendPageAndWait("admin/collection/assignRoles",{"collectionID":collectionID},result);
		assertEditCollection(collectionID);
		result = null;


		if (cocoon.request.get("submit_return") || cocoon.request.get("submit_metadata") ||
			cocoon.request.get("submit_roles") || cocoon.request.get("submit_harvesting") ||
                        cocoon.request.get("submit_curate"))
		{
			// return to the editCollection function which will determine where to go next.
			return null;
		}

		else if (cocoon.request.get("submit_authorizations"))
		{
			// general authorizations
			// assertAdminCollection(collectionID);
			result = doAuthorizeCollection(collectionID);
		}

		// ADMIN
		else if (cocoon.request.get("submit_edit_admin") || cocoon.request.get("submit_create_admin"))
		{
			assertEditCollection(collectionID);
			var groupID = FlowContainerUtils.getCollectionRole(getDSContext(),collectionID, "ADMIN");
			result = doEditGroup(groupID);
		}
		else if (cocoon.request.get("submit_delete_admin")) {
			result = doDeleteCollectionRole(collectionID, "ADMIN");
		}

		// SUBMIT
		else if (cocoon.request.get("submit_edit_submit") || cocoon.request.get("submit_create_submit"))
		{
			assertEditCollection(collectionID);
			var groupID = FlowContainerUtils.getCollectionRole(getDSContext(),collectionID, "SUBMIT");
			result = doEditGroup(groupID);
		}
		else if (cocoon.request.get("submit_delete_submit"))
		{
			result = doDeleteCollectionRole(collectionID, "SUBMIT");
		}

		// DEFAULT_READ
		else if (cocoon.request.get("submit_create_default_read"))
		{
        assertAdminCollection(collectionID);

		    var groupID = FlowContainerUtils.createCollectionDefaultReadGroup(getDSContext(), collectionID);
		    result = doEditGroup(groupID);
		}
		else if (cocoon.request.get("submit_edit_default_read"))
		{
			assertEditCollection(collectionID);
			var groupID = FlowContainerUtils.getCollectionDefaultRead(getDSContext(), collectionID);
			result = doEditGroup(groupID);
		}
		else if (cocoon.request.get("submit_delete_default_read"))
		{
			result = doDeleteCollectionRole(collectionID, "DEFAULT_READ");
		}else{
            if(WorkflowServiceFactory.getInstance().getWorkflowService() instanceof XmlWorkflowService){
                if(workflow == null){
                    var collection = getCollectionService().find(getDSContext(),collectionID);
                    workflow = getXmlWorkflowFactory().getWorkflow(collection);
                }
                result = setXMLWorkflowRoles(workflow, collectionID, result);
            }else{
                result = setOriginalWorkflowRoles(collectionID, result);
		    }
        }

	}while(true);
}

/**
 * Curate a Collection
 * Can only be performed by someone who is able to edit that collection.
 */
function doCurateCollection(collectionID, task) {
    var result;

    do {
                   if (cocoon.request.get("select_curate_group"))
                   {
                      var select_curate_group = cocoon.request.getParameter("select_curate_group");
                      sendPageAndWait("admin/collection/curateCollection",{"collectionID":collectionID,"select_curate_group":select_curate_group}, result);
                   } else {
                      sendPageAndWait("admin/collection/curateCollection",{"collectionID":collectionID}, result);
                   } 
		   assertEditCollection(collectionID);
		   result = null;
		   if (cocoon.request.get("submit_return") || cocoon.request.get("submit_metadata") ||
			cocoon.request.get("submit_roles") || cocoon.request.get("submit_harvesting") ||
                        cocoon.request.get("submit_curate"))
                   {
			  // return to the editCollection function which will determine where to go next.
			  return null;
		   }

                  if (cocoon.request.get("submit_curate_task"))
		   {
			   result = FlowContainerUtils.processCurateCollection(getDSContext(), collectionID, cocoon.request);
		   }
                   else if (cocoon.request.get("submit_queue_task"))
                   {
                           result = FlowContainerUtils.processQueueCollection(getDSContext(), collectionID, cocoon.request);
                   }
    }
    while (true);
}



/**
 * Set up various harvesting options.
 * From here the user can also move on to edit roles and edit metadata screen.
 */
function doSetupCollectionHarvesting(collectionID)
{
	assertAdminCollection(collectionID);

	var result = null;
	var oaiProviderValue = null;
	var oaiSetAll = null;
	var oaiSetIdValue = null;
	var metadataFormatValue = null;
	var harvestLevelValue = null;

	do {
		sendPageAndWait("admin/collection/setupHarvesting",{"collectionID":collectionID,"oaiProviderValue":oaiProviderValue,"oaiSetAll":oaiSetAll,"oaiSetIdValue":oaiSetIdValue,"metadataFormatValue":metadataFormatValue,"harvestLevelValue":harvestLevelValue},result);
		result = null;
		oaiProviderValue = cocoon.request.get("oai_provider");
		oaiSetAll = cocoon.request.get("oai-set-setting");
		oaiSetIdValue = cocoon.request.get("oai_setid");
		metadataFormatValue = cocoon.request.get("metadata_format");
		harvestLevelValue = cocoon.request.get("harvest_level");

		assertAdminCollection(collectionID);

		if (cocoon.request.get("submit_return") || cocoon.request.get("submit_metadata") ||
			cocoon.request.get("submit_roles") || cocoon.request.get("submit_harvesting") ||
		cocoon.request.get("submit_curate"))
		{
			// return to the editCollection function which will determine where to go next.
			return null;
		}
		else if (cocoon.request.get("submit_save"))
		{
			// Save updates
			result = FlowContainerUtils.processSetupCollectionHarvesting(getDSContext(), collectionID, cocoon.request);
		}
		else if (cocoon.request.get("submit_test"))
		{
			// Ping the OAI server and verify that the address/set/metadata combo is present there
			// Can get this either in a single GetRecords OAI request or via two separate ones: ListSets and ListMetadataFormats
			result = FlowContainerUtils.testOAISettings(getDSContext(), cocoon.request);
		}

	} while (!result.getContinue());
}


/**
 * Edit existing harvesting options.
 * From here the user can also move on to edit roles and edit metadata screen.
 */
function doEditCollectionHarvesting(collectionID)
{
	assertAdminCollection(collectionID);

	var result = null;
	do
	{
		// If this collection's havresting is not set up properly, redirect to the setup screen
        var collection = getCollectionService().find(getDSContext(), collectionID);
        if (getHarvestedCollectionService().find(getDSContext(), collection) == null) {
			sendPageAndWait("admin/collection/toggleHarvesting",{"collectionID":collectionID},result);
		}
		else if (!getHarvestedCollectionService().isHarvestable(getDSContext(), collection)) {
			doSetupCollectionHarvesting(collectionID);
		}
		else {
			sendPageAndWait("admin/collection/editHarvesting",{"collectionID":collectionID},result);
		}

		result = null;
		assertAdminCollection(collectionID);

		if (cocoon.request.get("submit_return") || cocoon.request.get("submit_metadata") ||
			cocoon.request.get("submit_roles") || cocoon.request.get("submit_harvesting") ||
                        cocoon.request.get("submit_curate"))
		{
			// return to the editCollection function which will determine where to go next.
			return null;
		}
		else if (cocoon.request.get("submit_save"))
		{
			// Save updates
			result = FlowContainerUtils.processSetupCollectionHarvesting(getDSContext(), collectionID, cocoon.request);
		}
		else if (cocoon.request.get("submit_import_now"))
		{
			// Test the settings and run the import immediately
			result = FlowContainerUtils.processRunCollectionHarvest(getDSContext(), collectionID, cocoon.request);
		}
		else if (cocoon.request.get("submit_reimport"))
		{
			// Test the settings and run the import immediately
			result = FlowContainerUtils.processReimportCollection(getDSContext(), collectionID, cocoon.request);
		}
		else if (cocoon.request.get("submit_change"))
		{
			doSetupCollectionHarvesting(collectionID);
		}
	} while (true);
}




/**
 * Delete a specified collection role. Under the current implementation, the only roles this applies to
 * directly are the workflow steps. Admin and submitter authorizations cannot be deleted once formed, and
 * the default read group is changed to Anonymous instead.
 */
function doDeleteCollectionRole(collectionID,role)
{
	assertAdminCollection(collectionID);

	var groupID;

	if (role == "DEFAULT_READ") {
		groupID = FlowContainerUtils.getCollectionDefaultRead(getDSContext(), getCollectionService().find(getDSContext(), collectionID)).getID();
	}
	else {
		groupID = FlowContainerUtils.getCollectionRole(getDSContext(),collectionID, role);
	}

	sendPageAndWait("admin/collection/deleteRole",{"collectionID":collectionID,"role":role,"groupID":groupID});
 	assertAdminCollection(collectionID);

	if (cocoon.request.get("submit_confirm") && role == "DEFAULT_READ")
	{
	    // Special case for default_read
	    var result = FlowContainerUtils.changeCollectionDefaultReadToAnonymous(getDSContext(), collectionID);
	    return result;
	}
	else if  (cocoon.request.get("submit_confirm"))
	{
	    // All other roles use the standard methods
            var result = FlowContainerUtils.processDeleteCollectionRole(getDSContext(),collectionID,role,groupID);
            return result;
	}
	return null;
}

/**
 * Delete an entire collection, requesting a confirmation first.
 */
function doDeleteCollection(collectionID)
{
	assertAuthorized(Constants.COLLECTION, collectionID, Constants.DELETE);

	sendPageAndWait("admin/collection/delete",{"collectionID":collectionID});
	assertAuthorized(Constants.COLLECTION, collectionID, Constants.DELETE);

	if (cocoon.request.get("submit_confirm"))
	{
		var result = FlowContainerUtils.processDeleteCollection(getDSContext(),collectionID);

		if (result.getContinue()) {
			cocoon.redirectTo(cocoon.request.getContextPath()+"/community-list",true);
			getDSContext().complete();
			cocoon.exit();
		}
	}
	return null;
}


// Creating a new collection, given the ID of its parent community
function doCreateCollection(communityID)
{
	assertAuthorized(Constants.COMMUNITY,communityID,Constants.ADD);

	var result;
	var collectionID;

	do {
		sendPageAndWait("admin/collection/createCollection",{"communityID":communityID},result);
		assertAuthorized(Constants.COMMUNITY,communityID,Constants.ADD);
		result=null;

		if (cocoon.request.get("submit_save")) {
			// create the collection, passing back its ID
			result = FlowContainerUtils.processCreateCollection(getDSContext(), communityID, cocoon.request);

			// send the user to the authorization screen
			if (result.getContinue() && result.getParameter("collectionID")) {
				collectionID = UUID.fromString(result.getParameter("collectionID"));
				result = doEditCollection(collectionID,true);
				// If they return then pass them back to where they came from.
				return result;
			}
		}
		else if (cocoon.request.get("submit_cancel")) {
			cocoon.redirectTo(cocoon.request.getContextPath()+"/community-list",true);
			getDSContext().complete();
			cocoon.exit();
		}
	} while (true);
}





// Creating a new community, given the ID of its parent community or an ID of -1 to designate top-level
function doCreateCommunity(parentCommunityID)
{
	var result;
	var newCommunityID;
	// If we are not passed a communityID from the flow, we assume that is passed in from the sitemap
	if (parentCommunityID == null && cocoon.request.getParameter("communityID") != null)
	{
        parentCommunityID = UUID.fromString(cocoon.request.getParameter("communityID"));
	}

	assertEditCommunity(parentCommunityID);

    do {
		sendPageAndWait("admin/community/createCommunity",{"communityID":parentCommunityID},result);
    assertEditCommunity(parentCommunityID);
		result=null;

		if (cocoon.request.get("submit_save")) {
			// create the community, passing back its ID
			result = FlowContainerUtils.processCreateCommunity(getDSContext(), parentCommunityID, cocoon.request);

			// send the user to the newly created community
			if (result.getContinue() && result.getParameter("communityID")) {
				newCommunityID = UUID.fromString(result.getParameter("communityID"));
				result = doEditCommunity(newCommunityID);
				return result;
			}
		}
		else if (cocoon.request.get("submit_cancel")) {
			cocoon.redirectTo(cocoon.request.getContextPath()+"/community-list",true);
			getDSContext().complete();
			cocoon.exit();
		}
	} while (true);
}


/**
 * Edit a community.
 */
function doEditCommunity(communityID)
{
	// Always go to the status page first
	doEditCommunityMetadata(communityID);

	do {
	    if (cocoon.request.get("submit_return"))
		{
			return null;
		}
                else if (cocoon.request.get("submit_metadata")) {
                    doEditCommunityMetadata(communityID);
                }
		else if (cocoon.request.get("submit_status"))
		{
			doEditItemStatus(communityID);
		}
		else if (cocoon.request.get("submit_bitstreams"))
		{
			doEditItemBitstreams(communityID);
		}
		else if (cocoon.request.get("submit_save") || cocoon.request.get("submit_delete") || cocoon.request.get("submit_delete_logo"))
		{
			doEditCommunityMetadata(communityID, -1);
		}
                else if (cocoon.request.get("submit_authorizations")) {
			result = doAuthorizeCommunity(communityID);
		}
		else if (cocoon.request.get("submit_roles"))
		{
			doAssignCommunityRoles(communityID);
		}
	        else if (cocoon.request.get("submit_curate"))  {
                        doCurateCommunity(communityID, cocoon.request.get("curate_task"));
                }
                else
		{
			// This case should never happen but to prevent an infinite loop
			// from occurring let's just return null.
			return null;
		}
	} while (true)
}

/**
 * Edit metadata of a community; presenting the user with a form of standard community metadata,
 * an option add/remove a logo and a link to the authorizations screen
 */
function doEditCommunityMetadata(communityID)
{
	assertEditCommunity(communityID);
	var result;

	do {
		sendPageAndWait("admin/community/editMetadata",{"communityID":communityID},result);
		assertEditCommunity(communityID);
		result=null;

		if (cocoon.request.get("submit_return") || cocoon.request.get("submit_metadata") ||  cocoon.request.get("submit_roles") || cocoon.request.get("submit_curate")){
			return null;
		}
		if (cocoon.request.get("submit_save")) {
			result = FlowContainerUtils.processEditCommunity(getDSContext(), communityID, false, cocoon.request);
                        if (result.getContinue())
                            return null;
		} else if (cocoon.request.get("submit_delete"))  {
			assertAuthorized(Constants.COMMUNITY, communityID, Constants.DELETE);
			result = doDeleteCommunity(communityID);
		}
		else if (cocoon.request.get("submit_delete_logo")) {
			result = FlowContainerUtils.processEditCommunity(getDSContext(), communityID, true, cocoon.request);
		}
	}
        while (true);
}

/**
 * Delete an entire community, asking for a confirmation first
 */
function doDeleteCommunity(communityID) {
	assertAuthorized(Constants.COMMUNITY, communityID, Constants.DELETE);

	sendPageAndWait("admin/community/delete",{"communityID":communityID});
	assertAuthorized(Constants.COMMUNITY, communityID, Constants.DELETE);

	if (cocoon.request.get("submit_confirm"))
	{
		var result = FlowContainerUtils.processDeleteCommunity(getDSContext(),communityID);

		if (result.getContinue()) {
			cocoon.redirectTo(cocoon.request.getContextPath()+"/community-list",true);
			getDSContext().complete();
			cocoon.exit();
		}
	}
	return null;
}

/**
 * Edit the administrative role for a community.
 */
function doAssignCommunityRoles(communityID)
{
    var result;

    do {
		   sendPageAndWait("admin/community/assignRoles",{"communityID":communityID},result);
		   assertEditCommunity(communityID);
		   result = null;


		   if (cocoon.request.get("submit_return") || cocoon.request.get("submit_metadata") || cocoon.request.get("submit_roles") || cocoon.request.get("submit_curate") )
		   {
			   // return to the editCommunity function which will determine where to go next.
			   return null;
		   }
		   else if (cocoon.request.get("submit_authorizations"))
		   {
			   // authorization check moved to FlowAuthorizationUtils
			   // assertAdministrator();
			   result = doAuthorizeCommunity(communityID);
		   }

		   // ADMIN
		   else if (cocoon.request.get("submit_edit_admin") || cocoon.request.get("submit_create_admin"))
		   {
			   var groupID = FlowContainerUtils.getCommunityRole(getDSContext(),communityID, "ADMIN");
			   result = doEditGroup(groupID);
		   }
		   else if (cocoon.request.get("submit_delete_admin")) {
			   result = doDeleteCommunityRole(communityID, "ADMIN");
		   }

    }while (true);
}

/**
 * Curate a Community
 * Can only be performed by someone who is able to edit that Community.
 */
function doCurateCommunity(communityID, task) {
    var result;

    do {
		   if (cocoon.request.get("select_curate_group"))
                   {
                      var select_curate_group = cocoon.request.getParameter("select_curate_group");
                      sendPageAndWait("admin/community/curateCommunity",{"communityID":communityID,"select_curate_group":select_curate_group}, result);
                   } else {
                      sendPageAndWait("admin/community/curateCommunity",{"communityID":communityID}, result);
           	   }
		   assertEditCommunity(communityID);
		   result = null;
		   if (cocoon.request.get("submit_return") || cocoon.request.get("submit_metadata") || cocoon.request.get("submit_roles") || cocoon.request.get("submit_curate") )
		   {
			return null;
		   }
		   else if (cocoon.request.get("submit_curate_task"))
		   {
                        result = FlowContainerUtils.processCurateCommunity(getDSContext(), communityID, cocoon.request);
		   }
                   else if (cocoon.request.get("submit_queue_task"))
                    {
                      result = FlowContainerUtils.processQueueCommunity(getDSContext(), communityID, cocoon.request);
                    }

    } while (true);
}

/**
 * Delete a specified community role. Under the current
 * implementation, admin authorizations cannot be deleted once formed,
 * and the default read group is changed to Anonymous instead.
 */
function doDeleteCommunityRole(communityID,role)
{
	// authorization check performed directly by the dspace-api
	// assertAdminCommunity(communityID);
	var groupID = FlowContainerUtils.getCommunityRole(getDSContext(), communityID, role);

	sendPageAndWait("admin/community/deleteRole",{"communityID":communityID,"role":role,"groupID":groupID});
	// authorization check performed directly by the dspace-api
	// assertAdminCommunity(communityID);

	if (cocoon.request.get("submit_confirm"))
	{
		// All other roles use the standard methods
		var result = FlowContainerUtils.processDeleteCommunityRole(getDSContext(),communityID,role,groupID);
		return result;
	}
	return null;
}

/**
 * Curate a DSpace Object, from site-wide Administrator tools
 * (Can only be performed by a DSpace Administrator)
 */
function doCurate() 
{
    var result;
    
    assertAdministrator();

    var identifier;
    var curateTask;
    do 
    {
        if (cocoon.request.get("select_curate_group"))
        {
            var select_curate_group = cocoon.request.getParameter("select_curate_group");
            var identifier = cocoon.request.getParameter("identifier");
            sendPageAndWait("admin/curate/main",{"identifier":identifier,"curate_task":curateTask, "select_curate_group":select_curate_group},result);
        }
        else
        {
             sendPageAndWait("admin/curate/main",{"identifier":identifier,"curate_task":curateTask},result);	   
        } 
       	   
        if (cocoon.request.get("submit_curate_task"))
        {
            result = FlowCurationUtils.processCurateObject(getDSContext(), cocoon.request);
        }
        else if (cocoon.request.get("submit_queue_task"))
        {
            result = FlowCurationUtils.processQueueObject(getDSContext(), cocoon.request);
        }
        
        //if 'identifier' parameter was set in result, pass it back to sendPageAndWait call (so it is prepopulated in Admin UI)
        if (result != null && result.getParameter("identifier")) {
            identifier = result.getParameter("identifier");
        }
        else if (!cocoon.request.get("select_curate_group")) {
             identifier = null;
        }
       
        //if 'curate_task' parameter was set in result, pass it back to sendPageAndWait call (so it is prepopulated in Admin UI)
        if (result != null && result.getParameter("curate_task")) {
            curateTask = result.getParameter("curate_task");
        }
        else if (!cocoon.request.get("select_curate_group")) {
            curateTask = null;
        }  
    }
    while (true);
}
