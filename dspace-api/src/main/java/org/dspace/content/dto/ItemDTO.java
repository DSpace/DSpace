/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dto;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This class acts as Data transfer object in which we can store data like in a
 * regular Item object, but this one isn't saved in the DB. This can freely be
 * used to represent Item without it being saved in the database, this will
 * typically be used when transferring data.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4Science)
 *
 */
public class ItemDTO {

    private final String id;

    private final boolean discoverable;

    private final List<MetadataValueDTO> metadataValues;

    private final List<BitstreamDTO> bitstreams;

    public ItemDTO(String id, List<MetadataValueDTO> metadataValues) {
        this(id, true, metadataValues, List.of());
    }

    public ItemDTO(String id, List<MetadataValueDTO> metadataValues, List<BitstreamDTO> bitstreams) {
        this(id, true, metadataValues, bitstreams);
    }

    public ItemDTO(String id, boolean discoverable, List<MetadataValueDTO> metadataValues,
        List<BitstreamDTO> bitstreams) {
        this.id = id;
        this.discoverable = discoverable;
        this.metadataValues = emptyIfNull(metadataValues);
        this.bitstreams = emptyIfNull(bitstreams);
    }

    public List<MetadataValueDTO> getMetadataValues(String metadataField) {
        return metadataValues.stream()
            .filter(metadataValue -> metadataValue.getMetadataField().equals(metadataField))
            .collect(Collectors.toList());
    }

    public String getId() {
        return id;
    }

    public boolean isDiscoverable() {
        return discoverable;
    }

    public List<MetadataValueDTO> getMetadataValues() {
        return metadataValues;
    }

    public List<BitstreamDTO> getBitstreams() {
        return bitstreams;
    }

}
