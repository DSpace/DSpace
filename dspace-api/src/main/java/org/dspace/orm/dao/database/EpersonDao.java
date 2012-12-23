/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.dao.database;

import org.dspace.orm.dao.api.IEpersonDao;

import org.dspace.orm.entity.Eperson;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author João Melo <jmelo@lyncode.com>
 * @author Miguel Pinto <mpinto@lyncode.com>
 */
@Transactional
@Repository("IItemDao")
public class EpersonDao extends DSpaceDao<Eperson> implements IEpersonDao {
    
	public EpersonDao() {
		super(Eperson.class);
	}

	@Override
	public Eperson selectByEmail(String eperson) {
		return (Eperson) super.getSession().createCriteria(Eperson.class)
			.add(Restrictions.eq("email", eperson))
			.uniqueResult();
	}
}
