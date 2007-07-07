/*
 * AuthorizationMain.java
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
package org.dspace.app.xmlui.aspect.administrative.authorization;

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
import org.dspace.authorize.ResourcePolicy;

/**
 * @author Alexey Maslov
 */
public class DeletePoliciesConfirm extends AbstractDSpaceTransformer   
{	
	/** Language Strings */
	private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
	private static final Message T_title =
		message("xmlui.administrative.authorization.DeletePoliciesConfirm.title");
	private static final Message T_trail =
		message("xmlui.administrative.authorization.DeletePoliciesConfirm.trail");
	private static final Message T_authorize_trail =
		message("xmlui.administrative.authorization.general.authorize_trail");
	private static final Message T_policyList_trail =
		message("xmlui.administrative.authorization.general.policyList_trail");
	
	private static final Message T_confirm_head =
		message("xmlui.administrative.authorization.DeletePoliciesConfirm.confirm_head");
	private static final Message T_confirm_para =
		message("xmlui.administrative.authorization.DeletePoliciesConfirm.confirm_para");
	
	private static final Message T_head_id =
		message("xmlui.administrative.authorization.DeletePoliciesConfirm.head_id");
	private static final Message T_head_action =
		message("xmlui.administrative.authorization.DeletePoliciesConfirm.head_action");
	private static final Message T_head_group =
		message("xmlui.administrative.authorization.DeletePoliciesConfirm.head_group");
	
	private static final Message T_submit_confirm =
		message("xmlui.general.delete");
	private static final Message T_submit_cancel =
		message("xmlui.general.cancel");
	
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/authorize", T_authorize_trail);
        pageMeta.addTrail().addContent(T_policyList_trail);
        pageMeta.addTrail().addContent(T_trail);
    }
		
	public void addBody(Body body) throws WingException, SQLException 
	{
		// Get all our parameters
		String idsString = parameters.getParameter("policyIDs", null);
		
		ArrayList<ResourcePolicy> policies = new ArrayList<ResourcePolicy>();
		for (String id : idsString.split(","))
		{
			ResourcePolicy policy = ResourcePolicy.find(context,Integer.valueOf(id));
			policies.add(policy);
		}
 
		// DIVISION: policies-confirm-delete
    	Division deleted = body.addInteractiveDivision("policies-confirm-delete",contextPath+"/admin/authorize",Division.METHOD_POST,"primary administrative authorization");
    	deleted.setHead(T_confirm_head);
    	deleted.addPara(T_confirm_para);
    	
    	Table table = deleted.addTable("policies-confirm-delete",policies.size() + 1, 4);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_head_id);
        header.addCell().addContent(T_head_action);
        header.addCell().addContent(T_head_group);
    	
    	for (ResourcePolicy policy : policies) 
    	{
    		Row row = table.addRow();
    		row.addCell().addContent(policy.getID());
        	row.addCell().addContent(policy.getActionText());
        	if (policy.getGroup() != null) row.addCell().addContent(policy.getGroup().getName());
        	else row.addCell().addContent("...");
	    }
    	Para buttons = deleted.addPara();
    	buttons.addButton("submit_confirm").setValue(T_submit_confirm);
    	buttons.addButton("submit_cancel").setValue(T_submit_cancel);
    	
    	deleted.addHidden("administrative-continue").setValue(knot.getId());
   }
}
