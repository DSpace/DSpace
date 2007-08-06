/*
 * CrossLinks.java
 *
 * Version: $Revision: 1.9 $
 *
 * Date: $Date: 2005/04/20 14:23:12 $
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.browse;

import java.util.HashMap;

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
	private HashMap links = new HashMap();
	
	/**
	 * Construct a new object which will obtain the configuration for itself
	 * 
	 * @throws BrowseException
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
	 * Is there a link for the given canonical form of metadata (i.e. schema.element.qualifier)
	 * 
	 * @param metadata	the metadata to check for a link on
	 * @return
	 */
	public boolean hasLink(String metadata)
	{
		return links.containsKey(metadata);
	}
	
	/**
	 * get the type of link that the bit of metadata has
	 * 
	 * @param metadata	the metadata to get the link type for
	 * @return
	 */
	public String getLinkType(String metadata)
	{
		return (String) links.get(metadata);
	}
}
