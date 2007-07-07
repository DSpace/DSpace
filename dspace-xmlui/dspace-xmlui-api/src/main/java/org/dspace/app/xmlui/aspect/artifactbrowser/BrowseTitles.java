/*
 * BrowseTitles.java
 *
 * Version: $Revision: 1.15 $
 *
 * Date: $Date: 2006/07/27 18:24:34 $
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
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.xml.sax.SAXException;

/**
 * Display the results of browsing title index. This component may either apply
 * to all communities and collection or be scoped to just one community /
 * collection depending upon the url.
 * 
 * @author Scott Phillips
 */
public class BrowseTitles extends AbstractBrowse implements CacheableProcessingComponent
{	
    /** Language strings */
    private final static Message T_title =
        message("xmlui.ArtifactBrowser.BrowseTitles.title");
    
    private final static Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private final static Message T_trail =
        message("xmlui.ArtifactBrowser.BrowseTitles.trail");
    
    private final static Message T_head =
        message("xmlui.ArtifactBrowser.BrowseTitles.head");
    
    private final static Message T_starts_with = 
        message("xmlui.ArtifactBrowser.BrowseTitles.starts_with");
    
    private final static Message T_starts_with_help =
        message("xmlui.ArtifactBrowser.BrowseTitles.starts_with_help");
    
    private final static Message T_go =
        message("xmlui.general.go");
    
    
    
    
    
	/** Cached validity object */
	private SourceValidity validity;
	
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey()
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        
        String key = "";
        
        key += "-" + request.getParameter("top");
        key += "-" + request.getParameter("bottom");
        key += "-" + request.getParameter("startsWith");
        key += "-" + request.getParameter("month");
        key += "-" + request.getParameter("year");
        key += "-" + request.getParameter("author");
        
        try
        {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            if (dso != null)
                key += "-" + dso.getHandle();

            return HashUtil.hash(key);
        }
        catch (Exception e)
        {
            // Ignore all errors and just don't cache.
            return "0";
        }
    }


    /**
     * Generate the cache validity object.
     * 
     * The validity object will include all items on this browse page, along 
     * with all bundles and bitstreams refrenced by them. The one change that 
     * will not be refelected  in this cache is a change is community/collection 
     * hierarchy. 
     */
    public SourceValidity getValidity()
    {
    	if (this.validity == null)
    	{
	        try
	        {
	            DSpaceValidity validity = new DSpaceValidity();
	            
	            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
	            validity.add(dso);
	            
	            performBrowse(MODE_BY_TITLE);
	            
	            for (Item item : browseInfo.getItemResults())
	            {
	                validity.add(item);
	            } 
	            
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
     * Add Page metadata.
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        pageMeta.addMetadata("title").addContent(T_title);

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        if (dso != null)
            HandleUtil.buildHandleTrail(dso, pageMeta,contextPath);
        pageMeta.addTrail().addContent(T_trail);
    }

    /**
     * Add the browse-title division.
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
    	performBrowse(MODE_BY_TITLE);
    	
        // Pagination variables
        int itemsTotal = browseInfo.getTotal();
        int firstItemIndex = browseInfo.getOverallPosition() + 1;
        int lastItemIndex = browseInfo.getOverallPosition()
                + browseInfo.getResultCount();
        String previousPage = previousPageURL("browse-title");
        String nextPage = nextPageURL("browse-title");

        // Build the DRI Body
        Division div = body.addDivision("browse-by-title","primary");
        div.setHead(T_head);

        // Navigatioal aid (really this is a poor version of pagination)
        Division jump = div.addInteractiveDivision("browse-navigation",
                "browse-title", Division.METHOD_POST, "secondary navigation");
        List jumpList = jump.addList("jump-list", List.TYPE_SIMPLE, "alphabet");
        for (char c = 'A'; c <= 'Z'; c++)
            jumpList.addItemXref("browse-title?startsWith=" + c, String
                    .valueOf(c));
        Para jumpForm = jump.addPara();
        jumpForm.addContent(T_starts_with);
        jumpForm.addText("startsWith").setHelp(T_starts_with_help);
        jumpForm.addButton("submit").setValue(T_go);

        // This div will hold the browsing results
        Division results = div.addDivision("browse-by-title-results","primary");
        results.setSimplePagination(itemsTotal, firstItemIndex, lastItemIndex,
                previousPage, nextPage);

        // Refrence all the browsed items
        ReferenceSet referenceSet = results.addReferenceSet("browse-by-title",
                ReferenceSet.TYPE_SUMMARY_LIST, "title", null);
        for (Item item : browseInfo.getItemResults())
        {
            referenceSet.addReference(item);
        }
    }
    
    /**
     * Recycle
     */
    public void recycle() {
    	this.validity = null;
    	super.recycle();
    }
}
