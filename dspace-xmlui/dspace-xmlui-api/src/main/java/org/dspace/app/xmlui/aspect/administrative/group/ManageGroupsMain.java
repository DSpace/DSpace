/*
 * ManageGroupsMain.java
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
package org.dspace.app.xmlui.aspect.administrative.group;

import java.sql.SQLException;

import org.dspace.app.xmlui.aspect.administrative.FlowGroupUtils;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Highlight;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.content.Collection;
import org.dspace.eperson.Group;

/**
 * Manage groups page is the entry point for group management. From here the user
 * may browse/search a the list of groups, they may also add new groups or select
 * exiting groups to edit or delete.
 * 
 * @author Alexey Maslov
 * @author Scott Phillips
 */
public class ManageGroupsMain extends AbstractDSpaceTransformer   
{
	/** Language Strings */
	private static final Message T_dspace_home =
		message("xmlui.general.dspace_home");

	private static final Message T_group_trail =
		message("xmlui.administrative.group.general.group_trail");
	
	private static final Message T_title =
		message("xmlui.administrative.group.ManageGroupsMain.title");

	private static final Message T_main_head =
		message("xmlui.administrative.group.ManageGroupsMain.main_head");
	
	private static final Message T_actions_head =
		message("xmlui.administrative.group.ManageGroupsMain.actions_head");
	
	private static final Message T_actions_create=
		message("xmlui.administrative.group.ManageGroupsMain.actions_create");
	
	private static final Message T_actions_create_link =
		message("xmlui.administrative.group.ManageGroupsMain.actions_create_link");
	
	private static final Message T_actions_browse =
		message("xmlui.administrative.group.ManageGroupsMain.actions_browse");
	
	private static final Message T_actions_browse_link =
		message("xmlui.administrative.group.ManageGroupsMain.actions_browse_link");
	
	private static final Message T_actions_search =
		message("xmlui.administrative.group.ManageGroupsMain.actions_search");
	
	private static final Message T_search_help =
		message("xmlui.administrative.group.ManageGroupsMain.search_help");
	
	private static final Message T_go =
		message("xmlui.general.go");
	
	private static final Message T_search_head =
		message("xmlui.administrative.group.ManageGroupsMain.search_head");

	private static final Message T_search_column1 =
		message("xmlui.administrative.group.ManageGroupsMain.search_column1");
	
	private static final Message T_search_column2 =
		message("xmlui.administrative.group.ManageGroupsMain.search_column2");
	
	private static final Message T_search_column3 =
		message("xmlui.administrative.group.ManageGroupsMain.search_column3");
	
	private static final Message T_search_column4 =
		message("xmlui.administrative.group.ManageGroupsMain.search_column4");

	private static final Message T_search_column5 =
		message("xmlui.administrative.group.ManageGroupsMain.search_column5");

	private static final Message T_collection_link = 
		message("xmlui.administrative.group.ManageGroupsMain.collection_link");
	
	private static final Message T_submit_delete =
		message("xmlui.administrative.group.ManageGroupsMain.submit_delete");
	
	private static final Message T_no_results =
		message("xmlui.administrative.group.ManageGroupsMain.no_results");
	
	/** The number of results to show on one page. */
	private static final int PAGE_SIZE = 15;
	
