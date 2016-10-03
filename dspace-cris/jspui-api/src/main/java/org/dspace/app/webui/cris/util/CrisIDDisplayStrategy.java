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

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.util.IDisplayMetadataValueStrategy;
import org.dspace.browse.BrowseDSpaceObject;
import org.dspace.browse.BrowseItem;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Utils;
import org.dspace.discovery.IGlobalSearchResult;

public class CrisIDDisplayStrategy implements IDisplayMetadataValueStrategy
{
    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, BrowseItem item,
            boolean disableCrossLinks, boolean emph)
    {
        ACrisObject crisObject = (ACrisObject) ((BrowseDSpaceObject) item)
                .getBrowsableDSpaceObject();
        String metadata = "";
        
        metadata = internalDisplay(hrq, emph, crisObject);
        return metadata;
    }

    private String internalDisplay(HttpServletRequest hrq, boolean emph,
            ACrisObject crisObject)
    {
        String metadata;
        String persistentIdentifier = ResearcherPageUtils.getPersistentIdentifier(crisObject);
		metadata = "<a href=\"" + hrq.getContextPath() + "/cris/"
                    + crisObject.getPublicPath() + "/"
                    + persistentIdentifier
                    + "\">" + Utils.addEntities(persistentIdentifier)
                    + "</a>";
        
        metadata = (emph ? "<strong>" : "") + metadata
                + (emph ? "</strong>" : "");
        return metadata;
    }
    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, Item item, boolean disableCrossLinks,
            boolean emph)
    {
        // not used
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
    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, int colIdx, String field,
            Metadatum[] metadataArray, BrowseItem browseItem,
            boolean disableCrossLinks, boolean emph)
            throws JspException
    {
        return null;
    }
    
	@Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType,
			int colIdx, String field, Metadatum[] metadataArray, IGlobalSearchResult item, boolean disableCrossLinks,
			boolean emph) throws JspException {
		ACrisObject crisObject = (ACrisObject)item;
        String metadata = "";
        
        metadata = internalDisplay(hrq, emph, crisObject);
        return metadata;
	}
}
