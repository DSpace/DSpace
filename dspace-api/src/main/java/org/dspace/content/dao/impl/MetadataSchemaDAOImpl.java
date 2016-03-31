/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import org.dspace.content.MetadataSchema;
import org.dspace.content.dao.MetadataSchemaDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Order;

import java.sql.SQLException;
import java.util.List;

/**
 * Hibernate implementation of the Database Access Object interface class for the MetadataSchema object.
 * This class is responsible for all database calls for the MetadataSchema object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class MetadataSchemaDAOImpl extends AbstractHibernateDAO<MetadataSchema> implements MetadataSchemaDAO
{
    protected MetadataSchemaDAOImpl()
    {
        super();
    }

    /**
     * Get the schema object corresponding to this namespace URI.
     *
     * @param context DSpace context
     * @param namespace namespace URI to match
     * @return metadata schema object or null if none found.
     * @throws SQLException if database error
     */
    @Override
    public MetadataSchema findByNamespace(Context context, String namespace) throws SQLException
    {
        // Grab rows from DB
        Query query = createQuery(context,
                "SELECT ms FROM MetadataSchema ms " +
                "WHERE ms.namespace = :namespace ");

        query.setParameter("namespace", namespace);

        query.setCacheable(true);
        return singleResult(query);
    }

    @Override
    public List<MetadataSchema> findAll(Context context, Class clazz) throws SQLException {
        // Get all the metadataschema rows
        Criteria criteria = createCriteria(context, MetadataSchema.class);
        criteria.addOrder(Order.asc("id"));
        criteria.setCacheable(true);

        return list(criteria);
    }

    /**
     * Return true if and only if the passed name appears within the allowed
     * number of times in the current schema.
     *
     * @param context DSpace context
     * @param metadataSchemaId schema id
     * @param namespace namespace URI to match
     * @return true of false
     * @throws SQLException if database error
     */
    @Override
    public boolean uniqueNamespace(Context context, int metadataSchemaId, String namespace) throws SQLException
    {
        Query query = createQuery(context,
                "SELECT ms FROM MetadataSchema ms " +
                "WHERE ms.namespace = :namespace and ms.id != :id");

        query.setParameter("namespace", namespace);
        query.setParameter("id", metadataSchemaId);

        query.setCacheable(true);
        return singleResult(query) == null;
    }

    /**
     * Return true if and only if the passed name is unique.
     *
     * @param context DSpace context
     * @param metadataSchemaId schema id
     * @param name  short name of schema
     * @return true of false
     * @throws SQLException if database error
     */
    @Override
    public boolean uniqueShortName(Context context, int metadataSchemaId, String name) throws SQLException
    {
        Query query = createQuery(context,
                "SELECT ms FROM MetadataSchema ms " +
                "WHERE ms.name = :name and ms.id != :id");

        query.setParameter("name", name);
        query.setParameter("id", metadataSchemaId);

        query.setCacheable(true);
        return singleResult(query) == null;
    }

    /**
     * Get the schema corresponding with this short name.
     *
     * @param context
     *            context, in case we need to read it in from DB
     * @param shortName
     *            the short name for the schema
     * @return the metadata schema object
     * @throws SQLException if database error
     */
    @Override
    public MetadataSchema find(Context context, String shortName) throws SQLException
    {
        Query query = createQuery(context,
                "SELECT ms FROM MetadataSchema ms " +
                "WHERE ms.name = :name");

        query.setParameter("name", shortName);

        query.setCacheable(true);
        return singleResult(query);
    }
}
