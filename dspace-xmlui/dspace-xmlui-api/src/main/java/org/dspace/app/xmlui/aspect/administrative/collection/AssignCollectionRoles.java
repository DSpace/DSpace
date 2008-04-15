/*
 * AssignCollectionRoles.java
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
package org.dspace.app.xmlui.aspect.administrative.collection;

import java.sql.SQLException;

import org.dspace.app.xmlui.aspect.administrative.FlowContainerUtils;
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
import org.dspace.content.Collection;
import org.dspace.eperson.Group;

/**
 * Presents the user (most likely a global administrator) with the form to edit
 * the collection's special authorization groups (or roles). Those include submission
 * group, workflows, collection admin, and default read.
 * @author Alexey Maslov
 */
public class AssignCollectionRoles extends AbstractDSpaceTransformer   
{
	/** Language Strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	
	private static final Message T_collection_trail = message("xmlui.administrative.collection.general.collection_trail");
	private static final Message T_options_metadata = message("xmlui.administrative.collection.general.options_metadata");	
	private static final Message T_options_roles = message("xmlui.administrative.collection.general.options_roles");
	
	private static final Message T_submit_return = message("xmlui.general.return");
	
	private static final Message T_title = message("xmlui.administrative.collection.AssignCollectionRoles.title");
	private static final Message T_trail = message("xmlui.administrative.collection.AssignCollectionRoles.trail");

	private static final Message T_main_head = message("xmlui.administrative.collection.AssignCollectionRoles.main_head");
	private static final Message T_no_role = message("xmlui.administrative.collection.AssignCollectionRoles.no_role");
	
	private static final Message T_create = message("xmlui.administrative.collection.AssignCollectionRoles.create");
	private static final Message T_delete = message("xmlui.general.delete");
	private static final Message T_restrict = message("xmlui.administrative.collection.AssignCollectionRoles.restrict");

	private static final Message T_help_admins = message("xmlui.administrative.collection.AssignCollectionRoles.help_admins");
	private static final Message T_help_wf_step1 = message("xmlui.administrative.collection.AssignCollectionRoles.help_wf_step1");
	private static final Message T_help_wf_step2 = message("xmlui.administrative.collection.AssignCollectionRoles.help_wf_step2");
	private static final Message T_help_wf_step3 = message("xmlui.administrative.collection.AssignCollectionRoles.help_wf_step3");
	private static final Message T_help_submitters = message("xmlui.administrative.collection.AssignCollectionRoles.help_submitters");
	private static final Message T_help_default_read = message("xmlui.administrative.collection.AssignCollectionRoles.help_default_read");

	private static final Message T_default_read_custom = message("xmlui.administrative.collection.AssignCollectionRoles.default_read_custom");
		private static final Message T_default_read_anonymous = message("xmlui.administrative.collection.AssignCollectionRoles.default_read_anonymous");

	private static final Message T_edit_authorization = message("xmlui.administrative.collection.AssignCollectionRoles.edit_authorization");

	private static final Message T_role_name = message("xmlui.administrative.collection.AssignCollectionRoles.role_name");
	private static final Message T_role_group = message("xmlui.administrative.collection.AssignCollectionRoles.role_group");
	private static final Message T_role_buttons = message("xmlui.administrative.collection.AssignCollectionRoles.role_buttons");

	private static final Message T_label_admins = message("xmlui.administrative.collection.AssignCollectionRoles.label_admins");
	private static final Message T_label_wf_step1 = message("xmlui.administrative.collection.AssignCollectionRoles.label_wf_step1");
	private static final Message T_label_wf_step2 = message("xmlui.administrative.collection.AssignCollectionRoles.label_wf_step2");
	private static final Message T_label_wf_step3 = message("xmlui.administrative.collection.AssignCollectionRoles.label_wf_step3");
	private static final Message T_label_submitters = message("xmlui.administrative.collection.AssignCollectionRoles.label_submitters");
	private static final Message T_label_default_read = message("xmlui.administrative.collection.AssignCollectionRoles.label_default_read");
	
	private static final Message T_sysadmins_only = message("xmlui.administrative.collection.AssignCollectionRoles.sysadmins_only");

	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent(T_collection_trail);
        pageMeta.addTrail().addContent(T_trail);
    }
	
	public void addBody(Body body) throws WingException, SQLException, AuthorizeException
	{
		int collectionID = parameters.getParameterAsInteger("collectionID", -1);
		Collection thisCollection = Collection.find(context, collectionID);
		
		String baseURL = contextPath + "/admin/collection?administrative-continue=" + knot.getId();
		
		Group admins = thisCollection.getAdministrators();
		Group wfStep1 = thisCollection.getWorkflowGroup(1);
		Group wfStep2 = thisCollection.getWorkflowGroup(2);
		Group wfStep3 = thisCollection.getWorkflowGroup(3);
		Group submitters = thisCollection.getSubmitters();

		Group defaultRead = null;
		int defaultReadID = FlowContainerUtils.getCollectionDefaultRead(context, collectionID);
		if (defaultReadID >= 0)
			defaultRead = Group.find(context, defaultReadID);
		
		// DIVISION: main
	    Division main = body.addInteractiveDivision("collection-assign-roles",contextPath+"/admin/collection",Division.METHOD_POST,"primary administrative collection");
	    main.setHead(T_main_head.parameterize(thisCollection.getMetadata("name")));
	    
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
	     * The collection admins 
	     */
	    // data row
	    tableRow = rolesTable.addRow(Row.ROLE_DATA);
	    tableRow.addCell(Cell.ROLE_HEADER).addContent(T_label_admins);
	    if (admins != null) 
	    {
		    tableRow.addCell().addXref(baseURL + "&submit_edit_admin", admins.getName());
		    addAdministratorOnlyButton(tableRow.addCell(),"submit_delete_admin",T_delete);
	    }
	    else 
	    {
	    	tableRow.addCell().addContent(T_no_role);
	    	addAdministratorOnlyButton(tableRow.addCell(),"submit_create_admin",T_create);
	    }
	    // help and directions row
	    tableRow = rolesTable.addRow(Row.ROLE_DATA);
	    tableRow.addCell();
	    tableRow.addCell(1,2).addHighlight("fade offset").addContent(T_help_admins);
	    
	    
	    /* 
	     * Workflow steps 1-3 
	     */
	    // data row
	    tableRow = rolesTable.addRow(Row.ROLE_DATA);
	    tableRow.addCell(Cell.ROLE_HEADER).addContent(T_label_wf_step1);
	    if (wfStep1 != null) 
	    {
	    	tableRow.addCell().addXref(baseURL + "&submit_edit_wf_step1", wfStep1.getName());
		    addAdministratorOnlyButton(tableRow.addCell(),"submit_delete_wf_step1",T_delete);
	    }
	    else 
	    {
	    	tableRow.addCell().addContent(T_no_role);
		    addAdministratorOnlyButton(tableRow.addCell(),"submit_create_wf_step1",T_create);
	    }
	    // help and directions row
	    tableRow = rolesTable.addRow(Row.ROLE_DATA);
	    tableRow.addCell();
	    tableRow.addCell(1,2).addHighlight("fade offset").addContent(T_help_wf_step1);
	    
	    
	    // data row
	    tableRow = rolesTable.addRow(Row.ROLE_DATA);
	    tableRow.addCell(Cell.ROLE_HEADER).addContent(T_label_wf_step2);
	    if (wfStep2 != null) 
	    {
	    	tableRow.addCell().addXref(baseURL + "&submit_edit_wf_step2", wfStep2.getName());
		    addAdministratorOnlyButton(tableRow.addCell(),"submit_delete_wf_step2",T_delete);
	    }
	    else 
	    {
	    	tableRow.addCell().addContent(T_no_role);
	    	addAdministratorOnlyButton(tableRow.addCell(),"submit_create_wf_step2",T_create);
	    }
	    // help and directions row
	    tableRow = rolesTable.addRow(Row.ROLE_DATA);
	    tableRow.addCell();
	    tableRow.addCell(1,2).addHighlight("fade offset").addContent(T_help_wf_step2);
	    
	    
	    // data row
	    tableRow = rolesTable.addRow(Row.ROLE_DATA);
	    tableRow.addCell(Cell.ROLE_HEADER).addContent(T_label_wf_step3);
	    if (wfStep3 != null) 
	    {
	    	tableRow.addCell().addXref(baseURL + "&submit_edit_wf_step3", wfStep3.getName());
		    addAdministratorOnlyButton(tableRow.addCell(),"submit_delete_wf_step3",T_delete);
	    }
	    else 
	    {
	    	tableRow.addCell().addContent(T_no_role);
	    	addAdministratorOnlyButton(tableRow.addCell(),"submit_create_wf_step3",T_create);
	    }
	    // help and directions row
	    tableRow = rolesTable.addRow(Row.ROLE_DATA);
	    tableRow.addCell();
	    tableRow.addCell(1,2).addHighlight("fade offset").addContent(T_help_wf_step3);
	    	
