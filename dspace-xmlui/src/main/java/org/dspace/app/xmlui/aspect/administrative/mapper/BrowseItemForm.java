/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.mapper;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.xml.sax.SAXException;

/**
 * List all items in this collection that are mapped from other collections.
 * 
 * @author Scott Phillips
 */

public class BrowseItemForm extends AbstractDSpaceTransformer {

	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_submit_return = message("xmlui.general.return");
	private static final Message T_mapper_trail = message("xmlui.administrative.mapper.general.mapper_trail");
	
	private static final Message T_title = message("xmlui.administrative.mapper.BrowseItemForm.title");
	private static final Message T_trail = message("xmlui.administrative.mapper.BrowseItemForm.trail");
	private static final Message T_head1 = message("xmlui.administrative.mapper.BrowseItemForm.head1");
	private static final Message T_submit_unmap = message("xmlui.administrative.mapper.BrowseItemForm.submit_unmap");
	private static final Message T_column1 = message("xmlui.administrative.mapper.BrowseItemForm.column1");
	private static final Message T_column2 = message("xmlui.administrative.mapper.BrowseItemForm.column2");
	private static final Message T_column3 = message("xmlui.administrative.mapper.BrowseItemForm.column3");
	private static final Message T_column4 = message("xmlui.administrative.mapper.BrowseItemForm.column4");
	
	private static final Message T_no_remove = message("xmlui.administrative.mapper.BrowseItemForm.no_remove");

	protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
	protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
	
	public void addPageMeta(PageMeta pageMeta) throws WingException  
	{
		pageMeta.addMetadata("title").addContent(T_title);
		
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrail().addContent(T_mapper_trail);
		pageMeta.addTrail().addContent(T_trail);
	}

	
	public void addBody(Body body) throws SAXException, WingException, SQLException
	{
		// Get our parameters and state;
		UUID collectionID = UUID.fromString(parameters.getParameter("collectionID", null));
		Collection collection = collectionService.find(context,collectionID);
		
		List<Item> items = getMappedItems(collection);
		
		// DIVISION: browse-items
		Division div = body.addInteractiveDivision("browse-items",contextPath + "/admin/mapper", Division.METHOD_GET,"primary administrative mapper");
		div.setHead(T_head1);
		
		if (authorizeService.authorizeActionBoolean(context, collection, Constants.REMOVE))
		{
			Para actions = div.addPara();
			actions.addButton("submit_unmap").setValue(T_submit_unmap);
			actions.addButton("submit_return").setValue(T_submit_return);
		}
		else
		{
			Para actions = div.addPara();
			Button button = actions.addButton("submit_unmap");
			button.setValue(T_submit_unmap);
			button.setDisabled();
			actions.addButton("submit_return").setValue(T_submit_return);
			
			div.addPara().addHighlight("fade").addContent(T_no_remove);
		}
		
		Table table = div.addTable("browse-items-table",1,1);
		
		Row header = table.addRow(Row.ROLE_HEADER);
		header.addCellContent(T_column1);
		header.addCellContent(T_column2);
		header.addCellContent(T_column3);
		header.addCellContent(T_column4);
		
		for (Item item : items)
		{
			String itemID = String.valueOf(item.getID());
			Collection owningCollection = item.getOwningCollection();
			String owning = owningCollection.getName();
			String author = "unknown";
			List<MetadataValue> dcAuthors = itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "contributor", Item.ANY, Item.ANY);
			if (dcAuthors != null && dcAuthors.size() >= 1)
            {
                author = dcAuthors.get(0).getValue();
            }
			
			String title = "untitled";
			List<MetadataValue> dcTitles = itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);
			if (dcTitles != null && dcTitles.size() >= 1)
            {
                title = dcTitles.get(0).getValue();
            }

			String url = contextPath+"/handle/"+item.getHandle();
			
			Row row = table.addRow();
			
			CheckBox select = row.addCell().addCheckBox("itemID");
			select.setLabel("Select");
			select.addOption(itemID);
			
			row.addCellContent(owning);
			row.addCell().addXref(url,author);
			row.addCell().addXref(url,title);
		}
		
		if (authorizeService.authorizeActionBoolean(context, collection, Constants.REMOVE))
		{
			Para actions = div.addPara();
			actions.addButton("submit_unmap").setValue(T_submit_unmap);
			actions.addButton("submit_return").setValue(T_submit_return);
		}
		else
		{
			Para actions = div.addPara();
			Button button = actions.addButton("submit_unmap");
			button.setValue(T_submit_unmap);
			button.setDisabled();
			actions.addButton("submit_return").setValue(T_submit_return);
			
			div.addPara().addHighlight("fade").addContent(T_no_remove);
		}
		
		
		
		div.addHidden("administrative-continue").setValue(knot.getId());
	}
	
	
	/**
	 * Get a list of all items that are mapped from other collections.
	 * 
	 * @param collection The collection to look in.
	 */
	private List<Item> getMappedItems(Collection collection) throws SQLException
	{

		ArrayList<Item> items = new ArrayList<Item>();
		
		// get all items from that collection
        Iterator<Item> iterator = itemService.findByCollection(context, collection);
		while (iterator.hasNext())
		{
			Item item = iterator.next();

			if (! itemService.isOwningCollection(item, collection))
			{
				items.add(item);
			}
		}

        return items;
	}

}
