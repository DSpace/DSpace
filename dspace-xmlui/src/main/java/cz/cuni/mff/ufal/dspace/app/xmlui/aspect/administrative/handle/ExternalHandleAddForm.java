/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative.handle;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;

import java.sql.SQLException;

/**
 */
public class ExternalHandleAddForm extends AbstractDSpaceTransformer
{
	/** Language Strings */
	private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
	private static final Message T_submit_cancel =
			message("xmlui.general.cancel");
	private static final Message T_submit_confirm =
		message("xmlui.general.next");
	private static final Message T_handles_trail =
			message("xmlui.administrative.handle.ManageHandlesMain.trail");
	private static final Message T_title =
			message("xmlui.administrative.handle.HandeAddForm.title");
	private static final Message T_trail =
			message("xmlui.administrative.handle.HandeAddForm.trail");
	private static final Message T_head =
			message("xmlui.administrative.handle.HandeAddForm.head");
	private static final Message T_help =
			message("xmlui.administrative.handle.HandeAddForm.help");
	private static final Message T_url_help =
			message("xmlui.administrative.handle.HandeAddForm.url_help");
	private static final Message T_url_label =
			message("xmlui.administrative.handle.HandeAddForm.url_label");
	private static final Message T_handle_help =
			message("xmlui.administrative.handle.HandeAddForm.handle_help");
	private static final Message T_handle_label =
			message("xmlui.administrative.handle.HandeAddForm.handle_label");


	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/handles",T_handles_trail);
        pageMeta.addTrail().addContent(T_trail);
    }

	
	public void addBody(Body body) throws WingException, SQLException, AuthorizeException
	{
    	Division prefix = body.addInteractiveDivision("handle-add",
    			contextPath+"/admin/handles",Division.METHOD_POST,"alert alert-info");
    	prefix.setHead(T_head);
    	prefix.addPara(T_help);

		List list = prefix.addList("handle-add-list", List.TYPE_FORM);
		Text text = list.addItem().addText("url");
		text.setLabel(T_url_label);
		text.setHelp(T_url_help);
		text = list.addItem().addText("handle_id");
		text.setLabel(T_handle_label);
		text.setHelp(T_handle_help);
		list.addItem().addButton("submit_confirm").setValue(T_submit_confirm);
		list.addItem().addButton("submit_cancel").setValue(T_submit_cancel);
		prefix.addHidden(ManageExternalHandles.EDIT_EXTERNAL).setValue(ManageExternalHandles.EDIT_EXTERNAL);
    	prefix.addHidden("administrative-continue").setValue(knot.getId());
    }
}
