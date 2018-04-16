/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import java.sql.SQLException;

import org.dspace.content.EntityType;
import org.dspace.content.RelationshipType;
import org.dspace.content.dao.RelationshipTypeDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public class RelationshipTypeDAOImpl extends AbstractHibernateDAO<RelationshipType> implements RelationshipTypeDAO {

    public RelationshipType findbyTypesAndLabels(Context context,EntityType leftType,EntityType rightType,
                                                 String leftLabel,String rightLabel)
                                                    throws SQLException {
        Criteria criteria = createCriteria(context,RelationshipType.class);
        criteria.add(Restrictions.and(
            Restrictions.eq("leftType", leftType),
            Restrictions.eq("rightType", rightType),
            Restrictions.eq("leftLabel", leftLabel),
            Restrictions.eq("rightLabel", rightLabel)
        ));
        return singleResult(criteria);
    }

}
