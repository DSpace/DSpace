package org.dspace.app.webui.util;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

public class AbstractDisplayStrategy extends AUniformDisplayStrategy
{
    /** log4j category */
    private static Logger log = Logger.getLogger(AbstractDisplayStrategy.class);

    protected String getDisplayForValue(HttpServletRequest hrq, String value)
    {
        if (value != null) 
        {
        	return value.replaceAll("\\r\\n|\\n","<br/>");
        }
        else 
        {
        	return null;
        }
    }
}
