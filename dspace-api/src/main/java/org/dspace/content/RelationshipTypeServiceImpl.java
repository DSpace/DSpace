/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.RelationshipTypeDAO;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;

public class RelationshipTypeServiceImpl implements RelationshipTypeService {

    @Autowired(required = true)
    protected RelationshipTypeDAO relationshipTypeDAO;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Override
    public RelationshipType create(Context context) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only administrators can modify relationshipType");
        }
        return relationshipTypeDAO.create(context.getSession(), new RelationshipType());
    }

    @Override
    public RelationshipType create(Context context, RelationshipType relationshipType)
        throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only administrators can modify relationshipType");
        }
        return relationshipTypeDAO.create(context.getSession(), relationshipType);
    }

    @Override
    public RelationshipType findbyTypesAndTypeName(Session session,
            EntityType leftType, EntityType rightType,
            String leftwardType,String rightwardType) throws SQLException {
        return relationshipTypeDAO.findbyTypesAndTypeName(session,
                leftType, rightType, leftwardType, rightwardType);
    }

    @Override
    public List<RelationshipType> findAll(Session session) throws SQLException {
        return findAll(session, -1, -1);
    }

    @Override
    public List<RelationshipType> findAll(Session session, Integer limit, Integer offset) throws SQLException {
        return relationshipTypeDAO.findAll(session, RelationshipType.class, limit, offset);
    }

    @Override
    public List<RelationshipType> findByLeftwardOrRightwardTypeName(Session session, String typeName)
            throws SQLException {
        return findByLeftwardOrRightwardTypeName(session, typeName, -1, -1);
    }

    @Override
    public List<RelationshipType> findByLeftwardOrRightwardTypeName(Session session, String typeName, Integer limit,
                                                                    Integer offset)
            throws SQLException {
        return relationshipTypeDAO.findByLeftwardOrRightwardTypeName(session, typeName, limit, offset);
    }

    @Override
    public List<RelationshipType> findByEntityType(Session session, EntityType entityType) throws SQLException {
        return findByEntityType(session, entityType, -1, -1);
    }

    @Override
    public List<RelationshipType> findByEntityType(Session session, EntityType entityType,
                                                   Integer limit, Integer offset) throws SQLException {
        return relationshipTypeDAO.findByEntityType(session, entityType, limit, offset);
    }

    @Override
    public List<RelationshipType> findByEntityType(Session session, EntityType entityType, boolean isLeft)
            throws SQLException {
        return findByEntityType(session, entityType, isLeft, -1, -1);
    }

    @Override
    public List<RelationshipType> findByEntityType(Session session, EntityType entityType, boolean isLeft,
                                                   Integer limit, Integer offset) throws SQLException {
        return relationshipTypeDAO.findByEntityType(session, entityType, isLeft, limit, offset);
    }

    @Override
    public RelationshipType create(Context context, EntityType leftEntityType, EntityType rightEntityType,
                                   String leftwardType, String rightwardType, Integer leftCardinalityMinInteger,
                                   Integer leftCardinalityMaxInteger, Integer rightCardinalityMinInteger,
                                   Integer rightCardinalityMaxInteger)
        throws SQLException, AuthorizeException {
        RelationshipType relationshipType = new RelationshipType();
        relationshipType.setLeftType(leftEntityType);
        relationshipType.setRightType(rightEntityType);
        relationshipType.setLeftwardType(leftwardType);
        relationshipType.setRightwardType(rightwardType);
        relationshipType.setLeftMinCardinality(leftCardinalityMinInteger);
        relationshipType.setLeftMaxCardinality(leftCardinalityMaxInteger);
        relationshipType.setRightMinCardinality(rightCardinalityMinInteger);
        relationshipType.setRightMaxCardinality(rightCardinalityMaxInteger);
        return create(context, relationshipType);
    }

    @Override
    public RelationshipType create(Context context, EntityType leftEntityType, EntityType rightEntityType,
                                   String leftwardType, String rightwardType, Integer leftCardinalityMinInteger,
                                   Integer leftCardinalityMaxInteger, Integer rightCardinalityMinInteger,
                                   Integer rightCardinalityMaxInteger, Boolean copyToLeft, Boolean copyToRight,
                                   RelationshipType.Tilted tilted)
        throws SQLException, AuthorizeException {
        RelationshipType relationshipType = new RelationshipType();
        relationshipType.setLeftType(leftEntityType);
        relationshipType.setRightType(rightEntityType);
        relationshipType.setLeftwardType(leftwardType);
        relationshipType.setRightwardType(rightwardType);
        relationshipType.setCopyToLeft(copyToLeft);
        relationshipType.setCopyToRight(copyToRight);
        relationshipType.setTilted(tilted);
        relationshipType.setLeftMinCardinality(leftCardinalityMinInteger);
        relationshipType.setLeftMaxCardinality(leftCardinalityMaxInteger);
        relationshipType.setRightMinCardinality(rightCardinalityMinInteger);
        relationshipType.setRightMaxCardinality(rightCardinalityMaxInteger);
        return create(context, relationshipType);
    }

    @Override
    public RelationshipType find(Session session,int id) throws SQLException {
        return relationshipTypeDAO.findByID(session, RelationshipType.class, id);
    }

    @Override
    public void update(Context context,RelationshipType relationshipType) throws SQLException, AuthorizeException {
        update(context,Collections.singletonList(relationshipType));
    }

    @Override
    public void update(Context context,List<RelationshipType> relationshipTypes)
        throws SQLException, AuthorizeException {
        if (CollectionUtils.isNotEmpty(relationshipTypes)) {
            // Check authorisation - only administrators can change formats
            if (!authorizeService.isAdmin(context)) {
                throw new AuthorizeException(
                    "Only administrators can modify RelationshipType");
            }

            for (RelationshipType relationshipType : relationshipTypes) {
                relationshipTypeDAO.save(context.getSession(), relationshipType);
            }
        }

    }

    @Override
    public void delete(Context context,RelationshipType relationshipType) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only administrators can delete entityType");
        }
        relationshipTypeDAO.delete(context.getSession(), relationshipType);
    }

    @Override
    public int countByEntityType(Context context, EntityType entityType) throws SQLException {
        return relationshipTypeDAO.countByEntityType(context.getSession(), entityType);
    }
}
