/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Utils;

/**
 * Configuration bean to map listener metadata that have a constrain to be processed together
 * (i.e dc.identifier.applicationnumber -> dc.date.filled).
 * 
 * @author Giuseppe Digilio (giuseppe dot digilio at 4science dot it)
 *
 */
public class MetadataListenerConstrain {

    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    /**
     * Metadata to constrain map
     */
    public Map<String, String> metadata;

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public List<MetadataValue> getCurrentConstrainMetadata(String metadataName, Item item,
            Map<String, List<MetadataValue>> metadataMap) {
        List<MetadataValue> currents = new ArrayList<MetadataValue>();
        if (isConstrainValid(metadataName, item, metadataMap)) {
            String[] tokenized = Utils.tokenize(metadataName);
            currents.addAll(itemService.getMetadata(item, tokenized[0], tokenized[1], tokenized[2], Item.ANY));
            String constrainMetadata = getConstrainMetadataName(metadataName);
            if (constrainMetadata == null) {
                currents = new ArrayList<MetadataValue>();
            }
        }

        return currents;
    }

    public String getConstrainMetadataName(String metadataName) {
        String constrainMetadata = null;

        if (hasConstrain(metadataName)) {
            constrainMetadata = this.metadata.get(metadataName);
            if (constrainMetadata == null) {
                for (Map.Entry<String,String> entry : metadata.entrySet()) {
                    if (entry.getValue() == metadataName) {
                        constrainMetadata = entry.getKey();
                        break;
                    }
                }
            }
        }

        return constrainMetadata;
    }

    public boolean hasConstrain(String metadataName) {
        boolean hasConstrain = false;
        for (Map.Entry<String,String> entry : metadata.entrySet()) {
            if (entry.getKey() == metadataName || entry.getValue() == metadataName) {
                hasConstrain = true;
                break;
            }
        }

        return hasConstrain;
    }

    public boolean isConstrainValid(String metadata, Item item, Map<String, List<MetadataValue>> metadataMap) {
        boolean isValid = false;

        String constrainMetadata = getConstrainMetadataName(metadata);
        if (constrainMetadata != null) {
            isValid = ((metadataMap.containsKey(metadata) && hasValue(metadata, item)) &&
                    (metadataMap.containsKey(constrainMetadata) && hasValue(constrainMetadata, item)));
        }

        return isValid;
    }

    protected boolean hasValue(String metadataName, Item item) {
        List<MetadataValue> values = new ArrayList<MetadataValue>();
        String[] tokenized = Utils.tokenize(metadataName);
        values = itemService.getMetadata(item, tokenized[0], tokenized[1], tokenized[2], Item.ANY);

        return !values.isEmpty();
    }
}
