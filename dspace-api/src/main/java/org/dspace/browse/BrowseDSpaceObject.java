/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.sql.SQLException;

import org.dspace.content.DCValue;
import org.dspace.core.Context;

public class BrowseDSpaceObject extends BrowseItem
{
    private BrowsableDSpaceObject browseObject;

    public BrowseDSpaceObject(Context context, BrowsableDSpaceObject browseObject)
    {
        super(context, browseObject.getID(), browseObject.isArchived(), browseObject.isWithdrawn());
        this.browseObject = browseObject;
    }
    
    @Override
    public DCValue[] getMetadata(String schema, String element,
            String qualifier, String lang) throws SQLException
    {
        return browseObject.getMetadata(schema, element, qualifier, lang);
    }
    
    public BrowsableDSpaceObject getBrowsableDSpaceObject()
    {
        return browseObject;
    }

}
