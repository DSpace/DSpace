/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.service.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.iiif.model.ObjectMapperFactory;
import org.dspace.app.rest.iiif.model.info.Info;
import org.dspace.app.rest.iiif.model.info.Range;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IIIFUtils {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(IIIFUtils.class);

    // The canvas position will be appended to this string.
    private static final String CANVAS_PATH_BASE = "/canvas/c";

    // get module subclass.
    protected SimpleModule iiifModule = ObjectMapperFactory.getIiifModule();
    // Use the object mapper subclass.
    protected ObjectMapper mapper = ObjectMapperFactory.getIiifObjectMapper();

    @Autowired
    protected BitstreamService bitstreamService;

    /**
     * For IIIF entities, this method returns the bundle assigned to IIIF
     * bitstreams. If the item is not an IIIF entity, the default (ORIGINAL)
     * bundle list is returned instead.
     * @param item the DSpace item
     * @param iiifBundle the name of the IIIF bundle
     * @return DSpace bundle
     */
    public List<Bundle> getIiifBundle(Item item, String iiifBundle) {
        boolean iiif = item.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString().contentEquals("dspace_entity_type"))
                .anyMatch(m -> m.getValue().contentEquals("IIIF")  ||
                        m.getValue().contentEquals("IIIFSearchable"));
        List<Bundle> bundles = new ArrayList<>();
        if (iiif) {
            bundles = item.getBundles(iiifBundle);
            if (bundles.size() == 0) {
                bundles = item.getBundles("ORIGINAL");
            }
        }
        return bundles;
    }

    /**
     * Returns the requested bundle.
     * @param item DSpace item
     * @param name bundle name
     * @return
     */
    public List<Bundle> getBundle(Item item, String name) {
        return item.getBundles(name);
    }

    /**
     * Returns bitstreams for the first bundle in the list.
     * @param bundles list of DSpace bundles
     * @return list of bitstreams
     */
    public List<Bitstream> getBitstreams(List<Bundle> bundles) {
        if (bundles != null && bundles.size() > 0) {
            return bundles.get(0).getBitstreams();
        }
        return null;
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
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Checks to see if the item is searchable. Based on the entity type.
     * @param item DSpace item
     * @return true if searchable
     */
    public boolean isSearchable(Item item) {
        return item.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString().contentEquals("dspace_entity_type"))
                .anyMatch(m -> m.getValue().contentEquals("IIIFSearchable"));
    }

    /**
     * Returns a metadata field name.
     * @param meta the DSpace metadata value object
     * @return field name as string
     */
    public String getMetadataFieldName(MetadataValue meta) {
        String element = meta.getMetadataField().getElement();
        String qualifier = meta.getMetadataField().getQualifier();
        // Need to distinguish DC type from DSpace relationship.type.
        // Setting element to be the schema name.
        if (meta.getMetadataField().getMetadataSchema().getName().contentEquals("relationship")) {
            qualifier = element;
            element = meta.getMetadataField().getMetadataSchema().getName();
        }
        String field = element;
        // Add qualifier if defined.
        if (qualifier != null) {
            field = field + "." + qualifier;
        }
        return field;
    }

    /**
     * Retrives a bitstream based on its position in the IIIF bundle.
     * @param item DSpace Item
     * @param canvasPosition bitstream position
     * @return bitstream
     */
    public Bitstream getBitstreamForCanvas(Item item, String bundleName, int canvasPosition) {
        List<Bundle> bundles = item.getBundles(bundleName);
        if (bundles.size() == 0) {
            return null;
        }
        List<Bitstream> bitstreams = bundles.get(0).getBitstreams();
        try {
            return bitstreams.get(canvasPosition);
        } catch (RuntimeException e) {
            throw new RuntimeException("The requested canvas is not available", e);
        }
    }

    /**
     * Attempts to find info.json file in the bitstream bundle and convert
     * the json into the Info.class domain model for canvas and range parameters.
     * @param context DSpace context
     * @param bundleName the IIIF bundle
     * @return info domain model
     */
    public Info getInfo(Context context, Item item, String bundleName) {
        Info info = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            // Look for expected json file bitstream in bundle.
            Bitstream infoBitstream = bitstreamService
                    .getBitstreamByName(item, bundleName, "info.json");
            if (infoBitstream != null)  {
                InputStream is = bitstreamService.retrieve(context, infoBitstream);
                info = mapper.readValue(is, Info.class);
            }
        } catch (IOException | SQLException e) {
            log.warn("Unable to read info.json file.", e);
        } catch (AuthorizeException e) {
            log.warn("Not authorized to access info.json file.", e);
        }
        return info;
    }

    /**
     * Returns the range parameter List or null
     * @param info the parameters model
     * @return list of range models
     */
    public List<Range> getRangesFromInfoObject(Info info) {
        if (info != null) {
            return info.getStructures();
        }
        return null;
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
     * Convenience method to compare canvas parameter and bitstream list size.
     * @param info the parameter model
     * @param bitstreams the list of DSpace bitstreams
     * @return true if sizes match
     */
    public boolean isListSizeMatch(Info info, List<Bitstream> bitstreams) {
        // If Info is not null then the bitstream bundle contains info.json; exclude
        // the file from comparison.
        if (info != null && info.getCanvases().size() == bitstreams.size() - 1) {
            return true;
        }
        return false;
    }

    /**
     * Convenience method verifies that the requested canvas exists in the
     * parameters model object.
     * @param info parameter model
     * @param canvasPosition requested canvas position
     * @return true if index is in bounds
     */
    public boolean canvasOutOfBounds(Info info, int canvasPosition) {
        return canvasPosition < 0 || canvasPosition >= info.getCanvases().size();
    }

    /**
     * Validates info.json for a single canvas.
     * Unless global settings are being used, when canvas information is not available
     * use defaults. The canvas information is defined in the info.json file.
     * @param info the information model
     * @param position the position of the requested canvas
     * @return information model
     */
    public Info validateInfoForSingleCanvas(Info info, int position) {
        if (info != null && info.getGlobalDefaults() != null) {
            if (canvasOutOfBounds(info, position) && !info.getGlobalDefaults().isActivated()) {
                log.warn("Canvas for position " + position + " not defined.\n" +
                        "Ignoring info.json canvas definitions and using defaults. " +
                        "Any other canvas-level annotations will also be ignored.");
                info.setCanvases(new ArrayList<>());
            }
        }
        return info;
    }

    /**
     * Unless global settings are being used, when canvas information list size does
     * not match the number of bitstreams use defaults. The canvas information is
     * defined in the info.json file.
     * @param info the information model
     * @param bitstreams the list of bitstreams
     * @return information model
     */
    public Info validateInfoForManifest(Info info, List<Bitstream> bitstreams) {
        if (info != null && info.getGlobalDefaults() != null) {
            if (!isListSizeMatch(info, bitstreams) && !info.getGlobalDefaults().isActivated()) {
                log.warn("Mismatch between info.json canvases and DSpace bitstream count.\n" +
                        "Ignoring info.json canvas definitions and using defaults." +
                        "Any other canvas-level annotations will also be ignored.");
                info.setCanvases(new ArrayList<>());
            }
        }
        return info;
    }

    /**
     * Serializes the resource. This method uses a customized
     * object mapper instead of the Spring default to serialize the
     * iiif response.
     * @param resource to be serialized
     * @return json string
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
}
