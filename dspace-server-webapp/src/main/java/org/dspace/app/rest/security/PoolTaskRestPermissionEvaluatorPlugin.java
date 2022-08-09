/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.PoolTaskRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * An authenticated user is allowed to interact with a pool task only if it is in his list.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class PoolTaskRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LoggerFactory.getLogger(PoolTaskRestPermissionEvaluatorPlugin.class);

    @Autowired
    private RequestService requestService;

    @Autowired
    private PoolTaskService poolTaskService;

    @Autowired
    private EPersonService ePersonService;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId,
                                 String targetType, DSpaceRestPermission permission) {

        if (!StringUtils.equalsIgnoreCase(PoolTaskRest.NAME, targetType)) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getHttpServletRequest());
        EPerson ePerson = null;
        try {
            ePerson = ePersonService.findByEmail(context, (String) authentication.getPrincipal());
            if (ePerson == null) {
                return false;
            }
            int dsoId = Integer.parseInt(targetId.toString());

            PoolTask poolTask = poolTaskService.find(context, dsoId);
            // If the pool task is null then we give permission so we can throw another status code instead
            if (poolTask == null) {
                return true;
            }

            XmlWorkflowItem workflowItem = poolTask.getWorkflowItem();

            PoolTask poolTask2 = poolTaskService.findByWorkflowIdAndEPerson(context, workflowItem, ePerson);
            if (poolTask2 != null && poolTask2.getID().equals(poolTask.getID())) {
                return true;
            }
        } catch (SQLException | AuthorizeException | IOException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }
}
