/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.OrganizationUnit;

public class OUAuthorityForCRIS extends CRISAuthorityForCRIS<OrganizationUnit>
{

    @Override
    public int getCRISTargetTypeID()
    {
        return CrisConstants.OU_TYPE_ID;
    }

    @Override
    public Class<OrganizationUnit> getCRISTargetClass()
    {
        return OrganizationUnit.class;
    }

    
    @Override
    public String getPublicPath() {
    	return "ou";
    }

	@Override
	public OrganizationUnit getNewCrisObject() {
		return new OrganizationUnit();
	}
    

}
