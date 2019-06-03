/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.springframework.core.convert.converter.Converter;

/**
 * Converts between DSpace and REST model objects.
 * @param <M> type of a DSpace model object (e.g. {@link org.dspace.content.Collection}).
 * @param <R> type of a REST model object (e.g. {@link org.dspace.app.rest.model.CollectionRest}).
 */
public interface DSpaceConverter<M, R> extends Converter<M, R> {
    @Override
    public default R convert(M source) {
        return fromModel(source);
    }

    /**
     * Convert a DSpace model object to its REST representation.
     * @param obj a DSpace model object (e.g. {@link org.dspace.content.Item})
     * @return a REST DTO for that type of object.
     */
    public abstract R fromModel(M obj);

    /**
     * Convert a REST representation of a DSpace model object to a model object.
     * @param obj REST DTO for the object.
     * @return DSpace model object represented by {@link obj}.
     */
    public abstract M toModel(R obj);
}
