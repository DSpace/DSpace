/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.util;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.util.IDisplayMetadataValueStrategy;
import org.dspace.browse.BrowseDSpaceObject;
import org.dspace.browse.BrowseItem;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Utils;
import org.dspace.discovery.IGlobalSearchResult;
import org.dspace.utils.DSpace;

public class CrisPeriodDisplayStrategy implements IDisplayMetadataValueStrategy {

	/**
	 * log4j category
	 */
	public static final Log log = LogFactory.getLog(CrisPeriodDisplayStrategy.class);

	private ApplicationService applicationService = new DSpace().getServiceManager()
			.getServiceByName("applicationService", ApplicationService.class);
	@Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType, int colIdx,
			String field, Metadatum[] metadataArray, BrowseItem item, boolean disableCrossLinks, boolean emph) {
		ACrisObject crisObject = (ACrisObject) ((BrowseDSpaceObject) item).getBrowsableDSpaceObject();
		String metadata = internalDisplay(hrq, metadataArray, crisObject);
		return metadata;
	}
	@Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType, int colIdx,
			String field, Metadatum[] metadataArray, Item item, boolean disableCrossLinks, boolean emph) {
		// not used
		return null;
	}
	@Override
	public String getExtraCssDisplay(HttpServletRequest hrq, int limit, boolean b, String browseType, int colIdx,
			String field, Metadatum[] metadataArray, Item item, boolean disableCrossLinks, boolean emph) throws JspException {
		return null;
	}

	@Override
	public String getExtraCssDisplay(HttpServletRequest hrq, int limit, boolean b, String browseType, int colIdx,
			String field, Metadatum[] metadataArray, BrowseItem browseItem, boolean disableCrossLinks, boolean emph) throws JspException {
		return null;
	}

	@Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType, int colIdx,
			String field, Metadatum[] metadataArray, IGlobalSearchResult item, boolean disableCrossLinks, boolean emph) throws JspException {

		ACrisObject crisObject = (ACrisObject) item;
		String metadata = internalDisplay(hrq, metadataArray, crisObject);
		return metadata;
	}
	
    private String internalDisplay(HttpServletRequest hrq,
            Metadatum[] metadataArray, ACrisObject crisObject)
    {
        String metadata = "N/A";
        if (metadataArray!=null && metadataArray.length > 0) {
            String publicPath = crisObject.getAuthorityPrefix();
            String authority = crisObject.getCrisID();
            
            metadata = "";
            metadata = prepareDateFrom(hrq, metadataArray, crisObject, publicPath,
                    authority);
            metadata += prepareDateTo(hrq, metadataArray, crisObject, publicPath,
                    authority);
        }
        return metadata;
    }
    

    private String prepareDateFrom(HttpServletRequest hrq,
            Metadatum[] metadataArray, ACrisObject crisObject,
            String publicPath, String authority)
    {
        String metadata = "";
        List<String> data = crisObject.getMetadataValue("communityservicestartdate");
        for(String value : data) {
            metadata += Utils.addEntities(value);
        }                
        return metadata;
    }
    
    private String prepareDateTo(HttpServletRequest hrq,
            Metadatum[] metadataArray, ACrisObject crisObject,
            String publicPath, String authority)
    {
        String metadata = "";
        List<String> data = crisObject.getMetadataValue("communityserviceenddate");
        for(String value : data) {
            metadata += "&nbsp;-&nbsp;";
            metadata += Utils.addEntities(value);
        }        
        return metadata;
    }
}