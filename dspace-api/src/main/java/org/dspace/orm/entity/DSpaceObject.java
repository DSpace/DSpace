package org.dspace.orm.entity;

import java.util.List;

import javax.persistence.Transient;

import org.dspace.orm.dao.database.HandleDao;
import org.dspace.orm.dao.database.MetadataValueDao;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class DSpaceObject implements IDSpaceObject {

	@Override
	public abstract int getID();

	@Override
	@Transient
	public abstract int getType();

	
	@Autowired HandleDao handleDao;
	@Autowired MetadataValueDao metadataDao;
	
	@Transient
	public List<MetadataValue> getMetadata () {
		return metadataDao.selectByResourceId(getType(), getID());
	}
	
	@Transient
	public Handle getHandle () {
		return handleDao.selectByResourceId(getType(), getID());
	}
}
