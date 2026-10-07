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
import org.dspace.app.rest.model.WorkspaceItemRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Permission evaluator plugin for workspace items in shared workspace collections.
 * <p>
 * This plugin enables <strong>shared workspace functionality</strong> where multiple users can collaborate
 * on workspace items during the submission process, even when they are not the original submitter.
 *
 * <p>
 * <strong>Configuration:</strong>
 * <p>
 * Shared workspace is configured per collection and affects resource policy creation. When enabled:
 * <ul>
 *   <li>Workspace items get resource policies for the collection's submitters GROUP (not just individual)</li>
 *   <li>Discovery configuration "otherWorkspace" (discovery.xml) allows searching accessible items</li>
 *   <li>Policy count: 10 policies (5 individual + 5 group) vs 5 policies (individual only) without sharing</li>
 * </ul>
 *
 * <p>
 * <strong>Example: Shared Workspace Collaboration</strong>
 * <pre>
 * Collection: "Publications" with shared workspace enabled
 * Submitters Group: [Alice, Bob, Charlie]
 *
 * Alice creates WorkspaceItem #123
 * Resource Policies on Item:
 *   - READ, WRITE, DELETE, REMOVE, ADD for Alice (individual)
 *   - READ, WRITE, DELETE, REMOVE, ADD for Submitters Group
 *
 * Permission Checks:
 *
 * 1. Alice (original submitter) tries to WRITE to item #123
 *    → hasDSpacePermission(alice, 123, "workspaceitem", WRITE)
 *    → authorizeActionBoolean checks policies
 *    → Alice has individual WRITE policy
 *    → Returns TRUE → Alice can modify item
 *
 * 2. Bob (other submitter) tries to DELETE item #123
 *    → hasDSpacePermission(bob, 123, "workspaceitem", DELETE)
 *    → authorizeActionBoolean checks policies
 *    → Bob has DELETE policy via Submitters Group membership
 *    → Returns TRUE → Bob can delete Alice's item (shared workspace)
 *
 * 3. David (not a submitter) tries to READ item #123
 *    → hasDSpacePermission(david, 123, "workspaceitem", READ)
 *    → authorizeActionBoolean checks policies
 *    → David has NO matching READ policy
 *    → David is not admin
 *    → Returns FALSE → HTTP 403 Forbidden
 *
 * 4. Bob tries to WRITE to item #123 in NON-shared collection
 *    → authorizeActionBoolean checks policies
 *    → Item has policies ONLY for Alice (no group policies)
 *    → Bob has NO matching WRITE policy
 *    → Returns FALSE → HTTP 403 Forbidden
 *
 * 5. Anonymous user tries to READ item #123
 *    → ePerson == null
 *    → Returns FALSE immediately → HTTP 401 Unauthorized
 * </pre>
 * <p>
 * <strong>Integration:</strong>
 * <p>
 * This plugin delegates to {@link AuthorizeService#authorizeActionBoolean} which checks both
 * direct resource policies and Solr-indexed permissions. The Solr "read" field is populated by
 * {@link org.dspace.discovery.SharedWorkspaceSolrIndexPlugin} for ownership scenarios.
 *
 * @see org.dspace.discovery.SharedWorkspaceSolrIndexPlugin
 * @see org.dspace.content.WorkspaceItemServiceImpl
 * @see org.dspace.authorize.service.AuthorizeService#authorizeActionBoolean
 */
@Component
public class OtherWorkspaceItemRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {
    private static final Logger log = LoggerFactory.getLogger(OtherWorkspaceItemRestPermissionEvaluatorPlugin.class);

    @Autowired
    private RequestService requestService;
    @Autowired
    private WorkspaceItemService wis;
    @Autowired
    private AuthorizeService authorizeService;

    /**
     * Checks if the current user has permission to perform an action on a workspace item.
     * <p>
     * <strong>Method Logic:</strong>
     * <ol>
     *   <li>Validates {@code targetType} equals "workspaceitem" - returns {@code false} if not</li>
     *   <li>Validates {@code permission} is READ, WRITE, or DELETE - returns {@code false} if not</li>
     *   <li>Retrieves current user from context - returns {@code false} if null (anonymous)</li>
     *   <li>Finds workspace item by ID - returns {@code true} if not found (allows 404 response)</li>
     *   <li>Gets underlying Item from workspace item - returns {@code true} if null (error handling)</li>
     *   <li>Delegates to {@code authorizeService.authorizeActionBoolean(context, user, item, action, true)}
     *       which checks resource policies and admin status</li>
     * </ol>
     * <p>
     * The {@code checkAdmin = true} parameter means administrators always pass authorization.
     *
     * @param authentication the Spring Security authentication (unused)
     * @param targetId the workspace item ID to check
     * @param targetType must be "workspaceitem"
     * @param permission must be READ, WRITE, or DELETE
     * @return {@code true} if authorized; {@code false} otherwise
     */
    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
                                       DSpaceRestPermission permission) {

        DSpaceRestPermission restPermission = DSpaceRestPermission.convert(permission);
        if (!StringUtils.equalsIgnoreCase(targetType, WorkspaceItemRest.NAME) ||
            (!DSpaceRestPermission.READ.equals(restPermission)
                && !DSpaceRestPermission.WRITE.equals(restPermission)
                && !DSpaceRestPermission.DELETE.equals(restPermission))) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getHttpServletRequest());

        EPerson ePerson = null;
        WorkspaceItem witem = null;
        try {
            ePerson = context.getCurrentUser();
            Integer dsoId = Integer.parseInt(targetId.toString());

            // anonymous user
            if (ePerson == null) {
                return false;
            }

            witem = wis.find(context, dsoId);

            // If the dso is null then we give permission so we can throw another status
            // code instead
            if (witem == null) {
                return true;
            }

            Item dSpaceObject = witem.getItem();
            // If the dso is null then we give permission so we can throw another status
            // code instead
            if (dSpaceObject == null) {
                return true;
            }

            return authorizeService.authorizeActionBoolean(context, ePerson, dSpaceObject,
                                                           restPermission.getDspaceApiActionId(), true);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

        return false;
    }
}
