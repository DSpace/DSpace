/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.MappedItemRestController;

/**
 * The REST object that will define a list of ItemRest objects to be returned by the REST api
 */
public class MappedItemRestWrapper implements RestAddressableModel {

    @JsonIgnore
    private List<ItemRest> mappedItemRestList;

    private UUID collectionUuid;

    public UUID getCollectionUuid() {
        return collectionUuid;
    }

    public void setCollectionUuid(UUID collectionUuid) {
        this.collectionUuid = collectionUuid;
    }

    public List<ItemRest> getMappedItemRestList() {
        return mappedItemRestList;
    }

    public void setMappedItemRestList(List<ItemRest> mappedItemRestList) {
        this.mappedItemRestList = mappedItemRestList;
    }

    public String getCategory() {
        return "core";
    }

    public Class getController() {
        return MappedItemRestController.class;
    }

    public String getType() {
        return "collection";
    }
}
