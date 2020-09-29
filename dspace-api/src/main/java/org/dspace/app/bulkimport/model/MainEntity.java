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
import static org.apache.commons.lang3.StringUtils.isBlank;

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

    private final int row;

    private final ImportAction action;

    private final MultiValuedMap<String, String> metadata;

    private final List<NestedEntity> nestedEntities;

    public MainEntity(String id, String action, int row, MultiValuedMap<String, String> metadata,
        List<NestedEntity> nestedEntities) {
        super();
        this.id = id;
        this.row = row + 1;
        this.action = isBlank(action) ? ImportAction.NOT_SPECIFIED : ImportAction.valueOf(action.toUpperCase());
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

    public ImportAction getAction() {
        return action;
    }

    public int getRow() {
        return row;
    }

}
