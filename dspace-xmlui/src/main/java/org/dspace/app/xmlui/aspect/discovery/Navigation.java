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

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
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
        options.addList("discovery");
        options.addList("browse");
        options.addList("account");
        options.addList("context");
        options.addList("administrative");

    }

    /**
     * Ensure that the context path is added to the page meta.
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
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