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
import org.dspace.app.rest.model.WorkflowItemRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * An authenticated user is allowed to interact with workflow item only if they belong to a task that she own or could
 * claim.
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class WorkflowRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LoggerFactory.getLogger(WorkflowRestPermissionEvaluatorPlugin.class);

    @Autowired
    private RequestService requestService;

    @Autowired
    private XmlWorkflowItemService workflowItemService;

    @Autowired
    private PoolTaskService poolTaskService;

    @Autowired
    private ClaimedTaskService claimedTaskService;

    @Autowired
    private EPersonService ePersonService;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId,
                                 String targetType, DSpaceRestPermission permission) {

        //This plugin currently only evaluates READ access
        DSpaceRestPermission restPermission = DSpaceRestPermission.convert(permission);
        if (!DSpaceRestPermission.READ.equals(restPermission)
                || !StringUtils.equalsIgnoreCase(WorkflowItemRest.NAME, targetType)) {
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
            XmlWorkflowItem workflowItem = workflowItemService.find(context, dsoId);
            // submitter can see their inprogress submission
            if (ePerson.equals(workflowItem.getSubmitter())) {
                return true;
            }

            if (poolTaskService.findByWorkflowIdAndEPerson(context, workflowItem, ePerson) != null) {
                return true;
            }

            if (claimedTaskService.findByWorkflowIdAndEPerson(context, workflowItem, ePerson) != null) {
                return true;
            }
        } catch (SQLException | AuthorizeException | IOException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }
}
