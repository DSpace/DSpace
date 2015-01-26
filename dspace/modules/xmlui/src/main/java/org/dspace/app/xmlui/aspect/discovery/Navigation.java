/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery;

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
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

/**
 * Navigation that adds code needed for discovery search
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 *
 * This class has been adjusted to leave out the community browse option
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
    private static final Message T_navigation_workflow_overview =
            message("xmlui.Discovery.Navigation.workflow-overview");


    private static final Message T_administrative_head = 
            message("xmlui.administrative.Navigation.administrative_head");

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

//        List browse = options.addList("browse");
//        browse.setHead(T_head_browse);
//        List browseGlobal = browse.addList("global");
//        List browseContext = browse.addList("context");

//        browseGlobal.setHead(T_head_all_of_dspace);
//        browseGlobal.addItem().addXref(contextPath + "/community-list", T_head_all_of_dspace );

        // Add the configured browse lists for 'top level' browsing
//        addBrowseOptions(browseGlobal, contextPath + "/browse-discovery");

        /*
        if (dso != null)
        {
            if (dso instanceof Item)
            {
                // If we are an item change the browse scope to the parent
                // collection.
                dso = ((Item) dso).getOwningCollection();
                //Can only happen if we have a workspace/workflowitem
                if(dso == null)
                    return;
            }

            if (dso instanceof Collection)
            {
                browseContext.setHead(T_head_this_collection);
            }
            if (dso instanceof Community)
            {
                browseContext.setHead(T_head_this_community);
            }

        }
        */

        if(AuthorizeManager.isCuratorOrAdmin(context)){
            List admin = options.addList("administrative");
            admin.setHead(T_administrative_head);

            admin.addItem().addXref(contextPath + "/workflow-overview").addContent(T_navigation_workflow_overview);
        }
    }

    /**
     * Insure that the context path is added to the page meta.
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        // FIXME: I don't think these should be set here, but there needed and I'm
        // not sure where else it could go. Perhaps the linkResolver?
    	Request request = ObjectModelHelper.getRequest(objectModel);
        pageMeta.addMetadata("contextPath").addContent(contextPath);
        pageMeta.addMetadata("request","queryString").addContent(request.getQueryString());
        pageMeta.addMetadata("request","scheme").addContent(request.getScheme());
        pageMeta.addMetadata("request","serverPort").addContent(request.getServerPort());
        pageMeta.addMetadata("request","serverName").addContent(request.getServerName());
        pageMeta.addMetadata("request","URI").addContent(request.getSitemapURI());


        String analyticsKey = ConfigurationManager.getProperty("xmlui.google.analytics.key");
        if (analyticsKey != null && analyticsKey.length() > 0)
        {
        	analyticsKey = analyticsKey.trim();
        	pageMeta.addMetadata("google","analytics").addContent(analyticsKey);
        }

        // Add metadata for quick searches:
        pageMeta.addMetadata("search", "simpleURL").addContent(
                contextPath + "/discover");
        pageMeta.addMetadata("search", "advancedURL").addContent(
                contextPath + "/discover");
        pageMeta.addMetadata("search", "queryField").addContent("query");

        pageMeta.addMetadata("page","contactURL").addContent(contextPath + "/contact");
        pageMeta.addMetadata("page","feedbackURL").addContent(contextPath + "/feedback");

        // Get realPort and nodeName
        String port = ConfigurationManager.getProperty("dspace.port");
        String nodeName = ConfigurationManager.getProperty("dryad.home");

        // If we're using Apache, we may not have the real port in serverPort
        if (port != null) {
            pageMeta.addMetadata("request", "realServerPort").addContent(port);
        }
        else {
            pageMeta.addMetadata("request", "realServerPort").addContent("80");
        }
        if (nodeName != null) {
            pageMeta.addMetadata("dryad", "node").addContent(nodeName);
        }

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (dso != null)
        {
            if (dso instanceof Item)
            {
                pageMeta.addMetadata("focus","object").addContent("hdl:"+dso.getHandle());
                this.getObjectManager().manageObject(dso);
                dso = ((Item) dso).getOwningCollection();
            }
        }

    }
}