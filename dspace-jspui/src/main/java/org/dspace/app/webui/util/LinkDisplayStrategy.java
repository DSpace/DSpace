/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.apache.log4j.Logger;
import org.dspace.content.Metadatum;
import org.dspace.core.Utils;

public class LinkDisplayStrategy extends AUniformDisplayStrategy implements IAtomicDisplayStrategy
{
    /** log4j category */
    private static Logger log = Logger.getLogger(LinkDisplayStrategy.class);

    protected String getDisplayForValue(HttpServletRequest hrq, String value, int itemid)
    {
        return getDisplayForValue(hrq, null, value, null, null, -1, itemid, false, null, false, false);
    }

    @Override
    public String getDisplayForValue(HttpServletRequest hrq, String field,
            String value, String authority, String language, int confidence,
            int itemid, boolean viewFull, String browseType,
            boolean disableCrossLinks, boolean emph)
    {
        StringBuffer sb = new StringBuffer();
        String startLink = "<a href=\"" + value + "\">";
        String endLink = "</a>";
        sb.append(startLink);
        sb.append(value);
        sb.append(endLink);
        return sb.toString();
    }

}
