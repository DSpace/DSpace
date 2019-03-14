/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

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
    .DSpaceObjectRest>
    extends DSpaceConverter<M, R> {

    @Autowired(required = true)
    private MetadataConverter metadataConverter;

    @Override
    public R fromModel(M obj) {
        R resource = newInstance();
        resource.setHandle(obj.getHandle());
        if (obj.getID() != null) {
            resource.setUuid(obj.getID().toString());
        }
        resource.setName(obj.getName());
        resource.setMetadata(metadataConverter.convert(obj.getMetadata()));
        return resource;
    }

    @Override
    public M toModel(R obj) {
        return null;
    }

    public boolean supportsModel(DSpaceObject object) {
        return object != null && object.getClass().equals(getModelClass());
    }

    protected abstract R newInstance();

    protected abstract Class<M> getModelClass();

}