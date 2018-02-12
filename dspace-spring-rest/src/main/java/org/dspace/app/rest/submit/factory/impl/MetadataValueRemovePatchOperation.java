/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.core.Utils;

/**
 * Submission "remove" PATCH operation.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public abstract class MetadataValueRemovePatchOperation<DSO extends DSpaceObject>
    extends RemovePatchOperation<MetadataValueRest> {

    @Override
    protected Class<MetadataValueRest[]> getArrayClassForEvaluation() {
        return MetadataValueRest[].class;
    }

    @Override
    protected Class<MetadataValueRest> getClassForEvaluation() {
        return MetadataValueRest.class;
    }

    protected void deleteValue(Context context, DSO source, String target, int index) throws SQLException {
        String[] metadata = Utils.tokenize(target);
        List<MetadataValue> mm = getDSpaceObjectService().getMetadata(source, metadata[0], metadata[1], metadata[2],
                                                                      Item.ANY);
        getDSpaceObjectService().clearMetadata(context, source, metadata[0], metadata[1], metadata[2], Item.ANY);
        if (index != -1) {
            int idx = 0;
            for (MetadataValue m : mm) {
                if (idx != index) {
                    getDSpaceObjectService().addMetadata(context, source, metadata[0], metadata[1], metadata[2],
                                                         m.getLanguage(), m.getValue(), m.getAuthority(),
                                                         m.getConfidence());
                }
                idx++;
            }
        }
    }

    protected abstract DSpaceObjectService<DSO> getDSpaceObjectService();

}
