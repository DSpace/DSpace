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
	
	public List<OrcidQueue> findOrcidQueueByOwner(String crisId);
	public List<OrcidQueue> findOrcidQueueByProjectId(Integer pjId);
	public List<OrcidQueue> findOrcidQueueByOwnerAndTypeId(String owner, Integer typeId);	
	public OrcidQueue uniqueOrcidQueueByProjectIdAndOwner(Integer pjId, String crisId); 
	public List<OrcidQueue> findOrcidQueueByPublicationId(Integer pId); 
	public OrcidQueue uniqueOrcidQueueByPublicationIdAndOwner(Integer pId, String crisId); 
	public List<OrcidQueue> findOrcidQueueByEntityIdAndTypeId(Integer entityID, Integer typeId); 
	public OrcidQueue uniqueOrcidQueueByEntityIdAndTypeIdAndOwner(Integer entityID, Integer typeId, String crisId);
	public void deleteByOwnerAndTypeId(String crisID, int typeId);
	public void deleteByOwnerAndUuid(String crisID, String uuId);
	 
}
