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

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements virtual field processing to build custom identifier
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class VirtualFieldCruiIdentifier implements VirtualField {

    private final ItemService itemService;

    @Autowired
    public VirtualFieldCruiIdentifier(ItemService itemService) {
        this.itemService = itemService;
    }

    public String[] getMetadata(Item item, Map<String, String> fieldCache, String fieldName) {
        if (fieldCache.containsKey(fieldName)) {
            return (new String[] { (String) fieldCache.get(fieldName) });
        }
        List<MetadataValue> mds = itemService.getMetadataByMetadataString(item, "dc.identifier.issn");
        String element = "";
        if (mds != null && mds.size() > 0) {
            element = mds.get(0).getValue();
        }
        mds = itemService.getMetadataByMetadataString(item, "dc.identifier.eissn");
        if (mds != null && mds.size() > 0) {
            element = mds.get(0).getValue();
        }
        mds = itemService.getMetadataByMetadataString(item, "dc.identifier.isbn");
        if (mds != null && mds.size() > 0) {
            element = mds.get(0).getValue();
        }
        mds = itemService.getMetadataByMetadataString(item, "dc.identifier.eisbn");
        if (mds != null && mds.size() > 0) {
            element = mds.get(0).getValue();
        }
        String handle = item.getHandle();
        if (StringUtils.isNotBlank(handle)) {
            fieldCache.put("virtual.cruiidentifier", element + "/" + handle.substring(6));
        } else {
            fieldCache.put("virtual.cruiidentifier", "ATT-" + item.getID());
        }
        if (fieldCache.containsKey(fieldName)) {
            return (new String[] { (String) fieldCache.get(fieldName) });
        } else {
            return null;
        }
    }
}
