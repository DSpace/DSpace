/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.service;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import de.digitalcollections.iiif.model.sharedcanvas.AnnotationList;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.iiif.model.generator.CanvasGenerator;
import org.dspace.app.rest.iiif.model.generator.CanvasItemsGenerator;
import org.dspace.app.rest.iiif.model.generator.ContentSearchGenerator;
import org.dspace.app.rest.iiif.model.generator.ExternalLinksGenerator;
import org.dspace.app.rest.iiif.model.generator.ManifestGenerator;
import org.dspace.app.rest.iiif.model.generator.RangeGenerator;
import org.dspace.app.rest.iiif.model.info.Info;
import org.dspace.app.rest.iiif.model.info.RangeModel;
import org.dspace.app.rest.iiif.service.util.IIIFUtils;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Generates IIIF Manifest JSON response for a DSpace Item.
 */
@Component
public class ManifestService extends AbstractResourceService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ManifestService.class);

    private static final String PDF_DOWNLOAD_LABEL = "Download as PDF";
    private static final String RELATED_ITEM_LABEL = "DSpace item view";
    private static final String SEE_ALSO_LABEL = "More descriptions of this resource";

    @Autowired
    protected ItemService itemService;

    @Autowired
    CanvasService canvasService;

    @Autowired
    ExternalLinksGenerator otherContentGenerator;

    @Autowired
    ManifestGenerator manifestGenerator;

    @Autowired
    CanvasItemsGenerator sequenceGenerator;

    @Autowired
    RangeGenerator rangeGenerator;

    @Autowired
    ContentSearchGenerator contentSearchGenerator;

    @Autowired
    IIIFUtils utils;

    /**
     * Constructor.
     * @param configurationService the DSpace configuration service.
     */
    public ManifestService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
    }

    /**
     * Returns serialized Manifest response for a DSpace item.
     *
     * @param item the DSpace Item
     * @param context the DSpace context
     * @return Manifest as JSON
     */
    public String getManifest(Item item, Context context) {
        initializeManifestGenerator(item, context);
        return utils.asJson(manifestGenerator.getResource());
    }

    /**
     * Initializes the Manifest for a DSpace item.
     *
     * @param item DSpace Item
     * @param context DSpace context
     * @return manifest object
     */
    private void initializeManifestGenerator(Item item, Context context) {
        List<Bundle> bundles = utils.getIiifBundle(item, IIIF_BUNDLE);
        List<Bitstream> bitstreams = utils.getBitstreams(bundles);
        Info info = utils.validateInfoForManifest(utils.getInfo(context, item, IIIF_BUNDLE), bitstreams);
        manifestGenerator.setIdentifier(getManifestId(item.getID()));
        manifestGenerator.setLabel(item.getName());
        addRelated(item);
        addSearchService(item);
        addMetadata(item.getMetadata());
        addViewingHint(bitstreams.size());
        addThumbnail(bitstreams, context);
        addSequence(item, bitstreams, context, info);
        addRanges(info, item.getID().toString());
        addSeeAlso(item);
    }

    /**
     * Returns a single sequence with canvases and a rendering property (optional).
     * @param item DSpace Item
     * @param bitstreams list of bitstreams
     * @param context the DSpace context
     * @return a sequence of canvases
     */
    private void addSequence(Item item, List<Bitstream> bitstreams, Context context, Info info) {
        sequenceGenerator.setIdentifier(IIIF_ENDPOINT + item.getID() + "/sequence/s0");
        if (bitstreams.size() > 0) {
            addCanvases(sequenceGenerator, context, item, bitstreams, info);
        }
        addRendering(sequenceGenerator, item, context);
        manifestGenerator.addSequence(sequenceGenerator);
    }

    /**
     * Adds DSpace Item metadata to the manifest.
     *
     * @param metadata list of DSpace metadata values
     */
    private void addMetadata(List<MetadataValue> metadata) {
        for (MetadataValue meta : metadata) {
            String field = utils.getMetadataFieldName(meta);
            if (field.contentEquals("rights.uri")) {
                manifestGenerator.addMetadata(field, meta.getValue());
                manifestGenerator.addLicense(meta.getValue());
            } else if (field.contentEquals("description")) {
                // Add manifest description field.
                manifestGenerator.addDescription(field, meta.getValue());
            } else {
                // Exclude DSpace description.provenance field.
                if (!field.contentEquals("description.provenance")) {
                    // Everything else, add to manifest metadata fields.
                    manifestGenerator.addMetadata(field, meta.getValue());
                }
            }
        }
    }

    /**
     * A link to an external resource intended to be displayed directly to the user,
     * and is related to the resource that has the related property. Examples might
     * include a video or academic paper about the resource, a website, an HTML
     * description, and so forth.
     *
     * This method adds a link to the Item represented in the DSpace Angular UI.
     *
     * @param item the DSpace Item
     */
    private void addRelated(Item item) {
        String url = CLIENT_URL  + "/items/" + item.getID();
        otherContentGenerator.setIdentifier(url);
        otherContentGenerator.setFormat("text/html");
        otherContentGenerator.setLabel(RELATED_ITEM_LABEL);
        manifestGenerator.addRelated(otherContentGenerator);
    }

    /**
     * This method adds a canvas to the sequence for each item in the list of DSpace bitstreams.
     * To be added bitstreams must be on image mime type.
     *
     * @param sequence the sequence object
     * @param context the DSpace context
     * @param item the DSpace Item
     * @param bitstreams list of DSpace bitstreams
     */
    private void addCanvases(CanvasItemsGenerator sequence, Context context, Item item,
                             List<Bitstream> bitstreams, Info info) {
        // TODO: This adds all bitstreams from a bundle.  Consider bitstream pagination.
        /**
         * Counter tracks the position of the bitstream in the list and is used to create the canvas identifier.
         * Bitstream order is determined by position in the IIIF DSpace bundle.
         */
        int counter = 0;
        for (Bitstream bitstream : bitstreams) {
            UUID bitstreamID = bitstream.getID();
            String mimeType = utils.getBitstreamMimeType(bitstream, context);
            if (utils.checkImageMimeType(mimeType)) {
                CanvasGenerator canvas = canvasService.getCanvas(item.getID().toString(), info, counter);
                addImage(canvas, mimeType, bitstreamID);
                if (counter == 2) {
                    addImage(canvas, mimeType, bitstreamID);
                }
                sequence.addCanvas(canvas);
                counter++;
            }
        }
    }

    /**
     * A hint to the client as to the most appropriate method of displaying the resource.
     *
     * @param bitstreamCount count of bitstreams in the IIIF bundle.
     */
    private void addViewingHint(int bitstreamCount) {
        if (bitstreamCount > 2) {
            manifestGenerator.addViewingHint(DOCUMENT_VIEWING_HINT);
        }
    }

    /**
     * A link to a machine readable document that semantically describes the resource with
     * the seeAlso property, such as an XML or RDF description. This document could be used
     * for search and discovery or inferencing purposes, or just to provide a longer
     * description of the resource. May have one or more external descriptions related to it.
     *
     * This method appends an AnnotationList of resources found in the Item's OtherContent bundle.
     * A typical use case would be METS or ALTO files that describe the resource.
     *
     * @param item the DSpace Item.
     */
    private void addSeeAlso(Item item) {
        List<Bundle> bundles = utils.getBundle(item, OTHER_CONTENT_BUNDLE);
        if (bundles.size() == 0) {
            return;
        }
        otherContentGenerator.setIdentifier(IIIF_ENDPOINT + item.getID() + "/manifest/seeAlso");
        otherContentGenerator.setType(AnnotationList.TYPE);
        otherContentGenerator.setLabel(SEE_ALSO_LABEL);
        manifestGenerator.addSeeAlso(otherContentGenerator);
    }

    /**
     * A link to an external resource intended for display or download by a human user.
     * This property can be used to link from a manifest, collection or other resource
     * to the preferred viewing environment for that resource, such as a viewer page on
     * the publisher’s web site. Other uses include a rendering of a manifest as a PDF
     * or EPUB.
     *
     * This method looks for a PDF rendering in the Item's ORIGINAL bundle and adds
     * it to the Sequence if found.
     *
     * @param sequence Sequence object
     * @param item DSpace Item
     * @param context DSpace context
     */
    private void addRendering(CanvasItemsGenerator sequence, Item item, Context context) {
        List<Bundle> bundles = item.getBundles("ORIGINAL");
        if (bundles.size() == 0) {
            return;
        }
        Bundle bundle = bundles.get(0);
        List<Bitstream> bitstreams = bundle.getBitstreams();
        for (Bitstream bitstream : bitstreams) {
            String mimeType = null;
            try {
                mimeType = bitstream.getFormat(context).getMIMEType();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            // If the ORIGINAL bundle contains a PDF, assume that it represents the
            // item and add to rendering. Ignore other mime-types. This convention should
            // be documented.
            if (mimeType != null && mimeType.contentEquals("application/pdf")) {
                String id = BITSTREAM_PATH_PREFIX + "/" + bitstream.getID() + "/content";
                otherContentGenerator.setIdentifier(id);
                otherContentGenerator.setLabel(PDF_DOWNLOAD_LABEL);
                otherContentGenerator.setFormat(mimeType);
                sequence.addRendering(otherContentGenerator);
            }
        }
    }

    /**
     * A link to a service that makes more functionality available for the resource,
     * such as the base URI of an associated IIIF Search API service.
     *
     * This method returns a search service definition. Search scope is the manifest.
     *
     * @param item DSpace Item
     * @return the IIIF search service definition for the item
     */
    private void addSearchService(Item item) {
        if (utils.isSearchable(item)) {
            contentSearchGenerator.setIdentifier(IIIF_ENDPOINT + item.getID() + "/manifest/search");
            // TODO: get label from configuration then set on generator?
            manifestGenerator.addService(contentSearchGenerator);
        }
    }

    /**
     * Adds Ranges to manifest structures element.
     * Ranges are defined in the info.json file.
     * @param info
     * @param identifier
     */
    private void addRanges(Info info, String identifier) {
        List<RangeModel> rangesFromConfig = utils.getRangesFromInfoObject(info);
        if (rangesFromConfig != null) {
            for (int pos = 0; pos < rangesFromConfig.size(); pos++) {
                setRange(identifier, rangesFromConfig.get(pos), pos);
                manifestGenerator.addRange(rangeGenerator);
            }
        }
    }

    /**
     * Sets properties on the RangeFacade.
     * @param identifier DSpace item id
     * @param range range from info.json configuration
     * @param pos list position of the range
     */
    private void setRange(String identifier, RangeModel range, int pos) {
        String id = IIIF_ENDPOINT + identifier + "/r" + pos;
        String label = range.getLabel();
        rangeGenerator.setIdentifier(id);
        rangeGenerator.setLabel(label);
        String startCanvas = utils.getCanvasId(range.getStart());
        rangeGenerator.addCanvas(canvasService.getRangeCanvasReference(identifier, startCanvas));
    }

    /**
     * Adds thumbnail to the manifest
     * @param bitstreams
     * @param context
     */
    private void addThumbnail(List<Bitstream> bitstreams, Context context) {
        if (bitstreams.size() > 0) {
            String mimeType = utils.getBitstreamMimeType(bitstreams.get(0), context);
            if (utils.checkImageMimeType(mimeType)) {
                manifestGenerator.addThumbnail(getThumbnailAnnotation(bitstreams.get(0).getID(), mimeType));
            }
        }
    }
}
