package org.dspace.app.rest.repository.patch.operation;

import static org.dspace.app.rest.repository.patch.operation.PatchOperation.OPERATION_REPLACE;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.core.Context;
import org.dspace.eperson.Unit;
import org.springframework.stereotype.Component;

/**
 * Implementation for Unit facultyOnly patches.Example: <code>
 curl -X PATCH http://${dspace.server.url}/api/epersons/units/<:id-unit> -H "
 Content-Type: application/json" -d '[{ "op": "replace", "path": "
 /facultyOnly", "value": true|false]'
 </code>
 *
 * @param <R> the type of object being patched
 */
@Component
public class UnitFacultyOnlyReplaceOperation<R> extends PatchOperation<R> {

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String OPERATION_PATH_FACULTY_ONLY = "/facultyOnly";

    @Override
    public R perform(Context context, R object, Operation operation) {
        checkOperationValue(operation.getValue());
        Boolean facultyOnly = getBooleanOperationValue(operation.getValue());
        if (supports(object, operation)) {
            Unit unit = (Unit) object;
            unit.setFacultyOnly(facultyOnly);
            return object;
        } else {
            throw new DSpaceBadRequestException("UnitFacultyOnlyReplaceOperation does not support this operation.");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof Unit && operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE)
                && operation.getPath().trim().equalsIgnoreCase(OPERATION_PATH_FACULTY_ONLY));
    }
}
