/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

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
    private java.util.Map<String, String> styles;

    /** log4j logger */
    private static Logger log = Logger.getLogger(CollectionStyleSelection.class);
    
    private final transient ConfigurationService configurationService
             = DSpaceServicesFactory.getInstance().getConfigurationService();
    
    /**
     * Get the style using the owning collection handle
     */
    @Override
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

        // Get all properties starting with "webui.itemdisplay"
        List<String> keys = configurationService.getPropertyKeys("webui.itemdisplay");
       
        for(String key: keys)
        {
            if (key.endsWith(".collections"))
            {
                String styleName = key.substring("webui.itemdisplay.".length(),
                        key.length() - ".collections".length());

                String[] collections = configurationService.getArrayProperty(key);

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
        if (!isConfigurationDefinedForStyle(styleName))
        {
            log.warn("dspace.cfg specifies undefined item display style '"
                    + styleName + "' for collection handle " + handle + ".  Using default");
            return "default";
        }

        return styleName;
    }
}
