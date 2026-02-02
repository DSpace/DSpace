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
 * Implementation for NotifyService Description Add patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/ldn/ldnservices/<:id-notifyService> -H "
 * Content-Type: application/json" -d '
 * [{
 *  "op": "add",
 *  "path": "/description",
 *  "value": "description value"
 *  }]'
 * </code>
 */
@Component
public class NotifyServiceDescriptionAddOperation extends PatchOperation<NotifyServiceEntity> {

    @Autowired
    private NotifyService notifyService;

    private static final String OPERATION_PATH = "/description";

    @Override
    public NotifyServiceEntity perform(Context context, NotifyServiceEntity notifyServiceEntity, Operation operation)
        throws SQLException {
        checkOperationValue(operation.getValue());

        Object description = operation.getValue();
        if (description == null | !(description instanceof String)) {
            throw new UnprocessableEntityException("The /description value must be a string");
        }

        checkNonExistingDescriptionValue(notifyServiceEntity);
        notifyServiceEntity.setDescription((String) description);
        notifyService.update(context, notifyServiceEntity);
        return notifyServiceEntity;
    }

    /**
     * Throws PatchBadRequestException if a value is already set in the /description path.
     *
     * @param notifyServiceEntity the notifyServiceEntity to update

     */
    void checkNonExistingDescriptionValue(NotifyServiceEntity notifyServiceEntity) {
        if (notifyServiceEntity.getDescription() != null) {
            throw new DSpaceBadRequestException("Attempting to add a value to an already existing path.");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof NotifyServiceEntity &&
            operation.getOp().trim().equalsIgnoreCase(OPERATION_ADD) &&
            operation.getPath().trim().toLowerCase().equalsIgnoreCase(OPERATION_PATH));
    }
}
