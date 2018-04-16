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
import org.dspace.content.RelationshipType;
import org.dspace.core.Context;
import org.dspace.service.DSpaceCRUDService;

public interface RelationshipTypeService extends DSpaceCRUDService<RelationshipType> {

    RelationshipType create(Context context,RelationshipType relationshipType) throws SQLException, AuthorizeException;

    RelationshipType findbyTypesAndLabels(Context context,EntityType leftType,EntityType rightType,
                                          String leftLabel,String rightLabel)
                                            throws SQLException;
    List<RelationshipType> findAll(Context context) throws SQLException;
}
