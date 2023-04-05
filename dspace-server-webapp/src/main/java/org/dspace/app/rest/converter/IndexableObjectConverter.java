/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.discovery.IndexableObject;

/**
 * This is the base converter from/to objects in the DSpace API data model and
 * the REST data model that can be indexed
 *
 * @param <M> the Class in the DSpace API data model
 * @param <R> the Class in the DSpace REST data model
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public interface IndexableObjectConverter<M,
    R extends org.dspace.app.rest.model.RestAddressableModel> extends DSpaceConverter<M, R> {

    /**
     *
     * @param idxo
     *            the IndexableObject to check
     * @return true if the actual converter implementation is able to manage the supplied IndexableObject
     */
    boolean supportsModel(IndexableObject idxo);
}
