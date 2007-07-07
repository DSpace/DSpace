/*
 * BrowseAuthors.java
 *
 * Version: $Revision: 1.16 $
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
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.xml.sax.SAXException;

/**
 * Display the results of browsing authors index. This component may either apply
 * to all communities and collection or be scoped to just one community /
 * collection depending upon the url.
 * 
 * @author Scott Phillips
 */
public class BrowseAuthors extends AbstractBrowse implements CacheableProcessingComponent
{
    /** Language Strings */
    private static final Message T_title = 
        message("xmlui.ArtifactBrowser.BrowseAuthors.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail = 
        message("xmlui.ArtifactBrowser.BrowseAuthors.trail");
    
    private static final Message T_head = 
        message("xmlui.ArtifactBrowser.BrowseAuthors.head");
    
    private static final Message T_starts_with = 
        message("xmlui.ArtifactBrowser.BrowseAuthors.starts_with");
    
    private static final Message T_go = 
        message("xmlui.general.go");
    
    private static final Message T_column_heading = 
        message("xmlui.ArtifactBrowser.BrowseAuthors.column_heading");
    
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
    	if (validity == null)
    	{
	        try
	        {
	            DSpaceValidity validity = new DSpaceValidity();
	            
	            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
	            validity.add(dso);
	            
	            performBrowse(MODE_BY_AUTHOR);
	            
	            for (String author : browseInfo.getStringResults())
	            {
	                validity.add(author);
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
     * Add the browse-author division.
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        performBrowse(MODE_BY_AUTHOR);
        
        // Pagination variables
        int itemsTotal = browseInfo.getTotal();
        int firstItemIndex = browseInfo.getOverallPosition() + 1;
        int lastItemIndex = browseInfo.getOverallPosition()
                + browseInfo.getResultCount();
        String previousPage = previousPageURL("browse-author");
        String nextPage = nextPageURL("browse-author");

        
        // Build the DRI Body
        Division div = body.addDivision("browse-by-author","primary");
        div.setHead(T_head);

        // Navigatioal aid (really this is a poor version of pagination)
        Division jump = div.addInteractiveDivision("browse-navigation",
                "browse-author", Division.METHOD_POST,"secondary navigation");
        List jumpList = jump.addList("jump-list", List.TYPE_SIMPLE, "alphabet");
        for (char c = 'A'; c <= 'Z'; c++)
            jumpList.addItemXref("browse-author?startsWith=" + c, String
                    .valueOf(c));
        Para jumpForm = jump.addPara();
        jumpForm.addContent(T_starts_with);
        jumpForm.addText("startsWith");
        jumpForm.addButton("submit").setValue(T_go);

        // This div will hold the browsing results
        Division results = div.addDivision("browse-by-author-results","primary");
        results.setSimplePagination(itemsTotal, firstItemIndex, lastItemIndex,
                previousPage, nextPage);

        Table authorTable = results.addTable("browse-by-author-results",
                browseInfo.getResultCount() + 1, 1);
        authorTable.addRow(Row.ROLE_HEADER).addCell().addContent(T_column_heading);

        for (String author : browseInfo.getStringResults())
        {
            Cell cell = authorTable.addRow().addCell();
            cell.addXref("browse-author-items?author=" + URLEncode(author),
                    author);
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
