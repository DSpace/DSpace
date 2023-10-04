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
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.repository.patch.operation.PatchOperation;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for NotifyService Outbound patterns Replace All patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/ldn/ldnservices/<:id-notifyService> -H "
 * Content-Type: application/json" -d '
 * [{
 *  "op": "replace",
 *  "path": "notifyServiceOutboundPatterns",
 *  "value": [{"pattern":"patternA","constraint":"itemFilterA"}]
 *  }]'
 * </code>
 */
@Component
public class NotifyServiceOutboundPatternsReplaceOperation extends PatchOperation<NotifyServiceEntity> {

    @Autowired
    private NotifyServiceOutboundPatternService outboundPatternService;

    @Autowired
    private NotifyServicePatchUtils notifyServicePatchUtils;

    private static final String OPERATION_PATH = NOTIFY_SERVICE_OUTBOUND_PATTERNS;

    @Override
    public NotifyServiceEntity perform(Context context, NotifyServiceEntity notifyServiceEntity, Operation operation) {
        checkOperationValue(operation.getValue());
        if (supports(notifyServiceEntity, operation)) {
            try {
                List<NotifyServiceOutboundPattern> patchOutboundPatterns =
                    notifyServicePatchUtils.extractNotifyServiceOutboundPatternsFromOperation(operation);

                notifyServiceEntity.getOutboundPatterns().forEach(outboundPattern -> {
                    try {
                        outboundPatternService.delete(context, outboundPattern);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });

                for (NotifyServiceOutboundPattern patchOutboundPattern : patchOutboundPatterns) {
                    NotifyServiceOutboundPattern outboundPattern =
                        outboundPatternService.create(context, notifyServiceEntity);
                    outboundPattern.setPattern(patchOutboundPattern.getPattern());
                    outboundPattern.setConstraint(patchOutboundPattern.getConstraint());
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            return notifyServiceEntity;
        } else {
            throw new DSpaceBadRequestException(
                "NotifyServiceOutboundPatternsReplaceOperation does not support this operation");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof NotifyServiceEntity &&
            operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE) &&
            operation.getPath().trim().equalsIgnoreCase(OPERATION_PATH));
    }
}
