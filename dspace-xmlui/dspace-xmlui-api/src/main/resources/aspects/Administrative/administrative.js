/*
 * administrative.js
 *
 * Version: $Revision: 1.2 $
 *
 * Date: $Date: 2006/06/02 21:37:32 $
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

importClass(Packages.org.apache.cocoon.components.CocoonComponentManager);

importClass(Packages.org.dspace.authorize.AuthorizeManager);
importClass(Packages.org.dspace.core.Constants);
importClass(Packages.org.dspace.content.Bitstream);
importClass(Packages.org.dspace.content.Bundle);
importClass(Packages.org.dspace.content.Item);
importClass(Packages.org.dspace.content.Collection);
importClass(Packages.org.dspace.content.Community);
importClass(Packages.org.dspace.eperson.EPerson);
importClass(Packages.org.dspace.eperson.Group);

importClass(Packages.org.dspace.app.xmlui.utils.ContextUtil);
importClass(Packages.org.dspace.app.xmlui.aspect.administrative.FlowEPersonUtils);
importClass(Packages.org.dspace.app.xmlui.aspect.administrative.FlowGroupUtils);
importClass(Packages.org.dspace.app.xmlui.aspect.administrative.FlowRegistryUtils);
importClass(Packages.org.dspace.app.xmlui.aspect.administrative.FlowItemUtils);
importClass(Packages.org.dspace.app.xmlui.aspect.administrative.FlowMapperUtils);
importClass(Packages.org.dspace.app.xmlui.aspect.administrative.FlowAuthorizationUtils);
importClass(Packages.org.dspace.app.xmlui.aspect.administrative.FlowContainerUtils);
importClass(Packages.java.lang.System);

/**
 * Simple access method to access the current cocoon object model.
 */
