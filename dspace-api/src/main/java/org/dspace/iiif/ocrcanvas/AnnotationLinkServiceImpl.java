/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif.ocrcanvas;

import static org.dspace.iiif.util.IIIFSharedUtils.getIIIFBundles;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.core.Context;
import org.dspace.iiif.ocrcanvas.service.AnnotationLinkService;
import org.dspace.iiif.util.IIIFSharedUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class AnnotationLinkServiceImpl implements AnnotationLinkService {

    @Autowired
    BitstreamService bitstreamService;

    @Autowired()
    ItemService itemService;

    @Autowired(required = true)
    MetadataFieldService metadataFieldService;

    @Autowired
    MetadataSchemaService metadataSchemaService;

    @Autowired
    MetadataValueService metadataValueService;

    private int processed = 0;

    private boolean replace = false;
    private boolean delete = false;

    private static final String METADATA_CANVASID_SCHEMA = "bitstream";
    private static final String METADATA_CANVASID_ELEMENT = "iiif";
    private static final String METADATA_CANVASID_QUALIFIER = "canvasid";
    private static final String METADATA_CANVASID_FIELD = METADATA_CANVASID_SCHEMA + "." +
        METADATA_CANVASID_ELEMENT + "."  + METADATA_CANVASID_QUALIFIER;

    @Override
    public int processCommunity(Context context, Community community) throws Exception {
        List<Community> subcommunities = community.getSubcommunities();
        for (Community subcommunity : subcommunities) {
            processCommunity(context, subcommunity);
        }
        List<Collection> collections = community.getCollections();
        for (Collection collection : collections) {
            processCollection(context, collection);
        }
        return processed;
    }

    @Override
    public int processCollection(Context context, Collection collection) throws Exception {
        Iterator<Item> itemIterator = itemService.findAllByCollection(context, collection);
        while (itemIterator.hasNext()) {
            processItem(context, itemIterator.next());
        }
        return processed;
    }

    @Override
    public void processItem(Context context, Item item) throws Exception {
        boolean isIIIFItem = IIIFSharedUtils.isIIIFItem(item);
        if (isIIIFItem) {
            if (processIIIFItem(context, item)) {
                ++processed;
            }
            context.uncacheEntity(item);
        }
    }

    public void setReplaceAction(boolean replace) {
        this.replace = replace;
    }

    @Override
    public void setDeleteAction(boolean delete) {
        this.delete = delete;
    }

    private boolean processIIIFItem(Context context, Item item) throws Exception {
        List<Bundle> otherContent = item.getBundles("OtherContent");
        if (otherContent.size() == 0) {
            System.out.println("ERROR: The OtherContent bundle was not found.");
            return false;
        }

        List<Bitstream> otherContentFiles = otherContent.get(0).getBitstreams();
        List<Bitstream> ocrFiles = new ArrayList<>();
        for (Bitstream bit: otherContentFiles) {
            if (bit.getFormat(context).getMIMEType().contains("text/")) {
                if (isOcrFormat(context, bit)) {
                    ocrFiles.add(bit);
                }
            }
        }

        // If the delete option is true remove canvasid metadata and return.
        if (delete) {
            for (Bitstream ocr: ocrFiles) {
                if (hasCanvasId(ocr.getMetadata())) {
                    removeCanvasId(context, ocr);
                }
            }
            System.out.println("Deleted canvasid metadata from OCR files for the item: " + item.getID() + "\n");
            return true;
        }

        List<Bundle> imageBundles = getIIIFBundles(item);
        List<Bitstream> iiifImageBitstreams = new ArrayList<>();
        for (Bundle bundle: imageBundles) {
            List<Bitstream> bits = bundle.getBitstreams();
            for (Bitstream bit : bits) {
                if (bit.getFormat(context).getMIMEType().contains("image/")) {
                    iiifImageBitstreams.add(bit);
                }
            }
        }

        if (iiifImageBitstreams.size() != ocrFiles.size()) {
            System.out.println("Cannot set canvas IDs for the item " + item.getID() + ". The image " +
                "bitstream and ocr bitstream counts do not match.");
            return false;
        }

        MetadataField metadataField = metadataFieldService
            .findByString(context, METADATA_CANVASID_FIELD, '.');

        for (int i = 0; i < iiifImageBitstreams.size(); i++) {
            if (hasCanvasId(ocrFiles.get(i).getMetadata())) {
                if (replace) {
                    removeCanvasId(context, ocrFiles.get(i));
                } else {
                    System.out.println("Found existing metadata for " + ocrFiles.get(i).getID() + ". Skipping.");
                    return false;
                }
            }
            UUID bitstreamId = iiifImageBitstreams.get(i).getID();
            bitstreamService.addMetadata(context, ocrFiles.get(i), metadataField, null, bitstreamId.toString());
        }
        String action = "Added";
        if (replace) {
            action = "Replaced";
        }
        System.out.println(action + " canvasid metadata to OCR files for the item: " + item.getID() + "\n");
        return true;
    }

    /**
     * Checks to see if the OCR bitstream metadata already includes a canvasid.
     * @param metadataValues
     * @return
     */
    private boolean hasCanvasId(List<MetadataValue> metadataValues) {
        for (MetadataValue meta: metadataValues) {
            String element = meta.getMetadataField().getElement();
            String qualifier = meta.getMetadataField().getQualifier();
            if (element.contains(METADATA_CANVASID_ELEMENT) && qualifier.contains(METADATA_CANVASID_QUALIFIER)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves file content and verifies that this is an OCR file.
     * @param context
     * @param bitstream
     * @return
     * @throws IOException
     */
    private boolean isOcrFormat(Context context, Bitstream bitstream) throws IOException {
        InputStream is;
        try {
            is = bitstreamService.retrieve(context, bitstream);
        } catch (IOException | SQLException | AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (is != null) {
            String content = IOUtils.toString(is, StandardCharsets.UTF_8);
            return checkFormat(content);
        }
        return false;
    }

    /**
     * Checks for OCR file. It should match on ALTO, hOCR, and MiniOcr files.
     * @param content the ocr file content.
     * @return
     */
    private boolean checkFormat(String content) {
        boolean isOcr = false;
        // alto
        if (content.contains("<alto ")) {
            return true;
        }
        // hocr
        if (content.contains("ocr_document") || content.contains("ocr_page") || content.contains("ocr_par") ||
            content.contains("ocr_line") || content.contains("ocrx_word")) {
            return true;
        }
        // miniocr ( unique format that can be used with https://dbmdz.github.io/solr-ocrhighlighting/0.8.3/ )
        if (content.contains("<ocr>")) {
            return true;
        }
        return isOcr;

    }

    /**
     * Removes the canvasid metadata field from the OCR bitstream.
     * @param context
     * @param ocrFile the OCR bitstream
     * @throws SQLException
     */
    private void removeCanvasId(Context context, Bitstream ocrFile) throws SQLException {
        bitstreamService.clearMetadata(context, ocrFile, METADATA_CANVASID_SCHEMA, METADATA_CANVASID_ELEMENT,
            METADATA_CANVASID_QUALIFIER, null);
    }

}
