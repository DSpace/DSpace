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
import org.dspace.core.ConfigurationManager;

/**
 * Implements virtual field processing for split keywords. At the moment only
 * fullname is available
 * 
 * @author bollini
 */
public class VirtualFieldKeywords implements VirtualFieldDisseminator, VirtualFieldIngester
{
    public String[] getMetadata(Item item, Map<String, String> fieldCache, String fieldName)
    {
        // Get the citation from the item
        String keywordsDC = "dc.subject.keywords";
        if(ConfigurationManager.getProperty("crosswalk.virtualkeywords.value") != null) 
        	keywordsDC = ConfigurationManager.getProperty("crosswalk.virtualkeywords.value");
        
        Metadatum[] dcvs = item.getMetadataValueInDCFormat(keywordsDC);
        
		String[] virtualFieldName = fieldName.split("\\.");

		// virtualFieldName[0] == "virtual"
		String qualifier = virtualFieldName[2];

		if ("single".equalsIgnoreCase(qualifier)) {
			if (dcvs != null && dcvs.length > 0) {
				if (dcvs.length > 1) {
					String[] result = new String[dcvs.length];
					for (int i = 0; i < dcvs.length; i++) {
						result[i] = dcvs[i].value;
					}
					return result;
				} else {
					String keywords = dcvs[0].value;
					String[] allKw = keywords.split("\\s*[,;]\\s*");
					return allKw;
				}
			}
		} else {
			if (dcvs != null && dcvs.length > 0) {
				if (dcvs.length > 1) {
					StringBuffer sb = new StringBuffer();
					for (int i = 0; i < dcvs.length; i++) {
						sb.append(dcvs[i].value).append("; ");
					}
					
					return new String[] { sb.toString() };
				} else {
					return new String[] { dcvs[0].value };
        		}
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
