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

import org.dspace.content.clarin.ClarinLicenseResourceMapping;
import org.dspace.content.dao.clarin.ClarinLicenseResourceMappingDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

public class ClarinLicenseResourceMappingDAOImpl extends AbstractHibernateDAO<ClarinLicenseResourceMapping>
        implements ClarinLicenseResourceMappingDAO {
    protected ClarinLicenseResourceMappingDAOImpl() {
        super();
    }

    @Override
    public List<ClarinLicenseResourceMapping> findByBitstreamUUID(Context context, UUID bitstreamUUID)
            throws SQLException {
        Query query = createQuery(context, "SELECT clrm " +
                "FROM ClarinLicenseResourceMapping clrm " +
                "WHERE clrm.bitstream.id = :bitstreamUUID");

        query.setParameter("bitstreamUUID", bitstreamUUID);
        query.setHint("org.hibernate.cacheable", Boolean.TRUE);

        return list(query);
    }

    @Override
    public void delete(Context context, ClarinLicenseResourceMapping clarinLicenseResourceMapping) throws SQLException {
        clarinLicenseResourceMapping.setBitstream(null);
        super.delete(context, clarinLicenseResourceMapping);
    }
}
