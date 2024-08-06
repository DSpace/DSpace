/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.content.QAEvent;
import org.dspace.core.Context;
import org.dspace.qaevent.service.QAEventActionService;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Replace operation related to the {@link QAEvent} status.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component
public class QAEventStatusReplaceOperation extends PatchOperation<QAEvent> {
    @Autowired
    private RequestService requestService;

    @Autowired
    private QAEventActionService qaEventActionService;

    @Override
    public QAEvent perform(Context context, QAEvent qaevent, Operation operation) throws SQLException {
        String value = (String) operation.getValue();
        if (StringUtils.equalsIgnoreCase(value, QAEvent.ACCEPTED)) {
            qaEventActionService.accept(context, qaevent);
        } else if (StringUtils.equalsIgnoreCase(value, QAEvent.REJECTED)) {
            qaEventActionService.reject(context, qaevent);
        } else if (StringUtils.equalsIgnoreCase(value, QAEvent.DISCARDED)) {
            qaEventActionService.discard(context, qaevent);
        } else {
            throw new IllegalArgumentException(
                    "The received operation is not valid: " + operation.getPath() + " - " + value);
        }
        qaevent.setStatus(value.toUpperCase());
        // HACK, we need to store the temporary object in the request so that a subsequent find would get it
        requestService.getCurrentRequest().setAttribute("patchedNotificationEvent", qaevent);
        return qaevent;
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return StringUtils.equals(operation.getOp(), "replace") && objectToMatch instanceof QAEvent && StringUtils
                .containsAny(operation.getValue().toString().toLowerCase(), QAEvent.ACCEPTED, QAEvent.DISCARDED,
                        QAEvent.REJECTED);
    }
}
