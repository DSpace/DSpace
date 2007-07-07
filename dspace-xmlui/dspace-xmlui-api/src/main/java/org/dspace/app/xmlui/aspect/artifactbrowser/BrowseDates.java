/*
 * BrowseDates.java
 *
 * Version: $Revision: 1.18 $
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
import java.util.Locale;

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
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.xml.sax.SAXException;

/**
 * Display the results of browsing by dates index. This component may either
 * apply to all communities and collection or be scoped to just one community /
 * collection depending upon the url.
 * 
 * @author Scott Phillips
 */
public class BrowseDates extends AbstractBrowse implements CacheableProcessingComponent
{   
    /** Languag strings */
    private static final Message T_title =
        message("xmlui.ArtifactBrowser.BrowseDates.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail =
        message("xmlui.ArtifactBrowser.BrowseDates.trail");
    
    private static final Message T_head =
        message("xmlui.ArtifactBrowser.BrowseDates.head");
    
    private static final Message T_jump_select =
        message("xmlui.ArtifactBrowser.BrowseDates.jump_select");
    
    private static final Message T_choose_month =
        message("xmlui.ArtifactBrowser.BrowseDates.choose_month");
    
    private static final Message T_choose_year = 
        message("xmlui.ArtifactBrowser.BrowseDates.choose_year");
    
    private static final Message T_jump_year =
        message("xmlui.ArtifactBrowser.BrowseDates.jump_year");
    
    private static final Message T_jump_year_help =
        message("xmlui.ArtifactBrowser.BrowseDates.jump_year_help");
    
    private static final Message T_go =
        message("xmlui.general.go");
    
    
    /**
     * These varables dictate when the drop down list of years is to break from
     * 1 year increments, to 5 year increments, to 10 year increments, and
     * finialy to stop.
     */
    private static final int ONE_YEAR_LIMIT = 10;

    private static final int FIVE_YEAR_LIMIT = 30;

    private static final int TEN_YEAR_LIMIT = 100;

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
	            
	            performBrowse(MODE_BY_DATE);
	            
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
     * Add the browse-date division.
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {

        this.performBrowse(MODE_BY_DATE);
        
        // Pagination variables
        int itemsTotal = browseInfo.getTotal();
        int firstItemIndex = browseInfo.getOverallPosition() + 1;
        int lastItemIndex = browseInfo.getOverallPosition()
                + browseInfo.getResultCount();
        String previousPage = previousPageURL("browse-date");
        String nextPage = nextPageURL("browse-date");
        
        
        
        // Build the DRI Body
        Division div = body.addDivision("browse-by-date","primary");
        div.setHead(T_head);

        // Navigatioal aid
        Division jump = div.addInteractiveDivision("browse-navigation",
                "browse-date", Division.METHOD_POST,"secondary navigation");
        Para jumpForm = jump.addPara();
        jumpForm.addContent(T_jump_select);
        Select month = jumpForm.addSelect("month");
        month.addOption(false, "-1", T_choose_month);
        for (int i = 1; i <= 12; i++)
        {
            month.addOption(false, String.valueOf(i), DCDate.getMonthName(i, Locale.getDefault()));
        }
        
        Select year = jumpForm.addSelect("year");
        year.addOption(false, "-1", T_choose_year);
        int currentYear = DCDate.getCurrent().getYear();
        int i = currentYear;
        int oneYearBreak = ((currentYear - ONE_YEAR_LIMIT) / 5) * 5;
        int fiveYearBreak = ((currentYear - FIVE_YEAR_LIMIT) / 10) * 10;
        int tenYearBreak = (currentYear - TEN_YEAR_LIMIT);
        do
        {
            year.addOption(false, String.valueOf(i), String.valueOf(i));

            if (i <= fiveYearBreak)
                i -= 10;
            else if (i <= oneYearBreak)
                i -= 5;
            else
                i--;
        }
        while (i > tenYearBreak);

        jumpForm = jump.addPara();
        jumpForm.addContent(T_jump_year);
        jumpForm.addText("startsWith").setHelp(T_jump_year_help);
        jumpForm.addButton("submit").setValue(T_go);

        // This div will hold the browsing results
        Division results = div.addDivision("browse-by-date-results","primary");
        results.setSimplePagination(itemsTotal, firstItemIndex, lastItemIndex,
                previousPage, nextPage);

        // Refrence all the browsed items
        ReferenceSet referenceSet = results.addReferenceSet("browse-by-date",
                ReferenceSet.TYPE_SUMMARY_LIST, "date", null);
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
