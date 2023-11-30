/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Plugin to restrict or grant access to workspace and workflow items
 * based on the discovery configuration used.
 */
public class SolrServiceWorkspaceWorkflowRestrictionPlugin implements SolrServiceSearchPlugin {

    /**
     * The name of the discover configuration used to search for inprogress submission in the mydspace
     */
    public static final String DISCOVER_WORKSPACE_CONFIGURATION_NAME = "workspace";

    /**
     * The name of the discover configuration used to search for workflow tasks in the mydspace
     */
    public static final String DISCOVER_WORKFLOW_CONFIGURATION_NAME = "workflow";

    /**
     * The name of the discover configuration used by administrators to search for workflow tasks
     */
    public static final String DISCOVER_WORKFLOW_ADMIN_CONFIGURATION_NAME = "workflowAdmin";

    /**
     * The name of the discover configuration used by administrators to search for workspace and workflow tasks
     */
    public static final String DISCOVER_SUPERVISION_CONFIGURATION_NAME = "supervision";

    @Autowired(required = true)
    protected GroupService groupService;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Override
    public void additionalSearchParameters(
            Context context, DiscoverQuery discoveryQuery, SolrQuery solrQuery
    ) throws SearchServiceException {
        boolean isWorkspace = StringUtils.startsWith(
                discoveryQuery.getDiscoveryConfigurationName(),
                DISCOVER_WORKSPACE_CONFIGURATION_NAME
        );
        boolean isWorkflow = StringUtils.startsWith(
                discoveryQuery.getDiscoveryConfigurationName(),
                DISCOVER_WORKFLOW_CONFIGURATION_NAME
        );
        boolean isWorkflowAdmin = isAdmin(context)
                && DISCOVER_WORKFLOW_ADMIN_CONFIGURATION_NAME.equals(discoveryQuery.getDiscoveryConfigurationName());

        boolean isSupervision =
            DISCOVER_SUPERVISION_CONFIGURATION_NAME.equals(discoveryQuery.getDiscoveryConfigurationName());

        EPerson currentUser = context.getCurrentUser();

        // extra security check to avoid the possibility that an anonymous user
        // get access to workspace or workflow
        if (currentUser == null && (isWorkflow || isWorkspace || isSupervision)) {
            throw new IllegalStateException(
                    "An anonymous user cannot perform a workspace or workflow search");
        }
        if (isWorkspace) {
            // insert filter by submitter
            solrQuery.addFilterQuery("submitter_authority:(" + currentUser.getID() + ")");
        } else if ((isWorkflow && !isWorkflowAdmin) || (isSupervision && !isAdmin(context))) {
            // Retrieve all the groups the current user is a member of !
            Set<Group> groups;
            try {
                groups = groupService.allMemberGroupsSet(context, currentUser);
            } catch (SQLException e) {
                throw new SearchServiceException(e.getMessage(), e);
            }

            // insert filter by controllers
            StringBuilder controllerQuery = new StringBuilder();
            controllerQuery.append("taskfor:(e").append(currentUser.getID());
            for (Group group : groups) {
                controllerQuery.append(" OR g").append(group.getID());
            }
            controllerQuery.append(")");
            solrQuery.addFilterQuery(controllerQuery.toString());
        }
    }

    private boolean isAdmin(Context context) throws SearchServiceException {
        try {
            return authorizeService.isAdmin(context);
        } catch (SQLException e) {
            throw new SearchServiceException(e.getMessage(), e);
        }
    }
}
