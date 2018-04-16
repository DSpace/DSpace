/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.EntityType;
import org.dspace.core.Context;
import org.dspace.service.DSpaceCRUDService;

public interface EntityTypeService extends DSpaceCRUDService<EntityType> {

    public EntityType findByEntityType(Context context,String entityType) throws SQLException;

    public List<EntityType> findAll(Context context) throws SQLException;

    public EntityType create(Context context, String entityTypeString) throws SQLException, AuthorizeException;
}
