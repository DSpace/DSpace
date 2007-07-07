/*
 * ItemViewer.java
 *
 * Version: $Revision: 1.14 $
 *
 * Date: $Date: 2006/08/08 20:58:30 $
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
import java.util.Map;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.xml.sax.SAXException;

/**
 * Display a single item.
 * 
 * @author Scott Phillips
 */
public class ItemViewer extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /** Language strings */
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail =
        message("xmlui.ArtifactBrowser.ItemViewer.trail");
    
    private static final Message T_show_simple =
        message("xmlui.ArtifactBrowser.ItemViewer.show_simple");
    
    private static final Message T_show_full =
        message("xmlui.ArtifactBrowser.ItemViewer.show_full");
    
    private static final Message T_head_parent_collections =
        message("xmlui.ArtifactBrowser.ItemViewer.head_parent_collections");
    
	/** Cached validity object */
	private SourceValidity validity = null;
	
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        try {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            
            if (dso == null)
                return "0"; // no item, something is wrong.
            
            return HashUtil.hash(dso.getHandle() + "full:" + showFullItem(objectModel));
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
     * The validity object will include the item being viewed, 
     * along with all bundles & bitstreams.
     */
    public SourceValidity getValidity() 
    {
    	if (this.validity == null)
    	{
	        try {
	            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
	            
	            DSpaceValidity validity = new DSpaceValidity();
	            validity.add(dso);
	            this.validity =  validity.complete();
	        } 
	        catch (Exception e)
	        {
	            // Ignore all errors and just invalidate the cache.
	        }
    	}
    	return this.validity;
    }
    
    
    /**
     * Add the item's title and trail links to the page's metadata.
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Item))
            return;
        Item item = (Item) dso;

        // Set the page title
        String title = getItemTitle(item);

        if (title != null)
            pageMeta.addMetadata("title").addContent(title);
        else
            pageMeta.addMetadata("title").addContent(item.getHandle());

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        HandleUtil.buildHandleTrail(item,pageMeta,contextPath);
        pageMeta.addTrail().addContent(T_trail);
    }

    /**
     * Display a single item
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Item))
            return;
        Item item = (Item) dso;

        // Build the item viewer division.
        Division division = body.addDivision("item-view","primary");
        String title = getItemTitle(item);
        if (title != null)
            division.setHead(title);
        else
            division.setHead(item.getHandle());

        Para showfullPara = division.addPara(null, "item-view-toggle item-view-toggle-top");

        if (showFullItem(objectModel))
        {
            String link = contextPath + "/handle/" + item.getHandle();
            showfullPara.addXref(link).addContent(T_show_simple);
        }
        else
        {
            String link = contextPath + "/handle/" + item.getHandle()
                    + "?show=full";
            showfullPara.addXref(link).addContent(T_show_full);
        }
        
        ReferenceSet referenceSet;
        if (showFullItem(objectModel))
        {
            referenceSet = division.addReferenceSet("collection-viewer",
                    ReferenceSet.TYPE_DETAIL_VIEW);
        }
        else
        {
            referenceSet = division.addReferenceSet("collection-viewer",
                    ReferenceSet.TYPE_SUMMARY_VIEW);
        }

        // Refrence the actual Item
        ReferenceSet appearsInclude = referenceSet.addReference(item).addReferenceSet(ReferenceSet.TYPE_DETAIL_LIST,null,"hierarchy");
        appearsInclude.setHead(T_head_parent_collections);
        
        // Reference all collections the item appears in.
        for (Collection collection : item.getCollections())
        {
            appearsInclude.addReference(collection);
        }
        
        showfullPara = division.addPara(null,"item-view-toggle item-view-toggle-bottom");

        if (showFullItem(objectModel))
        {
            String link = contextPath + "/handle/" + item.getHandle();
            showfullPara.addXref(link).addContent(T_show_simple);
        }
        else
        {
            String link = contextPath + "/handle/" + item.getHandle()
                    + "?show=full";
            showfullPara.addXref(link).addContent(T_show_full);
        }
    }
    
    /**
     * Determine if the full item should be referenced or just a summary.
     */
    public static boolean showFullItem(Map objectModel)
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String show = request.getParameter("show");

        if (show != null && show.length() > 0)
            return true;
        return false;
    }

    /**
     * Obtain the item's title.
     */
    public static String getItemTitle(Item item)
    {
        DCValue[] titles = item.getDC("title", Item.ANY, Item.ANY);

        String title;
        if (titles != null && titles.length > 0)
            title = titles[0].value;
        else
            title = null;
        return title;
    }
    
    /**
     * Recycle
     */
    public void recycle() {
    	this.validity = null;
    	super.recycle();
    }
}
