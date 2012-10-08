/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.xml.sax.SAXException;

/**
 * Navigation that adds code needed for discovery search
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class Navigation extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /** Language Strings */
    private static final Message T_head_all_of_dspace =
        message("xmlui.ArtifactBrowser.Navigation.head_all_of_dspace");

    private static final Message T_head_browse =
        message("xmlui.ArtifactBrowser.Navigation.head_browse");

    private static final Message T_communities_and_collections =
        message("xmlui.ArtifactBrowser.Navigation.communities_and_collections");

    private static final Message T_head_this_collection =
        message("xmlui.ArtifactBrowser.Navigation.head_this_collection");

    private static final Message T_head_this_community =
        message("xmlui.ArtifactBrowser.Navigation.head_this_community");

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        try {
            Request request = ObjectModelHelper.getRequest(objectModel);
            String key = request.getScheme() + request.getServerName() + request.getServerPort() + request.getSitemapURI() + request.getQueryString();
            
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            if (dso != null)
            {
                key += "-" + dso.getHandle();
            }

            return HashUtil.hash(key);
        } 
        catch (SQLException sqle)
        {
            // Ignore all errors and just return that the component is not cachable.
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     * 
     * The cache is always valid.
     */
    public SourceValidity getValidity() {
        return NOPValidity.SHARED_INSTANCE;
    }
    
    /**
     * Add the basic navigational options:
     * 
     * Search - advanced search
     * 
     * browse - browse by Titles - browse by Authors - browse by Dates
     * 
     * language FIXME: add languages
     * 
     * context no context options are added.
     * 
     * action no action options are added.
     */
    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
//        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

//        List test = options.addList("browse");

//        List discovery = options.addList("discovery-search");

//        discovery.setHead("Discovery");
//
//        discovery.addItem().addXref(contextPath + "/discover" , "Discover");

         /*
        List browse = options.addList("browse");

        browse.setHead(T_head_browse);

        List browseGlobal = browse.addList("global");
        List browseContext = browse.addList("context");

        browseGlobal.setHead(T_head_all_of_dspace);


        if (dso != null)
        {
            if (dso instanceof Collection)
            {
                browseContext.addItem().addXref(contextPath + "/discovery/?q=search.resourcetype%3A2+AND+location%3Al" + dso.getID(), T_head_this_collection );
            }
            if (dso instanceof Community)
            {
                browseContext.addItem().addXref(contextPath + "/discovery/?q=search.resourcetype%3A2+AND+location%3Am" + dso.getID(), T_head_this_community );
            }
        }
        browseGlobal.addItem().addXref(contextPath + "/community-list", T_head_all_of_dspace );
    */

        /* regulate the ordering */
        List browse = options.addList("browse");
        options.addList("discovery");
        options.addList("account");
        options.addList("context");
        options.addList("administrative");

        browse.setHead(T_head_browse);

        List browseGlobal = browse.addList("global");
        List browseContext = browse.addList("context");

        browseGlobal.setHead(T_head_all_of_dspace);

        browseGlobal.addItemXref(contextPath + "/community-list",T_communities_and_collections);

        Map<String, String> browseTitleParams = new HashMap<String, String>();
        browseTitleParams.put("sort_by", "dc.title_sort");
        browseTitleParams.put("order", "asc");

        browseGlobal.addItemXref(generateURL(contextPath + "/discover", browseTitleParams),
                message("xmlui.ArtifactBrowser.AdvancedSearch.type_title"));

        // Add the configured browse lists for 'top level' browsing
        addBrowseOptions(browseGlobal, contextPath + "/search-filter", null);

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (dso != null)
        {
            if (dso instanceof Item)
            {
                // If we are an item change the browse scope to the parent
                // collection.
                dso = ((Item) dso).getOwningCollection();
            }

            if (dso instanceof Collection)
            {
                browseContext.setHead(T_head_this_collection);
            }
            if (dso instanceof Community)
            {
                browseContext.setHead(T_head_this_community);
            }

            // Add the configured browse lists for scoped browsing
            String handle = dso.getHandle();
            browseContext.addItemXref(generateURL(contextPath + "/handle/" + handle + "/discover", browseTitleParams),
                    message("xmlui.ArtifactBrowser.AdvancedSearch.type_title"));
            addBrowseOptions(browseContext, contextPath + "/handle/" + handle + "/search-filter", dso);
        }
    }

    /**
     * Add navigation for the configured browse tables to the supplied list.
     *
     * @param browseList
     * @param browseURL
     * @throws WingException
     */
    private void addBrowseOptions(List browseList, String browseURL, DSpaceObject scope) throws WingException
    {
        // Get a Map of all the browse tables
        DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration(scope);
        java.util.List<DiscoverySearchFilterFacet> facets = discoveryConfiguration.getSidebarFacets();

        for (DiscoverySearchFilterFacet facet : facets)
        {
            if(facet.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE))
            {
                //Browse by date isn't support for our facets.
                continue;
            }

            // Create a Map of the query parameters for this link
            Map<String, String> queryParams = new HashMap<String, String>();

            queryParams.put("field", facet.getIndexFieldName());

            // Add a link to this browse
            browseList.addItemXref(generateURL(browseURL, queryParams),
                    message("xmlui.ArtifactBrowser.AdvancedSearch.type_" + facet.getIndexFieldName()));
        }
    }


    /**
     * Ensure that the context path is added to the page meta.
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, SQLException, IOException,
            AuthorizeException
    {

        // Add metadata for quick searches:
        pageMeta.addMetadata("search", "simpleURL").addContent(
                "/discover");
        pageMeta.addMetadata("search", "advancedURL").addContent(
                contextPath + "/discover");
        pageMeta.addMetadata("search", "queryField").addContent("query");
        
    }

}