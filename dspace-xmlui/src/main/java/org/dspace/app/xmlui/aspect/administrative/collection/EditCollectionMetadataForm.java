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

import org.dspace.app.xmlui.aspect.administrative.FlowContainerUtils;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeServiceImpl;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;


/**
 * Presents the user (in this case an administrator over the collection) with the
 * form to edit that collection's metadata, logo, and item template.
 * @author Alexey Maslov
 */
public class EditCollectionMetadataForm extends AbstractDSpaceTransformer   
{
    /** Language Strings */
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");

    private static final Message T_collection_trail = message("xmlui.administrative.collection.general.collection_trail");
    private static final Message T_options_metadata = message("xmlui.administrative.collection.general.options_metadata");	
    private static final Message T_options_roles = message("xmlui.administrative.collection.general.options_roles");
    private static final Message T_options_harvest = message("xmlui.administrative.collection.GeneralCollectionHarvestingForm.options_harvest");
    private static final Message T_options_curate = message("xmlui.administrative.collection.general.options_curate");	
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

	protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
	protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

    @Override
    public void addPageMeta(PageMeta pageMeta)
            throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent(T_collection_trail);
        pageMeta.addTrail().addContent(T_trail);
    }
	
	
    @Override
    public void addBody(Body body)
            throws WingException, SQLException, AuthorizeException
    {
		UUID collectionID = UUID.fromString(parameters.getParameter("collectionID", null));
		Collection thisCollection = collectionService.find(context, collectionID);
		
		String baseURL = contextPath + "/admin/collection?administrative-continue=" + knot.getId();

            //Check that all HTML input fields contain valid XHTML
            String short_description_error = FlowContainerUtils.checkXMLFragment(collectionService.getMetadata(thisCollection, "short_description"));
	    String introductory_text_error = FlowContainerUtils.checkXMLFragment(collectionService.getMetadata(thisCollection, "introductory_text"));
	    String copyright_text_error = FlowContainerUtils.checkXMLFragment(collectionService.getMetadata(thisCollection, "copyright_text"));
	    String side_bar_text_error = FlowContainerUtils.checkXMLFragment(collectionService.getMetadata(thisCollection, "side_bar_text"));
	    
		// DIVISION: main
	    Division main = body.addInteractiveDivision("collection-metadata-edit",contextPath+"/admin/collection",Division.METHOD_MULTIPART,"primary administrative collection");
	    main.setHead(T_main_head.parameterize(collectionService.getMetadata(thisCollection, "name")));
   
	    List options = main.addList("options",List.TYPE_SIMPLE,"horizontal");
	    options.addItem().addHighlight("bold").addXref(baseURL+"&submit_metadata",T_options_metadata);
	    options.addItem().addXref(baseURL+"&submit_roles",T_options_roles);
	    options.addItem().addXref(baseURL+"&submit_harvesting",T_options_harvest);
            options.addItem().addXref(baseURL+"&submit_curate",T_options_curate);
	    
	    
	    // The grand list of metadata options
	    List metadataList = main.addList("metadataList", "form");
	    
	    // collection name
	    metadataList.addLabel(T_label_name);
	    Text name = metadataList.addItem().addText("name");
	    name.setSize(40);
	    name.setValue(collectionService.getMetadata(thisCollection, "name"));
	    
	    // short description
	    metadataList.addLabel(T_label_short_description);
	    Text short_description = metadataList.addItem().addText("short_description");
	    short_description.setValue(collectionService.getMetadata(thisCollection, "short_description"));
	    short_description.setSize(40);
	    if (short_description_error != null)
        {
            short_description.addError(short_description_error);
        }
	    
	    // introductory text
	    metadataList.addLabel(T_label_introductory_text);
	    TextArea introductory_text = metadataList.addItem().addTextArea("introductory_text");
	    introductory_text.setValue(collectionService.getMetadata(thisCollection, "introductory_text"));
	    introductory_text.setSize(6, 40);
	    if (introductory_text_error != null)
        {
            introductory_text.addError(introductory_text_error);
        }
	    
	    // copyright text
	    metadataList.addLabel(T_label_copyright_text);
	    TextArea copyright_text = metadataList.addItem().addTextArea("copyright_text");
	    copyright_text.setValue(collectionService.getMetadata(thisCollection, "copyright_text"));
	    copyright_text.setSize(6, 40);
	    if (copyright_text_error != null)
        {
            copyright_text.addError(copyright_text_error);
        }
	    
	    // legacy sidebar text; may or may not be used for news 
	    metadataList.addLabel(T_label_side_bar_text);
	    TextArea side_bar_text = metadataList.addItem().addTextArea("side_bar_text");
	    side_bar_text.setValue(collectionService.getMetadata(thisCollection, "side_bar_text"));
	    side_bar_text.setSize(6, 40);
	    if (side_bar_text_error != null)
        {
            side_bar_text.addError(side_bar_text_error);
        }
	    
	    // license text
	    metadataList.addLabel(T_label_license);
	    TextArea license = metadataList.addItem().addTextArea("license");
	    license.setValue(collectionService.getMetadata(thisCollection, "license"));
	    license.setSize(6, 40);
	    
	    // provenance description
	    metadataList.addLabel(T_label_provenance_description);
	    TextArea provenance_description = metadataList.addItem().addTextArea("provenance_description");
	    provenance_description.setValue(collectionService.getMetadata(thisCollection, "provenance_description"));
	    provenance_description.setSize(6, 40);
	    	    
	    // the row to upload a new logo 
	    metadataList.addLabel(T_label_logo);
	    metadataList.addItem().addFile("logo");

	    // the row displaying an existing logo
	    Item item;
	    if (thisCollection.getLogo() != null) {
	    	metadataList.addLabel(T_label_existing_logo);
	    	item = metadataList.addItem();
                // Filename in URL is  ignored by the sitemap.  It's needed to
                // provide a format hint to the browser, since logo bitstreams
                // don't have names(!).
                item.addFigure(contextPath + "/bitstream/id/"
                        + thisCollection.getLogo().getID() + "/bob.jpg", null,
                        null);
	    	item.addButton("submit_delete_logo").setValue(T_submit_delete_logo);
	    }
	    
	    // item template creation and removal
	    metadataList.addLabel(T_label_item_template);
	    item = metadataList.addItem();
	    
	    if (thisCollection.getTemplateItem() == null)
        {
            item.addButton("submit_create_template").setValue(T_submit_create_template);
        }
	    else 
	    {
	    	item.addButton("submit_edit_template").setValue(T_submit_edit_template);
	    	item.addButton("submit_delete_template").setValue(T_submit_delete_template);
	    }
	    
		Para buttonList = main.addPara();
	    buttonList.addButton("submit_save").setValue(T_submit_save);
        //Only System Admins can Delete Collections
	    if (authorizeService.isAdmin(context))
        {
	    	buttonList.addButton("submit_delete").setValue(T_submit_delete);
        }
	    buttonList.addButton("submit_return").setValue(T_submit_return);
	    
    	main.addHidden("administrative-continue").setValue(knot.getId());
    }
	
}
