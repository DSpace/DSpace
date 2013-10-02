package org.dspace.rest.common;

import javax.xml.bind.annotation.XmlElement;

/**
 * Created with IntelliJ IDEA.
 * User: peterdietz
 * Date: 9/29/13
 * Time: 11:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class LiteItem {
    //Internal value
    private Integer itemID;

    @XmlElement(name = "type", required = true)
    final String type = "item";

    @XmlElement(name = "link", required = true)
    private String link;

    //Internal metadata
    private String name;
    private String handle;

    public LiteItem() {

    }

    public LiteItem(org.dspace.content.Item item) {
        this.itemID = item.getID();
        this.name = item.getName();
        this.handle = item.getHandle();

        link = "/items/" + this.itemID;
    }


    public Integer getItemID() {
        return itemID;
    }

    public void setItemID(Integer itemID) {
        this.itemID = itemID;
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

    public String getLink() {
        return link;
    }
}
