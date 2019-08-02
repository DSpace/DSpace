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
import org.dspace.app.rest.MappedCollectionRestController;
import org.dspace.content.Item;

/**
 * The REST object that will define a list of CollectionRest objects to be returned by the REST api
 */
public class MappedCollectionRestWrapper implements RestAddressableModel {

    @JsonIgnore
    private List<CollectionRest> mappedCollectionRestList;

    @JsonIgnore
    private Item item;
    public List<CollectionRest> getMappedCollectionRestList() {
        return mappedCollectionRestList;
    }

    public void setMappedCollectionRestList(List<CollectionRest> mappedCollectionRestList) {
        this.mappedCollectionRestList = mappedCollectionRestList;
    }

    public String getCategory() {
        return "core";
    }

    public Class getController() {
        return MappedCollectionRestController.class;
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
