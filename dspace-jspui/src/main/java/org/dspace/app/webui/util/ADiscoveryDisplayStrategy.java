/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.apache.commons.lang.ArrayUtils;
import org.dspace.browse.BrowseItem;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.core.SelfNamedPlugin;
import org.dspace.discovery.IGlobalSearchResult;

public abstract class ADiscoveryDisplayStrategy extends SelfNamedPlugin implements
        IDisplayMetadataValueStrategy
{

	public abstract String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType,
			int colIdx, String field, List<String> metadataArray, IGlobalSearchResult item, boolean disableCrossLinks,
			boolean emph, PageContext pageContext) throws JspException; 
	
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType,
			int colIdx, String field, Metadatum[] metadataArray, IGlobalSearchResult item, boolean disableCrossLinks,
			boolean emph, PageContext pageContext) throws JspException 
	{		
		List<String> metadataList = new ArrayList<String>();
		for(Metadatum mdc : metadataArray) {
			metadataList.add(mdc.value);
		}
		return getMetadataDisplay(hrq, limit, viewFull, browseType, colIdx, field, metadataList, item, disableCrossLinks, emph, pageContext);
	}
}
