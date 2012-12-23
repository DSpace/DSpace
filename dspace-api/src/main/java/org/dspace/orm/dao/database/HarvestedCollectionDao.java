/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.dao.database;

import org.dspace.orm.dao.api.IHarvestedCollectionDao;

import org.dspace.orm.entity.HarvestedCollection;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Miguel Pinto <mpinto@lyncode.com>
 * @version $Revision$
 */

@Transactional
@Repository("IHarvestedItemDao")
public class HarvestedCollectionDao extends DSpaceDao<HarvestedCollection> implements IHarvestedCollectionDao {
    
	public HarvestedCollectionDao() {
		super(HarvestedCollection.class);
	}
}
