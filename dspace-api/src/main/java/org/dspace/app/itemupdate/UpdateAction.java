/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;

/**
 *    Interface for actions to update an item
 *
 */
public interface UpdateAction 
{
    public ItemService itemService = ContentServiceFactory.getInstance().getItemService();


	/**
	 * Action to update item
	 * 
	 * @param context DSpace context
	 * @param itarch item archive
	 * @param isTest test flag
	 * @param suppressUndo undo flag
	 * @throws Exception if error
	 */
	public void execute(Context context, ItemArchive itarch, boolean isTest, boolean suppressUndo) 
	throws Exception;
	
}
