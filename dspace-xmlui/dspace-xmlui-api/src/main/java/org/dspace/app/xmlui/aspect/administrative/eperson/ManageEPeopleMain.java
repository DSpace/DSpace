/*
 * ManageEPeopleMain.java
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
package org.dspace.app.xmlui.aspect.administrative.eperson;

import java.sql.SQLException;
import java.util.Vector;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.eperson.EPerson;

/**
 * The manage epeople page is the starting point page for managing 
 * epeople. From here the user is able to browse or search for epeople, 
 * once identified the user can selected them for deletition by selecting 
 * the checkboxes and clicking delete or click their name to edit the 
 * eperson.
 * 
 * @author Alexey Maslov
 * @author Scott Phillips
 */
public class ManageEPeopleMain extends AbstractDSpaceTransformer   
{	
	
	/** Language Strings */
	private static final Message T_title = 
		message("xmlui.administrative.eperson.ManageEPeopleMain.title");
	
	private static final Message T_eperson_trail =
		message("xmlui.administrative.eperson.general.epeople_trail");
	
	private static final Message T_main_head =
		message("xmlui.administrative.eperson.ManageEPeopleMain.main_head");
	
	private static final Message T_actions_head =
		message("xmlui.administrative.eperson.ManageEPeopleMain.actions_head");
	
	private static final Message T_actions_create =
		message("xmlui.administrative.eperson.ManageEPeopleMain.actions_create");
	
	private static final Message T_actions_create_link =
		message("xmlui.administrative.eperson.ManageEPeopleMain.actions_create_link");
	
	private static final Message T_actions_browse =
		message("xmlui.administrative.eperson.ManageEPeopleMain.actions_browse");
	
	private static final Message T_actions_browse_link =
		message("xmlui.administrative.eperson.ManageEPeopleMain.actions_browse_link");
	
	private static final Message T_actions_search =
		message("xmlui.administrative.eperson.ManageEPeopleMain.actions_search");
	
	private static final Message T_search_help =
		message("xmlui.administrative.eperson.ManageEPeopleMain.search_help");
	
	private static final Message T_dspace_home =
		message("xmlui.general.dspace_home");
	
	private static final Message T_go =
		message("xmlui.general.go");

	private static final Message T_search_head =
		message("xmlui.administrative.eperson.ManageEPeopleMain.search_head");

	private static final Message T_search_column1 =
		message("xmlui.administrative.eperson.ManageEPeopleMain.search_column1");
	
	private static final Message T_search_column2 =
		message("xmlui.administrative.eperson.ManageEPeopleMain.search_column2");

	private static final Message T_search_column3 =
		message("xmlui.administrative.eperson.ManageEPeopleMain.search_column3");

	private static final Message T_search_column4 =
		message("xmlui.administrative.eperson.ManageEPeopleMain.search_column4");

	private static final Message T_submit_delete =
		message("xmlui.administrative.eperson.ManageEPeopleMain.submit_delete");

	private static final Message T_no_results =
		message("xmlui.administrative.eperson.ManageEPeopleMain.no_results");
	
	
	
	/**
	 * The total number of entries to show on a page
	 */
	private static final int PAGE_SIZE = 15;
	
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/epeople",T_eperson_trail);
    }
		
	
	public void addBody(Body body) throws WingException, SQLException 
	{
		/* Get and setup our parameters */
        int page          = parameters.getParameterAsInteger("page",0);
        int highlightID   = parameters.getParameterAsInteger("highlightID",-1);
        String query      = URLDecode(parameters.getParameter("query",null));
        String baseURL    = contextPath+"/admin/epeople?administrative-continue="+knot.getId();
        int resultCount   = EPerson.searchResultCount(context, query);	
        EPerson[] epeople = EPerson.search(context, query, page*PAGE_SIZE, PAGE_SIZE);
        
        
        // DIVISION: eperson-main
		Division main = body.addInteractiveDivision("epeople-main",contextPath+"/admin/epeople",Division.METHOD_POST,"primary administrative eperson");
		main.setHead(T_main_head);
		
		
		
		
		// DIVISION: eperson-actions
        Division actions = main.addDivision("epeople-actions");
        actions.setHead(T_actions_head);
        
        List actionsList = actions.addList("actions");
        actionsList.addLabel(T_actions_create);
        actionsList.addItemXref(baseURL+"&submit_add", T_actions_create_link);
        actionsList.addLabel(T_actions_browse);
        actionsList.addItemXref(baseURL+"&query&submit_search", 
        		T_actions_browse_link);
	    
        actionsList.addLabel(T_actions_search);
        org.dspace.app.xmlui.wing.element.Item actionItem = actionsList.addItem();
        Text queryField = actionItem.addText("query");
        if (query != null)
        	queryField.setValue(query);
        queryField.setHelp(T_search_help);
        actionItem.addButton("submit_search").setValue(T_go);
       
        
        
        // DIVISION: eperson-search
        Division search = main.addDivision("eperson-search");
		search.setHead(T_search_head);
        
        // If there are more than 10 results the paginate the division.
        if (resultCount > PAGE_SIZE) 
		{
        	// If there are enough results then paginate the results
        	int firstIndex = page*PAGE_SIZE+1; 
        	int lastIndex = page*PAGE_SIZE + epeople.length;
       
        	String nextURL = null, prevURL = null;
        	if (page < (resultCount / PAGE_SIZE))
        		nextURL = baseURL+"&page="+(page+1);
        	if (page > 0)
        		prevURL = baseURL+"&page="+(page-1);
        	
			search.setSimplePagination(resultCount,firstIndex,lastIndex,prevURL, nextURL);
		}
        
		
        	
        Table table = search.addTable("eperson-search-table", epeople.length + 1, 1);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_search_column1);
        header.addCell().addContent(T_search_column2);
        header.addCell().addContent(T_search_column3);
        header.addCell().addContent(T_search_column4);

        CheckBox selectEPerson; 
        for (EPerson person : epeople)
        {
        	String epersonID = String.valueOf(person.getID());
        	String fullName = person.getFullName();
        	String email = person.getEmail();
        	String url = baseURL+"&submit_edit&epersonID="+epersonID;
        	Vector<String> deleteConstraints = person.getDeleteConstraints();
        	
        	
        	Row row;
        	if (person.getID() == highlightID)
        		// This is a highlighted eperson
        		row = table.addRow(null, null, "highlight");
        	else
        		row = table.addRow();
        	
        	selectEPerson = row.addCell().addCheckBox("select_eperson");
        	selectEPerson.setLabel(epersonID);
        	selectEPerson.addOption(epersonID);
        	if (deleteConstraints != null && deleteConstraints.size() > 0)
        		selectEPerson.setDisabled();
        	
        	
        	row.addCellContent(epersonID);
            row.addCell().addXref(url, fullName);
            row.addCell().addXref(url, email);
        }
        
        if (epeople.length <= 0) 
		{
        	Cell cell = table.addRow().addCell(1, 4);
        	cell.addHighlight("italic").addContent(T_no_results);
        }
        else 
        {
        	search.addPara().addButton("submit_delete").setValue(T_submit_delete);
        }
		
		main.addHidden("administrative-continue").setValue(knot.getId());
        
   }
}
