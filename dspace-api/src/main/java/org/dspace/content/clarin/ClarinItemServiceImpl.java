/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.clarin;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
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
    private static final String DELIMETER = ",";
    private static final String NO_YEAR = "0000";

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

    @Override
    public void updateItemFilesMetadata(Context context, Bitstream bit) throws SQLException {
        // Get the Item the bitstream is associated with
        Item item = null;
        Bundle bundle = null;
        List<Bundle> origs = bit.getBundles();
        for (Bundle orig : origs) {
            if (!Constants.CONTENT_BUNDLE_NAME.equals(orig.getName())) {
                continue;
            }

            List<Item> items = orig.getItems();
            if (CollectionUtils.isEmpty(items)) {
                continue;
            }

            item = items.get(0);
            bundle = orig;
            break;
        }

        // It could be null when the bundle name is e.g. `LICENSE`
        if (Objects.isNull(item) || Objects.isNull(bundle)) {
            return;
        }
        this.updateItemFilesMetadata(context, item, bundle);
    }

    @Override
    public void updateItemDatesMetadata(Context context, Item item) throws SQLException {
        if (Objects.isNull(context)) {
            log.error("Cannot update item dates metadata because the context is null.");
            return;
        }

        List<MetadataValue> approximatedDates =
                itemService.getMetadata(item, "local", "approximateDate", "issued", Item.ANY, false);

        if (CollectionUtils.isEmpty(approximatedDates) || StringUtils.isBlank(approximatedDates.get(0).getValue())) {
            log.warn("Cannot update item dates metadata because the approximate date is empty.");
            return;
        }

        // Get the approximate date value from the metadata
        String approximateDateValue = approximatedDates.get(0).getValue();

        // Split the approximate date value by the delimeter and get the list of years.
        List<String> listOfYearValues = Arrays.asList(approximateDateValue.split(DELIMETER));
        // Trim the list of years - remove leading and trailing whitespaces
        listOfYearValues.replaceAll(String::trim);

        try {
            // Clear the current `dc.date.issued` metadata
            itemService.clearMetadata(context, item, "dc", "date", "issued", Item.ANY);

            // Update the `dc.date.issued` metadata with a new value: `0000` or the last year from the sequence
            if (CollectionUtils.isNotEmpty(listOfYearValues) && isListOfNumbers(listOfYearValues)) {
                // Take the last year from the list of years and add it to the `dc.date.issued` metadata
                itemService.addMetadata(context, item, "dc", "date", "issued", Item.ANY,
                        getLastNumber(listOfYearValues));
            } else {
                // Add the `0000` value to the `dc.date.issued` metadata
                itemService.addMetadata(context, item, "dc", "date", "issued", Item.ANY, NO_YEAR);
            }
        } catch (SQLException e) {
            log.error("Cannot remove `dc.date.issued` metadata because: {}", e.getMessage());
        }
    }

    public static boolean isListOfNumbers(List<String> values) {
        for (String value : values) {
            if (!NumberUtils.isCreatable(value)) {
                return false;
            }
        }
        return true;
    }

    private static String getLastNumber(List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return NO_YEAR;
        }
        return values.get(values.size() - 1);
    }


}
