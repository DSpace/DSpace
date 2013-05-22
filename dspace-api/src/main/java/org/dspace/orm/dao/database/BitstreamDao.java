/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.dao.database;

import java.util.List;

import org.dspace.orm.dao.api.IBitstreamDao;
import org.dspace.orm.entity.Bitstream;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author João Melo <jmelo@lyncode.com>
 * @author Miguel Pinto <mpinto@lyncode.com>
 */
@Transactional
@Repository("org.dspace.orm.dao.api.IBitstreamDao")
public class BitstreamDao extends DSpaceDao<Bitstream> implements IBitstreamDao {
    // private static Logger log = LogManager.getLogger(BitstreamDao.class);
    public BitstreamDao () {
    	super(Bitstream.class);
    }

	@SuppressWarnings("unchecked")
	@Override
	public List<Bitstream> selectAllDeleted() {
		return (List<Bitstream>) super.getSession().createCriteria(Bitstream.class)
				.add(Restrictions.eq("deleted", true))
				.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Bitstream> selectDuplicateInternalIdentifier(Bitstream bitstream) {
		return (List<Bitstream>) super.getSession().createCriteria(Bitstream.class)
				.add(Restrictions.and(
						Restrictions.eq("internalId", bitstream.getInternalId()),
						Restrictions.ne("ID", bitstream.getID())
				))
				.list();
	}
}
