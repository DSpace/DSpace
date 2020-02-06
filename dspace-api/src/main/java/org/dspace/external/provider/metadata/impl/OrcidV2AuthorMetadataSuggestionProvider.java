/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.metadata.impl;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.impl.OrcidV2AuthorDataProvider;
import org.dspace.external.provider.metadata.MetadataSuggestionProvider;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The implementation of the {@link MetadataSuggestionProvider} for the {@link OrcidV2AuthorDataProvider}
 */
public class OrcidV2AuthorMetadataSuggestionProvider extends MetadataSuggestionProvider<OrcidV2AuthorDataProvider> {

    @Autowired
    private ItemService itemService;

    public List<ExternalDataObject> metadataQuery(Item item, int start, int limit) {
        // Concatenate metadata and send to query
        String familyName = itemService.getMetadataFirstValue(item, "person", "familyName", null, Item.ANY);
        String firstName = itemService.getMetadataFirstValue(item, "person", "givenName", null, Item.ANY);
        String query = null;
        if (StringUtils.isNotBlank(familyName) && StringUtils.isNotBlank(firstName)) {
            query = familyName + ", " + firstName;
        } else if (StringUtils.isBlank(familyName)) {
            query = firstName;
        } else if (StringUtils.isBlank(firstName)) {
            query = familyName;
        } else if (StringUtils.isBlank(familyName) && StringUtils.isBlank(firstName)) {
            return Collections.emptyList();
        }
        return query(query, start, limit);
    }

    public List<ExternalDataObject> query(String query, int start, int limit) {
        return getExternalDataProvider().searchExternalDataObjects(query, start, limit);
    }
}
