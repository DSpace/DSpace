/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import org.apache.log4j.Logger;
import org.dspace.app.util.MetadataExposure;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bundle;
import org.dspace.content.DCValue;
import org.dspace.core.Context;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.sql.SQLException;
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
public class Item extends DSpaceObject {
    Logger log = Logger.getLogger(Item.class);

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

    //Bitstreams

    public Item(){}

    public Item(org.dspace.content.Item item, String expand, Context context) throws SQLException, WebApplicationException{
        super(item);
        setup(item, expand, context);
    }

    private void setup(org.dspace.content.Item item, String expand, Context context) throws SQLException{
        List<String> expandFields = new ArrayList<String>();
        if(expand != null) {
            expandFields = Arrays.asList(expand.split(","));
        }

        //Add Item metadata, omit restricted metadata fields (i.e. provenance).
        metadata = new Metadata();

        DCValue[] dcvs = item.getMetadata(org.dspace.content.Item.ANY, org.dspace.content.Item.ANY, org.dspace.content.Item.ANY, org.dspace.content.Item.ANY);
        for (DCValue dcv : dcvs) {
            if (!MetadataExposure.isHidden(context, dcv.schema, dcv.element, dcv.qualifier)) {
                metadata.addMetadataEntry(new MetadataEntry(dcv.getField(), dcv.value));
            }
        }

        this.setArchived(Boolean.toString(item.isArchived()));
        this.setWithdrawn(Boolean.toString(item.isWithdrawn()));
        this.setLastModified(item.getLastModified().toString());

        //TODO make optional, and set to object
        this.setOwningCollectionID(item.getOwningCollection().getID());
        this.setOwningCollectionName(item.getOwningCollection().getName());

        //Should be optional...
        //maybe TODO: Limit the number of response bitstreams in case of mega#
        bitstreams = new ArrayList<Bitstream>();
        Bundle[] bundles = item.getBundles();
        for(Bundle bundle : bundles) {
            //TODO, don't show license...
            org.dspace.content.Bitstream[] itemBitstreams = bundle.getBitstreams();
            for(org.dspace.content.Bitstream itemBitstream : itemBitstreams) {
                if(AuthorizeManager.authorizeActionBoolean(context, itemBitstream, org.dspace.core.Constants.READ)) {
                    bitstreams.add(new Bitstream(itemBitstream, expand));
                }
            }
        }
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
}
