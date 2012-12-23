/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.dao.database;

import java.util.List;

import org.dspace.orm.dao.api.ICommunityDao;
import org.dspace.orm.entity.Community;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author João Melo <jmelo@lyncode.com>
 * @author Miguel Pinto <mpinto@lyncode.com>
 */
@Transactional
@Repository("ICommunityDao")
public class CommunityDao extends DSpaceDao<Community> implements ICommunityDao {
//    private static Logger log = LogManager.getLogger(CommunityDao.class);

    public CommunityDao () {
    	super(Community.class);
    }

    
    
	@SuppressWarnings("unchecked")
	@Override
	/**
	 * @return List of top communities
	 */
	public List<Community> selectTop() {
		return (List<Community>) super.getSession().createCriteria(Community.class)
				.add(Restrictions.eq("isTop", true))
				.list();
	}
}
