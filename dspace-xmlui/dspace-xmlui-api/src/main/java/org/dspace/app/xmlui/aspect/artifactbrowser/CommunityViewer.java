/*
 * CommunityViewer.java
 *
 * Version: $Revision: 1.22 $
 *
 * Date: $Date: 2006/08/30 19:16:56 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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
package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.cocoon.DSpaceFeedGenerator;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Reference;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowseItem;
import org.dspace.browse.BrowserScope;
import org.dspace.sort.SortOption;
import org.dspace.sort.SortException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

/**
 * Display a single community. This includes a full text search, browse by list,
 * community display and a list of recent submissions.
 *     private static final Logger log = Logger.getLogger(DSpaceFeedGenerator.class);

 * @author Scott Phillips
 */
public class CommunityViewer extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    private static final Logger log = Logger.getLogger(DSpaceFeedGenerator.class);
	
    /** Language Strings */
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_full_text_search =
        message("xmlui.ArtifactBrowser.CommunityViewer.full_text_search");
    
    private static final Message T_go =
        message("xmlui.general.go");
    
    public static final Message T_untitled = 
    	message("xmlui.general.untitled");

    private static final Message T_head_browse =
        message("xmlui.ArtifactBrowser.CommunityViewer.head_browse");
    
    private static final Message T_browse_titles = 
        message("xmlui.ArtifactBrowser.CommunityViewer.browse_titles");
    
    private static final Message T_browse_authors =
        message("xmlui.ArtifactBrowser.CommunityViewer.browse_authors");
    
    private static final Message T_browse_dates =
        message("xmlui.ArtifactBrowser.CommunityViewer.browse_dates");
    
    private static final Message T_advanced_search_link=
    	message("xmlui.ArtifactBrowser.CommunityViewer.advanced_search_link");
    
    private static final Message T_head_sub_communities = 
        message("xmlui.ArtifactBrowser.CommunityViewer.head_sub_communities");
    
    private static final Message T_head_sub_collections =
        message("xmlui.ArtifactBrowser.CommunityViewer.head_sub_collections");
    
    private static final Message T_head_recent_submissions =
        message("xmlui.ArtifactBrowser.CommunityViewer.head_recent_submissions");
    
    /** How many recient submissions to list */
    private static final int RECENT_SUBMISISONS = 5;

    /** The cache of recently submitted items */
    private java.util.List<BrowseItem> recentSubmittedItems;
    
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
                return "0"; // no item, something is wrong
            
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
	        try {
	            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
	            
	            if (dso == null)
	                return null;
	            
	            if (!(dso instanceof Community))
	                return null;
	            
	            Community community = (Community) dso;
	            
	            DSpaceValidity validity = new DSpaceValidity();
	            validity.add(community);
	            
	            Community[] subCommunities = community.getSubcommunities();
	            Collection[] collections = community.getCollections();
	            // Sub communities
	            for (Community subCommunity : subCommunities)
	            {
	                validity.add(subCommunity);
	            }
	            // Sub collections
	            for (Collection collection : collections)
	            {
	                validity.add(collection);
	            }
	
	            // Recently submitted items
	            for (BrowseItem item : getRecientlySubmittedIems(community))
	            {
	                validity.add(item);
	            }
	            
	            this.validity = validity.complete();
	        } 
	        catch (Exception e)
	        {
	            // Ignore all errors and invalidate the cache.
	        }
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
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Community))
            return;

        // Set up the major variables
        Community community = (Community) dso;
        // Set the page title
        String name = community.getMetadata("name");
        if (name == null || name.length() == 0)
        	pageMeta.addMetadata("title").addContent(T_untitled);
        else
        	pageMeta.addMetadata("title").addContent(name);

        // Add the trail back to the repository root.
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        HandleUtil.buildHandleTrail(community, pageMeta,contextPath);
        
        // Add RSS links if available
        String formats = ConfigurationManager.getProperty("webui.feed.formats");
		if ( formats != null )
		{
			for (String format : formats.split(","))
			{
				// Remove the protocol number, i.e. just list 'rss' or' atom'
				String[] parts = format.split("_");
				if (parts.length < 1) 
					continue;
				
				String feedFormat = parts[0].trim()+"+xml";
					
				String feedURL = contextPath+"/feed/"+format.trim()+"/"+community.getHandle();
				pageMeta.addMetadata("feed", feedFormat).addContent(feedURL);
			}
		}
    }

    /**
     * Display a single community (and refrence any sub communites or
     * collections)
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Community))
            return;

        // Set up the major variables
        Community community = (Community) dso;
        Community[] subCommunities = community.getSubcommunities();
        Collection[] collections = community.getCollections();

        // Build the community viewer division.
        Division home = body.addDivision("community-home", "primary repository community");
        String name = community.getMetadata("name");
        if (name == null || name.length() == 0)
        	home.setHead(T_untitled);
        else
        	home.setHead(name);

        // The search / browse box.
        {
            Division search = home.addDivision("community-search-browse",
                    "secondary search-browse");

            // Search query
            Division query = search.addInteractiveDivision("community-search",
                    contextPath + "/handle/" + community.getHandle() + "/search", 
                    Division.METHOD_POST, "secondary search");
            
            Para para = query.addPara("search-query", null);
            para.addContent(T_full_text_search);
            para.addContent(" ");
            para.addText("query");
            para.addContent(" ");
            para.addButton("submit").setValue(T_go);
            query.addPara().addXref(contextPath + "/handle/" + community.getHandle() + "/advanced-search", T_advanced_search_link);

            // Browse by list
            Division browseDiv = search.addDivision("community-browse","secondary browse");
            List browse = browseDiv.addList("community-browse", List.TYPE_SIMPLE,
                    "community-browse");
            browse.setHead(T_head_browse);
            String url = contextPath + "/handle/" + community.getHandle();
            browse.addItemXref(url + "/browse?type=title",T_browse_titles);
            browse.addItemXref(url + "/browse?type=author",T_browse_authors);
            browse.addItemXref(url + "/browse?type=dateissued",T_browse_dates);
        }

        // Add main reference:
        {
        	Division viewer = home.addDivision("community-view","secondary");
        	
            ReferenceSet referenceSet = viewer.addReferenceSet("community-view",
                    ReferenceSet.TYPE_DETAIL_VIEW);
            Reference communityInclude = referenceSet.addReference(community);

            // If the community has any children communities also refrence them.
            if (subCommunities != null && subCommunities.length > 0)
            {
                ReferenceSet communityReferenceSet = communityInclude
                        .addReferenceSet(ReferenceSet.TYPE_SUMMARY_LIST,null,"hierarchy");

                communityReferenceSet.setHead(T_head_sub_communities);

                // Sub communities
                for (Community subCommunity : subCommunities)
                {
                    communityReferenceSet.addReference(subCommunity);
                }
            }
            if (collections != null && collections.length > 0)
            {
                ReferenceSet communityReferenceSet = communityInclude
                        .addReferenceSet(ReferenceSet.TYPE_SUMMARY_LIST,null,"hierarchy");

                communityReferenceSet.setHead(T_head_sub_collections);
                       

                // Sub collections
                for (Collection collection : collections)
                {
                    communityReferenceSet.addReference(collection);
                }

            }
        }// main refrence

        // Reciently submitted items
        {
            java.util.List<BrowseItem> items = getRecientlySubmittedIems(community);

            Division lastSubmittedDiv = home
                    .addDivision("community-recent-submission","secondary recent-submission");
            lastSubmittedDiv.setHead(T_head_recent_submissions);
            ReferenceSet lastSubmitted = lastSubmittedDiv.addReferenceSet(
                    "collection-last-submitted", ReferenceSet.TYPE_SUMMARY_LIST,
                    null, "recent-submissions");
            for (BrowseItem item : items)
            {
                lastSubmitted.addReference(item);
            }
        }
    }
    
    /**
     * Get the recently submitted items for the given community.
     * 
     * @param community The community.
     */
    @SuppressWarnings("unchecked") 
    private java.util.List<BrowseItem> getRecientlySubmittedIems(Community community)
            throws SQLException
    {
        if (recentSubmittedItems != null)
            return recentSubmittedItems;

        String source = ConfigurationManager.getProperty("recent.submissions.sort-option");
        BrowserScope scope = new BrowserScope(context);
        scope.setCommunity(community);
        scope.setResultsPerPage(RECENT_SUBMISISONS);
        
        // FIXME Exception Handling
        try
        {
        	scope.setBrowseIndex(BrowseIndex.getItemBrowseIndex());
            for (SortOption so : SortOption.getSortOptions())
            {
                if (so.getName().equals(source))
                {
                    scope.setSortBy(so.getNumber());
                	scope.setOrder(SortOption.DESCENDING);
                }
            }

        	BrowseEngine be = new BrowseEngine(context);
        	this.recentSubmittedItems = be.browse(scope).getResults();
        }
        catch (SortException se)
        {
            log.error("Caught SortException", se);
        }
        catch (BrowseException bex)
        {
        	log.error("Caught BrowseException", bex);
        }
        
        return this.recentSubmittedItems;
    }

    /**
     * Recycle
     */
    public void recycle()
    {
        // Clear out our item's cache.
        this.recentSubmittedItems = null;
        this.validity = null;
        super.recycle();
    }
    
    
    
}
