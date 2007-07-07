/*
 * EditBitstreamForm.java
 *
 * Version: $Revision: 1.4 $
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

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.xml.sax.SAXException;

/**
 * 
 * Show a form allowing the user to edit a bitstream's metadata, the description & format.
 * 
 * @author Scott Phillips
 */
public class EditBitstreamForm extends AbstractDSpaceTransformer
{

	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_submit_save = message("xmlui.general.save");
	private static final Message T_submit_cancel = message("xmlui.general.cancel");
	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");
	
	private static final Message T_title = message("xmlui.administrative.item.EditBitstreamForm.title");
	private static final Message T_trail = message("xmlui.administrative.item.EditBitstreamForm.trail");
	private static final Message T_head1 = message("xmlui.administrative.item.EditBitstreamForm.head1");
	private static final Message T_file_label = message("xmlui.administrative.item.EditBitstreamForm.file_label");
	private static final Message T_primary_label = message("xmlui.administrative.item.EditBitstreamForm.primary_label");
	private static final Message T_primary_option_yes = message("xmlui.administrative.item.EditBitstreamForm.primary_option_yes");
	private static final Message T_primary_option_no = message("xmlui.administrative.item.EditBitstreamForm.primary_option_no");
	private static final Message T_description_label = message("xmlui.administrative.item.EditBitstreamForm.description_label");
	private static final Message T_description_help = message("xmlui.administrative.item.EditBitstreamForm.description_help");
	private static final Message T_para1 = message("xmlui.administrative.item.EditBitstreamForm.para1");
	private static final Message T_format_label = message("xmlui.administrative.item.EditBitstreamForm.format_label");
	private static final Message T_format_default = message("xmlui.administrative.item.EditBitstreamForm.format_default");
	private static final Message T_para2 = message("xmlui.administrative.item.EditBitstreamForm.para2");
	private static final Message T_user_label = message("xmlui.administrative.item.EditBitstreamForm.user_label");
	private static final Message T_user_help = message("xmlui.administrative.item.EditBitstreamForm.user_help");



	public void addPageMeta(PageMeta pageMeta) throws WingException
	{
		pageMeta.addMetadata("title").addContent(T_title);

		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrailLink(contextPath + "/admin/item",T_item_trail);
		pageMeta.addTrail().addContent(T_trail);
	}

	public void addBody(Body body) throws SAXException, WingException,
	UIException, SQLException, IOException, AuthorizeException
	{    	
		// Get our parameters
		int bitstreamID = parameters.getParameterAsInteger("bitstreamID",-1);

		// Get the bitstream and all the various formats
		Bitstream bitstream = Bitstream.find(context, bitstreamID);
		BitstreamFormat currentFormat = bitstream.getFormat();
		BitstreamFormat[] bitstreamFormats = BitstreamFormat.findNonInternal(context);
		
		boolean primaryBitstream = false;
		Bundle[] bundles = bitstream.getBundles();
		if (bundles != null && bundles.length > 0)
		{
			if (bitstreamID == bundles[0].getPrimaryBitstreamID())
			{
				primaryBitstream = true;
			}
		}

		// File name & url
		String fileUrl = contextPath + "/bitstream/id/" +bitstream.getID() + "/" + bitstream.getName();
		String fileName = bitstream.getName();




		// DIVISION: main
		Division div = body.addInteractiveDivision("edit-bitstream", contextPath+"/admin/item", Division.METHOD_MULTIPART, "primary administrative item");    	
		div.setHead(T_head1);


		// LIST: edit form
		List edit = div.addList("edit-bitstream-list", List.TYPE_FORM);

		edit.addLabel(T_file_label);
		edit.addItem().addXref(fileUrl, fileName);

		
		Select primarySelect = edit.addItem().addSelect("primary");
		primarySelect.setLabel(T_primary_label);
		primarySelect.addOption(primaryBitstream,"yes",T_primary_option_yes);
		primarySelect.addOption(!primaryBitstream,"no",T_primary_option_no);
		
		Text description = edit.addItem().addText("description");
		description.setLabel(T_description_label);
		description.setHelp(T_description_help);
		description.setValue(bitstream.getDescription());

		edit.addItem(T_para1);

		// System supported formats
		Select format = edit.addItem().addSelect("formatID");
		format.setLabel(T_format_label);

		format.addOption(-1,T_format_default);
		for (BitstreamFormat bitstreamFormat : bitstreamFormats)
		{
			String supportLevel = "Unknown";
			if (bitstreamFormat.getSupportLevel() == BitstreamFormat.KNOWN)
				supportLevel = "known";
			else if (bitstreamFormat.getSupportLevel() == BitstreamFormat.SUPPORTED)
				supportLevel = "Supported";
			String name = bitstreamFormat.getShortDescription()+" ("+supportLevel+")";
			int id = bitstreamFormat.getID();

			format.addOption(id,name);
		}
		if (currentFormat != null)
		{
			format.setOptionSelected(currentFormat.getID());
		}
		else
		{
			format.setOptionSelected(-1);
		}

		edit.addItem(T_para2);

		// User supplied format
		Text userFormat = edit.addItem().addText("user_format");
		userFormat.setLabel(T_user_label);
		userFormat.setHelp(T_user_help);
		userFormat.setValue(bitstream.getUserFormatDescription());




		// ITEM: form actions
		org.dspace.app.xmlui.wing.element.Item actions = edit.addItem();
		actions.addButton("submit_save").setValue(T_submit_save);
		actions.addButton("submit_cancel").setValue(T_submit_cancel);

		div.addHidden("administrative-continue").setValue(knot.getId()); 

	}
}
