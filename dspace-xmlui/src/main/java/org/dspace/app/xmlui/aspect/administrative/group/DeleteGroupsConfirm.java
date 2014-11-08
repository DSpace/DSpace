/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.group;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

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
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;

/**
 * Present the user with a list of soon-to-be-deleted Groups. 
 * If the user clicks confirm deletion then they will be 
 * deleted otherwise they will be spared the wrath of deletion.
 * @author Scott Phillips
 */
public class DeleteGroupsConfirm extends AbstractDSpaceTransformer   
{
	/** Language Strings */
	private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
	private static final Message T_group_trail =
		message("xmlui.administrative.group.general.group_trail");
	private static final Message T_title =
		message("xmlui.administrative.group.DeleteGroupsConfirm.title");
	private static final Message T_trail =
		message("xmlui.administrative.group.DeleteGroupsConfirm.trail");
	private static final Message T_head =
		message("xmlui.administrative.group.DeleteGroupsConfirm.head");
	private static final Message T_para =
		message("xmlui.administrative.group.DeleteGroupsConfirm.para");
	private static final Message T_column1 =
		message("xmlui.administrative.group.DeleteGroupsConfirm.column1");
	private static final Message T_column2 =
		message("xmlui.administrative.group.DeleteGroupsConfirm.column2");
	private static final Message T_column3 =
		message("xmlui.administrative.group.DeleteGroupsConfirm.column3");
	private static final Message T_column4 =
		message("xmlui.administrative.group.DeleteGroupsConfirm.column4");
	private static final Message T_submit_confirm =
		message("xmlui.general.delete");
	private static final Message T_submit_cancel =
		message("xmlui.general.cancel");

	protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/groups",T_group_trail);
        pageMeta.addTrail().addContent(T_trail);
    }
	
	public void addBody(Body body) throws WingException, SQLException, AuthorizeException
	{
		String idsString = parameters.getParameter("groupIDs", null);
		
		ArrayList<Group> groups = new ArrayList<Group>();
		for (String id : idsString.split(","))
		{
			Group group = groupService.find(context, UUID.fromString(id));
			groups.add(group);
		}
     
    	Division deleted = body.addInteractiveDivision("group-confirm-delete",
    			contextPath+"/admin/epeople",Division.METHOD_POST,"primary administrative groups");
    	deleted.setHead(T_head);
    	deleted.addPara(T_para);
    	
    	Table table = deleted.addTable("groups-list",groups.size() + 1, 3);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_column1);
        header.addCell().addContent(T_column2);
        header.addCell().addContent(T_column3);
        header.addCell().addContent(T_column4);
        
    	for (Group group : groups) 
    	{	
    		Row row = table.addRow();
    		row.addCell().addContent(group.getID().toString());
        	row.addCell().addContent(group.getName());
        	row.addCell().addContent(group.getMembers().size());
        	row.addCell().addContent(group.getMemberGroups().size());
	    }
    	
    	Para buttons = deleted.addPara();
    	buttons.addButton("submit_confirm").setValue(T_submit_confirm);
    	buttons.addButton("submit_cancel").setValue(T_submit_cancel);
    	
    	deleted.addHidden("administrative-continue").setValue(knot.getId());
    }
}
