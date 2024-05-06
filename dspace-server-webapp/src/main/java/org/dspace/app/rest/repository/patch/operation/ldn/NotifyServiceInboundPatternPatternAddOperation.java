/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation.ldn;

import static org.dspace.app.rest.repository.patch.operation.ldn.NotifyServicePatchUtils.NOTIFY_SERVICE_INBOUND_PATTERNS;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.NotifyServiceInboundPattern;
import org.dspace.app.ldn.service.NotifyServiceInboundPatternService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.repository.patch.operation.PatchOperation;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for NotifyService Inbound patterns Pattern Add patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/ldn/ldnservices/<:id-notifyService> -H "
 * Content-Type: application/json" -d '
 * [{
 *  "op": "add",
 *  "path": "notifyServiceInboundPatterns[index]/pattern"
 *  }]'
 * </code>
 */
@Component
public class NotifyServiceInboundPatternPatternAddOperation extends PatchOperation<NotifyServiceEntity> {

    @Autowired
    private NotifyServiceInboundPatternService inboundPatternService;

    @Autowired
    private NotifyServicePatchUtils notifyServicePatchUtils;

    private static final String OPERATION_PATH = "/pattern";

    @Override
    public NotifyServiceEntity perform(Context context, NotifyServiceEntity notifyServiceEntity, Operation operation) {
        checkOperationValue(operation.getValue());
        if (supports(notifyServiceEntity, operation)) {
            try {
                int index = notifyServicePatchUtils.extractIndexFromOperation(operation);

                List<NotifyServiceInboundPattern> inboundPatterns = notifyServiceEntity.getInboundPatterns();

                if (index >= inboundPatterns.size()) {
                    throw new DSpaceBadRequestException("the provided index[" + index + "] is out of the rang");
                }

                NotifyServiceInboundPattern inboundPattern = inboundPatterns.get(index);
                Object pattern = operation.getValue();
                if (pattern == null | !(pattern instanceof String)) {
                    throw new UnprocessableEntityException("The /pattern value must be a string");
                }

                checkNonExistingPatternValue(inboundPattern);
                inboundPattern.setPattern((String) pattern);
                inboundPatternService.update(context, inboundPattern);
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            return notifyServiceEntity;
        } else {
            throw new DSpaceBadRequestException(
                "NotifyServiceInboundPatternPatternAddOperation does not support this operation");
        }
    }

    private void checkNonExistingPatternValue(NotifyServiceInboundPattern inboundPattern) {
        if (inboundPattern.getPattern() != null) {
            throw new DSpaceBadRequestException("Attempting to add a value to an already existing path.");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        String path = operation.getPath().trim();
        return (objectToMatch instanceof NotifyServiceEntity &&
            operation.getOp().trim().equalsIgnoreCase(OPERATION_ADD) &&
            path.startsWith(NOTIFY_SERVICE_INBOUND_PATTERNS + "[") &&
            path.endsWith(OPERATION_PATH));
    }
}
