/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.service.utils;

import static org.dspace.iiif.util.IIIFSharedUtils.METADATA_IIIF_HEIGHT_QUALIFIER;
import static org.dspace.iiif.util.IIIFSharedUtils.METADATA_IIIF_IMAGE_ELEMENT;
import static org.dspace.iiif.util.IIIFSharedUtils.METADATA_IIIF_SCHEMA;
import static org.dspace.iiif.util.IIIFSharedUtils.METADATA_IIIF_WIDTH_QUALIFIER;

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
import org.dspace.app.iiif.model.ObjectMapperFactory;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.iiif.IIIFApiQueryService;
import org.dspace.iiif.util.IIIFSharedUtils;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class IIIFUtils {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(IIIFUtils.class);

    // The DSpace bundle for other content related to item.
    protected static final String OTHER_CONTENT_BUNDLE = "OtherContent";

    // The canvas position will be appended to this string.
    private static final String CANVAS_PATH_BASE = "/canvas/c";

    // metadata used to enable the iiif features on the item
    public static final String METADATA_IIIF_ENABLED = "dspace.iiif.enabled";
    // metadata used to enable the iiif search service on the item
    public static final String METADATA_IIIF_SEARCH_ENABLED = "iiif.search.enabled";
    // metadata used to override the title/name exposed as label to iiif client
    public static final String METADATA_IIIF_LABEL = "iiif.label";
    // metadata used to override the description/abstract exposed as label to iiif client
    public static final String METADATA_IIIF_DESCRIPTION = "iiif.description";
    // metadata used to set the position of the resource in the iiif manifest structure
    public static final String METADATA_IIIF_TOC = "iiif.toc";
    // metadata used to set the naming convention (prefix) used for all canvas that has not an explicit name
    public static final String METADATA_IIIF_CANVAS_NAMING = "iiif.canvas.naming";
    // metadata used to set the iiif viewing hint
    public static final String METADATA_IIIF_VIEWING_HINT  = "iiif.viewing.hint";
    // metadata used to set the width of the canvas that has not an explicit name
    public static final String METADATA_IMAGE_WIDTH = METADATA_IIIF_SCHEMA + "." + METADATA_IIIF_IMAGE_ELEMENT
        + "." + METADATA_IIIF_WIDTH_QUALIFIER;
    // metadata used to set the height of the canvas that has not an explicit name
    public static final String METADATA_IMAGE_HEIGHT = METADATA_IIIF_SCHEMA + "." + METADATA_IIIF_IMAGE_ELEMENT
        + "." + METADATA_IIIF_HEIGHT_QUALIFIER;

    // string used in the metadata toc as separator among the different levels
    public static final String TOC_SEPARATOR = "|||";
    // convenient constant to split a toc in its components
    public static final String TOC_SEPARATOR_REGEX = "\\|\\|\\|";

    // get module subclass.
    protected SimpleModule iiifModule = ObjectMapperFactory.getIiifModule();
    // Use the object mapper subclass.
    protected ObjectMapper mapper = ObjectMapperFactory.getIiifObjectMapper();

    @Autowired
    protected BitstreamService bitstreamService;

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    IIIFApiQueryService iiifApiQueryService;


    public List<Bundle> getIIIFBundles(Item item) {
        return IIIFSharedUtils.getIIIFBundles(item);
    }

    public boolean isIIIFEnabled(Item item) {
        return IIIFSharedUtils.isIIIFEnabled(item);
    }

    /**
     * Return all the bitstreams in the item to be used as IIIF resources
     *
     * @param context the DSpace Context
     * @param item    the DSpace item
     * @return a not null list of bitstreams to use as IIIF resources in the
     *         manifest
     */
    public List<Bitstream> getIIIFBitstreams(Context context, Item item) {
        List<Bitstream> bitstreams = new ArrayList<Bitstream>();
        for (Bundle bnd : IIIFSharedUtils.getIIIFBundles(item)) {
            bitstreams
                    .addAll(getIIIFBitstreams(context, bnd));
        }
        return bitstreams;
    }

    /**
     * Return all the bitstreams in the bundle to be used as IIIF resources
     *
     * @param context the DSpace Context
     * @param bundle    the DSpace Bundle
     * @return a not null list of bitstreams to use as IIIF resources in the
     *         manifest
     */
    public List<Bitstream> getIIIFBitstreams(Context context, Bundle bundle) {
        return bundle.getBitstreams().stream().filter(b -> isIIIFBitstream(context, b))
                .collect(Collectors.toList());
    }

    /**
     * Utility method to check is a bitstream can be used as IIIF resources
     *
     * @param b the DSpace bitstream to check
     * @return true if the bitstream can be used as IIIF resource
     */
    private boolean isIIIFBitstream(Context context, Bitstream b) {
        return checkImageMimeType(getBitstreamMimeType(b, context)) && b.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_ENABLED))
                .noneMatch(m -> m.getValue().equalsIgnoreCase("false") || m.getValue().equalsIgnoreCase("no"));
    }

    /**
     * Returns the bitstream mime type
     *
     * @param bitstream DSpace bitstream
     * @param context   DSpace context
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
     * Checks to see if the item is searchable. Based on the
     * {@link #METADATA_IIIF_SEARCH_ENABLED} metadata.
     *
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
     *
     * @param context        DSpace Context
     * @param item           DSpace Item
     * @param canvasPosition bitstream position
     * @return bitstream or null if the specified canvasPosition doesn't exist in
     *         the manifest
     */
    public Bitstream getBitstreamForCanvas(Context context, Item item, int canvasPosition) {
        List<Bitstream> bitstreams = getIIIFBitstreams(context, item);
        return bitstreams.size() > canvasPosition ? bitstreams.get(canvasPosition) : null;
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

    /**
     * Return all the bitstreams in the item to be used as annotations
     *
     * @param item    the DSpace item
     * @return a not null list of bitstreams to use as IIIF resources in the
     *         manifest
     */
    public List<Bitstream> getSeeAlsoBitstreams(Item item) {
        List<Bitstream> seeAlsoBitstreams = new ArrayList<>();
        List<Bundle> bundles = item.getBundles(OTHER_CONTENT_BUNDLE);
        if (bundles.size() > 0) {
            for (Bundle bundle : bundles) {
                List<Bitstream> bitstreams = bundle.getBitstreams();
                seeAlsoBitstreams.addAll(bitstreams);
            }
        }
        return seeAlsoBitstreams;
    }

    /**
     * Return the custom iiif label for the resource or the provided default if none
     *
     * @param dso          the dspace object to use as iiif resource
     * @param defaultLabel the default label to return if none is specified in the
     *                     metadata
     * @return the iiif label for the dspace object
     */
    public String getIIIFLabel(DSpaceObject dso, String defaultLabel) {
        return dso.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_LABEL))
                .findFirst().map(m -> m.getValue()).orElse(defaultLabel);
    }

    /**
     * Return the custom iiif description for the resource or the provided default if none
     *
     * @param dso          the dspace object to use as iiif resource
     * @param defaultDescription the default description to return if none is specified in the
     *                     metadata
     * @return the iiif label for the dspace object
     */
    public String getIIIFDescription(DSpaceObject dso, String defaultDescription) {
        return dso.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_DESCRIPTION))
                .findFirst().map(m -> m.getValue()).orElse(defaultDescription);
    }

    /**
     * Return the table of contents (toc) positions in the iiif structure where the
     * resource appears. Please note that the same resource can belong to multiple
     * ranges (i.e. a page that contains the last paragraph of a section and start
     * the new section)
     *
     * @param bitstream    the dspace bitstream
     * @param prefix a string to add to all the returned toc inherited from the
     *               parent dspace object
     * @return the iiif tocs for the dspace object
     */
    public List<String> getIIIFToCs(Bitstream bitstream, String prefix) {
        List<String> tocs = bitstream.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_TOC))
                .map(m -> StringUtils.isNotBlank(prefix) ? prefix + TOC_SEPARATOR + m.getValue() : m.getValue())
                .collect(Collectors.toList());
        if (tocs.size() == 0 && StringUtils.isNotBlank(prefix)) {
            return List.of(prefix);
        } else {
            return tocs;
        }
    }

    /**
     * Retrieves image dimensions from the image server (IIIF Image API v.2.1.1).
     * @param bitstream the bitstream DSO
     * @return image dimensions
     */
    @Cacheable(key = "#bitstream.getID().toString()", cacheNames = "canvasdimensions")
    public int[] getImageDimensions(Bitstream bitstream) {
        return iiifApiQueryService.getImageDimensions(bitstream);
    }

    /**
     * Test to see if the bitstream contains iiif image width metadata.
     * @param bitstream the bitstream DSo
     * @return true if width metadata was found
     */
    public boolean hasWidthMetadata(Bitstream bitstream) {
        return bitstream.getMetadata().stream()
                  .filter(m -> m.getMetadataField().toString('.').contentEquals("iiif.image.width"))
                  .findFirst().map(m -> m != null).orElse(false);

    }

    /**
     * Return the iiif toc for the specified bundle
     * 
     * @param bundle the dspace bundle
     * @return the iiif toc for the specified bundle
     */
    public String getBundleIIIFToC(Bundle bundle) {
        String label = bundle.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_LABEL))
                .findFirst().map(m -> m.getValue()).orElse(bundle.getName());
        return bundle.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_TOC))
                .findFirst().map(m -> m.getValue() + TOC_SEPARATOR + label).orElse(label);
    }

    /**
     * Return the iiif viewing hint for the item
     * 
     * @param item        the dspace item
     * @param defaultHint the default hint to apply if nothing else is defined at
     *                    the item leve
     * @return the iiif viewing hint for the item
     */
    public String getIIIFViewingHint(Item item, String defaultHint) {
        return item.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_VIEWING_HINT))
                .findFirst().map(m -> m.getValue()).orElse(defaultHint);
    }

    /**
     * Return the width for the canvas associated with the bitstream. If the
     * bitstream doesn't provide directly the information it is retrieved from the
     * bundle, item or default.
     * 
     * @param bitstream    the dspace bitstream used in the canvas
     * @param bundle       the bundle the bitstream belong to
     * @param item         the item the bitstream belong to
     * @param defaultWidth the default width to apply if no other preferences are
     *                     found
     * @return the width in pixel for the canvas associated with the bitstream
     */
    public int getCanvasWidth(Bitstream bitstream, Bundle bundle, Item item, int defaultWidth) {
        return getSizeFromMetadata(bitstream, METADATA_IMAGE_WIDTH,
                    getSizeFromMetadata(bundle, METADATA_IMAGE_WIDTH,
                        getSizeFromMetadata(item, METADATA_IMAGE_WIDTH, defaultWidth)));
    }

    /**
     * Return the height for the canvas associated with the bitstream. If the
     * bitstream doesn't provide directly the information it is retrieved from the
     * bundle, item or default.
     * 
     * @param bitstream    the dspace bitstream used in the canvas
     * @param bundle       the bundle the bitstream belong to
     * @param item         the item the bitstream belong to
     * @param defaultHeight the default width to apply if no other preferences are
     *                     found
     * @return the height in pixel for the canvas associated with the bitstream
     */
    public int getCanvasHeight(Bitstream bitstream, Bundle bundle, Item item, int defaultHeight) {
        return getSizeFromMetadata(bitstream, METADATA_IMAGE_HEIGHT,
                getSizeFromMetadata(bundle, METADATA_IMAGE_HEIGHT,
                    getSizeFromMetadata(item, METADATA_IMAGE_HEIGHT, defaultHeight)));
    }

    /**
     * Utility method to extract an integer from metadata value. The defaultValue is
     * returned if there are not values for the specified metadata or the value is
     * not a valid integer. Only the first metadata value if any is used
     * 
     * @param dso          the dspace object
     * @param metadata     the metadata key (schema.element[.qualifier]
     * @param defaultValue default to return if the metadata value is not an integer
     * @return an integer from metadata value
     */
    private int getSizeFromMetadata(DSpaceObject dso, String metadata, int defaultValue) {
        return dso.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString('.').contentEquals(metadata))
                .findFirst().map(m -> castToInt(m, defaultValue))
                  .orElse(defaultValue);
    }

    /**
     * Utility method to cast a metadata value to int. The defaultInt is returned if
     * the metadata value is not a valid integer
     * 
     * @param m             the metadata value
     * @param defaultInt default to return if the metadata value is not an
     *                      integer
     * @return an int corresponding to the metadata value
     */
    private int castToInt(MetadataValue m, int defaultInt) {
        try {
            return Integer.parseInt(m.getValue());
        } catch (NumberFormatException e) {
            log.error("Error parsing " + m.getMetadataField().toString('.') + " of " + m.getDSpaceObject().getID()
                    + " the value " + m.getValue() + " is not an integer. Returning the default.");
        }
        return defaultInt;
    }

    /**
     * Return the prefix to use to generate canvas name for canvas that has no an
     * explicit IIIF label
     * 
     * @param item          the DSpace Item
     * @param defaultNaming a default to return if the item has not a custom value
     * @return the prefix to use to generate canvas name for canvas that has no an
     *         explicit IIIF label
     */
    public String getCanvasNaming(Item item, String defaultNaming) {
        return item.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_CANVAS_NAMING))
                .findFirst().map(m -> m.getValue()).orElse(defaultNaming);
    }

}
