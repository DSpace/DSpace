/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.NotifyServiceOutboundPattern;
import org.dspace.app.ldn.service.NotifyServiceOutboundPatternService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for NotifyService Outbound patterns patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/ldn/ldnservices/<:id-notifyService> -H "
 * Content-Type: application/json" -d '
 * [{
 *  "op": "replace",
 *  "path": "notifyservices_outbound_patterns",
 *  "value": {"pattern":"patternA","constraint":"itemFilterA"}
 *  }]'
 * </code>
 */
@Component
public class NotifyServiceOutboundReplaceOperation<R> extends PatchOperation<R> {

    @Autowired
    private NotifyServiceOutboundPatternService outboundPatternService;

    private static final String OPERATION_PATH = "notifyservices_outbound_patterns";

    @Override
    public R perform(Context context, R object, Operation operation) {
        checkOperationValue(operation.getValue());
        if (supports(object, operation)) {
            NotifyServiceEntity notifyServiceEntity = (NotifyServiceEntity) object;

            ObjectMapper mapper = new ObjectMapper();
            try {
                NotifyServiceOutboundPattern patchOutboundPattern = mapper.readValue((String) operation.getValue(),
                    NotifyServiceOutboundPattern.class);

                NotifyServiceOutboundPattern persistOutboundPattern = outboundPatternService.findByServiceAndPattern(
                    context, notifyServiceEntity, patchOutboundPattern.getPattern());

                if (persistOutboundPattern == null) {
                    NotifyServiceOutboundPattern c =
                        outboundPatternService.create(context, notifyServiceEntity);
                    c.setPattern(patchOutboundPattern.getPattern());
                    c.setConstraint(patchOutboundPattern.getConstraint());
                } else {
                    persistOutboundPattern.setConstraint(patchOutboundPattern.getConstraint());
                    outboundPatternService.update(context, persistOutboundPattern);
                }
            } catch (SQLException | JsonProcessingException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            return object;
        } else {
            throw new DSpaceBadRequestException(
                "NotifyServiceOutboundReplaceOperation does not support this operation");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof NotifyServiceEntity &&
            operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE) &&
            operation.getPath().trim().equalsIgnoreCase(OPERATION_PATH));
    }
}
