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
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.repository.patch.operation.PatchOperation;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for NotifyService Inbound patterns Replace All patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/ldn/ldnservices/<:id-notifyService> -H "
 * Content-Type: application/json" -d '
 * [{
 *  "op": "replace",
 *  "path": "notifyServiceInboundPatterns",
 *  "value": [{"pattern":"patternA","constraint":"itemFilterA","automatic":"false"}]
 *  }]'
 * </code>
 */
@Component
public class NotifyServiceInboundPatternsReplaceOperation extends PatchOperation<NotifyServiceEntity> {

    @Autowired
    private NotifyServiceInboundPatternService inboundPatternService;

    @Autowired
    private NotifyServicePatchUtils notifyServicePatchUtils;

    private static final String OPERATION_PATH = NOTIFY_SERVICE_INBOUND_PATTERNS;

    @Override
    public NotifyServiceEntity perform(Context context, NotifyServiceEntity notifyServiceEntity, Operation operation) {
        checkOperationValue(operation.getValue());
        if (supports(notifyServiceEntity, operation)) {
            try {
                List<NotifyServiceInboundPattern> patchInboundPatterns =
                    notifyServicePatchUtils.extractNotifyServiceInboundPatternsFromOperation(operation);

                notifyServiceEntity.getInboundPatterns().forEach(inboundPattern -> {
                    try {
                        inboundPatternService.delete(context, inboundPattern);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });

                for (NotifyServiceInboundPattern patchInboundPattern : patchInboundPatterns) {
                    NotifyServiceInboundPattern inboundPattern =
                        inboundPatternService.create(context, notifyServiceEntity);
                    inboundPattern.setPattern(patchInboundPattern.getPattern());
                    inboundPattern.setConstraint(patchInboundPattern.getConstraint());
                    inboundPattern.setAutomatic(patchInboundPattern.isAutomatic());
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            return notifyServiceEntity;
        } else {
            throw new DSpaceBadRequestException(
                "NotifyServiceInboundPatternsReplaceOperation does not support this operation");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof NotifyServiceEntity &&
            operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE) &&
            operation.getPath().trim().equalsIgnoreCase(OPERATION_PATH));
    }
}
