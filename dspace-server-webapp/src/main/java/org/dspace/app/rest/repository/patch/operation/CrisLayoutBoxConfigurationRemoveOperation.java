/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.service.CrisLayoutFieldService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private CrisLayoutFieldService fieldService;

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
                List<CrisLayoutField> fields = fieldService.findFieldByBoxId(context, box.getID(), row);
                if (fields != null && fields.size() > position) {
                    try {
                        fieldService.delete(context, fields.get(position));
                        fields.remove(fields.get(position));
                        // Update other field position
                        for (int i = position; i < fields.size(); i++) {
                            fields.get(i).setPriority(
                                    fields.get(i).getPriority() - 1
                            );
                        }
                        fieldService.update(context, fields);
                    } catch (AuthorizeException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                } else if (fields.isEmpty()) {
                    throw new
                    UnprocessableEntityException("The row <" + row + "> hasn't fields!");
                } else {
                    throw new
                    UnprocessableEntityException("The row <" + row + "> hasn't a field in position <" + position + ">."
                            + "Hint: the position is zero-based");
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
