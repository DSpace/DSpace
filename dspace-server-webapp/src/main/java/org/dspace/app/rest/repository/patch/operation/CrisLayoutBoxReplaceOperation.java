/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.LayoutSecurity;
import org.springframework.stereotype.Component;

/**
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class CrisLayoutBoxReplaceOperation<D> extends PatchOperation<D> {

    /**
     * Path in json body of patch that uses this operation
     */
    private static List<String> OPERATION_PATH;
    static {
        OPERATION_PATH = new ArrayList<>();
        OPERATION_PATH.add("/shortname");
        OPERATION_PATH.add("/header");
        OPERATION_PATH.add("/collapsed");
        OPERATION_PATH.add("/minor");
        OPERATION_PATH.add("/style");
        OPERATION_PATH.add("/security");
        OPERATION_PATH.add("/boxType");
        OPERATION_PATH.add("/clear");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.patch.operation.PatchOperation#
     * perform(org.dspace.core.Context, java.lang.Object, org.dspace.app.rest.model.patch.Operation)
     */
    @Override
    public D perform(Context context, D resource, Operation operation) throws SQLException {
        checkOperationValue(operation.getValue());
        if (supports(resource, operation)) {
            CrisLayoutBox box = (CrisLayoutBox) resource;
            try {
                if (operation.getPath().equalsIgnoreCase(OPERATION_PATH.get(0))) {
                    box.setShortname((String) operation.getValue());
                }
                if (operation.getPath().equalsIgnoreCase(OPERATION_PATH.get(1))) {
                    box.setHeader((String) operation.getValue());
                }
                if (operation.getPath().equalsIgnoreCase(OPERATION_PATH.get(2))) {
                    box.setCollapsed(getBooleanValue(operation.getValue()));
                }
                if (operation.getPath().equalsIgnoreCase(OPERATION_PATH.get(3))) {
                    box.setMinor(getBooleanValue(operation.getValue()));
                }
                if (operation.getPath().equalsIgnoreCase(OPERATION_PATH.get(4))) {
                    box.setStyle((String) operation.getValue());
                }
                if (operation.getPath().equalsIgnoreCase(OPERATION_PATH.get(5))) {
                    box.setSecurity(
                            LayoutSecurity.valueOf(
                                    Integer.valueOf((String) operation.getValue())));
                }
                if (operation.getPath().equalsIgnoreCase(OPERATION_PATH.get(6))) {
                    box.setType((String) operation.getValue());
                }
                if (operation.getPath().equalsIgnoreCase(OPERATION_PATH.get(7))) {
                    box.setClear(getBooleanValue(operation.getValue()));
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        } else {
            throw new DSpaceBadRequestException
            ("CrisLayoutBoxReplaceOperation does not support this operation");
        }
        return resource;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.patch.operation.PatchOperation#
     * supports(java.lang.Object, org.dspace.app.rest.model.patch.Operation)
     */
    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof CrisLayoutBox && operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE)
                && OPERATION_PATH.contains(operation.getPath()));
    }

    private boolean getBooleanValue(Object obj) {
        Boolean val = null;
        if (obj instanceof Boolean) {
            val = (boolean) obj;
        } else if (obj instanceof String) {
            val = Boolean.valueOf((String) obj);
        }
        return val;
    }
}
