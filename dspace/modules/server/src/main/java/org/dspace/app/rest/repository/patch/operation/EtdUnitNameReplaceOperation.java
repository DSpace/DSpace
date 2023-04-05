package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.content.EtdUnit;
import org.dspace.content.service.EtdUnitService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for EtdUnit name replacement patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/core/etdunits/<:id-unit> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /name", "value": "new name"]'
 * </code>
 *
 * @param <R> the type of object being patched
 */
@Component
public class EtdUnitNameReplaceOperation<R> extends PatchOperation<R> {

    @Autowired
    EtdUnitService etdUnitService;

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String OPERATION_PATH_NAME = "/name";

    @Override
    public R perform(Context context, R object, Operation operation) {
        checkOperationValue(operation.getValue());
        if (supports(object, operation)) {
            EtdUnit etdUnit = (EtdUnit) object;
            checkModelForExistingValue(etdUnit);
            try {
                etdUnitService.setName(etdUnit, (String) operation.getValue());
            } catch (SQLException e) {
                throw new DSpaceBadRequestException(
                        "SQLException in EtdUnitNameReplaceOperation.perform "
                                + "trying to replace the name of the ETD unit.", e);
            }
            return object;
        } else {
            throw new DSpaceBadRequestException(
                    "EtdUnitNameReplaceOperation does not support this operation");
        }
    }

    /**
     * Checks whether the name of EtdUnit has an existing value to replace
     *
     * @param unit Object on which patch is being done
     */
    private void checkModelForExistingValue(EtdUnit etdUnit) {
        if (etdUnit.getName() == null) {
            throw new DSpaceBadRequestException("Attempting to replace a non-existent value (name).");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof EtdUnit
                && operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE)
                && operation.getPath().trim().equalsIgnoreCase(OPERATION_PATH_NAME));
    }
}
