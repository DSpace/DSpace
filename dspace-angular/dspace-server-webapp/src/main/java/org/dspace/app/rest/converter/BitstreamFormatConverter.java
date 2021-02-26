/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.BitstreamFormatRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.service.BitstreamFormatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the BitstreamFormat in the DSpace API data model and
 * the REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class BitstreamFormatConverter implements DSpaceConverter<BitstreamFormat, BitstreamFormatRest> {

    @Autowired
    BitstreamFormatService bitstreamFormatService;

    @Override
    public BitstreamFormatRest convert(BitstreamFormat obj, Projection projection) {
        BitstreamFormatRest bf = new BitstreamFormatRest();
        bf.setProjection(projection);
        bf.setId(obj.getID());
        bf.setShortDescription(obj.getShortDescription());
        bf.setDescription(obj.getDescription());
        bf.setMimetype(obj.getMIMEType());
        bf.setInternal(obj.isInternal());
        if (obj.getSupportLevel() > 0) {
            bf.setSupportLevel(bitstreamFormatService.getSupportLevelText(obj));
        } else {
            bf.setSupportLevel("UNKNOWN");
        }
        bf.setExtensions(obj.getExtensions());
        return bf;
    }

    @Override
    public Class<BitstreamFormat> getModelClass() {
        return BitstreamFormat.class;
    }
}
