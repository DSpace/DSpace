/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.IndexBrowse;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseManager;

import cz.cuni.mff.ufal.dspace.content.Handle;

/**
 * Utility methods to processes actions on Handles. These methods are used
 * exclusively from the administrative flow scripts.
 *
 * @author Michal Josífko
 * modified for LINDAT/CLARIN
 */
public class FlowHandleUtils {
	
    /** log4j category */
    private static Logger log = Logger.getLogger(FlowHandleUtils.class);
    
    private static final Message T_handle_successfully_deleted =
    		new Message("default","xmlui.administrative.handle.FlowHandleUtils.handle_successfully_deleted");
    private static final Message T_handle_successfully_saved =
    		new Message("default","xmlui.administrative.handle.FlowHandleUtils.handle_successfully_saved"); 
    private static final Message T_prefix_successfully_changed =
    		new Message("default","xmlui.administrative.handle.FlowHandleUtils.prefix_successfully_changed"); 
    private static final Message T_handle_deletion_failed =
    		new Message("default","xmlui.administrative.handle.FlowHandleUtils.handle_deletion_failed");
    private static final Message T_handle_saving_failed =
    		new Message("default","xmlui.administrative.handle.FlowHandleUtils.handle_saving_failed"); 
    private static final Message T_prefix_change_failed =
    		new Message("default","xmlui.administrative.handle.FlowHandleUtils.prefix_change_failed");
	
	/**
	 * Save the handle. 
	 * 
	 * If the handleID is -1 then a new handle is created.
	 * 
	 * @param context The current dspace context
	 * @param handleID The handle ID, or -1 for a new handle.
	 * @param url The handle URL 
	 * @param resourceTypeID The type of referenced resource
	 * @param resourceID ID of referenced resource
	 * @return A result
	 */
	public static FlowResult processSaveHandle(Context context, int handleID, String handle, String url, int resourceTypeID, int resourceID,  boolean archiveOldHandle) throws SQLException, AuthorizeException, UIException
	{
		FlowResult result = new FlowResult();
		result.setParameter("handle_id", handleID);		
		
		result.setContinue(false);
		result.setOutcome(false);

		// If we have errors, the form needs to be resubmitted to fix those problems
	    if (StringUtils.isEmpty(handle)) {
            result.addError("handle_empty");
        }
		if (resourceTypeID == -1 && resourceID == -1 && StringUtils.isEmpty(url)) {
            result.addError("url_empty");
        }								
		else if(StringUtils.isEmpty(url)) {
			if (resourceTypeID == -1) {		        
				result.addError("resource_type_id_empty");
			}
			if (resourceID == -1) {		        
				result.addError("resource_id_empty");
			}
		}		
		
		if(result.getErrors() == null) {
			
			try {
					
				Handle h = null;
				if (handleID == -1) {
					h = Handle.create(context, null, handle);			
				}
				else {				
					h = Handle.find(context, handleID);		
					if(h.getHandle() != handle) {
						HandleManager.changeHandle(context, h.getHandle(), handle, archiveOldHandle);
					}
					
				}			
						
				h.setHandle(handle);		
				h.setURL(url);
				h.setResourceTypeID(resourceTypeID);
				h.setResourceID(resourceID);
				h.update();
						
				context.commit();
							
				result.setContinue(true);
				result.setOutcome(true);
				result.setMessage(T_handle_successfully_saved);
			}
			catch(Exception e) {
				result.setMessage(T_handle_saving_failed);
				log.error(e.getMessage());
				context.abort();
			}
		}
		
		return result;
	}
	
	/**
	 * Change handle prefix. It is assumed that the user has already confirmed this selection.
	 * 
	 * @param context The current DSpace context.
	 * @param oldPrefix The prefix to be replace.
	 * @param newPrefix The prefix to be used.
	 * @param archiveOldHandles Should the former handles be archived?
	 * @return A results object.
	 */
	public static FlowResult changeHandlePrefix(Context context, String oldPrefix, String newPrefix, boolean archiveOldHandles) throws SQLException, AuthorizeException, IOException
	{
		FlowResult result = new FlowResult();		

   		result.setContinue(false);
    	result.setOutcome(false);		
		
		// If we have errors, the form needs to be resubmitted to fix those problems
	    if (StringUtils.isEmpty(oldPrefix)) {
            result.addError("old_prefix_empty");
        }
		if (StringUtils.isEmpty(newPrefix)) {
            result.addError("new_prefix_empty");
        }											
		if (result.getErrors() == null && oldPrefix.equals(newPrefix)) {
            result.addError("old_prefix_equals_new_prefix");            
        }
		
		if(result.getErrors() == null) {		
			try {
				// change prefixes
				HandleManager.changePrefix(context, oldPrefix, newPrefix, archiveOldHandles);
				context.commit();
				
				// reindex
				IndexBrowse.main(new String[] {"-i"});
				
				result.setContinue(true);
		    	result.setOutcome(true);
				result.setMessage(T_prefix_successfully_changed);
				
			} catch (Exception e) {
				result.setMessage(T_prefix_change_failed);
				log.error(e.getMessage());
				context.abort();
			}		
		}
    	
    	return result;
	}
	
	/**
	 * Delete the specified handle. It is assumed that the user has already confirmed this selection.
	 * 
	 * @param context The current DSpace context.
	 * @param handleID ID of handle to be removed.
	 * @return A results object.
	 */
	public static FlowResult processDeleteHandle(Context context, int handleID) throws SQLException, AuthorizeException, IOException
	{
		FlowResult result = new FlowResult();	
		
		result.setContinue(true);
    	result.setOutcome(true);
		result.setMessage(T_handle_deletion_failed);
		
		try {
			
			Handle handleDeleted = Handle.find(context, handleID);
			
			HandleManager.changeHandle(context, handleDeleted.getHandle(), null, false);
				   		   		
	   		handleDeleted.delete();    		
	   		
	   		context.commit();
 
	   		result.setContinue(true);
	    	result.setOutcome(true);
			result.setMessage(T_handle_successfully_deleted);
		}
		catch(Exception e) {
			log.error(e.getMessage());
			context.abort();
		}
    	
    	return result;
	}
		
}
