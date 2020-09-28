/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.model;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;

import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.MultiValuedMap;

/**
 * Class that model a row of the first section of the Bulk import excel (main
 * entity).
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class MainEntity {

    private final String id;

    private final MultiValuedMap<String, String> metadata;

    private final List<NestedEntity> nestedEntities;

    /**
     * Create a new main entity.
     *
     * @param id             the internal item uuid
     * @param businessId     the item business id in the format sourceRef::id
     * @param metadata       a map between metadata fields and their values
     * @param nestedEntities the nested entitites related to the main entity
     */
    public MainEntity(String id, MultiValuedMap<String, String> metadata, List<NestedEntity> nestedEntities) {
        super();
        this.id = id;
        this.metadata = metadata;
        this.nestedEntities = nestedEntities;
    }

    public Collection<String> getMetadataValues(String metadataField) {
        return unmodifiableCollection(metadata.get(metadataField));
    }

    public Collection<String> getMetadataFields() {
        return unmodifiableCollection(metadata.keySet());
    }

    public List<NestedEntity> getNestedEntities() {
        return unmodifiableList(nestedEntities);
    }

    public String getId() {
        return id;
    }

}
