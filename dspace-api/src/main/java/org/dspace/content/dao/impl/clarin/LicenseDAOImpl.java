package org.dspace.content.dao.impl.clarin;

import org.dspace.content.clarin.License;
import org.dspace.content.dao.clarin.LicenseDAO;
import org.dspace.core.AbstractHibernateDAO;

public class LicenseDAOImpl extends AbstractHibernateDAO<License> implements LicenseDAO {
    protected LicenseDAOImpl() {
        super();
    }
}
