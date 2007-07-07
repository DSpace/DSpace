/*
 * DeleteGroupsConfirm.java
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
package org.dspace.app.xmlui.aspect.administrative.group;

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
import org.dspace.eperson.Group;

/**
 * Present the user with a list of soon-to-be-deleted Groups. 
 * If the user clicks confirm deletition then they will be 
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
			Group group = Group.find(context,Integer.valueOf(id));
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
    		row.addCell().addContent(group.getID());
        	row.addCell().addContent(group.getName());
        	row.addCell().addContent(group.getMembers().length);
        	row.addCell().addContent(group.getMemberGroups().length);
	    }
    	
    	Para buttons = deleted.addPara();
    	buttons.addButton("submit_confirm").setValue(T_submit_confirm);
    	buttons.addButton("submit_cancel").setValue(T_submit_cancel);
    	
    	deleted.addHidden("administrative-continue").setValue(knot.getId());
    }
}
