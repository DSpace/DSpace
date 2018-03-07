/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import javax.servlet.http.HttpServletRequest;

public interface IAtomicDisplayStrategy
{
    
    public String getPluginInstanceName();
    public void setPluginInstanceName(String name);
    
    public String getDisplayForValue(HttpServletRequest hrq, String field, String value, String authority, String language, int confidence, int itemid, boolean viewFull, String browseType, boolean disableCrossLinks,
            boolean emph);
}
