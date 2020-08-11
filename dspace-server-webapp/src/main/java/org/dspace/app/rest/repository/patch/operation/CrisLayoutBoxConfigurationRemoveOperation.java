/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Set;

import org.dspace.app.rest.model.patch.Operation;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutField;
import org.springframework.stereotype.Component;

/**
 *  * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/layout/boxmetadataconfigurations/<:box_id> -H "
 * Content-Type: application/json" -d '[{ "op": "remove", "path": "/rows/2/fields/1"]'
 * </code>
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class CrisLayoutBoxConfigurationRemoveOperation<D> extends PatchOperation<D> {

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String OPERATION_CONFIGURATION_PATH = "^/rows/[0-9]+/fields/[0-9]+";

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.patch.operation.PatchOperation#perform
     * (org.dspace.core.Context, java.lang.Object, org.dspace.app.rest.model.patch.Operation)
     */
    @Override
    public D perform(Context context, D resource, Operation operation) throws SQLException {
        if (supports(resource, operation)) {
            CrisLayoutBox box = (CrisLayoutBox) resource;
            Integer row = null;
            Integer position = null;
            String[] tks = operation.getPath().split("/");
            if (tks != null && tks.length > 0) {
                for (int i = 0; i < tks.length; i++) {
                    if (tks[i].equalsIgnoreCase("rows") && tks.length > i + 1) {
                        row = parseInteger(tks[++i]);
                    } else if (tks[i].equalsIgnoreCase("fields") && tks.length > i + 1) {
                        position = parseInteger(tks[++i]);
                    }
                }
            }
            if (row != null && position != null) {
                Set<CrisLayoutField> fields = box.getLayoutFields();
                for (Iterator<CrisLayoutField> it = fields.iterator(); it.hasNext(); ) {
                    CrisLayoutField field = it.next();
                    // TODO convert priority to position
                    if (field.getPriority().equals(position)) {
                        it.remove();
                    }
                }
            }
        }
        return resource;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.patch.operation.PatchOperation#supports
     * (java.lang.Object, org.dspace.app.rest.model.patch.Operation)
     */
    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof CrisLayoutBox && operation.getOp().trim().equalsIgnoreCase(OPERATION_REMOVE)
                && operation.getPath().matches(OPERATION_CONFIGURATION_PATH));
    }

    /**
     * Returns an Integer object holding the value of the specified String,
     * if the string cannot be parsed as an integer returns null
     * @param val
     * @return
     */
    private Integer parseInteger(String val) {
        Integer value = null;
        try {
            value = Integer.valueOf(val);
        } catch ( Exception e ) {
            value = null;
        }
        return value;
    }
}
