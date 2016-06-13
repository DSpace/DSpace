/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.core.PluginManager;

/**
 * Implements virtual field processing for citation information (based on Grahamt version).
 * 
 * @author pascarelli
 *
 */
public class VirtualFieldCitation implements VirtualFieldDisseminator, VirtualFieldIngester
{
    /** Logger */
    private static Logger log = Logger.getLogger(VirtualFieldCitation.class);
    
    public String[] getMetadata(Item item, Map<String, String> fieldCache, String fieldName)
    {   
        StreamDisseminationCrosswalk crosswalk = (StreamDisseminationCrosswalk)PluginManager.getNamedPlugin(StreamDisseminationCrosswalk.class, fieldName);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        String[] result = null;
        
        try
        {            
            crosswalk.disseminate(null, item, out);
            result = new String[]{out.toString()};   
        }        
        catch (Exception e)
        {
            log.error(e.getMessage(),e);
        }
        
        return result;
    }

    public boolean addMetadata(Item item, Map<String, String> fieldCache, String fieldName, String value)
    {
        // NOOP - we won't add any metadata yet, we'll pick it up when we finalise the item
        return true;
    }

    public boolean finalizeItem(Item item, Map<String, String> fieldCache)
    {     
        return false;
    }
}
