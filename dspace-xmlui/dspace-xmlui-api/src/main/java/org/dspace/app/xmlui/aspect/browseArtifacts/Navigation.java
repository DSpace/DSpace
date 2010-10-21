/*
 * Navigation.java
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
package org.dspace.app.xmlui.aspect.browseArtifacts;

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
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Navigation that adds code needed for the browse features from dspace
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class Navigation extends AbstractDSpaceTransformer implements CacheableProcessingComponent {

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
                key += "-" + dso.getHandle();

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
        /* Create skeleton menu structure to ensure consistent order between aspects,
         * even if they are never used
         */
        List browse = options.addList("browse");
        options.addList("account");
        options.addList("context");
        options.addList("administrative");


        browse.setHead(T_head_browse);

        List browseGlobal = browse.addList("global");
        List browseContext = browse.addList("context");

        browseGlobal.setHead(T_head_all_of_dspace);

        browseGlobal.addItemXref(contextPath + "/community-list",T_communities_and_collections);

        // Add the configured browse lists for 'top level' browsing
        addBrowseOptions(browseGlobal, contextPath + "/browse");

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
            addBrowseOptions(browseContext, contextPath + "/handle/" + handle + "/browse");
        }
    }

    /**
     * Add navigation for the configured browse tables to the supplied list.
     *
     * @param browseList
     * @param browseURL
     * @throws WingException
     */
    private void addBrowseOptions(List browseList, String browseURL) throws WingException
    {
        // FIXME Exception handling
        try
        {
            // Get a Map of all the browse tables
            BrowseIndex[] bis = BrowseIndex.getBrowseIndices();
            for (BrowseIndex bix : bis)
            {
                // Create a Map of the query parameters for this link
                Map<String, String> queryParams = new HashMap<String, String>();

                queryParams.put("type", bix.getName());

                // Add a link to this browse
                browseList.addItemXref(super.generateURL(browseURL, queryParams),
                        message("xmlui.ArtifactBrowser.Navigation.browse_" + bix.getName()));
            }
        }
        catch (BrowseException bex)
        {
            throw new UIException("Unable to get browse indicies", bex);
        }
    }

}
