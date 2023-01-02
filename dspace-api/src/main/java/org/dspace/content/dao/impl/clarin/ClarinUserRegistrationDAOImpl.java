/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl.clarin;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.persistence.Query;

import org.dspace.content.clarin.ClarinUserRegistration;
import org.dspace.content.dao.clarin.ClarinUserRegistrationDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

public class ClarinUserRegistrationDAOImpl extends AbstractHibernateDAO<ClarinUserRegistration>
        implements ClarinUserRegistrationDAO {

    protected ClarinUserRegistrationDAOImpl() {
        super();
    }

    @Override
    public List<ClarinUserRegistration> findByEPersonUUID(Context context, UUID epersonUUID) throws SQLException {
        Query query = createQuery(context, "SELECT cur FROM ClarinUserRegistration as cur " +
                "WHERE cur.ePersonID = :epersonUUID");

        query.setParameter("epersonUUID", epersonUUID);
        query.setHint("org.hibernate.cacheable", Boolean.TRUE);

        return list(query);
    }
}
