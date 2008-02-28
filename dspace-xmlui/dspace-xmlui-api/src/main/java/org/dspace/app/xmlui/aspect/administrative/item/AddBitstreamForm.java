/*
 * AddBitstreamForm.java
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
import java.util.ArrayList;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.components.flow.FlowHelper;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.File;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bundle;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.xml.sax.SAXException;

/**
 * 
 * Show a form that allows the user to upload a new bitstream. The 
 * user can select the new bitstream's bundle (which is unchangable 
 * after upload) and a description for the file.
 * 
 * @author Scott Phillips
 */
public class AddBitstreamForm extends AbstractDSpaceTransformer
{
	
	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_submit_cancel = message("xmlui.general.cancel");
	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");

	private static final Message T_title = message("xmlui.administrative.item.AddBitstreamForm.title.");
	private static final Message T_trail = message("xmlui.administrative.item.AddBitstreamForm.trail");
	private static final Message T_head1 = message("xmlui.administrative.item.AddBitstreamForm.head1");
	private static final Message T_bundle_label = message("xmlui.administrative.item.AddBitstreamForm.bundle_label");
	private static final Message T_file_label = message("xmlui.administrative.item.AddBitstreamForm.file_label");
	private static final Message T_file_help = message("xmlui.administrative.item.AddBitstreamForm.file_help");
	private static final Message T_description_label = message("xmlui.administrative.item.AddBitstreamForm.description_label");
	private static final Message T_description_help = message("xmlui.administrative.item.AddBitstreamForm.description_help");
	private static final Message T_submit_upload = message("xmlui.administrative.item.AddBitstreamForm.submit_upload");

	private static final Message T_no_bundles = message("xmlui.administrative.item.AddBitstreamForm.no_bundles");

	
	private static String DEFAULT_BUNDLE_LIST = "ORIGINAL, METADATA, THUMBNAIL, LICENSE, CC_LICENSE";
		
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
		int itemID = parameters.getParameterAsInteger("itemID",-1);
		org.dspace.content.Item item = org.dspace.content.Item.find(context,itemID);
		
		// DIVISION: main div
		Division div = body.addInteractiveDivision("add-bitstream", contextPath+"/admin/item", Division.METHOD_MULTIPART, "primary administrative item");    	

		// LIST: upload form
		List upload = div.addList("submit-upload-new", List.TYPE_FORM);
		upload.setHead(T_head1);    

		int bundleCount = 0; // record how many bundles we are able to upload too.
		Select select = upload.addItem().addSelect("bundle");
		select.setLabel(T_bundle_label);
		
		// Get the list of bundles to allow the user to upload too. Either use the default 
		// or one supplied from the dspace.cfg.
		String bundleString = ConfigurationManager.getProperty("xmlui.bundle.upload");
        if (bundleString == null || bundleString.length() == 0)
        	bundleString = DEFAULT_BUNDLE_LIST;
        String[] parts = bundleString.split(",");
        for (String part : parts)
        {
        	if (addBundleOption(item,select,part.trim()))
        		bundleCount++;
        }
        select.setOptionSelected("ORIGINAL");
		
		if (bundleCount == 0)
			select.setDisabled();
		

		File file = upload.addItem().addFile("file");
		file.setLabel(T_file_label);
		file.setHelp(T_file_help);
		file.setRequired();

		if (bundleCount == 0)
			file.setDisabled();
		
		Text description = upload.addItem().addText("description");
		description.setLabel(T_description_label);
		description.setHelp(T_description_help);

		if (bundleCount == 0)
			description.setDisabled();
		
		if (bundleCount == 0)
			upload.addItem().addContent(T_no_bundles);
		
		// ITEM: actions
		Item actions = upload.addItem();
		Button button = actions.addButton("submit_upload");
		button.setValue(T_submit_upload);
		if (bundleCount == 0)
			button.setDisabled();
		
		actions.addButton("submit_cancel").setValue(T_submit_cancel);

		div.addHidden("administrative-continue").setValue(knot.getId()); 
	}	
	
	public boolean addBundleOption(org.dspace.content.Item item, Select select, String bundleName) throws SQLException, WingException
	{
		
		// For some crazzy reason multiple bundles can share the same name
		Bundle[] bundles = item.getBundles(bundleName);
		if (bundles == null || bundles.length == 0)
		{
			// If the bundle does not exist then you have to be supper admin to be able
			// to upload to this bundle because at upload time the bundle will be created but
			// there is no way anyone but super admin could have access to add to the bundle.
			if ( ! AuthorizeManager.isAdmin(context))
				return false; // you can't upload to this bundle.
		}
		else
		{
			// At least one bundle exists, does the user have privleges to upload to it?
			Bundle bundle = bundles[0];
			if ( ! AuthorizeManager.authorizeActionBoolean(context, bundle, Constants.ADD))
				return false; // you can't upload to this bundle.
			
			// You also need the write privlege on the bundle.
			if ( ! AuthorizeManager.authorizeActionBoolean(context, bundle, Constants.WRITE))
				return false; // you can't upload.
		}
		
		// It's okay to upload.
		select.addOption(bundleName, message("xmlui.administrative.item.AddBitstreamForm.bundle."+bundleName));
		return true;
	}
	
}
