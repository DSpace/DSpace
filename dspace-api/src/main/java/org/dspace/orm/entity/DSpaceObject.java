/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.entity;

import java.util.List;

import javax.persistence.Transient;

import org.dspace.orm.dao.api.IResourcePolicyDao;
import org.dspace.orm.dao.database.HandleDao;
import org.dspace.orm.dao.database.MetadataValueDao;
import org.dspace.services.auth.Action;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * 
 * @author João Melo <jmelo@lyncode.com>
 *
 */
@Configurable
public abstract class DSpaceObject implements IDSpaceObject {

	@Override
	public abstract int getID();

	@Autowired HandleDao handleDao;
	@Autowired MetadataValueDao metadataDao;
	@Autowired IResourcePolicyDao resourcePolicyDao;
	
	@Transient
	public List<MetadataValue> getMetadata () {
		return metadataDao.selectByResourceId(getType().getId(), getID());
	}
	
	@Transient
	public Handle getHandle () {
		return handleDao.selectByResourceId(getType().getId(), getID());
	}
	
	@Transient
	public IDSpaceObject getAdminObject(Action action) {
		if (action == Action.ADMIN)
			throw new IllegalArgumentException("Illegal call to the DSpaceObject.getAdminObject method");
		return this;
	}
	
	@Transient
	public IDSpaceObject getParentObject () {
		return null; // By default there is no parent object
	}
	
	@Transient
	public boolean isAdmin (Eperson e) {
		if (e == null) return false;
		
		List<ResourcePolicy> policies = resourcePolicyDao.selectByObjectAndAction(this, Action.ADMIN);
		
		for (ResourcePolicy rp : policies)
        {
            // check policies for date validity
            if (rp.isDateValid())
            {
                if ((rp.getEperson() != null) && (rp.getEperson().getID() == e.getID()))
                {
                    return true; // match
                }

                if ((rp.getEpersonGroup() != null)
                        && (e.memberOf(rp.getEpersonGroup())))
                {
                    // group was set, and eperson is a member
                    // of that group
                    return true;
                }
            }
        }

        // If user doesn't have specific Admin permissions on this object,
        // check the *parent* objects of this object.  This allows Admin
        // permissions to be inherited automatically (e.g. Admin on Community
        // is also an Admin of all Collections/Items in that Community)
        IDSpaceObject parent = this.getParentObject();
        if (parent != null)
        {
            return parent.isAdmin(e);
        }

        return false;
	}
}
