/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.GoogleMetadata;
import org.dspace.app.webui.util.Authenticate;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.SubscribeService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.identifier.DOI;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.DOIService;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.plugin.CollectionHomeProcessor;
import org.dspace.plugin.CommunityHomeProcessor;
import org.dspace.plugin.ItemHomeProcessor;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.usage.UsageEvent;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.output.XMLOutputter;

/**
 * Servlet for handling requests within a community or collection. The Handle is
 * extracted from the URL, e.g: <code>/community/1721.1/1234</code>. If there
 * is anything after the Handle, the request is forwarded to the appropriate
 * servlet. For example:
 * <P>
 * <code>/community/1721.1/1234/simple-search</code>
 * <P>
 * would be forwarded to <code>/simple-search</code>. If there is nothing
 * after the Handle, the community or collection home page is shown.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
@SuppressWarnings("deprecation")
public class HandleServlet extends DSpaceServlet
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(HandleServlet.class);

    // services API
    private final transient HandleService handleService
             = HandleServiceFactory.getInstance().getHandleService();

    private final transient SubscribeService subscribeService
             = EPersonServiceFactory.getInstance().getSubscribeService();
    
    private final transient ItemService itemService
             = ContentServiceFactory.getInstance().getItemService();

    private final transient CommunityService communityService
             = ContentServiceFactory.getInstance().getCommunityService();
    
    private final transient CollectionService collectionService
             = ContentServiceFactory.getInstance().getCollectionService();
    
    private final transient PluginService pluginService
             = CoreServiceFactory.getInstance().getPluginService();
    
    /** For obtaining &lt;meta&gt; elements to put in the &lt;head&gt; */
    private final transient DisseminationCrosswalk xHTMLHeadCrosswalk
             = (DisseminationCrosswalk) pluginService
                .getNamedPlugin(DisseminationCrosswalk.class, "XHTML_HEAD_ITEM");

    private final transient IdentifierService identifierService
            = IdentifierServiceFactory.getInstance().getIdentifierService();
    
    private final transient DOIService doiService
            = IdentifierServiceFactory.getInstance().getDOIService();
    
    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String handle = null;
        String extraPathInfo = null;
        DSpaceObject dso = null;

        // Original path info, of the form "1721.x/1234"
        // or "1721.x/1234/extra/stuff"
        String path = request.getPathInfo();

        if (path != null)
        {
            // substring(1) is to remove initial '/'
            path = path.substring(1);

            try
            {
                // Extract the Handle
                int firstSlash = path.indexOf('/');
                int secondSlash = path.indexOf('/', firstSlash + 1);

                if (secondSlash != -1)
                {
                    // We have extra path info
                    handle = path.substring(0, secondSlash);
                    extraPathInfo = path.substring(secondSlash);
                }
                else
                {
                    // The path is just the Handle
                    handle = path;
                }
            }
            catch (NumberFormatException nfe)
            {
                // Leave handle as null
            }
        }

        
        
        // Find out what the handle relates to
        if (handle != null)
        {
            dso = handleService.resolveToObject(context, handle);
        }

        if (dso == null)
        {
            log.info(LogManager.getHeader(context, "invalid_id", "path=" + path));
            JSPManager.showInvalidIDError(request, response, StringEscapeUtils.escapeHtml(path), -1);

            return;
        }

        if("/statistics".equals(extraPathInfo))
        {
            // Check configuration properties, auth, etc.
            // Inject handle attribute
            log.info(LogManager.getHeader(context, "display_statistics", "handle=" + handle + ", path=" + extraPathInfo));
            request.setAttribute("handle", handle);

            // Forward to DisplayStatisticsServlet without changing path.
            RequestDispatcher dispatch = getServletContext().getNamedDispatcher("displaystats");
            dispatch.forward(request, response);

            // If we don't return here, we keep processing and end up
            // throwing a NPE when checking community authorization
            // and firing a usage event for the DSO we're reporting for
            return;

        } else if ("/display-statistics.jsp".equals(extraPathInfo))
        {
            request.getRequestDispatcher(extraPathInfo).forward(request, response);
            // If we don't return here, we keep processing and end up
            // throwing a NPE when checking community authorization
            // and firing a usage event for the DSO we're reporting for
            return;
        } else if ("/browse".equals((extraPathInfo)) || StringUtils.startsWith(extraPathInfo, "/browse?")) {
        	// Add the location if we got a community or collection
        	if (dso instanceof Community)
        	{
        		Community c = (Community) dso;
        		request.setAttribute("dspace.community", c);
        	} else if (dso instanceof Collection)
        	{
        		Collection c = (Collection) dso;
        		request.setAttribute("dspace.collection", c);
        	}
            request.getRequestDispatcher(extraPathInfo).forward(request, response);
            // If we don't return here, we keep processing and end up
            // throwing a NPE when checking community authorization
            // and firing a usage event for the DSO we're reporting for
            return;
        } else if ("/simple-search".equals(extraPathInfo) || StringUtils.startsWith(extraPathInfo, "simple-search?")) {
        	// Add the location if we got a community or collection
        	if (dso instanceof Community)
        	{
        		Community c = (Community) dso;
        		request.setAttribute("dspace.community", c);
        	} else if (dso instanceof Collection)
        	{
        		Collection c = (Collection) dso;
        		request.setAttribute("dspace.collection", c);
        	}
            request.getRequestDispatcher(extraPathInfo).forward(request, response);
            // If we don't return here, we keep processing and end up
            // throwing a NPE when checking community authorization
            // and firing a usage event for the DSO we're reporting for
            return;
        }


        // OK, we have a valid Handle. What is it?
        if (dso.getType() == Constants.ITEM)
        {
         // do we actually want to display the item, or forward to another page?
            if ((extraPathInfo == null) || (extraPathInfo.equals("/")))
            {
                Item item = (Item) dso;

                // Only use last-modified if this is an anonymous access
                // - caching content that may be generated under authorisation
                //   is a security problem

                if (context.getCurrentUser() == null)
                {
                    response.setDateHeader("Last-Modified", item
                            .getLastModified().getTime());

                    // Check for if-modified-since header

                    long modSince = request.getDateHeader("If-Modified-Since");

                    if (modSince != -1 && item.getLastModified().getTime() < modSince)
                    {
                        // Item has not been modified since requested date,
                        // hence bitstream has not been, either; return 304
                        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    }
                    else
                    {
                        // Display the item page
                        displayItem(context, request, response, item, handle);
                    }
                }
                else
                {
                    // Display the item page
                    displayItem(context, request, response, item, handle);
                }
            }
            else
            {
                log.debug("Found Item with extraPathInfo => Error.");
                JSPManager.showInvalidIDError(request, response, StringEscapeUtils.escapeHtml(path), -1);
                return;
            }

        }
        else if (dso.getType() == Constants.COLLECTION)
        {
            Collection c = (Collection) dso;

            // Store collection location in request
            request.setAttribute("dspace.collection", c);

            /*
             * Find the "parent" community the collection, mainly for
             * "breadcrumbs" FIXME: At the moment, just grab the first community
             * the collection is in. This should probably be more context
             * sensitive when we have multiple inclusion.
             */
            List<Community> parents = c.getCommunities();
            request.setAttribute("dspace.community", parents.get(0));

            /*
             * Find all the "parent" communities for the collection for
             * "breadcrumbs"
             */
            request.setAttribute("dspace.communities", getParents(context, parents.get(0),
                    true));

            // home page, or forward to another page?
            if ((extraPathInfo == null) || (extraPathInfo.equals("/")))
            {
                collectionHome(context, request, response, parents.get(0), c);
            }
            else
            {
                log.debug("Found Collection with extraPathInfo => Error.");
                JSPManager.showInvalidIDError(request, response, StringEscapeUtils.escapeHtml(path), -1);
                return;
            }
        }
        else if (dso.getType() == Constants.COMMUNITY)
        {
            Community c = (Community) dso;

            // Store collection location in request
            request.setAttribute("dspace.community", c);

            /*
             * Find all the "parent" communities for the community
             */
            request.setAttribute("dspace.communities", getParents(context, c, false));

            // home page, or forward to another page?
            if ((extraPathInfo == null) || (extraPathInfo.equals("/")))
            {
                communityHome(context, request, response, c);
            }
            else
            {
                log.debug("Found Community with extraPathInfo => Error.");
                JSPManager.showInvalidIDError(request, response, StringEscapeUtils.escapeHtml(path), -1);
                return;
            }
        }
        else
        {
            // Shouldn't happen. Log and treat as invalid ID
            log.info(LogManager.getHeader(context,
                    "Handle not an item, collection or community", "handle="
                            + handle));
            JSPManager.showInvalidIDError(request, response, StringEscapeUtils.escapeHtml(path), -1);

            return;
        }
    }

    /**
     * Show an item page
     * 
     * @param context
     *            Context object
     * @param request
     *            the HTTP request
     * @param response
     *            the HTTP response
     * @param item
     *            the item
     * @param handle
     *            the item's handle
     */
    private void displayItem(Context context, HttpServletRequest request,
            HttpServletResponse response, Item item, String handle)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        // perform any necessary pre-processing
        preProcessItemHome(context, request, response, item);
        
        // Tombstone?
        if (item.isWithdrawn())
        {
            JSPManager.showJSP(request, response, "/tombstone.jsp");

            return;
        }

        // Ensure the user has authorisation
        authorizeService.authorizeAction(context, item, Constants.READ);

        log
                .info(LogManager.getHeader(context, "view_item", "handle="
                        + handle));
        
        // show edit link
        if (itemService.canEdit(context, item))
        {
            // set a variable to create an edit button
            request.setAttribute("admin_button", Boolean.TRUE);
        } 
        // show submitters a button to create a new item version
        else if (itemService.canCreateNewVersion(context, item))
        {
            // set a variable to create a button to create a new item version
            request.setAttribute("submitter_button", Boolean.TRUE);
        }
  		  
        // Get the collections
        List<Collection> collections = item.getCollections();

        // For the breadcrumbs, get the first collection and the first community
        // that is in. FIXME: Not multiple-inclusion friendly--should be
        // smarter, context-sensitive
        request.setAttribute("dspace.collection", item.getOwningCollection());

        List<Community> comms = item.getOwningCollection().getCommunities();
        request.setAttribute("dspace.community", comms.get(0));

        /*
         * Find all the "parent" communities for the collection
         */
        request.setAttribute("dspace.communities", getParents(context, comms.get(0), true));

        // Full or simple display?
        boolean displayAll = false;
        String modeParam = request.getParameter("mode");

        if ((modeParam != null) && modeParam.equalsIgnoreCase("full"))
        {
            displayAll = true;
        }

        String headMetadata = "";

        // Produce <meta> elements for header from crosswalk
        try
        {
            List<Element> l = xHTMLHeadCrosswalk.disseminateList(context, item);
            StringWriter sw = new StringWriter();

            XMLOutputter xmlo = new XMLOutputter();
            xmlo.output(new Text("\n"), sw);
            for (Element e : l)
            {
                // FIXME: we unset the Namespace so it's not printed.
                // This is fairly yucky, but means the same crosswalk should
                // work for Manakin as well as the JSP-based UI.
                e.setNamespace(null);
                xmlo.output(e, sw);
                xmlo.output(new Text("\n"), sw);
            }
            boolean googleEnabled = ConfigurationManager.getBooleanProperty("google-metadata.enable", false);
            if (googleEnabled)
            {
                // Add Google metadata field names & values
                GoogleMetadata gmd = new GoogleMetadata(context, item);
                xmlo.output(new Text("\n"), sw);

                for (Element e: gmd.disseminateList())
                {
                    xmlo.output(e, sw);
                    xmlo.output(new Text("\n"), sw);
                }
            }
            headMetadata = sw.toString();
        }
        catch (CrosswalkException ce)
        {
            throw new ServletException(ce);
        }
        
        // lookup if we have a DOI
        String doi = null;
        if (identifierService != null)
        {
            doi = identifierService.lookup(UIUtil.obtainContext(request), item, DOI.class);
        }
        if (doi != null)
        {
            try
            {
                doi = doiService.DOIToExternalForm(doi);
            }
            catch (Exception ex)
            {
                doi = null;
                log.error("Unable to convert DOI '" + doi + "' into external form.", ex);
            }
        }
        
        // use handle as preferred Identifier if not configured otherwise.    
        String preferredIdentifier = null;
        if (item.getHandle() != null) {
            preferredIdentifier = handleService.getCanonicalForm(item.getHandle());
        }
        if ("doi".equalsIgnoreCase(ConfigurationManager.getProperty("webui.preferred.identifier")))
        {
            if (doi != null)
            {
                preferredIdentifier = doi;
            }
        }

        // Enable suggest link or not
        boolean suggestEnable = false;
        if (!ConfigurationManager.getBooleanProperty("webui.suggest.enable"))
        {
            // do nothing, the suggestLink is already set to false 
        }
        else
        {
            // it is in general enabled
            suggestEnable= true;
            
            // check for the enable only for logged in users option
            if(!ConfigurationManager.getBooleanProperty("webui.suggest.loggedinusers.only"))
            {
                // do nothing, the suggestLink stays as it is
            }
            else
            {
                // check whether there is a logged in user
                suggestEnable = (context.getCurrentUser() != null);
            }
        }

        // Fire usage event.
        DSpaceServicesFactory.getInstance().getEventService().fireEvent(
            		new UsageEvent(
            				UsageEvent.Action.VIEW,
            				request,
            				context,
            				item));

        // Set attributes and display
        request.setAttribute("suggest.enable", suggestEnable);
        request.setAttribute("display.all", displayAll);
        request.setAttribute("item", item);
        request.setAttribute("collections", collections);
        request.setAttribute("dspace.layout.head", headMetadata);
        request.setAttribute("doi", doi);
        request.setAttribute("preferred_identifier", preferredIdentifier);
        JSPManager.showJSP(request, response, "/display-item.jsp");
    }
    
    private void preProcessItemHome(Context context, HttpServletRequest request,
            HttpServletResponse response, Item item)
        throws ServletException, IOException, SQLException
    {
        try
        {
            ItemHomeProcessor[] chp = (ItemHomeProcessor[]) pluginService.getPluginSequence(ItemHomeProcessor.class);
            for (int i = 0; i < chp.length; i++)
            {
                chp[i].process(context, request, response, item);
            }
        }
        catch (Exception e)
        {
            log.error("caught exception: ", e);
            throw new ServletException(e);
        }
    }

    /**
     * Show a community home page, or deal with button press on home page
     * 
     * @param context
     *            Context object
     * @param request
     *            the HTTP request
     * @param response
     *            the HTTP response
     * @param community
     *            the community
     */
    private void communityHome(Context context, HttpServletRequest request,
            HttpServletResponse response, Community community)
            throws ServletException, IOException, SQLException
    {
        // Handle click on a browse or search button
        if (!handleButton(request, response, community.getHandle()))
        {
            // No button pressed, display community home page
            log.info(LogManager.getHeader(context, "view_community",
                    "community_id=" + community.getID()));

            // Get the collections within the community
            List<Collection> collections = community.getCollections();

            // get any subcommunities of the community
            List<Community> subcommunities = community.getSubcommunities();

            // perform any necessary pre-processing
            preProcessCommunityHome(context, request, response, community);

            // is the user a COMMUNITY_EDITOR?
            if (communityService.canEditBoolean(context, community))
            {
                // set a variable to create an edit button
                request.setAttribute("editor_button", Boolean.TRUE);
            }

            // can they add to this community?
            if (authorizeService.authorizeActionBoolean(context, community,
                    Constants.ADD))
            {
                // set a variable to create an edit button
                request.setAttribute("add_button", Boolean.TRUE);
            }

            // can they remove from this community?
            if (authorizeService.authorizeActionBoolean(context, community,
                    Constants.REMOVE))
            {
                // set a variable to create an edit button
                request.setAttribute("remove_button", Boolean.TRUE);
            }

            // Fire usage event.
            DSpaceServicesFactory.getInstance().getEventService().fireEvent(
            		new UsageEvent(
            				UsageEvent.Action.VIEW,
            				request,
            				context,
            				community));

            // Forward to community home page
            request.setAttribute("community", community);
            request.setAttribute("collections", collections);
            request.setAttribute("subcommunities", subcommunities);
            JSPManager.showJSP(request, response, "/community-home.jsp");
        }
    }

    private void preProcessCommunityHome(Context context, HttpServletRequest request,
            HttpServletResponse response, Community community)
    	throws ServletException, IOException, SQLException
    {
    	try
    	{
    		CommunityHomeProcessor[] chp = (CommunityHomeProcessor[]) pluginService.getPluginSequence(CommunityHomeProcessor.class);
    		for (int i = 0; i < chp.length; i++)
    		{
    			chp[i].process(context, request, response, community);
    		}
    	}
    	catch (Exception e)
    	{
    		log.error("caught exception: ", e);
    		throw new ServletException(e);
    	}
    }
    
    
    /**
     * Show a collection home page, or deal with button press on home page
     * 
     * @param context
     *            Context object
     * @param request
     *            the HTTP request
     * @param response
     *            the HTTP response
     * @param community
     *            the community
     * @param collection
     *            the collection
     */
    private void collectionHome(Context context, HttpServletRequest request,
            HttpServletResponse response, Community community,
            Collection collection) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // Handle click on a browse or search button
        if (!handleButton(request, response, collection.getHandle()))
        {
            // Will need to know whether to commit to DB
            boolean updated = false;

            // No search or browse button pressed, check for
            if (request.getParameter("submit_subscribe") != null)
            {
                // Subscribe button pressed.
                // Only registered can subscribe, so redirect unless logged in.
                if (context.getCurrentUser() == null &&
                    !Authenticate
                            .startAuthentication(context, request, response))

                {
                    return;
                }
                else
                {
                    subscribeService.subscribe(context, context.getCurrentUser(),
                            collection);
                    updated = true;
                }
            }
            else if (request.getParameter("submit_unsubscribe") != null)
            {
            	subscribeService.unsubscribe(context, context.getCurrentUser(),
                        collection);
                updated = true;
            }

            // display collection home page
            log.info(LogManager.getHeader(context, "view_collection",
                    "collection_id=" + collection.getID()));

            // perform any necessary pre-processing
            preProcessCollectionHome(context, request, response, collection);
            
            // Is the user logged in/subscribed?
            EPerson e = context.getCurrentUser();
            boolean subscribed = false;

            if (e != null)
            {
            	subscribeService.isSubscribed(context, e, collection);

                // is the user a COLLECTION_EDITOR?
                if (collectionService.canEditBoolean(context, collection, true))
                {
                    // set a variable to create an edit button
                    request.setAttribute("editor_button", Boolean.TRUE);
                }

                // can they admin this collection?
                if (authorizeService.authorizeActionBoolean(context,
                        collection, Constants.COLLECTION_ADMIN))
                {
                    request.setAttribute("admin_button", Boolean.TRUE);

                    // give them a button to manage submitter list
                    // what group is the submitter?
                    Group group = collection.getSubmitters();

                    if (group != null)
                    {
                        request.setAttribute("submitters", group);
                    }
                }

                // can they submit to this collection?
                if (authorizeService.authorizeActionBoolean(context,
                        collection, Constants.ADD))
                {
                    request
                            .setAttribute("can_submit_button",
                                    Boolean.TRUE);

                }
                else
                {
                    request.setAttribute("can_submit_button",
                            Boolean.FALSE);
                }
            }

            // Fire usage event.
            DSpaceServicesFactory.getInstance().getEventService().fireEvent(
            		new UsageEvent(
            				UsageEvent.Action.VIEW,
            				request,
            				context,
            				collection));

            // Forward to collection home page
            request.setAttribute("collection", collection);
            request.setAttribute("community", community);
            request.setAttribute("logged.in", e != null);
            request.setAttribute("subscribed", subscribed);
            JSPManager.showJSP(request, response, "/collection-home.jsp");

            if (updated)
            {
                context.complete();
            }
        }
    }

    private void preProcessCollectionHome(Context context, HttpServletRequest request,
            HttpServletResponse response, Collection collection)
    	throws ServletException, IOException, SQLException
    {
    	try
    	{
    		CollectionHomeProcessor[] chp = (CollectionHomeProcessor[]) pluginService.getPluginSequence(CollectionHomeProcessor.class);
    		for (int i = 0; i < chp.length; i++)
    		{
    			chp[i].process(context, request, response, collection);
    		}
    	}
    	catch (Exception e)
    	{
    		log.error("caught exception: ", e);
    		throw new ServletException(e);
    	}
    }
    
    /**
     * Check to see if a browse or search button has been pressed on a community
     * or collection home page. If so, redirect to the appropriate URL.
     * 
     * @param request
     *            HTTP request
     * @param response
     *            HTTP response
     * @param handle
     *            Handle of the community/collection home page
     * 
     * @return true if a browse/search button was pressed and the user was
     *         redirected
     */
    private boolean handleButton(HttpServletRequest request,
            HttpServletResponse response, String handle) throws IOException
    {
        String button = UIUtil.getSubmitButton(request, "");
        String location = request.getParameter("location");
        String prefix = "/";
        String url = null;

        if (location == null)
        {
            return false;
        }

        /*
         * Work out the "prefix" to which to redirect If "/", scope is all of
         * DSpace, so prefix is "/" If prefix is a handle, scope is a community
         * or collection, so "/handle/1721.x/xxxx/" is the prefix.
         */
        if (!location.equals("/"))
        {
            prefix = "/handle/" + location + "/";
        }

        if (button.equals("submit_search")
                || (request.getParameter("query") != null))
        {
            /*
             * Have to check for search button and query - in some browsers,
             * typing a query into the box and hitting return doesn't produce a
             * submit button parameter. Redirect to appropriate search page
             */
            url = request.getContextPath()
                    + prefix
                    + "simple-search?query="
                    + URLEncoder.encode(request.getParameter("query"),
                            Constants.DEFAULT_ENCODING);
        }

        // If a button was pressed, redirect to appropriate page
        if (url != null)
        {
            response.sendRedirect(response.encodeRedirectURL(url));

            return true;
        }

        return false;
    }

    /**
     * Utility method to produce a list of parent communities for a given
     * community, ending with the passed community, if include is true. If
     * commmunity is top-level, the array will be empty, or contain only the
     * passed community, if include is true. The array is ordered highest level
     * to lowest
     */
    private List<Community> getParents(Context context, Community c, boolean include)
            throws SQLException
    {
        // Find all the "parent" communities for the community
        List<Community> parents = communityService.getAllParents(context, c);
        parents = Lists.reverse(parents);
        if (include)
        {
            parents.add(c);
        }
        return parents;
    }
}
