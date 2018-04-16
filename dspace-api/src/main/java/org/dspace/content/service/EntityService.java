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
import java.util.UUID;

import org.dspace.content.Entity;
import org.dspace.content.EntityType;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.core.Context;

public interface EntityService {

    Entity findByItemId(Context context, UUID itemId) throws SQLException;
    EntityType getType(Context context, Entity entity) throws SQLException;
    List<Relationship> getAllRelations(Context context, Entity entity);
    List<Relationship> getLeftRelations(Context context, Entity entity);
    List<Relationship> getRightRelations(Context context, Entity entity);
    List<Relationship> getRelationsByLabel(Context context, String label) throws SQLException;
    List<RelationshipType> getAllRelationshipTypes(Context context, Entity entity) throws SQLException;
    List<RelationshipType> getLeftRelationshipTypes(Context context, Entity entity) throws SQLException;
    List<RelationshipType> getRightRelationshipTypes(Context context, Entity entity) throws SQLException;
    List<RelationshipType> getRelationshipTypesByLabel(Context context, String label) throws SQLException;

}
