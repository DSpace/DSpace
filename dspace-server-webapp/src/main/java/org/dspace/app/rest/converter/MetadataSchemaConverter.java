/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.MetadataSchemaRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.MetadataSchema;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the MetadataSchema in the DSpace API data model and
 * the REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class MetadataSchemaConverter implements DSpaceConverter<MetadataSchema, MetadataSchemaRest> {
    @Override
    public MetadataSchemaRest convert(MetadataSchema obj, Projection projection) {
        MetadataSchemaRest schema = new MetadataSchemaRest();
        schema.setProjection(projection);
        schema.setId(obj.getID());
        schema.setNamespace(obj.getNamespace());
        schema.setPrefix(obj.getName());
        return schema;
    }

    @Override
    public Class<MetadataSchema> getModelClass() {
        return MetadataSchema.class;
    }
}
