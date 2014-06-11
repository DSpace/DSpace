/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.util;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.util.IDisplayMetadataValueStrategy;
import org.dspace.browse.BrowseDSpaceObject;
import org.dspace.browse.BrowseItem;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.Utils;

public class CrisDisplayStrategy implements IDisplayMetadataValueStrategy
{

    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            DCValue[] metadataArray, BrowseItem item,
            boolean disableCrossLinks, boolean emph, PageContext pageContext)
    {
        ACrisObject crisObject = (ACrisObject) ((BrowseDSpaceObject) item)
                .getBrowsableDSpaceObject();
        String metadata = "-";
        if (metadataArray.length > 0)
        {
            metadata = "<a href=\"" + hrq.getContextPath() + "/cris/"
                    + crisObject.getPublicPath() + "/"
                    + ResearcherPageUtils.getPersistentIdentifier(crisObject)
                    + "\">" + Utils.addEntities(metadataArray[0].value)
                    + "</a>";
        }
        metadata = (emph ? "<strong>" : "") + metadata
                + (emph ? "</strong>" : "");
        return metadata;
    }

    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            DCValue[] metadataArray, Item item, boolean disableCrossLinks,
            boolean emph, PageContext pageContext)
    {
        // not used
        return null;
    }

    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, int colIdx, String field,
            DCValue[] metadataArray, Item item, boolean disableCrossLinks,
            boolean emph, PageContext pageContext) throws JspException
    {
        return null;
    }

    @Override
    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, int colIdx, String field,
            DCValue[] metadataArray, BrowseItem browseItem,
            boolean disableCrossLinks, boolean emph, PageContext pageContext)
            throws JspException
    {
        return null;
    }
}
