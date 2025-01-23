/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.util.List;

import org.dspace.app.bulkaccesscontrol.model.BulkAccessControlInput;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;

/**
 *  The ProvenanceService is responsible for creating provenance metadata for items based on the actions performed.
 *
 *  @author Milan Majchrak (dspace at dataquest.sk)
 */
public interface ProvenanceService {
    /**
     * Add a provenance message to the item when a new access condition is added
     *
     * @param context DSpace context object
     * @param item item to which the access condition is added
     * @param accessControl the access control input
     */
    void setItemPolicies(Context context, Item item, BulkAccessControlInput accessControl);

    /**
     * Add a provenance message to the item when a read policy is removed
     *
     * @param context DSpace context object
     * @param dso DSpace object from which the read policy is removed
     * @param resPolicies list of resource policies that are removed
     */
    void removeReadPolicies(Context context, DSpaceObject dso, List<ResourcePolicy> resPolicies);

    /**
     * Add a provenance message to the item when a bitstream policy is set
     *
     * @param context DSpace context object
     * @param bitstream bitstream to which the policy is set
     * @param item item to which the bitstream belongs
     * @param accessControl the access control input
     */
    void setBitstreamPolicies(Context context, Bitstream bitstream, Item item,
                                     BulkAccessControlInput accessControl);

    /**
     * Add a provenance message to the item when an item's license is edited
     *
     * @param context DSpace context object
     * @param item item to which the license is edited
     * @param newLicense true if the license is new, false if it's edited
     */
    void updateLicense(Context context, Item item, boolean newLicense);

    /**
     * Add a provenance message to the item when it's moved to a collection
     *
     * @param context DSpace context object
     * @param item item that is moved
     * @param collection collection to which the item is moved
     */
    void moveItem(Context context, Item item, Collection collection);

    /**
     * Add a provenance message to the item when it's mapped to a collection
     *
     * @param context DSpace context object
     * @param item item that is mapped
     * @param collection collection to which the item is mapped
     */
    void mappedItem(Context context, Item item, Collection collection);

    /**
     * Add a provenance message to the item when it's deleted from a mapped collection
     *
     * @param context DSpace context object
     * @param item item that is deleted from a mapped collection
     * @param collection collection from which the item is deleted
     */
    void deletedItemFromMapped(Context context, Item item, Collection collection);

    /**
     * Add a provenance message to the item when it's bitstream is deleted
     *
     * @param context DSpace context object
     * @param bitstream deleted bitstream
     * @param item item from which the bitstream is deleted
     */
    void deleteBitstream(Context context, Bitstream bitstream, Item item);

    /**
     * Add a provenance message to the item when metadata is added
     *
     * @param context DSpace context object
     * @param dso DSpace object to which the metadata is added
     * @param metadataField metadata field that is added
     */
    void addMetadata(Context context, DSpaceObject dso, MetadataField metadataField);

    /**
     * Add a provenance message to the item when metadata is removed
     *
     * @param context DSpace context object
     * @param dso DSpace object from which the metadata is removed
     */
    void removeMetadata(Context context, DSpaceObject dso, String schema, String element, String qualifier);

    /**
     * Add a provenance message to the item when metadata is removed at a given index
     *
     * @param context DSpace context object
     * @param dso DSpace object from which the metadata is removed
     * @param metadataValues list of metadata values
     * @param indexInt index at which the metadata is removed
     */
    void removeMetadataAtIndex(Context context, DSpaceObject dso, List<MetadataValue> metadataValues,
                               int indexInt);

    /**
     * Add a provenance message to the item when metadata is replaced
     *
     * @param context DSpace context object
     * @param dso DSpace object to which the metadata is replaced
     * @param metadataField metadata field that is replaced
     * @param oldMtdVal old metadata value
     */
    void replaceMetadata(Context context, DSpaceObject dso, MetadataField metadataField, String oldMtdVal);

    /**
     * Add a provenance message to the item when metadata is replaced
     *
     * @param context DSpace context object
     * @param dso DSpace object to which the metadata is replaced
     * @param metadataField metadata field that is replaced
     * @param oldMtdVal old metadata value
     */
    void replaceMetadataSingle(Context context, DSpaceObject dso, MetadataField metadataField,
                               String oldMtdVal);

    /**
     * Add a provenance message to the item when metadata is updated
     *
     * @param context DSpace context object
     * @param item item to which the metadata is updated
     * @param discoverable true if the item is discoverable, false if it's not
     */
    void makeDiscoverable(Context context, Item item, boolean discoverable);

    /**
     * Add a provenance message to the item when a bitstream is uploaded
     *
     * @param context DSpace context object
     * @param bundle bundle to which the bitstream is uploaded
     */
    void uploadBitstream(Context context, Bundle bundle);

    /**
     * Fetch an Item object using a service and return the first Item object from the list.
     * Log an error if the list is empty or if there is an SQL error
     *
     * @param context DSpace context object
     * @param bitstream bitstream to which the item is fetched
     */
    Item findItemByBitstream(Context context, Bitstream bitstream);

}
