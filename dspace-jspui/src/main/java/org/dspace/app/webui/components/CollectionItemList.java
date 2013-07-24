/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.components;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.plugin.CollectionHomeProcessor;
import org.dspace.plugin.PluginException;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowserScope;
import org.dspace.browse.BrowseException;

/**
 * This class obtains item list of the given collection by
 * implementing the CollectionHomeProcessor.
 * 
 * @author Keiji Suzuki
 *
 */
public class CollectionItemList implements CollectionHomeProcessor
{
    // the name of browse index
    private static String type = ConfigurationManager.getProperty("webui.collectionhome.browse-name");
    static
    {
        if (type == null)
        {
            type = "dateissued";
        }
    }

    // 
    private static final int etal    = ConfigurationManager.getIntProperty("webui.browse.author-limit", -1);
    //
    private static final int perpage = ConfigurationManager.getIntProperty("webui.collectionhome.perpage", 20);

	/**
	 * blank constructor - does nothing.
	 *
	 */
	public CollectionItemList()
	{
		
	}
	
	/* (non-Javadoc)
	 * @see org.dspace.plugin.CommunityHomeProcessor#process(org.dspace.core.Context, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.dspace.content.Community)
	 */
	public void process(Context context, HttpServletRequest request, HttpServletResponse response, Collection collection) 
		throws PluginException, AuthorizeException
	{
        try
        {
            BrowseIndex bi = BrowseIndex.getBrowseIndex(type);
            if (bi == null || !"item".equals(bi.getDisplayType()))
            {
                request.setAttribute("show.title", new Boolean(false));
                return;
            }

            BrowserScope scope = new BrowserScope(context);
            scope.setBrowseContainer(collection);
            scope.setBrowseIndex(bi);
            scope.setEtAl(etal);
            scope.setResultsPerPage(perpage);  
            BrowseEngine be = new BrowseEngine(context);
            BrowseInfo binfo = be.browse(scope);
            request.setAttribute("browse.info", binfo);

            if (binfo.hasResults())
            {
                request.setAttribute("show.title", new Boolean(true));
            }
            else
            {
                request.setAttribute("show.title", new Boolean(false));
            }
        }
        catch (BrowseException e)
        {
            request.setAttribute("show.title", new Boolean(false));
        }
	}
}
