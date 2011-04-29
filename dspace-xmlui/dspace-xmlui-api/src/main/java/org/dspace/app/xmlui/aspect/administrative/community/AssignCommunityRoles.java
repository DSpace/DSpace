/*
 * AssignCommunityRoles.java
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
package org.dspace.app.xmlui.aspect.administrative.community;

import java.sql.SQLException;

import org.dspace.app.util.AuthorizeUtil;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Community;
import org.dspace.eperson.Group;

/**
 * Presents the user (most likely a global administrator) with the form to edit
 * the community's special authorization groups (or roles). The only role
 * support at the moment is that of community administrator.
 * @author Alexey Maslov
 * @author Nicholas Riley
 */
public class AssignCommunityRoles extends AbstractDSpaceTransformer   
{
	/** Language Strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	
	private static final Message T_community_trail = message("xmlui.administrative.community.general.community_trail");
	private static final Message T_options_metadata = message("xmlui.administrative.community.general.options_metadata");	
	private static final Message T_options_roles = message("xmlui.administrative.community.general.options_roles");
	
	private static final Message T_submit_return = message("xmlui.general.return");
	
	private static final Message T_title = message("xmlui.administrative.community.AssignCommunityRoles.title");
	private static final Message T_trail = message("xmlui.administrative.community.AssignCommunityRoles.trail");

	private static final Message T_main_head = message("xmlui.administrative.community.AssignCommunityRoles.main_head");
	private static final Message T_no_role = message("xmlui.administrative.community.AssignCommunityRoles.no_role");
	
	private static final Message T_create = message("xmlui.administrative.community.AssignCommunityRoles.create");
	private static final Message T_delete = message("xmlui.general.delete");

	private static final Message T_help_admins = message("xmlui.administrative.community.AssignCommunityRoles.help_admins");

    private static final Message T_edit_authorizations = message("xmlui.administrative.community.EditCommunityMetadataForm.edit_authorizations");

	private static final Message T_role_name = message("xmlui.administrative.community.AssignCommunityRoles.role_name");
	private static final Message T_role_group = message("xmlui.administrative.community.AssignCommunityRoles.role_group");
	private static final Message T_role_buttons = message("xmlui.administrative.community.AssignCommunityRoles.role_buttons");

	private static final Message T_label_admins = message("xmlui.administrative.community.AssignCommunityRoles.label_admins");
	
	private static final Message T_sysadmins_only = message("xmlui.administrative.community.AssignCommunityRoles.sysadmins_only");

	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent(T_community_trail);
        pageMeta.addTrail().addContent(T_trail);
    }
	
	public void addBody(Body body) throws WingException, SQLException, AuthorizeException
	{
        int communityID = parameters.getParameterAsInteger("communityID", -1);
        Community thisCommunity = Community.find(context, communityID);
		
		String baseURL = contextPath + "/admin/community?administrative-continue=" + knot.getId();
		
		Group admins = thisCommunity.getAdministrators();

		// DIVISION: main
	    Division main = body.addInteractiveDivision("community-assign-roles",contextPath+"/admin/community",Division.METHOD_POST,"primary administrative community");
        main.setHead(T_main_head.parameterize(thisCommunity.getName()));
	    
	    List options = main.addList("options", List.TYPE_SIMPLE, "horizontal");
	    options.addItem().addXref(baseURL+"&submit_metadata",T_options_metadata);
	    options.addItem().addHighlight("bold").addXref(baseURL+"&submit_roles",T_options_roles);

	    // The table of admin roles
	    Table rolesTable = main.addTable("roles-table", 6, 5);
	    Row tableRow;
	    
	    // The header row
	    Row tableHeader = rolesTable.addRow(Row.ROLE_HEADER);
	    tableHeader.addCell().addContent(T_role_name);
	    tableHeader.addCell().addContent(T_role_group);
	    tableHeader.addCell().addContent(T_role_buttons);
	    rolesTable.addRow();
	    	    
	    /* 
	     * The community admins 
	     */
	    // data row
	    tableRow = rolesTable.addRow(Row.ROLE_DATA);
	    tableRow.addCell(Cell.ROLE_HEADER).addContent(T_label_admins);
	    if (admins != null) 
	    {
	        try
	        {
    	        AuthorizeUtil.authorizeManageAdminGroup(context, thisCommunity);
    	        tableRow.addCell().addXref(baseURL + "&submit_edit_admin", admins.getName());
	        }
	        catch (AuthorizeException authex) {
	            // add a notice, the user is not authorized to create/edit community's admin group
	            tableRow.addCell().addContent(T_sysadmins_only);
	        }
	        try
	        {
	            AuthorizeUtil.authorizeRemoveAdminGroup(context, thisCommunity);
	            tableRow.addCell().addButton("submit_delete_admin").setValue(T_delete);
	        }
	        catch (AuthorizeException authex)
	        {
	            // nothing to add, the user is not allowed to delete the group
	        }
	    }
	    else 
	    {
	    	tableRow.addCell().addContent(T_no_role);
	    	Cell commAdminCell = tableRow.addCell();
	    	try
            {
                AuthorizeUtil.authorizeManageAdminGroup(context, thisCommunity);
                commAdminCell.addButton("submit_create_admin").setValue(T_create);
            }
            catch (AuthorizeException authex) 
            {
                // add a notice, the user is not authorized to create/edit community's admin group
                addAdministratorOnlyButton(commAdminCell, "submit_create_admin", T_create);
            }   
	    }
	    // help and directions row
	    tableRow = rolesTable.addRow(Row.ROLE_DATA);
	    tableRow.addCell();
	    tableRow.addCell(1,2).addHighlight("fade offset").addContent(T_help_admins);
	    
	    try
	    {
	        AuthorizeUtil.authorizeManageCommunityPolicy(context, thisCommunity);
		    // add one last link to edit the raw authorizations
		    Cell authCell =rolesTable.addRow().addCell(1,3);
		    authCell.addXref(baseURL + "&submit_authorizations", T_edit_authorizations);
	    }
	    catch (AuthorizeException authex) {
	        // nothing to add, the user is not authorized to manage community's policies
	    }

	    Para buttonList = main.addPara();
	    buttonList.addButton("submit_return").setValue(T_submit_return);
	    
    	main.addHidden("administrative-continue").setValue(knot.getId());
    	
    }
	
	
	private void addAdministratorOnlyButton(Cell cell, String buttonName, Message buttonLabel) throws WingException, SQLException
	{
    	Button button = cell.addButton(buttonName);
    	button.setValue(buttonLabel);
    	if (!AuthorizeManager.isAdmin(context))
    	{
    		// Only admins can create or delete
    		button.setDisabled();
    		cell.addHighlight("fade").addContent(T_sysadmins_only);
    	}
	}
}
