/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.util.List;
import java.util.Map;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;

/**
 * Implements virtual field processing for split keywords. At the moment only
 * fullname is available
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4sciecnce.it)
 */
public class VirtualFieldKeywords implements VirtualFieldDisseminator, VirtualFieldIngester {

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    public String[] getMetadata(Item item, Map<String, String> fieldCache, String fieldName) {
        // Get the citation from the item
        String keywordsDC = "dc.subject.keywords";
        if (ConfigurationManager.getProperty("crosswalk.virtualkeywords.value") != null) {
            keywordsDC = ConfigurationManager.getProperty("crosswalk.virtualkeywords.value");
        }

        List<MetadataValue> dcvs = itemService.getMetadataByMetadataString(item, keywordsDC);
        String[] virtualFieldName = fieldName.split("\\.");
        String qualifier = virtualFieldName[2];

        if ("single".equalsIgnoreCase(qualifier)) {
            if (dcvs != null && dcvs.size() > 0) {
                if (dcvs.size() > 1) {
                    String[] result = new String[dcvs.size()];
                    for (MetadataValue mv : dcvs) {
                        int i = 0;
                        result[i] = mv.getValue();
                        i++;
                    }
                    return result;
                } else {
                    String keywords = dcvs.get(0).getValue();
                    String[] allKw = keywords.split("\\s*[,;]\\s*");
                    return allKw;
                }
            }
        } else {
            if (dcvs != null && dcvs.size() > 0) {
                if (dcvs.size() > 1) {
                    StringBuffer sb = new StringBuffer();
                    for (MetadataValue mv : dcvs) {
                        sb.append(mv.getValue()).append("; ");
                    }
                    return new String[] { sb.toString() };
                } else {
                    return new String[] { dcvs.get(0).getValue() };
                }
            }
        }
        return null;
    }

    public boolean addMetadata(Item item, Map<String, String> fieldCache, String fieldName, String value) {
        // NOOP - we won't add any metadata yet, we'll pick it up when we finalise the
        // item
        return true;
    }

    public boolean finalizeItem(Item item, Map<String, String> fieldCache) {
        return false;
    }
}
