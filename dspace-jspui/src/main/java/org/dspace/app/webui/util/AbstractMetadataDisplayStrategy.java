/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.sql.SQLException;
import java.text.MessageFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.Metadatum;
import org.dspace.core.I18nUtil;


public class AbstractMetadataDisplayStrategy extends ASimpleDisplayStrategy
{
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType,int itemid, int colIdx, String field,
            Metadatum[] metadataArray, boolean disableCrossLinks, boolean emph)
    {	
        String metadataDisplay = "-";
        boolean found = false;
        
        for (Metadatum descrMetadata : metadataArray) {
        	if (StringUtils.startsWith(descrMetadata.qualifier, "abstract")) {
        		found = true; 
        		break;
        	}
        }
        
        //Assente di default 
        try
        {  
            metadataDisplay = MessageFormat.format(I18nUtil.getMessage(
                    "jsp.hasabstract.display-strategy.none", UIUtil
                            .obtainContext(hrq)), hrq.getContextPath(), "");
        }
        catch (SQLException e)
        {
            // converto a runtime
            throw new RuntimeException(e.getMessage(), e);
        }
        
        if (found)
        {

            try
            {  
                metadataDisplay = MessageFormat.format(I18nUtil.getMessage(
                        "jsp.hasabstract.display-strategy.default", UIUtil
                                .obtainContext(hrq)), hrq.getContextPath());
               
            }
            catch (SQLException e)
            {
                // converto a runtime
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        
        return metadataDisplay;
    }

    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String string, int colIdx, String field,
            Metadatum[] metadataArray, boolean disableCrossLinks, boolean emph,
            PageContext pageContext)
    {
        return "nowrap=\"nowrap\" align=\"center\"";
    }
}
