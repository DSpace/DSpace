/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.dao;

import it.cilea.osd.common.dao.PaginableObjectDao;

import java.util.List;

import org.dspace.app.cris.model.CrisSubscription;

/**
 * This interface define the methods available to retrieve RPSubscription
 * 
 * @author cilea
 * 
 */
public interface CrisSubscriptionDao extends PaginableObjectDao<CrisSubscription, Integer> {
	public long countByUUID(String uuid);
	public List<String> findUUIDByEpersonID(int epersonID);
	public CrisSubscription uniqueByEpersonIDandUUID(int epersonID, String uuid);
	public void deleteByEpersonID(int id);
	public long countByEpersonIDandUUID(int epersonID, String uuid);
}
