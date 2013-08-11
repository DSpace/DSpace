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
import org.dspace.app.webui.util.UIUtil;
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
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;

/**
 * This class obtains item list of the given collection by
 * implementing the CollectionHomeProcessor.
 * 
 * @author Keiji Suzuki
 *
 */
public class CollectionItemList implements CollectionHomeProcessor
{
    // the name of a browse index to display collection's items
    private static String name = ConfigurationManager.getProperty("webui.collectionhome.browse-name");
    // the number of authors to display before trncating
    private static final int etal    = ConfigurationManager.getIntProperty("webui.browse.author-limit", -1);
    // the number of items to display per page
    private static final int perpage = ConfigurationManager.getIntProperty("webui.collectionhome.perpage", 20);
    // the sort option: use "dateaccessioned" if exists
    private static int sort_by = -1;

    static
    {
        if (name == null)
        {
            name = "title";
        }

        try
        {
            for (SortOption option : SortOption.getSortOptions())
            {
                if ("dateaccessioned".equals(option.getName()))
                {
                    sort_by = option.getNumber();
                    break;
                }
            }
        }
        catch (SortException e)
        {
            // does nothing
        }
    }

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
        int offset = UIUtil.getIntParameter(request, "offset");
        if (offset < 0)
        {
            offset = 0;
        }
        
        try
        {
            BrowseIndex bi = BrowseIndex.getBrowseIndex(name);
            if (bi == null || !"item".equals(bi.getDisplayType()))
            {
                request.setAttribute("show.title", Boolean.FALSE);
                return;
            }

            BrowserScope scope = new BrowserScope(context);
            scope.setBrowseContainer(collection);
            scope.setBrowseIndex(bi);
            scope.setEtAl(etal);
            scope.setOffset(offset);
            scope.setResultsPerPage(perpage);
            if (sort_by != -1)
            {
                scope.setSortBy(sort_by);
                scope.setOrder(SortOption.DESCENDING);
            }
            BrowseEngine be = new BrowseEngine(context);
            BrowseInfo binfo = be.browse(scope);
            request.setAttribute("browse.info", binfo);

            if (binfo.hasResults())
            {
                request.setAttribute("show.title", Boolean.TRUE);
            }
            else
            {
                request.setAttribute("show.title", Boolean.FALSE);
            }
        }
        catch (BrowseException e)
        {
            request.setAttribute("show.title", Boolean.FALSE);
        }
    }
}
