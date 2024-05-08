/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.clarin;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.dao.clarin.ClarinItemDAO;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.clarin.ClarinItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the Item object.
 * This service is enhancement of the ItemService service for Clarin project purposes.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinItemServiceImpl implements ClarinItemService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ClarinItemServiceImpl.class);
    @Autowired
    ClarinItemDAO clarinItemDAO;

    @Autowired
    CollectionService collectionService;

    @Autowired
    ItemService itemService;

    @Override
    public List<Item> findByBitstreamUUID(Context context, UUID bitstreamUUID) throws SQLException {
        return clarinItemDAO.findByBitstreamUUID(context, bitstreamUUID);
    }

    @Override
    public List<Item> findByHandle(Context context, MetadataField metadataField, String handle) throws SQLException {
        return clarinItemDAO.findByHandle(context, metadataField, handle);
    }

    @Override
    public Community getOwningCommunity(Context context, DSpaceObject dso) {
        if (Objects.isNull(dso)) {
            return null;
        }
        int type = dso.getType();
        if (Objects.equals(type, Constants.COMMUNITY)) {
            return (Community) dso;
        }

        Collection owningCollection = null;
        if (Objects.equals(type, Constants.COLLECTION)) {
            owningCollection = (Collection) dso;
        }

        if (Objects.equals(type, Constants.ITEM)) {
            owningCollection = ((Item) dso).getOwningCollection();
        }

        if (Objects.isNull(owningCollection)) {
            return null;
        }

        try {
            List<Community> communities = owningCollection.getCommunities();
            if (CollectionUtils.isEmpty(communities)) {
                log.error("Community list of the owning collection is empty.");
                return null;
            }

            // First community is the owning community.
            Community owningCommunity = communities.get(0);
            if (Objects.isNull(owningCommunity)) {
                log.error("Owning community is null.");
                return null;
            }

            return owningCommunity;
        } catch (SQLException e) {
            log.error("Cannot getOwningCommunity for the Item: " + dso.getID() + ", because: " + e.getSQLState());
        }

        return null;
    }

    @Override
    public Community getOwningCommunity(Context context, UUID owningCollectionId) throws SQLException {
        Collection owningCollection = collectionService.find(context, owningCollectionId);

        if (Objects.isNull(owningCollection)) {
            return null;
        }

        try {
            List<Community> communities = owningCollection.getCommunities();
            if (CollectionUtils.isEmpty(communities)) {
                log.error("Community list of the owning collection is empty.");
                return null;
            }

            // First community is the owning community.
            Community owningCommunity = communities.get(0);
            if (Objects.isNull(owningCommunity)) {
                log.error("Owning community is null.");
                return null;
            }

            return owningCommunity;
        } catch (SQLException e) {
            log.error("Cannot getOwningCommunity for the Collection: " + owningCollectionId +
                    ", because: " + e.getSQLState());
        }
        return null;
    }

    @Override
    public void updateItemFilesMetadata(Context context, Item item) throws SQLException {
        List<Bundle> originalBundles = itemService.getBundles(item, Constants.CONTENT_BUNDLE_NAME);
        if (Objects.nonNull(originalBundles.get(0))) {
            updateItemFilesMetadata(context, item, originalBundles.get(0));
        } else {
            log.error("Cannot update item files metadata because the ORIGINAL bundle is null.");
        }
    }

    @Override
    public void updateItemFilesMetadata(Context context, Item item, Bundle bundle) throws SQLException {
        if (!Objects.equals(bundle.getName(), Constants.CONTENT_BUNDLE_NAME)) {
            return;
        }

        int totalNumberOfFiles = 0;
        long totalSizeofFiles = 0;

        /* Add local.has.files metadata */
        boolean hasFiles = false;
        List<Bundle> origs = itemService.getBundles(item, Constants.CONTENT_BUNDLE_NAME);
        for (Bundle orig : origs) {
            if (CollectionUtils.isNotEmpty(orig.getBitstreams())) {
                hasFiles = true;
            }
            for (Bitstream bit : orig.getBitstreams()) {
                totalNumberOfFiles ++;
                totalSizeofFiles += bit.getSizeBytes();
            }
        }

        itemService.clearMetadata(context, item, "local", "has", "files", Item.ANY);
        itemService.clearMetadata(context, item, "local", "files", "count", Item.ANY);
        itemService.clearMetadata(context, item, "local", "files", "size", Item.ANY);
        if ( hasFiles ) {
            itemService.addMetadata(context, item, "local", "has", "files", Item.ANY, "yes");
        } else {
            itemService.addMetadata(context, item,"local", "has", "files", Item.ANY, "no");
        }
        itemService.addMetadata(context, item,"local", "files", "count", Item.ANY, "" + totalNumberOfFiles);
        itemService.addMetadata(context, item,"local", "files", "size", Item.ANY, "" + totalSizeofFiles);
    }
}
