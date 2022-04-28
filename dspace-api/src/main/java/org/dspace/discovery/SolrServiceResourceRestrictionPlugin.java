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
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
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
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Restriction plugin that ensures that indexes all the resource policies.
 * When a search is performed extra filter queries are added to retrieve only results to which the user has READ access
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
                List<ResourcePolicy> policies = authorizeService.getPoliciesActionFilter(context, dso, Constants.READ);
                for (ResourcePolicy resourcePolicy : policies) {
                    if (resourcePolicyService.isDateValid(resourcePolicy)) {
                        String fieldValue;
                        if (resourcePolicy.getGroup() != null) {
                            //We have a group add it to the value
                            fieldValue = "g" + resourcePolicy.getGroup().getID();
                        } else {
                            //We have an eperson add it to the value
                            fieldValue = "e" + resourcePolicy.getEPerson().getID();

                        }

                        document.addField("read", fieldValue);
                    }

                    //remove the policy from the cache to save memory
                    context.uncacheEntity(resourcePolicy);
                }
                 // also index ADMIN policies as ADMIN permissions provides READ access
                // going up through the hierarchy for communities, collections and items
                while (dso != null) {
                    if (dso instanceof Community || dso instanceof Collection || dso instanceof Item) {
                        List<ResourcePolicy> policiesAdmin = authorizeService
                                     .getPoliciesActionFilter(context, dso, Constants.ADMIN);
                        for (ResourcePolicy resourcePolicy : policiesAdmin) {
                            if (resourcePolicyService.isDateValid(resourcePolicy)) {
                                String fieldValue;
                                if (resourcePolicy.getGroup() != null) {
                                    // We have a group add it to the value
                                    fieldValue = "g" + resourcePolicy.getGroup().getID();
                                } else {
                                    // We have an eperson add it to the value
                                    fieldValue = "e" + resourcePolicy.getEPerson().getID();
                                }
                                document.addField("read", fieldValue);
                                document.addField("admin", fieldValue);
                            }

                            // remove the policy from the cache to save memory
                            context.uncacheEntity(resourcePolicy);
                        }
                    }
                    dso = ContentServiceFactory.getInstance().getDSpaceObjectService(dso).getParentObject(context, dso);
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
                StringBuilder resourceQuery = new StringBuilder();
                //Always add the anonymous group id to the query
                Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);
                String anonGroupId = "";
                if (anonymousGroup != null) {
                    anonGroupId = anonymousGroup.getID().toString();
                }
                resourceQuery.append("read:(g" + anonGroupId);
                EPerson currentUser = context.getCurrentUser();
                if (currentUser != null) {
                    resourceQuery.append(" OR e").append(currentUser.getID());
                }

                //Retrieve all the groups the current user is a member of !
                Set<Group> groups = groupService.allMemberGroupsSet(context, currentUser);
                for (Group group : groups) {
                    resourceQuery.append(" OR g").append(group.getID());
                }

                resourceQuery.append(")");

                String locations = DSpaceServicesFactory.getInstance()
                                                          .getServiceManager()
                                                          .getServiceByName(SearchService.class.getName(),
                                                                            SearchService.class)
                                                          .createLocationQueryForAdministrableItems(context);

                if (StringUtils.isNotBlank(locations)) {
                    resourceQuery.append(" OR ");
                    resourceQuery.append(locations);
                }

                solrQuery.addFilterQuery(resourceQuery.toString());
            }
        } catch (SQLException e) {
            log.error(LogHelper.getHeader(context, "Error while adding resource policy information to query", ""), e);
        }
    }
}
