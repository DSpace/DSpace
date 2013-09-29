package org.dspace.rest.common;

import javax.xml.bind.annotation.XmlElement;

/**
 * Created with IntelliJ IDEA.
 * User: peterdietz
 * Date: 9/29/13
 * Time: 11:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class LiteCollection {
    //Internal value
    private Integer collectionID;

    @XmlElement(name = "type", required = true)
    final String type = "collection";

    //Internal metadata
    private String name;
    private String handle;

    public LiteCollection() {

    }

    public LiteCollection(org.dspace.content.Collection collection) {
        this.collectionID = collection.getID();
        this.name = collection.getName();
        this.handle = collection.getHandle();
    }


    public Integer getCollectionID() {
        return collectionID;
    }

    public void setCollectionID(Integer collectionID) {
        this.collectionID = collectionID;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }
}
