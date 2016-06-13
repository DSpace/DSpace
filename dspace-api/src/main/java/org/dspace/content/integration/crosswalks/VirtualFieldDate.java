/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.util.Map;

import org.dspace.content.Metadatum;
import org.dspace.content.Item;

/**
 * Implements virtual field processing for split pagenumber range information.
 * 
 * @author bollini
 */
public class VirtualFieldDate implements VirtualFieldDisseminator, VirtualFieldIngester
{
    public String[] getMetadata(Item item, Map<String, String> fieldCache, String fieldName)
    {
        // Check to see if the virtual field is already in the cache
        // - processing is quite intensive, so we generate all the values on first request
        if (fieldCache.containsKey(fieldName))
            return new String[] { fieldCache.get(fieldName) };

        String[] virtualFieldName = fieldName.split("\\.");
        
        String qualifier = virtualFieldName[1];
        
        if(qualifier.equals("date")){
        
	        // Get the citation from the item
	        Metadatum[] dcvs = item.getMetadataValueInDCFormat("dc.date.issued");
	        
	        if (dcvs != null && dcvs.length > 0) 
	        {   
	            fieldCache.put("virtual.date.year", dcvs[0].value.substring(0, 4));
	            if(dcvs[0].value.length() > 4)
	            	fieldCache.put("virtual.date.month", dcvs[0].value.substring(5, 7));
	
	            // Return the value of the virtual field (if any)
	            if (fieldCache.containsKey(fieldName))
	                return new String[] { fieldCache.get(fieldName) };
	        }
        }
        
        return null;
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
