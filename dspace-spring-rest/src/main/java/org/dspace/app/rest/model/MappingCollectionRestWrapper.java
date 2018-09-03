/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.MappingCollectionRestController;
import org.dspace.content.Item;

public class MappingCollectionRestWrapper implements RestAddressableModel {

    @JsonIgnore
    private List<CollectionRest> mappingCollectionRestList;

    @JsonIgnore
    private Item item;
    public List<CollectionRest> getMappingCollectionRestList() {
        return mappingCollectionRestList;
    }

    public void setMappingCollectionRestList(List<CollectionRest> mappingCollectionRestList) {
        this.mappingCollectionRestList = mappingCollectionRestList;
    }

    public String getCategory() {
        return "core";
    }

    public Class getController() {
        return MappingCollectionRestController.class;
    }

    public String getType() {
        return "collection";
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }
}
