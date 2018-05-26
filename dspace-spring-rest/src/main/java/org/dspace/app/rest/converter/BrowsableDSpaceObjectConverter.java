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
 * This is the base converter from/to objects in the DSpace API data model and
 * the REST data model
 *
 * @param <M> the Class in the DSpace API data model
 * @param <R> the Class in the DSpace REST data model
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public interface BrowsableDSpaceObjectConverter<M extends Object, R extends org.dspace.app.rest.model
    .RestAddressableModel>
    extends Converter<M, R> {

    public default boolean supportsModel(Object object) {
        return false;
    }
}