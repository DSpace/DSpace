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
import java.util.List;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.LogManager;
import org.xml.sax.SAXException;

/**
 * Renders the search box for a community
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class CommunitySearch extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    private static final Logger log = Logger.getLogger(CommunitySearch.class);

    /** Language Strings */
    private static final Message T_full_text_search =
        message("xmlui.ArtifactBrowser.CommunityViewer.full_text_search");

    private static final Message T_go =
        message("xmlui.general.go");

    public static final Message T_untitled =
    	message("xmlui.general.untitled");

    private static final Message T_head_sub_collections =
        message("xmlui.ArtifactBrowser.CommunityViewer.head_sub_collections");

    /** Cached validity object */
    private SourceValidity validity;

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        try {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

            if (dso == null)
            {
                return "0"; // no item, something is wrong
            }

            return HashUtil.hash(dso.getHandle());
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
     * This validity object includes the community being viewed, all
     * sub-communites (one level deep), all sub-collections, and
     * recently submitted items.
     */
    public SourceValidity getValidity()
    {
        if (this.validity == null)
    	{
            Community community = null;
	        try {
	            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

	            if (dso == null)
                {
                    return null;
                }

	            if (!(dso instanceof Community))
                {
                    return null;
                }

	            community = (Community) dso;

	            DSpaceValidity validity = new DSpaceValidity();
	            validity.add(context, community);

	            List<Community> subCommunities = community.getSubcommunities();
	            List<Collection> collections = community.getCollections();
	            // Sub communities
	            for (Community subCommunity : subCommunities)
	            {
	                validity.add(context, subCommunity);
	            }
	            // Sub collections
	            for (Collection collection : collections)
	            {
	                validity.add(context, collection);
	            }

	            this.validity = validity.complete();
	        }
	        catch (Exception e)
	        {
	            // Ignore all errors and invalidate the cache.
	        }

            log.info(LogManager.getHeader(context, "view_community", "community_id=" + (community == null ? "" : community.getID())));
    	}
        return this.validity;
    }


    /**
     * Add the community's title and trail links to the page's metadata
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {

    }

    /**
     * Display a single community (and reference any subcommunites or
     * collections)
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        if (!(dso instanceof Community))
        {
            return;
        }

        // Set up the major variables
        Community community = (Community) dso;

        // Build the community viewer division.
        Division home = body.addDivision("community-home", "primary repository community");
        String name = community.getName();
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
            Division search = home.addDivision("community-search-browse",
                    "secondary search-browse");

            // Search query
            Division query = search.addInteractiveDivision("community-search",
                    contextPath + "/handle/" + community.getHandle() + "/discover",
                    Division.METHOD_POST, "secondary search");

            Para para = query.addPara("search-query", null);
            para.addContent(T_full_text_search);
            para.addContent(" ");
            para.addText("query");
            para.addContent(" ");
            para.addButton("submit").setValue(T_go);
           //query.addPara().addXref(contextPath + "/handle/" + community.getHandle() + "/advanced-search", T_advanced_search_link);

            // Browse by list
       //     Division browseDiv = search.addDivision("community-browse","secondary browse");
         //   List browse = browseDiv.addList("community-browse", List.TYPE_SIMPLE,
        //            "community-browse");
      //      browse.setHead(T_head_browse);
    //        String url = contextPath + "/handle/" + community.getHandle();
  //          browse.addItemXref(url + "/browse?type=title",T_browse_titles);
  //          browse.addItemXref(url + "/browse?type=author",T_browse_authors);
//            browse.addItemXref(url + "/browse?type=dateissued",T_browse_dates);
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
