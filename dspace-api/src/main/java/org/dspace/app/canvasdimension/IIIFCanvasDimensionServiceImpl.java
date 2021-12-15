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
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

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
import org.dspace.core.Context;
import org.dspace.iiif.Utils;
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

    // field used to check for existing bitstream metadata
    private static final String IIIF_WIDTH_METADATA = "iiif.image.width";

    private boolean forceProcessing = false;
    private boolean isQuiet = false;
    private List<String> skipList = null;
    private int max2Process = Integer.MAX_VALUE;
    private int processed = 0;

    /**
     * Set the force processing property. If true, existing canvas
     * metadata will be replaced.
     * @param force
     */
    @Override
    public void setForceProcessing(boolean force) {
        forceProcessing = force;
    }

    /**
     * Set whether to output messages during processing.
     * @param quiet
     */
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
            boolean isIIIFItem = Utils.isIIIFItem(item);
            if (isIIIFItem) {
                if (processItemBundles(context, item)) {
                    ++processed;
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
        List<Bundle> bundles = Utils.getIIIFBundles(item);
        boolean done = false;
        for (Bundle bundle : bundles) {
            List<Bitstream> bitstreams = bundle.getBitstreams();
            for (Bitstream bit : bitstreams) {
                done |= processBitstream(context, bit);
            }
        }
        if (done) {
            // update the item
            // itemService.update(context, item);
            if (!isQuiet) {
                System.out.println("Updated canvas metadata for item: " + item.getID());
            }
        }
        return done;

    }

    /**
     * Gets image height and width for the bitstream. For jp2 images, height and width are
     * obtained from the IIIF image server. For other formats supported by ImageIO these values
     * are read from the actual DSpace bitstream content. If bitstream width metadata already exists,
     * the bitstream is processed when forceProcessing is true.
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
                if (forceProcessing && !isQuiet) {
                    System.out.println("Force processing for bitstream: " + bitstream.getID());
                }
                int[] dims;
                if (isUnsupported) {
                    dims = iiifApiQuery.getImageDimensions(bitstream);
                } else {
                    InputStream stream = bitstreamService.retrieve(context, bitstream);
                    dims = ImageDimensionReader.getImageDimensions(stream);
                }
                if (dims != null) {
                    processed = setBitstreamMetadata(context, bitstream, dims);
                    // update the bitstream
                    bitstreamService.update(context, bitstream);
                }
            }
        }
        return processed;
    }

    /**
     * Sets bitstream metadata for "iiif.image.width" and "iiif.image.height".
     * @param context
     * @param bitstream
     * @param dims
     * @return
     */
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
            if (!isQuiet) {
                System.out.println("Added IIIF canvas metadata to bitstream: " + bitstream.getID());
            }
            return true;
        } catch (SQLException e) {
            System.out.println("Unable to update metadata: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tests whether the identifier is in the skip list.
     * @param identifier
     * @return
     */
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

}
