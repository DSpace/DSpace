/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.MetadataValueList;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.DSpaceObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is the base converter from/to objects in the DSpace API data model and
 * the REST data model
 *
 * @param <M> the Class in the DSpace API data model
 * @param <R> the Class in the DSpace REST data model
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public abstract class DSpaceObjectConverter<M extends DSpaceObject, R extends org.dspace.app.rest.model
    .DSpaceObjectRest> implements DSpaceConverter<M, R> {

    @Autowired
    ConverterService converter;

    @Override
    public R convert(M obj, Projection projection) {
        R resource = newInstance();
        resource.setProjection(projection);
        resource.setHandle(obj.getHandle());
        if (obj.getID() != null) {
            resource.setUuid(obj.getID().toString());
        }
        resource.setName(obj.getName());
        MetadataValueList metadataValues = new MetadataValueList(obj.getMetadata());
        resource.setMetadata(converter.toRest(metadataValues, projection));
        return resource;
    }

    protected abstract R newInstance();
}
