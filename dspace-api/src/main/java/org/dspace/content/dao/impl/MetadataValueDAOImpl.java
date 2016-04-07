/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.dao.MetadataValueDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.util.List;

/**
 * Hibernate implementation of the Database Access Object interface class for the MetadataValue object.
 * This class is responsible for all database calls for the MetadataValue object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class MetadataValueDAOImpl extends AbstractHibernateDAO<MetadataValue> implements MetadataValueDAO
{
    protected MetadataValueDAOImpl()
    {
        super();
    }


    @Override
    public List<MetadataValue> findByField(Context context, MetadataField metadataField) throws SQLException
    {
        Criteria criteria = createCriteria(context, MetadataValue.class);
        criteria.add(
                Restrictions.eq("metadataField.id", metadataField.getID())
        );
        criteria.setFetchMode("metadataField", FetchMode.JOIN);

        return list(criteria);
    }

    @Override
    public List<MetadataValue> findByValueLike(Context context, String value) throws SQLException {
        Criteria criteria = createCriteria(context, MetadataValue.class);
        criteria.add(
                Restrictions.like("value", "%" + value + "%")
        );
        criteria.setFetchMode("metadataField", FetchMode.JOIN);

        return list(criteria);
    }

    @Override
    public void deleteByMetadataField(Context context, MetadataField metadataField) throws SQLException {
        String queryString = "delete from MetadataValue where metadataField= :metadataField";
        Query query = createQuery(context, queryString);
        query.setParameter("metadataField", metadataField);
        query.executeUpdate();
    }

    @Override
    public MetadataValue getMinimum(Context context, int metadataFieldId)
            throws SQLException
    {
        String queryString = "SELECT m FROM MetadataValue m JOIN FETCH m.metadataField WHERE m.metadataField.id = :metadata_field_id ORDER BY text_value";
        Query query = createQuery(context, queryString);
        query.setParameter("metadata_field_id", metadataFieldId);
        query.setMaxResults(1);
        return (MetadataValue) query.uniqueResult();
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM MetadataValue"));
    }

}
