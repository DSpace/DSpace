package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.patch.Operation;
import static org.dspace.app.rest.repository.patch.operation.PatchOperation.OPERATION_REPLACE;
import org.dspace.core.Context;
import org.dspace.eperson.Unit;
import org.dspace.eperson.service.UnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for Unit name replacement patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/epersons/units/<:id-unit> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /name", "value": "new name"]'
 * </code>
 */
@Component
public class UnitNameReplaceOperation<R> extends PatchOperation<R> {

    @Autowired
    UnitService unitService;

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String OPERATION_PATH_NAME = "/name";

    @Override
    public R perform(Context context, R object, Operation operation) {
        checkOperationValue(operation.getValue());
        if (supports(object, operation)) {
            //UnitService unitService = EPersonServiceFactory.getInstance().getUnitService();
            Unit unit = (Unit) object;
            checkModelForExistingValue(unit);
            try {
                unitService.setName(unit, (String) operation.getValue());
            } catch (SQLException e) {
                throw new DSpaceBadRequestException(
                        "SQLException in UnitNameReplaceOperation.perform "
                                + "trying to replace the name of the group.", e);
            }
            return object;
        } else {
            throw new DSpaceBadRequestException(
                    "UnitNameReplaceOperation does not support this operation");
        }
    }

    /**
     * Checks whether the name of Unit has an existing value to replace
     *
     * @param unit Object on which patch is being done
     */
    private void checkModelForExistingValue(Unit unit) {
        if (unit.getName() == null) {
            throw new DSpaceBadRequestException("Attempting to replace a non-existent value (name).");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof Unit
                && operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE)
                && operation.getPath().trim().equalsIgnoreCase(OPERATION_PATH_NAME));
    }
}

