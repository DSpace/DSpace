/*
 * MapperMain.java
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

import java.sql.SQLException;

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
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Constants;
import org.xml.sax.SAXException;

/**
 * Manage the mapping of items into this collection, allow the user to 
 * search for new items to import or browse a list of currently mapped 
 * items.
 * 
 * @author Scott phillips
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
	
	public void addPageMeta(PageMeta pageMeta) throws WingException  
	{
		pageMeta.addMetadata("title").addContent(T_title);
		
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrail().addContent(T_mapper_trail);
	}

	
	public void addBody(Body body) throws SAXException, WingException, SQLException
	{
		// Get our parameters and state;
		int collectionID = parameters.getParameterAsInteger("collectionID",-1);
		Collection collection = Collection.find(context,collectionID);
		
		int[] counts = getNumberOfMappedAndUnmappedItems(collection);
		int count_native = counts[0];
		int count_import = counts[1];
		
		
		
		
		// DIVISION: manage-mapper
		Division div = body.addInteractiveDivision("manage-mapper",contextPath + "/admin/mapper", Division.METHOD_GET,"primary administrative mapper");
		div.setHead(T_head1);
		
		div.addPara(T_para1.parameterize(collection.getMetadata("name")));
		
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
		if (!AuthorizeManager.authorizeActionBoolean(context, collection, Constants.ADD))
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
        ItemIterator iterator = collection.getItems();

        // iterate through the items in this collection, and count how many
        // are native, and how many are imports, and which collections they
        // came from
        while (iterator.hasNext())
        {
            Item item = iterator.next();

            if (item.isOwningCollection(collection))
                count_native++;
            else
                count_import++;
        }	
        
        int[] counts = new int[2];
        counts[0] = count_native;
        counts[1] = count_import;
        return counts;
	}

}
