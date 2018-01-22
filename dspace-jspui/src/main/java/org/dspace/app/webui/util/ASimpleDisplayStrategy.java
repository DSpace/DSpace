/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.dspace.browse.BrowseItem;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.core.SelfNamedPlugin;
import org.dspace.discovery.IGlobalSearchResult;

public abstract class ASimpleDisplayStrategy extends SelfNamedPlugin implements
        IDisplayMetadataValueStrategy
{
    public abstract String getMetadataDisplay(HttpServletRequest hrq,
            int limit, boolean viewFull, String browseType, int colIdx,int itemid,
            String field, Metadatum[] metadataArray, boolean disableCrossLinks,
            boolean emph) throws JspException;

    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, boolean disableCrossLinks, boolean emph) throws JspException
    {
        return null;
    }

    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, BrowseItem browseItem,
            boolean disableCrossLinks, boolean emph)
            throws JspException
    {
        return getExtraCssDisplay(hrq, limit, b, browseType, colIdx, field,
                metadataArray, disableCrossLinks, emph);
    }

    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, Item item, boolean disableCrossLinks,
            boolean emph) throws JspException
    {
        return getExtraCssDisplay(hrq, limit, b, browseType, colIdx, field,
                metadataArray, disableCrossLinks, emph);
    }

    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, BrowseItem item,
            boolean disableCrossLinks, boolean emph)
            throws JspException
    {
        return getMetadataDisplay(hrq, limit, viewFull, browseType, colIdx,item.getID(),
                field, metadataArray, disableCrossLinks, emph);
    }

    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, Item item, boolean disableCrossLinks,
            boolean emph) throws JspException
    {
        return getMetadataDisplay(hrq, limit, viewFull, browseType, colIdx,item.getID(),
                field, metadataArray, disableCrossLinks, emph);
    }

	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType,
			int colIdx, String field, List<String> metadataArray, IGlobalSearchResult item, boolean disableCrossLinks,
			boolean emph) throws JspException 
	{		
        return getMetadataDisplay(hrq, limit, viewFull, browseType, colIdx, item.getID(),
                field, item.getMetadataValueInDCFormat(field), disableCrossLinks, emph);
	}
	
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType,
			int colIdx, String field, Metadatum[] metadataArray, IGlobalSearchResult item, boolean disableCrossLinks,
			boolean emph) throws JspException 
	{		
        return getMetadataDisplay(hrq, limit, viewFull, browseType, colIdx, item.getID(),
                field, metadataArray, disableCrossLinks, emph);
	}
	
}
