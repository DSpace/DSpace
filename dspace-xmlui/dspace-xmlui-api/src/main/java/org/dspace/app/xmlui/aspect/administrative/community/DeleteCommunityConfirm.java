/*
 * DeleteCommunityConfirm.java
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
package org.dspace.app.xmlui.aspect.administrative.community;

import java.sql.SQLException;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;

/**
 * Confirmation step for the deletion of an entire community
 * @author Alexey Maslov
 */
public class DeleteCommunityConfirm extends AbstractDSpaceTransformer   
{
	/** Language Strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	
	private static final Message T_title = message("xmlui.administrative.community.DeleteCommunityConfirm.title");
	private static final Message T_trail = message("xmlui.administrative.community.DeleteCommunityConfirm.trail");

	private static final Message T_main_head = message("xmlui.administrative.community.DeleteCommunityConfirm.main_head");

	private static final Message T_main_para = message("xmlui.administrative.community.DeleteCommunityConfirm.main_para");
	private static final Message T_confirm_item1 = message("xmlui.administrative.community.DeleteCommunityConfirm.confirm_item1");
	private static final Message T_confirm_item2 = message("xmlui.administrative.community.DeleteCommunityConfirm.confirm_item2");
	private static final Message T_confirm_item3 = message("xmlui.administrative.community.DeleteCommunityConfirm.confirm_item3");
	private static final Message T_confirm_item4 = message("xmlui.administrative.community.DeleteCommunityConfirm.confirm_item4");

	private static final Message T_submit_confirm = message("xmlui.general.delete");
	private static final Message T_submit_cancel = message("xmlui.general.cancel");
	
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);
    }
	
	public void addBody(Body body) throws WingException, SQLException, AuthorizeException
	{
		int communityID = parameters.getParameterAsInteger("communityID", -1);
		Community thisCommunity = Community.find(context, communityID);
		
		
		// DIVISION: main
	    Division main = body.addInteractiveDivision("community-confirm-delete",contextPath+"/admin/community",Division.METHOD_POST,"primary administrative community");
	    main.setHead(T_main_head.parameterize(communityID));
	    main.addPara(T_main_para.parameterize(thisCommunity.getMetadata("name")));	    
	    List deleteConfirmHelp = main.addList("consequences",List.TYPE_BULLETED);
	    deleteConfirmHelp.addItem(T_confirm_item1);
	    deleteConfirmHelp.addItem(T_confirm_item2);
	    deleteConfirmHelp.addItem(T_confirm_item3);
	    deleteConfirmHelp.addItem(T_confirm_item4);
	    
	    Para buttonList = main.addPara();
	    buttonList.addButton("submit_confirm").setValue(T_submit_confirm);
	    buttonList.addButton("submit_cancel").setValue(T_submit_cancel);
	    
    	main.addHidden("administrative-continue").setValue(knot.getId());
    }
}
