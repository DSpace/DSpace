package org.dspace.app.webui.cris.util;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.webui.util.ASimpleDisplayStrategy;
import org.dspace.app.webui.util.IAtomicDisplayStrategy;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.Metadatum;
import org.dspace.core.I18nUtil;

public class SimpleGenericDisplayStrategy extends ASimpleDisplayStrategy
        implements IAtomicDisplayStrategy
{
    /**
     * log4j category
     */
    public static final Log log = LogFactory
            .getLog(SimpleGenericDisplayStrategy.class);

    @Override
    public String getDisplayForValue(HttpServletRequest hrq, String field,
            String value, String authority, String language, int confidence,
            int itemid, boolean viewFull, String browseType,
            boolean disableCrossLinks, boolean emph)
    {

        try
        {
            String tmpValue = value;
            if (StringUtils.isNotBlank(authority))
            {
                tmpValue = authority;
            }
            String metadataDisplay = MessageFormat.format(
                    I18nUtil.getMessage("jsp.display-strategy." + field + "." + this.getPluginInstanceName(),
                            UIUtil.obtainContext(hrq)),
                    hrq.getContextPath(), tmpValue, value, authority, field);
            return metadataDisplay;
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, int itemid,
            String field, Metadatum[] metadataArray, boolean disableCrossLinks,
            boolean emph) throws JspException
    {
        String metadata;
        // limit the number of records if this is the author field (if
        // -1, then the limit is the full list)
        boolean truncated = false;
        int loopLimit = metadataArray.length;
        if (limit != -1)
        {
            loopLimit = (limit > metadataArray.length ? metadataArray.length
                    : limit);
            truncated = (limit < metadataArray.length);
            log.debug("Limiting output of field " + field + " to "
                    + Integer.toString(loopLimit) + " from an original "
                    + Integer.toString(metadataArray.length));
        }

        StringBuffer sb = new StringBuffer();
        for (int j = 0; j < loopLimit; j++)
        {
            if (metadataArray != null && metadataArray.length > 0)
            {
                sb.append(getDisplayForValue(hrq, field, metadataArray[j].value,
                        metadataArray[j].authority, metadataArray[j].language,
                        metadataArray[j].confidence, itemid, viewFull,
                        browseType, disableCrossLinks, emph));
                if (j < (loopLimit - 1))
                {
                    if (colIdx != -1)
                    {
                        sb.append(";&nbsp;");
                    }
                    else
                    {
                        // we are in the item tag
                        sb.append("<br />");
                    }
                }
            }
            else
            {
                break;
            }
        }
        if (truncated)
        {
            Locale locale = UIUtil.getSessionLocale(hrq);
            String etal = I18nUtil.getMessage("itemlist.et-al", locale);
            sb.append(", " + etal);
        }

        if (colIdx != -1) // we are showing metadata in a table row (browse or
                          // item list)
        {
            metadata = (emph ? "<strong><em>" : "<em>") + sb.toString()
                    + (emph ? "</em></strong>" : "</em>");
        }
        else
        {
            // we are in the item tag
            metadata = (emph ? "<strong>" : "") + sb.toString()
                    + (emph ? "</strong>" : "");
        }

        return metadata;
    }

}
