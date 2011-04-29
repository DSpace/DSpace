/*
 * Item.java
 *
 * Version: $Revision: 4196 $
 *
 * Date: $Date: 2009-08-06 08:29:46 -0500 (Thu, 06 Aug 2009) $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its
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

package org.dspace.harvest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * @author Alexey Maslov
 */

public class HarvestedItem 
{
	private Context context;
	private TableRow harvestRow;
	boolean modified;
	
	
	HarvestedItem(Context c, TableRow row)
    {
        context = c;
        harvestRow = row;
        modified = false;
    }
    
    
    public static void exists(Context c) throws SQLException {
    	DatabaseManager.queryTable(c, "harvested_item", "SELECT COUNT(*) FROM harvested_item");    	
    }
	
	
    /**
     * Find the harvest parameters corresponding to the specified DSpace item 
     * @return a HarvestedItem object corresponding to this item, null if not found.
     */
    public static HarvestedItem find(Context c, int item_id) throws SQLException 
    {
    	TableRow row = DatabaseManager.findByUnique(c, "harvested_item", "item_id", item_id);
    	
    	if (row == null) {
    		return null;
    	}
    	
    	return new HarvestedItem(c, row);
    }
    
    
    /*
     * select foo.item_id from (select item.item_id, item.owning_collection from item join item2bundle on item.item_id=item2bundle.item_id where item2bundle.bundle_id=22) as foo join collection on foo.owning_collection=collection.collection_id where collection.collection_id=5;
     */
    
    /**
     * Retrieve a DSpace Item that corresponds to this particular combination of owning collection and OAI ID. 
     * @param context 
     * @param itemOaiID the string used by the OAI-PMH provider to identify the item
     * @param collectionID id of the local collection that the item should be found in
     * @return DSpace Item or null if no item was found
     */
    public static Item getItemByOAIId(Context context, String itemOaiID, int collectionID) throws SQLException
    {
    	/*
         * FYI: This method has to be scoped to a collection. Otherwise, we could have collisions as more 
         * than one collection might be importing the same item. That is OAI_ID's might be unique to the 
         * provider but not to the harvester.
         */
   	 	Item resolvedItem = null;
        TableRowIterator tri = null;
        final String selectItemFromOaiId = "SELECT dsi.item_id FROM " + 
        	"(SELECT item.item_id, item.owning_collection FROM item JOIN harvested_item ON item.item_id=harvested_item.item_id WHERE harvested_item.oai_id=?) " + 
        	"dsi JOIN collection ON dsi.owning_collection=collection.collection_id WHERE collection.collection_id=?";
        
        try
        {
            tri = DatabaseManager.query(context, selectItemFromOaiId, itemOaiID, collectionID);

            if (tri.hasNext())
            {
                TableRow row = tri.next();
                int itemID = row.getIntColumn("item_id");
                resolvedItem = Item.find(context, itemID);
            }
            else {
           	 return null;
            }
        }
        finally {
            if (tri != null)
                tri.close();
        }

        return resolvedItem;
    }
        
    /**
     * Create a new harvested item row for a specified item id.  
     * @return a new HarvestedItem object
     */
    public static HarvestedItem create(Context c, int itemId, String itemOAIid) throws SQLException {
    	TableRow row = DatabaseManager.create(c, "harvested_item");
    	row.setColumn("item_id", itemId);
    	row.setColumn("oai_id", itemOAIid);
    	DatabaseManager.update(c, row);
    	
    	return new HarvestedItem(c, row);    	
    }
    
    
    public String getItemID()
    {
        String oai_id = harvestRow.getStringColumn("item_id");

        return oai_id;
    }

    /**
     * Get the oai_id associated with this item 
     */
    public String getOaiID()
    {
        String oai_id = harvestRow.getStringColumn("oai_id");

        return oai_id;
    }
    
    /**
     * Set the oai_id associated with this item 
     */
    public void setOaiID(String itemOaiID)
    {
    	harvestRow.setColumn("oai_id",itemOaiID);
        return;
    }
    
    
    public void setHarvestDate(Date date) {
    	if (date == null) {    	
    		date = new Date();
    	}
    	harvestRow.setColumn("last_harvested", date);
    	modified = true;
    }
    
    public Date getHarvestDate() {
    	return harvestRow.getDateColumn("last_harvested");
    }
    
    
    
    public void delete() throws SQLException {
    	DatabaseManager.delete(context, harvestRow);
    }
    
    
    
    public void update() throws SQLException, IOException, AuthorizeException {
        DatabaseManager.update(context, harvestRow);
    }

}
