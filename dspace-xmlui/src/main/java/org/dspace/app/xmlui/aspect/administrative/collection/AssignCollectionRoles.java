/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.collection;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.app.xmlui.aspect.administrative.FlowContainerUtils;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.dspace.xmlworkflow.Role;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.WorkflowUtils;
import org.dspace.xmlworkflow.service.XmlWorkflowService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

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
	private static final Message T_options_harvest = message("xmlui.administrative.collection.GeneralCollectionHarvestingForm.options_harvest");
	private static final Message T_options_curate = message("xmlui.administrative.collection.general.options_curate");
        
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
	private static final Message T_label_wf = message("xmlui.administrative.collection.AssignCollectionRoles.label_wf");
	private static final Message T_label_wf_step1 = message("xmlui.administrative.collection.AssignCollectionRoles.label_wf_step1");
	private static final Message T_label_wf_step2 = message("xmlui.administrative.collection.AssignCollectionRoles.label_wf_step2");
	private static final Message T_label_wf_step3 = message("xmlui.administrative.collection.AssignCollectionRoles.label_wf_step3");
	private static final Message T_label_submitters = message("xmlui.administrative.collection.AssignCollectionRoles.label_submitters");
	private static final Message T_label_default_read = message("xmlui.administrative.collection.AssignCollectionRoles.label_default_read");
	
	private static final Message T_sysadmins_only = message("xmlui.administrative.collection.AssignCollectionRoles.sysadmins_only");
	private static final Message T_sysadmins_only_repository_role = message("xmlui.administrative.collection.AssignCollectionRoles.repository_role");
	private static final Message T_not_allowed = message("xmlui.administrative.collection.AssignCollectionRoles.not_allowed");

	
    private static Logger log = Logger.getLogger(AssignCollectionRoles.class);

	protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

	protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

	protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();


	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent(T_collection_trail);
        pageMeta.addTrail().addContent(T_trail);
    }
	
	public void addBody(Body body) throws WingException, SQLException, AuthorizeException
	{
		UUID collectionID = UUID.fromString(parameters.getParameter("collectionID", null));
		Collection thisCollection = collectionService.find(context, collectionID);
		
		String baseURL = contextPath + "/admin/collection?administrative-continue=" + knot.getId();
		
		Group admins = thisCollection.getAdministrators();
		Group submitters = thisCollection.getSubmitters();

		Group defaultRead = FlowContainerUtils.getCollectionDefaultRead(context, thisCollection);
		
		// DIVISION: main
	    Division main = body.addInteractiveDivision("collection-assign-roles",contextPath+"/admin/collection",Division.METHOD_POST,"primary administrative collection");
	    main.setHead(T_main_head.parameterize(collectionService.getMetadata(thisCollection, "name")));
	    
	    List options = main.addList("options", List.TYPE_SIMPLE, "horizontal");
	    options.addItem().addXref(baseURL+"&submit_metadata",T_options_metadata);
	    options.addItem().addHighlight("bold").addXref(baseURL+"&submit_roles",T_options_roles);
	    options.addItem().addXref(baseURL+"&submit_harvesting",T_options_harvest);
            options.addItem().addXref(baseURL+"&submit_curate",T_options_curate);
	    	    
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
	        try
            {
                AuthorizeUtil.authorizeManageAdminGroup(context, thisCollection);
                tableRow.addCell().addXref(baseURL + "&submit_edit_admin", admins.getName());
            }
	        catch (AuthorizeException authex) {
                // add a notice, the user is not authorized to create/edit collection's admin group
                tableRow.addCell().addContent(T_not_allowed);
            }
            try
            {
                AuthorizeUtil.authorizeRemoveAdminGroup(context, thisCollection);
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
	    	try
            {
                AuthorizeUtil.authorizeManageAdminGroup(context, thisCollection);
                tableRow.addCell().addButton("submit_create_admin").setValue(T_create);
            }
            catch (AuthorizeException authex) {
                // add a notice, the user is not authorized to create/edit collection's admin group
                tableRow.addCell().addContent(T_not_allowed);
            }
	    }
	    // help and directions row
	    tableRow = rolesTable.addRow(Row.ROLE_DATA);
	    tableRow.addCell();
	    tableRow.addCell(1,2).addHighlight("fade offset").addContent(T_help_admins);
	    
	    /*
	     * The collection submitters
	     */
	    tableRow = rolesTable.addRow(Row.ROLE_DATA);
	    tableRow.addCell(Cell.ROLE_HEADER).addContent(T_label_submitters);
	    try
	    {
	        AuthorizeUtil.authorizeManageSubmittersGroup(context, thisCollection);
    	    if (submitters != null)
    	    {
    	    	tableRow.addCell().addXref(baseURL + "&submit_edit_submit", submitters.getName());
                tableRow.addCell().addButton("submit_delete_submit").setValue(T_delete);
    	    }
    	    else
    	    {
    	    	tableRow.addCell().addContent(T_no_role);
                tableRow.addCell().addButton("submit_create_submit").setValue(T_create);
    	    }
	    }
	    catch (AuthorizeException authex)
	    {
	        tableRow.addCell().addContent(T_not_allowed);
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
	    else if (StringUtils.equals(defaultRead.getName(), Group.ANONYMOUS)) {
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


		if(WorkflowServiceFactory.getInstance().getWorkflowService() instanceof XmlWorkflowService) {
             try{
                 HashMap<String, Role> roles = WorkflowUtils.getAllExternalRoles(thisCollection);
                 addXMLWorkflowRoles(thisCollection, baseURL, roles, rolesTable);
             } catch (WorkflowConfigurationException e) {
                log.error(LogManager.getHeader(context, "error while getting collection roles", "Collection id: " + thisCollection.getID()));
             } catch (IOException e) {
                 log.error(LogManager.getHeader(context, "error while getting collection roles", "Collection id: " + thisCollection.getID()));
             }
         }else{
             addOriginalWorkflowRoles(thisCollection, baseURL, rolesTable);
         }

	    try
	    {
	        AuthorizeUtil.authorizeManageCollectionPolicy(context, thisCollection);
		    // add one last link to edit the raw authorizations
		    Cell authCell =rolesTable.addRow().addCell(1,3);
		    authCell.addXref(baseURL + "&submit_authorizations", T_edit_authorization);
	    }
	    catch (AuthorizeException authex) {
            // nothing to add, the user is not authorized to edit collection's policies
        }

	    Para buttonList = main.addPara();
	    buttonList.addButton("submit_return").setValue(T_submit_return);



    	main.addHidden("administrative-continue").setValue(knot.getId());

    }

    private void addOriginalWorkflowRoles(Collection thisCollection, String baseURL, Table rolesTable) throws SQLException, WingException {
        Row tableRow;/*
	     * Workflow steps 1-3
	     */
	    // data row
	    try
        {
            Group wfStep1 = collectionService.getWorkflowGroup(thisCollection, 1);
            Group wfStep2 = collectionService.getWorkflowGroup(thisCollection, 2);
            Group wfStep3 = collectionService.getWorkflowGroup(thisCollection, 3);
            AuthorizeUtil.authorizeManageWorkflowsGroup(context, thisCollection);
    	    tableRow = rolesTable.addRow(Row.ROLE_DATA);
    	    tableRow.addCell(Cell.ROLE_HEADER).addContent(T_label_wf_step1);
    	    if (wfStep1 != null) 
    	    {
    	    	tableRow.addCell().addXref(baseURL + "&submit_edit_wf_step1", wfStep1.getName());
                tableRow.addCell().addButton("submit_delete_wf_step1").setValue(T_delete);
    	    }
    	    else 
    	    {
    	    	tableRow.addCell().addContent(T_no_role);
                tableRow.addCell().addButton("submit_create_wf_step1").setValue(T_create);
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
                tableRow.addCell().addButton("submit_delete_wf_step2").setValue(T_delete);
    	    }
    	    else 
    	    {
    	    	tableRow.addCell().addContent(T_no_role);
                tableRow.addCell().addButton("submit_create_wf_step2").setValue(T_create);
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
                tableRow.addCell().addButton("submit_delete_wf_step3").setValue(T_delete);
    	    }
    	    else 
    	    {
    	    	tableRow.addCell().addContent(T_no_role);
                tableRow.addCell().addButton("submit_create_wf_step3").setValue(T_create);
    	    }
    	    // help and directions row
    	    tableRow = rolesTable.addRow(Row.ROLE_DATA);
    	    tableRow.addCell();
    	    tableRow.addCell(1,2).addHighlight("fade offset").addContent(T_help_wf_step3);
        }
	    catch (AuthorizeException authex) {
            // add a notice, the user is not allowed to manage workflow group
	        tableRow = rolesTable.addRow(Row.ROLE_DATA);
            tableRow.addCell(Cell.ROLE_HEADER).addContent(T_label_wf);
            tableRow.addCell().addContent(T_not_allowed);
        }
    }

    private void addXMLWorkflowRoles(Collection thisCollection, String baseURL, HashMap<String, Role> roles, Table rolesTable) throws WingException, SQLException {
        Row tableRow;
        if(roles != null){
            //ROLES: show group name instead of role name
            for(String roleId: roles.keySet()){
                Role role = roles.get(roleId);

                if (role.getScope() == Role.Scope.COLLECTION || role.getScope() == Role.Scope.REPOSITORY) {
                    tableRow = rolesTable.addRow(Row.ROLE_DATA);
                    tableRow.addCell(Cell.ROLE_HEADER).addContent(role.getName());
                    Group roleGroup = WorkflowUtils.getRoleGroup(context, thisCollection, role);
                    if (roleGroup != null) {
                        if(role.getScope() == Role.Scope.REPOSITORY){
                            if(authorizeService.isAdmin(context)){
                                tableRow.addCell().addXref(baseURL + "&submit_edit_wf_role_" + roleId, roleGroup.getName());
                            }else{
                                Cell cell = tableRow.addCell();
                                cell.addContent(roleGroup.getName());
                                cell.addHighlight("fade").addContent(T_sysadmins_only_repository_role);
                            }
                        }else{
                            tableRow.addCell().addXref(baseURL + "&submit_edit_wf_role_" + roleId, roleGroup.getName());
                        }

                        if (role.getScope() == Role.Scope.COLLECTION) {
                            addAdministratorOnlyButton(tableRow.addCell(), "submit_delete_wf_role_" + roleId, T_delete);
                        } else {
                            tableRow.addCell();
                        }
                    } else {
                        tableRow.addCell().addContent(T_no_role);
                        if (role.getScope() == Role.Scope.COLLECTION || role.getScope() == Role.Scope.REPOSITORY) {
                            addAdministratorOnlyButton(tableRow.addCell(), "submit_create_wf_role_" + roleId, T_create);
                        } else {
                            tableRow.addCell();
                        }
                    }
                    // help and directions row
                    tableRow = rolesTable.addRow(Row.ROLE_DATA);
                    tableRow.addCell();
                    if (role.getDescription() != null){
                        tableRow.addCell(1,2).addHighlight("fade offset").addContent(role.getDescription());
                    }

                } else {
                    tableRow = rolesTable.addRow(Row.ROLE_DATA);
                    tableRow.addCell(Cell.ROLE_HEADER).addContent(role.getName());

                    tableRow.addCell().addContent(T_no_role);
                    tableRow.addCell();

                    // help and directions row
                    tableRow = rolesTable.addRow(Row.ROLE_DATA);
                    tableRow.addCell();
                    if (role.getDescription() != null){
                        tableRow.addCell(1,2).addHighlight("fade offset").addContent(role.getDescription());
                    }
                }
            }
        }
    }


    private void addAdministratorOnlyButton(Cell cell, String buttonName, Message buttonLabel) throws WingException, SQLException
	{
    	Button button = cell.addButton(buttonName);
    	button.setValue(buttonLabel);
    	if (!authorizeService.isAdmin(context))
    	{
    		// Only admins can create or delete
    		button.setDisabled();
    		cell.addHighlight("fade").addContent(T_sysadmins_only);
    	}
	}
}
