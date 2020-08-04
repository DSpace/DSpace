/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataField_;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataSchema_;
import org.dspace.content.dao.MetadataFieldDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Hibernate implementation of the Database Access Object interface class for the MetadataField object.
 * This class is responsible for all database calls for the MetadataField object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class MetadataFieldDAOImpl extends AbstractHibernateDAO<MetadataField> implements MetadataFieldDAO {
    protected MetadataFieldDAOImpl() {
        super();
    }

    @Override
    public MetadataField find(Context context, int metadataFieldId, MetadataSchema metadataSchema, String element,
                              String qualifier) throws SQLException {
        Query query;

        if (qualifier != null) {
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

        if (qualifier != null) {
            query.setParameter("qualifier", qualifier);
        }
        query.setHint("org.hibernate.cacheable", Boolean.TRUE);

        return singleResult(query);
    }

    @Override
    public MetadataField findByElement(Context context, MetadataSchema metadataSchema, String element, String qualifier)
        throws SQLException {
        return findByElement(context, metadataSchema.getName(), element, qualifier);
    }

    @Override
    public MetadataField findByElement(Context context, String metadataSchema, String element, String qualifier)
        throws SQLException {
        Query query;

        if (StringUtils.isNotBlank(qualifier)) {
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

        if (StringUtils.isNotBlank(qualifier)) {
            query.setParameter("qualifier", qualifier);
        }
        query.setHint("org.hibernate.cacheable", Boolean.TRUE);

        return singleResult(query);
    }

    @Override
    public List<MetadataField> findAll(Context context, Class<MetadataField> clazz) throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, MetadataField.class);
        Root<MetadataField> metadataFieldRoot = criteriaQuery.from(MetadataField.class);
        Join<MetadataField, MetadataSchema> join = metadataFieldRoot.join("metadataSchema");
        criteriaQuery.select(metadataFieldRoot);

        List<javax.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.asc(join.get(MetadataSchema_.name)));
        orderList.add(criteriaBuilder.asc(metadataFieldRoot.get(MetadataField_.element)));
        orderList.add(criteriaBuilder.asc(metadataFieldRoot.get(MetadataField_.qualifier)));
        criteriaQuery.orderBy(orderList);

        return list(context, criteriaQuery, true, MetadataField.class, -1, -1, false);
    }

    @Override
    public List<MetadataField> findFieldsByElementNameUnqualified(Context context, String metadataSchema,
                                                                  String element) throws SQLException {
        Query query = createQuery(context, "SELECT mf " +
            "FROM MetadataField mf " +
            "JOIN FETCH mf.metadataSchema ms " +
            "WHERE ms.name = :name AND mf.element = :element ");


        query.setParameter("name", metadataSchema);
        query.setParameter("element", element);
        query.setHint("org.hibernate.cacheable", Boolean.TRUE);

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

        query.setHint("org.hibernate.cacheable", Boolean.TRUE);

        return list(query);
    }
}
