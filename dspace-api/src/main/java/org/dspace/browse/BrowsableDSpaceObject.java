/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.util.Map;

import org.dspace.content.Metadatum;
import org.dspace.discovery.IGlobalSearchResult;

public interface BrowsableDSpaceObject extends IGlobalSearchResult
{
	public Map<String, Object> getExtraInfo();

    public boolean isArchived();

    public boolean isWithdrawn();

    public Metadatum[] getMetadata(String schema, String element,
            String qualifier, String lang);

    public int getID();

	public boolean isDiscoverable();
}
