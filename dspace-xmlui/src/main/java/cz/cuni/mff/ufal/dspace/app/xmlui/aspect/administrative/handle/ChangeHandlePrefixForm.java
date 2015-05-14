/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative.handle;

import cz.cuni.mff.ufal.dspace.handle.ConfigurableHandleIdentifierProvider;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Enable administrator to change handle prefix  
 * 
 * @author Michal Josífko
 * modified for LINDAT/CLARIN
 */
public class ChangeHandlePrefixForm extends AbstractDSpaceTransformer   
{		
	
	private static final Message T_dspace_home =
	        message("xmlui.general.dspace_home");	
	private static final Message T_submit_change =
			message("xmlui.administrative.handle.ChangeHandlePrefixForm.change");
	private static final Message T_submit_cancel =
			message("xmlui.general.cancel");
	private static final Message T_handles_trail =
			message("xmlui.administrative.handle.ManageHandlesMain.trail");
	private static final Message T_trail =
			message("xmlui.administrative.handle.ChangeHandlePrefixForm.trail");
	private static final Message T_title =
			message("xmlui.administrative.handle.ChangeHandlePrefixForm.title");
	private static final Message T_head =
			message("xmlui.administrative.handle.ChangeHandlePrefixForm.head");
	private static final Message T_old_prefix =
			message("xmlui.administrative.handle.ChangeHandlePrefixForm.old_prefix");
	private static final Message T_new_prefix =
			message("xmlui.administrative.handle.ChangeHandlePrefixForm.new_prefix");
	private static final Message T_archive_old_handles =
			message("xmlui.administrative.handle.ChangeHandlePrefixForm.archive_old_handles");
	private static final Message T_required_field =
			message("xmlui.administrative.handle.general.required_field");
	private static final Message T_old_prefix_and_new_prefix_must_differ =
			message("xmlui.administrative.handle.ChangeHandlePrefixForm.old_prefix_and_new_prefix_must_differ");
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/handles",T_handles_trail);
        pageMeta.addTrail().addContent(T_trail);
    }
	
	public void addBody(Body body) throws WingException, SQLException 
	{
		Request request = ObjectModelHelper.getRequest(objectModel);
		               
      	// Retrieve attributes from form
    	String oldPrefix = request.getParameter("old_prefix");
        String newPrefix = request.getParameter("new_prefix");        
        boolean archiveOldHandles = (request.getParameter("archive_old_handles") == null)  ? false : true;                       	
	
		String errorString = parameters.getParameter("errors",null);
		ArrayList<String> errors = new ArrayList<String>();
		if (errorString != null)
        {
            for (String error : errorString.split(","))
            {
                errors.add(error);
            }
        }				
		                                              		        	        
		// Change handle prefix div
		Division main = body.addInteractiveDivision("change-handle-prefix-div",contextPath+"/admin/handles",Division.METHOD_POST,"primary administrative handles");		
        main.setHead(T_head);                
        		
		// Change handle prefix form
		List form = main.addList("change-handle-prefix-form",List.TYPE_FORM);			
				
		// Old handle prefix		
		java.util.List<String> prefixes = ConfigurableHandleIdentifierProvider.getPrefixes(
			context);
		Select oldPrefixSelect = form.addItem().addSelect("old_prefix");			
		oldPrefixSelect.setRequired();			
		oldPrefixSelect.setLabel(T_old_prefix);
		for(int i = 0; i < prefixes.size(); i++) {					
			oldPrefixSelect.addOption(prefixes.get(i), prefixes.get(i));
		}		
		oldPrefixSelect.setOptionSelected(oldPrefix);
		if(errors.contains("old_prefix_empty")) {
			oldPrefixSelect.addError(T_required_field);
		}		
		
		// New handle prefix 		
		Text newPrefixText = form.addItem().addText("new_prefix");
		newPrefixText.setRequired();
		newPrefixText.setLabel(T_new_prefix);		
		newPrefixText.setValue(newPrefix);	
		if(errors.contains("new_prefix_empty")) {
			newPrefixText.addError(T_required_field);
		}
		if(errors.contains("old_prefix_equals_new_prefix")) {
			newPrefixText.addError(T_old_prefix_and_new_prefix_must_differ);
		}
		
		// Archive old handles
		CheckBox archiveOldHandlesCheckBox = form.addItem().addCheckBox("archive_old_handles");
		archiveOldHandlesCheckBox.setLabel(T_archive_old_handles);		
		archiveOldHandlesCheckBox.addOption(archiveOldHandles, "yes");
		
		// Action buttons
		Item actions = form.addItem();
		actions.addButton("submit_change").setValue(T_submit_change);
		actions.addButton("submit_cancel").setValue(T_submit_cancel);
		
		// Continuation for cocoon workflow
		main.addHidden("administrative-continue").setValue(knot.getId());
        
   }		
	
}
