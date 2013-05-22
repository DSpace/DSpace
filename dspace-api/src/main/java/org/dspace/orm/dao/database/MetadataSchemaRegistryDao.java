/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.dao.database;

import org.dspace.orm.dao.api.IMetadataSchemaRegistryDao;

import org.dspace.orm.entity.MetadataSchemaRegistry;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Miguel Pinto <mpinto@lyncode.com>
 * @version $Revision$
 */

@Transactional
@Repository("org.dspace.orm.dao.api.IMetadataSchemaRegistryDao")
public class MetadataSchemaRegistryDao extends DSpaceDao<MetadataSchemaRegistry> implements IMetadataSchemaRegistryDao {
    
	public MetadataSchemaRegistryDao() {
		super(MetadataSchemaRegistry.class);
	}

	@Override
	public MetadataSchemaRegistry selectByName(String schema) {
		return (MetadataSchemaRegistry) super.getSession().createCriteria(MetadataSchemaRegistry.class)
				.add(Restrictions.eq("shortID", schema))
				.uniqueResult();
	}
}
