/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation.ldn;

import static org.dspace.app.rest.repository.patch.operation.ldn.NotifyServicePatchUtils.NOTIFY_SERVICE_OUTBOUND_PATTERNS;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.NotifyServiceOutboundPattern;
import org.dspace.app.ldn.service.NotifyServiceOutboundPatternService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.repository.patch.operation.PatchOperation;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for NotifyService Outbound patterns Constraint Add patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/ldn/ldnservices/<:id-notifyService> -H "
 * Content-Type: application/json" -d '
 * [{
 *  "op": "add",
 *  "path": "notifyservices_outbound_patterns[index]/constraint"
 *  }]'
 * </code>
 */
@Component
public class NotifyServiceOutboundPatternConstraintAddOperation extends PatchOperation<NotifyServiceEntity> {

    @Autowired
    private NotifyServiceOutboundPatternService outboundPatternService;

    @Autowired
    private NotifyServicePatchUtils notifyServicePatchUtils;

    private static final String OPERATION_PATH = "/constraint";

    @Override
    public NotifyServiceEntity perform(Context context, NotifyServiceEntity notifyServiceEntity, Operation operation) {
        checkOperationValue(operation.getValue());
        if (supports(notifyServiceEntity, operation)) {
            try {
                int index = notifyServicePatchUtils.extractIndexFromOperation(operation);

                List<NotifyServiceOutboundPattern> outboundPatterns = notifyServiceEntity.getOutboundPatterns();

                if (index >= outboundPatterns.size()) {
                    throw new DSpaceBadRequestException("the provided index[" + index + "] is out of the rang");
                }

                NotifyServiceOutboundPattern outboundPattern = outboundPatterns.get(index);
                Object constraint = operation.getValue();
                if (constraint == null | !(constraint instanceof String)) {
                    throw new UnprocessableEntityException("The /constraint value must be a string");
                }

                checkNonExistingConstraintValue(outboundPattern);
                outboundPattern.setConstraint((String) constraint);
                outboundPatternService.update(context, outboundPattern);
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            return notifyServiceEntity;
        } else {
            throw new DSpaceBadRequestException(
                "NotifyServiceOutboundPatternConstraintAddOperation does not support this operation");
        }
    }

    private void checkNonExistingConstraintValue(NotifyServiceOutboundPattern outboundPattern) {
        if (outboundPattern.getConstraint() != null) {
            throw new DSpaceBadRequestException("Attempting to add a value to an already existing path.");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        String path = operation.getPath().trim().toLowerCase();
        return (objectToMatch instanceof NotifyServiceEntity &&
            operation.getOp().trim().equalsIgnoreCase(OPERATION_ADD) &&
            path.startsWith(NOTIFY_SERVICE_OUTBOUND_PATTERNS + "[") &&
            path.endsWith(OPERATION_PATH));
    }
}
