/*
 * CollectionStyleSelection.java
 *
 * Version: $Revision: 1 $
 *
 * Date: $Date: 2007-10-25 09:00:00 +0100 (thu, 25 oct 2007) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.webui.util;

import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;

/**
 * This is the standard (until 1.4.x) configuration mode based on owning collection handle
 * Style name is case insensitive.
 * 
 * @author Andrea Bollini
 * @version $Revision: 1 $
 * 
 */
public class CollectionStyleSelection extends AKeyBasedStyleSelection
{
    /** Hashmap of collection Handles to styles to use, from dspace.cfg */
    private static java.util.Map<String, String> styles;

    /** log4j logger */
    private static Logger log = Logger.getLogger(CollectionStyleSelection.class);
    
    /**
     * Get the style using the owning collection handle
     */
    public String getStyleForItem(Item item) throws SQLException
    {
        Collection c = item.getOwningCollection();
 
        if(c!=null)
        {    
            // Style specified & exists
            return getFromMap(c.getHandle());
        }
        else
            return "default";  //no specific style - item is an in progress Submission
    }
    
    /**
     * Put collection handle/style name mapping in an in-memory map.
     */
    private void readKeyStyleConfig()
    {
        styles = new HashMap();

        Enumeration e = ConfigurationManager.propertyNames();

        while (e.hasMoreElements())
        {
            String key = (String) e.nextElement();

            if (key.startsWith("webui.itemdisplay.")
                    && key.endsWith(".collections"))
            {
                String styleName = key.substring("webui.itemdisplay.".length(),
                        key.length() - ".collections".length());

                String[] collections = ConfigurationManager.getProperty(key)
                        .split(",");

                for (int i = 0; i < collections.length; i++)
                {
                    styles.put(collections[i].trim(), styleName.toLowerCase());
                }
            }
        }
    }
    
    /**
     * Get the style associated with the handle from the in-memory map. If the map is not already 
     * initialized read it from dspace.cfg
     * Check for the style configuration: return the default style if no configuration has found.
     * 
     * @param handle
     * @return the specific style or the default if not properly defined
     */
    public String getFromMap(String handle)
    {
        if (styles == null)
        {
            readKeyStyleConfig();
        }

        String styleName = (String) styles.get(handle);

        if (styleName == null)
        {
            // No specific style specified for this collection
            return "default";
        }

        // Specific style specified. Check style exists
        if (isConfigurationDefinedForStyle(styleName))
        {
            log.warn("dspace.cfg specifies undefined item display style '"
                    + styleName + "' for collection handle " + handle + ".  Using default");
            return "default";
        }

        return styleName;
    }
}
