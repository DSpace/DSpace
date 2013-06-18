package org.dspace.browse;

import org.dspace.content.DCValue;

public interface BrowsableDSpaceObject
{

    public boolean isArchived();

    public boolean isWithdrawn();

    public DCValue[] getMetadata(String schema, String element,
            String qualifier, String lang);

    public int getID();
}