	/** The maximum size of a collection name allowed */
	private static final int MAX_COLLECTION_NAME = 30;
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/groups",T_group_trail);
    }
		
	public void addBody(Body body) throws WingException, SQLException 
	{
		// Get all our parameters
		String baseURL  = contextPath +"/admin/groups?administrative-continue="+knot.getId();
		String query    = URLDecode(parameters.getParameter("query",""));
		int page        = parameters.getParameterAsInteger("page",0);
		int highlightID = parameters.getParameterAsInteger("highlightID",-1);
        int resultCount = Group.searchResultCount(context, query);
        Group[] groups  = Group.search(context, query, page*PAGE_SIZE, PAGE_SIZE);
		
		
		
		// DIVISION: groups-main
		Division main = body.addInteractiveDivision("groups-main",contextPath +"/admin/groups",Division.METHOD_POST,"primary administrative groups");
		main.setHead(T_main_head);
		
		
		
		
		// DIVISION: group-actions
        Division actions = main.addDivision("group-actions");
        actions.setHead(T_actions_head);
        
        // Browse Epeople
        List actionsList = actions.addList("actions");
        actionsList.addLabel(T_actions_create);
        actionsList.addItemXref(baseURL+"&submit_add", T_actions_create_link);
        actionsList.addLabel(T_actions_browse);
        actionsList.addItemXref(baseURL+"&query&submit_search",T_actions_browse_link);
	    
        actionsList.addLabel(T_actions_search);
        org.dspace.app.xmlui.wing.element.Item actionItem = actionsList.addItem();
        Text queryField = actionItem.addText("query");
        if (query != null)
        	queryField.setValue(query);
        queryField.setHelp(T_search_help);
        actionItem.addButton("submit_search").setValue(T_go);
        
        
        
        
        
        
        
        // DIVISION: group-search
        Division search = main.addDivision("group-search");
		search.setHead(T_search_head);
        

        if (resultCount > PAGE_SIZE) 
		{
        	// If there are enough results then paginate the results
        	int firstIndex = page*PAGE_SIZE+1; 
        	int lastIndex = page*PAGE_SIZE + groups.length;
       
        	String nextURL = null, prevURL = null;
        	if (page < (resultCount / PAGE_SIZE))
        		nextURL = baseURL+"&page="+(page+1);
        	if (page > 0)
        		prevURL = baseURL+"&page="+(page-1);
        	
			search.setSimplePagination(resultCount,firstIndex,lastIndex,prevURL, nextURL);
		}
        
        
        Table table = search.addTable("groups-search-table",groups.length + 1, 1);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_search_column1);
        header.addCell().addContent(T_search_column2);
        header.addCell().addContent(T_search_column3);
        header.addCell().addContent(T_search_column4);
        header.addCell().addContent(T_search_column5);
        
        for (Group group : groups)
        {
        	Row row;
        	if (group.getID() == highlightID)
        		row = table.addRow(null,null,"highlight");
        	else
        		row = table.addRow();
        	
        	if (group.getID() > 1)
        	{
	        	CheckBox select = row.addCell().addCheckBox("select_group");
	        	select.setLabel(new Integer(group.getID()).toString());
	        	select.addOption(new Integer(group.getID()).toString());
        	}
        	else
        	{
        		// Don't allow the user to remove the administrative (id:1) or 
        		// anonymous group (id:0) 
        		row.addCell();
        	}
        	
        	row.addCell().addContent(group.getID());
        	row.addCell().addXref(baseURL+"&submit_edit&groupID="+group.getID(), group.getName());
        	
        	int memberCount = group.getMembers().length + group.getMemberGroups().length;
        	row.addCell().addContent(memberCount == 0 ? "-" : String.valueOf(memberCount));
        	
        	Cell cell = row.addCell();
        	if (FlowGroupUtils.getCollectionId(group.getName()) > -1)
        	{
        		Collection collection = Collection.find(context, FlowGroupUtils.getCollectionId(group.getName()) );
        		if (collection != null)
        		{
	        		String collectionName = collection.getMetadata("name");
	        		
	        		if (collectionName == null)
	        			collectionName = "";
	        		else if (collectionName.length() > MAX_COLLECTION_NAME)
	        			collectionName = collectionName.substring(0,MAX_COLLECTION_NAME-3) + "...";
	        		
	        		cell.addContent(collectionName+" ");
	        		
	        		Highlight highlight = cell.addHighlight("fade");
	        		
	        		highlight.addContent("[");
	        		highlight.addXref(contextPath+"/handle/"+collection.getHandle(), T_collection_link);
	        		highlight.addContent("]");
        		}
        	}
        	
        }
        
        if (groups.length <= 0)
        {
        	Cell cell = table.addRow().addCell(1,5);
        	cell.addHighlight("italic").addContent(T_no_results);
        }
        else
        {
        	search.addPara().addButton("submit_delete").setValue(T_submit_delete);	
        }
        
        search.addHidden("administrative-continue").setValue(knot.getId());
   }
}
