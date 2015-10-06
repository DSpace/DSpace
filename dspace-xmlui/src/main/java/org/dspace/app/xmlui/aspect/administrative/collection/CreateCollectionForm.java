/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.collection;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;

/**
 * Presents the user with a form to enter the initial metadata for creation of a new collection
 * @author Alexey Maslov
 */
public class CreateCollectionForm extends AbstractDSpaceTransformer   
{
	/** Language Strings */
	private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
	
	private static final Message T_title = message("xmlui.administrative.collection.CreateCollectionForm.title");
	private static final Message T_trail = message("xmlui.administrative.collection.CreateCollectionForm.trail");
	private static final Message T_main_head = message("xmlui.administrative.collection.CreateCollectionForm.main_head");
	
	private static final Message T_label_name = message("xmlui.administrative.collection.EditCollectionMetadataForm.label_name");
	private static final Message T_label_short_description = message("xmlui.administrative.collection.EditCollectionMetadataForm.label_short_description");
	private static final Message T_label_introductory_text = message("xmlui.administrative.collection.EditCollectionMetadataForm.label_introductory_text");
	private static final Message T_label_copyright_text = message("xmlui.administrative.collection.EditCollectionMetadataForm.label_copyright_text");
	private static final Message T_label_side_bar_text = message("xmlui.administrative.collection.EditCollectionMetadataForm.label_side_bar_text");
	private static final Message T_label_license = message("xmlui.administrative.collection.EditCollectionMetadataForm.label_license");
	private static final Message T_label_provenance_description = message("xmlui.administrative.collection.EditCollectionMetadataForm.label_provenance_description");
	private static final Message T_label_logo = message("xmlui.administrative.collection.EditCollectionMetadataForm.label_logo");

	private static final Message T_submit_save = message("xmlui.administrative.collection.CreateCollectionForm.submit_save");
	private static final Message T_submit_cancel = message("xmlui.general.cancel");

	protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();

	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);
    }
	
	public void addBody(Body body) throws WingException, SQLException, AuthorizeException
	{
		UUID communityID = UUID.fromString(parameters.getParameter("communityID", null));
		Community parentCommunity = communityService.find(context, communityID);
		
		// DIVISION: main
	    Division main = body.addInteractiveDivision("create-collection",contextPath+"/admin/collection",Division.METHOD_MULTIPART,"primary administrative collection");
	    main.setHead(T_main_head.parameterize(communityService.getMetadata(parentCommunity, "name")));
	        
	    
	    // The grand list of metadata options
	    List metadataList = main.addList("metadataList", "form");
	    
	    // collection name
	    metadataList.addLabel(T_label_name);
	    Text name = metadataList.addItem().addText("name");
        name.setAutofocus("autofocus");
	    name.setSize(40);
	    
	    // short description
	    metadataList.addLabel(T_label_short_description);
	    Text short_description = metadataList.addItem().addText("short_description");
	    short_description.setSize(40);
	    
	    // introductory text
	    metadataList.addLabel(T_label_introductory_text);
	    TextArea introductory_text = metadataList.addItem().addTextArea("introductory_text");
	    introductory_text.setSize(6, 40);
	    
	    // copyright text
	    metadataList.addLabel(T_label_copyright_text);
	    TextArea copyright_text = metadataList.addItem().addTextArea("copyright_text");
	    copyright_text.setSize(6, 40);
	    
	    // legacy sidebar text; may or may not be used for news 
	    metadataList.addLabel(T_label_side_bar_text);
	    TextArea side_bar_text = metadataList.addItem().addTextArea("side_bar_text");
	    side_bar_text.setSize(6, 40);
	    
	    // license text
	    metadataList.addLabel(T_label_license);
	    TextArea license = metadataList.addItem().addTextArea("license");
	    license.setSize(6, 40);
	    
	    // provenance description
	    metadataList.addLabel(T_label_provenance_description);
	    TextArea provenance_description = metadataList.addItem().addTextArea("provenance_description");
	    provenance_description.setSize(6, 40);
	    	    
	    // the row to upload a new logo 
	    metadataList.addLabel(T_label_logo);
	    metadataList.addItem().addFile("logo");

	    Para buttonList = main.addPara();
	    buttonList.addButton("submit_save").setValue(T_submit_save);
	    buttonList.addButton("submit_cancel").setValue(T_submit_cancel);
	    
    	main.addHidden("administrative-continue").setValue(knot.getId());
    }
}
