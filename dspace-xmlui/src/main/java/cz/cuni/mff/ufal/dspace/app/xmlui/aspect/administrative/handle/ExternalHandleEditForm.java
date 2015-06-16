/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative.handle;

import cz.cuni.mff.ufal.dspace.PIDService;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.hibernate.type.ListType;

import java.sql.SQLException;

/**
 */
public class ExternalHandleEditForm extends AbstractDSpaceTransformer
{
	private static Logger log = cz.cuni.mff.ufal.Logger.getLogger(ExternalHandleEditForm.class);
	/** Language Strings */
	private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");		
	private static final Message T_submit_confirm =
		message("xmlui.general.next");
	private static final Message T_submit_cancel =
			message("xmlui.general.cancel");
	private static final Message T_handles_trail =
			message("xmlui.administrative.handle.ManageHandlesMain.trail");
	private static final Message T_title =
			message("xmlui.administrative.handle.HandleEditForm.title");
	private static final Message T_trail =
			message("xmlui.administrative.handle.HandleEditForm.trail");
	private static final Message T_head =
			message("xmlui.administrative.handle.HandleEditForm.head");
	private static final Message T_help_delete =
			message("xmlui.administrative.handle.HandleEditForm.help_delete");
	private static final Message T_help_edit =
			message("xmlui.administrative.handle.HandleEditForm.help_edit");
	private static final Message T_handle_label =
			message("xmlui.administrative.handle.HandleEditForm.handle_label");
	private static final Message T_url_label =
			message("xmlui.administrative.handle.HandleEditForm.url_label");


	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/handles",T_handles_trail);
        pageMeta.addTrail().addContent(T_trail);
    }

	
	public void addBody(Body body) throws WingException, SQLException, AuthorizeException
	{

		String PID = parameters.getParameter("handle_id", null);
		boolean isDelete = parameters.getParameterAsBoolean("isDelete", false);
		Division prefix = body.addInteractiveDivision("handle-prefix",
				contextPath+"/admin/handles",Division.METHOD_POST,"alert alert-info");
		prefix.setHead(T_head);
		if(isDelete) {
			prefix.addPara(T_help_delete);
		}else{
			prefix.addPara(T_help_edit);
		}
		List list = prefix.addList("handle-prefix-list", List.TYPE_FORM);
		Text text = null;
		text = list.addItem().addText("handel_id");
		text.setLabel(T_handle_label);
		text.setDisabled();
		text.setValue(PID);
		try {
			String url = PIDService.resolvePID(PID);
			text = list.addItem().addText("url");
			text.setLabel(T_url_label);
			text.setValue(url);
			if(isDelete) {
				text.setDisabled();
			}
		} catch (Exception e) {
			log.error(e);
			throw new WingException(e);
		}

		list.addItem().addButton("submit_confirm").setValue(T_submit_confirm);
		list.addItem().addButton("submit_cancel").setValue(T_submit_cancel);
		prefix.addHidden(ManageExternalHandles.EDIT_EXTERNAL).setValue(ManageExternalHandles.EDIT_EXTERNAL);
    	prefix.addHidden("administrative-continue").setValue(knot.getId());
    }
}
