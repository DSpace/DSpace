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
import java.util.UUID;

import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
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

	protected static final AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
	protected static final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();



	/**
	 * Map the given items into this collection
	 * 
	 * @param context The current DSpace content
	 * @param collectionID The collection to map items into.
	 * @param itemIDs The items to map.
	 * @return Flow result
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws org.dspace.app.xmlui.utils.UIException passed through.
     * @throws java.io.IOException passed through.
	 */
	public static FlowResult processMapItems(Context context, UUID collectionID, String[] itemIDs)
            throws SQLException, AuthorizeException, UIException, IOException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);

		Collection toCollection = collectionService.find(context,collectionID);

		for (String itemID : itemIDs)
        {
            Item item = itemService.find(context, UUID.fromString(itemID));

            if (authorizeService.authorizeActionBoolean(context, item, Constants.READ))
            {
                // make sure item doesn't belong to this collection
                if (!itemService.isOwningCollection(item, toCollection))
                {
                    collectionService.addItem(context, toCollection, item);
                }
            }
        }
		

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
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws org.dspace.app.xmlui.utils.UIException passed through.
     * @throws java.io.IOException passed through.
	 */
	public static FlowResult processUnmapItems(Context context, UUID collectionID, String[] itemIDs)
            throws SQLException, AuthorizeException, UIException, IOException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);

		Collection toCollection = collectionService.find(context,collectionID);
		
		for (String itemID : itemIDs)
        {
            Item item = itemService.find(context, UUID.fromString(itemID));

            if (authorizeService.authorizeActionBoolean(context, item, Constants.READ))
            {
                // make sure item doesn't belong to this collection
                if (!itemService.isOwningCollection(item, toCollection))
                {
                    collectionService.removeItem(context, toCollection, item);
                }
            }
        }

		result.setContinue(true);
		result.setOutcome(true);
		result.setMessage(T_unmaped_items);
		
		return result;
	}
	
}
