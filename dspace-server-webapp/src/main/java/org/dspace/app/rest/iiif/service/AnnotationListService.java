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
import org.dspace.app.rest.iiif.service.util.IIIFUtils;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class AnnotationListService extends AbstractResourceService {

    ApplicationContext applicationContext;

    @Autowired
    IIIFUtils utils;

    @Autowired
    ItemService itemService;

    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    BitstreamFormatService bitstreamFormatService;

    @Autowired
    ExternalLinksGenerator externalLinksGenerator;

    @Autowired
    AnnotationListGenerator annotationList;


    public AnnotationListService(ApplicationContext applicationContext, ConfigurationService configurationService) {
        setConfiguration(configurationService);
        this.applicationContext = applicationContext;
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

        // We need the DSpace item to proceed
        Item item;
        try {
            item = itemService.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        // AnnotationList requires an identifier.
        annotationList.setIdentifier(IIIF_ENDPOINT + id + "/manifest/seeAlso");

        // Get the "seeAlso" bitstreams for the item. Add
        // Annotations for each bitstream found.
        List<Bitstream> bitstreams = utils.getSeeAlsoBitstreams(item);
        for (Bitstream bitstream : bitstreams) {
            BitstreamFormat format;
            String mimetype;
            try {
                format = bitstream.getFormat(context);
                mimetype = format.getMIMEType();
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            AnnotationGenerator annotation = applicationContext
                    .getBean(AnnotationGenerator.class, IIIF_ENDPOINT + bitstream.getID()
                            + "/annot", AnnotationGenerator.LINKING);
            annotation.setResource(getLinksGenerator(mimetype, bitstream));
            annotationList.addResource(annotation);
        }
        return utils.asJson(annotationList.getResource());
    }

    private ExternalLinksGenerator getLinksGenerator(String mimetype, Bitstream bitstream) {
        String identifier = BITSTREAM_PATH_PREFIX
                + "/"
                + bitstream.getID()
                + "/content";

        return externalLinksGenerator
                .setIdentifier(identifier)
                .setFormat(mimetype)
                .setLabel(bitstream.getName());
    }
}
