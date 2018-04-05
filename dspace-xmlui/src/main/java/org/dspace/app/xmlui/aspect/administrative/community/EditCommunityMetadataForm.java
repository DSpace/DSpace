/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.community;

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
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;

/**
 * Presents the user (in this case an administrator over the community) with the
 * form to edit that community's metadata and logo.
 * @author Alexey Maslov
 */
public class EditCommunityMetadataForm extends AbstractDSpaceTransformer   
{
    /** Language Strings */
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");

    private static final Message T_community_trail = message("xmlui.administrative.community.general.community_trail");
    private static final Message T_options_metadata = message("xmlui.administrative.community.general.options_metadata");  
    private static final Message T_options_roles = message("xmlui.administrative.community.general.options_roles");
    private static final Message T_options_curate = message("xmlui.administrative.community.general.options_curate");

    private static final Message T_title = message("xmlui.administrative.community.EditCommunityMetadataForm.title");
    private static final Message T_trail = message("xmlui.administrative.community.EditCommunityMetadataForm.trail");

    private static final Message T_main_head = message("xmlui.administrative.community.EditCommunityMetadataForm.main_head");

    private static final Message T_label_name = message("xmlui.administrative.community.EditCommunityMetadataForm.label_name");
    private static final Message T_label_short_description = message("xmlui.administrative.community.EditCommunityMetadataForm.label_short_description");
    private static final Message T_label_introductory_text = message("xmlui.administrative.community.EditCommunityMetadataForm.label_introductory_text");
    private static final Message T_label_copyright_text = message("xmlui.administrative.community.EditCommunityMetadataForm.label_copyright_text");
    private static final Message T_label_side_bar_text = message("xmlui.administrative.community.EditCommunityMetadataForm.label_side_bar_text");

    private static final Message T_label_logo = message("xmlui.administrative.community.EditCommunityMetadataForm.label_logo");
    private static final Message T_label_existing_logo = message("xmlui.administrative.community.EditCommunityMetadataForm.label_existing_logo");

    private static final Message T_submit_delete_logo = message("xmlui.administrative.community.EditCommunityMetadataForm.submit_delete_logo");
    private static final Message T_submit_delete = message("xmlui.administrative.community.EditCommunityMetadataForm.submit_delete");
    private static final Message T_submit_update = message("xmlui.general.update");
    private static final Message T_submit_return = message("xmlui.general.return");

    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();

    @Override
    public void addPageMeta(PageMeta pageMeta)
            throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent(T_community_trail);
        pageMeta.addTrail().addContent(T_trail);
    }
	
    @Override
    public void addBody(Body body)
            throws WingException, SQLException, AuthorizeException
	{
		UUID communityID = UUID.fromString(parameters.getParameter("communityID", null));
		Community thisCommunity = communityService.find(context, communityID);

		String baseURL = contextPath + "/admin/community?administrative-continue=" + knot.getId();

	    String short_description_error = FlowContainerUtils.checkXMLFragment(communityService.getMetadata(thisCommunity, "short_description"));
	    String introductory_text_error = FlowContainerUtils.checkXMLFragment(communityService.getMetadata(thisCommunity, "introductory_text"));
	    String copyright_text_error = FlowContainerUtils.checkXMLFragment(communityService.getMetadata(thisCommunity, "copyright_text"));
	    String side_bar_text_error = FlowContainerUtils.checkXMLFragment(communityService.getMetadata(thisCommunity, "side_bar_text"));
	    
		// DIVISION: main
	    Division main = body.addInteractiveDivision("community-metadata-edit",contextPath+"/admin/community",Division.METHOD_MULTIPART,"primary administrative community");
	    main.setHead(T_main_head.parameterize(thisCommunity.getName()));
	    
        List options = main.addList("options",List.TYPE_SIMPLE,"horizontal");
        options.addItem().addHighlight("bold").addXref(baseURL+"&submit_metadata",T_options_metadata);
        options.addItem().addXref(baseURL+"&submit_roles",T_options_roles);
        options.addItem().addXref(baseURL+"&submit_curate",T_options_curate);
	    
	    // The grand list of metadata options
	    List metadataList = main.addList("metadataList", "form");
	    
	    // community name
	    metadataList.addLabel(T_label_name);
	    Text name = metadataList.addItem().addText("name");
	    name.setSize(40);
	    name.setValue(communityService.getMetadata(thisCommunity, "name"));
	    
	    // short description
	    metadataList.addLabel(T_label_short_description);
	    Text short_description = metadataList.addItem().addText("short_description");
	    short_description.setValue(communityService.getMetadata(thisCommunity, "short_description"));
	    short_description.setSize(40);
	    if (short_description_error != null)
        {
            short_description.addError(short_description_error);
        }
	    
	    // introductory text
	    metadataList.addLabel(T_label_introductory_text);
	    TextArea introductory_text = metadataList.addItem().addTextArea("introductory_text");
	    introductory_text.setValue(communityService.getMetadata(thisCommunity, "introductory_text"));
	    introductory_text.setSize(6, 40);
	    if (introductory_text_error != null)
        {
            introductory_text.addError(introductory_text_error);
        }
	    
	    // copyright text
	    metadataList.addLabel(T_label_copyright_text);
	    TextArea copyright_text = metadataList.addItem().addTextArea("copyright_text");
	    copyright_text.setValue(communityService.getMetadata(thisCommunity, "copyright_text"));
	    copyright_text.setSize(6, 40);
	    if (copyright_text_error != null)
        {
            copyright_text.addError(copyright_text_error);
        }
	    
	    // legacy sidebar text; may or may not be used for news 
	    metadataList.addLabel(T_label_side_bar_text);
	    TextArea side_bar_text = metadataList.addItem().addTextArea("side_bar_text");
	    side_bar_text.setValue(communityService.getMetadata(thisCommunity, "side_bar_text"));
	    side_bar_text.setSize(6, 40);
	    if (side_bar_text_error != null)
        {
            side_bar_text.addError(side_bar_text_error);
        }
	    	    
	    // the row to upload a new logo 
	    metadataList.addLabel(T_label_logo);
	    metadataList.addItem().addFile("logo");

	    // the row displaying an existing logo
	    Item item;
	    if (thisCommunity.getLogo() != null) {
	    	metadataList.addLabel(T_label_existing_logo);
	    	item = metadataList.addItem();
                // Filename in URL is  ignored by the sitemap.  It's needed to
                // provide a format hint to the browser, since logo bitstreams
                // don't have names(!).
                item.addFigure(contextPath + "/bitstream/id/"
                        + thisCommunity.getLogo().getID() + "/bob.jpg", null,
                        null);
	    	item.addButton("submit_delete_logo").setValue(T_submit_delete_logo);
	    }
	    
	    Para buttonList = main.addPara();
	    buttonList.addButton("submit_save").setValue(T_submit_update);

	    if (authorizeService.authorizeActionBoolean(context, thisCommunity, Constants.DELETE))
        {
	         buttonList.addButton("submit_delete").setValue(T_submit_delete);
        }
	    buttonList.addButton("submit_return").setValue(T_submit_return);
	    
    	main.addHidden("administrative-continue").setValue(knot.getId());
	    
    }
}
