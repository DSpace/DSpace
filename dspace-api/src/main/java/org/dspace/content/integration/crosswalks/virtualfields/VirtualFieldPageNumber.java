/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.util.List;
import java.util.Map;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements virtual field processing for split pagenumber range information.
 *
 * @author bollini
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class VirtualFieldPageNumber implements VirtualField {

    private final ItemService itemService;

    @Autowired
    public VirtualFieldPageNumber(ItemService itemService) {
        this.itemService = itemService;
    }

    public String[] getMetadata(Item item, Map<String, String> fieldCache, String fieldName) {
        // Check to see if the virtual field is already in the cache
        // - processing is quite intensive, so we generate all the values on
        // first request
        if (fieldCache.containsKey(fieldName)) {
            return new String[] { fieldCache.get(fieldName) };
        }

        String[] virtualFieldName = fieldName.split("\\.");
        String qualifier = virtualFieldName[2];
        String separator = " - ";

        if (qualifier.equals("bibtex")) {
            separator = "--";
        }

        String metadataFirstPage = "dc.relation.firstpage";
        String metadataLastPage = "dc.relation.lastpage";
        // Get the citation from the item
        List<MetadataValue> dcvs = itemService.getMetadataByMetadataString(item, metadataFirstPage);
        List<MetadataValue> dcvs2 = itemService.getMetadataByMetadataString(item, metadataLastPage);

        if ((dcvs != null && dcvs.size() > 0) && (dcvs2 != null && dcvs2.size() > 0)) {
            String value = dcvs.get(0).getValue() + separator + dcvs2.get(0).getValue();
            fieldCache.put(fieldName, value);
            return new String[] { value };
        }
        return null;
    }
}