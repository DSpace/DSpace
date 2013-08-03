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
import org.dspace.content.DCValue;
import org.dspace.core.I18nUtil;


public class AbstractMetadataDisplayStrategy extends ASimpleDisplayStrategy
{
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            DCValue[] metadataArray, boolean disableCrossLinks, boolean emph,
            PageContext pageContext)
    {	
        String metadataDisplay = "-";
        String metadataValue = metadataArray.length > 0 ?metadataArray[0].value:null;
        
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
        
        if (metadataValue != null)
        {
        	metadataValue = metadataValue.replace("\"", "");
        	metadataValue = StringUtils.abbreviate(metadataValue, 23);
            try
            {  
                metadataDisplay = MessageFormat.format(I18nUtil.getMessage(
                        "jsp.hasabstract.display-strategy.default", UIUtil
                                .obtainContext(hrq)), hrq.getContextPath(),
                        metadataValue);
               
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
            DCValue[] metadataArray, boolean disableCrossLinks, boolean emph,
            PageContext pageContext)
    {
        return "nowrap=\"nowrap\" align=\"center\"";
    }
}
