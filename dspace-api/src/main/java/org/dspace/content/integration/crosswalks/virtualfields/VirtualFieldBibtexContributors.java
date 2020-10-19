/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements virtual field processing for authors and editors information
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class VirtualFieldBibtexContributors implements VirtualField {

    private final ItemService itemService;

    @Autowired
    public VirtualFieldBibtexContributors(ItemService itemService) {
        this.itemService = itemService;
    }

    public String[] getMetadata(Context context, Item item, String fieldName) {

        String metadata = "dc.contributor.author";
        if (StringUtils.endsWith(fieldName, "editor")) {
            metadata = "dc.contributor.editor";
        }

        List<MetadataValue> dcvs = itemService.getMetadataByMetadataString(item, metadata);
        if (dcvs != null && dcvs.size() > 0) {
            StringBuffer sb = new StringBuffer();
            for (MetadataValue  metadataValue : dcvs) {
                String[] split = metadataValue.getValue().split(", ");
                int splitLength = split.length;
                String str = (splitLength > 1) ? split[1] : "";
                String str2 = split[0];
                if (StringUtils.isNotBlank(str)) {
                    sb.append(str).append(" ");
                }
                sb.append(str2).append(" and ");
            }
            return new String[] { sb.substring(0, sb.length() - 5) };
        }
        return null;
    }

}