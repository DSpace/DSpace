/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.canvasdimension;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.canvasdimension.service.IIIFApiQueryService;
import org.dspace.app.canvasdimension.service.IIIFCanvasDimensionService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.license.CreativeCommonsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

public class IIIFCanvasDimensionServiceImpl implements IIIFCanvasDimensionService {

    @Autowired(required = true)
    ItemService itemService;
    @Autowired(required = true)
    CommunityService communityService;
    @Autowired(required = true)
    BitstreamService bitstreamService;
    @Autowired(required = true)
    DSpaceObjectService<Bitstream> dSpaceObjectService;
    @Autowired(required = true)
    IIIFApiQueryService iiifApiQuery;

    // metadata used to enable the iiif features on the item
    public static final String METADATA_IIIF_ENABLED = "dspace.iiif.enabled";
    private static final String IIIF_WIDTH_METADATA = "iiif.image.width";
    // The DSpace bundle for other content related to item.
    protected static final String OTHER_CONTENT_BUNDLE = "OtherContent";

    private boolean forceProcessing = false;
    private boolean isQuiet = false;
    private List<String> skipList = null;
    private int max2Process = Integer.MAX_VALUE;  // TODO no option for this yet.
    private int processed = 0;
    protected Item currentItem = null; // TODO needed?

    /**
     * Set the force processing property. If true, existing canvas
     * metadata will be replaced.
     * @param force
     */
    @Override
    public void setForceProcessing(boolean force) {
        forceProcessing = force;
    }

    @Override
    public void setIsQuiet(boolean quiet) {
        isQuiet = quiet;
    }

    /**
     * Set the maximum number of items to process.
     * @param max2Process
     */
    @Override
    public void setMax2Process(int max2Process) {
        this.max2Process = max2Process;
    }

    /**
     * Set dso identifiers to skip.
     * @param skipList
     */
    @Override
    public void setSkipList(List<String> skipList) {
        this.skipList = skipList;
    }

    /**
     * Set IIIF canvas dimensions on all IIIF items in the site.
     * @param context
     * @throws Exception
     */
    @Override
    public void processSite(Context context) throws Exception {
        if (skipList != null) {
            //if a skip-list exists, we need to filter community-by-community
            //so we can respect what is in the skip-list
            List<Community> topLevelCommunities = communityService.findAllTop(context);

            for (Community topLevelCommunity : topLevelCommunities) {
                processCommunity(context, topLevelCommunity);
            }
        } else {
            //otherwise, just find every item and process
            Iterator<Item> itemIterator = itemService.findAll(context);
            while (itemIterator.hasNext() && processed < max2Process) {
                processItem(context, itemIterator.next());
            }
        }
    }

    /**
     * Set IIIF canvas dimensions on all IIIF items in a community and its
     * sub-communities.
     * @param context
     * @param community
     * @throws Exception
     */
    @Override
    public void processCommunity(Context context, Community community) throws Exception {
        if (!inSkipList(community.getHandle())) {
            List<Community> subcommunities = community.getSubcommunities();
            for (Community subcommunity : subcommunities) {
                processCommunity(context, subcommunity);
            }
            List<Collection> collections = community.getCollections();
            for (Collection collection : collections) {
                processCollection(context, collection);
            }
        }
    }

    /**
     * Set IIIF canvas dimensions on all IIIF items in a collection.
     * @param context
     * @param collection
     * @throws Exception
     */
    @Override
    public void processCollection(Context context, Collection collection) throws Exception {
        if (!inSkipList(collection.getHandle())) {
            Iterator<Item> itemIterator = itemService.findAllByCollection(context, collection);
            while (itemIterator.hasNext() && processed < max2Process) {
                processItem(context, itemIterator.next());
            }
        }
    }

    /**
     * Set IIIF canvas dimensions for an item.
     * @param context
     * @param item
     * @throws Exception
     */
    @Override
    public void processItem(Context context, Item item) throws Exception {
        if (!inSkipList(item.getHandle())) {
            boolean isIIIFItem = item.getMetadata().stream().filter(m -> m.getMetadataField().toString('.')
                                                                          .contentEquals(METADATA_IIIF_ENABLED))
                                     .anyMatch(m -> m.getValue().equalsIgnoreCase("true") ||
                                         m.getValue().equalsIgnoreCase("yes"));
            if (isIIIFItem) {
                if (processItemBundles(context, item)) {
                    ++processed;
                    // commit changes
                    context.commit();
                }
            }
        }
    }

