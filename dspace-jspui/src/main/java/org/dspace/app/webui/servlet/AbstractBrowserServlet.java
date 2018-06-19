/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowserScope;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.sort.SortOption;
import org.dspace.sort.SortException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.Utils;
import org.dspace.discovery.configuration.TagCloudConfiguration;

/**
 * Servlet for browsing through indices, as they are defined in
 * the configuration.  This class can take a wide variety of inputs from
 * the user interface:
 *
 * - type:  the type of browse (index name) being performed
 * - order: (ASC | DESC) the direction for result sorting
 * - value: A specific value to find items around.  For example the author name or subject
 * - month: integer specification of the month of a date browse
 * - year: integer specification of the year of a date browse
 * - starts_with: string value at which to start browsing
 * - vfocus: start browsing with a value of this string
 * - focus: integer id of the item at which to start browsing
 * - rpp: integer number of results per page to display
 * - sort_by: integer specification of the field to search on
 * - etal: integer number to limit multiple value items specified in config to
 *
 * @author Richard Jones
 * @version $Revision$
 */
public abstract class AbstractBrowserServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(AbstractBrowserServlet.class);

    public AbstractBrowserServlet()
    {
        super();
    }

    /**
     * Create a BrowserScope from the current request
     *
     * @param context The database context
     * @param request The servlet request
     * @param response The servlet response
     * @return A BrowserScope for the current parameters
     * @throws ServletException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    protected BrowserScope getBrowserScopeForRequest(Context context, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException, AuthorizeException
    {
        try
        {
            // first, lift all the stuff out of the request that we might need
            String type = request.getParameter("type");
            String order = request.getParameter("order");
            String value = request.getParameter("value");
            String valueLang = request.getParameter("value_lang");
            String month = request.getParameter("month");
            String year = request.getParameter("year");
            String startsWith = request.getParameter("starts_with");
            //validate input to avoid cross-site scripting
            try {
            	if (StringUtils.isNotBlank(month) && !"-1".equals(month)) {
            		Integer.valueOf(month);
            	}
            	if (StringUtils.isNotBlank(year) && !"-1".equals(year)) {
            		Integer.valueOf(year);
            	}
            	if(StringUtils.isNotBlank(startsWith)) {
            		startsWith = Utils.addEntities(startsWith);
            	}
            }
            catch(Exception ex) {
                log.warn("We were unable to parse the browse request: maybe a cross-site scripting attach?");
                return null;
            }
            
            
            
            String valueFocus = request.getParameter("vfocus");
            String valueFocusLang = request.getParameter("vfocus_lang");
            String authority = request.getParameter("authority");
            int focus = UIUtil.getIntParameter(request, "focus");
            int offset = UIUtil.getIntParameter(request, "offset");
            int resultsperpage = UIUtil.getIntParameter(request, "rpp");
            int sortBy = UIUtil.getIntParameter(request, "sort_by");
            int etAl = UIUtil.getIntParameter(request, "etal");

            // get the community or collection location for the browse request
            // Note that we are only interested in getting the "smallest" container,
            // so if we find a collection, we don't bother looking up the community
            Collection collection = null;
            Community community = null;
            collection = UIUtil.getCollectionLocation(request);
            if (collection == null)
            {
                community = UIUtil.getCommunityLocation(request);
            }

            // process the input, performing some inline validation
            BrowseIndex bi = null;
            if (StringUtils.isNotEmpty(type))
            {
                bi = BrowseIndex.getBrowseIndex(type);
            }

            // don't override a requested index, if no index is set,
            // try to find it on a possibly specified sort option.
            if (type == null && bi == null)
            {
                if (sortBy > 0)
                {
                    bi = BrowseIndex.getBrowseIndex(SortOption.getSortOption(sortBy));
                }
                else
                {
                    bi = BrowseIndex.getBrowseIndex(SortOption.getDefaultSortOption());
                }
            }

            // If we don't have a sort column
            if (bi != null && sortBy == -1)
            {
                // Get the default one
                SortOption so = bi.getSortOption();
                if (so != null)
                {
                    sortBy = so.getNumber();
                }
            }
            else if (bi != null && bi.isItemIndex() && !bi.isInternalIndex())
            {
                // If a default sort option is specified by the index, but it isn't
                // the same as sort option requested, attempt to find an index that
                // is configured to use that sort by default
                // This is so that we can then highlight the correct option in the navigation
                SortOption bso = bi.getSortOption();
                SortOption so = SortOption.getSortOption(sortBy);
                if ( bso != null && bso.equals(so))
                {
                    BrowseIndex newBi = BrowseIndex.getBrowseIndex(so);
                    if (newBi != null)
                    {
                        bi   = newBi;
                        type = bi.getName();
                    }
                }
            }

            if (order == null && bi != null)
            {
                order = bi.getDefaultOrder();
            }

            // If the offset is invalid, reset to 0
            if (offset < 0)
            {
                offset = 0;
            }

            // if no resultsperpage set, default to 20 - if tag cloud enabled, leave it as is!
            if (bi != null && resultsperpage < 0 && !bi.isTagCloudEnabled())
            {
                resultsperpage = 20;
            }

            // if year and perhaps month have been selected, we translate these into "startsWith"
            // if startsWith has already been defined then it is overwritten
            if (year != null && !"".equals(year) && !"-1".equals(year))
            {
                startsWith = year;
                if ((month != null) && !"-1".equals(month) && !"".equals(month))
                {
                    // subtract 1 from the month, so the match works appropriately
                    if ("ASC".equals(order))
                    {
                        month = Integer.toString((Integer.parseInt(month) - 1));
                    }

                    // They've selected a month as well
                    if (month.length() == 1)
                    {
                        // Ensure double-digit month number
                        month = "0" + month;
                    }

                    startsWith = year + "-" + month;

                    if ("ASC".equals(order))
                    {
                        startsWith = startsWith + "-32";
                    }
                }
            }

            // determine which level of the browse we are at: 0 for top, 1 for second
            int level = 0;
            if (value != null || authority != null)
            {
                level = 1;
            }

            // if sortBy is still not set, set it to 0, which is default to use the primary index value
            if (sortBy == -1)
            {
                sortBy = 0;
            }

            // figure out the setting for author list truncation
            if (etAl == -1)     // there is no limit, or the UI says to use the default
            {
                int limitLine = ConfigurationManager.getIntProperty("webui.browse.author-limit");
                if (limitLine != 0)
                {
                    etAl = limitLine;
                }
            }
            else  // if the user has set a limit
            {
                if (etAl == 0)  // 0 is the user setting for unlimited
                {
                    etAl = -1;  // but -1 is the application setting for unlimited
                }
            }

            // log the request
            String comHandle = "n/a";
            if (community != null)
            {
                comHandle = community.getHandle();
            }
            String colHandle = "n/a";
            if (collection != null)
            {
                colHandle = collection.getHandle();
            }

            String arguments = "type=" + type + ",order=" + order + ",value=" + value +
                ",month=" + month + ",year=" + year + ",starts_with=" + startsWith +
                ",vfocus=" + valueFocus + ",focus=" + focus + ",rpp=" + resultsperpage +
                ",sort_by=" + sortBy + ",community=" + comHandle + ",collection=" + colHandle +
                ",level=" + level + ",etal=" + etAl;

            log.info(LogManager.getHeader(context, "browse", arguments));

            // set up a BrowseScope and start loading the values into it
            BrowserScope scope = new BrowserScope(context);
            scope.setBrowseIndex(bi);
            scope.setOrder(order);
            scope.setFilterValue(value != null?value:authority);
            scope.setFilterValueLang(valueLang);
            scope.setJumpToItem(focus);
            scope.setJumpToValue(valueFocus);
            scope.setJumpToValueLang(valueFocusLang);
            scope.setStartsWith(startsWith);
            scope.setOffset(offset);
            scope.setResultsPerPage(resultsperpage);
            scope.setSortBy(sortBy);
            scope.setBrowseLevel(level);
            scope.setEtAl(etAl);
            scope.setAuthorityValue(authority);

            // assign the scope of either Community or Collection if necessary
            if (community != null)
            {
                scope.setBrowseContainer(community);
            }
            else if (collection != null)
            {
                scope.setBrowseContainer(collection);
            }

            // For second level browses on metadata indexes, we need to adjust the default sorting
            if (bi != null && bi.isMetadataIndex() && scope.isSecondLevel() && scope.getSortBy() <= 0)
            {
                scope.setSortBy(1);
            }

            return scope;
        }
        catch (SortException se)
        {
            log.error("caught exception: ", se);
            throw new ServletException(se);
        }
        catch (BrowseException e)
        {
            log.error("caught exception: ", e);
            throw new ServletException(e);
        }
    }

    /**
     * Do the usual DSpace GET method.  You will notice that browse does not currently
     * respond to POST requests.
     */
    protected void processBrowse(Context context, BrowserScope scope, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        try
        {
            BrowseIndex bi = scope.getBrowseIndex();

            // now start up a browse engine and get it to do the work for us
            BrowseEngine be = new BrowseEngine(context);
            BrowseInfo binfo = be.browse(scope);
            
            request.setAttribute("browse.info", binfo);

            if (authorizeService.isAdmin(context))
            {
                // Set a variable to create admin buttons
                request.setAttribute("admin_button", Boolean.TRUE);
            }

            if (binfo.hasResults())
            {
                if (bi.isMetadataIndex() && !scope.isSecondLevel())
                {
                	if (bi.isTagCloudEnabled()){
                		TagCloudConfiguration tagCloudConfiguration = DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("browseTagCloudConfiguration", TagCloudConfiguration.class);
                		if (tagCloudConfiguration == null){
                			tagCloudConfiguration = new TagCloudConfiguration();
                		}
                		request.setAttribute("tagCloudConfig", tagCloudConfiguration);
                	}
                	
                    showSinglePage(context, request, response);
                }
                else
                {
                    showFullPage(context, request, response);
                }
            }
            else
            {
                showNoResultsPage(context, request, response);
            }
        }
        catch (BrowseException e)
        {
            log.error("caught exception: ", e);
            throw new ServletException(e);
        }
    }

    /**
     * Display the error page
     *
     * @param context
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    protected abstract void showError(Context context, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException,
            AuthorizeException;

    /**
     * Display the No Results page
     *
     * @param context
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    protected abstract void showNoResultsPage(Context context, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException,
            AuthorizeException;

    /**
     * Display the single page.  This is the page which lists just the single values of a
     * metadata browse, not individual items.  Single values are links through to all the items
     * that match that metadata value
     *
     * @param context
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    protected abstract void showSinglePage(Context context, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException,
            AuthorizeException;

    protected abstract void showFullPage(Context context, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException,
            AuthorizeException;
}
