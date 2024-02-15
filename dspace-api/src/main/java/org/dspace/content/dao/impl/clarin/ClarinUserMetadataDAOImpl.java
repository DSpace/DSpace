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

import org.dspace.content.clarin.ClarinUserMetadata;
import org.dspace.content.dao.clarin.ClarinUserMetadataDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

public class ClarinUserMetadataDAOImpl extends AbstractHibernateDAO<ClarinUserMetadata>
        implements ClarinUserMetadataDAO {

    protected ClarinUserMetadataDAOImpl() {
        super();
    }

    @Override
    public List<ClarinUserMetadata> findByUserRegistrationAndBitstream(Context context, Integer userRegUUID,
                                                                       UUID bitstreamUUID) throws SQLException {
        Query query = createQuery(context, "SELECT cum FROM ClarinUserMetadata as cum " +
                "JOIN cum.eperson as ur " +
                "JOIN cum.transaction as clrua " +
                "JOIN clrua.licenseResourceMapping as map " +
                "WHERE ur.id = :userRegUUID " +
                "AND map.bitstream.id = :bitstreamUUID " +
                "ORDER BY clrua.id DESC");

        query.setParameter("userRegUUID", userRegUUID);
        query.setParameter("bitstreamUUID", bitstreamUUID);
        query.setHint("org.hibernate.cacheable", Boolean.TRUE);

        return list(query);
    }
}
