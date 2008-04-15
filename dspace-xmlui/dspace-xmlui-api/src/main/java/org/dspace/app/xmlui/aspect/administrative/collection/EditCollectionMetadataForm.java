/*
 * EditCollectionMetadataForm.java
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
package org.dspace.app.xmlui.aspect.administrative.collection;

import java.sql.SQLException;

import org.dspace.app.xmlui.aspect.administrative.FlowContainerUtils;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;


/**
 * Presents the user (in this case an administrator over the collection) with the
 * form to edit that collection's metadata, logo, and item template.
 * @author Alexey Maslov
 */
public class EditCollectionMetadataForm extends AbstractDSpaceTransformer   
{
	/** Language Strings */
	private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
	
	private static final Message T_collection_trail = message("xmlui.administrative.collection.general.collection_trail");
	private static final Message T_options_metadata = message("xmlui.administrative.collection.general.options_metadata");	
	private static final Message T_options_roles = message("xmlui.administrative.collection.general.options_roles");
	
	private static final Message T_submit_return = message("xmlui.general.return");
	
	private static final Message T_title = message("xmlui.administrative.collection.EditCollectionMetadataForm.title");
	private static final Message T_trail = message("xmlui.administrative.collection.EditCollectionMetadataForm.trail");

	private static final Message T_main_head = message("xmlui.administrative.collection.EditCollectionMetadataForm.main_head");
	
	private static final Message T_label_name = message("xmlui.administrative.collection.EditCollectionMetadataForm.label_name");
	private static final Message T_label_short_description = message("xmlui.administrative.collection.EditCollectionMetadataForm.label_short_description");
	private static final Message T_label_introductory_text = message("xmlui.administrative.collection.EditCollectionMetadataForm.label_introductory_text");
	private static final Message T_label_copyright_text = message("xmlui.administrative.collection.EditCollectionMetadataForm.label_copyright_text");
	private static final Message T_label_side_bar_text = message("xmlui.administrative.collection.EditCollectionMetadataForm.label_side_bar_text");
	private static final Message T_label_license = message("xmlui.administrative.collection.EditCollectionMetadataForm.label_license");
	private static final Message T_label_provenance_description = message("xmlui.administrative.collection.EditCollectionMetadataForm.label_provenance_description");

	private static final Message T_label_logo = message("xmlui.administrative.collection.EditCollectionMetadataForm.label_logo");
	private static final Message T_label_existing_logo = message("xmlui.administrative.collection.EditCollectionMetadataForm.label_existing_logo");

	private static final Message T_label_item_template = message("xmlui.administrative.collection.EditCollectionMetadataForm.label_item_template");

	private static final Message T_submit_create_template = message("xmlui.administrative.collection.EditCollectionMetadataForm.submit_create_template");
	private static final Message T_submit_edit_template = message("xmlui.administrative.collection.EditCollectionMetadataForm.submit_edit_template");
	private static final Message T_submit_delete_template = message("xmlui.general.delete");

	private static final Message T_submit_delete_logo = message("xmlui.administrative.collection.EditCollectionMetadataForm.submit_delete_logo");
	private static final Message T_submit_delete = message("xmlui.administrative.collection.EditCollectionMetadataForm.submit_delete");
	private static final Message T_submit_save = message("xmlui.administrative.collection.EditCollectionMetadataForm.submit_save");
	
