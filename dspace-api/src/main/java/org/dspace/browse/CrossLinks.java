/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.util.HashMap;
import java.util.Map;

import org.dspace.core.ConfigurationManager;

/**
 * Class to represent the configuration of the cross-linking between browse
 * pages (for example, between the author name in one full listing to the
 * author's list of publications).
 * 
 * @author Richard Jones
 *
 */
public class CrossLinks
{
	/** a map of the desired links */
	private Map<String, String> links = new HashMap<String, String>();
	
	/**
	 * Construct a new object which will obtain the configuration for itself.
	 * 
	 * @throws BrowseException if browse error
	 */
	public CrossLinks()
		throws BrowseException
	{
		int i = 1;
		while (true)
		{
			String field = "webui.browse.link." + i;
			String config = ConfigurationManager.getProperty(field);
			if (config == null)
			{
				break;
			}
			
			String[] parts = config.split(":");
			if (parts.length != 2)
			{
				throw new BrowseException("Invalid configuration for " + field + ": " + config);
			}
			links.put(parts[1], parts[0]);
			i++;
		}
	}

	/**
	 * Is there a link for the given canonical form of metadata (i.e. schema.element.qualifier)?
	 * 
	 * @param metadata	the metadata to check for a link on
         * @return true/false
	 */
	public boolean hasLink(String metadata)
	{
		return links.containsKey(metadata);
	}
	
	/**
	 * Get the type of link that the bit of metadata has.
	 * 
	 * @param metadata	the metadata to get the link type for
         * @return type
	 */
	public String getLinkType(String metadata)
	{
		return links.get(metadata);
	}
}
