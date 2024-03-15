/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.MetadataBitstreamWrapperRest;
import org.dspace.app.rest.model.wrapper.MetadataBitstreamWrapper;
import org.dspace.app.rest.projection.Projection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the MetadataBitstreamWrapper in the DSpace API data model and the
 * REST data model
 *
 * @author longtv
 */
@Component
public class MetadataBitstreamWrapperConverter implements DSpaceConverter<MetadataBitstreamWrapper,
        MetadataBitstreamWrapperRest> {

    @Lazy
    @Autowired
    private ConverterService converter;


    @Autowired
    private BitstreamConverter bitstreamConverter;

    @Override
    public MetadataBitstreamWrapperRest convert(MetadataBitstreamWrapper modelObject, Projection projection) {
        MetadataBitstreamWrapperRest bitstreamWrapperRest = new MetadataBitstreamWrapperRest();
        bitstreamWrapperRest.setProjection(projection);
        bitstreamWrapperRest.setName(modelObject.getBitstream().getName());
        bitstreamWrapperRest.setId(modelObject.getBitstream().getID().toString());
        bitstreamWrapperRest.setDescription(modelObject.getDescription());
        bitstreamWrapperRest.setChecksum(modelObject.getBitstream().getChecksum());
        bitstreamWrapperRest.setFileSize(modelObject.getBitstream().getSizeBytes());
        bitstreamWrapperRest.setFileInfo(modelObject.getFileInfo());
        bitstreamWrapperRest.setHref(modelObject.getHref());
        bitstreamWrapperRest.setFormat(modelObject.getFormat());
        bitstreamWrapperRest.setCanPreview(modelObject.isCanPreview());
        return bitstreamWrapperRest;
    }

    @Override
    public Class<MetadataBitstreamWrapper> getModelClass() {
        return MetadataBitstreamWrapper.class;
    }
}
