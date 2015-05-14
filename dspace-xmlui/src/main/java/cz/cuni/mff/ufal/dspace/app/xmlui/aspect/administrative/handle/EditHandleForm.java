/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative.handle;

import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Hidden;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.core.Constants;
//Can't use isEmpty in spring 3.1.1
//import org.springframework.util.StringUtils;
//use apache.commons instead
import org.apache.commons.lang3.StringUtils;

import cz.cuni.mff.ufal.dspace.handle.Handle;

/**
 * Enable administrator to edit handle  
 * 
 * @author Michal Josífko
 * modified for LINDAT/CLARIN
 */

public class EditHandleForm extends AbstractDSpaceTransformer   
{		
		
	private static final Message T_submit_save =
			message("xmlui.general.save");
	private static final Message T_submit_cancel =
			message("xmlui.general.cancel");
	private static final Message T_dspace_home =
			message("xmlui.general.dspace_home");
	private static final Message T_handles_trail =
			message("xmlui.administrative.handle.ManageHandlesMain.trail");
	private static final Message T_new_handle_title =
			message("xmlui.administrative.handle.EditHandleForm.new_handle_title");
	private static final Message T_edit_handle_title =
			message("xmlui.administrative.handle.EditHandleForm.edit_handle_title");
	private static final Message T_new_handle_trail =
			message("xmlui.administrative.handle.EditHandleForm.new_handle_trail");
	private static final Message T_edit_handle_trail =
			message("xmlui.administrative.handle.EditHandleForm.edit_handle_trail");
	private static final Message T_new_handle_head =
			message("xmlui.administrative.handle.EditHandleForm.new_handle_head");
	private static final Message T_edit_handle_head =
			message("xmlui.administrative.handle.EditHandleForm.edit_handle_head");
	private static final Message T_archive_old_handle =
			message("xmlui.administrative.handle.EditHandleForm.archive_old_handle");
	private static final Message T_handle =
			message("xmlui.administrative.handle.general.handle");
	private static final Message T_url =
			message("xmlui.administrative.handle.general.url");
	private static final Message T_resource_type =
			message("xmlui.administrative.handle.general.resource_type");
	private static final Message T_resource_id =
			message("xmlui.administrative.handle.general.resource_id");
	private static final Message T_required_field =
			message("xmlui.administrative.handle.general.required_field");
	
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
		int id = parameters.getParameterAsInteger("handle_id",-1);		
		
