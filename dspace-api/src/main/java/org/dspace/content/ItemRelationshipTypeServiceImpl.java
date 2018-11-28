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
import org.dspace.content.dao.ItemRelationshipTypeDAO;
import org.dspace.content.service.ItemRelationshipTypeService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

public class ItemRelationshipTypeServiceImpl implements ItemRelationshipTypeService {

    @Autowired(required = true)
    protected ItemRelationshipTypeDAO itemRelationshipTypeDAO;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    public ItemRelationshipsType findByEntityType(Context context, String entityType) throws SQLException {
        return itemRelationshipTypeDAO.findByEntityType(context, entityType);
    }

    public List<ItemRelationshipsType> findAll(Context context) throws SQLException {
        return itemRelationshipTypeDAO.findAll(context, ItemRelationshipsType.class);
    }

    public ItemRelationshipsType create(Context context) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only administrators can modify entityType");
        }
        return itemRelationshipTypeDAO.create(context, new ItemRelationshipsType());
    }

    public ItemRelationshipsType create(Context context, String entityTypeString)
        throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only administrators can modify entityType");
        }
        ItemRelationshipsType itemRelationshipsType = new ItemRelationshipsType();
        itemRelationshipsType.setLabel(entityTypeString);
        return itemRelationshipTypeDAO.create(context, itemRelationshipsType);
    }

    public ItemRelationshipsType find(Context context, int id) throws SQLException {
        ItemRelationshipsType itemRelationshipsType = itemRelationshipTypeDAO
            .findByID(context, ItemRelationshipsType.class, id);
        return itemRelationshipsType;
    }

    public void update(Context context, ItemRelationshipsType itemRelationshipsType)
        throws SQLException, AuthorizeException {
        update(context, Collections.singletonList(itemRelationshipsType));
    }

    public void update(Context context, List<ItemRelationshipsType> itemRelationshipsTypes)
        throws SQLException, AuthorizeException {
        if (CollectionUtils.isNotEmpty(itemRelationshipsTypes)) {
            // Check authorisation - only administrators can change formats
            if (!authorizeService.isAdmin(context)) {
                throw new AuthorizeException(
                    "Only administrators can modify entityType");
            }

            for (ItemRelationshipsType itemRelationshipsType : itemRelationshipsTypes) {
                itemRelationshipTypeDAO.save(context, itemRelationshipsType);
            }
        }
    }

    public void delete(Context context, ItemRelationshipsType itemRelationshipsType)
        throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only administrators can delete entityType");
        }
        itemRelationshipTypeDAO.delete(context, itemRelationshipsType);
    }
}
