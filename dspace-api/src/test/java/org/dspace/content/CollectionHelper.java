/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.content;

import java.io.IOException;
import java.sql.SQLException;
import org.dspace.authorize.AuthorizeException;

/**
 * Give tests access to package-private operations on Collection.
 * @author mwood
 */
public class CollectionHelper
{
    /**
     * Delete the Collection by calling {@link org.dspace.content.Collection#delete()}.
     *
     * @param collection to be deleted.
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     * @throws IOException passed through.
     */
    static public void delete(Collection collection)
            throws SQLException, AuthorizeException, IOException
    {
        collection.delete();
    }
}
