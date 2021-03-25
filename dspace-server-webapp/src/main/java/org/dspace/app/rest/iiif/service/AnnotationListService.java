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

import org.dspace.app.rest.iiif.model.generator.AnnotationGenerator;
import org.dspace.app.rest.iiif.model.generator.AnnotationListGenerator;
import org.dspace.app.rest.iiif.model.generator.ExternalLinksGenerator;
import org.dspace.app.rest.iiif.model.generator.PropertyValueGenerator;
import org.dspace.app.rest.iiif.service.util.IIIFUtils;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AnnotationListService extends AbstractResourceService {

    @Autowired
    IIIFUtils utils;

    @Autowired
    ItemService itemService;

    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    BitstreamFormatService bitstreamFormatService;

    @Autowired
    PropertyValueGenerator propertyValue;

    @Autowired
    AnnotationListGenerator annotationList;

    @Autowired
    AnnotationGenerator annotation;

    @Autowired
    ExternalLinksGenerator otherContent;

    public AnnotationListService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
    }

    /**
     * Returns an AnnotationList for bitstreams in the OtherContent bundle.
     * These resources are not appended directly to the manifest but can be accessed
     * via the seeAlso link.
     *
     * The semantics of this linking property may be extended to full text files, but
     * machine readable formats like ALTO, METS, and schema.org descriptions are preferred.
     *
     * @param context DSpace context
     * @param id bitstream uuid
     * @return AnnotationList as JSON
     */
    public String getSeeAlsoAnnotations(Context context, UUID id)
            throws RuntimeException {
        /**
         * We need the DSpace item to proceed.
         */
        Item item;
        try {
            item = itemService.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        /**
         * AnnotationList requires an identifier.
         */
        annotationList.setIdentifier(IIIF_ENDPOINT + id + "/manifest/seeAlso");
        /**
         * Get the "OtherContent" bundle for the item. Then add
         * Annotations for each bitstream found in the bundle.
         */
        List<Bundle> bundles = utils.getBundle(item, OTHER_CONTENT_BUNDLE);
        if (bundles.size() > 0) {
            for (Bundle bundle : bundles) {
                List<Bitstream> bitstreams = bundle.getBitstreams();
                for (Bitstream bitstream : bitstreams) {
                    BitstreamFormat format;
                    String mimetype;
                    try {
                        format = bitstream.getFormat(context);
                        mimetype = format.getMIMEType();
                    } catch (SQLException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                    annotation.setIdentifier(IIIF_ENDPOINT + bitstream.getID() + "/annot");
                    annotation.setMotivation(AnnotationGenerator.LINKING);
                    otherContent.setIdentifier(BITSTREAM_PATH_PREFIX
                            + "/"
                            + bitstream.getID()
                            + "/content");
                    otherContent.setFormat(mimetype);
                    otherContent.setLabel(bitstream.getName());
                    annotation.setResource(otherContent);
                    annotationList.addResource(annotation);
                }
            }
        }
        return utils.asJson(annotationList.getResource());
    }
}
