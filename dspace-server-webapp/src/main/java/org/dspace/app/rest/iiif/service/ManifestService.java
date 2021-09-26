/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.iiif.model.generator.CanvasGenerator;
import org.dspace.app.rest.iiif.model.generator.ContentSearchGenerator;
import org.dspace.app.rest.iiif.model.generator.ImageContentGenerator;
import org.dspace.app.rest.iiif.model.generator.ManifestGenerator;
import org.dspace.app.rest.iiif.model.generator.RangeGenerator;
import org.dspace.app.rest.iiif.service.util.IIIFUtils;
import org.dspace.app.util.service.MetadataExposureService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Generates IIIF Manifest JSON response for a DSpace Item. Please note that
 * this is a request scoped bean. This mean that for each http request a
 * different instance will be initialized by Spring and used to serve this
 * specific request. This is needed because some configuration are cached in the
 * instance. Moreover, many injected dependencies are also request scoped or
 * prototype (that will turn in a request scope when injected in a request scope
 * bean). The generators need to be request scoped as they act as builder
 * storing the object state during each incremental building step until the
 * final object is returned (IIIF Resource)
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

    @Autowired
    MetadataExposureService metadataExposureService;

    protected String[] METADATA_FIELDS;
    /**
     * Constructor.
     * @param configurationService the DSpace configuration service.
     */
    public ManifestService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
        METADATA_FIELDS = configurationService.getArrayProperty("iiif.metadata.item");
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
        String manifestId = getManifestId(item.getID());
        manifestGenerator.setIdentifier(manifestId);
        manifestGenerator.setLabel(item.getName());
        setLogoContainer();
        addRelated(item);
        addSearchService(item);
        addMetadata(context, item);
        addViewingHint(item);
        addThumbnail(item, context);
        addRanges(context, item, manifestId);
        manifestGenerator.addSequence(
                sequenceService.getSequence(item, context));
        addSeeAlso(item);
    }

    /**
     * Add the ranges to the manifest structure. Ranges are generated from the
     * iiif.toc metadata
     * 
     * @param context the DSpace Context
     * @param item the DSpace Item to represent
     * @param manifestId the generated manifestId
     */
    private void addRanges(Context context, Item item, String manifestId) {
        List<Bundle> bundles = utils.getIIIFBundles(item);
        RangeGenerator root = new RangeGenerator(rangeService);
        root.setLabel(I18nUtil.getMessage("iiif.toc.root-label"));
        root.setIdentifier(manifestId + "/range/r0");
//        manifestGenerator.addRange(root);

        Map<String, RangeGenerator> tocRanges = new LinkedHashMap<String, RangeGenerator>();
        for (Bundle bnd : bundles) {
            String bundleToCPrefix = null;
            if (bundles.size() > 1) {
                bundleToCPrefix = utils.getBundleIIIFToC(bnd);
            }
            RangeGenerator lastRange = root;
            for (Bitstream b : utils.getIIIFBitstreams(context, bnd)) {
                CanvasGenerator canvasId = sequenceService.addCanvas(context, item, bnd, b);
                List<String> tocs = utils.getIIIFToCs(b, bundleToCPrefix);
                if (tocs.size() > 0) {
                    for (String toc : tocs) {
                        RangeGenerator currRange = root;
                        String[] parts = toc.split(IIIFUtils.TOC_SEPARATOR_REGEX);
                        String key = "";
                        for (int pIdx = 0; pIdx < parts.length; pIdx++) {
                            if (pIdx > 0) {
                                key += IIIFUtils.TOC_SEPARATOR;
                            }
                            key += parts[pIdx];
                            if (tocRanges.get(key) != null) {
                                currRange = tocRanges.get(key);
                            } else {
                                // create the sub range
                                RangeGenerator range = new RangeGenerator(rangeService);
                                range.setLabel(parts[pIdx]);
                                // add the range reference to the currRange so to get an identifier
                                currRange.addSubRange(range);

                                // add the range to the manifest
//                                manifestGenerator.addRange(range);

                                // move the current range
                                currRange = range;
                                tocRanges.put(key, range);
                            }
                        }
                        // add the bitstream canvas to the currRange
                        currRange
                                .addCanvas(canvasService.getRangeCanvasReference(manifestId, canvasId.getIdentifier()));
                        lastRange = currRange;
                    }
                } else {
                    lastRange.addCanvas(canvasService.getRangeCanvasReference(manifestId, canvasId.getIdentifier()));
                }
            }
        }
        if (tocRanges.size() > 0) {
            manifestGenerator.addRange(root);
            for (RangeGenerator range : tocRanges.values()) {
                manifestGenerator.addRange(range);
            }
        }
    }

    /**
     * Adds DSpace Item metadata to the manifest.
     * 
     * @param context the DSpace Context
     * @param item the DSpace item
     */
    private void addMetadata(Context context, Item item) {
        for (String field : METADATA_FIELDS) {
            String[] eq = field.split("\\.");
            String schema = eq[0];
            String element = eq[1];
            String qualifier = null;
            if (eq.length > 2) {
                qualifier = eq[2];
            }
            List<MetadataValue> metadata = item.getItemService().getMetadata(item, schema, element, qualifier,
                    Item.ANY);
            List<String> values = new ArrayList<String>();
            for (MetadataValue meta : metadata) {
                // we need to perform the check here as the configuration can include jolly
                // characters (i.e. dc.description.*) and we need to be sure to hide qualified
                // metadata (dc.description.provenance)
                try {
                    if (metadataExposureService.isHidden(context, meta.getMetadataField().getMetadataSchema().getName(),
                            meta.getMetadataField().getElement(), meta.getMetadataField().getQualifier())) {
                        continue;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                values.add(meta.getValue());
            }
            if (values.size() > 0) {
                if (values.size() > 1) {
                    manifestGenerator.addMetadata(field, values.get(0),
                            values.subList(1, values.size()).toArray(new String[values.size() - 1]));
                } else {
                    manifestGenerator.addMetadata(field, values.get(0));
                }
            }
        }
        String descrValue = item.getItemService().getMetadataFirstValue(item, "dc", "description", null, Item.ANY);
        if (StringUtils.isNotBlank(descrValue)) {
            manifestGenerator.addDescription(descrValue);
        }

        String licenseUriValue = item.getItemService().getMetadataFirstValue(item, "dc", "rights", "uri", Item.ANY);
        if (StringUtils.isNotBlank(licenseUriValue)) {
            manifestGenerator.addLicense(licenseUriValue);
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
     * @param item the DSpace Item
     */
    private void addViewingHint(Item item) {
        manifestGenerator.addViewingHint(utils.getIIIFViewingHint(item, DOCUMENT_VIEWING_HINT));
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
        manifestGenerator.addSeeAlso(seeAlsoService.getSeeAlso(item));
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
     * Adds thumbnail to the manifest. Uses first image in the manifest.
     * @param item the DSpace Item
     * @param context DSpace context
     */
    private void addThumbnail(Item item, Context context) {
        List<Bitstream> bitstreams = utils.getIIIFBitstreams(context, item);
        if (bitstreams != null && bitstreams.size() > 0) {
            String mimeType = utils.getBitstreamMimeType(bitstreams.get(0), context);
            ImageContentGenerator image = imageContentService
                    .getImageContent(bitstreams.get(0).getID(), mimeType,
                            thumbUtil.getThumbnailProfile(), THUMBNAIL_PATH);
            manifestGenerator.addThumbnail(image);
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
