/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.xmlui.aspect.administrative.mapper;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

/**
 * Generic search.  See plug-ins derived from this interface.
 *
 * @author mwood
 */
interface SearchRequestProcessor
{
    /**
     * Search for Items to be mapped into a Collection.
     *
     * @param context session context.
     * @param query matches the interesting Items.
     * @param collection into which the found Items may be mapped.
     * @return found Items.
     * @throws IOException whenever.
     * @throws SQLException whenever.
     */
    List<DSpaceObject> doItemMapSearch(Context context, String query, Collection collection)
            throws IOException, SQLException;
}
