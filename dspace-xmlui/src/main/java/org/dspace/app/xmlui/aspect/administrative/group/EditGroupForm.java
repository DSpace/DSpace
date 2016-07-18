/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.group;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.xmlui.aspect.administrative.FlowGroupUtils;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;

import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * Present the user with the group's current state. The user may select to 
 * change the group's name, OR search for new epeople / groups to add, OR select 
 * current group members for removal.
 * 
 * @author Alexey Maslov
 * @author Scott Phillips
 * @author Oriol Olivé - DS-3205
 */
public class EditGroupForm extends AbstractDSpaceTransformer   
{
	/** Language Strings */
	private static final Message T_dspace_home =
		message("xmlui.general.dspace_home");

	private static final Message T_group_trail =
		message("xmlui.administrative.group.general.group_trail");
	
	private static final Message T_title = 
		message("xmlui.administrative.group.EditGroupForm.title");

	private static final Message T_trail = 
		message("xmlui.administrative.group.EditGroupForm.trail");
	
	private static final Message T_main_head = 
		message("xmlui.administrative.group.EditGroupForm.main_head");
	
	private static final Message T_collection_para = 
		message("xmlui.administrative.group.EditGroupForm.collection_para");

    private static final Message T_community_para = 
        message("xmlui.administrative.group.EditGroupForm.community_para");

	private static final Message T_label_name = 
		message("xmlui.administrative.group.EditGroupForm.label_name");
	
	private static final Message T_label_instructions = 
		message("xmlui.administrative.group.EditGroupForm.label_instructions");

	private static final Message T_label_search = 
		message("xmlui.administrative.group.EditGroupForm.label_search");

	private static final Message T_submit_search_people = 
		message("xmlui.administrative.group.EditGroupForm.submit_search_people");

	private static final Message T_submit_search_groups = 
		message("xmlui.administrative.group.EditGroupForm.submit_search_groups");

	private static final Message T_no_results =
		message("xmlui.administrative.group.EditGroupForm.no_results");
	
	private static final Message T_main_head_new =
		message("xmlui.administrative.group.EditGroupForm.main_head_new");
	
	private static final Message T_submit_clear =
		message("xmlui.administrative.group.EditGroupForm.submit_clear");
	
	private static final Message T_submit_save =
		message("xmlui.general.save");
	
	private static final Message T_submit_cancel =
		message("xmlui.general.cancel");
	
	private static final Message T_member =
		message("xmlui.administrative.group.EditGroupForm.member");
	
	private static final Message T_cycle =
		message("xmlui.administrative.group.EditGroupForm.cycle");
	
	private static final Message T_pending =
		message("xmlui.administrative.group.EditGroupForm.pending");
	
	private static final Message T_pending_warn =
		message("xmlui.administrative.group.EditGroupForm.pending_warn");
	
	private static final Message T_submit_add =
		message("xmlui.administrative.group.EditGroupForm.submit_add");
	
	private static final Message T_submit_remove =
		message("xmlui.administrative.group.EditGroupForm.submit_remove");
	
	
	// EPeople Search
	private static final Message T_epeople_column1 =
		message("xmlui.administrative.group.EditGroupForm.epeople_column1");
	
	private static final Message T_epeople_column2 =
		message("xmlui.administrative.group.EditGroupForm.epeople_column2");

	private static final Message T_epeople_column3 =
		message("xmlui.administrative.group.EditGroupForm.epeople_column3");

	private static final Message T_epeople_column4 =
		message("xmlui.administrative.group.EditGroupForm.epeople_column4");
	
	// Group Search
	private static final Message T_groups_column1 =
		message("xmlui.administrative.group.EditGroupForm.groups_column1");
	
	private static final Message T_groups_column2 =
		message("xmlui.administrative.group.EditGroupForm.groups_column2");

	private static final Message T_groups_column3 =
		message("xmlui.administrative.group.EditGroupForm.groups_column3");

	private static final Message T_groups_column4 =
		message("xmlui.administrative.group.EditGroupForm.groups_column4");

	private static final Message T_groups_column5 =
		message("xmlui.administrative.group.EditGroupForm.groups_column5");
	
	private static final Message T_groups_collection_link = 
		message("xmlui.administrative.group.ManageGroupsMain.groups_collection_link");
	
