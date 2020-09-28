/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.model;

import static java.util.Collections.unmodifiableCollection;

import java.util.Collection;

import org.apache.commons.collections4.MultiValuedMap;

public class NestedEntity {

    private final MultiValuedMap<String, String> metadata;

    private final String parentSourceId;

    public NestedEntity(MultiValuedMap<String, String> metadata, String parentSourceId) {
        super();
        this.metadata = metadata;
        this.parentSourceId = parentSourceId;
    }

    public Collection<String> getMetadataValues(String metadataField) {
        return unmodifiableCollection(metadata.get(metadataField));
    }

    public Collection<String> getMetadataFields() {
        return unmodifiableCollection(metadata.keySet());
    }

    public String getParentSourceId() {
        return parentSourceId;
    }

}