	private static final Message T_sysadmins_only = message("xmlui.administrative.collection.EditCollectionMetadataForm.sysadmins_only");
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent(T_collection_trail);
        pageMeta.addTrail().addContent(T_trail);
    }
	
	
	public void addBody(Body body) throws WingException, SQLException, AuthorizeException
	{
		int collectionID = parameters.getParameterAsInteger("collectionID", -1);
		Collection thisCollection = Collection.find(context, collectionID);
		
		String baseURL = contextPath + "/admin/collection?administrative-continue=" + knot.getId();
		
		String short_description_error = FlowContainerUtils.checkXMLFragment(thisCollection.getMetadata("short_description"));
	    String introductory_text_error = FlowContainerUtils.checkXMLFragment(thisCollection.getMetadata("introductory_text"));
	    String copyright_text_error = FlowContainerUtils.checkXMLFragment(thisCollection.getMetadata("copyright_text"));
	    String side_bar_text_error = FlowContainerUtils.checkXMLFragment(thisCollection.getMetadata("side_bar_text"));
	    String license_error = FlowContainerUtils.checkXMLFragment(thisCollection.getMetadata("license"));
	    String provenance_description_error = FlowContainerUtils.checkXMLFragment(thisCollection.getMetadata("provenance_description"));
		
	    
		// DIVISION: main
	    Division main = body.addInteractiveDivision("collection-metadata-edit",contextPath+"/admin/collection",Division.METHOD_MULTIPART,"primary administrative collection");
	    main.setHead(T_main_head.parameterize(thisCollection.getMetadata("name")));
   
	    List options = main.addList("options",List.TYPE_SIMPLE,"horizontal");
	    options.addItem().addHighlight("bold").addXref(baseURL+"&submit_metadata",T_options_metadata);
	    options.addItem().addXref(baseURL+"&submit_roles",T_options_roles);
	    
	    
	    // The grand list of metadata options
	    List metadataList = main.addList("metadataList", "form");
	    
	    // collection name
	    metadataList.addLabel(T_label_name);
	    Text name = metadataList.addItem().addText("name");
	    name.setSize(40);
	    name.setValue(thisCollection.getMetadata("name"));
	    
	    // short description
	    metadataList.addLabel(T_label_short_description);
	    Text short_description = metadataList.addItem().addText("short_description");
	    short_description.setValue(thisCollection.getMetadata("short_description"));
	    short_description.setSize(40);
	    if (short_description_error != null) 
	    	short_description.addError(short_description_error);
	    
	    // introductory text
	    metadataList.addLabel(T_label_introductory_text);
	    TextArea introductory_text = metadataList.addItem().addTextArea("introductory_text");
	    introductory_text.setValue(thisCollection.getMetadata("introductory_text"));
	    introductory_text.setSize(6, 40);
	    if (introductory_text_error != null) 
	    	introductory_text.addError(introductory_text_error);
	    
	    // copyright text
	    metadataList.addLabel(T_label_copyright_text);
	    TextArea copyright_text = metadataList.addItem().addTextArea("copyright_text");
	    copyright_text.setValue(thisCollection.getMetadata("copyright_text"));
	    copyright_text.setSize(6, 40);
	    if (copyright_text_error != null) 
	    	copyright_text.addError(copyright_text_error);
	    
	    // legacy sidebar text; may or may not be used for news 
	    metadataList.addLabel(T_label_side_bar_text);
	    TextArea side_bar_text = metadataList.addItem().addTextArea("side_bar_text");
	    side_bar_text.setValue(thisCollection.getMetadata("side_bar_text"));
	    side_bar_text.setSize(6, 40);
	    if (side_bar_text_error != null) 
	    	side_bar_text.addError(side_bar_text_error);
	    
	    // license text
	    metadataList.addLabel(T_label_license);
	    TextArea license = metadataList.addItem().addTextArea("license");
	    license.setValue(thisCollection.getMetadata("license"));
	    license.setSize(6, 40);
	    if (license_error != null) 
	    	license.addError(license_error);
	    
	    // provenance description
	    metadataList.addLabel(T_label_provenance_description);
	    TextArea provenance_description = metadataList.addItem().addTextArea("provenance_description");
	    provenance_description.setValue(thisCollection.getMetadata("provenance_description"));
	    provenance_description.setSize(6, 40);
	    if (provenance_description_error != null) 
	    	provenance_description.addError(provenance_description_error);
	    	    
	    // the row to upload a new logo 
	    metadataList.addLabel(T_label_logo);
	    metadataList.addItem().addFile("logo");

	    // the row displaying an existing logo
	    Item item;
	    if (thisCollection.getLogo() != null) {
	    	metadataList.addLabel(T_label_existing_logo);
	    	item = metadataList.addItem();
	    	item.addFigure(contextPath + "/bitstream/id/" + thisCollection.getLogo().getID() + "/bob.jpg", null, null);
	    	item.addButton("submit_delete_logo").setValue(T_submit_delete_logo);
	    }
	    
	    // item template creation and removal
	    metadataList.addLabel(T_label_item_template);
	    item = metadataList.addItem();
	    
	    if (thisCollection.getTemplateItem() == null)
	    	addAdministratorOnlyButton(item, "submit_create_template", T_submit_create_template);
	    else 
	    {
	    	item.addButton("submit_edit_template").setValue(T_submit_edit_template);
	    	addAdministratorOnlyButton(item, "submit_delete_template", T_submit_delete_template);
	    }
	    
		Para buttonList = main.addPara();
	    buttonList.addButton("submit_save").setValue(T_submit_save);
	    if (AuthorizeManager.isAdmin(context))
	    	buttonList.addButton("submit_delete").setValue(T_submit_delete);
	    buttonList.addButton("submit_return").setValue(T_submit_return);
	    
    	main.addHidden("administrative-continue").setValue(knot.getId());
    }
	
	private void addAdministratorOnlyButton(Item item, String buttonName, Message buttonLabel) throws WingException, SQLException
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
