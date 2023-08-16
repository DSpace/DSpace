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
import org.dspace.app.ldn.NotifyServiceInboundPattern;
import org.dspace.app.ldn.service.NotifyServiceInboundPatternService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for NotifyService Inbound patterns patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/ldn/ldnservices/<:id-notifyService> -H "
 * Content-Type: application/json" -d '
 * [{
 *  "op": "replace",
 *  "path": "notifyservices_inbound_patterns",
 *  "value": {"pattern":"patternA","constraint":"itemFilterA","automatic":"false"}
 *  }]'
 * </code>
 */
@Component
public class NotifyServiceInboundReplaceOperation<R> extends PatchOperation<R> {

    @Autowired
    private NotifyServiceInboundPatternService inboundPatternService;

    private static final String OPERATION_PATH = "notifyservices_inbound_patterns";

    @Override
    public R perform(Context context, R object, Operation operation) {
        checkOperationValue(operation.getValue());
        if (supports(object, operation)) {
            NotifyServiceEntity notifyServiceEntity = (NotifyServiceEntity) object;

            ObjectMapper mapper = new ObjectMapper();
            try {
                NotifyServiceInboundPattern patchInboundPattern = mapper.readValue((String) operation.getValue(),
                    NotifyServiceInboundPattern.class);

                NotifyServiceInboundPattern persistInboundPattern = inboundPatternService.findByServiceAndPattern(
                    context, notifyServiceEntity, patchInboundPattern.getPattern());

                if (persistInboundPattern == null) {
                    NotifyServiceInboundPattern c =
                        inboundPatternService.create(context, notifyServiceEntity);
                    c.setPattern(patchInboundPattern.getPattern());
                    c.setConstraint(patchInboundPattern.getConstraint());
                    c.setAutomatic(patchInboundPattern.isAutomatic());
                } else {
                    persistInboundPattern.setConstraint(patchInboundPattern.getConstraint());
                    persistInboundPattern.setAutomatic(patchInboundPattern.isAutomatic());
                    inboundPatternService.update(context, persistInboundPattern);
                }
            } catch (SQLException | JsonProcessingException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            return object;
        } else {
            throw new DSpaceBadRequestException(
                "NotifyServiceInboundReplaceOperation does not support this operation");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof NotifyServiceEntity &&
            operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE) &&
            operation.getPath().trim().equalsIgnoreCase(OPERATION_PATH));
    }
}
