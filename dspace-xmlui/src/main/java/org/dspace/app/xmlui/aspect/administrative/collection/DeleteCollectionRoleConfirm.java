/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.collection;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.authorize.AuthorizeException;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;

/**
 * Confirmation step for the deletion a collection's role
 * @author Alexey Maslov
 */
public class DeleteCollectionRoleConfirm extends AbstractDSpaceTransformer   
{
	/** Language Strings */
	private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
	
	private static final Message T_title = message("xmlui.administrative.collection.DeleteCollectionRoleConfirm.title");
	private static final Message T_trail = message("xmlui.administrative.collection.DeleteCollectionRoleConfirm.trail");

	private static final Message T_main_head = message("xmlui.administrative.collection.DeleteCollectionRoleConfirm.main_head");
	private static final Message T_main_para_read = message("xmlui.administrative.collection.DeleteCollectionRoleConfirm.main_para_read");
	private static final Message T_main_para = message("xmlui.administrative.collection.DeleteCollectionRoleConfirm.main_para");

	private static final Message T_submit_confirm = message("xmlui.general.delete");
	private static final Message T_submit_cancel = message("xmlui.general.cancel");
	
	protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);
    }
	
	public void addBody(Body body) throws WingException, SQLException, AuthorizeException
	{
		String role = parameters.getParameter("role", null);
		UUID groupID = UUID.fromString(parameters.getParameter("groupID", null));
		Group toBeDeleted = groupService.find(context, groupID);
		
		
		// DIVISION: main
	    Division main = body.addInteractiveDivision("collection-role-delete",contextPath+"/admin/collection",Division.METHOD_POST,"primary administrative collection");
	    main.setHead(T_main_head.parameterize(role));
	    // Different help message for the default read group to enforce its non-retroactive nature
	    if ("DEFAULT_READ".equals(role))
        {
            main.addPara(T_main_para_read.parameterize(toBeDeleted.getName()));
        }
	    else
        {
            main.addPara(T_main_para.parameterize(toBeDeleted.getName()));
        }
	    
	    Para buttonList = main.addPara();
	    buttonList.addButton("submit_confirm").setValue(T_submit_confirm);
	    buttonList.addButton("submit_cancel").setValue(T_submit_cancel);
	    	    
	    
    	main.addHidden("administrative-continue").setValue(knot.getId());
    }
}
