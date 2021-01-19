/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.FacetParams;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.model.EntityTypeRest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.EntityType;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.external.service.ExternalDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository that is responsible to manage EntityType Rest objects
 */
@Component(EntityTypeRest.CATEGORY + "." + EntityTypeRest.NAME)
public class EntityTypeRestRepository extends DSpaceRestRepository<EntityTypeRest, Integer> {

    @Autowired
    private EntityTypeService entityTypeService;
    @Autowired
    private CollectionService collectionService;
    @Autowired
    private ExternalDataService externalDataService;
    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected GroupService groupService;
    @Autowired(required = true)
    protected SearchService searchService;

    @Override
    @PreAuthorize("permitAll()")
    public EntityTypeRest findOne(Context context, Integer integer) {
        try {
            EntityType entityType = entityTypeService.find(context, integer);
            if (entityType == null) {
                throw new ResourceNotFoundException("The entityType for ID: " + integer + " could not be found");
            }
            return converter.toRest(entityType, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Page<EntityTypeRest> findAll(Context context, Pageable pageable) {
        try {
            List<EntityType> entityTypes = entityTypeService.findAll(context);
            return converter.toRestPage(entityTypes, pageable, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<EntityTypeRest> getDomainClass() {
        return EntityTypeRest.class;
    }

    @SearchRestMethod(name = "findAllByAuthorizedCollection")
    public Page<EntityTypeRest> findAllByAuthorizedCollection(Pageable pageable) {
        try {
            Context context = obtainContext();
            List<String> types = getSubmitAuthorizedTypes(context);
            List<EntityType> entityTypes = types.stream().map(type -> {
                if (StringUtils.isBlank(type)) {
                    return null;
                }
                try {
                    return entityTypeService.findByEntityType(context, type);
                } catch (SQLException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }).filter(x -> x != null).collect(Collectors.toList());
            return converter.toRestPage(entityTypes, pageable, utils.obtainProjection());
        } catch (SQLException | SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SearchRestMethod(name = "findAllByAuthorizedExternalSource")
    public Page<EntityTypeRest> findAllByAuthorizedExternalSource(Pageable pageable) {
        try {
            Context context = obtainContext();
            List<String> types = getSubmitAuthorizedTypes(context);
            List<EntityType> entityTypes = types.stream()
                    .filter(x -> externalDataService.getExternalDataProvidersForEntityType(x).size() > 0)
                    .map(type -> {
                        if (StringUtils.isBlank(type)) {
                            return null;
                        }
                        try {
                            return entityTypeService.findByEntityType(context, type);
                        } catch (SQLException e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                    })
                    .filter(x -> x != null)
                    .collect(Collectors.toList());
            return converter.toRestPage(entityTypes, pageable, utils.obtainProjection());
        } catch (SQLException | SolrServerException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    private List<String> getSubmitAuthorizedTypes(Context context)
            throws SQLException, SolrServerException, IOException {
        List<String> types = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        EPerson currentUser = context.getCurrentUser();
        if (!authorizeService.isAdmin(context)) {
            String userId = "";
            if (currentUser != null) {
                userId = currentUser.getID().toString();
            }
            query.append("submit:(e").append(userId);
            Set<Group> groups = groupService.allMemberGroupsSet(context, currentUser);
            for (Group group : groups) {
                query.append(" OR g").append(group.getID());
            }
            query.append(")");
        } else {
            query.append("*:*");
        }

        SolrQuery sQuery = new SolrQuery(query.toString());
        sQuery.addFilterQuery("search.resourcetype:" + IndexableCollection.TYPE);
        sQuery.setRows(0);
        sQuery.addFacetField("search.entitytype");
        sQuery.setFacetMinCount(1);
        sQuery.setFacetLimit(Integer.MAX_VALUE);
        sQuery.setFacetSort(FacetParams.FACET_SORT_INDEX);
        QueryResponse qResp = searchService.getSolrSearchCore().getSolr().query(sQuery);
        FacetField ff = qResp.getFacetField("search.entitytype");
        if (ff != null) {
            for (Count c : ff.getValues()) {
                types.add(c.getName());
            }
        }
        return types;
    }
}