	// Members
	private static final Message T_members_head =
		message("xmlui.administrative.group.EditGroupForm.members_head");
	
	private static final Message T_members_column1 =
		message("xmlui.administrative.group.EditGroupForm.members_column1");
	
	private static final Message T_members_column2 =
		message("xmlui.administrative.group.EditGroupForm.members_column2");

	private static final Message T_members_column3 =
		message("xmlui.administrative.group.EditGroupForm.members_column3");

	private static final Message T_members_column4 =
		message("xmlui.administrative.group.EditGroupForm.members_column4");

	private static final Message T_members_group_name =
		message("xmlui.administrative.group.EditGroupForm.members_group_name");	
	
	private static final Message T_members_pending = 
		message("xmlui.administrative.group.EditGroupForm.members_pending");
	
	private static final Message T_members_none =
		message("xmlui.administrative.group.EditGroupForm.members_none");
	
	
	// How many results to show on a page.
	private static final int RESULTS_PER_PAGE = 5;
	
	/** The maximum size of a collection name allowed */
	private static final int MAX_COLLECTION_NAME = 25;

	protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
	protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
	protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
	protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
	protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();

	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/groups",T_group_trail);
        pageMeta.addTrail().addContent(T_trail);
    }
	
	
	public void addBody(Body body) throws WingException, SQLException 
	{		
		// Find the group in question
		UUID groupID = null;
		if (StringUtils.isNotBlank(parameters.getParameter("groupID", null)))
		{
			groupID = UUID.fromString(parameters.getParameter("groupID",null));
		}
		String currentName = decodeFromURL(parameters.getParameter("groupName",null));
		if (currentName == null || currentName.length() == 0)
        {
            currentName = FlowGroupUtils.getName(context, groupID);
        }
		
		Group group = null;
		if (groupID != null)
        {
            group = groupService.find(context, groupID);
        }

		// Find the collection or community if applicable
		Collection collection = null;
		Community community = null;
		if (group != null)
		{
			UUID collectionID = FlowGroupUtils.getCollectionId(context, group.getName());
			if (collectionID != null)
            {
                collection = collectionService.find(context, collectionID);
            }
			else
			{
			    UUID communityID = FlowGroupUtils.getCommunityId(context, group.getName());
			    if (communityID != null)
                {
                    community = communityService.find(context, communityID);
                }
		    }
		}
		
		// Get list of member groups
		String memberGroupIDsString = parameters.getParameter("memberGroupIDs",null);
		List<UUID> memberGroupIDs = new ArrayList<UUID>();
		if (memberGroupIDsString != null)
		{
			for (String id : memberGroupIDsString.split(","))
			{
				if (id.length() > 0)
                {
                    memberGroupIDs.add(UUID.fromString(id));
                }
			}
		}
	    
		// Get list of member epeople
		String memberEPeopleIDsString = parameters.getParameter("memberEPeopleIDs",null);
		List<UUID> memberEPeopleIDs = new ArrayList<UUID>();
		if (memberEPeopleIDsString != null)
		{
			for (String id : memberEPeopleIDsString.split(","))
            {
				if (id.length() > 0)
                {
					memberEPeopleIDs.add(UUID.fromString(id));
                }
            }
		}
		
		// Get highlight parameters
		String highlightEPersonID = parameters.getParameter("highlightEPersonID", null);
		String highlightGroupID = parameters.getParameter("highlightGroupID",null);
		
		// Get search parameters
	    String query = decodeFromURL(parameters.getParameter("query",null));
	    int page     = parameters.getParameterAsInteger("page",0);
	    String type  = parameters.getParameter("type",null);
	    
	    // Get any errors
	    String errorString = parameters.getParameter("errors",null);
		List<String> errors = new ArrayList<String>();
		if (errorString != null)
        {
			for (String error : errorString.split(","))
            {
				errors.add(error);
            }
        }
	    
	    //DIVISION: group-edit
	    Division main = body.addInteractiveDivision("group-edit",contextPath+"/admin/groups",Division.METHOD_POST,"primary administrative groups");
	    if (group == null)
        {
            main.setHead(T_main_head_new);
        }
	    else
        {
            main.setHead(T_main_head.parameterize(group.getName(), groupID));
        }
	 
	    
	    if(collection != null)
	    {
	    	Para para = main.addPara();
	    	para.addContent(T_collection_para);
	    	para.addXref(contextPath + "/handle/" + collection.getHandle(), collectionService.getMetadata(collection, "name"));
	    }
	    else if(community != null)
        {
            Para para = main.addPara();
            para.addContent(T_community_para);
            para.addXref(contextPath + "/handle/" + community.getHandle(), communityService.getMetadata(community, "name"));
        }
	   
	    // DIVISION: group-actions
	    Division actions = main.addDivision("group-edit-actions");
	    Para groupName = actions.addPara();
        groupName.addContent(T_label_name);
        Text groupText = groupName.addText("group_name");
        groupText.setValue(currentName);
        if(collection != null || community != null)
        {
        	// If this group is associated with a collection or community then it is special,
        	// thus they shouldn't be able to update it.
        	groupText.setDisabled();
        	groupText.setHelp(T_label_instructions);
        }
        else if (errors.contains("group_name") || errors.contains("group_name_duplicate"))
        {
            groupText.addError("");
        }
        

        Para searchBoxes = actions.addPara();
        searchBoxes.addContent(T_label_search);
        Text queryField = searchBoxes.addText("query");
        queryField.setValue(query);
        queryField.setSize(15);
        searchBoxes.addButton("submit_search_epeople").setValue(T_submit_search_people);
        searchBoxes.addButton("submit_search_groups").setValue(T_submit_search_groups);
		
        
        if (query != null)
        {
	        if ("eperson".equals(type))
	        {
	        	searchBoxes.addButton("submit_clear").setValue(T_submit_clear);
	        	addEPeopleSearch(main,query,page,group,memberEPeopleIDs);
	        }
	        else if ("group".equals(type))
	        {
	        	searchBoxes.addButton("submit_clear").setValue(T_submit_clear);
        		addGroupSearch(main,group,query,page,group,memberGroupIDs);
	        }
        }
                     
        boolean changes = false;
        if (group != null)
        {
            changes = addMemberList(main, group, memberGroupIDs, memberEPeopleIDs, highlightEPersonID, highlightGroupID);
        }
        
               
    	Para buttons = main.addPara();
    	buttons.addButton("submit_save").setValue(T_submit_save);
    	buttons.addButton("submit_cancel").setValue(T_submit_cancel);
       
    	if (changes)
        {
            main.addPara().addHighlight("warn").addContent(T_pending_warn);
        }
    	
        
        main.addHidden("administrative-continue").setValue(knot.getId());
	}
	
	
	
	/**
	 * Search for epeople to add to this group.
	 */
	private void addEPeopleSearch(Division div, String query, int page, Group group, List<UUID> memberEPeopleIDs) throws SQLException, WingException
	{
		int resultCount = ePersonService.searchResultCount(context, query);
        java.util.List<EPerson> epeople = ePersonService.search(context, query, page*RESULTS_PER_PAGE, RESULTS_PER_PAGE);
		
		Division results = div.addDivision("results");
		
		if (resultCount > RESULTS_PER_PAGE) 
		{
        	// If there are enough results then paginate the results
        	String baseURL = contextPath +"/admin/groups?administrative-continue="+knot.getId();
        	int firstIndex = page*RESULTS_PER_PAGE+1; 
        	int lastIndex = page*RESULTS_PER_PAGE + epeople.size();
       
        	String nextURL = null, prevURL = null;
        	if (page < (resultCount / RESULTS_PER_PAGE))
            {
                nextURL = baseURL + "&page=" + (page + 1);
            }
        	if (page > 0)
            {
                prevURL = baseURL + "&page=" + (page - 1);
            }
        	
			results.setSimplePagination(resultCount,firstIndex,lastIndex,prevURL, nextURL);
		}
		
		/* Set up a table with search results (if there are any). */
		Table table = results.addTable("group-edit-search-eperson",epeople.size() + 1, 1);
		Row header = table.addRow(Row.ROLE_HEADER);
		header.addCell().addContent(T_epeople_column1);
		header.addCell().addContent(T_epeople_column2);
		header.addCell().addContent(T_epeople_column3);
		header.addCell().addContent(T_epeople_column4);

		for (EPerson person : epeople)
		{
			String epersonID = String.valueOf(person.getID());
			String fullName = person.getFullName();
			String email = person.getEmail();
			String url = contextPath+"/admin/epeople?administrative-continue="+knot.getId()+"&submit_edit_eperson&epersonID="+epersonID;
			
			
			
			Row personData = table.addRow();

			personData.addCell().addContent(person.getID().toString());
			personData.addCell().addXref(url, fullName);
			personData.addCell().addXref(url, email);
			
			// check if they are already a member of the group
			if (memberEPeopleIDs.contains(person.getID()))
			{
				// Check if they really members or just pending members
				if (group != null && groupService.isDirectMember(group, person))
                {
                    personData.addCellContent(T_member);
                }
				else
                {
                    personData.addCell().addHighlight("warn").addContent(T_pending);
                }
			}
			else
			{
				personData.addCell().addButton("submit_add_eperson_"+epersonID).setValue(T_submit_add);
			}
		}

		if (epeople.size() <= 0) {
			table.addRow().addCell(1, 4).addContent(T_no_results);
		}
	}
	
	
	
	
	
	/**
	 * Search for groups to add to this group.
	 */
	private void addGroupSearch(Division div, Group sourceGroup, String query, int page, Group parent, List<UUID> memberGroupIDs) throws WingException, SQLException
	{
		int resultCount = groupService.searchResultCount(context, query);
        java.util.List<Group> groups = groupService.search(context, query, page*RESULTS_PER_PAGE, RESULTS_PER_PAGE);
		
		Division results = div.addDivision("results");
		
		if (resultCount > RESULTS_PER_PAGE) 
		{
        	// If there are enough results then paginate the results
        	String baseURL = contextPath +"/admin/groups?administrative-continue="+knot.getId();
        	int firstIndex = page*RESULTS_PER_PAGE+1; 
        	int lastIndex = page*RESULTS_PER_PAGE + groups.size();
       
        	String nextURL = null, prevURL = null;
        	if (page < (resultCount / RESULTS_PER_PAGE))
            {
                nextURL = baseURL + "&page=" + (page + 1);
            }
        	if (page > 0)
            {
                prevURL = baseURL + "&page=" + (page - 1);
            }
        	
			results.setSimplePagination(resultCount,firstIndex,lastIndex,prevURL, nextURL);
		}

// DS-3205 fix		
//        Table table = results.addTable("roup-edit-search-group",groups.size() + 1, 1);
        Table table = results.addTable("group-edit-search-group",groups.size() + 1, 1);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_groups_column1);
        header.addCell().addContent(T_groups_column2);
        header.addCell().addContent(T_groups_column3);
        header.addCell().addContent(T_groups_column4);
        header.addCell().addContent(T_groups_column5);  
        
        for (Group group : groups)
        {
        	String groupID = String.valueOf(group.getID());
        	String name = group.getName();
        	String url = contextPath+"/admin/groups?administrative-continue="+knot.getId()+"&submit_edit_group&groupID="+groupID;
        	int memberCount = group.getMembers().size() + group.getMemberGroups().size();
        	
        	Row row = table.addRow();
	        	
        	row.addCell().addContent(groupID);
        	if (authorizeService.isAdmin(context))
        		// Only administrators can edit other groups.
            {
                row.addCell().addXref(url, name);
            }
        	else
            {
                row.addCell().addContent(name);
            }
        	
        	
        	
        	row.addCell().addContent(memberCount == 0 ? "-" : String.valueOf(memberCount));
        	
        	Cell cell = row.addCell();
        	if (FlowGroupUtils.getCollectionId(context, group.getName()) != null)
        	{
        		Collection collection = collectionService.find(context, FlowGroupUtils.getCollectionId(context, group.getName()) );
        		if (collection != null)
        		{
	        		String collectionName = collectionService.getMetadata(collection, "name");
	        		
	        		if (collectionName == null)
                    {
                        collectionName = "";
                    }
	        		else if (collectionName.length() > MAX_COLLECTION_NAME)
                    {
                        collectionName = collectionName.substring(0, MAX_COLLECTION_NAME - 3) + "...";
                    }
	        		
	        		cell.addContent(collectionName+" ");
	        		
	        		Highlight highlight = cell.addHighlight("fade");
	        		highlight.addContent("[");
	        		highlight.addXref(contextPath+"/handle/"+collection.getHandle(), T_groups_collection_link);
	        		highlight.addContent("]");
        		}
        	}
        	
        	
        	// Check if the group is already a member or would create a cycle.
        	if (memberGroupIDs.contains(group.getID()))
        	{
        		// Check if they really members or just pending members
				if (parent != null && groupService.isMember(parent, group))
                {
                    row.addCellContent(T_member);
                }
				else
                {
                    row.addCell().addHighlight("warn").addContent(T_pending);
                }
        	}
        	else if (isDescendant(sourceGroup, group, memberGroupIDs)) 
        	{
        		row.addCellContent(T_cycle);
	        }
	        else 
	        {
	        	row.addCell().addButton("submit_add_group_"+groupID).setValue(T_submit_add);
	        }
            
        }
        if (groups.size() <= 0) {
			table.addRow().addCell(1, 4).addContent(T_no_results);
		}
	}
	
	
	
	
	
	
	/**
	 * Method to extensively check whether the first group has the second group as a distant
	 * parent. This is used to avoid creating cycles like A->B, B->C, C->D, D->A which leads 
	 * all the groups involved to essentially include themselves.  
	 */
	private boolean isDescendant(Group descendant, Group ancestor, List<UUID> memberGroupIDs) throws SQLException
	{
		Queue<Group> toVisit = new LinkedList<Group>();
		Group currentGroup;
		
		toVisit.offer(ancestor);
		
		// Initialize by adding a list of our current list of group members.
		for (UUID groupid : memberGroupIDs)
		{
			Group member = groupService.find(context,groupid);
			toVisit.offer(member);
		}
		
		while (!toVisit.isEmpty()) {
			// 1. Grab a group from the queue
			currentGroup = toVisit.poll();
			
			// 2. See if it's the descendant we're looking for
			if (currentGroup.equals(descendant)) {
                return true;
            }
			
			// 3. If not, add that group's children to the queue
			for (Group nextBatch : currentGroup.getMemberGroups()) {
				toVisit.offer(nextBatch);
			}
		}
		return false;
	}
	
	
	
	
	
	
	/**
	 * Add a table with all the current group's members to the specified division.
	 * @throws SQLException 
	 */
	private boolean addMemberList(Division div, Group parent, List<UUID> memberGroupIDs, List<UUID> memberEPeopleIDs, String highlightEPersonID, String highlightGroupID) throws WingException, SQLException
	{
		// Flag to remember if there are any pending changes.
        boolean changes = false;
        
        Division members = div.addDivision("group-edit-members");
        members.setHead(T_members_head);
        
        Table table = members.addTable("group-edit-members-table",memberGroupIDs.size() + memberEPeopleIDs.size() + 1, 4);
        
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_members_column1);
        header.addCell().addContent(T_members_column2);
        header.addCell().addContent(T_members_column3);
        header.addCell().addContent(T_members_column4);
        
        // get all group members, pend or actual
        @SuppressWarnings("unchecked") // the cast is correct
        List<UUID> allMemberGroupIDs = new ArrayList<UUID>(memberGroupIDs);
        for (Group group : parent.getMemberGroups())
        {
        	if (!allMemberGroupIDs.contains(group.getID()))
            {
                allMemberGroupIDs.add(group.getID());
            }
        }
        // Sort them to a consistent ordering
        Collections.sort(allMemberGroupIDs);
        
        // Loop through all group ids and display them.
        for (UUID groupID : allMemberGroupIDs)
        {
			Group group = groupService.find(context,groupID);
        	boolean highlight = (group.getID().toString().equals(highlightGroupID));
        	boolean pendingAddition = !groupService.isMember(parent, group);
        	boolean pendingRemoval = !memberGroupIDs.contains(groupID);
        	addMemberRow(table, group, highlight,pendingAddition,pendingRemoval);   
        	
        	if (pendingAddition || pendingRemoval)
            {
                changes = true;
            }
        }
        
        
        // get all members, pend or actual
        @SuppressWarnings("unchecked") // the cast is correct
        List<UUID> allMemberEPeopleIDs = new ArrayList<UUID>(memberEPeopleIDs);
        for (EPerson eperson : parent.getMembers())
        {
        	if (!allMemberEPeopleIDs.contains(eperson.getID()))
            {
                allMemberEPeopleIDs.add(eperson.getID());
            }
        }
        // Sort them to a consistent ordering
        Collections.sort(allMemberEPeopleIDs);
        
        for (UUID epersonID : allMemberEPeopleIDs)
        {
        	EPerson eperson = ePersonService.find(context, epersonID);
        	boolean highlight = (eperson.getID().toString().equals(highlightEPersonID));
        	boolean pendingAddition = !groupService.isDirectMember(parent, eperson);
        	boolean pendingRemoval = !memberEPeopleIDs.contains(epersonID);
        	addMemberRow(table,eperson,highlight,pendingAddition,pendingRemoval);
        	
        	if (pendingAddition || pendingRemoval)
            {
                changes = true;
            }
        }
    
        if (allMemberGroupIDs.size() <= 0 && allMemberEPeopleIDs.size() <= 0)
        {
        	table.addRow().addCell(1, 4).addContent(T_members_none);
        }
        
        return changes;
	}
	
	
	/**
	 * Add a single member row for groups.
	 * 
	 * @param table The table to add the row too.
	 * @param group The group being displayed in this row.
	 * @param highlight Should the row be highlighted.
	 * @param pendingAddition Is this group pending addition
	 * @param pendingRemoval Is this group pending removal
	 */
	private void addMemberRow(Table table,Group group, boolean highlight, boolean pendingAddition, boolean pendingRemoval) throws WingException, SQLException
	{
    	String name = group.getName();
    	String url = contextPath+"/admin/groups?administrative-continue="+knot.getId()+"&submit_edit_group&groupID="+group.getID();
   
    	Row groupData = table.addRow(null,null,highlight ? "highlight" : null);
    	
    	groupData.addCell().addHighlight("bold").addContent(group.getID().toString());
    	
    	// Mark if this member is pending or not.
    	Cell nameCell = groupData.addCell();
    	if (authorizeService.isAdmin(context))
        {
            nameCell.addHighlight("bold").addXref(url, T_members_group_name.parameterize(name));
        }
    	else
        {
            nameCell.addHighlight("bold").addContent(T_members_group_name.parameterize(name));
        }
    	
    	if (pendingAddition)
    	{
    		nameCell.addContent(" ");
    		nameCell.addHighlight("warn").addContent(T_members_pending);
    	}
    	
    	groupData.addCell().addContent("-");
    	
    	if (pendingRemoval)
        {
            groupData.addCell().addHighlight("warn").addContent(T_pending);
        }
    	else
        {
            groupData.addCell().addButton("submit_remove_group_" + group.getID()).setValue(T_submit_remove);
        }
	}
	
	/**
	 * Add a single member row for epeople.
	 * 
	 * @param table The table to add a row too.
	 * @param eperson The eperson being displayed
	 * @param highlight Should this eperson be highlighted?
	 * @param pendingAddition Is this eperson pending addition?
	 * @param pendingRemoval Is this eperson pending removal?
	 */
	private void addMemberRow(Table table, EPerson eperson, boolean highlight, boolean pendingAddition, boolean pendingRemoval) throws WingException, SQLException
	{
    	String fullName = eperson.getFullName();
    	String email = eperson.getEmail();
    	String url = contextPath+"/admin/epeople?administrative-continue="+knot.getId()+"&submit_edit_eperson&epersonID="+eperson.getID();
		
    	
    	Row personData = table.addRow(null,null,highlight ? "highlight" : null);
    	
    	personData.addCell().addContent(eperson.getID().toString());
    	
		Cell nameCell = personData.addCell();
		nameCell.addXref(url, fullName);
		if (pendingAddition)
		{
			nameCell.addContent(" ");
    		nameCell.addHighlight("warn").addContent(T_members_pending);
		}
		
		personData.addCell().addXref(url, email);
		
		if (pendingRemoval)
        {
            personData.addCell().addHighlight("warn").addContent(T_pending);
        }
		else
        {
            personData.addCell().addButton("submit_remove_eperson_" + eperson.getID()).setValue(T_submit_remove);
        }
	}
}
