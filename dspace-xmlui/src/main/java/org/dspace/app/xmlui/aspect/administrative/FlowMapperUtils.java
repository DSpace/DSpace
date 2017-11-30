/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.browse.BrowseException;
import org.dspace.browse.IndexBrowse;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 *
 * @author Scott Phillips
 */
public class FlowMapperUtils 
{

	/** Language Strings */
	private static final Message T_map_items = new Message("default","xmlui.administrative.FlowMapperUtils.map_items");
	private static final Message T_unmaped_items = new Message("default","xmlui.administrative.FlowMapperUtils.unmaped_items");	
	

	/**
	 * Map the given items into this collection
	 * 
	 * @param context The current DSpace content
	 * @param collectionID The collection to map items into.
	 * @param itemIDs The items to map.
	 * @return Flow result
	 */
	public static FlowResult processMapItems(Context context, int collectionID, String[] itemIDs) throws SQLException, AuthorizeException, UIException, IOException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);

		Collection toCollection = Collection.find(context,collectionID);
		
		for (String itemID : itemIDs)
        {
            Item item = Item.find(context, Integer.valueOf(itemID));

            if (AuthorizeManager.authorizeActionBoolean(context, item, Constants.READ))
            {
                // make sure item doesn't belong to this collection
                if (!item.isOwningCollection(toCollection))
                {
                    toCollection.addItem(item);
                    // FIXME Exception handling
                    try
                    {
                    	IndexBrowse ib = new IndexBrowse(context);
                    	ib.indexItem(item);
                    }
                    catch (BrowseException bex)
                    {
                    	throw new UIException("Unable to process browse", bex);
                    }
                }
            }
        }
		
		context.commit();
		
		result.setContinue(true);
		result.setOutcome(true);
		result.setMessage(T_map_items);
		
		return result;
	}
	
	/**
	 * Unmap the given items from this collection
	 * 
	 * @param context The DSpace context
	 * @param collectionID The collection to unmap these items from.
	 * @param itemIDs The items to be unmapped.
	 * @return A flow result
	 */
	public static FlowResult processUnmapItems(Context context, int collectionID, String[] itemIDs) throws SQLException, AuthorizeException, UIException, IOException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);

		Collection toCollection = Collection.find(context,collectionID);
		
		for (String itemID : itemIDs)
        {
            Item item = Item.find(context, Integer.valueOf(itemID));

            if (AuthorizeManager.authorizeActionBoolean(context, item, Constants.READ))
            {
                // make sure item doesn't belong to this collection
                if (!item.isOwningCollection(toCollection))
                {
                    toCollection.removeItem(item);
                    // FIXME Exception handling
                    try
                    {
                    	IndexBrowse ib = new IndexBrowse(context);
                    	ib.indexItem(item);
                    }
                    catch (BrowseException bex)
                    {
                    	throw new UIException("Unable to process browse", bex);
                    }
                }
            }
        }
		
		context.commit();
		
		result.setContinue(true);
		result.setOutcome(true);
		result.setMessage(T_unmaped_items);
		
		return result;
	}
	
}
