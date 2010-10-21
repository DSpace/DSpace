/*
 * CommunityRecentSubmissions.java
 *
 * Version: $Revision: 5497 $
 *
 * Date: $Date: 2010-10-20 23:06:10 +0200 (wo, 20 okt 2010) $
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
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.*;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Renders a list of recently submitted items for the community by using the dspace browse
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class CommunityRecentSubmissions extends AbstractDSpaceTransformer implements CacheableProcessingComponent {

    private static final Logger log = Logger.getLogger(CommunityRecentSubmissions.class);

    private static final Message T_head_recent_submissions =
        message("xmlui.ArtifactBrowser.CommunityViewer.head_recent_submissions");

    /** How many recent submissions to list */
    private static final int RECENT_SUBMISSIONS = 5;

    /** The cache of recently submitted items */
    private java.util.List<BrowseItem> recentSubmittedItems;

    /** Cached validity object */
    private SourceValidity validity;


    @Override
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

    @Override
    public SourceValidity getValidity() {
        if (this.validity == null)
    	{
            Community community = null;
	        try {
	            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

	            if (dso == null)
	                return null;

	            if (!(dso instanceof Community))
	                return null;

	            community = (Community) dso;

	            DSpaceValidity validity = new DSpaceValidity();
	            validity.add(community);

	            // Recently submitted items
	            for (BrowseItem item : getRecentlySubmittedItems(community))
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

    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Community))
            return;

        Community community = (Community) dso;

        Division home = body.addDivision("community-home", "primary repository community");

        java.util.List<BrowseItem> items = getRecentlySubmittedItems(community);
        if(items.size() == 0)
            return;
        
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

    /**
     * Get the recently submitted items for the given community.
     *
     * @param community The community.
     * @return List of recently submitted items
     */
    @SuppressWarnings("unchecked")
    private java.util.List<BrowseItem> getRecentlySubmittedItems(Community community)
            throws SQLException
    {
        if (recentSubmittedItems != null)
            return recentSubmittedItems;

        String source = ConfigurationManager.getProperty("recent.submissions.sort-option");
        int numRecentSubmissions = ConfigurationManager.getIntProperty("recent.submissions.count", RECENT_SUBMISSIONS);
        if(numRecentSubmissions == 0)
            return new ArrayList<BrowseItem>();
        BrowserScope scope = new BrowserScope(context);
        scope.setCommunity(community);
        scope.setResultsPerPage(numRecentSubmissions);

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

    @Override
    public void recycle() {
        this.recentSubmittedItems = null;
        super.recycle();
    }
}
