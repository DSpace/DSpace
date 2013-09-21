package org.dspace.rest.common;

import org.dspace.content.DCValue;
import org.dspace.core.Context;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: peterdietz
 * Date: 9/19/13
 * Time: 4:50 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlRootElement(name = "item")
public class Item {
    Integer itemID;

    String handle;

    @XmlElement(name = "metadata", required = true)
    Metadata metadata;

    List<Collection> parentCollections;


    //Bitstreams

    private static Context context;

    public Item(){}

    public Item(org.dspace.content.Item item, String expand) {
        setup(item, expand);
    }

    public Item(Integer itemID, String expand) {
        try {
            if(context == null || !context.isValid()) {
                context = new Context();
            }

            org.dspace.content.Item item = org.dspace.content.Item.find(context, itemID);
            setup(item, expand);
        } catch(Exception e) {
            //TODO
        }
    }

    private void setup(org.dspace.content.Item item, String expand) {
        List<String> expandFields = new ArrayList<String>();
        if(expand != null) {
            expandFields = Arrays.asList(expand.split(","));
        }

        metadata = new Metadata();

        try {
            this.setItemID(item.getID());
            DCValue[] allMetadata = item.getMetadata(org.dspace.content.Item.ANY, org.dspace.content.Item.ANY, org.dspace.content.Item.ANY, org.dspace.content.Item.ANY);
            metadata.setDCValues(Arrays.asList(allMetadata));

            this.setHandle(item.getHandle());
        } catch (Exception e) {

        }
    }


    public Integer getItemID() {
        return itemID;
    }

    public void setItemID(Integer itemID) {
        this.itemID = itemID;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }


}
