/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.dao;

import org.dspace.app.cris.model.OrganizationUnit;

/**
 * This interface define the methods available to retrieve Project
 * 
 * @author cilea
 * 
 */
public interface OrganizationUnitDao extends CrisObjectDao<OrganizationUnit>
{
 
    public OrganizationUnit uniqueBySourceID(String sourceRef, String sourceId);

}
