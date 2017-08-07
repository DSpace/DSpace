/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.util;

import java.sql.SQLException;
import java.util.List;
import java.util.MissingResourceException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.webui.util.ASimpleDisplayStrategy;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.I18nUtil;
import org.dspace.discovery.IGlobalSearchResult;

public class ItemEnhancerDisplayStrategy extends ASimpleDisplayStrategy
{

    /**
     * log4j category
     */
    public static final Log log = LogFactory
            .getLog(ItemEnhancerDisplayStrategy.class);

    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, int itemId,
            String field, Metadatum[] metadataArray, boolean disableCrossLinks,
            boolean emph) throws JspException
    {
        return commonDisplay(hrq, itemId, field);
    }

    private String commonDisplay(HttpServletRequest hrq, int itemId,
            String field)
    {
        Item item;
        String metadata = "";
        try
        {
            item = Item.find(UIUtil.obtainContext(hrq), itemId);
            String result = item.getMetadata(field);
            
            if(result != null) {
                try {      
                    metadata += I18nUtil.getMessage(
                                "ItemEnhancerDisplayStrategy." + result, true);
                }
                catch(Exception ex) {
                    metadata += result;
                }
            }
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }

        return metadata;
    }

    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            List<String> metadataArray, IGlobalSearchResult item,
            boolean disableCrossLinks, boolean emph) throws JspException
    {
        return commonDisplay(hrq, item.getID(), field);
    }

}
