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
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.SAXException;

/**
 * Renders the search box for a collection
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class CollectionSearch extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /** Language Strings */
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");

    private static final Message T_full_text_search =
        message("xmlui.ArtifactBrowser.CollectionViewer.full_text_search");

    private static final Message T_go =
        message("xmlui.general.go");

    public static final Message T_untitled =
    	message("xmlui.general.untitled");

    /**
     Might implement browse links to activate views into search instead...
    private static final Message T_head_browse =
        message("xmlui.ArtifactBrowser.CollectionViewer.head_browse");

    private static final Message T_browse_titles =
        message("xmlui.ArtifactBrowser.CollectionViewer.browse_titles");

    private static final Message T_browse_authors =
        message("xmlui.ArtifactBrowser.CollectionViewer.browse_authors");

    private static final Message T_browse_dates =
        message("xmlui.ArtifactBrowser.CollectionViewer.browse_dates");

    private static final Message T_advanced_search_link=
    	message("xmlui.ArtifactBrowser.CollectionViewer.advanced_search_link");
    */

    /** Cached validity object */
    private SourceValidity validity;

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey()
    {
        try
        {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

            if (dso == null)
            {
                return "0";
            }

            return HashUtil.hash(dso.getHandle());
        }
        catch (SQLException sqle)
        {
            // Ignore all errors and just return that the component is not
            // cachable.
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     *
     * The validity object will include the collection being viewed and
     * all recently submitted items. This does not include the community / collection
     * hierarchy, when this changes they will not be reflected in the cache.
     */
    public SourceValidity getValidity()
    {
    	if (this.validity == null)
    	{
            Collection collection = null;
	        try
	        {
	            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

	            if (dso == null)
                {
                    return null;
                }

	            if (!(dso instanceof Collection))
                {
                    return null;
                }

	            collection = (Collection) dso;

	            DSpaceValidity validity = new DSpaceValidity();

	            // Add the actual collection;
	            validity.add(context, collection);

	            this.validity = validity.complete();
	        }
	        catch (Exception e)
	        {
	            // Just ignore all errors and return an invalid cache.
	        }
            
    	}
    	return this.validity;
    }


    /**
     * Add a page title and trail links.
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Collection))
        {
            return;
        }

        Collection collection = (Collection) dso;

        // Set the page title
        String name = collection.getName();
        if (name == null || name.length() == 0)
        {
            pageMeta.addMetadata("title").addContent(T_untitled);
        }
        else
        {
            pageMeta.addMetadata("title").addContent(name);
        }

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        HandleUtil.buildHandleTrail(context, collection,pageMeta,contextPath);

        // Add RSS links if available
        String[] formats = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("webui.feed.formats");
		if ( formats != null )
		{
			for (String format : formats)
			{
				// Remove the protocol number, i.e. just list 'rss' or' atom'
				String[] parts = format.split("_");
				if (parts.length < 1)
                {
                    continue;
                }

				String feedFormat = parts[0].trim()+"+xml";

				String feedURL = contextPath+"/feed/"+format.trim()+"/"+collection.getHandle();
				pageMeta.addMetadata("feed", feedFormat).addContent(feedURL);
			}
		}
    }

    /**
     * Display a single collection
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Collection))
        {
            return;
        }

        // Set up the major variables
        Collection collection = (Collection) dso;

        // Build the collection viewer division.
        Division home = body.addDivision("collection-home", "primary repository collection");
        String name = collection.getName();
        if (name == null || name.length() == 0)
        {
            home.setHead(T_untitled);
        }
        else
        {
            home.setHead(name);
        }

        // The search / browse box.
        {
            Division search = home.addDivision("collection-search-browse",
                    "secondary search-browse");

            // Search query
            Division query = search.addInteractiveDivision("collection-search",
                    contextPath + "/handle/" + collection.getHandle() + "/discover",
                    Division.METHOD_POST, "secondary search");

            Para para = query.addPara("search-query", null);
            para.addContent(T_full_text_search);
            para.addContent(" ");
            para.addText("query");
            para.addContent(" ");
            para.addButton("submit").setValue(T_go);
            //query.addPara().addXref(contextPath + "/handle/" + collection.getHandle()+ "/advanced-search", T_advanced_search_link);

            // Browse by list
            //Division browseDiv = search.addDivision("collection-browse","secondary browse");
            //List browse = browseDiv.addList("collection-browse", List.TYPE_SIMPLE,
            //        "collection-browse");
            //browse.setHead(T_head_browse);
            //String url = contextPath + "/handle/" + collection.getHandle();
            //browse.addItemXref(url + "/browse?type=title",T_browse_titles);
            //browse.addItemXref(url + "/browse?type=author",T_browse_authors);
            //browse.addItemXref(url + "/browse?type=dateissued",T_browse_dates);
        }

    }


    /**
     * Recycle
     */
    public void recycle()
    {
        // Clear out our item's cache.
        this.validity = null;
        super.recycle();
    }
}