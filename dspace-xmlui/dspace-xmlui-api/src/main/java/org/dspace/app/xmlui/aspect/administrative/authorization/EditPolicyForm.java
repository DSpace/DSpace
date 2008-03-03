/*
 * AuthorizationMain.java
 *
 * Version: $Revision: 1.0 $
 *
 * Date: $Date: 2006/07/13 23:20:54 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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
package org.dspace.app.xmlui.aspect.administrative.authorization;

import java.sql.SQLException;
import java.util.ArrayList;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;

/**
 * @author Alexey Maslov
 */
public class EditPolicyForm extends AbstractDSpaceTransformer   
{	
	private static final Message T_title = 
		message("xmlui.administrative.authorization.EditPolicyForm.title");
	private static final Message T_trail =
		message("xmlui.administrative.authorization.EditPolicyForm.trail");
	private static final Message T_authorize_trail =
		message("xmlui.administrative.authorization.general.authorize_trail");
	private static final Message T_policyList_trail =
		message("xmlui.administrative.authorization.general.policyList_trail");	
	
	private static final Message T_main_head_new =
		message("xmlui.administrative.authorization.EditPolicyForm.main_head_new");
	private static final Message T_main_head_edit =
		message("xmlui.administrative.authorization.EditPolicyForm.main_head_edit");
	
	
	private static final Message T_error_no_group =
		message("xmlui.administrative.authorization.EditPolicyForm.error_no_group");
	private static final Message T_error_no_action =
		message("xmlui.administrative.authorization.EditPolicyForm.error_no_action");
	
	
	private static final Message T_no_results =
		message("xmlui.administrative.group.EditGroupForm.no_results");
	private static final Message T_groups_column1 =
		message("xmlui.administrative.authorization.EditPolicyForm.groups_column1");
	private static final Message T_groups_column2 =
		message("xmlui.administrative.authorization.EditPolicyForm.groups_column2");
	private static final Message T_groups_column3 =
		message("xmlui.administrative.authorization.EditPolicyForm.groups_column3");
	private static final Message T_groups_column4 =
		message("xmlui.administrative.authorization.EditPolicyForm.groups_column4");
	
	private static final Message T_submit_save =
		message("xmlui.general.save");
	private static final Message T_submit_cancel =
		message("xmlui.general.cancel");
	
	private static final Message T_set_group = 
		message("xmlui.administrative.authorization.EditPolicyForm.set_group");
	private static final Message T_current_group = 
		message("xmlui.administrative.authorization.EditPolicyForm.current_group");
	private static final Message T_groups_head = 
		message("xmlui.administrative.authorization.EditPolicyForm.groups_head");
	private static final Message T_policy_currentGroup = 
		message("xmlui.administrative.authorization.EditPolicyForm.policy_currentGroup");
	private static final Message T_label_search = 
		message("xmlui.administrative.authorization.EditPolicyForm.label_search");
    private static final Message T_submit_search_groups = 
    	message("xmlui.administrative.authorization.EditPolicyForm.submit_search_groups");
    private static final Message T_label_action = 
    	message("xmlui.administrative.authorization.EditPolicyForm.label_action");
   
	
	private static final Message T_dspace_home =
		message("xmlui.general.dspace_home");

