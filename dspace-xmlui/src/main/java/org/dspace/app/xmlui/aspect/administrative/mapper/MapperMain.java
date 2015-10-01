/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.mapper;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.UUID;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.xml.sax.SAXException;

/**
 * Manage the mapping of items into this collection, allow the user to 
 * search for new items to import or browse a list of currently mapped 
 * items.
 * 
 * @author Scott Phillips
 */

public class MapperMain extends AbstractDSpaceTransformer {

	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_submit_return = message("xmlui.general.return");
	private static final Message T_mapper_trail = message("xmlui.administrative.mapper.general.mapper_trail");
	
	private static final Message T_title = message("xmlui.administrative.mapper.MapperMain.title");
	private static final Message T_head1 = message("xmlui.administrative.mapper.MapperMain.head1");
	private static final Message T_para1 = message("xmlui.administrative.mapper.MapperMain.para1");
	private static final Message T_para2 = message("xmlui.administrative.mapper.MapperMain.para2");
	private static final Message T_stat_label = message("xmlui.administrative.mapper.MapperMain.stat_label");
	private static final Message T_stat_info = message("xmlui.administrative.mapper.MapperMain.stat_info");
	private static final Message T_search_label = message("xmlui.administrative.mapper.MapperMain.search_label");
	private static final Message T_submit_search = message("xmlui.administrative.mapper.MapperMain.submit_search");
	private static final Message T_submit_browse = message("xmlui.administrative.mapper.MapperMain.submit_browse");
	
	private static final Message T_no_add = message("xmlui.administrative.mapper.MapperMain.no_add");


	protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
	protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

	public void addPageMeta(PageMeta pageMeta) throws WingException  
	{
		pageMeta.addMetadata("title").addContent(T_title);
		
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrail().addContent(T_mapper_trail);
	}

	
	public void addBody(Body body) throws SAXException, WingException, SQLException {
		// Get our parameters and state;
		UUID collectionID = UUID.fromString(parameters.getParameter("collectionID",null));
		Collection collection = collectionService.find(context,collectionID);
		
		int[] counts = getNumberOfMappedAndUnmappedItems(collection);
		int count_native = counts[0];
		int count_import = counts[1];
		
		
		
		
		// DIVISION: manage-mapper
		Division div = body.addInteractiveDivision("manage-mapper",contextPath + "/admin/mapper", Division.METHOD_GET,"primary administrative mapper");
		div.setHead(T_head1);
		
		div.addPara(T_para1.parameterize(collection.getName()));
		
		div.addPara(T_para2);
		
		
		// LIST: Author search form
		List form = div.addList("mapper-form");	
		
		form.addLabel(T_stat_label);
		form.addItem(T_stat_info.parameterize(count_import,count_native+count_import));
		
		form.addLabel(T_search_label);
		org.dspace.app.xmlui.wing.element.Item queryItem = form.addItem();
		Text query = queryItem.addText("query");
		Button button = queryItem.addButton("submit_author");
		button.setValue(T_submit_search);
		if (!authorizeService.authorizeActionBoolean(context, collection, Constants.ADD))
		{
			query.setDisabled();
			button.setDisabled();
			queryItem.addHighlight("fade").addContent(T_no_add);
		}
		
		// PARA: actions
		Para actions = div.addPara();
		actions.addButton("submit_browse").setValue(T_submit_browse);
		actions.addButton("submit_return").setValue(T_submit_return);
		
		
		div.addHidden("administrative-continue").setValue(knot.getId());
	}
	
	/**
	 * Count the number of unmapped and mapped items in this collection
	 * 
	 * @param collection The collection to count from.
	 * @return a two integer array of native items and imported items.
	 */
	private int[] getNumberOfMappedAndUnmappedItems(Collection collection) throws SQLException 
	{
		int count_native = 0;
		int count_import = 0;
		
		// get all items from that collection
        Iterator<Item> iterator = itemService.findByCollection(context, collection);

		// iterate through the items in this collection, and count how many
		// are native, and how many are imports, and which collections they
		// came from
		while (iterator.hasNext())
		{
			Item item = iterator.next();

			if (itemService.isOwningCollection(item, collection))
			{
				count_native++;
			}
			else
			{
				count_import++;
			}
		}

        int[] counts = new int[2];
        counts[0] = count_native;
        counts[1] = count_import;
        return counts;
	}

}
