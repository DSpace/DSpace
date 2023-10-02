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
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.core.Context;
import org.hibernate.Session;

/**
 * Service interface class for the MetadataSchema object.
 * The implementation of this class is responsible for all business logic calls for the MetadataSchema object and is
 * autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface MetadataSchemaService {

    /**
     * Creates a new metadata schema in the database, using the name and namespace.
     *
     * @param context   DSpace context object
     * @param name      name
     * @param namespace namespace
     * @return new MetadataSchema
     * @throws SQLException               if database error
     * @throws AuthorizeException         if authorization error
     * @throws NonUniqueMetadataException if an existing field with an identical element and qualifier is already
     *                                    present
     */
    public MetadataSchema create(Context context, String name, String namespace)
        throws SQLException, AuthorizeException, NonUniqueMetadataException;

    /**
     * Get the schema object corresponding to this namespace URI.
     *
     * @param session   current request's database context.
     * @param namespace namespace URI to match
     * @return metadata schema object or null if none found.
     * @throws SQLException if database error
     */
    public MetadataSchema findByNamespace(Session session, String namespace) throws SQLException;

    /**
     * Update the metadata schema in the database.
     *
     * @param context        DSpace context
     * @param metadataSchema metadata schema
     * @throws SQLException               if database error
     * @throws AuthorizeException         if authorization error
     * @throws NonUniqueMetadataException if an existing field with an identical element and qualifier is already
     *                                    present
     */
    public void update(Context context, MetadataSchema metadataSchema)
        throws SQLException, AuthorizeException, NonUniqueMetadataException;

    /**
     * Delete the metadata schema.
     *
     * @param context        DSpace context
     * @param metadataSchema metadata schema
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public void delete(Context context, MetadataSchema metadataSchema) throws SQLException, AuthorizeException;

    /**
     * Return all metadata schemas.
     *
     * @param session DSpace context
     * @return array of metadata schemas
     * @throws SQLException if database error
     */
    public List<MetadataSchema> findAll(Session session) throws SQLException;

    /**
     * Get the schema corresponding with this numeric ID.
     * The ID is a database key internal to DSpace.
     *
     * @param session current request's database context.
     * @param id      the schema ID
     * @return the metadata schema object
     * @throws SQLException if database error
     */
    public MetadataSchema find(Session session, int id) throws SQLException;

    /**
     * Get the schema corresponding with this short name.
     *
     * @param session   current request's database session.
     * @param shortName the short name for the schema
     * @return the metadata schema object
     * @throws SQLException if database error
     */
    public MetadataSchema find(Session session, String shortName) throws SQLException;
}
