/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.projection.Projection;

/**
 * Conversion between DSpace model object and REST resource.
 *
 * @param <M> type of DSpace model object (e.g. Item)
 * @param <R> type of REST resource (e.g. ItemResource)
 */
public interface DSpaceConverter<M, R> {

    /**
     * Convert a DSpace model object into its equivalent REST resource, applying
     * a given projection.
     *
     * @param modelObject a DSpace API model object.
     * @param projection
     * @return a resource representing the model object.
     */
    R convert(M modelObject, Projection projection);

    /**
     * For what DSpace API model class does this converter convert?
     * @return Class of model objects represented.
     */
    Class<M> getModelClass();
}
