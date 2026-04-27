/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.Serializable;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.WorkflowItemRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.profile.service.ResearcherProfileService;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Evaluates WRITE (deposit) permission on the WORKFLOWITEM target type.
 * <p>
 * During deposit, the {@code @PreAuthorize} annotation on
 * {@link org.dspace.app.rest.repository.WorkflowItemRestRepository#createAndReturn}
 * passes a workspace item ID as the target ID with target type WORKFLOWITEM and
 * permission WRITE.
 * </p>
 * <p>
 * This plugin loads the workspace item by that ID and checks:
 * <ul>
 *   <li>If the current user is the author of the item, deposit is denied.</li>
 *   <li>Otherwise, deposit is allowed if the current user is the submitter.</li>
 * </ul>
 * </p>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
@Component
public class DepositRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private RequestService requestService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private ResearcherProfileService researcherProfileService;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId,
                                       String targetType, DSpaceRestPermission permission) {

        DSpaceRestPermission restPermission = DSpaceRestPermission.convert(permission);
        if (!DSpaceRestPermission.WRITE.equals(restPermission)
                || !StringUtils.equalsIgnoreCase(WorkflowItemRest.NAME, targetType)) {
            return false;
        }

        if (targetId == null) {
            throw new UnprocessableEntityException("The given WorkSpaceItem id is not valid");
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getHttpServletRequest());

        try {
            EPerson ePerson = ePersonService.findByEmail(context,
                    (String) authentication.getPrincipal());
            if (ePerson == null) {
                return false;
            }

            int dsoId = Integer.parseInt(targetId.toString());
            WorkspaceItem workspaceItem = workspaceItemService.find(context, dsoId);
            if (workspaceItem == null) {
                return false;
            }

            // Authors of the item cannot deposit it
            if (researcherProfileService.isAuthorOf(context, ePerson, workspaceItem.getItem())) {
                return false;
            }

            // Non-author submitters are allowed to deposit
            return ePerson.equals(workspaceItem.getSubmitter());
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

        return false;
    }
}
