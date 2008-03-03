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
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.content.Collection;
import org.dspace.content.Community;

/**
 * @author Alexey Maslov
 */
public class AuthorizationMain extends AbstractDSpaceTransformer   
{	
	private static final Message T_title = 
		message("xmlui.administrative.authorization.AuthorizationMain.title");
	private static final Message T_authorize_trail =
		message("xmlui.administrative.authorization.general.authorize_trail");
	
	private static final Message T_main_head =
		message("xmlui.administrative.authorization.AuthorizationMain.main_head");
	
	private static final Message T_actions_head =
		message("xmlui.administrative.authorization.AuthorizationMain.actions_head");
    private static final Message T_actions_item_lookup =
		message("xmlui.administrative.authorization.AuthorizationMain.actions_item_lookup");
    
    private static final Message T_bad_name =
		message("xmlui.administrative.authorization.AuthorizationMain.bad_name");
    private static final Message T_search_help =
		message("xmlui.administrative.authorization.AuthorizationMain.search_help");
    private static final Message T_submit_find =
		message("xmlui.administrative.authorization.AuthorizationMain.submit_find");
    
    private static final Message T_actions_advanced =
		message("xmlui.administrative.authorization.AuthorizationMain.actions_advanced");
    private static final Message T_actions_advanced_link =
		message("xmlui.administrative.authorization.AuthorizationMain.actions_advanced_link");
    
    private static final Message T_containerList_head =
		message("xmlui.administrative.authorization.AuthorizationMain.containerList_head");
    private static final Message T_containerList_para =
		message("xmlui.administrative.authorization.AuthorizationMain.containerList_para");
	
	private static final Message T_dspace_home =
		message("xmlui.general.dspace_home");
	
	private static final Message T_untitled =
		message("xmlui.general.untitled");
	
	
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/authorize", T_authorize_trail);
    }
	
	public void addBody(Body body) throws WingException, SQLException 
	{
		/* Get and setup our parameters */
        String query = URLDecode(parameters.getParameter("query",null));
        String baseURL = contextPath+"/admin/epeople?administrative-continue="+knot.getId();
        
        String errorString = parameters.getParameter("errors",null);
		ArrayList<String> errors = new ArrayList<String>();
		if (errorString != null)
		{
			for (String error : errorString.split(","))
				errors.add(error);
		}
        
        Division main = body.addInteractiveDivision("authorization-main",contextPath+"/admin/authorize",Division.METHOD_POST,"primary administrative authorization");
		main.setHead(T_main_head);
		//main.addPara(T_main_para);		
		
		
		// DIVISION: authorization-actions
        Division actions = main.addDivision("authorization-actions");
        actions.setHead(T_actions_head);
        
        List actionsList = actions.addList("actions");
        actionsList.addLabel(T_actions_item_lookup);
        Item actionItem = actionsList.addItem();
        Text queryField = actionItem.addText("identifier");
        if (query != null)
        	queryField.setValue(query);
        if (errors.contains("identifier")) 
        	queryField.addError(T_bad_name);
        queryField.setHelp(T_search_help);
        actionItem.addButton("submit_edit").setValue(T_submit_find);
        actionsList.addLabel(T_actions_advanced);
        actionsList.addItemXref(baseURL+"&submit_wildcard", T_actions_advanced_link);
		
        // DIVISION: authorization-containerList
        Division containers = main.addDivision("authorization-containerList");
        containers.setHead(T_containerList_head);
        containers.addPara(T_containerList_para);
        
        List containerList = containers.addList("containerList");
        this.containerListBuilder(baseURL,containerList,null);
        
		main.addHidden("administrative-continue").setValue(knot.getId());
	}
	
	
	/* A recursive helper method to build the community/collection hierarchy list */
	private void containerListBuilder (String baseURL, List parentList, Community currentCommunity) 
		throws SQLException, WingException
	{
		if (currentCommunity == null) {
			for (Community topLevel : Community.findAllTop(context)) {
				containerListBuilder(baseURL,parentList,topLevel);
			}
		}
		else {
			parentList.addItem().addHighlight("bold").addXref(baseURL+"&submit_edit&community_id="+currentCommunity.getID(), currentCommunity.getMetadata("name"));
			List containerSubList = null;
			for (Collection subCols : currentCommunity.getCollections()) 
			{
				if (containerSubList == null)
					containerSubList = parentList.addList("subList" + currentCommunity.getID());
				String name = subCols.getMetadata("name");
				if (name == null || name.length() == 0)
					containerSubList.addItemXref(baseURL+"&submit_edit&collection_id="+subCols.getID(), T_untitled);
				else
					containerSubList.addItemXref(baseURL+"&submit_edit&collection_id="+subCols.getID(), name);
        	}
			for (Community subComs : currentCommunity.getSubcommunities()) 
			{
				if (containerSubList == null)
					containerSubList = parentList.addList("subList" + currentCommunity.getID());
				containerListBuilder(baseURL,containerSubList,subComs);
			}
		}
	}
	
	
	
	
	
	
	
}
