/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Simple abstract class that provide utility method for get/check style configuration from dspace.cfg file 
 * @author Andrea Bollini
 * @version $Revision$
 *
 */
public abstract class AKeyBasedStyleSelection implements StyleSelection
{   
    public String[] getConfigurationForStyle(String style)
    {
        return DSpaceServicesFactory.getInstance().getConfigurationService()
                .getArrayProperty("webui.itemdisplay." + style);
    }
    
    protected boolean isConfigurationDefinedForStyle(String style)
    {
        return DSpaceServicesFactory.getInstance().getConfigurationService()
                .getProperty("webui.itemdisplay." + style) != null;
    }
}
