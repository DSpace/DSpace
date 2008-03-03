/*
 * SearchItemForm.java
 *
 * Version: $Revision: 1.3 $
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
package org.dspace.app.xmlui.aspect.administrative.mapper;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

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
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.handle.HandleManager;
import org.dspace.search.DSQuery;
import org.dspace.search.QueryArgs;
import org.dspace.search.QueryResults;
import org.xml.sax.SAXException;

/**
 * Search for items from other collections to map into this collection.
 * 
 * @author Scott phillips
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
	
	
	public void addPageMeta(PageMeta pageMeta) throws WingException  
	{
		pageMeta.addMetadata("title").addContent(T_title);
		
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrail().addContent(T_mapper_trail);
		pageMeta.addTrail().addContent(T_trail);
	}

	
	public void addBody(Body body) throws SAXException, WingException, SQLException, IOException
	{
		// Get our parameters and state;
		int collectionID = parameters.getParameterAsInteger("collectionID",-1);
		Collection collection = Collection.find(context,collectionID);	
		
		String query = URLDecode(parameters.getParameter("query",null));
		ArrayList<Item> items = preformSearch(collection,query);
		
		
		
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
			String owning = owningCollection.getMetadata("name");
			String author = "unkown";
			DCValue[] dcAuthors = item.getDC("contributor",Item.ANY,Item.ANY);
			if (dcAuthors != null && dcAuthors.length >= 1)
				author = dcAuthors[0].value;
			
			String title = "untitled";
			DCValue[] dcTitles = item.getDC("title",null,Item.ANY);
			if (dcTitles != null && dcTitles.length >= 1)
				title = dcTitles[0].value;

			String url = contextPath+"/handle/"+item.getHandle();
			
			Row row = table.addRow();
			
			CheckBox select = row.addCell().addCheckBox("itemID");
			select.setLabel("Select");
			select.addOption(itemID);
			
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
	 * @param collection The collection to mapp into
	 * @param query The search query.
	 */
	private ArrayList<Item> preformSearch(Collection collection, String query) throws SQLException, IOException
	{
		
		// Search the repository
        QueryArgs queryArgs = new QueryArgs();
        queryArgs.setQuery(query);
        queryArgs.setPageSize(Integer.MAX_VALUE);
        QueryResults results = DSQuery.doQuery(context, queryArgs);
        

        // Get a list of found items
        ArrayList<Item> items = new ArrayList<Item>();
        @SuppressWarnings("unchecked")
        java.util.List<String> handles = results.getHitHandles();
        for (String handle : handles)
        {
            DSpaceObject resultDSO = HandleManager.resolveToObject(context, handle);

            if (resultDSO instanceof Item)
            {
            	Item item = (Item) resultDSO;
            	
            	if (!item.isOwningCollection(collection))
            		items.add(item);
            }
        }
        
        return items;
	}
	
	
	
}
