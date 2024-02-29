/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation.ldn;

import java.sql.SQLException;

import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.service.NotifyService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.repository.patch.operation.PatchOperation;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for NotifyService Name Replace patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/ldn/ldnservices/<:id-notifyService> -H "
 * Content-Type: application/json" -d '
 * [{
 *  "op": "replace",
 *  "path": "/name",
 *  "value": "name value"
 *  }]'
 * </code>
 */
@Component
public class NotifyServiceNameReplaceOperation extends PatchOperation<NotifyServiceEntity> {

    @Autowired
    private NotifyService notifyService;

    private static final String OPERATION_PATH = "/name";

    @Override
    public NotifyServiceEntity perform(Context context, NotifyServiceEntity notifyServiceEntity, Operation operation)
        throws SQLException {
        checkOperationValue(operation.getValue());

        Object name = operation.getValue();
        if (name == null | !(name instanceof String)) {
            throw new UnprocessableEntityException("The /name value must be a string");
        }

        checkModelForExistingValue(notifyServiceEntity);
        notifyServiceEntity.setName((String) name);
        notifyService.update(context, notifyServiceEntity);
        return notifyServiceEntity;
    }

    /**
     * Checks whether the name of notifyServiceEntity has an existing value to replace
     * @param notifyServiceEntity Object on which patch is being done
     */
    private void checkModelForExistingValue(NotifyServiceEntity notifyServiceEntity) {
        if (notifyServiceEntity.getName() == null) {
            throw new DSpaceBadRequestException("Attempting to replace a non-existent value (name).");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof NotifyServiceEntity &&
            operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE) &&
            operation.getPath().trim().toLowerCase().equalsIgnoreCase(OPERATION_PATH));
    }
}
