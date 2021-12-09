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
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.license.CreativeCommonsServiceImpl;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

public class IIIFCanvasDimensionProcessor implements InitializingBean {

    @Autowired
    ItemService itemService;
    @Autowired
    CommunityService communityService;
    @Autowired
    BitstreamService bitstreamService;
    @Autowired
    DSpaceObjectService<Bitstream> dSpaceObjectService;

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

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    public void setForceProcessing(boolean force) {
        forceProcessing = force;
    }

    public void setMax2Process(int max2Process) {
        this.max2Process = max2Process;
    }

    public void setSkipList(List<String> skipList) {
        this.skipList = skipList;
    }

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

    public void processCollection(Context context, Collection collection) throws Exception {
        if (!inSkipList(collection.getHandle())) {
            Iterator<Item> itemIterator = itemService.findAllByCollection(context, collection);
            while (itemIterator.hasNext() && processed < max2Process) {
                processItem(context, itemIterator.next());
            }
        }
    }

    public void processItem(Context context, Item item) throws Exception {
        if (!inSkipList(item.getHandle())) {
            boolean isIIIFItem = item.getMetadata().stream().filter(m -> m.getMetadataField().toString('.')
                                                                          .contentEquals(METADATA_IIIF_ENABLED))
                                     .anyMatch(m -> m.getValue().equalsIgnoreCase("true") ||
                                         m.getValue().equalsIgnoreCase("yes"));
            if (isIIIFItem) {
                if (processBundles(context, item)) {
                    ++processed;
                }
            }
        }
    }

    private boolean processBundles(Context context, Item item) throws Exception {
        List<Bundle> bundles = getIIIFBundles(item);
        boolean done = false;
        for (Bundle bundle : bundles) {
            List<Bitstream> myBitstreams = bundle.getBitstreams();
            for (Bitstream myBitstream : myBitstreams) {
                done |= processBitstream(context, item, myBitstream);
            }
        }
        return done;

    }

    private boolean processBitstream(Context context, Item item, Bitstream bitstream) throws Exception {
        boolean processed = false;
        boolean isImage = bitstream.getFormat(context).getMIMEType().contains("image/");
        if (isImage) {
            Optional op = bitstream.getMetadata().stream().filter(m -> m.getMetadataField().toString('.')
                                                                        .contentEquals(IIIF_WIDTH_METADATA)).findFirst();
            if (op.isEmpty() || forceProcessing) {
                InputStream srcStream = bitstreamService.retrieve(context, bitstream);
                int[] dims = ImageDimensionReader.getImageDimensions(srcStream);
                if (dims != null) {
                    processed = setBitstreamMetadata(context, bitstream, dims);
                }
            }
        }
        return processed;
    }

    private boolean setBitstreamMetadata(Context context, Bitstream bitstream, int[] dims) {
        try {
            dSpaceObjectService.clearMetadata(context, bitstream, "iiif",
                "image", "width", Item.ANY);
            dSpaceObjectService.addAndShiftRightMetadata(context, bitstream, "iiif",
                "image", "width", null,
                String.valueOf(dims[0]), null, -1, -1);
            dSpaceObjectService.clearMetadata(context, bitstream, "iiif",
                "image", "height", Item.ANY);
            dSpaceObjectService.addAndShiftRightMetadata(context, bitstream, "iiif",
                "image", "height", null,
                String.valueOf(dims[1]), null, -1, -1);
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
