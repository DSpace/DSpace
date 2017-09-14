/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.dao;

import java.util.List;

import org.dspace.app.cris.model.orcid.OrcidHistory;

import it.cilea.osd.common.dao.PaginableObjectDao;

/**
 * This interface define the methods available to retrieve OrcidQueue
 * 
 * @author l.pascarelli
 * 
 */
public interface OrcidHistoryDao extends PaginableObjectDao<OrcidHistory, Integer> {
	
    public List<OrcidHistory> findOrcidHistoryByOrcidAndEntityUUIDAndTypeId(String orcid, String uuid, Integer typeId);
	public List<OrcidHistory> findOrcidHistoryByOrcidAndTypeId(String orcid, Integer typeId);
	public List<OrcidHistory> findOrcidHistoryInSuccess();
	public List<OrcidHistory> findOrcidHistoryInError();
	public List<OrcidHistory> findOrcidHistoryInSuccessByOwner(String owner);
	public List<OrcidHistory> findOrcidHistoryInSuccessByOwnerAndTypeId(String owner, Integer typeId);
	public OrcidHistory uniqueOrcidHistoryInSuccessByOwnerAndEntityUUIDAndTypeId(String owner, String uuid, Integer typeId);
	public OrcidHistory uniqueOrcidHistoryByOwnerAndOrcidAndTypeId(String owner, String orcid, Integer typeId);
	public OrcidHistory uniqueOrcidHistoryByOwnerAndEntityUUIDAndTypeId(String owner, String uuid, Integer typeId);
	
	
}
