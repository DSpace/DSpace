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
import java.util.stream.Collectors;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.enhancer.impl.RelatedEntityItemEnhancerUtils;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.CrisConstants;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link VirtualField} that returns the values from the a
 * cris.virtual.<element> metadata using the <qualifier> provided in the form of
 * <schema>-<element>-<qualifier> as source metadata.
 * Source metadata that are not found in the cris.virtualsource.<element> leads to a PLACEHOLDER
 *
 * @author Andrea Bollini at 4science.comm
 *
 */
public class VirtualFieldToEnhancedMetadata implements VirtualField {

    @Autowired
    private ItemService itemService;

    @Autowired
    private RelatedEntityItemEnhancerUtils relatedEntityItemEnhancerUtils;

    @Override
    public String[] getMetadata(Context context, Item item, String fieldName) {
        ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        String[] fieldBits = fieldName.split("\\.");
        if (fieldBits.length != 3) {
            throw new IllegalArgumentException(
                    "VirtualFieldToEnhancedMetadata must be used specifying the EnhancedMetadata qualifier as "
                    + "element and the source metadata as qualifier, i.e. virtual.department.dc-contributor-author");
        }
        String virtualQualifier = fieldBits[1];
        String metadata = fieldBits[2].replaceAll("-", ".");
        Map<String, List<MetadataValue>> map = relatedEntityItemEnhancerUtils.getCurrentVirtualsMap(item,
                virtualQualifier);
        List<String> values = itemService.getMetadataByMetadataString(item, metadata).stream()
                .map(mv -> mv.getAuthority() != null && map.containsKey(mv.getAuthority())
                        ? map.get(mv.getAuthority()).get(0).getValue()
                        : CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
                .collect(Collectors.toList());
        String[] resultValues = values.toArray(new String[0]);
        return resultValues;
    }

}
