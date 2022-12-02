/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl.clarin;

import org.dspace.content.Item;
import org.dspace.content.clarin.ClarinLicenseResourceMapping;
import org.dspace.content.dao.clarin.ClarinItemDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

import javax.persistence.Query;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class ClarinItemDAOImpl extends AbstractHibernateDAO<Item>
        implements ClarinItemDAO {
    @Override
    public List<Item> findByBitstreamUUID(Context context, UUID bitstreamUUID) throws SQLException {
        Query query = createQuery(context, "SELECT item FROM Item as item join item.bundles bundle " +
                "join bundle.bitstreams bitstream WHERE bitstream.id = :bitstreamUUID");

        query.setParameter("bitstreamUUID", bitstreamUUID);
        query.setHint("org.hibernate.cacheable", Boolean.TRUE);

        return list(query);
    }
}
