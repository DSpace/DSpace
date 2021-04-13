/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.authorize.ResourcePolicyOwnerVO;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.indexobject.IndexableClaimedTask;
import org.dspace.discovery.indexobject.IndexableDSpaceObject;
import org.dspace.discovery.indexobject.IndexableInProgressSubmission;
import org.dspace.discovery.indexobject.IndexablePoolTask;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.factory.DSpaceServicesFactory;
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
    @SuppressWarnings("rawtypes")
    public void additionalIndex(Context context, IndexableObject indexableObject, SolrInputDocument document) {

        DSpaceObject dso = getDSpaceObject(indexableObject);
        if (dso == null) {
            return;
        }

        try {

            List<ResourcePolicyOwnerVO> policies = authorizeService
                .getValidPolicyOwnersActionFilter(context, List.of(dso.getID()), Constants.READ);

            for (ResourcePolicyOwnerVO resourcePolicy : policies) {
                addReadField(document, resourcePolicy, false);
            }

            // also index ADMIN policies as ADMIN permissions provides READ access
            // going up through the hierarchy for communities, collections and items

            List<UUID> dsoIds = new ArrayList<>();

            while (dso != null) {
                if (dso instanceof Community || dso instanceof Collection || dso instanceof Item) {
                    dsoIds.add(dso.getID());
                }
                dso = ContentServiceFactory.getInstance().getDSpaceObjectService(dso).getParentObject(context, dso);
            }

            if (!dsoIds.isEmpty()) {

                List<ResourcePolicyOwnerVO> policiesAdmin = authorizeService
                    .getValidPolicyOwnersActionFilter(context, dsoIds, Constants.ADMIN);

                for (ResourcePolicyOwnerVO resourcePolicy : policiesAdmin) {
                    addReadField(document, resourcePolicy, true);
                }

            }

        } catch (SQLException e) {
            log.error(LogManager.getHeader(context, "Error while indexing resource policies",
                "DSpace object: (id " + dso.getID() + " type " + dso.getType() + ")"));
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
            log.error(LogManager.getHeader(context, "Error while adding resource policy information to query", ""), e);
        }
    }

    @SuppressWarnings("rawtypes")
    private DSpaceObject getDSpaceObject(IndexableObject idxObj) {

        if (idxObj instanceof IndexableDSpaceObject) {
            return ((IndexableDSpaceObject) idxObj).getIndexedObject();
        } else if (idxObj instanceof IndexableInProgressSubmission) {
            return ((IndexableInProgressSubmission) idxObj).getIndexedObject().getItem();
        } else if (idxObj instanceof IndexablePoolTask) {
            return ((IndexablePoolTask) idxObj).getIndexedObject().getWorkflowItem().getItem();
        } else if (idxObj instanceof IndexableClaimedTask) {
            return ((IndexableClaimedTask) idxObj).getIndexedObject().getWorkflowItem().getItem();
        }

        return null;
    }

    private void addReadField(SolrInputDocument document, ResourcePolicyOwnerVO resourcePolicy, boolean addAdminField) {

        String fieldValue;
        if (resourcePolicy.getGroupId() != null) {
            // We have a group add it to the value
            fieldValue = "g" + resourcePolicy.getGroupId();
        } else {
            // We have an eperson add it to the value
            fieldValue = "e" + resourcePolicy.getEPersonId();
        }

        document.addField("read", fieldValue);
        if (addAdminField) {
            document.addField("admin", fieldValue);
        }
    }
}
