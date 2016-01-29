/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.text.DecimalFormat;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.core.I18nUtil;

public class CountDisplayStrategy extends AUniformDisplayStrategy
{
    /** log4j category */
    private static Logger log = Logger.getLogger(CountDisplayStrategy.class);

    private DecimalFormat formatter = new DecimalFormat("###,###");
    protected String getDisplayForValue(HttpServletRequest hrq, String value, int itemid)
    {
    	Double d = null;
    	try {
    		d = Double.parseDouble(value);
    		if (d == null) return I18nUtil.getMessage("itemlist.count.na");
    		return formatter.format(d);
    	}
    	catch (Exception e) {
    		return value;
    	}
    }
}
