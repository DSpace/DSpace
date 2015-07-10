/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.dao;

import java.util.List;

import org.dspace.app.cris.model.orcid.OrcidQueue;

import it.cilea.osd.common.dao.PaginableObjectDao;

/**
 * This interface define the methods available to retrieve OrcidQueue
 * 
 * @author l.pascarelli
 * 
 */
public interface OrcidQueueDao extends PaginableObjectDao<OrcidQueue, Integer> {
	
	public List<OrcidQueue> findOrcidQueueByResearcherId(Integer researcherID); 
}
