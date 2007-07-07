/*
 * FindItemForm.java
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

import java.util.ArrayList;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Text;
import org.xml.sax.SAXException;

/**
 * Query the user for an item's identifier.
 * 
 * @author Jay Paz
 */

public class FindItemForm extends AbstractDSpaceTransformer {

	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");
	
	private static final Message T_title = message("xmlui.administrative.item.FindItemForm.title");
	private static final Message T_head1 = message("xmlui.administrative.item.FindItemForm.head1");
	private static final Message T_identifier_label = message("xmlui.administrative.item.FindItemForm.identifier_label");
	private static final Message T_identifier_error = message("xmlui.administrative.item.FindItemForm.identifier_error");
	private static final Message T_find = message("xmlui.administrative.item.FindItemForm.find");
	
	public void addPageMeta(PageMeta pageMeta) throws WingException  
	{
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrail().addContent(T_item_trail);
		pageMeta.addMetadata("title").addContent(T_title);
	}

	
	public void addBody(Body body) throws SAXException, WingException
	{
		// Get our parameters and state;
		String identifier = parameters.getParameter("identifier",null);
		
		String errorString = parameters.getParameter("errors",null);
		ArrayList<String> errors = new ArrayList<String>();
		if (errorString != null)
			for (String error : errorString.split(","))
				errors.add(error);
		
		// DIVISION: find-item
		Division findItem = body.addInteractiveDivision("find-item",contextPath + "/admin/item", Division.METHOD_GET,"primary administrative item");
		findItem.setHead(T_head1);
		
		List form = findItem.addList("find-item-form", List.TYPE_FORM);
		
		Text id = form.addItem().addText("identifier");
		id.setLabel(T_identifier_label);
		if (identifier != null)
			id.setValue(identifier);
		if (errors.contains("identifier"))
			id.addError(T_identifier_error);
		
		form.addItem().addButton("submit_find").setValue(T_find);
		
		findItem.addHidden("administrative-continue").setValue(knot.getId());
	}
}
