/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.service.util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.iiif.model.ObjectMapperFactory;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.license.CreativeCommonsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IIIFUtils {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(IIIFUtils.class);

    // The canvas position will be appended to this string.
    private static final String CANVAS_PATH_BASE = "/canvas/c";

    public static final String METADATA_IIIF_ENABLED = "dspace.iiif.enabled";
    public static final String METADATA_IIIF_SEARCH_ENABLED = "iiif.search.enabled";
    public static final String METADATA_IIIF_LABEL = "iiif.label";
    public static final String METADATA_IIIF_DESCRIPTION = "iiif.description";
    public static final String METADATA_IIIF_TOC = "iiif.toc";
    public static final String METADATA_IIIF_VIEWING_HINT  = "iiif.viewing.hint";
    public static final String METADATA_IMAGE_WIDTH = "iiif.image.width";
    public static final String METADATA_IMAGE_HEIGTH = "iiif.image.heigth";

    public static final String TOC_SEPARATOR = "|||";
    public static final String TOC_SEPARATOR_REGEX = "\\|\\|\\|";

    // get module subclass.
    protected SimpleModule iiifModule = ObjectMapperFactory.getIiifModule();
    // Use the object mapper subclass.
    protected ObjectMapper mapper = ObjectMapperFactory.getIiifObjectMapper();

    @Autowired
    protected BitstreamService bitstreamService;

    /**
     * This method returns the bundles holding IIIF resources if any.
     * If there is no IIIF content available an empty bundle list is returned.
     * @param item the DSpace item
     * 
     * @return list of DSpace bundles with IIIF content
     */
    public List<Bundle> getIiifBundles(Item item) {
        boolean iiif = isIIIFEnabled(item);
        List<Bundle> bundles = new ArrayList<>();
        if (iiif) {
            bundles = item.getBundles().stream().filter(b -> isIIIFBundle(b)).collect(Collectors.toList());
        }
        return bundles;
    }

    public boolean isIIIFEnabled(Item item) {
        return item.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_ENABLED))
                .anyMatch(m -> m.getValue().equalsIgnoreCase("true")  ||
                        m.getValue().equalsIgnoreCase("yes"));
    }

    private boolean isIIIFBundle(Bundle b) {
        return !StringUtils.equalsAnyIgnoreCase(b.getName(), Constants.LICENSE_BUNDLE_NAME,
                Constants.METADATA_BUNDLE_NAME, CreativeCommonsServiceImpl.CC_BUNDLE_NAME, "THUMBNAIL",
                "BRANDED_PREVIEW", "TEXT")
                && b.getMetadata().stream()
                        .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_ENABLED))
                        .noneMatch(m -> m.getValue().equalsIgnoreCase("false") || m.getValue().equalsIgnoreCase("no"));
    }

    public List<Bitstream> getIiifBitstreams(Context context, Item item) {
        List<Bitstream> bitstreams = new ArrayList<Bitstream>();
        for (Bundle bnd : getIiifBundles(item)) {
            bitstreams
                    .addAll(getIiifBitstreams(context, bnd));
        }
        return bitstreams;
    }

    public List<Bitstream> getIiifBitstreams(Context context, Bundle bundle) {
        return bundle.getBitstreams().stream().filter(b -> isIiifBitstream(context, b))
                .collect(Collectors.toList());
    }

    private boolean isIiifBitstream(Context context, Bitstream b) {
        return checkImageMimeType(getBitstreamMimeType(b, context)) && b.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_ENABLED))
                .noneMatch(m -> m.getValue().equalsIgnoreCase("false") || m.getValue().equalsIgnoreCase("no"));
    }

    /**
     * Returns the bitstream mime type
     * @param bitstream DSpace bitstream
     * @param context DSpace context
     * @return mime type
     */
    public String getBitstreamMimeType(Bitstream bitstream, Context context) {
        try {
            BitstreamFormat bitstreamFormat = bitstream.getFormat(context);
            return bitstreamFormat.getMIMEType();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Checks to see if the item is searchable. Based on the {@link #METADATA_IIIF_SEARCH_ENABLED} metadata.
     * @param item DSpace item
     * @return true if the iiif search is enabled
     */
    public boolean isSearchable(Item item) {
        return item.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString('.').contentEquals("iiif.search.enabled"))
                .anyMatch(m -> m.getValue().equalsIgnoreCase("true")  ||
                        m.getValue().equalsIgnoreCase("yes"));
    }

    /**
     * Retrives a bitstream based on its position in the IIIF bundle.
     * @param context DSpace Context
     * @param item DSpace Item
     * @param canvasPosition bitstream position
     * @return bitstream
     */
    public Bitstream getBitstreamForCanvas(Context context, Item item, int canvasPosition) {
        List<Bitstream> bitstreams = getIiifBitstreams(context, item);
        try {
            return bitstreams.size() > canvasPosition ? bitstreams.get(canvasPosition) : null;
        } catch (RuntimeException e) {
            throw new RuntimeException("The requested canvas is not available", e);
        }
    }

    /**
     * Extracts canvas position from the URL input path.
     * @param canvasId e.g. "c12"
     * @return the position, e.g. 12
     */
    public int getCanvasId(String canvasId) {
        return Integer.parseInt(canvasId.substring(1));
    }

    /**
     * Returns the canvas path with position. The path
     * returned is partial, not the fully qualified URI.
     * @param position position of the bitstream in the DSpace bundle.
     * @return partial canvas path.
     */
    public String getCanvasId(int position) {
        return CANVAS_PATH_BASE + position;
    }

    /**
     * Serializes the json response.
     * @param resource to be serialized
     * @return
     */
    public String asJson(Resource<?> resource) {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.registerModule(iiifModule);
        try {
            return mapper.writeValueAsString(resource);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Tests for image mimetype. Presentation API 2.1.1 canvas supports images only.
     * Other media types introduced in version 3.
     * @param mimetype
     * @return true if an image
     */
    public boolean checkImageMimeType(String mimetype) {
        if (mimetype != null && mimetype.contains("image/")) {
            return true;
        }
        return false;
    }

    public List<Bitstream> getSeeAlsoBitstreams(Item item) {
        return new ArrayList<Bitstream>();
    }

    public String getIIIFLabel(DSpaceObject dso, String defaultLabel) {
        return dso.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_LABEL))
                .findFirst().map(m -> m.getValue()).orElse(defaultLabel);
    }

    public String getIIIFDescription(DSpaceObject dso, String defaultDescription) {
        return dso.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_DESCRIPTION))
                .findFirst().map(m -> m.getValue()).orElse(defaultDescription);
    }

    public List<String> getIIIFToCs(DSpaceObject dso, String prefix) {
        List<String> tocs = dso.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_TOC))
                .map(m -> StringUtils.isNotBlank(prefix) ? prefix + TOC_SEPARATOR + m.getValue() : m.getValue())
                .collect(Collectors.toList());
        if (tocs.size() == 0 && StringUtils.isNotBlank(prefix)) {
            return List.of(prefix);
        } else {
            return tocs;
        }
    }

    public String getIIIFFirstToC(DSpaceObject dso) {
        return dso.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_TOC))
                .findFirst().map(m -> m.getValue()).orElse(null);
    }

    public String getIIIFViewingHint(Item item, String defaultHint) {
        return item.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_VIEWING_HINT))
                .findFirst().map(m -> m.getValue()).orElse(defaultHint);
    }

    public int getCanvasWidth(Bitstream bitstream, Bundle bundle, Item item, int defaultWidth) {
        return getSizeFromMetadata(bitstream, METADATA_IMAGE_WIDTH,
                    getSizeFromMetadata(bundle, METADATA_IMAGE_WIDTH,
                        getSizeFromMetadata(item, METADATA_IMAGE_WIDTH, defaultWidth)));
    }

    public int getCanvasHeight(Bitstream bitstream, Bundle bundle, Item item, int defaultHeight) {
        return getSizeFromMetadata(bitstream, METADATA_IMAGE_HEIGTH,
                getSizeFromMetadata(bundle, METADATA_IMAGE_HEIGTH,
                    getSizeFromMetadata(item, METADATA_IMAGE_HEIGTH, defaultHeight)));
    }

    private int getSizeFromMetadata(DSpaceObject dso, String metadata, int defaultValue) {
        return dso.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString('.').contentEquals(metadata))
                .findFirst().map(m -> castToInt(m, defaultValue)).orElse(defaultValue);
    }

    private int castToInt(MetadataValue m, int defaultWidth) {
        try {
            Integer.parseInt(m.getValue());
        } catch (NumberFormatException e) {
            log.error("Error parsing " + m.getMetadataField().toString('.') + " of " + m.getDSpaceObject().getID()
                    + " the value " + m.getValue() + " is not an integer. Returning the default.");
        }
        return defaultWidth;
    }

}
