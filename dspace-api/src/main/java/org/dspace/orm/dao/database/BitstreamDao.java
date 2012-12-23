/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.dao.database;

import org.dspace.orm.dao.api.IBitstreamDao;
import org.dspace.orm.entity.Bitstream;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author João Melo <jmelo@lyncode.com>
 * @author Miguel Pinto <mpinto@lyncode.com>
 */
@Transactional
@Repository("IBitstreamDao")
public class BitstreamDao extends DSpaceDao<Bitstream> implements IBitstreamDao {
    // private static Logger log = LogManager.getLogger(BitstreamDao.class);
    
    public BitstreamDao () {
    	super(Bitstream.class);
    }
}
