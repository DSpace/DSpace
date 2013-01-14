/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.dao.database;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.dspace.orm.dao.api.IMetadataFieldRegistryDao;
import org.dspace.orm.dao.api.IMetadataSchemaRegistryDao;
import org.dspace.orm.dao.api.IMetadataValueDao;

import org.dspace.orm.entity.MetadataFieldRegistry;
import org.dspace.orm.entity.MetadataSchemaRegistry;
import org.dspace.orm.entity.MetadataValue;
import org.dspace.orm.entity.content.DSpaceObjectType;
import org.dspace.utils.DSpace;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Miguel Pinto <mpinto@lyncode.com>
 * @version $Revision$
 */

@Transactional
@Repository("org.dspace.orm.dao.api.IMetadataValueDao")
public class MetadataValueDao extends DSpaceDao<MetadataValue> implements IMetadataValueDao {
	private Map<String, MetadataSchemaRegistry> schemas = null;
	private Map<String, MetadataFieldRegistry> fields = null;
	
	private IMetadataFieldRegistryDao fieldRegistry;
	private IMetadataSchemaRegistryDao schemaRegistry;
	
	public MetadataValueDao() {
		super(MetadataValue.class);
	}

	private IMetadataSchemaRegistryDao getSchemaRegistry () {		
		if (schemaRegistry == null) schemaRegistry = new DSpace().getSingletonService(IMetadataSchemaRegistryDao.class);
		return schemaRegistry;
	}
	

	private IMetadataFieldRegistryDao getFieldRegistry () {
		if (fieldRegistry == null) fieldRegistry = new DSpace().getSingletonService(IMetadataFieldRegistryDao.class);
		return fieldRegistry;
	}
	
	
	
	private MetadataSchemaRegistry getSchema (String schema) {
		if (schemas == null) 
			schemas = new TreeMap<String, MetadataSchemaRegistry>();
		
		if (!schemas.containsKey(schema)) {
			schemas.put(schema, this.getSchemaRegistry().selectByName(schema));
		}
		
		return schemas.get(schema);
	}
	
	private MetadataFieldRegistry getField (String field) {
		int pos = field.indexOf(".");
		String schemaName = field.substring(0, pos);
		String fieldName = field.substring(pos+1);
		
		if (fields == null)
			fields = new TreeMap<String, MetadataFieldRegistry>();
		
		if (!fields.containsKey(field)) {
			fields.put(field, this.getFieldRegistry().selectByNameAndSchema(this.getSchema(schemaName), fieldName));
		}
			
		return fields.get(field);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<MetadataValue> selectByResourceId(DSpaceObjectType resourceType,
			int resourceId) {
		return (List<MetadataValue>) this.getSession().createCriteria(MetadataValue.class)
				.add(Restrictions.and(
						Restrictions.eq("resourceType", resourceType.getId()), 
						Restrictions.eq("resource", resourceId)
				))
				.list();
				
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<MetadataValue> selectByResourceAndField (DSpaceObjectType resourceType, int resourceId, String field)
	{
		return (List<MetadataValue>) this.getSession().createCriteria(MetadataValue.class)
				.add(Restrictions.and(
						Restrictions.eq("resourceType", resourceType.getId()), 
						Restrictions.eq("resource", resourceId),  
						Restrictions.eq("metadataField", this.getField(field))
				))
				.list();
	}
}
