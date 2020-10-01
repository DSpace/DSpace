/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.model;

import static org.apache.commons.collections4.multimap.UnmodifiableMultiValuedMap.unmodifiableMultiValuedMap;

import org.apache.commons.collections4.MultiValuedMap;

public class MetadataGroup {

    private final String parentId;

    private final String name;

    private final MultiValuedMap<String, MetadataValueVO> metadata;

    public MetadataGroup(String parentId, String name, MultiValuedMap<String, MetadataValueVO> metadata) {
        super();
        this.metadata = metadata;
        this.parentId = parentId;
        this.name = name;
    }

    public MultiValuedMap<String, MetadataValueVO> getMetadata() {
        return unmodifiableMultiValuedMap(metadata);
    }

    public String getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

}
