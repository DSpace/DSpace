/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.mapper;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.PluginConfigurationError;
import org.dspace.core.factory.CoreServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Search for items from other collections to map into this collection.
 * 
 * @author Scott Phillips
 */

public class SearchItemForm extends AbstractDSpaceTransformer {

	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_submit_cancel = message("xmlui.general.cancel");
	private static final Message T_mapper_trail = message("xmlui.administrative.mapper.general.mapper_trail");
	
	private static final Message T_title = message("xmlui.administrative.mapper.SearchItemForm.title");
	private static final Message T_trail = message("xmlui.administrative.mapper.SearchItemForm.trail");
	private static final Message T_head1 = message("xmlui.administrative.mapper.SearchItemForm.head1");
	private static final Message T_submit_map = message("xmlui.administrative.mapper.SearchItemForm.submit_map");
	private static final Message T_column1 = message("xmlui.administrative.mapper.SearchItemForm.column1");
	private static final Message T_column2 = message("xmlui.administrative.mapper.SearchItemForm.column2");
	private static final Message T_column3 = message("xmlui.administrative.mapper.SearchItemForm.column3");
	private static final Message T_column4 = message("xmlui.administrative.mapper.SearchItemForm.column4");

    private static final Logger log = LoggerFactory.getLogger(SearchItemForm.class);

	protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    @Override
	public void addPageMeta(PageMeta pageMeta) throws WingException  
	{
		pageMeta.addMetadata("title").addContent(T_title);
		
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrail().addContent(T_mapper_trail);
		pageMeta.addTrail().addContent(T_trail);
	}

	
    @Override
	public void addBody(Body body) throws SAXException, WingException, SQLException, IOException
	{
		// Get our parameters and state;
		UUID collectionID = UUID.fromString(parameters.getParameter("collectionID", null));
		Collection collection = collectionService.find(context,collectionID);
		
		String query = decodeFromURL(parameters.getParameter("query",null));
		java.util.List<Item> items = performSearch(collection,query);
		
		
		
		// DIVISION: manage-mapper
		Division div = body.addInteractiveDivision("search-items",contextPath + "/admin/mapper", Division.METHOD_GET,"primary administrative mapper");
		div.setHead(T_head1.parameterize(query));
		
		Para actions = div.addPara();
		actions.addButton("submit_map").setValue(T_submit_map);
		actions.addButton("submit_cancel").setValue(T_submit_cancel);
		
		Table table = div.addTable("search-items-table",1,1);
		
		Row header = table.addRow(Row.ROLE_HEADER);
		header.addCellContent(T_column1);
		header.addCellContent(T_column2);
		header.addCellContent(T_column3);
		header.addCellContent(T_column4);
		
		for (Item item : items)
		{
			String itemID = String.valueOf(item.getID());
			Collection owningCollection = item.getOwningCollection();
			String owning = "unknown";
			if (owningCollection != null)
				owning = owningCollection.getName();
			String author = "unknown";
			List<MetadataValue> dcCreators = itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "creator", Item.ANY, Item.ANY);
			if (dcCreators != null && dcCreators.size() >= 1)
            {
                author = dcCreators.get(0).getValue();
            } else {
            	// Do a fallback look for contributors
				List<MetadataValue> dcContributors = itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "contributor", Item.ANY, Item.ANY);
				if (dcContributors != null && dcContributors.size() >= 1)
	            {
	                author = dcContributors.get(0).getValue();
	            }
			}
			
			String title = "untitled";
			List<MetadataValue> dcTitles = itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);
			if (dcTitles != null && dcTitles.size() >= 1)
            {
                title = dcTitles.get(0).getValue();
            }

			String url = contextPath+"/handle/"+item.getHandle();
			
			Row row = table.addRow();

            boolean canBeMapped = true;
            List<Collection> collections = item.getCollections();
            for (Collection c : collections)
            {
                if (c.getID() == collectionID)
                {
                    canBeMapped = false;
                }
            }

            if (canBeMapped)
            {
                CheckBox select = row.addCell().addCheckBox("itemID");
                select.setLabel("Select");
                select.addOption(itemID);
            }
            else
            {
                row.addCell().addContent("");
            }

			row.addCellContent(owning);
			row.addCell().addXref(url,author);
			row.addCell().addXref(url,title);
		}
		
		actions = div.addPara();
		actions.addButton("submit_map").setValue(T_submit_map);
		actions.addButton("submit_cancel").setValue(T_submit_cancel);
		
		
		
		
		div.addHidden("administrative-continue").setValue(knot.getId());
	}
	
	
	/**
	 * Search the repository for items in other collections that can be mapped into this one.
	 * 
	 * @param collection The collection to map into
	 * @param query The search query.
	 */
	private java.util.List<Item> performSearch(Collection collection, String query) throws SQLException, IOException
	{
        // Which search provider do we use?
        SearchRequestProcessor processor = null;
        try {
            processor = (SearchRequestProcessor) CoreServiceFactory.getInstance().getPluginService()
                    .getSinglePlugin(SearchRequestProcessor.class);
        } catch (PluginConfigurationError e) {
            log.warn("{} not properly configured.  Please configure the {} plugin.  {}",
                    new Object[] {
                        SearchItemForm.class.getName(),
                        SearchRequestProcessor.class.getName(),
                        e.getMessage()
                    });
        }
        if (processor == null)
        {   // Discovery is the default search provider since DSpace 4.0
            processor = new DiscoverySearchRequestProcessor();
        }

        // Search the repository
        List<DSpaceObject> results = processor.doItemMapSearch(context, query, collection);

        // Get a list of found items
        ArrayList<Item> items = new ArrayList<Item>();
        for (DSpaceObject resultDSO : results)
        {
            if (resultDSO instanceof Item)
            {
            	Item item = (Item) resultDSO;

            	if (!itemService.isOwningCollection(item, collection))
                {
                    items.add(item);
                }
            }
        }

        return items;
    }

}
