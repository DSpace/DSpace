/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.app;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.CrisConstants;
import org.dspace.xoai.util.ItemUtils;

/**
 * XOAIExtensionItemCompilePlugin to generate virtual metadata to represent
 * linked entity metadata as nested metadata (similar to the DSpace-CRIS < 7
 * representation)
 *
 */
public class LinkedEntityMetadataElementItemCompilePlugin implements XOAIExtensionItemCompilePlugin {
    // dc.contributor.author -> (crisitem.author.orcid, person.identifier.orcid)
    private Map<String, Map<String, String>> mapping;

    @Override
    public Metadata additionalMetadata(Context context, Metadata metadata, Item item) {
        try {
            ItemService itemService = ContentServiceFactory.getInstance().getItemService();
            for (String m : mapping.keySet()) {
                Map<String, String> virtualMetadataMap = mapping.get(m);
                for (MetadataValue mv : itemService.getMetadataByMetadataString(item, m)) {
                    Item linkedItem = null;
                    if (mv.getAuthority() != null) {
                        try {
                            linkedItem = itemService.find(context, UUID.fromString(mv.getAuthority()));
                        } catch (IllegalArgumentException e) {
                            // not an uuid
                        }
                    }
                    for (String target : virtualMetadataMap.keySet()) {
                        String source = virtualMetadataMap.get(target);
                        String value = CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
                        if (linkedItem != null) {
                            value = itemService.getMetadataByMetadataString(linkedItem, source).stream()
                                .map(v -> v.getValue()).findFirst()
                                .orElse(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE);
                        }
                        String[] mToCreate = target.split("\\.");
                        List<Element> parent = metadata.getElement();
                        Element other = null;
                        for (String part : mToCreate) {
                            List<Element> elements = metadata.getElement();
                            if (ItemUtils.getElement(elements, part) != null) {
                                other = ItemUtils.getElement(elements, part);
                            } else {
                                other = ItemUtils.create(part);
                                parent.add(other);
                            }
                            parent = other.getElement();
                        }
                        Element none = ItemUtils.create("none");
                        none.getField().add(ItemUtils.createValue("value", value));
                        other.getElement().add(none);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return metadata;
    }

    public void setMapping(Map<String, Map<String, String>> mapping) {
        this.mapping = mapping;
    }
}
