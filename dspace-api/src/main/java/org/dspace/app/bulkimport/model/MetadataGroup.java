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
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.vo.MetadataValueVO;

public class MetadataGroup implements ChildRow {

    private final String parentId;

    private final String name;

    private final MultiValuedMap<String, MetadataValueVO> metadata;

    private final String validationError;

    public MetadataGroup(String parentId, String name, String validationError) {
        super();
        this.metadata = new HashSetValuedHashMap<>();
        this.parentId = parentId;
        this.name = name;
        this.validationError = validationError;
    }

    public MetadataGroup(String parentId, String name, MultiValuedMap<String, MetadataValueVO> metadata) {
        super();
        this.metadata = metadata;
        this.parentId = parentId;
        this.name = name;
        this.validationError = null;
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

    public boolean isNotValid() {
        return StringUtils.isNotEmpty(validationError);
    }

    public String getValidationError() {
        return validationError;
    }

}
