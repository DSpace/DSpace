/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.FacetParams;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.EntityTypeDAO;
import org.dspace.content.service.EntityTypeService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SolrSearchCore;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;

public class EntityTypeServiceImpl implements EntityTypeService {

    @Autowired(required = true)
    protected EntityTypeDAO entityTypeDAO;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Autowired
    protected GroupService groupService;

    @Autowired
    protected SolrSearchCore solrSearchCore;

    @Override
    public EntityType findByEntityType(Context context, String entityType) throws SQLException {
        return entityTypeDAO.findByEntityType(context, entityType);
    }

    @Override
    public List<EntityType> findAll(Context context) throws SQLException {

        return findAll(context, -1, -1);
    }

    @Override
    public List<EntityType> findAll(Context context, Integer limit, Integer offset) throws SQLException {

        return entityTypeDAO.findAll(context, EntityType.class, limit, offset);
    }

    @Override
    public EntityType create(Context context) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only administrators can modify entityType");
        }
        return entityTypeDAO.create(context, new EntityType());
    }

    @Override
    public EntityType create(Context context, String entityTypeString) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only administrators can modify entityType");
        }
        EntityType entityType = new EntityType();
        entityType.setLabel(entityTypeString);
        return entityTypeDAO.create(context, entityType);
    }

    @Override
    public EntityType find(Context context,int id) throws SQLException {
        EntityType entityType = entityTypeDAO.findByID(context, EntityType.class, id);
        return entityType;
    }

    @Override
    public void update(Context context,EntityType entityType) throws SQLException, AuthorizeException {
        update(context,Collections.singletonList(entityType));
    }

    @Override
    public void update(Context context,List<EntityType> entityTypes) throws SQLException, AuthorizeException {
        if (CollectionUtils.isNotEmpty(entityTypes)) {
            // Check authorisation - only administrators can change formats
            if (!authorizeService.isAdmin(context)) {
                throw new AuthorizeException(
                    "Only administrators can modify entityType");
            }

            for (EntityType entityType : entityTypes) {
                entityTypeDAO.save(context, entityType);
            }
        }
    }

    @Override
    public void delete(Context context,EntityType entityType) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only administrators can delete entityType");
        }
        entityTypeDAO.delete(context, entityType);
    }

    @Override
    public List<String> getSubmitAuthorizedTypes(Context context)
            throws SQLException, SolrServerException, IOException {
        List<String> types = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        org.dspace.eperson.EPerson currentUser = context.getCurrentUser();
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
        QueryResponse qResp = solrSearchCore.getSolr().query(sQuery);
        FacetField facetField = qResp.getFacetField("search.entitytype");
        if (Objects.nonNull(facetField)) {
            for (Count c : facetField.getValues()) {
                types.add(c.getName());
            }
        }
        return types;
    }

    @Override
    public List<EntityType> getEntityTypesByNames(Context context, List<String> names, Integer limit, Integer offset)
            throws SQLException {
        return entityTypeDAO.getEntityTypesByNames(context, names, limit, offset);
    }

    @Override
    public int countEntityTypesByNames(Context context, List<String> names) throws SQLException {
        return entityTypeDAO.countEntityTypesByNames(context, names);
    }

    @Override
    public void initDefaultEntityTypeNames(Context context) throws SQLException, AuthorizeException {
        EntityType noneEntityType = this.findByEntityType(context, Constants.ENTITY_TYPE_NONE);
        if (Objects.isNull(noneEntityType)) {
            noneEntityType = this.create(context, Constants.ENTITY_TYPE_NONE);
            this.update(context, noneEntityType);
        }
    }

}
