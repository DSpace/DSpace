package org.dspace.app.webui.util;

import javax.servlet.http.HttpServletRequest;

public interface IAtomicDisplayStrategy
{
    
    public String getPluginInstanceName();
    public void setPluginInstanceName(String name);
    
    public String getDisplayForValue(HttpServletRequest hrq, String field, String value, String authority, String language, int confidence, int itemid, boolean viewFull, String browseType, boolean disableCrossLinks,
            boolean emph);
}
