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
import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.util.List;

/**
 * Hibernate implementation of the Database Access Object interface class for the MetadataField object.
 * This class is responsible for all database calls for the MetadataField object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class MetadataFieldDAOImpl extends AbstractHibernateDAO<MetadataField> implements MetadataFieldDAO {

    @Override
    public MetadataField find(Context context, int metadataFieldId, MetadataSchema metadataSchema, String element,
                           String qualifier) throws SQLException{
        Criteria criteria = createCriteria(context, MetadataField.class);
        criteria.add(
                Restrictions.and(
                        Restrictions.not(Restrictions.eq("id", metadataFieldId)),
                        Restrictions.eq("metadataSchema", metadataSchema),
                        Restrictions.eq("element", element),
                        Restrictions.eqOrIsNull("qualifier", qualifier)
                )
        );
        return singleResult(criteria);
    }

    @Override
    public MetadataField findByElement(Context context, MetadataSchema metadataSchema, String element, String qualifier) throws SQLException
    {
        Criteria criteria = createCriteria(context, MetadataField.class);
        criteria.add(
                Restrictions.and(
                        Restrictions.eq("metadataSchema", metadataSchema),
                        Restrictions.eq("element", element),
                        Restrictions.eqOrIsNull("qualifier", qualifier)
                )
        );
        return singleResult(criteria);
    }

    @Override
    public MetadataField findByElement(Context context, String metadataSchema, String element, String qualifier) throws SQLException
    {
        Criteria criteria = createCriteria(context, MetadataField.class);
        criteria.createAlias("metadataSchema", "s").add(
                Restrictions.and(
                        Restrictions.eq("s.name", metadataSchema),
                        Restrictions.eq("element", element),
                        Restrictions.eqOrIsNull("qualifier", qualifier)
                )
        );
        return singleResult(criteria);
    }

    @Override
    public List<MetadataField> findFieldsByElementNameUnqualified(Context context, String metadataSchema, String element) throws SQLException
    {
        Criteria criteria = createCriteria(context, MetadataField.class);
        criteria.createAlias("metadataSchema", "s").add(
                Restrictions.and(
                        Restrictions.eq("s.name", metadataSchema),
                        Restrictions.eq("element", element)
                )
        );
        return list(criteria);
    }

    
    @Override
    public List<MetadataField> findAllInSchema(Context context, MetadataSchema metadataSchema) throws SQLException {
        // Get all the metadatafieldregistry rows
        Criteria criteria = createCriteria(context, MetadataField.class);
        criteria.add(Restrictions.eq("metadataSchema", metadataSchema));
        return list(criteria);
    }
}
