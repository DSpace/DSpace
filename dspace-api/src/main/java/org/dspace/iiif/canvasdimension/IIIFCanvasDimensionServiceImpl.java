/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif.canvasdimension;

import static org.dspace.iiif.util.IIIFSharedUtils.METADATA_IIIF_HEIGHT_QUALIFIER;
import static org.dspace.iiif.util.IIIFSharedUtils.METADATA_IIIF_IMAGE_ELEMENT;
import static org.dspace.iiif.util.IIIFSharedUtils.METADATA_IIIF_SCHEMA;
import static org.dspace.iiif.util.IIIFSharedUtils.METADATA_IIIF_WIDTH_QUALIFIER;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.dspace.authorize.AuthorizeException;
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
import org.dspace.iiif.IIIFApiQueryService;
import org.dspace.iiif.canvasdimension.service.IIIFCanvasDimensionService;
import org.dspace.iiif.util.IIIFSharedUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This service sets canvas dimensions for bitstreams. Processes communities,
 * collections, and individual items.
 *
 * @author Michael Spalti mspalti@willamette.edu
 */
public class IIIFCanvasDimensionServiceImpl implements IIIFCanvasDimensionService {

    @Autowired()
    ItemService itemService;
    @Autowired()
    CommunityService communityService;
    @Autowired()
    BitstreamService bitstreamService;
    @Autowired()
    DSpaceObjectService<Bitstream> dSpaceObjectService;
    @Autowired()
    IIIFApiQueryService iiifApiQuery;

    private boolean forceProcessing = false;
    private boolean isQuiet = false;
    private List<String> skipList = null;
    private int max2Process = Integer.MAX_VALUE;
    private int processed = 0;

    // used to check for existing canvas dimension
    private static final String IIIF_WIDTH_METADATA = METADATA_IIIF_SCHEMA + "." + METADATA_IIIF_IMAGE_ELEMENT +
        "." + METADATA_IIIF_WIDTH_QUALIFIER;

    @Override
    public void setForceProcessing(boolean force) {
        forceProcessing = force;
    }

    @Override
    public void setIsQuiet(boolean quiet) {
        isQuiet = quiet;
    }

    @Override
    public void setMax2Process(int max2Process) {
        this.max2Process = max2Process;
    }

    @Override
    public void setSkipList(List<String> skipList) {
        this.skipList = skipList;
    }

    @Override
    public int processCommunity(Context context, Community community) throws Exception {
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
        return processed;
    }

    @Override
    public int processCollection(Context context, Collection collection) throws Exception {
        if (!inSkipList(collection.getHandle())) {
            Iterator<Item> itemIterator = itemService.findAllByCollection(context, collection);
            while (itemIterator.hasNext() && processed < max2Process) {
                processItem(context, itemIterator.next());
            }
        }
        return processed;
    }

    @Override
    public void processItem(Context context, Item item) throws Exception {
        if (!inSkipList(item.getHandle())) {
            boolean isIIIFItem = IIIFSharedUtils.isIIIFItem(item);
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
        List<Bundle> bundles = IIIFSharedUtils.getIIIFBundles(item);
        boolean done = false;
        for (Bundle bundle : bundles) {
            List<Bitstream> bitstreams = bundle.getBitstreams();
            for (Bitstream bit : bitstreams) {
                done |= processBitstream(context, bit);
            }
        }
        if (done) {
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
    private boolean processBitstream(Context context, Bitstream bitstream) throws SQLException, AuthorizeException,
        IOException {

        boolean processed = false;
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
                InputStream stream = null;
                try {
                    stream = bitstreamService.retrieve(context, bitstream);
                    try {
                        dims = ImageDimensionReader.getImageDimensions(stream);
                        if (dims == null) {
                            // If image dimensions are not available try the iiif image server.
                            dims = iiifApiQuery.getImageDimensions(bitstream);
                        }
                    } catch (IOException e) {
                        // If an exception was raised by ImageIO, try the iiif image server.
                        dims = iiifApiQuery.getImageDimensions(bitstream);
                    }
                } finally {
                    if (stream != null) {
                        stream.close();
                    }
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
    private boolean setBitstreamMetadata(Context context, Bitstream bitstream, int[] dims) throws SQLException {
        dSpaceObjectService.clearMetadata(context, bitstream, METADATA_IIIF_SCHEMA,
            METADATA_IIIF_IMAGE_ELEMENT, METADATA_IIIF_WIDTH_QUALIFIER, Item.ANY);
        dSpaceObjectService.setMetadataSingleValue(context, bitstream, METADATA_IIIF_SCHEMA,
            METADATA_IIIF_IMAGE_ELEMENT, METADATA_IIIF_WIDTH_QUALIFIER, null, String.valueOf(dims[0]));
        dSpaceObjectService.clearMetadata(context, bitstream, METADATA_IIIF_SCHEMA,
            METADATA_IIIF_IMAGE_ELEMENT, METADATA_IIIF_HEIGHT_QUALIFIER, Item.ANY);
        dSpaceObjectService.setMetadataSingleValue(context, bitstream, METADATA_IIIF_SCHEMA,
            METADATA_IIIF_IMAGE_ELEMENT, METADATA_IIIF_HEIGHT_QUALIFIER, null, String.valueOf(dims[1]));
        if (!isQuiet) {
            System.out.println("Added IIIF canvas metadata to bitstream: " + bitstream.getID());
        }
        return true;
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
