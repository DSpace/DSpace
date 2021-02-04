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
import org.dspace.app.nbevent.NBEventActionService;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.content.NBEvent;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NBEventStatusReplaceOperation extends PatchOperation<NBEvent> {
    @Autowired
    private RequestService requestService;

    @Autowired
    private NBEventActionService nbEventActionService;

    @Override
    public NBEvent perform(Context context, NBEvent nbevent, Operation operation) throws SQLException {
        String value = (String) operation.getValue();
        if (StringUtils.equalsIgnoreCase(value, NBEvent.ACCEPTED)) {
            nbEventActionService.accept(context, nbevent);
        } else if (StringUtils.equalsIgnoreCase(value, NBEvent.REJECTED)) {
            nbEventActionService.reject(context, nbevent);
        } else if (StringUtils.equalsIgnoreCase(value, NBEvent.DISCARDED)) {
            nbEventActionService.discard(context, nbevent);
        } else {
            throw new IllegalArgumentException(
                    "The received operation is not valid: " + operation.getPath() + " - " + value);
        }
        nbevent.setStatus(value.toUpperCase());
        // HACK, we need to store the temporary object in the request so that a subsequent find would get it
        requestService.getCurrentRequest().setAttribute("patchedNotificationEvent", nbevent);
        return nbevent;
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return StringUtils.equals(operation.getOp(), "replace") && objectToMatch instanceof NBEvent && StringUtils
                .containsAny(operation.getValue().toString().toLowerCase(), NBEvent.ACCEPTED, NBEvent.DISCARDED,
                        NBEvent.REJECTED);
    }
}
