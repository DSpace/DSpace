/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.dao;

import it.cilea.osd.jdyna.dao.EditTabDao;

import org.dspace.app.cris.model.jdyna.BoxResearcherPage;
import org.dspace.app.cris.model.jdyna.EditTabResearcherPage;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.TabResearcherPage;



public interface RPEditTabDao extends EditTabDao<BoxResearcherPage,TabResearcherPage,EditTabResearcherPage,RPPropertiesDefinition> {
	
}
