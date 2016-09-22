/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.dspace.browse.BrowseItem;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Utils;
import org.dspace.discovery.IGlobalSearchResult;

public class TitleDisplayStrategy implements IDisplayMetadataValueStrategy
{

    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, BrowseItem item, boolean disableCrossLinks, boolean emph)
    {
        return getDisplay(hrq, metadataArray, item.isWithdrawn(), item.getHandle(), emph);
    }

    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, Item item, boolean disableCrossLinks, boolean emph)
    {
        return getDisplay(hrq, metadataArray, item.isWithdrawn(), item.getHandle(), emph);
    }
    

    private String getDisplay(HttpServletRequest hrq, Metadatum[] metadataArray,
            boolean isWithdrawn, String handle, boolean emph)
    {
        String metadata = "-";
        if (metadataArray.length > 0)
        {
            if (isWithdrawn)
            {
                metadata = Utils.addEntities(metadataArray[0].value);
            }
            else
            {
                metadata = "<a href=\"" + hrq.getContextPath() + "/handle/"
                + handle + "\">"
                + Utils.addEntities(metadataArray[0].value)
                + "</a>";
            }
        }
        metadata = (emph? "<strong>":"") + metadata + (emph? "</strong>":"");
        return metadata;
    }

    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String string, int colIdx, String field,
            Metadatum[] metadataArray, BrowseItem browseItem,
            boolean disableCrossLinks, boolean emph)
    {
        return null;
    }

    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, Item item, boolean disableCrossLinks,
            boolean emph) throws JspException
    {
        return null;
    }
    
	@Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType,
			int colIdx, String field, Metadatum[] metadataArray, IGlobalSearchResult item, boolean disableCrossLinks,
			boolean emph) throws JspException {
        String metadata = "-";
        if (metadataArray.length > 0)
        {
            if (item.isWithdrawn())
            {
                metadata = Utils.addEntities(metadataArray[0].value);
            }
            else
            {
                metadata = "<a href=\"" + hrq.getContextPath() + "/handle/"
                + item.getHandle() + "\">"
                + Utils.addEntities(metadataArray[0].value)
                + "</a>";
            }
        }
        metadata = (emph? "<strong>":"") + metadata + (emph? "</strong>":"");
        return metadata;		
	}
}
