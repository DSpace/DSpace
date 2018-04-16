/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import java.sql.SQLException;

import org.dspace.content.EntityType;
import org.dspace.content.RelationshipType;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

public interface RelationshipTypeDAO extends GenericDAO<RelationshipType> {

    RelationshipType findbyTypesAndLabels(Context context,
                                          EntityType leftType,EntityType rightType,String leftLabel,String rightLabel)
                                                throws SQLException;

}
