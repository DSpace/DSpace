/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import org.dspace.core.Context;
import org.dspace.content.DSpaceObject;

/**
 * Interface for data access of cached community and collection item count
 * information
 * 
 * @author Richard Jones
 *
 */
public interface ItemCountDAO
{
	/**
	 * Set the DSpace Context to use during data access
	 * 
	 * @param context DSpace Context
	 * @throws ItemCountException if count error
	 */
	public void setContext(Context context) throws ItemCountException;
	
	/**
	 * Get the number of items in the given DSpaceObject container.  This method will
	 * only succeed if the DSpaceObject is an instance of either a Community or a
	 * Collection.  Otherwise it will throw an exception.
	 * 
	 * @param dso Dspace Object
         * @return count
	 * @throws ItemCountException if count error
	 */
	public int getCount(DSpaceObject dso) throws ItemCountException;
}
