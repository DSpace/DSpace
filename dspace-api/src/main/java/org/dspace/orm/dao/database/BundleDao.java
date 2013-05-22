/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.dao.database;

import org.dspace.orm.dao.api.IBundleDao;
import org.dspace.orm.entity.Bundle;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


/**
 * @author João Melo <jmelo@lyncode.com>
 * @author Miguel Pinto <mpinto@lyncode.com>
 */
@Transactional
@Repository("org.dspace.orm.dao.api.IBundleDao")
public class BundleDao extends DSpaceDao<Bundle> implements IBundleDao {

//	private static Logger log = LogManager.getLogger(BundleDao.class);

    public BundleDao() {
		super(Bundle.class);
	}
    
}
