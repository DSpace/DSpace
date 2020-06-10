/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutTab;
import org.springframework.stereotype.Component;

/**
 * Implementation for CrisLayoutTab boxes patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/layout/tabs/<:id> -H "
 * Content-Type: application/json" -d '[{ "op": "remove", "path": "
 * /boxes/<:index>"]'
 * </code>
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class CrisLayoutTabBoxesRemoveOperation<D> extends PatchOperation<D> {

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String OPERATION_PATH_BOXES = "/boxes";

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.patch.operation.PatchOperation#perform
     * (org.dspace.core.Context, java.lang.Object, org.dspace.app.rest.model.patch.Operation)
     */
    @Override
    public D perform(Context context, D resource, Operation operation) throws SQLException {
        if (supports(resource, operation)) {
            CrisLayoutTab tab = (CrisLayoutTab) resource;
            checkModelForExistingValue(tab);
            // get index to remove from operation path
            int objIndex = objectIndex(operation.getPath());
            if (objIndex > -1) {
                tab.removeBox(objIndex);
            }
        } else {
            throw new DSpaceBadRequestException("CrisLayoutTabBoxesRemoveOperation does not support this operation");
        }
        return resource;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.patch.operation.PatchOperation#supports
     * (java.lang.Object, org.dspace.app.rest.model.patch.Operation)
     */
    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof CrisLayoutTab && operation.getOp().trim().equalsIgnoreCase(OPERATION_REMOVE)
                && operation.getPath().trim().startsWith(OPERATION_PATH_BOXES));
    }

    /**
     * Checks whether the boxes of Tab has an existing value to remove
     * @param CrisLayoutTab Object on which patch is being done
     */
    private void checkModelForExistingValue(CrisLayoutTab tab) {
        if (tab.getBoxes() == null) {
            throw new DSpaceBadRequestException("Attempting to remove a non-existent value.");
        }
    }

    private int objectIndex(String path) {
        int idx = -1;
        if (path != null && path.trim().length() > 0) {
            int last = path.lastIndexOf('/');
            if (last > -1 && last < path.length()) {
                idx = Integer.valueOf( path.substring(++last));
            }
        }
        return idx;
    }
}
