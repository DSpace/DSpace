/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

import org.dspace.core.Context;

/**
 *    Interface for actions to update an item
 *
 */
public interface UpdateAction 
{
	/**
	 *   Action to update item
	 * 
	 * @param context
	 * @param itarch
	 * @param isTest
	 * @param suppressUndo
	 * @throws Exception
	 */
	public void execute(Context context, ItemArchive itarch, boolean isTest, boolean suppressUndo) 
	throws Exception;
	
}
