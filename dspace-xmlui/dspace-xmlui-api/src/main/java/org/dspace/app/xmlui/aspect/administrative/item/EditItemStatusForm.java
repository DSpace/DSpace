/*
 * EditItemStatus.java
 *
 * Version: $Revision: 1.3 $
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
package org.dspace.app.xmlui.aspect.administrative.item;

import java.sql.SQLException;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;

/**
 * Display basic meta-meta information about the item and allow the user to change 
 * it's state such as withdraw or reinstate, possibily even completely deleting the item!
 * 
 * @author Jay Paz
 * @author Scott Phillips
 */

public class EditItemStatusForm extends AbstractDSpaceTransformer {
	
	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_submit_return = message("xmlui.general.return");
	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");
	private static final Message T_option_head = message("xmlui.administrative.item.general.option_head");
	private static final Message T_option_status = message("xmlui.administrative.item.general.option_status");
	private static final Message T_option_bitstreams = message("xmlui.administrative.item.general.option_bitstreams");
	private static final Message T_option_metadata = message("xmlui.administrative.item.general.option_metadata");
	private static final Message T_option_view = message("xmlui.administrative.item.general.option_view");

	
	private static final Message T_title = message("xmlui.administrative.item.EditItemStatusForm.title");
	private static final Message T_trail = message("xmlui.administrative.item.EditItemStatusForm.trail");
	private static final Message T_para1 = message("xmlui.administrative.item.EditItemStatusForm.para1");
	private static final Message T_label_id = message("xmlui.administrative.item.EditItemStatusForm.label_id");
	private static final Message T_label_handle = message("xmlui.administrative.item.EditItemStatusForm.label_handle");
	private static final Message T_label_modified = message("xmlui.administrative.item.EditItemStatusForm.label_modified");
	private static final Message T_label_in = message("xmlui.administrative.item.EditItemStatusForm.label_in");
	private static final Message T_label_page = message("xmlui.administrative.item.EditItemStatusForm.label_page");
	private static final Message T_label_auth = message("xmlui.administrative.item.EditItemStatusForm.label_auth");
	private static final Message T_label_withdraw = message("xmlui.administrative.item.EditItemStatusForm.label_withdraw");
	private static final Message T_label_reinstate = message("xmlui.administrative.item.EditItemStatusForm.label_reinstate");
	private static final Message T_label_delete = message("xmlui.administrative.item.EditItemStatusForm.label_delete");
	private static final Message T_submit_authorizations = message("xmlui.administrative.item.EditItemStatusForm.submit_authorizations");
	private static final Message T_submit_withdraw = message("xmlui.administrative.item.EditItemStatusForm.submit_withdraw");
	private static final Message T_submit_reinstate = message("xmlui.administrative.item.EditItemStatusForm.submit_reinstate");
	private static final Message T_submit_delete = message("xmlui.administrative.item.EditItemStatusForm.submit_delete");
	private static final Message T_na = message("xmlui.administrative.item.EditItemStatusForm.na");
	
	private static final Message T_sysadmins_only = message("xmlui.administrative.item.EditItemStatusForm.sysadmins_only");
	
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
	{
		pageMeta.addMetadata("title").addContent(T_title);
		
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrailLink(contextPath + "/admin/item",T_item_trail);
		pageMeta.addTrail().addContent(T_trail);
	}
	
	public void addBody(Body body) throws SQLException, WingException
	{
		// Get our parameters and state
		int itemID = parameters.getParameterAsInteger("itemID",-1);
		Item item = Item.find(context, itemID);
		String baseURL = contextPath+"/admin/item?administrative-continue="+knot.getId();
		
	
		// DIVISION: main
		Division main = body.addInteractiveDivision("edit-item-status", contextPath+"/admin/item", Division.METHOD_POST,"primary administrative edit-item-status");
		main.setHead(T_option_head);
		
		
		
		
		// LIST: options
		List options = main.addList("options",List.TYPE_SIMPLE,"horizontal");
		options.addItem().addHighlight("bold").addXref(baseURL+"&submit_status",T_option_status);
		options.addItem().addXref(baseURL+"&submit_bitstreams",T_option_bitstreams);
		options.addItem().addXref(baseURL+"&submit_metadata",T_option_metadata);
		options.addItem().addXref(baseURL + "&view_item", T_option_view);
		
		
		
		
		
		// PARA: Helpfull instructions
		main.addPara(T_para1);
		
		
		

		
		// LIST: Item meta-meta information
		List itemInfo = main.addList("item-info");
		
		itemInfo.addLabel(T_label_id);
		itemInfo.addItem(String.valueOf(item.getID()));
		
		itemInfo.addLabel(T_label_handle);
		itemInfo.addItem(item.getHandle()==null?"None":item.getHandle());
		
		itemInfo.addLabel(T_label_modified);
		itemInfo.addItem(item.getLastModified().toString());
		
		itemInfo.addLabel(T_label_in);
		
		List subList = itemInfo.addList("collections", List.TYPE_SIMPLE);
		Collection[] collections = item.getCollections();
		for(Collection collection : collections) {
			subList.addItem(collection.getMetadata("name"));
		}
		
		itemInfo.addLabel(T_label_page);
		if(item.getHandle()==null){
			itemInfo.addItem(T_na);		
		}
		else{
			itemInfo.addItem().addXref(ConfigurationManager.getProperty("dspace.url") + "/handle/" + item.getHandle(),ConfigurationManager.getProperty("dspace.url") + "/handle/" + item.getHandle());		
		}
		
		itemInfo.addLabel(T_label_auth);
		addAdministratorOnlyButton(itemInfo.addItem(), "submit_authorization", T_submit_authorizations);
	
		if(!item.isWithdrawn())
		{
			itemInfo.addLabel(T_label_withdraw);
			itemInfo.addItem().addButton("submit_withdraw").setValue(T_submit_withdraw);
		}
		else
		{	
			itemInfo.addLabel(T_label_reinstate);
			itemInfo.addItem().addButton("submit_reinstate").setValue(T_submit_reinstate);
		}
		
		itemInfo.addLabel(T_label_delete);
		addAdministratorOnlyButton(itemInfo.addItem(), "submit_delete", T_submit_delete);
		
		
		
		
		// PARA: main actions
		main.addPara().addButton("submit_return").setValue(T_submit_return);
		
		main.addHidden("administrative-continue").setValue(knot.getId());
	}
	
	private void addAdministratorOnlyButton(org.dspace.app.xmlui.wing.element.Item item, String buttonName, Message buttonLabel) throws WingException, SQLException
	{
    	Button button = item.addButton(buttonName);
    	button.setValue(buttonLabel);
    	if (!AuthorizeManager.isAdmin(context))
    	{
    		// Only admins can create or delete
    		button.setDisabled();
    		item.addHighlight("fade").addContent(T_sysadmins_only);
    	}
	}
	
}
