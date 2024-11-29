/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

/**
 * Interface for data access of cached community and collection item count
 * information
 */
public interface ItemCountDAO {

    /**
     * Get the number of items in the given DSpaceObject container.  This method will
     * only succeed if the DSpaceObject is an instance of either a Community or a
     * Collection.  Otherwise it will throw an exception.
     *
     * @param context DSpace context
     * @param dso Dspace Object
     * @return count
     */
    int getCount(Context context, DSpaceObject dso);
}
