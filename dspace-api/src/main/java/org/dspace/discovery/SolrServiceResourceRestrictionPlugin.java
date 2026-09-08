/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.discovery.indexobject.IndexableClaimedTask;
import org.dspace.discovery.indexobject.IndexableDSpaceObject;
import org.dspace.discovery.indexobject.IndexableInProgressSubmission;
import org.dspace.discovery.indexobject.IndexablePoolTask;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Restriction plugin that ensures that indexes all the resource policies.
 * When a search is performed extra filter queries are added to retrieve only results to which the user has the
 * required authorization.
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class SolrServiceResourceRestrictionPlugin implements SolrServiceIndexPlugin, SolrServiceSearchPlugin {

    private static final Logger log =
            org.apache.logging.log4j.LogManager.getLogger(SolrServiceResourceRestrictionPlugin.class);

    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected CommunityService communityService;
    @Autowired(required = true)
    protected CollectionService collectionService;
    @Autowired(required = true)
    protected GroupService groupService;
    @Autowired(required = true)
    protected ResourcePolicyService resourcePolicyService;
    @Autowired
    protected SearchService searchService;

    @Override
    public void additionalIndex(Context context, IndexableObject idxObj, SolrInputDocument document) {
        DSpaceObject dso = null;
        if (idxObj instanceof IndexableDSpaceObject) {
            dso = ((IndexableDSpaceObject) idxObj).getIndexedObject();
        } else if (idxObj instanceof IndexableInProgressSubmission) {
            final InProgressSubmission inProgressSubmission
                    = ((IndexableInProgressSubmission) idxObj).getIndexedObject();
            dso = inProgressSubmission.getItem();
        } else if (idxObj instanceof IndexablePoolTask) {
            final PoolTask poolTask = ((IndexablePoolTask) idxObj).getIndexedObject();
            dso = poolTask.getWorkflowItem().getItem();
        } else if (idxObj instanceof IndexableClaimedTask) {
            final ClaimedTask claimedTask = ((IndexableClaimedTask) idxObj).getIndexedObject();
            dso = claimedTask.getWorkflowItem().getItem();
        }
        if (dso != null) {
            try {
                // Index read, submit, edit and admin permissions
                int[] actionsToIndex = new int[] { Constants.READ, Constants.WRITE, Constants.ADD, Constants.ADMIN };

                for (int action : actionsToIndex) {
                    String indexedActionName = getIndexedActionName(action);
                    List<ResourcePolicy> policies = authorizeService.getPoliciesActionFilter(context, dso, action);
                    for (ResourcePolicy resourcePolicy : policies) {
                        if (resourcePolicyService.isDateValid(resourcePolicy)) {
                            String fieldValue;
                            // Avoid NPE in cases where the policy does not have group or eperson
                            if (resourcePolicy.getGroup() == null && resourcePolicy.getEPerson() == null) {
                                continue;
                            }
                            if (resourcePolicy.getGroup() != null) {
                                //We have a group add it to the value
                                fieldValue = "g" + resourcePolicy.getGroup().getID();
                            } else {
                                //We have an eperson add it to the value
                                fieldValue = "e" + resourcePolicy.getEPerson().getID();
                            }
                            document.addField(indexedActionName, fieldValue);
                        }

                        //remove the policy from the cache to save memory
                        context.uncacheEntity(resourcePolicy);
                    }
                }
            } catch (SQLException e) {
                log.error(LogHelper.getHeader(context, "Error while indexing resource policies",
                                               "DSpace object: (id " + dso.getID() + " type " + dso.getType() + ")"
                ));
            }
        }
    }

    @Override
    public void additionalSearchParameters(Context context, DiscoverQuery discoveryQuery, SolrQuery solrQuery) {
        try {
            if (!authorizeService.isAdmin(context)) {

                EPerson currentUser = context.getCurrentUser();
                StringBuilder epersonAndGroupClause = new StringBuilder();
                if (currentUser != null) {
                    epersonAndGroupClause.append("e").append(currentUser.getID());
                }
                //Retrieve all the groups the current user is a member of !
                Set<Group> groups = groupService.allMemberGroupsSet(context, currentUser);
                for (Group group : groups) {
                    if (!epersonAndGroupClause.isEmpty()) {
                        epersonAndGroupClause.append(" OR g").append(group.getID());
                    } else {
                        epersonAndGroupClause.append("g").append(group.getID());
                    }
                }

                StringBuilder resourceQuery = new StringBuilder();

                List<Integer> actions  = discoveryQuery.getRequiredAuthorizations();
                /*
                 * The `actions` list specifies the permissions required beyond the default "read" permission.
                 * It should not include "read" because checking for "read" is always implicit.
                 *
                 * The query is constructed as follows:
                 * - If no actions are provided, it checks only for "read" or "admin" permissions.
                 * - If "admin" is in the `actions` list, it checks only for admin permissions.
                 * - Otherwise, it checks for both "read" and the other specified actions.
                 *
                 * The resulting query follows this structure: (read AND action) OR admin.
                 */
                if (actions.isEmpty()) {
                    // If no actions are included, we only check for read permissions
                    resourceQuery.append("(read:(").append(epersonAndGroupClause).append("))").append( " OR ")
                        .append("admin:(").append(epersonAndGroupClause).append(")");
                } else if (actions.contains(Constants.ADMIN)) {
                    // If the actions array contains the admin action, we only check for admin permissions
                    resourceQuery.append("admin:(").append(epersonAndGroupClause).append(")");
                } else {
                    // If the actions array contains other actions, we check for read permissions and the actions passed
                    resourceQuery.append("(read:(").append(epersonAndGroupClause).append(")");
                    for (int action : actions) {
                        String actionName = getIndexedActionName(action);
                        resourceQuery.append(" AND ").append(actionName).append(":(").append(epersonAndGroupClause)
                            .append(")");
                    }
                    resourceQuery.append(")");
                    resourceQuery.append(" OR ").append("admin:(")
                        .append(epersonAndGroupClause).append(")");
                }

                // Add to the query the locations the user has administrative rights on to cover the cases of
                // inherited permissions only if the inherit authorizations flag is enabled
                if (discoveryQuery.isInheritAuthorizationsEnabled()) {
                    String locations = searchService
                        .createLocationQueryForAdministrableDSOs(epersonAndGroupClause.toString());

                    if (StringUtils.isNotBlank(locations)) {
                        resourceQuery.append(" OR ");
                        resourceQuery.append(locations);
                    }
                }

                if (discoveryQuery.isIncludeNotDiscoverableOrWithdrawn()) {
                    resourceQuery.append(" OR ");
                    resourceQuery.append("withdrawn: true");
                }

                solrQuery.addFilterQuery(resourceQuery.toString());
            }
        } catch (SQLException e) {
            log.error(LogHelper.getHeader(context, "Error while adding resource policy information to query", ""), e);
        }
    }

    /**
     * Get the action name used for solr indexing for the given action id
     *
     * @param action action id
     * @return solr action name used for indexing
     */
    private String getIndexedActionName(int action) {

        switch (action) {
            case Constants.READ:
                return "read";
            case Constants.WRITE:
                return "edit";
            case Constants.ADD:
                return "submit";
            case Constants.ADMIN:
                return "admin";
            default:
                return Constants.actionText[action].toLowerCase();
        }
    }
}
