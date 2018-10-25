/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.List;

/**
 * This class represents an Entity object. An Entity object has an Item that it describes with a list
 * of relationships that it includes as well.
 */
public class Entity {

    /**
     * The Item that is being described by this Entity
     */
    private Item item;
    /**
     * The relationships for the Item that is included in this Entity
     */
    private List<Relationship> relationships;

    /**
     * constructor for the Entity object
     * @param item              The Item to be included in this Entity object as a property
     * @param relationshipList  The list of relationships
     */
    public Entity(Item item,List<Relationship> relationshipList) {
        setItem(item);
        setRelationships(relationshipList);
    }

    /**
     * Standard getter for the Item for this Entity object
     * @return  The Item that is described in this Entity object
     */
    public Item getItem() {
        return item;
    }

    /**
     * Standard setter for the Item for this Entity object
     * @param item  The Item to be set
     */
    public void setItem(Item item) {
        this.item = item;
    }

    /**
     * Standard getter for the list of relationships for the Item in this Entity object
     * @return  the list of relationships
     */
    public List<Relationship> getRelationships() {
        return relationships;
    }

    /**
     * Standard setter for the list of relationships for the Item in this Entity object
     * @param relationships The list of relationships to be set
     */
    public void setRelationships(List<Relationship> relationships) {
        this.relationships = relationships;
    }
}
