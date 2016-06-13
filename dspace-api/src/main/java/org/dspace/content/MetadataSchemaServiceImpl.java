/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.MetadataSchemaDAO;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.List;

/**
 * Service implementation for the MetadataSchema object.
 * This class is responsible for all business logic calls for the MetadataSchema object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class MetadataSchemaServiceImpl implements MetadataSchemaService {

    /** log4j logger */
    private static Logger log = Logger.getLogger(MetadataSchemaServiceImpl.class);

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Autowired(required = true)
    protected MetadataSchemaDAO metadataSchemaDAO;

    protected MetadataSchemaServiceImpl()
    {

    }

    @Override
    public MetadataSchema create(Context context, String name, String namespace) throws SQLException, AuthorizeException, NonUniqueMetadataException {
                // Check authorisation: Only admins may create metadata schemas
        if (!authorizeService.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators may modify the metadata registry");
        }

        // Ensure the schema name is unique
        if (!uniqueShortName(context,-1, name))
        {
            throw new NonUniqueMetadataException("Please make the name " + name
                    + " unique");
        }

        // Ensure the schema namespace is unique
        if (!uniqueNamespace(context,-1, namespace))
        {
            throw new NonUniqueMetadataException("Please make the namespace " + namespace
                    + " unique");
        }


        // Create a table row and update it with the values
        MetadataSchema metadataSchema = metadataSchemaDAO.create(context, new MetadataSchema());
        metadataSchema.setNamespace(namespace);
        metadataSchema.setName(name);
        metadataSchemaDAO.save(context, metadataSchema);
        log.info(LogManager.getHeader(context, "create_metadata_schema",
                "metadata_schema_id="
                        + metadataSchema.getID()));
        return metadataSchema;
    }

    @Override
    public MetadataSchema findByNamespace(Context context, String namespace) throws SQLException {
        return metadataSchemaDAO.findByNamespace(context, namespace);
    }

    @Override
    public void update(Context context, MetadataSchema metadataSchema) throws SQLException, AuthorizeException, NonUniqueMetadataException {
        // Check authorisation: Only admins may update the metadata registry
        if (!authorizeService.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators may modify the metadata registry");
        }

        // Ensure the schema name is unique
        if (!uniqueShortName(context, metadataSchema.getID(), metadataSchema.getName()))
        {
            throw new NonUniqueMetadataException("Please make the name " + metadataSchema.getName()
                    + " unique");
        }

        // Ensure the schema namespace is unique
        if (!uniqueNamespace(context, metadataSchema.getID(), metadataSchema.getNamespace()))
        {
            throw new NonUniqueMetadataException("Please make the namespace " + metadataSchema.getNamespace()
                    + " unique");
        }
        metadataSchemaDAO.save(context, metadataSchema);
        log.info(LogManager.getHeader(context, "update_metadata_schema",
                "metadata_schema_id=" + metadataSchema.getID() + "namespace="
                        + metadataSchema.getNamespace() + "name=" + metadataSchema.getName()));
    }

    @Override
    public void delete(Context context, MetadataSchema metadataSchema) throws SQLException, AuthorizeException {
                // Check authorisation: Only admins may create DC types
        if (!authorizeService.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators may modify the metadata registry");
        }

        log.info(LogManager.getHeader(context, "delete_metadata_schema",
                "metadata_schema_id=" + metadataSchema.getID()));

        metadataSchemaDAO.delete(context, metadataSchema);
    }

    @Override
    public List<MetadataSchema> findAll(Context context) throws SQLException {
        return metadataSchemaDAO.findAll(context, MetadataSchema.class);
    }

    @Override
    public MetadataSchema find(Context context, int id) throws SQLException {
        return metadataSchemaDAO.findByID(context,  MetadataSchema.class, id);
    }

    @Override
    public MetadataSchema find(Context context, String shortName) throws SQLException {
        // If we are not passed a valid schema name then return
        if (shortName == null)
        {
            return null;
        }
        return metadataSchemaDAO.find(context, shortName);
    }


    /**
     * Return true if and only if the passed name appears within the allowed
     * number of times in the current schema.
     *
     * @param context DSpace context
     * @param metadataSchemaId metadata schema id
     * @param namespace namespace URI to match
     * @return true of false
     * @throws SQLException if database error
     */
    protected boolean uniqueNamespace(Context context, int metadataSchemaId, String namespace)
            throws SQLException
    {
        return metadataSchemaDAO.uniqueNamespace(context, metadataSchemaId, namespace);
    }

    /**
     * Return true if and only if the passed name is unique.
     *
     * @param context DSpace context
     * @param metadataSchemaId metadata schema id
     * @param name  short name of schema
     * @return true of false
     * @throws SQLException if database error
     */
    protected boolean uniqueShortName(Context context, int metadataSchemaId, String name)
            throws SQLException
    {
        return metadataSchemaDAO.uniqueShortName(context, metadataSchemaId, name);
    }
}
