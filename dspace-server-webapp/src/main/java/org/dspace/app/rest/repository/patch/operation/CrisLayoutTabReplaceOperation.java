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
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.LayoutSecurity;
import org.springframework.stereotype.Component;

/**
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class CrisLayoutTabReplaceOperation<D> extends PatchOperation<D> {

    /**
     * Path in json body of patch that uses this operation
     */
    private static List<String> OPERATION_PATH;
    static {
        OPERATION_PATH = new ArrayList<>();
        OPERATION_PATH.add("/shortname");
        OPERATION_PATH.add("/header");
        OPERATION_PATH.add("/priority");
        OPERATION_PATH.add("/security");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.patch.operation.PatchOperation#
     * perform(org.dspace.core.Context, java.lang.Object, org.dspace.app.rest.model.patch.Operation)
     */
    @Override
    public D perform(Context context, D resource, Operation operation) throws SQLException {
        checkOperationValue(operation.getValue());
        if (supports(resource, operation)) {
            CrisLayoutTab tab = (CrisLayoutTab) resource;
            if (operation.getPath().equalsIgnoreCase(OPERATION_PATH.get(0))) {
                tab.setShortName((String) operation.getValue());
            }
            if (operation.getPath().equalsIgnoreCase(OPERATION_PATH.get(1))) {
                tab.setHeader((String) operation.getValue());
            }
            if (operation.getPath().equalsIgnoreCase(OPERATION_PATH.get(2))) {
                tab.setPriority(parseInteger(operation.getValue()));
            }
            if (operation.getPath().equalsIgnoreCase(OPERATION_PATH.get(3))) {
                tab.setSecurity(
                        LayoutSecurity.valueOf(
                                parseInteger(operation.getValue())));
            }
        } else {
            throw new DSpaceBadRequestException
            ("CrisLayoutTabReplaceOperation does not support this operation");
        }
        return resource;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.patch.operation.PatchOperation#
     * supports(java.lang.Object, org.dspace.app.rest.model.patch.Operation)
     */
    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof CrisLayoutTab && operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE)
                && OPERATION_PATH.contains(operation.getPath()));
    }

    private Integer parseInteger(Object val) {
        Integer value = null;
        if ( val instanceof String ) {
            value = Integer.valueOf((String)val);
        } else if ( val instanceof Integer ) {
            value = (Integer) val;
        }
        return value;
    }
}
