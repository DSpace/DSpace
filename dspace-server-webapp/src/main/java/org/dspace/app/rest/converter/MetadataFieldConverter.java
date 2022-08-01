/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.MetadataFieldRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.MetadataField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the MetadataField in the DSpace API data model and
 * the REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class MetadataFieldConverter implements DSpaceConverter<MetadataField, MetadataFieldRest> {
    // Must be loaded @Lazy, as ConverterService autowires all DSpaceConverter components
    @Lazy
    @Autowired
    private ConverterService converter;

    @Override
    public MetadataFieldRest convert(MetadataField obj, Projection projection) {
        MetadataFieldRest field = new MetadataFieldRest();
        field.setProjection(projection);
        field.setId(obj.getID());
        field.setElement(obj.getElement());
        field.setQualifier(obj.getQualifier());
        field.setScopeNote(obj.getScopeNote());
        field.setSchema(converter.toRest(obj.getMetadataSchema(), projection));
        return field;
    }

    @Override
    public Class<MetadataField> getModelClass() {
        return MetadataField.class;
    }
}
