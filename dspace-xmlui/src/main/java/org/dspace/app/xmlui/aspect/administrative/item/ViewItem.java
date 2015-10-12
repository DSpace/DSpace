/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.item;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

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
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import cz.cuni.mff.ufal.dspace.app.xmlui.aspect.general.IfServiceManagerSelector;

/**
 * Display basic meta-meta information about the item and allow the user to
 * change its state such as withdraw or reinstate, possibly even completely
 * deleting the item!
 * 
 * based on class by Jay Paz and Scott Phillips
 * modified for LINDAT/CLARIN
 */

public class ViewItem extends AbstractDSpaceTransformer {

	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");
	private static final Message T_option_head = message("xmlui.administrative.item.general.option_head");
	
	public static final Message T_option_status = message("xmlui.administrative.item.general.option_status");
	public static final Message T_option_bitstreams = message("xmlui.administrative.item.general.option_bitstreams");
	public static final Message T_option_metadata = message("xmlui.administrative.item.general.option_metadata");
	public static final Message T_option_view = message("xmlui.administrative.item.general.option_view");
	public static final Message T_option_curate = message("xmlui.administrative.item.general.option_curate");
	public static final Message T_option_license = message("xmlui.administrative.item.general.option_license");
	public static final Message T_option_embargo = message("xmlui.administrative.item.general.option_embargo");
	public static final Message T_option_services = message("xmlui.administrative.item.general.option_services");

	private static final Message T_title = message("xmlui.administrative.item.ViewItem.title");
	private static final Message T_trail = message("xmlui.administrative.item.ViewItem.trail");
	private static final Message T_head_parent_collections = message("xmlui.ArtifactBrowser.ItemViewer.head_parent_collections");
	private static final Message T_show_simple = message("xmlui.ArtifactBrowser.ItemViewer.show_simple");
	private static final Message T_show_full = message("xmlui.ArtifactBrowser.ItemViewer.show_full");

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
		
		int itemID = parameters.getParameterAsInteger("itemID", -1);
		Item item = Item.find(context, itemID);
		String baseURL = contextPath + "/admin/item?administrative-continue="
				+ knot.getId() ;
		
		String link = baseURL + "&view_item"
				+ (showFullItem ? "" : "&show=full");
		String tabLink = baseURL + "&view_item"
				+ (!showFullItem ? "" : "&show=full");
		// DIVISION: main
		Division main = body.addInteractiveDivision("edit-item-status",
				contextPath + "/admin/item", Division.METHOD_POST,
				"primary administrative edit-item-status");
		main.setHead(T_option_head);
		
		// LIST: options
		List options = main.addList("options", List.TYPE_SIMPLE, "horizontal");
		add_options(context, eperson, options, baseURL, T_option_view, tabLink);

		
		main = main.addDivision("item-view", "well well-small well-light");
		
		// item
		
		Para showfullPara = main.addPara(null, "bold");

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
		
		showfullPara = main.addPara(null, "bold");

		if (showFullItem)
        {
            showfullPara.addXref(link).addContent(T_show_simple);
        }
        else
        {
            showfullPara.addXref(link).addContent(T_show_full);
        }
	}

	public static void add_options(
			List options, 
			String baseURL,
			Message to_highlight) throws WingException 
	{
		add_options( null, null, options, baseURL, to_highlight, null );
	}
	
	public static void add_options(
			List options, 
			String baseURL, Message to_highlight, String highlighted_link) throws WingException 
	{
		add_options( null, null, options, baseURL, to_highlight, highlighted_link );
	}
	
	public static void add_options(
			Context context, 
			EPerson eperson,
			List options, 
			String baseURL, Message to_highlight, String highlighted_link) throws WingException 
	{
		
		LinkedHashMap<String, Message> map = new LinkedHashMap<String, Message>();
		
		boolean isServiceManger = false;
		try {
			isServiceManger = IfServiceManagerSelector.isNonAdminServiceManager(context, eperson);
		} catch (SQLException e) {
			
		}
		
		if(isServiceManger) {
			map.put("&submit_status", T_option_status);
			map.put("&view_item", T_option_view);			
			map.put("&services", T_option_services);
		} else {		
			map.put("&submit_status", T_option_status);
			map.put("&submit_bitstreams", T_option_bitstreams);
			map.put("&submit_metadata", T_option_metadata);
			map.put("&view_item", T_option_view);
			map.put("&submit_curate", T_option_curate);
			map.put("&edit_license", T_option_license);
			map.put("&embargo", T_option_embargo);
			map.put("&services", T_option_services);
		}
		
		for ( Map.Entry<String, Message> tab : map.entrySet()) 
		{
			org.dspace.app.xmlui.wing.element.Item item = options.addItem(); 
			if ( to_highlight.equals(tab.getValue()) ) 
			{
				if ( highlighted_link == null ) {
					to_highlight = tab.getValue();
				}
				item.addHighlight("bold").addXref(
						highlighted_link, to_highlight);
				continue;
			}
			item.addXref(baseURL + tab.getKey(), tab.getValue());
		}
	}
	
}
