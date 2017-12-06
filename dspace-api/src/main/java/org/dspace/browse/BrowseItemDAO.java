/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import org.dspace.content.Metadatum;

import java.sql.SQLException;

public interface BrowseItemDAO
{
    /**
     * Get an array of all the items in the database
     *
     * @return array of items
     * @throws java.sql.SQLException
     */
    public BrowseItem[] findAll()
        throws SQLException;

    /**
     * perform a database query to obtain the string array of values corresponding to
     * the passed parameters.  In general you should use:
     *
     * <code>
     * getMetadata(schema, element, qualifier, lang);
     * </code>
     *
     * As this will obtain the value from cache if available first.
     *
     * @param itemId
     * @param schema
     * @param element
     * @param qualifier
     * @param lang
     * @return matching metadata values.
     * @throws SQLException
     */
    public Metadatum[] queryMetadata(int itemId, String schema, String element, String qualifier, String lang)
    	throws SQLException;
}
