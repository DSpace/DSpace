/*
 * RecentSubmissionsManager.java
 *
 * Version: $Revision:  $
 *
 * Date: $Date:  $
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
package org.dspace.app.webui.components;

import org.dspace.core.Context;
import org.dspace.content.DSpaceObject;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowserScope;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowseException;
import org.dspace.sort.SortOption;
import org.dspace.sort.SortException;
import org.dspace.core.ConfigurationManager;
import org.dspace.content.Item;

import org.apache.log4j.Logger;

/**
 * Class that obtains recent submissions to DSpace containers.
 * @author rdjones
 *
 */
public class RecentSubmissionsManager
{
	/** logger */
	private static Logger log = Logger.getLogger(RecentSubmissionsManager.class);
	
	/** DSpace context */
	private Context context;
	
	/**
	 * Construct a new RecentSubmissionsManager with the given DSpace context
	 * 
	 * @param context
	 */
	public RecentSubmissionsManager(Context context)
	{
		this.context = context;
	}

	/**
	 * Obtain the recent submissions from the given container object.  This
	 * method uses the configuration to determine which field and how many
	 * items to retrieve from the DSpace Object.
	 * 
	 * If the object you pass in is not a Community or Collection (e.g. an Item
	 * is a DSpaceObject which cannot be used here), an exception will be thrown
	 * 
	 * @param dso	DSpaceObject: Community or Collection
	 * @return		The recently submitted items
	 * @throws RecentSubmissionsException
	 */
	public RecentSubmissions getRecentSubmissions(DSpaceObject dso)
		throws RecentSubmissionsException
	{
		try
		{
			// get our configuration
			String source = ConfigurationManager.getProperty("recent.submissions.sort-option");
			String count = ConfigurationManager.getProperty("recent.submissions.count");
			
			// prep our engine and scope
			BrowseEngine be = new BrowseEngine(context);
			BrowserScope bs = new BrowserScope(context);
			BrowseIndex bi = BrowseIndex.getItemBrowseIndex();
			
			// fill in the scope with the relevant gubbins
			bs.setBrowseIndex(bi);
			bs.setOrder(SortOption.DESCENDING);
			bs.setResultsPerPage(Integer.parseInt(count));
			bs.setBrowseContainer(dso);
            for (SortOption so : SortOption.getSortOptions())
            {
                if (so.getName().equals(source))
                    bs.setSortBy(so.getNumber());
            }
			
			BrowseInfo results = be.browseMini(bs);
			
			Item[] items = results.getBrowseItemResults(context);
			
			RecentSubmissions rs = new RecentSubmissions(items);
			
			return rs;
		}
        catch (SortException se)
        {
            log.error("caught exception: ", se);
            throw new RecentSubmissionsException(se);
        }
		catch (BrowseException e)
		{
			log.error("caught exception: ", e);
			throw new RecentSubmissionsException(e);
		}
	}
	
}