	    /*
	     * The collection submitters 
	     */
	    tableRow = rolesTable.addRow(Row.ROLE_DATA);
	    tableRow.addCell(Cell.ROLE_HEADER).addContent(T_label_submitters);
	    if (submitters != null) 
	    {
	    	tableRow.addCell().addXref(baseURL + "&submit_edit_submit", submitters.getName());
	    	addAdministratorOnlyButton(tableRow.addCell(),"submit_delete_submit",T_delete);
	    }
	    else 
	    {
	    	tableRow.addCell().addContent(T_no_role);
	    	addAdministratorOnlyButton(tableRow.addCell(),"submit_create_submit",T_create);
	    }
	    // help and directions row
	    tableRow = rolesTable.addRow(Row.ROLE_DATA);
	    tableRow.addCell();
	    tableRow.addCell(1,2).addHighlight("fade offset").addContent(T_help_submitters);
	    
	    
	    /* 
	     * The collection's default read authorizations 
	     */
	    tableRow = rolesTable.addRow(Row.ROLE_DATA);
	    tableRow.addCell(Cell.ROLE_HEADER).addContent(T_label_default_read);
	    if (defaultRead == null) 
	    {
	    	// Custome reading permissions, we can't handle it, just provide a link to the 
	    	// authorizations manager.
	    	tableRow.addCell(1,2).addContent(T_default_read_custom);
	    }
	    else if (defaultRead.getID() == 0) {
	    	// Anonymous reading
	    	tableRow.addCell().addContent(T_default_read_anonymous);
	    	addAdministratorOnlyButton(tableRow.addCell(),"submit_create_default_read",T_restrict);
	    }
	    else 
	    {
	    	// A specific group is dedicated to reading.
	    	tableRow.addCell().addXref(baseURL + "&submit_edit_default_read", defaultRead.getName());
		    addAdministratorOnlyButton(tableRow.addCell(),"submit_delete_default_read",T_delete);
	    }
	 
	    // help and directions row
	    tableRow = rolesTable.addRow(Row.ROLE_DATA);
	    tableRow.addCell();
	    tableRow.addCell(1,2).addHighlight("fade offset").addContent(T_help_default_read);
	    
	    
	    if (AuthorizeManager.isAdmin(context))
	    {
		    // add one last link to edit the raw authorizations
		    Cell authCell =rolesTable.addRow().addCell(1,3);
		    authCell.addXref(baseURL + "&submit_authorizations", T_edit_authorization);
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
