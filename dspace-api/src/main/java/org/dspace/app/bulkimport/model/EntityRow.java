/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.model;

import static java.util.Collections.unmodifiableList;
import static org.apache.commons.collections4.multimap.UnmodifiableMultiValuedMap.unmodifiableMultiValuedMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.List;

import org.apache.commons.collections4.MultiValuedMap;

/**
 * Class that model a row of the first sheet of the Bulk import excel.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class EntityRow {

    private final String id;

    private final int row;

    private final ImportAction action;

    private final MultiValuedMap<String, MetadataValueVO> metadata;

    private final List<MetadataGroup> metadataGroups;

    public EntityRow(String id, String action, int row, MultiValuedMap<String, MetadataValueVO> metadata,
        List<MetadataGroup> metadataGroups) {
        super();
        this.id = id;
        this.row = row + 1;
        this.action = isBlank(action) ? ImportAction.NOT_SPECIFIED : ImportAction.valueOf(action.toUpperCase());
        this.metadata = metadata;
        this.metadataGroups = metadataGroups;
    }

    public MultiValuedMap<String, MetadataValueVO> getMetadata() {
        return unmodifiableMultiValuedMap(metadata);
    }

    public List<MetadataGroup> getMetadataGroups() {
        return unmodifiableList(metadataGroups);
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
