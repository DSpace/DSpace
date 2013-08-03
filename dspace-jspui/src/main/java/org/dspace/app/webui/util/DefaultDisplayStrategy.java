/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.DCValue;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.core.Utils;

public class DefaultDisplayStrategy extends ASimpleDisplayStrategy
{
    /** log4j category */
    private static Logger log = Logger.getLogger(DefaultDisplayStrategy.class);

    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            DCValue[] metadataArray, boolean disableCrossLinks, boolean emph,
            PageContext pageContext) throws JspException
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
            String startLink = "";
            String endLink = "";
            if (!StringUtils.isEmpty(browseType) && !disableCrossLinks)
            {
                String argument;
                String value;
                if (metadataArray[j].authority != null &&
                        metadataArray[j].confidence >= MetadataAuthorityManager.getManager()
                            .getMinConfidence(metadataArray[j].schema, metadataArray[j].element, metadataArray[j].qualifier))
                {
                    argument = "authority";
                    value = metadataArray[j].authority;
                }
                else
                {
                    argument = "value";
                    value = metadataArray[j].value;
                }
                if (viewFull)
                {
                    argument = "vfocus";
                }
                try
                {
                    startLink = "<a href=\"" + hrq.getContextPath()
                            + "/browse?type=" + browseType + "&amp;" + argument
                            + "=" + URLEncoder.encode(value, "UTF-8");
                }
                catch (UnsupportedEncodingException e)
                {
                    throw new RuntimeException(e.getMessage(), e);
                }

                if (metadataArray[j].language != null)
                {
                    try
                    {
                        startLink = startLink + "&amp;" + argument + "_lang="
                                + URLEncoder.encode(metadataArray[j].language, "UTF-8");
                    }
                    catch (UnsupportedEncodingException e)
                    {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }

                if ("authority".equals(argument))
                {
                    startLink += "\" class=\"authority " + browseType + "\">";
                }
                else
                {
                    startLink = startLink + "\">";
                }
                endLink = "</a>";
            }
            sb.append(startLink);
            sb.append(Utils.addEntities(metadataArray[j].value));
            sb.append(endLink);
            if (j < (loopLimit - 1))
            {
                if (colIdx != -1) // we are showing metadata in a table row (browse or item list)
                {
                    sb.append("; ");
                }
                else
                {
                    // we are in the item tag
                    sb.append("<br />");
                }
            }
        }
        if (truncated)
        {
            String etal = LocaleSupport.getLocalizedMessage(pageContext,
                    "itemlist.et-al");
            sb.append(", " + etal);
        }
        
        if (colIdx != -1) // we are showing metadata in a table row (browse or item list)
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