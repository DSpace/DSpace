/*
 * MoveItemForm.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
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
package org.dspace.app.xmlui.aspect.administrative.item;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.handle.HandleManager;


/**
 * This page displays collections to which the user can move an item.
 * 
 * @author Nicholas Riley
 */
public class MoveItemForm extends AbstractDSpaceTransformer {

	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");
	private static final Message T_submit_cancel = message("xmlui.general.cancel");
	
	private static final Message T_title = message("xmlui.administrative.item.MoveItemForm.title");
	private static final Message T_trail = message("xmlui.administrative.item.MoveItemForm.trail");
	private static final Message T_head1 = message("xmlui.administrative.item.MoveItemForm.head1");
	private static final Message T_collection = message("xmlui.administrative.item.MoveItemForm.collection");
	private static final Message T_collection_help = message("xmlui.administrative.item.MoveItemForm.collection_help");
	private static final Message T_collection_default = message("xmlui.administrative.item.MoveItemForm.collection_default");
	private static final Message T_submit_move = message("xmlui.administrative.item.MoveItemForm.submit_move");


	public void addPageMeta(PageMeta pageMeta) throws WingException
	{
		pageMeta.addMetadata("title").addContent(T_title);
		
		
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrailLink(contextPath+"/admin/item", T_item_trail);
		pageMeta.addTrail().addContent(T_trail);
	}

	public void addBody(Body body) throws WingException, SQLException 
	{
        // Get our parameters and state
        int itemID = parameters.getParameterAsInteger("itemID",-1);
        Item item = Item.find(context, itemID);
        
        // DIVISION: Main
        Division main = body.addInteractiveDivision("move-item", contextPath+"/admin/item", Division.METHOD_POST, "primary administrative item");
        main.setHead(T_head1.parameterize(item.getHandle()));

        Collection[] collections = Collection.findAuthorized(context, null, Constants.ADD);

        List list = main.addList("select-collection", List.TYPE_FORM);
        Select select = list.addItem().addSelect("collectionID");
        select.setLabel(T_collection);
        select.setHelp(T_collection_help);
        
        Collection owningCollection = item.getOwningCollection();
        if (owningCollection == null) {
            select.addOption("",T_collection_default);
        }
        
        for (Collection collection : collections)
        {
            String name = collection.getMetadata("name");
            if (name.length() > 50)
                name = name.substring(0, 47) + "...";
            select.addOption(collection.equals(owningCollection), collection.getID(), name);
        }
        
        org.dspace.app.xmlui.wing.element.Item actions = list.addItem();
        actions.addButton("submit_move").setValue(T_submit_move);
		actions.addButton("submit_cancel").setValue(T_submit_cancel);

		main.addHidden("administrative-continue").setValue(knot.getId());
	}

	/**
	 * Compare two metadata element's name so that they may be sorted.
	 */
	class DCValueComparator implements Comparator{
		public int compare(Object arg0, Object arg1) {
			final DCValue o1 = (DCValue)arg0;
			final DCValue o2 = (DCValue)arg1;
			final String s1 = o1.schema + o1.element + (o1.qualifier==null?"":("." + o1.qualifier));
			final  String s2 = o2.schema + o2.element + (o2.qualifier==null?"":("." + o2.qualifier));
			return s1.compareTo(s2);
		}
	}

}
