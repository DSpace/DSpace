/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.item;

import java.sql.SQLException;
import java.util.UUID;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;

/**
 * Display basic meta-meta information about the item and allow the user to
 * change its state such as withdraw or reinstate, possibly even completely
 * deleting the item!
 * 
 * @author Jay Paz
 * @author Scott Phillips
 */

public class ViewItem extends AbstractDSpaceTransformer {

	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");

	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");

	private static final Message T_option_head = message("xmlui.administrative.item.general.option_head");

	private static final Message T_option_status = message("xmlui.administrative.item.general.option_status");

	private static final Message T_option_bitstreams = message("xmlui.administrative.item.general.option_bitstreams");

	private static final Message T_option_metadata = message("xmlui.administrative.item.general.option_metadata");

	private static final Message T_option_view = message("xmlui.administrative.item.general.option_view");
        
        private static final Message T_option_curate = message("xmlui.administrative.item.general.option_curate");

	private static final Message T_title = message("xmlui.administrative.item.ViewItem.title");

	private static final Message T_trail = message("xmlui.administrative.item.ViewItem.trail");

	private static final Message T_head_parent_collections = message("xmlui.ArtifactBrowser.ItemViewer.head_parent_collections");
	private static final Message T_show_simple =
        message("xmlui.ArtifactBrowser.ItemViewer.show_simple");
    
        private static final Message T_show_full =
        message("xmlui.ArtifactBrowser.ItemViewer.show_full");

	protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

	public void addPageMeta(PageMeta pageMeta) throws WingException {
		pageMeta.addMetadata("title").addContent(T_title);

		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrailLink(contextPath + "/admin/item", T_item_trail);
		pageMeta.addTrail().addContent(T_trail);
	}

	public void addBody(Body body) throws SQLException, WingException {
		// Get our parameters and state
		Request request = ObjectModelHelper.getRequest(objectModel);
		String show = request.getParameter("show");
		boolean showFullItem = false;
		if (show != null && show.length() > 0)
		{
			showFullItem = true;
		}
		
		UUID itemID = UUID.fromString(parameters.getParameter("itemID", null));
		Item item = itemService.find(context, itemID);
		String baseURL = contextPath + "/admin/item?administrative-continue="
				+ knot.getId() ;
		
		String link = baseURL + "&view_item" + (showFullItem?"":"&show=full");
		String tabLink = baseURL + "&view_item" + (!showFullItem?"":"&show=full");
		// DIVISION: main
		Division main = body.addInteractiveDivision("edit-item-status",
				contextPath + "/admin/item", Division.METHOD_POST,
				"primary administrative edit-item-status");
		main.setHead(T_option_head);
		
		// LIST: options
		List options = main.addList("options", List.TYPE_SIMPLE, "horizontal");
		options.addItem().addXref(
				baseURL + "&submit_status", T_option_status);
		options.addItem().addXref(baseURL + "&submit_bitstreams",
				T_option_bitstreams);
		options.addItem().addXref(baseURL + "&submit_metadata",
				T_option_metadata);
		options.addItem().addHighlight("bold").addXref(tabLink, T_option_view);
                options.addItem().addXref(baseURL + "&submit_curate", T_option_curate);
		
		// item
		
		Para showfullPara = main.addPara(null, "item-view-toggle item-view-toggle-top");

        if (showFullItem)
        {
            link = baseURL + "&view_item";
            showfullPara.addXref(link).addContent(T_show_simple);
        }
        else
        {
            link = baseURL + "&view_item&show=full";
            showfullPara.addXref(link).addContent(T_show_full);
        }

		ReferenceSet referenceSet;
		referenceSet = main.addReferenceSet("collection-viewer",
				showFullItem?ReferenceSet.TYPE_DETAIL_VIEW:ReferenceSet.TYPE_SUMMARY_VIEW);
		// Reference the actual Item
		ReferenceSet appearsInclude = referenceSet.addReference(item)
				.addReferenceSet(ReferenceSet.TYPE_DETAIL_LIST, null, "hierarchy");
		appearsInclude.setHead(T_head_parent_collections);

		// Reference all collections the item appears in.
		for (Collection collection : item.getCollections()) {
			appearsInclude.addReference(collection);
		}
		
		showfullPara = main.addPara(null, "item-view-toggle item-view-toggle-bottom");

		if (showFullItem)
        {
            showfullPara.addXref(link).addContent(T_show_simple);
        }
        else
        {
            showfullPara.addXref(link).addContent(T_show_full);
        }
	}
}
