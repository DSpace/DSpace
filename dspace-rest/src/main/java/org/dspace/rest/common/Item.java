/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import org.apache.log4j.Logger;
import org.dspace.app.util.factory.UtilServiceFactory;
import org.dspace.app.util.service.MetadataExposureService;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bundle;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;

import javax.servlet.ServletContext;
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
@SuppressWarnings("deprecation")
@XmlRootElement(name = "item")
public class Item extends DSpaceObject {
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected MetadataExposureService metadataExposureService = UtilServiceFactory.getInstance().getMetadataExposureService();
    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

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

    public Item(org.dspace.content.Item item, ServletContext servletContext, String expand, Context context)
        throws SQLException, WebApplicationException
    {
        super(item, servletContext);
        setup(item, servletContext, expand, context);
    }

    private void setup(org.dspace.content.Item item, ServletContext servletContext, String expand, Context context)
        throws SQLException
    {
        List<String> expandFields = new ArrayList<String>();
        if (expand != null)
        {
            expandFields = Arrays.asList(expand.split(","));
        }

        if (expandFields.contains("metadata") || expandFields.contains("all"))
        {
            metadata = new ArrayList<MetadataEntry>();
            List<MetadataValue> metadataValues = itemService.getMetadata(
                item, org.dspace.content.Item.ANY, org.dspace.content.Item.ANY,
                org.dspace.content.Item.ANY, org.dspace.content.Item.ANY);

            for (MetadataValue metadataValue : metadataValues)
            {
                MetadataField metadataField = metadataValue.getMetadataField();
                if (!metadataExposureService.isHidden(context,
                    metadataField.getMetadataSchema().getName(),
                    metadataField.getElement(),
                    metadataField.getQualifier()))
                {
                    metadata.add(new MetadataEntry(metadataField.toString('.'),
                        metadataValue.getValue(), metadataValue.getLanguage()));
                }
            }
        }
        else
        {
            this.addExpand("metadata");
        }

        this.setArchived(Boolean.toString(item.isArchived()));
        this.setWithdrawn(Boolean.toString(item.isWithdrawn()));
        this.setLastModified(item.getLastModified().toString());

        if (expandFields.contains("parentCollection") || expandFields.contains("all"))
        {
            if (item.getOwningCollection() != null)
            {
                this.parentCollection = new Collection(item.getOwningCollection(),
                    servletContext, null, context, null, null);
            }
            else
            {
                this.addExpand("parentCollection");
            }
        }
        else
        {
            this.addExpand("parentCollection");
        }

        if (expandFields.contains("parentCollectionList") || expandFields.contains("all"))
        {
            this.parentCollectionList = new ArrayList<Collection>();
            List<org.dspace.content.Collection> collections = item.getCollections();
            for (org.dspace.content.Collection collection : collections)
            {
                this.parentCollectionList.add(new Collection(collection,
                    servletContext, null, context, null, null));
            }
        }
        else
        {
            this.addExpand("parentCollectionList");
        }

        if (expandFields.contains("parentCommunityList") || expandFields.contains("all"))
        {
            this.parentCommunityList = new ArrayList<Community>();
            List<org.dspace.content.Community> communities = itemService.getCommunities(context, item);

            for (org.dspace.content.Community community : communities)
            {
                this.parentCommunityList.add(new Community(community, servletContext, null, context));
            }
        }
        else
        {
            this.addExpand("parentCommunityList");
        }

        //TODO: paging - offset, limit
        if (expandFields.contains("bitstreams") || expandFields.contains("all"))
        {
            bitstreams = new ArrayList<Bitstream>();

            List<Bundle> bundles = item.getBundles();
            for (Bundle bundle : bundles)
            {

                List<org.dspace.content.Bitstream> itemBitstreams = bundle.getBitstreams();
                for (org.dspace.content.Bitstream itemBitstream : itemBitstreams)
                {
                    if (authorizeService.authorizeActionBoolean(context, itemBitstream, org.dspace.core.Constants.READ))
                    {
                        bitstreams.add(new Bitstream(itemBitstream, servletContext, null, context));
                    }
                }
            }
        }
        else
        {
            this.addExpand("bitstreams");
        }

        if (!expandFields.contains("all"))
        {
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
