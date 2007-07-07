/*
 * DeleteEPeopleConfirm.java
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
package org.dspace.app.xmlui.aspect.administrative.eperson;

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
import org.dspace.eperson.EPerson;

/**
 * Present the user with a list of not-yet-but-soon-to-be-deleted-epeople.
 * 
 * @author Alexey Maslov
 */
public class DeleteEPeopleConfirm extends AbstractDSpaceTransformer   
{
	/** Language Strings */
	private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
	
	private static final Message T_eperson_trail =
		message("xmlui.administrative.eperson.general.epeople_trail");
	
	private static final Message T_title =
		message("xmlui.administrative.eperson.DeleteEPeopleConfirm.title");
	
	private static final Message T_trail =
		message("xmlui.administrative.eperson.DeleteEPeopleConfirm.trail");
	
	private static final Message T_confirm_head =
		message("xmlui.administrative.eperson.DeleteEPeopleConfirm.confirm_head");
	
	private static final Message T_confirm_para =
		message("xmlui.administrative.eperson.DeleteEPeopleConfirm.confirm_para");
	
	private static final Message T_head_id =
		message("xmlui.administrative.eperson.DeleteEPeopleConfirm.head_id");
	
	private static final Message T_head_name =
		message("xmlui.administrative.eperson.DeleteEPeopleConfirm.head_name");
	
	private static final Message T_head_email =
		message("xmlui.administrative.eperson.DeleteEPeopleConfirm.head_email");
	
	private static final Message T_submit_confirm =
		message("xmlui.administrative.eperson.DeleteEPeopleConfirm.submit_confirm");
	
	private static final Message T_submit_cancel =
		message("xmlui.general.cancel");
	
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/epeople",T_eperson_trail);
        pageMeta.addTrail().addContent(T_trail);
    }
	
	public void addBody(Body body) throws WingException, SQLException, AuthorizeException
	{
		// Get all our parameters
		String idsString = parameters.getParameter("epeopleIDs", null);
		
		ArrayList<EPerson> epeople = new ArrayList<EPerson>();
		for (String id : idsString.split(","))
		{
			EPerson person = EPerson.find(context,Integer.valueOf(id));
			epeople.add(person);
		}
 
		// DIVISION: epeople-confirm-delete
    	Division deleted = body.addInteractiveDivision("epeople-confirm-delete",contextPath+"/admin/epeople",Division.METHOD_POST,"primary administrative eperson");
    	deleted.setHead(T_confirm_head);
    	deleted.addPara(T_confirm_para);
    	
    	Table table = deleted.addTable("epeople-confirm-delete",epeople.size() + 1, 1);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_head_id);
        header.addCell().addContent(T_head_name);
        header.addCell().addContent(T_head_email);
    	
    	for (EPerson eperson : epeople) 
    	{
    		Row row = table.addRow();
    		row.addCell().addContent(eperson.getID());
        	row.addCell().addContent(eperson.getFullName());
        	row.addCell().addContent(eperson.getEmail());
	    }
    	Para buttons = deleted.addPara();
    	buttons.addButton("submit_confirm").setValue(T_submit_confirm);
    	buttons.addButton("submit_cancel").setValue(T_submit_cancel);
    	
    	deleted.addHidden("administrative-continue").setValue(knot.getId());
    }
}
