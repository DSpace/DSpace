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
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.repository.patch.operation.PatchOperation;
import org.dspace.core.Context;
import org.springframework.stereotype.Component;

/**
 * Implementation for NotifyService LowerIp Or UpperIp Remove patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/ldn/ldnservices/<:id-notifyService> -H "
 * Content-Type: application/json" -d '
 * [{
 *  "op": "remove",
 *  "path": "/lowerIp"
 *  }]'
 * </code>
 */
@Component
public class NotifyServiceLowerOrUpperRemoveOperation extends PatchOperation<NotifyServiceEntity> {

    @Override
    public NotifyServiceEntity perform(Context context, NotifyServiceEntity notifyServiceEntity, Operation operation)
        throws SQLException {
        throw new UnprocessableEntityException("/lowerIp or /upperIp are mandatory and can't be removed");
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof NotifyServiceEntity &&
            operation.getOp().trim().equalsIgnoreCase(OPERATION_REMOVE) &&
            (operation.getPath().trim().toLowerCase().equalsIgnoreCase("/lowerip") ||
                operation.getPath().trim().toLowerCase().equalsIgnoreCase("/upperip")));
    }
}
