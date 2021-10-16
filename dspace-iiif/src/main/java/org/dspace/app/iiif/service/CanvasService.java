/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.iiif.model.generator.CanvasGenerator;
import org.dspace.app.iiif.model.generator.ImageContentGenerator;
import org.dspace.app.iiif.service.utils.BitstreamIIIFVirtualMetadata;
import org.dspace.app.iiif.service.utils.IIIFUtils;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This service provides methods for creating {@code Canvases}. There should be a single instance of
 * this service per request. The {@code @RequestScope} provides a single instance created and available during
 * complete lifecycle of the HTTP request.
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@RequestScope
@Component
public class CanvasService extends AbstractResourceService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(CanvasService.class);

    @Autowired
    ImageContentService imageContentService;

    @Autowired
    IIIFUtils utils;

    @Autowired
    ApplicationContext applicationContext;

    protected String[] BITSTREAM_METADATA_FIELDS;

    /**
     * Constructor.
     * 
     * @param configurationService the DSpace configuration service.
     */
    public CanvasService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
        BITSTREAM_METADATA_FIELDS = configurationService.getArrayProperty("iiif.metadata.bitstream");
    }

    /**
     * Creates a single {@code CanvasGenerator}.
     *
     * @param context DSpace Context
     * @param manifestId  manifest id
     * @param bitstream DSpace bitstream
     * @param bundle  DSpace bundle
     * @param item  DSpace item
     * @param count  the canvas position in the sequence.
     * @param mimeType  bitstream mimetype
     * @return a canvas generator
     */
    protected CanvasGenerator getCanvas(Context context, String manifestId, Bitstream bitstream, Bundle bundle,
            Item item, int count, String mimeType) {
        int pagePosition = count + 1;

        String canvasNaming = utils.getCanvasNaming(item, I18nUtil.getMessage("iiif.canvas.default-naming"));
        String label = utils.getIIIFLabel(bitstream, canvasNaming + " " + pagePosition);
        int canvasWidth = utils.getCanvasWidth(bitstream, bundle, item, DEFAULT_CANVAS_WIDTH);
        int canvasHeight = utils.getCanvasHeight(bitstream, bundle, item, DEFAULT_CANVAS_HEIGHT);
        UUID bitstreamId = bitstream.getID();

        ImageContentGenerator image = imageContentService.getImageContent(bitstreamId, mimeType,
                imageUtil.getImageProfile(), IMAGE_PATH);

        ImageContentGenerator thumb = imageContentService.getImageContent(bitstreamId, mimeType,
                thumbUtil.getThumbnailProfile(), THUMBNAIL_PATH);

        return addMetadata(context, bitstream,
                new CanvasGenerator(IIIF_ENDPOINT + manifestId + "/canvas/c" + count)
                    .addImage(image.generateResource()).addThumbnail(thumb.generateResource()).setHeight(canvasHeight)
                    .setWidth(canvasWidth).setLabel(label));
    }

    /**
     * Ranges expect the Canvas object to have only an identifier.
     *
     * @param startCanvas the start canvas identifier
     * @return canvas generator
     */
    protected CanvasGenerator getRangeCanvasReference(String startCanvas) {
        return new CanvasGenerator(startCanvas);
    }

    /**
     * Adds metadata to canvas.
     * @param context DSpace context
     * @param bitstream DSpace bitstream
     * @param canvasGenerator canvas generator
     * @return canvas generator
     */
    private CanvasGenerator addMetadata(Context context, Bitstream bitstream, CanvasGenerator canvasGenerator) {
        BitstreamService bService = ContentServiceFactory.getInstance().getBitstreamService();
        for (String field : BITSTREAM_METADATA_FIELDS) {
            if (StringUtils.startsWith(field, "@") && StringUtils.endsWith(field, "@")) {
                String virtualFieldName = field.substring(1, field.length() - 1);
                String beanName = BitstreamIIIFVirtualMetadata.IIIF_BITSTREAM_VIRTUAL_METADATA_BEAN_PREFIX +
                        virtualFieldName;
                BitstreamIIIFVirtualMetadata virtual = applicationContext.getBean(beanName,
                        BitstreamIIIFVirtualMetadata.class);
                List<String> values = virtual.getValues(context, bitstream);
                if (values.size() > 0) {
                    if (values.size() > 1) {
                        canvasGenerator.addMetadata("bitstream.iiif-virtual." + virtualFieldName, values.get(0),
                                values.subList(1, values.size()).toArray(new String[values.size() - 1]));
                    } else {
                        canvasGenerator.addMetadata("bitstream.iiif-virtual." + virtualFieldName, values.get(0));
                    }
                }
            } else {
                String[] eq = field.split("\\.");
                String schema = eq[0];
                String element = eq[1];
                String qualifier = null;
                if (eq.length > 2) {
                    qualifier = eq[2];
                }
                List<MetadataValue> metadata = bService.getMetadata(bitstream, schema, element, qualifier,
                        Item.ANY);
                List<String> values = new ArrayList<String>();
                for (MetadataValue meta : metadata) {
                    if (meta.getValue() != null) {
                        values.add(meta.getValue());
                    }
                }
                if (values.size() > 0) {
                    if (values.size() > 1) {
                        canvasGenerator.addMetadata("bitstream." + field, values.get(0),
                                values.subList(1, values.size()).toArray(new String[values.size() - 1]));
                    } else {
                        canvasGenerator.addMetadata("bitstream." + field, values.get(0));
                    }
                }
            }
        }
        return canvasGenerator;
    }

}
