/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.dao.MetadataFieldDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.criterion.Order;

import java.sql.SQLException;
import java.util.List;

/**
 * Hibernate implementation of the Database Access Object interface class for the MetadataField object.
 * This class is responsible for all database calls for the MetadataField object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class MetadataFieldDAOImpl extends AbstractHibernateDAO<MetadataField> implements MetadataFieldDAO
{
    protected MetadataFieldDAOImpl()
    {
        super();
    }

    @Override
    public MetadataField find(Context context, int metadataFieldId, MetadataSchema metadataSchema, String element,
                           String qualifier) throws SQLException{
        Query query;

        if(qualifier != null) {
            query = createQuery(context, "SELECT mf " +
                    "FROM MetadataField mf " +
                    "JOIN FETCH mf.metadataSchema ms " +
                    "WHERE mf.id != :id " +
                    "AND ms.name = :name AND mf.element = :element " +
                    "AND qualifier = :qualifier");
        } else {
            query = createQuery(context, "SELECT mf " +
                    "FROM MetadataField mf " +
                    "JOIN FETCH mf.metadataSchema ms " +
                    "WHERE mf.id != :id " +
                    "AND ms.name = :name AND mf.element = :element " +
                    "AND mf.qualifier IS NULL");
        }

        query.setParameter("id", metadataFieldId);
        query.setParameter("name", metadataSchema.getName());
        query.setParameter("element", element);

        if(qualifier != null) {
            query.setParameter("qualifier", qualifier);
        }

        query.setCacheable(true);
        return singleResult(query);
    }

    @Override
    public MetadataField findByElement(Context context, MetadataSchema metadataSchema, String element, String qualifier) throws SQLException
    {
        return findByElement(context, metadataSchema.getName(), element, qualifier);
    }

    @Override
    public MetadataField findByElement(Context context, String metadataSchema, String element, String qualifier) throws SQLException
    {
        Query query;

        if(qualifier != null) {
            query = createQuery(context, "SELECT mf " +
                    "FROM MetadataField mf " +
                    "JOIN FETCH mf.metadataSchema ms " +
                    "WHERE ms.name = :name AND mf.element = :element " +
                    "AND qualifier = :qualifier");
        } else {
            query = createQuery(context, "SELECT mf " +
                    "FROM MetadataField mf " +
                    "JOIN FETCH mf.metadataSchema ms " +
                    "WHERE ms.name = :name AND mf.element = :element " +
                    "AND mf.qualifier IS NULL");
        }

        query.setParameter("name", metadataSchema);
        query.setParameter("element", element);

        if(qualifier != null) {
            query.setParameter("qualifier", qualifier);
        }

        query.setCacheable(true);
        return singleResult(query);
    }

    @Override
    public List<MetadataField> findAll(Context context, Class<MetadataField> clazz) throws SQLException {
        Criteria criteria = createCriteria(context, MetadataField.class);
        criteria.createAlias("metadataSchema", "s").addOrder(Order.asc("s.name")).addOrder(Order.asc("element")).addOrder(Order.asc("qualifier"));
        criteria.setFetchMode("metadataSchema", FetchMode.JOIN);
        criteria.setCacheable(true);
        return list(criteria);
    }

    @Override
    public List<MetadataField> findFieldsByElementNameUnqualified(Context context, String metadataSchema, String element) throws SQLException
    {
        Query query = createQuery(context, "SELECT mf " +
                    "FROM MetadataField mf " +
                    "JOIN FETCH mf.metadataSchema ms " +
                    "WHERE ms.name = :name AND mf.element = :element ");


        query.setParameter("name", metadataSchema);
        query.setParameter("element", element);

        query.setCacheable(true);
        return list(query);
    }

    
    @Override
    public List<MetadataField> findAllInSchema(Context context, MetadataSchema metadataSchema) throws SQLException {

        Query query = createQuery(context, "SELECT mf " +
                "FROM MetadataField mf " +
                "JOIN FETCH mf.metadataSchema ms " +
                "WHERE ms.name = :name " +
                "ORDER BY mf.element ASC, mf.qualifier ASC ");

        query.setParameter("name", metadataSchema.getName());

        query.setCacheable(true);
        return list(query);
    }
}