    /**
     * Process all IIIF bundles for an item.
     * @param context
     * @param item
     * @return
     * @throws Exception
     */
    private boolean processItemBundles(Context context, Item item) throws Exception {
        List<Bundle> bundles = getIIIFBundles(item);
        boolean done = false;
        for (Bundle bundle : bundles) {
            List<Bitstream> bitstreams = bundle.getBitstreams();
            for (Bitstream bit : bitstreams) {
                done |= processBitstream(context, bit);
            }
        }
        itemService.update(context, item);
        return done;

    }

    /**
     * Sets the IIIF height and width metadata for all images. If width metadata already exists,
     * the bitstream is processed only if forceProcessing is true.
     * @param context
     * @param bitstream
     * @return
     * @throws Exception
     */
    private boolean processBitstream(Context context, Bitstream bitstream) throws Exception {
        boolean processed = false;
        boolean isUnsupported = bitstream.getFormat(context).getMIMEType().contains("image/jp2");
        boolean isImage = bitstream.getFormat(context).getMIMEType().contains("image/");
        if (isImage) {
            Optional<MetadataValue> op = bitstream.getMetadata().stream()
                                                  .filter(m -> m.getMetadataField().toString('.')
                                                                .contentEquals(IIIF_WIDTH_METADATA)).findFirst();
            if (op.isEmpty() || forceProcessing) {
                int[] dims;
                if (isUnsupported) {
                    dims = iiifApiQuery.getImageDimensions(bitstream);
                } else {
                    InputStream stream = bitstreamService.retrieve(context, bitstream);
                    dims = ImageDimensionReader.getImageDimensions(stream);
                }
                if (dims != null) {
                    processed = setBitstreamMetadata(context, bitstream, dims);
                    bitstreamService.update(context, bitstream);
                }
            }
        }
        return processed;
    }

    private boolean setBitstreamMetadata(Context context, Bitstream bitstream, int[] dims) {
        try {
            dSpaceObjectService.clearMetadata(context, bitstream, "iiif",
                "image", "width", Item.ANY);
            dSpaceObjectService.setMetadataSingleValue(context, bitstream, "iiif",
                "image", "width", Item.ANY, String.valueOf(dims[0]));
            dSpaceObjectService.clearMetadata(context, bitstream, "iiif",
                "image", "height", Item.ANY);
            dSpaceObjectService.setMetadataSingleValue(context, bitstream, "iiif",
                "image", "height", Item.ANY, String.valueOf(dims[1]));
            return true;
        } catch (SQLException e) {
            System.out.println("Unable to update metadata: " + e.getMessage());
            return false;
        }
    }

    private boolean inSkipList(String identifier) {
        if (skipList != null && skipList.contains(identifier)) {
            if (!isQuiet) {
                System.out.println("SKIP-LIST: skipped bitstreams within identifier " + identifier);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * This method returns the bundles holding IIIF resources if any.
     * If there is no IIIF content available an empty bundle list is returned.
     * @param item the DSpace item
     *
     * @return list of DSpace bundles with IIIF content
     */
    private List<Bundle> getIIIFBundles(Item item) {
        boolean iiif = isIIIFEnabled(item);
        List<Bundle> bundles = new ArrayList<>();
        if (iiif) {
            bundles = item.getBundles().stream().filter(b -> isIIIFBundle(b)).collect(Collectors.toList());
        }
        return bundles;
    }

    /**
     * This method verify if the IIIF feature is enabled on the item or parent collection.
     *
     * @param item the dspace item
     * @return true if the item supports IIIF
     */
    private boolean isIIIFEnabled(Item item) {
        return item.getOwningCollection().getMetadata().stream()
                   .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_ENABLED))
                   .anyMatch(m -> m.getValue().equalsIgnoreCase("true") ||
                       m.getValue().equalsIgnoreCase("yes"))
            || item.getMetadata().stream()
                   .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_ENABLED))
                   .anyMatch(m -> m.getValue().equalsIgnoreCase("true")  ||
                       m.getValue().equalsIgnoreCase("yes"));
    }

    /**
     * Utility method to check is a bundle can contain bitstreams to use as IIIF
     * resources
     *
     * @param b the DSpace bundle to check
     * @return true if the bundle can contain bitstreams to use as IIIF resources
     */
    private boolean isIIIFBundle(Bundle b) {
        return !StringUtils.equalsAnyIgnoreCase(b.getName(), Constants.LICENSE_BUNDLE_NAME,
            Constants.METADATA_BUNDLE_NAME, CreativeCommonsServiceImpl.CC_BUNDLE_NAME, "THUMBNAIL",
            "BRANDED_PREVIEW", "TEXT", OTHER_CONTENT_BUNDLE)
            && b.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_ENABLED))
                .noneMatch(m -> m.getValue().equalsIgnoreCase("false") || m.getValue().equalsIgnoreCase("no"));
    }

}
