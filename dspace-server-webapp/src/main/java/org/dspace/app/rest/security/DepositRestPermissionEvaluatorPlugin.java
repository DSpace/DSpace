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
import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
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
 * This plugin loads the workspace item by that ID and allows deposit if
 * the current user is either the original submitter or a member of the
 * collection's submitters group. This means:
 * <ul>
 *   <li>The original submitter can always deposit.</li>
 *   <li>Any submitter of the same collection (shared workspace) can deposit.</li>
 *   <li>Authors who are also submitters of the same collection can deposit.</li>
 *   <li>Authors who are not submitters of the collection cannot deposit.</li>
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
    private GroupService groupService;

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

            // Allow deposit if the user is the original submitter
            if (ePerson.equals(workspaceItem.getSubmitter())) {
                return true;
            }

            // Allow deposit if the user is in the collection's submitters group
            Collection collection = workspaceItem.getCollection();
            if (collection != null) {
                Group submitters = collection.getSubmitters();
                return submitters != null && groupService.isMember(context, ePerson, submitters);
            }

            return false;
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

        return false;
    }
}
