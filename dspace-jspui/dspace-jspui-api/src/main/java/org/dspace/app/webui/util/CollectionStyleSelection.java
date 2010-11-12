/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
 * @version $Revision$
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
        {
            return "default";  //no specific style - item is an in progress Submission
        }
    }
    
    /**
     * Put collection handle/style name mapping in an in-memory map.
     */
    private void readKeyStyleConfig()
    {
        styles = new HashMap<String, String>();

        Enumeration<String> e = (Enumeration<String>)ConfigurationManager.propertyNames();

        while (e.hasMoreElements())
        {
            String key = e.nextElement();

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