		if (id < 0) {
			pageMeta.addMetadata("title").addContent(T_new_handle_title);
			pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
			pageMeta.addTrailLink(contextPath + "/admin/handles",T_handles_trail);
        	pageMeta.addTrail().addContent(T_new_handle_trail);        	
        }
        else {
        	pageMeta.addMetadata("title").addContent(T_edit_handle_title);
        	pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        	pageMeta.addTrailLink(contextPath + "/admin/handles",T_handles_trail);
        	pageMeta.addTrail().addContent(T_edit_handle_trail);
        }		                                        
    }
	
	
	public void addBody(Body body) throws WingException, SQLException 
	{
		Request request = ObjectModelHelper.getRequest(objectModel);
		
		// Get our parameters		
		int id = parameters.getParameterAsInteger("handle_id",-1);
		boolean archiveOldHandle;
        boolean submitted = request.getParameter("submit_save") != null;
        
    	String errorString = parameters.getParameter("errors",null);
		ArrayList<String> errors = new ArrayList<String>();
		if (errorString != null)
        {
            for (String error : errorString.split(","))
            {
                errors.add(error);
            }
        }

		Handle h = null;

        // Find handle
		if (id > 0) {
			h = Handle.find(context, id);
        }		
		
		String handle;        
        String url;        
        int resourceTypeID;
        int resourceID;
        
        // Retrieve handle attributes
        if (!submitted && h != null) {
        	// Retrieve handle attributes from database record 
        	handle = h.getHandle();        
            url = h.getURL();
            resourceTypeID = h.getResourceTypeID();
            resourceID = h.getResourceID();
            archiveOldHandle = false;
        }
        else {
        	// Retrieve handle attributes from form
        	handle = request.getParameter("handle");        
            url = request.getParameter("url");
            
            String resourceTypeIDParam = request.getParameter("resource_type_id");        
            resourceTypeID = StringUtils.isEmpty(resourceTypeIDParam) ? -1 : Integer.valueOf(resourceTypeIDParam);
            
            String resourceIDParam = request.getParameter("resource_id");
            resourceID = StringUtils.isEmpty(resourceIDParam) ? -1 : Integer.valueOf(resourceIDParam);
            
            archiveOldHandle = (request.getParameter("archive_old_handle") == null)  ? false : true;
        }              
		                                              		        	        
		// Handle div
		Division main = body.addInteractiveDivision("edit-handle-div",contextPath+"/admin/handles",Division.METHOD_POST,"primary administrative handles");
		if (id == -1)
        {
            main.setHead(T_new_handle_head);
        }
		else
        {
            main.setHead(T_edit_handle_head);
        }		
	
		// Handle form
		List form = main.addList("edit-handle-form",List.TYPE_FORM);			
		
		// Handle input - always editable
		Text handleText = form.addItem().addText("handle");
		handleText.setRequired();
		handleText.setLabel(T_handle);		
		handleText.setValue(handle);
		handleText.setSize(64);	
		if(errors.contains("handle_empty")) {
			handleText.addError(T_required_field);
		}
		
		// URL input - only for (new) external handles
		if(h == null || !h.isInternalResource()) {			
			Text urlText = form.addItem().addText("url");
			urlText.setRequired();
			urlText.setLabel(T_url);
			urlText.setValue(url);
			urlText.setSize(128);
			if(errors.contains("url_empty")) {
				urlText.addError(T_required_field);
			}
		}

		// Resource Type ID input - only for existing internal handles and not editable
		if(h != null && h.isInternalResource()) { 
			Hidden resourceTypeIDHidden = form.addItem().addHidden("resource_type_id");
			resourceTypeIDHidden.setValue(resourceTypeID);
			
			Text resourceTypeIDText = form.addItem().addText("resource_type_id_text");
			resourceTypeIDText.setRequired();			
			resourceTypeIDText.setLabel(T_resource_type);				
			if(resourceTypeID >= 0) {
				resourceTypeIDText.setValue(Constants.typeText[resourceTypeID]);
			}
			resourceTypeIDText.setDisabled();		
			if(errors.contains("resource_type_id_empty")) {
				resourceTypeIDText.addError(T_required_field);
			}
		}
		
		// Resource ID input - only for existing internal handles and not editable 
		if(h != null && h.isInternalResource()) {
			Text resourceIDText = form.addItem().addText("resource_id");
			resourceIDText.setRequired();
			resourceIDText.setLabel(T_resource_id);			
			if(resourceID >= 0) {
				resourceIDText.setValue(String.valueOf(resourceID));
			}
			resourceIDText.setDisabled();
			if(errors.contains("resource_id_empty")) {
				resourceIDText.addError(T_required_field);
			}
		}
		
		if(h != null) {
			// Archive old handles
			CheckBox archiveOldHandleCheckBox = form.addItem().addCheckBox("archive_old_handle");
			archiveOldHandleCheckBox.setLabel(T_archive_old_handle);		
			archiveOldHandleCheckBox.addOption(archiveOldHandle, "yes");
		}
		
		// Action buttons
		Item actions = form.addItem();
		actions.addButton("submit_save").setValue(T_submit_save);
		actions.addButton("submit_cancel").setValue(T_submit_cancel);
		
		// Continuation for cocoon workflow
		main.addHidden("administrative-continue").setValue(knot.getId());
        
   }
	
}
