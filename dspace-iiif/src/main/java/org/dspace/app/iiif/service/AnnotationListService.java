/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.dspace.app.iiif.model.generator.AnnotationGenerator;
import org.dspace.app.iiif.model.generator.AnnotationListGenerator;
import org.dspace.app.iiif.model.generator.ExternalLinksGenerator;
import org.dspace.app.iiif.service.utils.IIIFUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.iiif.util.IIIFSharedUtils;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This service provides methods for creating an {@code Annotation List}. There should be a single instance of
 * this service per request. The {@code @RequestScope} provides a single instance created and available during
 * complete lifecycle of the HTTP request.
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@RequestScope
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
    AnnotationListGenerator annotationList;


    public AnnotationListService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
    }


    /**
     * Returns image annotation list from the bitstream metadata field. The
     * JSON annotation list is created outside DSpace and added as a bitstream
     * to the ANNOTATIONS bundle.
     * @param context DSpace context
     * @param uuid bitstream UUID
     * @return IIIF annotation list
     */
    public String getImageAnnotations(Context context, UUID uuid) throws RuntimeException {
        BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
        Bitstream bitstream;
        try {
            bitstream = bitstreamService.find(context, uuid);
            if (bitstream == null) {
                throw new ResourceNotFoundException("DSpace bitstream for  id " + uuid + " not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return getAnnotation(bitstream, context);
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
            AnnotationGenerator annotation = new AnnotationGenerator(IIIF_ENDPOINT + bitstream.getID())
                .setMotivation(AnnotationGenerator.LINKING)
                .setResource(getLinksGenerator(mimetype, bitstream));
            annotationList.addResource(annotation);
        }
        return utils.asJson(annotationList.generateResource());
    }

    private ExternalLinksGenerator getLinksGenerator(String mimetype, Bitstream bitstream) {
        String identifier = BITSTREAM_PATH_PREFIX
                + "/"
                + bitstream.getID()
                + "/content";

        return new ExternalLinksGenerator(identifier)
                .setFormat(mimetype)
                .setLabel(bitstream.getName());
    }

    /**
     * Returns the JSON formatted IIIF annotation if found.
     * @param bitstream Bitstream
     * @param context DSpace context
     * @return json annotation
     */
    private String getAnnotation(Bitstream bitstream, Context context)  {
        Pattern pattern = Pattern.compile("^" + bitstream.getID() + ".json");
        try {
            for (Bundle bundle : bitstream.getBundles()) {
                for (Item item : bundle.getItems()) {
                    for (Bundle annotationsBundle : itemService.getBundles(item, IIIFSharedUtils.ANNOTATIONS_BUNDLE)) {
                        for (Bitstream annotationFile : annotationsBundle.getBitstreams()) {
                            if (pattern.matcher(annotationFile.getName()).matches()) {
                                InputStream iiifAnnotation = bitstreamService.retrieve(context, annotationFile);
                                return new String(iiifAnnotation.readAllBytes(), StandardCharsets.UTF_8);
                            }
                        }
                    }
                }
            }
        } catch (SQLException | IOException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
        return "{}";
    }
}
