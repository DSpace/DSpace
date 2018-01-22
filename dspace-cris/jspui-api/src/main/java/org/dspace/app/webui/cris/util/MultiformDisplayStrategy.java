/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.cris.util;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.ASimpleDisplayStrategy;
import org.dspace.app.webui.util.IAtomicDisplayStrategy;
import org.dspace.app.webui.util.IDisplayMetadataValueStrategy;
import org.dspace.content.Metadatum;
import org.dspace.core.PluginManager;
import org.dspace.utils.DSpace;

public class MultiformDisplayStrategy extends ASimpleDisplayStrategy
{
    /** log4j category */
    private static Logger log = Logger
            .getLogger(MultiformDisplayStrategy.class);

    private MultiformRegexConfigurator multiformRegexConfigurator;

    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, int itemid,
            String field, Metadatum[] metadataArray, boolean disableCrossLinks,
            boolean emph)
    {
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
            String displayvalue = "";
            String authority = metadataArray[j].authority;
            String value = metadataArray[j].value;
            String language = metadataArray[j].language;
            int confidence = metadataArray[j].confidence;
            // discover decorator by regex
            String decorator = getMultiformRegexConfigurator().checkRegex(value,
                    authority);

            IDisplayMetadataValueStrategy strategy = (IDisplayMetadataValueStrategy) PluginManager
                    .getNamedPlugin(IDisplayMetadataValueStrategy.class,
                            decorator);
            if (strategy instanceof IAtomicDisplayStrategy)
            {
                IAtomicDisplayStrategy ss = (IAtomicDisplayStrategy) strategy;
                ss.setPluginInstanceName(decorator);
                
                displayvalue = ss
                        .getDisplayForValue(hrq, field, value, authority, language, confidence, itemid, viewFull, browseType, disableCrossLinks, emph);
            }

            sb.append(displayvalue);

            if (j < (loopLimit - 1))
            {
                if (colIdx != -1) // we are showing metadata in a table row
                                  // (browse or item list)
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
            if (colIdx != -1)
            {
                sb.append("; ...");
            }
            else
            {
                sb.append("<br />...");
            }
        }

        return sb.toString();
    }

    public MultiformRegexConfigurator getMultiformRegexConfigurator()
    {
        if (multiformRegexConfigurator == null)
        {
            multiformRegexConfigurator = new DSpace()
                    .getSingletonService(MultiformRegexConfigurator.class);
        }
        return multiformRegexConfigurator;
    }
}
