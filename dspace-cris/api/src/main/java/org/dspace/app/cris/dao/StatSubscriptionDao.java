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

import org.dspace.app.cris.model.StatSubscription;

/**
 * This interface define the methods available to retrieve StatSubscription
 * 
 * @author cilea
 * 
 */
public interface StatSubscriptionDao extends PaginableObjectDao<StatSubscription, Integer> {
    public List<StatSubscription> findByFreq(int freq);
    public List<StatSubscription> findByFreqAndType(int freq, int type);
	public List<StatSubscription> findByType(Integer type);
	public List<StatSubscription> findByUID(String uid);
	public List<StatSubscription> findByEPersonID(int epersonID);    
    public List<StatSubscription> findByEPersonIDandUID(int id, String uid);
    public List<StatSubscription> findByEPersonIDandType(int id, Integer type);
    public void deleteByEPersonID(int id);    
}
