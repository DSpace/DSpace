/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.QAEventRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.QAEvent;
import org.dspace.core.Context;
import org.dspace.qaevent.service.QAEventSecurityService;
import org.dspace.qaevent.service.QAEventService;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * This class will handle Permissions for the {@link QAEventRest} object and its calls
 *
 * @author Andrea Bollini (4Science)
 */
@Component
public class QAEventRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private QAEventService qaEventService;

    @Autowired
    private QAEventSecurityService qaEventSecurityService;

    @Autowired
    private RequestService requestService;

    /**
     * Responsible for checking whether or not the user has access to the requested QASource
     *
     * @param targetType the type of Rest Object that should be checked for permission. This class would deal only with
     * qaevent
     * @param targetId string to extract the sourcename from
     */
    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
            DSpaceRestPermission restPermission) {
        if (StringUtils.equalsIgnoreCase(QAEventRest.NAME, targetType)) {
            log.debug("Checking permission for targetId {}", targetId);
            Request request = requestService.getCurrentRequest();
            Context context = ContextUtil.obtainContext(request.getHttpServletRequest());
            if (Objects.isNull(targetId)) {
                return true;
            }
            QAEvent qaEvent = qaEventService.findEventByEventId(targetId.toString());
            // everyone is expected to be able to see a not existing event (so we can return not found)
            if ((qaEvent == null
                    || qaEventSecurityService.canSeeEvent(context, context.getCurrentUser(), qaEvent))) {
                return true;
            }
        }
        return false;
    }

}