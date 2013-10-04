package org.dspace.rest.common;

import org.dspace.content.Bundle;
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

    String name;

    String isArchived;
    String isWithdrawn;

    String lastModified;


    //TODO: Make optional
    Integer owningCollectionID;
    String owningCollectionName;

    @XmlElement(name = "metadata", required = true)
    Metadata metadata;

    @XmlElement(name = "bitstreams")
    List<Bitstream> bitstreams;

    List<Collection> parentCollections;

    @XmlElement(name = "link", required = true)
    private String link;

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

            this.setName(item.getName());

            this.setArchived(Boolean.toString(item.isArchived()));
            this.setWithdrawn(Boolean.toString(item.isWithdrawn()));
            this.setLastModified(item.getLastModified().toString());

            this.setOwningCollectionID(item.getOwningCollection().getID());
            this.setOwningCollectionName(item.getOwningCollection().getName());

            //Should be optional...
            bitstreams = new ArrayList<Bitstream>();
            Bundle[] bundles = item.getBundles();
            for(Bundle bundle : bundles) {
                org.dspace.content.Bitstream[] itemBitstreams = bundle.getBitstreams();
                for(org.dspace.content.Bitstream itemBitstream : itemBitstreams) {
                    bitstreams.add(new Bitstream(itemBitstream, expand));
                }

            }


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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArchived() {
        return isArchived;
    }

    public void setArchived(String archived) {
        isArchived = archived;
    }

    public String getWithdrawn() {
        return isWithdrawn;
    }

    public void setWithdrawn(String withdrawn) {
        isWithdrawn = withdrawn;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public Integer getOwningCollectionID() {
        return owningCollectionID;
    }

    public void setOwningCollectionID(Integer owningCollectionID) {
        this.owningCollectionID = owningCollectionID;
    }

    public String getOwningCollectionName() {
        return owningCollectionName;
    }

    public void setOwningCollectionName(String owningCollectionName) {
        this.owningCollectionName = owningCollectionName;
    }

    public String getLink() {
        return link;
    }

}
