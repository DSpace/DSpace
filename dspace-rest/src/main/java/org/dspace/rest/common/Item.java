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

    Collection parentCollection;
    List<Collection> parentCollectionList;

    List<Community> parentCommunityList;

    List<MetadataEntry> metadata;

    List<Bitstream> bitstreams;

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

        if(expandFields.contains("metadata") || expandFields.contains("all")) {
            metadata = new ArrayList<MetadataEntry>();
            DCValue[] dcvs = item.getMetadata(org.dspace.content.Item.ANY, org.dspace.content.Item.ANY, org.dspace.content.Item.ANY, org.dspace.content.Item.ANY);
            for (DCValue dcv : dcvs) {
                if (!MetadataExposure.isHidden(context, dcv.schema, dcv.element, dcv.qualifier)) {
                    metadata.add(new MetadataEntry(dcv.getField(), dcv.value));
                }
            }
        } else {
            this.addExpand("metadata");
        }

        this.setArchived(Boolean.toString(item.isArchived()));
        this.setWithdrawn(Boolean.toString(item.isWithdrawn()));
        this.setLastModified(item.getLastModified().toString());

        if(expandFields.contains("parentCollection") || expandFields.contains("all")) {
            this.parentCollection = new Collection(item.getOwningCollection(), null, context, null, null);
        } else {
            this.addExpand("parentCollection");
        }

        if(expandFields.contains("parentCollectionList") || expandFields.contains("all")) {
            this.parentCollectionList = new ArrayList<Collection>();
            org.dspace.content.Collection[] collections = item.getCollections();
            for(org.dspace.content.Collection collection : collections) {
                this.parentCollectionList.add(new Collection(collection, null, context, null, null));
            }
        } else {
            this.addExpand("parentCollectionList");
        }

        if(expandFields.contains("parentCommunityList") || expandFields.contains("all")) {
            this.parentCommunityList = new ArrayList<Community>();
            org.dspace.content.Community[] communities = item.getCommunities();
            for(org.dspace.content.Community community : communities) {
                this.parentCommunityList.add(new Community(community, null, context));
            }
        } else {
            this.addExpand("parentCommunityList");
        }

        //TODO: paging - offset, limit
        if(expandFields.contains("bitstreams") || expandFields.contains("all")) {
            bitstreams = new ArrayList<Bitstream>();
            Bundle[] bundles = item.getBundles();
            for(Bundle bundle : bundles) {
                org.dspace.content.Bitstream[] itemBitstreams = bundle.getBitstreams();
                for(org.dspace.content.Bitstream itemBitstream : itemBitstreams) {
                    if(AuthorizeManager.authorizeActionBoolean(context, itemBitstream, org.dspace.core.Constants.READ)) {
                        bitstreams.add(new Bitstream(itemBitstream, null));
                    }
                }
            }
        } else {
            this.addExpand("bitstreams");
        }

        if(!expandFields.contains("all")) {
            this.addExpand("all");
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

    public Collection getParentCollection() {
        return parentCollection;
    }

    public List<Collection> getParentCollectionList() {
        return parentCollectionList;
    }

    public List<MetadataEntry> getMetadata() {
        return metadata;
    }

    public List<Bitstream> getBitstreams() {
        return bitstreams;
    }

    public List<Community> getParentCommunityList() {
        return parentCommunityList;
    }

	public void setParentCollection(Collection parentCollection) {
		this.parentCollection = parentCollection;
	}

	public void setParentCollectionList(List<Collection> parentCollectionList) {
		this.parentCollectionList = parentCollectionList;
	}

	public void setParentCommunityList(List<Community> parentCommunityList) {
		this.parentCommunityList = parentCommunityList;
	}

	@XmlElement(required = true)
	public void setMetadata(List<MetadataEntry> metadata) {
		this.metadata = metadata;
	}

	public void setBitstreams(List<Bitstream> bitstreams) {
		this.bitstreams = bitstreams;
	}
}
