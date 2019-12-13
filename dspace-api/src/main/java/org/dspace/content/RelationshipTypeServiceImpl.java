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
        return relationshipTypeDAO.create(context, new RelationshipType());
    }

    @Override
    public RelationshipType create(Context context, RelationshipType relationshipType)
        throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only administrators can modify relationshipType");
        }
        return relationshipTypeDAO.create(context, relationshipType);
    }

    @Override
    public RelationshipType findbyTypesAndTypeName(Context context,EntityType leftType,EntityType rightType,
                                                 String leftwardType,String rightwardType) throws SQLException {
        return relationshipTypeDAO.findbyTypesAndTypeName(context, leftType, rightType, leftwardType, rightwardType);
    }

    @Override
    public List<RelationshipType> findAll(Context context) throws SQLException {
        return findAll(context, -1, -1);
    }

    @Override
    public List<RelationshipType> findAll(Context context, Integer limit, Integer offset) throws SQLException {

        return relationshipTypeDAO.findAll(context, RelationshipType.class, limit, offset);
    }

    @Override
    public List<RelationshipType> findByLeftwardOrRightwardTypeName(Context context, String typeName)
            throws SQLException {
        return findByLeftwardOrRightwardTypeName(context, typeName, -1, -1);
    }

    @Override
    public List<RelationshipType> findByLeftwardOrRightwardTypeName(Context context, String typeName, Integer limit,
                                                                    Integer offset)
            throws SQLException {
        return relationshipTypeDAO.findByLeftwardOrRightwardTypeName(context, typeName, limit, offset);
    }

    @Override
    public List<RelationshipType> findByEntityType(Context context, EntityType entityType) throws SQLException {
        return findByEntityType(context, entityType, -1, -1);
    }

    @Override
    public List<RelationshipType> findByEntityType(Context context, EntityType entityType,
                                                   Integer limit, Integer offset) throws SQLException {
        return relationshipTypeDAO.findByEntityType(context, entityType, limit, offset);
    }

    @Override
    public List<RelationshipType> findByEntityType(Context context, EntityType entityType, boolean isLeft)
            throws SQLException {
        return findByEntityType(context, entityType, isLeft, -1, -1);
    }

    @Override
    public List<RelationshipType> findByEntityType(Context context, EntityType entityType, boolean isLeft,
                                                   Integer limit, Integer offset) throws SQLException {
        return relationshipTypeDAO.findByEntityType(context, entityType, isLeft, limit, offset);
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
                                   Integer rightCardinalityMaxInteger, Boolean copyToLeft, Boolean copyToRight)
        throws SQLException, AuthorizeException {
        RelationshipType relationshipType = new RelationshipType();
        relationshipType.setLeftType(leftEntityType);
        relationshipType.setRightType(rightEntityType);
        relationshipType.setLeftwardType(leftwardType);
        relationshipType.setRightwardType(rightwardType);
        relationshipType.setCopyToLeft(copyToLeft);
        relationshipType.setCopyToRight(copyToRight);
        relationshipType.setLeftMinCardinality(leftCardinalityMinInteger);
        relationshipType.setLeftMaxCardinality(leftCardinalityMaxInteger);
        relationshipType.setRightMinCardinality(rightCardinalityMinInteger);
        relationshipType.setRightMaxCardinality(rightCardinalityMaxInteger);
        return create(context, relationshipType);
    }

    @Override
    public RelationshipType find(Context context,int id) throws SQLException {
        return relationshipTypeDAO.findByID(context, RelationshipType.class, id);
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
                relationshipTypeDAO.save(context, relationshipType);
            }
        }

    }

    @Override
    public void delete(Context context,RelationshipType relationshipType) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only administrators can delete entityType");
        }
        relationshipTypeDAO.delete(context, relationshipType);
    }
}
