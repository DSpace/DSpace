/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
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
