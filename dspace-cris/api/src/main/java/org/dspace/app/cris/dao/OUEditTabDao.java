/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.dao;

import it.cilea.osd.jdyna.dao.EditTabDao;

import org.dspace.app.cris.model.jdyna.BoxOrganizationUnit;
import org.dspace.app.cris.model.jdyna.EditTabOrganizationUnit;
import org.dspace.app.cris.model.jdyna.OUPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.TabOrganizationUnit;



public interface OUEditTabDao extends EditTabDao<BoxOrganizationUnit,TabOrganizationUnit,EditTabOrganizationUnit,OUPropertiesDefinition> {

	
}