	// How many search results are displayed at once
	private static final int RESULTS_PER_PAGE = 10;
		
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/authorize", T_authorize_trail);
        pageMeta.addTrail().addContent(T_policyList_trail);
        pageMeta.addTrail().addContent(T_trail);
    }
		
	public void addBody(Body body) throws WingException, SQLException 
	{
		/* Get and setup our parameters. We should always have an objectType and objectID, since every policy 
		 * has to have a parent resource. policyID may, however, be -1 for new, not-yet-created policies and
		 * the groupID is only set if the group is being changed. */
        int objectType = parameters.getParameterAsInteger("objectType",-1);
        int objectID = parameters.getParameterAsInteger("objectID",-1);
        int policyID = parameters.getParameterAsInteger("policyID",-1);
        int groupID = parameters.getParameterAsInteger("groupID",-1);
        int actionID = parameters.getParameterAsInteger("actionID",-1);
        int page = parameters.getParameterAsInteger("page",0);
        String query = URLDecode(parameters.getParameter("query","-1"));
        
        // The current policy, if it exists (i.e. we are not creating a new one)
        ResourcePolicy policy = ResourcePolicy.find(context, policyID);
        
        // The currently set group; it's value depends on wether previously clicked the "Set" button to change 
        // the associated group, came here to edit an existing group, or create a new one. 
        Group currentGroup;
        if (groupID != -1) {
        	currentGroup = Group.find(context, groupID);
        }
        else if (policy != null) {
        	currentGroup = policy.getGroup();
        }
        else currentGroup = null;
        
        // Same for the current action; it can either blank (-1), manually set, or inherited from the current policy
        if (policy != null && actionID == -1)
        	actionID = policy.getAction();
                
        String errorString = parameters.getParameter("errors",null);
		ArrayList<String> errors = new ArrayList<String>();
		if (errorString != null)
		{
			for (String error : errorString.split(","))
				errors.add(error);
		}
		
		
		/* Set up our current Dspace object */
		DSpaceObject dso;
		switch (objectType) {
			case Constants.COMMUNITY: dso = Community.find(context, objectID); break;
			case Constants.COLLECTION: dso = Collection.find(context, objectID); break;
			case Constants.ITEM: dso = org.dspace.content.Item.find(context, objectID); break;
			case Constants.BUNDLE: dso = Bundle.find(context, objectID); break;
			case Constants.BITSTREAM: dso = Bitstream.find(context, objectID); break;
			default: dso = null;
		}

		
        // DIVISION: edit-container-policies
        Division main = body.addInteractiveDivision("edit-policy",contextPath+"/admin/authorize",Division.METHOD_POST,"primary administrative authorization");
        
        if (policyID >= 0) {
        	objectID = policy.getResourceID();
        	objectType = policy.getResourceType();
        	main.setHead(T_main_head_edit.parameterize(policyID,Constants.typeText[objectType],objectID));
        }
        else
        	main.setHead(T_main_head_new.parameterize(Constants.typeText[objectType],objectID));
		
	    int resourceRelevance = 1 << objectType; 
		
		
	    // DIVISION: authorization-actions
        Division actions = main.addDivision("edit-policy-actions");
        
        List actionsList = actions.addList("actions","form");
        
        // actions radio buttons
        actionsList.addLabel(T_label_action);
        Item actionSelectItem = actionsList.addItem();
        Radio actionSelect = actionSelectItem.addRadio("action_id");
        actionSelect.setLabel(T_label_action);
		        //Select actionSelect = actionSelectItem.addSelect("action_id");
		        //actionsBox.addContent(T_label_action);
		        //Select actionSelect = actionsBox.addSelect("action_id");
        for( int i = 0; i < Constants.actionText.length; i++ )
        {
            // only display if action i is relevant
            //  to resource type resourceRelevance                             
            if( (Constants.actionTypeRelevance[i] & resourceRelevance) > 0)
            {
            	if (actionID == i)
            		actionSelect.addOption(true, i, Constants.actionText[i]);
            	else
            		actionSelect.addOption(i, Constants.actionText[i]);
            }
        }
        if (errors.contains("action_id"))
        	actionSelect.addError(T_error_no_action);        
        
        
        // currently set group
        actionsList.addLabel(T_policy_currentGroup);
    	Select groupSelect = actionsList.addItem().addSelect("group_id");
    	groupSelect.setSize(5);
    	for (Group group : Group.findAll(context, Group.NAME))
    	{
    		if (group == currentGroup)
    			groupSelect.addOption(true, group.getID(), group.getName());
    		else
    			groupSelect.addOption(group.getID(), group.getName());
    	}
    	if (errors.contains("group_id"))
    		groupSelect.addError(T_error_no_group);
    	
        
        // the search function
        actionsList.addLabel(T_label_search);
        Item searchItem = actionsList.addItem();
        Text searchText = searchItem.addText("query");
        if (!query.equals(new String("-1")))
        	searchText.setValue(query);
        searchItem.addButton("submit_search_groups").setValue(T_submit_search_groups);
        
        
        actionsList.addLabel();
        Item buttons = actionsList.addItem();
        buttons.addButton("submit_save").setValue(T_submit_save);
        buttons.addButton("submit_cancel").setValue(T_submit_cancel);
        
        
	    // Display the search results table
        if (!query.equals(new String("-1"))) {
        	Division groupsList = main.addDivision("edit-policy-groupsList");
            groupsList.setHead(T_groups_head);  
        	this.addGroupSearch(groupsList, currentGroup, dso, query, page);
        }       	
       	
    	main.addHidden("administrative-continue").setValue(knot.getId());
   }
	
	
	
	/**
	 * Search for groups to add to this group.
	 */
	private void addGroupSearch(Division div, Group sourceGroup, DSpaceObject dso, String query, int page) throws WingException, SQLException
	{
		Group[] groups = Group.search(context, query, page*RESULTS_PER_PAGE, (page+1)*RESULTS_PER_PAGE);
		int totalResults = Group.searchResultCount(context, query);
		ArrayList<ResourcePolicy> otherPolicies = (ArrayList<ResourcePolicy>)AuthorizeManager.getPolicies(context, dso);
		
		
		if (totalResults > RESULTS_PER_PAGE) {
			int firstIndex = page*RESULTS_PER_PAGE+1; 
        	int lastIndex = page*RESULTS_PER_PAGE + groups.length;
			String baseURL = contextPath+"/admin/authorize?administrative-continue="+knot.getId();
			
			String nextURL = null, prevURL = null;
        	if (page < ((totalResults - 1) / RESULTS_PER_PAGE))
        		nextURL = baseURL+"&page="+(page+1);
        	if (page > 0)
        		prevURL = baseURL+"&page="+(page-1);
        	
        	div.setSimplePagination(totalResults,firstIndex,lastIndex,prevURL, nextURL);
		}
		
		
		Table table = div.addTable("policy-edit-search-group",groups.length + 1, 1);
         
        Row header = table.addRow(Row.ROLE_HEADER);
        
        // Add the header row 
	    header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_groups_column1);
        header.addCell().addContent(T_groups_column2);
        header.addCell().addContent(T_groups_column3);
        header.addCell().addContent(T_groups_column4);
        
        // The rows of search results
        for (Group group : groups)
        {
        	String groupID = String.valueOf(group.getID());
        	String name = group.getName();
        	url = contextPath+"/admin/groups?administrative-continue="+knot.getId()+"&submit_edit_group&group_id="+groupID;
        	
    		Row row = table.addRow();
        	row.addCell().addContent(groupID);
        	row.addCell().addXref(url,name);
        	
        	// Iterate other other polices of our parent resource to see if any match the currently selected group
        	String otherAuthorizations = new String();
        	int groupsMatched = 0;
        	for (ResourcePolicy otherPolicy : otherPolicies) {
        		if (otherPolicy.getGroup() == group) {
        			otherAuthorizations += otherPolicy.getActionText() + ", ";
        			groupsMatched++;
        		}
        	}
        	
        	if (groupsMatched > 0) {
        		row.addCell().addContent(otherAuthorizations.substring(0,otherAuthorizations.lastIndexOf(", ")));
        	}
        	else 
        		row.addCell().addContent("-");
        	
        	if (group != sourceGroup)
    			row.addCell().addButton("submit_group_id_"+groupID).setValue(T_set_group);
    		else
    			row.addCell().addContent(T_current_group);
        	
        }
        if (groups.length <= 0) {
			table.addRow().addCell(1, 4).addContent(T_no_results);
		}
	}
	
	
}
