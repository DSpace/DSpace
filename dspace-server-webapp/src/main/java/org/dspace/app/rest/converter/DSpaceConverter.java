/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.projection.Projection;

public interface DSpaceConverter<M, R> {

    R convert(M modelObject, Projection projection);

    Class<M> getModelClass();
}
