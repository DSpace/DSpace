/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.utils;

import java.sql.SQLException;
import org.dspace.content.Collection;
import org.dspace.content.Community;

/**
 * Utility class for lists of collections.
 */

public class CollectionDropDown {

    /**
     * Get full path starting from a top-level community via subcommunities down to a collection.
     * The full path will not be truncated.
     * 
     * @param col 
     *            Get full path for this collection
     * @return Full path to the collection
     */
    public static String collectionPath(Collection col) throws SQLException
    {
        return CollectionDropDown.collectionPath(col, 0);
    }
    
    /**
     * Get full path starting from a top-level community via subcommunities down to a collection.
     * The full cat will be truncated to the specified number of characters and prepended with an ellipsis.
     * 
     * @param col 
     *            Get full path for this collection
     * @param maxchars 
     *            Truncate the full path to maxchar characters. 0 means do not truncate.
     * @return Full path to the collection (truncated)
     */
    public static String collectionPath(Collection col, int maxchars) throws SQLException
    {

        Community[] getCom = null;
        String name = "";
        getCom = col.getCommunities(); // all communities containing given collection
        for (Community com : getCom) {
            name = com.getMetadata("name") + "/" + name;
        }

        name = name + col.getMetadata("name");

        if (maxchars != 0) {
            int len = name.length();
            if (len > maxchars) {
                name = "…" + name.substring(len - (maxchars - 1), len);
            }
        }

        return name;
    }
}
