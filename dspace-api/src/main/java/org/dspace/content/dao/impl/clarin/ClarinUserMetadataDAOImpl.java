/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl.clarin;

import org.dspace.content.clarin.ClarinUserMetadata;
import org.dspace.content.dao.clarin.ClarinUserMetadataDAO;
import org.dspace.core.AbstractHibernateDAO;

public class ClarinUserMetadataDAOImpl extends AbstractHibernateDAO<ClarinUserMetadata>
        implements ClarinUserMetadataDAO {

    protected ClarinUserMetadataDAOImpl() {
        super();
    }
}
