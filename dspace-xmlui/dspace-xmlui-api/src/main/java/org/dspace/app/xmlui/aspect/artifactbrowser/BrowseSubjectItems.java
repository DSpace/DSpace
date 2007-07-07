/*
 * BrowseAuthorItems.java
 *
 * Version: $Revision: 1.15 $
 *
 * Date: $Date: 2006/06/02 21:36:56 $
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
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Xref;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.xml.sax.SAXException;

/**
 * Display the results of browsing items belonging to a particular subject index.
 * This component may either apply to all communities and collection or be
 * scoped to just one community / collection depending upon the url.
 *  
 * @author Paulo Jobim
 * @author Alexey Maslov
 */
public class BrowseSubjectItems extends AbstractBrowse implements CacheableProcessingComponent
{
    /** Language strings: */
    private static final Message T_title = 
        message("xmlui.ArtifactBrowser.BrowseSubjectItems.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail =
        message("xmlui.ArtifactBrowser.BrowseSubjectItems.trail");
    
    private static final Message T_head = 
        message("xmlui.ArtifactBrowser.BrowseSubjectItems.head");
    
    private static final Message T_back =
        message("xmlui.ArtifactBrowser.BrowseSubjectItems.back");
    
    /** How many items should appear on one page */
    private static final int ITEMS_PER_PAGE = 20; 
    
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
        key += "-" + request.getParameter("subject");
        key += "-" + request.getParameter("subjectTop");
        key += "-" + request.getParameter("subjectBottom");
        
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
    	if (validity == null)
    	{
	        try
	        {            
	            DSpaceValidity validity = new DSpaceValidity();
	            
	            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
	            validity.add(dso);
	            
	            performBrowse(MODE_BY_SUBJECT_ITEM);
	            
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
        pageMeta.addTrailLink(contextPath, T_dspace_home);
        
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (dso != null)
            HandleUtil.buildHandleTrail(dso, pageMeta,contextPath);
        pageMeta.addTrail().addContent(T_trail);
    }

    /**
     * Add the browse-subject-item division.
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
    	// Preform the actual search
    	performBrowse(MODE_BY_SUBJECT_ITEM);
    	
        Request request = ObjectModelHelper.getRequest(objectModel);
        Item[] items = browseInfo.getItemResults();
        String top = request.getParameter("subjectTop");
        String bottom = request.getParameter("subjectBottom");
        String subject = request.getParameter("subject");

        // Determine Pagination variables
        int itemsTotal = items.length;
        int firstItemIndex = 0;
        int lastItemIndex = firstItemIndex + ITEMS_PER_PAGE;

        // Should we shift the pagination view because of a top or bottom
        // directive?
        if (top != null)
        {
            // Move the indexs to match pagination
            firstItemIndex = Integer.valueOf(top);
            lastItemIndex = firstItemIndex + ITEMS_PER_PAGE;
        }
        else if (bottom != null)
        {
            lastItemIndex = Integer.valueOf(bottom);
            firstItemIndex = lastItemIndex - ITEMS_PER_PAGE;

        }

        // Check four out of bounds indices.
        if (firstItemIndex < 0)
            firstItemIndex = 0;
        if (lastItemIndex > items.length - 1)
            lastItemIndex = items.length - 1;

        // Determine the next & previous link;
        String baseURL = "browse-subject-items?subject=" + URLEncode(subject);
        String previousPage = null;
        String nextPage = null;
        if (firstItemIndex > 0)
            previousPage = baseURL + "&subjectBottom=" + (firstItemIndex);
        if (lastItemIndex < items.length - 1)
            nextPage = baseURL + "&subjectTop=" + (lastItemIndex);
        
        // Build the DRI Body
        Division div = body.addDivision("browse-by-subject-item","primary");
        div.setHead(T_head.parameterize(subject));

        // Navigatioal aid (really this is a poor version of pagination)
        Division jump = div.addInteractiveDivision("browse-navigation",
                "browse-subject-items", Division.METHOD_POST,"secondary navigation");
        Xref link = jump.addPara().addXref("browse-subject?startsWith=" + URLEncode(subject));
        link.addContent(T_back);
        
        // This div will hold the browsing results
        Division results = div.addDivision("browse-by-subject-item-results","primary");
        results.setSimplePagination(itemsTotal, firstItemIndex + 1, lastItemIndex + 1,
                previousPage, nextPage);

        // Refrence all the browsed items
        ReferenceSet referenceSet = results.addReferenceSet("browse-by-subject-item",
                ReferenceSet.TYPE_SUMMARY_LIST, "subject", null);

        for (int i = firstItemIndex; i <= lastItemIndex; i++)
        {
            referenceSet.addReference(items[i]);
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
