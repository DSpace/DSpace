/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class AbbreviateTextDisplayStrategy extends AUniformDisplayStrategy {
    /** log4j category */
    private static Logger log = Logger.getLogger(AbbreviateTextDisplayStrategy.class);

    protected String getDisplayForValue(HttpServletRequest hrq, String value, int itemid) {
        if (StringUtils.isNotBlank(value)) {
            value = value.replaceAll("\\r\\n|\\n", "<br/>");
            return "<div class=\"abbreviate-me\">" + value + "</div>";
        } 
        return null;
    }
}
