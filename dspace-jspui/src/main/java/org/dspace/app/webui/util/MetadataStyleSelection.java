/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;

/**
 * Use the value of the metadata specified with the key <code>webui.display.metadata-style</code>
 * as name for the display style of the item. Style name is case insensitive.
 * 
 * @author Andrea Bollini
 * @version $Revision$
 * 
 */
public class MetadataStyleSelection extends AKeyBasedStyleSelection
{
    /** log4j logger */
    private static Logger log = Logger
            .getLogger(MetadataStyleSelection.class);
    
    private ItemService itemService;
    
    
    public MetadataStyleSelection() {
    	itemService = ContentServiceFactory.getInstance().getItemService();
    }
    /**
     * Get the style using an item metadata
     */
    public String getStyleForItem(Item item) throws SQLException
    {
        String metadata = ConfigurationManager.getProperty("webui.itemdisplay.metadata-style");
        List<MetadataValue> value = itemService.getMetadataByMetadataString(item, metadata);
        String styleName = "default";
        if (value.size() > 0)
        {
            if (value.size() >= 1)
            {
                log
                .warn("more then one value for metadata '"
                        + metadata
                        + "'. Using the first one");
            }
            styleName = value.get(0).getValue().toLowerCase();            
        }
        
       
        // Specific style specified. Check style exists
        if (!isConfigurationDefinedForStyle(styleName))
        {
            log.warn("metadata '" + metadata + "' specify undefined item display style '"
                    + styleName + "'.  Using default");
            return "default";
        }
        // Style specified & exists
        return styleName;
    }
}
