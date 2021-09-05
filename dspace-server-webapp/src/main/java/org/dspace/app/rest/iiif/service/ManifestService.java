/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.service;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.iiif.model.generator.ContentSearchGenerator;
import org.dspace.app.rest.iiif.model.generator.ImageContentGenerator;
import org.dspace.app.rest.iiif.model.generator.ManifestGenerator;
import org.dspace.app.rest.iiif.model.info.Info;
import org.dspace.app.rest.iiif.model.info.Range;
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
import org.springframework.web.context.annotation.RequestScope;

/**
 * Generates IIIF Manifest JSON response for a DSpace Item.
 */
@Component
@RequestScope
public class ManifestService extends AbstractResourceService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ManifestService.class);

    @Autowired
    protected ItemService itemService;

    @Autowired
    CanvasService canvasService;

    @Autowired
    RangeService rangeService;

    @Autowired
    SequenceService sequenceService;

    @Autowired
    RelatedService relatedService;

    @Autowired
    SeeAlsoService seeAlsoService;

    @Autowired
    ImageContentService imageContentService;

    @Autowired
    IIIFUtils utils;

    @Autowired
    ContentSearchGenerator contentSearchGenerator;

    @Autowired
    ManifestGenerator manifestGenerator;


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
        populateManifest(item, context);
        return utils.asJson(manifestGenerator.getResource());
    }

    /**
     * Populates the Manifest for a DSpace Item.
     *
     * @param item DSpace Item
     * @param context DSpace context
     * @return manifest object
     */
    private void populateManifest(Item item, Context context) {
        // If an IIIF bundle is found it will be used. Otherwise,
        // images in the ORIGINAL bundle will be used.
        List<Bundle> bundles = utils.getIiifBundle(item, IIIF_BUNDLE);
        List<Bitstream> bitstreams = utils.getBitstreams(bundles);
        // List<Bundle> thumbnailBundle = utils.getBundle(item, "THUMBNAIL");
        Info info = utils.validateInfoForManifest(utils.getInfo(context, item, IIIF_BUNDLE), bitstreams);
        manifestGenerator.setIdentifier(getManifestId(item.getID()));
        manifestGenerator.setLabel(item.getName());
        setLogoContainer();
        addRelated(item);
        addSearchService(item);
        addMetadata(item.getMetadata());
        addViewingHint(bitstreams.size());
        addThumbnail(bundles, context);
        addSequence(item, bitstreams, context, info);
        addRanges(info, item.getID().toString());
        addSeeAlso(item);
    }

    /**
     * Adds a single sequence with canvases and a rendering property (optional).
     * @param item DSpace Item
     * @param bitstreams list of bitstreams
     * @param context the DSpace context
     * @return a sequence of canvases
     */
    private void addSequence(Item item, List<Bitstream> bitstreams, Context context, Info info) {
        manifestGenerator.addSequence(
                sequenceService.getSequence(item, bitstreams, context, info));
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
        manifestGenerator.addRelated(relatedService.getRelated(item));
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
        if (bundles.size() > 0) {
            manifestGenerator.addSeeAlso(seeAlsoService.getSeeAlso(item, bundles));
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
        List<Range> rangesFromConfig = utils.getRangesFromInfoObject(info);
        if (rangesFromConfig != null) {
            manifestGenerator.setRange(rangeService.getRanges(info, identifier));
        }
    }

    /**
     * Adds thumbnail to the manifest. Uses first image in bundle.
     * @param bundles image bundles
     * @param context DSpace context
     */
    private void addThumbnail(List<Bundle> bundles, Context context) {
        List<Bitstream> bitstreams = utils.getBitstreams(bundles);
        if (bitstreams != null && bitstreams.size() > 0) {
            String mimeType = utils.getBitstreamMimeType(bitstreams.get(0), context);
            if (utils.checkImageMimeType(mimeType)) {
                ImageContentGenerator image = imageContentService
                        .getImageContent(bitstreams.get(0).getID(), mimeType,
                                thumbUtil.getThumbnailProfile(), THUMBNAIL_PATH);
                manifestGenerator.addThumbnail(image);
            }
        }
    }

    /**
     * If the logo is defined in DSpace configuration add to manifest.
     */
    private void setLogoContainer() {
        if (IIIF_LOGO_IMAGE != null) {
            ImageContentGenerator logo = new ImageContentGenerator(IIIF_LOGO_IMAGE);
            manifestGenerator.addLogo(logo);
        }
    }

}
