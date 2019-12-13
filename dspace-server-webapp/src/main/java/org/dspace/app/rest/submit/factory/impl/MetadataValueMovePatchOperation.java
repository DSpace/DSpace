/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.sql.SQLException;

import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.core.Utils;

/**
 * Submission "move" common PATCH operation.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public abstract class MetadataValueMovePatchOperation<DSO extends DSpaceObject>
    extends MovePatchOperation<MetadataValueRest> {

    @Override
    protected Class<MetadataValueRest[]> getArrayClassForEvaluation() {
        return MetadataValueRest[].class;
    }

    @Override
    protected Class<MetadataValueRest> getClassForEvaluation() {
        return MetadataValueRest.class;
    }

    protected void moveValue(Context context, DSO source, String target, int from, int to) throws SQLException {
        String[] metadata = Utils.tokenize(target);
        getDSpaceObjectService().moveMetadata(context, source, metadata[0], metadata[1], metadata[2],
                                              from, to);
    }

    protected abstract DSpaceObjectService<DSO> getDSpaceObjectService();
}
