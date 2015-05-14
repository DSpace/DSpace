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

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Constants;
import org.dspace.handle.HandleManager;

import cz.cuni.mff.ufal.dspace.handle.Handle;

/**
 * Present the user with a soon-to-be-deleted Handle. 
 * If the user clicks confirm deletion then they will be 
 * deleted otherwise they will be spared the wrath of deletion.
 * 
 * @author Michal Josífko
 * modified for LINDAT/CLARIN
 */
public class DeleteHandleConfirm extends AbstractDSpaceTransformer   
{
	/** Language Strings */
	private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");		
	private static final Message T_submit_confirm =
		message("xmlui.general.delete");
	private static final Message T_submit_cancel =
		message("xmlui.general.cancel");
	private static final Message T_handles_trail =
			message("xmlui.administrative.handle.ManageHandlesMain.trail");
	private static final Message T_title =
			message("xmlui.administrative.handle.DeleteHandleConfirm.title");
	private static final Message T_trail =
			message("xmlui.administrative.handle.DeleteHandleConfirm.trail");
	private static final Message T_head =
			message("xmlui.administrative.handle.DeleteHandleConfirm.head");
	private static final Message T_help =
			message("xmlui.administrative.handle.DeleteHandleConfirm.help");
	private static final Message T_yes =
			message("xmlui.administrative.handle.general.yes");
	private static final Message T_no =
			message("xmlui.administrative.handle.general.no");
	private static final Message T_handle =
			message("xmlui.administrative.handle.general.handle");
	private static final Message T_internal =
			message("xmlui.administrative.handle.general.internal");
	private static final Message T_url =
			message("xmlui.administrative.handle.general.url");
	private static final Message T_resource_type =
			message("xmlui.administrative.handle.general.resource_type");	
	private static final Message T_resource_id =
			message("xmlui.administrative.handle.general.resource_id");
	
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/handles",T_handles_trail);
        pageMeta.addTrail().addContent(T_trail);
    }

	
	public void addBody(Body body) throws WingException, SQLException, AuthorizeException
	{
		int id = parameters.getParameterAsInteger("handle_id", -1);
		
		String errorString = parameters.getParameter("errors",null);
		ArrayList<String> errors = new ArrayList<String>();
		if (errorString != null)
        {
            for (String error : errorString.split(","))
            {
                errors.add(error);
            }
        }
		
		Handle h = Handle.find(context, id);
     
    	Division deleted = body.addInteractiveDivision("handle-confirm-delete",
    			contextPath+"/admin/handles",Division.METHOD_POST,"alert alert-danger");
    	deleted.setHead(T_head);
    	deleted.addPara(T_help);
    	
    	// Table of handles
		Table htable = deleted.addTable("handle-list-table", 1, 5);

		// Table headers
		Row hhead = htable.addRow(Row.ROLE_HEADER);
		
		hhead.addCellContent(T_handle);
		hhead.addCellContent(T_internal);
		hhead.addCellContent(T_url);
		hhead.addCellContent(T_resource_type);
		hhead.addCellContent(T_resource_id);
            	
		Row hrow = htable.addRow(null, Row.ROLE_DATA, null);

        if (h.getHandle() != null && !h.getHandle().isEmpty())
        {
            hrow.addCell().addXref(
                    HandleManager.getCanonicalForm(h.getHandle()),
                    h.getHandle(), "target_blank");
        }
        else
        {
            hrow.addCell().addContent(h.getHandle());
        }
        hrow.addCell().addContent(h.isInternalResource() ? T_yes : T_no);
        if (h.getHandle() != null && !h.getHandle().isEmpty())
        {
            String resolvedURL = HandleManager.resolveToURL(context,
                    h.getHandle());
            hrow.addCell().addXref(resolvedURL, resolvedURL, "target_blank");
        }
        else
        {
            hrow.addCell().addContent(h.getHandle());
        }
		String resourceType = "";
		if(h.getResourceTypeID() >= 0) {
			resourceType = Constants.typeText[h.getResourceTypeID()];			
		}		
		hrow.addCell().addContent(resourceType);
		String resourceID = "";
		if(h.getResourceID() >= 0) {
			resourceID = String.valueOf(h.getResourceID());			
		}
		hrow.addCell().addContent(resourceID);	    
    	
    	Para buttons = deleted.addPara();
    	buttons.addButton("submit_confirm").setValue(T_submit_confirm);
    	buttons.addButton("submit_cancel").setValue(T_submit_cancel);
    	
    	deleted.addHidden("administrative-continue").setValue(knot.getId());
    }
}