function getObjectModel() 
{
  return CocoonComponentManager.getCurrentEnvironment().getObjectModel();
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
 * Assert that the currently authenticated eperson is able to preform 
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
 * Return weather the currently authenticated eperson can edit the identified item.
 */
function canEditItem(itemID)
{
	var item = Item.find(getDSContext(),itemID);
	
	return item.canEdit()
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
	
	return collection.canEditBoolean()
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
 * Return whether the currently authenticated eperson can edit this community.
 */
function canEditCommunity(communityID)
{
	var community = Community.find(getDSContext(),communityID);
	
	return community.canEditBoolean()
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
 * Assert that the currently authenticated eperson can edit the given group. If they can
 * not then this method will never return.
 */
function assertEditGroup(groupName)
{
    // Check authorizations
	var collectionID = FlowGroupUtils.getCollectionId(groupName);
	if (collectionID >= 0)
	{
		// This group is associated with a collection, check that group's permission
		assertEditCollection(collectionID);
	}
	else
	{
		// Otherwise they need to be a super admin.
		assertAdministrator();
	}
}

/** 
 * Return whether the currently authenticated eperson is an
 * administrator.
 */
function isAdministrator() {
	return AuthorizeManager.isAdmin(getDSContext());
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
	var itemID = cocoon.request.get("itemID");

	assertEditItem(itemID);
	
	doEditItem(itemID);
	
	var item = Item.find(getDSContext(),itemID);
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
    var collectionID = cocoon.request.get("collectionID");
	
	// they can edit the collection they are maping items into.
	assertEditCollection(collectionID);
	
	doMapItems(collectionID);
	
	var collection = Collection.find(getDSContext(),collectionID);
	cocoon.redirectTo(cocoon.request.getContextPath()+"/handle/"+collection.getHandle(),true);
    getDSContext().complete();
	collection = null;
	cocoon.exit();  
}

/**
 * Start creating a new collection.
 */
function startCreateCollection()
{
	var communityID = cocoon.request.get("communityID");
	
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
	var collectionID = cocoon.request.get("collectionID");
	
	assertEditCollection(collectionID);
	
	doEditCollection(collectionID);
	
	// Go back to the collection
	var collection = Collection.find(getDSContext(),collectionID);
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
	
	assertAdministrator();
	
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
	var communityID = cocoon.request.get("communityID");
	
	assertEditCommunity(communityID);
	
	doEditCommunity(communityID);
	
	// Go back to the community
	var community = Community.find(getDSContext(),communityID);
	cocoon.redirectTo(cocoon.request.getContextPath()+"/handle/"+community.getHandle(),true);
	getDSContext().complete();
	community = null;
	cocoon.exit(); 
}







/********************
 * EPerson flows
 ********************/

/**
 * Manage epeople, allow users to create new, edit exiting, 
 * or remove epeople. The user may also search or browse 
 * for epeople.
 *
 * The is typicaly used as an entry point flow.
 */
function doManageEPeople() 
{
	assertAdministrator();
	
    var query = "";
    var page = 0;
    var highlightID = -1;
    var result;
    do {
        
        sendPageAndWait("admin/epeople/main",{"page":page,"query":escape(query),"highlightID":highlightID},result);
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
              	highlightID = result.getParameter("epersonID");
        }
        else if (cocoon.request.get("submit_edit") && cocoon.request.get("epersonID"))
        {
            // Edit an exting eperson
            assertAdministrator();
            
            var epersonID = cocoon.request.get("epersonID");
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
            // Not implemented in the UI, but should be incase someone links to us.
            return;
        }
        
    } while (true) // only way to exit is to hit the submit_back button.
}

/**
 * Add a new eperson, the user is presented with a form to create a new eperson. They will 
 * repeate this form untill the user has supplied a unique email address, first name, and
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
            // Save the new eperson, assuming they have meet all the requirements.
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
 * Edit an exiting eperson, all standard metadata elements are presented for editing. The same
 * validation is required as is in adding a new eperson: unique email, first name, and last name
 */
function doEditEPerson(epersonID)
{
	// FIXME:
	// We can't assert any privleges at this point, the user could be a collection 
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
            // Cancel out and return to where ever the user came from.
            return null;
        } 
        else if (cocoon.request.get("submit_edit_group") && cocoon.request.get("groupID"))
        {
            // Jump to a group that this user is a member.
            assertAdministrator();
            var groupID = cocoon.request.get("groupID");
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
        // The user has confirmed, preform the actualy delete.
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
 * Manage groups, allow users to create new, edit exiting, 
 * or remove groups. The user may also search or browse 
 * for groups.
 *
 * The is typicaly used as an entry point flow.
 */
function doManageGroups()
{
	assertAdministrator();
	
	var highlightID = -1
    var query = "";
    var page = 0;
    var result;
    do {
        

        sendPageAndWait("admin/group/main",{"query":escape(query),"page":page,"highlightID":highlightID},result);
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
            result = doEditGroup(-1);
            
            if (result != null && result.getParameter("groupID"))
           		highlightID = result.getParameter("groupID");
        }
        else if (cocoon.request.get("submit_edit") && cocoon.request.get("groupID"))
        {
            // Edit a specific group
			var groupID = cocoon.request.get("groupID");
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
            // Not implemented in the UI, but should be incase someone links to us.
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
	
	var groupName = ""; // This is only filled in if the user changes the group's name.
	var memberEPeopleIDs = FlowGroupUtils.getEPeopleMembers(getDSContext(),groupID);
	var memberGroupIDs = FlowGroupUtils.getGroupMembers(getDSContext(),groupID);
	
	assertEditGroup(groupName);
	
	var highlightEPersonID;
	var highlightGroupID;
    var type = "";
    var query = "";
    var page = 0;
    var result = null;
    do {
        sendPageAndWait("admin/group/edit",{"groupID":groupID,"groupName":escape(groupName),"memberGroupIDs":memberGroupIDs.join(','),"memberEPeopleIDs":memberEPeopleIDs.join(','),"highlightEPersonID":highlightEPersonID,"highlightGroupID":highlightGroupID,"query":escape(query),"page":page,"type":type},result);
        assertEditGroup(groupName);

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
            // Just return with out saving anything.
            return null;
        }
       	else if (cocoon.request.get("submit_save"))
       	{
       		result = FlowGroupUtils.processSaveGroup(getDSContext(),groupID,escape(groupName),memberEPeopleIDs,memberGroupIDs);
       		
       		// Incase a group was created, update our id.
       		if (result != null && result.getParameter("groupID"))
           		groupID = result.getParameter("groupID");
       	}
        else if (cocoon.request.get("submit_search_epeople") && cocoon.request.get("query"))
        {
            // Preform a new search for epeople.
            query = cocoon.request.get("query");
            page = 0;
            type = "eperson";
        }
        else if (cocoon.request.get("submit_search_groups") && cocoon.request.get("query"))
        {
            // Prefrom a new search for groups.
            query = cocoon.request.get("query");
            page = 0;
            type = "group";
        }
        else if (cocoon.request.get("submit_clear"))
        {
            // Preform a new search for epeople.
            query = "";
            page = 0;
            type = "";
        }
        else if (cocoon.request.get("submit_edit_eperson") && cocoon.request.get("epersonID"))
        {
            // Jump to a specific EPerson
            var epersonID = cocoon.request.get("epersonID");
            result = doEditEPerson(epersonID);
            
            if (result != null)
                result.setContinue(false); 
        }
        else if (cocoon.request.get("submit_edit_group") && cocoon.request.get("groupID"))
        {
            // Jump to another group.
            assertAdministrator();
            
            var newGroupID = cocoon.request.get("groupID");
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
        	
        	if ((match = name.match(/submit_add_eperson_(\d+)/)) != null)
        	{
        		// Add an eperson
        		var epersonID = match[1];
        		memberEPeopleIDs = FlowGroupUtils.addMember(memberEPeopleIDs,epersonID);
        		highlightEPersonID = epersonID;
        	}

        	if ((match = name.match(/submit_add_group_(\d+)/)) != null)
        	{
        		// Add a group
        		var _groupID = match[1];
        		memberGroupIDs = FlowGroupUtils.addMember(memberGroupIDs,_groupID);
        		highlightGroupID = _groupID;
        	}
        	if ((match = name.match(/submit_remove_eperson_(\d+)/)) != null)
        	{
        		// remove an eperson
        		var epersonID = match[1];
				memberEPeopleIDs = FlowGroupUtils.removeMember(memberEPeopleIDs,epersonID);
				highlightEPersonID = epersonID;
        	}
        	if ((match = name.match(/submit_remove_group_(\d+)/)) != null)
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
        // The user has confirmed, actualy delete these groups
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
            result = FlowRegistryUtils.processAddMetadataSchema(getDSContext(), escape(namespace), escape(name));
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
            // Go back to where ever they came from.
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
            result = FlowRegistryUtils.processAddMetadataField(getDSContext(),schemaID,escape(element),escape(qualifier),escape(note));
            highlightID = result.getParameter("fieldID");
        }
        else if (cocoon.request.get("submit_update") && updateID >= 0)
        {
            // Update an exiting field
            var element = cocoon.request.get("updateElement");
            var qualifier = cocoon.request.get("updateQualifier");
            var note = cocoon.request.get("updateNote");
            
            result = FlowRegistryUtils.processEditMetadataField(getDSContext(),schemaID,updateID,escape(element),escape(qualifier),escape(note));
        
            if (result != null && result.getContinue())
            {
                // If the update was successfull then clean the updateID;
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
 * Confirm the deletition of the listed metadata fields.
 */
function doDeleteMetadataFields(fieldIDs) 
{
	assertAdministrator();
	
    sendPageAndWait("admin/metadata-registry/delete-fields",{"fieldIDs":fieldIDs.join(',')})
    assertAdministrator();

    if (cocoon.request.get("submit_confirm"))
    {
        // Actualy delete the fields
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
        // Actualy move the fields
        var schemaID = cocoon.request.get("to_schema");
        var result = FlowRegistryUtils.processMoveMetadataField(getDSContext(), schemaID,fieldIDs);
        return result;
    }
    return null;
}


/**
 * Confirm the deletition of the listed schema
 */
function doDeleteMetadataSchemas(schemaIDs)
{
	assertAdministrator();
	
    sendPageAndWait("admin/metadata-registry/delete-schemas",{"schemaIDs":schemaIDs.join(',')});
    assertAdministrator();

    if (cocoon.request.get("submit_confirm"))
    {
        // Actualy delete the schemas
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
            // Go back to where ever they came from.
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
            // Go back to where ever they came from.
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
 * Confirm the deletition of the listed bitstream formats
 */
function doDeleteBitstreamFormats(formatIDs)
{
	assertAdministrator();
	
    sendPageAndWait("admin/format-registry/delete",{"formatIDs":formatIDs.join(',')});
	assertAdministrator();
    
    if (cocoon.request.get("submit_confirm"))
    {
        // Actualy delete the formats
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
				var itemID = result.getParameter("itemID");
				result = doEditItem(itemID);		
			}
		}
	} while (true)
}


/**
 * Edit a single item. This method allows the user too switch between the
 * three sections of editing an item: status, bitstreams, and metadata.
 */
function doEditItem(itemID)
{	
	// Always go to the status page first
	doEditItemStatus(itemID);	

	do {
	    if (cocoon.request.get("submit_return"))
		{
			// go back to where ever we came from.
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
			doEditItemMetadata(itemID);
		}
		else if (cocoon.request.get("view_item"))
		{
			doViewItem(itemID);
		}
		else
		{
			// This case should never happen but to prevent an infinite loop
			// from occuring let's just return null.
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
			// go back to where ever we came from.
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
		
		if (cocoon.request.get("submit_return") || cocoon.request.get("submit_status") || cocoon.request.get("submit_bitstreams") || cocoon.request.get("submit_metadata") || cocoon.request.get("view_item"))
		{
			// go back to where ever we came from.
			return null;
		}
		else if (cocoon.request.get("submit_delete"))
		{
			assertAdministrator();
			
			// Confirm the item's deletion
			result = doDeleteItem(itemID);
			
			// If the user actualy deleted the item the return back 
			// to the manage items page.
			if (result != null)
				return result;
		}  
		else if (cocoon.request.get("submit_withdraw"))
		{	
			// Confirm the withdrawl of the item
			result = doWithdrawItem(itemID);
		}	
		else if (cocoon.request.get("submit_reinstate"))
		{
			// Confirm the reinstation of the item
			result = doReinstateItem(itemID);
		}
		else if (cocoon.request.get("submit_authorization"))
		{
			// Edit the authorizations for this
			assertAdministrator();
			
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
		sendPageAndWait("admin/item/bitstreams",{"itemID":itemID},result);
		assertEditItem(itemID);
		result = null;
		
		if (cocoon.request.get("submit_return") || cocoon.request.get("submit_status") || cocoon.request.get("submit_bitstreams") || cocoon.request.get("submit_metadata") || cocoon.request.get("view_item"))
		{
			// go back to where ever we came from.
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
		    var bitstreamID = cocoon.request.get("bitstreamID");
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
	} while (true)
}

/**
 * Allow the user to update, remove, and add new metadata to the item.
 */
function doEditItemMetadata(itemID)
{
	assertEditItem(itemID);
	
	var result;
	do {
		sendPageAndWait("admin/item/metadata",{"itemID":itemID},result);
		assertEditItem(itemID);
		result = null;
		
		if (cocoon.request.get("submit_return") || cocoon.request.get("submit_status") || cocoon.request.get("submit_bitstreams") || cocoon.request.get("submit_metadata") || cocoon.request.get("view_item"))
		{
			// go back to where ever we came from.
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
 * Confrim the deletition of this item.
 */
function doDeleteItem(itemID)
{
	assertAdministrator();
	
	sendPageAndWait("admin/item/delete",{"itemID":itemID});
	
	if (cocoon.request.get("submit_confirm"))
	{
		// It's been confirmed, delete the item.
		assertAdministrator();
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
 * Confirm the withdrawl of this item.
 */
function doWithdrawItem(itemID)
{
	assertEditItem(itemID);
	
	sendPageAndWait("admin/item/withdraw",{"itemID":itemID});
	
	if (cocoon.request.get("submit_confirm"))
	{
		// Actualy withdraw the item
		assertEditItem(itemID);
		var result = FlowItemUtils.processWithdrawItem(getDSContext(),itemID);
		return result;
	}
	return null;
}

/**
 * Confirm the reinstatition of this item
 */
function doReinstateItem(itemID)
{
	assertEditItem(itemID);
	sendPageAndWait("admin/item/reinstate",{"itemID":itemID});
	
	if (cocoon.request.get("submit_confirm"))
	{
		// Actualy withdraw the item
		assertEditItem(itemID);
		
		var result = FlowItemUtils.processReinstateItem(getDSContext(),itemID);
		return result;
	}
	return null;
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
    } while (result == null && ! result.getContinue())
    
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
            
            result = FlowItemUtils.processEditBitstream(getDSContext(),itemID,bitstreamID,primary,description,formatID,userFormat);
        }
    } while (result == null && ! result.getContinue())
    
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
        // The user has confirmed,
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
    
    do {
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
 * Search for new items to map into the collection
 */
function doMapItemSearch(collectionID)
{
 	assertAuthorized(Constants.COLLECTION,collectionID,Constants.ADD);
    var result;
    var query = cocoon.request.get("query");


    sendPageAndWait("admin/mapper/search",{"collectionID":collectionID,"query":escape(query)},result);
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
    var result
    
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
	assertAdministrator();
	
    var result = null;
    var query = "";
    
    do {
        sendPageAndWait("admin/authorize/main",{"query":escape(query)},result);
        
        assertAdministrator();
        
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
            	var itemID = result.getParameter("itemID");
                result = doAuthorizeItem(itemID);
            }
            else if (result.getParameter("type") == Constants.COLLECTION && result.getParameter("collectionID"))
            {
            	var collectionID = result.getParameter("collectionID");
                result = doAuthorizeCollection(collectionID);
            }
            else if (result.getParameter("type") == Constants.COMMUNITY && result.getParameter("communityID"))
            {
            	var communityID = result.getParameter("communityID");
                result = doAuthorizeCommunity(communityID);
            }
        }
        // Clicking to edit a collection's authorizations
        else if (cocoon.request.get("submit_edit") && cocoon.request.get("collection_id"))
        {
            var collectionID = cocoon.request.get("collection_id");
            result = doAuthorizeCollection(collectionID);
        }
        // Clicking to edit a community's authorizations
        else if (cocoon.request.get("submit_edit") && cocoon.request.get("community_id"))
        {
            var communityID = cocoon.request.get("community_id");
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
 * Editing authorization policies for collections and communities, collectively refered to as "containers"
 */
function doAuthorizeContainer(containerType, containerID)
{
	assertAdministrator();
	
    var result;    
    var highlightID;
    
    do {
        sendPageAndWait("admin/authorize/container",{"containerType":containerType,"containerID":containerID,"highlightID":highlightID},result);
        assertAdministrator();
        result = null;
        
        // Cancel out the operation
        if (cocoon.request.get("submit_return")) {
            return null;
        }
        else if (cocoon.request.get("submit_add")) {
            // Create a new policy
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
            var groupID = cocoon.request.get("group_id");
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
	assertAdministrator();
	
    var result;
    var highlightID;
        
    do {
        sendPageAndWait("admin/authorize/item",{"itemID":itemID,"highlightID":highlightID},result);
        assertAdministrator();
        result = null;
        
        var bundleID = extractSubmitSuffix("submit_add_bundle_");
        var bitstreamID = extractSubmitSuffix("submit_add_bitstream_");
        
        // Cancel out the operation
        if (cocoon.request.get("submit_return")) {
            return null;
        }
        else if (bundleID) 
        {
            // Create a new policy for a bundle
            result = doEditPolicy(Constants.BUNDLE, bundleID,-1);
            if (result != null && result.getParameter("policyID"))
            	highlightID = result.getParameter("policyID");
        } 
        else if (bitstreamID) 
        {
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
            var objectID = cocoon.request.get("object_id");
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
            var groupID = cocoon.request.get("group_id");
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
    
    do {
        sendPageAndWait("admin/authorize/advanced",{},result);
        assertAdministrator();
        result = null;
        
        // For all of the selected groups...
        var groupIDs = cocoon.request.getParameterValues("group_id");
        // ...grant the ability to perform the following action...
        var actionID = cocoon.request.get("action_id");
        // ...for all following object types...
        var resourceID = cocoon.request.get("resource_id");
        // ...across the following collections.
        var collectionIDs = cocoon.request.getParameterValues("collection_id");
        
        if (cocoon.request.get("submit_return"))
        {
            return null;
        }
        else if (cocoon.request.get("submit_add"))
        {
            result = FlowAuthorizationUtils.processAdvancedPolicyAdd(getDSContext(),groupIDs,actionID,resourceID,collectionIDs);
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
	assertAdministrator();
	
    var result;
    var query= "-1";
    var groupID;
    var actionID;
    var page = 0;

    do {
    	/* The page recieves parameters for the type and ID of the DSpace object that the policy is assciated with, the
    	 * policy ID, the group search query (if a search was performed), the ID of the currenly associated group, the
    	 * current action and the currently viewed page (if a search returned more than one page of results) */ 
        sendPageAndWait("admin/authorize/edit",{"objectType":objectType,"objectID":objectID,"policyID":policyID,"query":escape(query),"groupID":groupID,"actionID":actionID,"page":page},result);
        assertAdministrator();
        result = null;
        
        // Figure out which button was pressed on the group search results page
        var names = cocoon.request.getParameterNames();
        while (names.hasMoreElements())
        {
			var name = names.nextElement();
			var match = null;
			
	        if ((match = name.match(/submit_group_id_(\d+)/)) != null)
			{
	        	groupID = match[1];
			}
			
			if (cocoon.request.get("action_id"))
        		actionID = cocoon.request.get("action_id");
        }
        
        if (cocoon.request.get("page")) {
        	page = cocoon.request.get("page");
        }
        
        // perform a search to set a group
        if (cocoon.request.get("submit_search_groups")) {
        	query = cocoon.request.get("query");
        	if (cocoon.request.get("action_id"))
        		actionID = cocoon.request.get("action_id");
        	page = 0;
        } 
        else if (cocoon.request.get("submit_save"))
        {
            groupID = cocoon.request.get("group_id");
            if (groupID == null) groupID = -1;
            
            actionID = cocoon.request.get("action_id");
            if (actionID == null) actionID = -1;
            
            result = FlowAuthorizationUtils.processEditPolicy(getDSContext(),objectType,objectID,policyID,groupID,actionID);                                    
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            return null;
        }
        // TODO; make sure to handle the odd case of the target group getting deleted from inside the authorization flow
        else if (cocoon.request.get("submit_edit_group") && cocoon.request.get("group_id"))
        {
            var editGroupID = cocoon.request.get("group_id");
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
	assertAdministrator();
    sendPageAndWait("admin/authorize/delete",{"policyIDs":policyIDs.join(',')});
    
    if (cocoon.request.get("submit_confirm"))
    {
        // The user has confirmed, actualy delete these policies
        assertAdministrator();
        
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
	    
		if (cocoon.request.get("submit_return"))
		{
			// go back to where ever we came from.
			return null;
		}
		else if (cocoon.request.get("submit_metadata"))
		{
			// go edit collection metadata
			doEditCollectionMetadata(collectionID)
		}
		else if (cocoon.request.get("submit_roles"))
		{
			// go assign colection roles
			doAssignCollectionRoles(collectionID);
		}
		else
		{
			// This case should never happen but to prevent an infinite loop
			// from occuring let's just return null.
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
		
		if (cocoon.request.get("submit_return") || cocoon.request.get("submit_metadata") || cocoon.request.get("submit_roles"))
		{
			// return to the editCollection function which will determine where to go next.
			return null;	
		}
		else if (cocoon.request.get("submit_save")) 
		{
			// Save updates
			result = FlowContainerUtils.processEditCollection(getDSContext(), collectionID, false, cocoon.request);
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
			result = doEditItem(itemID);
		}
		else if (cocoon.request.get("submit_delete_template")) 
		{
			// Delete the item's template
			assertAdministrator();
			
			var itemID = FlowContainerUtils.getTemplateItemID(getDSContext(), collectionID);
			result = doDeleteItem(itemID);
		}
				
	}while (true);
}

/**
 * Edit the set roles for a collection: admin, workflows, submitters, and default read.
 */
function doAssignCollectionRoles(collectionID) 
{
	assertEditCollection(collectionID);
	
	var result;
	
	do {
		sendPageAndWait("admin/collection/assignRoles",{"collectionID":collectionID},result);
		assertEditCollection(collectionID);
		result = null;
		
		
		if (cocoon.request.get("submit_return") || cocoon.request.get("submit_metadata") || cocoon.request.get("submit_roles"))
		{
			// return to the editCollection function which will determine where to go next.
			return null;	
		}
		
		else if (cocoon.request.get("submit_authorizations")) 
		{
			// general authorizations
			assertAdministrator();
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
		
		// WORKFLOW STEPS 1-3
		else if (cocoon.request.get("submit_edit_wf_step1") || cocoon.request.get("submit_create_wf_step1")) 
		{
			assertEditCollection(collectionID);
			var groupID = FlowContainerUtils.getCollectionRole(getDSContext(),collectionID, "WF_STEP1");
			result = doEditGroup(groupID);
		}
		else if (cocoon.request.get("submit_delete_wf_step1")) 
		{
			assertAdministrator();
			result = doDeleteCollectionRole(collectionID, "WF_STEP1");
		}
		
		else if (cocoon.request.get("submit_edit_wf_step2") || cocoon.request.get("submit_create_wf_step2")) 
		{
			assertEditCollection(collectionID);
			var groupID = FlowContainerUtils.getCollectionRole(getDSContext(),collectionID, "WF_STEP2");
			result = doEditGroup(groupID);
		}
		else if (cocoon.request.get("submit_delete_wf_step2")) 
		{
			assertAdministrator();
			result = doDeleteCollectionRole(collectionID, "WF_STEP2");
		}
		
		else if (cocoon.request.get("submit_edit_wf_step3") || cocoon.request.get("submit_create_wf_step3")) 
		{
			assertEditCollection(collectionID);
			var groupID = FlowContainerUtils.getCollectionRole(getDSContext(),collectionID, "WF_STEP3");
			result = doEditGroup(groupID);
		}
		else if (cocoon.request.get("submit_delete_wf_step3")) 
		{
			assertAdministrator();
			result = doDeleteCollectionRole(collectionID, "WF_STEP3");
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
		    assertAdministrator();
			result = doDeleteCollectionRole(collectionID, "SUBMIT");
		}
		
		// DEFAULT_READ
		else if (cocoon.request.get("submit_create_default_read"))
		{
			assertAdministrator();
			
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
			assertAdministrator();
			result = doDeleteCollectionRole(collectionID, "DEFAULT_READ");
		}
			
	}while(true);
}

/**
 * Delete a specified collection role. Under the current implementation, the only roles this applies to
 * directly are the workflow steps. Admin and submitter authorizations cannot be deleted once formed, and 
 * the default read group is changed to Anonymous instead. 
 */
function doDeleteCollectionRole(collectionID,role) 
{
	assertAdministrator();
	
	var groupID;
	
	if (role == "DEFAULT_READ") {
		groupID = FlowContainerUtils.getCollectionDefaultRead(getDSContext(), collectionID);
	}
	else {
		groupID = FlowContainerUtils.getCollectionRole(getDSContext(),collectionID, role);
	}
	
	sendPageAndWait("admin/collection/deleteRole",{"collectionID":collectionID,"role":role,"groupID":groupID});
 	assertAdministrator();
	
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
	assertAdministrator();	
	
	sendPageAndWait("admin/collection/delete",{"collectionID":collectionID});
    assertAdministrator();
	
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
				collectionID = result.getParameter("collectionID");	
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
	assertAdministrator();
	
	var result;
	var newCommunityID;
	// If we are not passed a communityID from the flow, we assume that is passed in from the sitemap
	if (parentCommunityID == null && cocoon.request.getParameter("communityID") != null) 
	{
		parentCommunityID = cocoon.request.getParameter("communityID");
	}
	else if (parentCommunityID == null) 
	{
		parentCommunityID = -1;
	}
	
	do {
		sendPageAndWait("admin/community/createCommunity",{"communityID":parentCommunityID},result);
		assertAdministrator();
		result=null;
		
		if (cocoon.request.get("submit_save")) {
			// create the community, passing back its ID
			result = FlowContainerUtils.processCreateCommunity(getDSContext(), parentCommunityID, cocoon.request);
			
			// send the user to the newly created community
			if (result.getContinue() && result.getParameter("communityID")) {
				newCommunityID = result.getParameter("communityID");	
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
 * Edit metadata of a community; presenting the user with a form of standard community metadata,
 * an option add/remove a logo and a link to the authorizations screen 
 */
function doEditCommunity(communityID)
{
	assertEditCommunity(communityID);
	var result;
	
	do {
		sendPageAndWait("admin/community/editMetadata",{"communityID":communityID},result);
		assertEditCommunity(communityID);
		result=null;
		
		if (cocoon.request.get("submit_return"))
		{
			return null;	
		}
		if (cocoon.request.get("submit_save")) 
		{
			result = FlowContainerUtils.processEditCommunity(getDSContext(), communityID, false, cocoon.request);
		}
		else if (cocoon.request.get("submit_delete")) 
		{
			assertAdministrator();
			result = doDeleteCommunity(communityID);
		}
		else if (cocoon.request.get("submit_delete_logo")) 
		{
			result = FlowContainerUtils.processEditCommunity(getDSContext(), communityID, true, cocoon.request);
		}
		if (cocoon.request.get("submit_authorizations")) 
		{
			assertAdministrator();
			result = doAuthorizeCommunity(communityID);
		}
	}while (true);
}

/**
 * Delete an entire community, asking for a confirmation first
 */
function doDeleteCommunity(communityID) {
	assertAdministrator();
	
	sendPageAndWait("admin/community/delete",{"communityID":communityID});
	assertAdministrator();
	
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


