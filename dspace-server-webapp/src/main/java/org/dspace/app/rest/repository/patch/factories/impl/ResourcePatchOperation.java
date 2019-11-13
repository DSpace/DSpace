/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import java.sql.SQLException;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.core.Context;

/**
 * The patch interface used by repository classes.
 * @param <M>
 */
public interface ResourcePatchOperation<M> {

    M perform(Context context, M resource, Operation operation)
            throws DSpaceBadRequestException, SQLException;

}
