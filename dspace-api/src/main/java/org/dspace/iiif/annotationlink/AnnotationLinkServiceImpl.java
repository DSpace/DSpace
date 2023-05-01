package org.dspace.iiif.annotationlink;

import static org.dspace.iiif.util.IIIFSharedUtils.getIIIFBundles;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import org.apache.commons.io.IOUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.core.Context;
import org.dspace.iiif.annotationlink.service.AnnotationLinkService;
import org.springframework.beans.factory.annotation.Autowired;

public class AnnotationLinkServiceImpl implements AnnotationLinkService {

    @Autowired
    BitstreamService bitstreamService;

    @Autowired(required = true)
    MetadataFieldService metadataFieldService;

    @Autowired
    MetadataSchemaService metadataSchemaService;

    @Autowired
    MetadataValueService metadataValueService;

    private static final String METADATA_CANVASID_SCHEMA = "bitstream";
    private static final String METADATA_CANVASID_ELEMENT = "iiif";
    private static final String METADATA_CANVASID_QUALIFIER = "canvasid";
    private static final String METADATA_CANVASID_FIELD = METADATA_CANVASID_SCHEMA + "." +
        METADATA_CANVASID_ELEMENT + "."  + METADATA_CANVASID_QUALIFIER;

    @Override
    public int processCommunity(Context context, Community community) throws Exception {
        return 0;
    }

    @Override
    public int processCollection(Context context, Collection collection) throws Exception {
        return 0;
    }

    @Override
    public void processItem(Context context, Item item) throws Exception {
        List<Bundle> otherContent = item.getBundles("OtherContent");
        if (otherContent.size() == 0) {
            System.out.println("ERROR: The OtherContent bundle was not found.");
            return;
        }

        List<Bitstream> otherContentFiles = otherContent.get(0).getBitstreams();
        List<Bitstream> ocrFiles = new ArrayList<>();
        for (Bitstream bit: otherContentFiles) {
            if (isOcrFormat(context, bit)) {
                ocrFiles.add(bit);
            }
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
            System.out.println("Cannot add canvas ids for the item " + item.getID() + ". The image " +
                "bitstream and ocr bitstream counts for the item do not match.");
            return;
        }

        MetadataField metadataField = metadataFieldService
            .findByString(context, METADATA_CANVASID_FIELD, '.');

        for (int i = 0; i < iiifImageBitstreams.size(); i++) {
            if (hasCanvasId(ocrFiles.get(i).getMetadata())) {
                System.out.println("Found existing metadata for " + ocrFiles.get(i).getID() + ". Skipping.");
                return;
            }
            UUID bitstreamId = iiifImageBitstreams.get(i).getID();
            bitstreamService.addMetadata(context, ocrFiles.get(i), metadataField, null, bitstreamId.toString());
            System.out.println("Added canvasid metadata to the OCR bitstream: " + ocrFiles.get(i).getID());
        }
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
     * Retrieves file content and verifies that is is an OCR file.
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
        if (content.contains("<alto ")) {
            return true;
        }
        if (content.contains("ocr_document")) {
            return true;
        }
        if (content.contains("<ocr>")) {
            return true;
        }
        return isOcr;

    }


}
