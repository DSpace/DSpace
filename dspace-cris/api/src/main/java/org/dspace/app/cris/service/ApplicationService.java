/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.service;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.dspace.app.cris.batch.ImportCRISDataModelConfiguration;
import org.dspace.app.cris.dao.CrisObjectDao;
import org.dspace.app.cris.dao.CrisSubscriptionDao;
import org.dspace.app.cris.dao.DynamicObjectDao;
import org.dspace.app.cris.dao.DynamicObjectTypeDao;
import org.dspace.app.cris.dao.OrcidHistoryDao;
import org.dspace.app.cris.dao.OrcidQueueDao;
import org.dspace.app.cris.dao.OrganizationUnitDao;
import org.dspace.app.cris.dao.ProjectDao;
import org.dspace.app.cris.dao.RelationPreferenceDao;
import org.dspace.app.cris.dao.ResearcherPageDao;
import org.dspace.app.cris.dao.StatSubscriptionDao;
import org.dspace.app.cris.dao.UserWSDao;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisSubscription;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.RelationPreference;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.StatSubscription;
import org.dspace.app.cris.model.jdyna.DynamicObjectType;
import org.dspace.app.cris.model.jdyna.DynamicTypeNestedObject;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.model.orcid.OrcidHistory;
import org.dspace.app.cris.model.orcid.OrcidQueue;
import org.dspace.app.cris.model.ws.User;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.util.Util;
import org.dspace.core.ConfigurationManager;
import org.dspace.services.ConfigurationService;
import org.dspace.storage.rdbms.DatabaseUtils;
import org.hibernate.Session;

import it.cilea.osd.common.model.Identifiable;
import jxl.read.biff.BiffException;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * This class provide access to the RP database service layer. Every methods
 * work in transactional context defined in the Spring configuration file:
 * applicationContext-rp-service.xml
 * 
 * @author cilea
 * 
 */
public class ApplicationService extends ExtendedTabService
{

    private ResearcherPageDao researcherPageDao;
    private ProjectDao projectDao;
    private OrganizationUnitDao organizationUnitDao;
    private DynamicObjectDao researchDao;
    
    private CrisSubscriptionDao crisSubscriptionDao;

    private StatSubscriptionDao statSubscriptionDao;

    private RelationPreferenceDao relationPreferenceDao;

    private UserWSDao userWSDao;

    private OrcidQueueDao orcidQueueDao;
    
    private OrcidHistoryDao orcidHistoryDao;
    
    private ConfigurationService configurationService;
    
    private CacheManager cacheManager;

    private Cache cache;
	private Cache cacheRpByEPerson;
	private Cache cacheByCrisID;
	private Cache cacheBySource;
	private Cache cacheByUUID;
	
    private static Logger log = Logger.getLogger(ApplicationService.class);

    /**
     * Initialization method
     */
    public void init()
    {
    	super.init();
        researcherPageDao = (ResearcherPageDao) getDaoByModel(ResearcherPage.class);
        projectDao = (ProjectDao) getDaoByModel(Project.class);
        organizationUnitDao = (OrganizationUnitDao) getDaoByModel(OrganizationUnit.class);
        crisSubscriptionDao = (CrisSubscriptionDao) getDaoByModel(CrisSubscription.class);
        statSubscriptionDao = (StatSubscriptionDao) getDaoByModel(StatSubscription.class);
        userWSDao = (UserWSDao) getDaoByModel(User.class);
        relationPreferenceDao = (RelationPreferenceDao) getDaoByModel(RelationPreference.class);
        researchDao = (DynamicObjectDao) getDaoByModel(ResearchObject.class);
        orcidQueueDao = (OrcidQueueDao) getDaoByModel(OrcidQueue.class);
        orcidHistoryDao = (OrcidHistoryDao) getDaoByModel(OrcidHistory.class);
        
		if (configurationService.getPropertyAsType("cris.applicationServiceCache.enabled", true, true))
        {
            enableCacheManager();
        }
    }

    private void enableCacheManager()
    {
        if (cache == null)
        {
            try
            {
                cacheManager = CacheManager.create();
                if (cacheManager != null)
                {
                    int maxInMemoryObjects = configurationService.getPropertyAsType("cris.applicationServiceCache.max-in-memory-objects", 1000);
                    boolean overflowToDisk = configurationService.getPropertyAsType("cris.applicationServiceCache.overflow-to-disk", true);
                    int timeToLive = configurationService.getPropertyAsType("cris.applicationServiceCache.time-to-live", 0);
                    int timeToIdle = configurationService.getPropertyAsType("cris.applicationServiceCache.time-to-idle", 0);
                    int diskExpireThreadInterval = configurationService.getPropertyAsType("cris.applicationServiceCache.disk-expire-thread-interval", 600);

                    cache = cacheManager.getCache("applicationServiceCache");
                    if (cache == null)
                    {
                        cache = new Cache("applicationServiceCache",
                                maxInMemoryObjects, overflowToDisk, false,
                                timeToLive, timeToIdle, false,
                                diskExpireThreadInterval);

                        cacheManager.addCache(cache);
                    }
                    cacheRpByEPerson = cacheManager
                            .getCache("applicationServiceCacheRpByEPerson");
                    if (cacheRpByEPerson == null)
                    {
                        cacheRpByEPerson = new Cache(
                                "applicationServiceCacheRpByEPerson",
                                maxInMemoryObjects, overflowToDisk, false,
                                timeToLive, timeToIdle, false,
                                diskExpireThreadInterval);
                        cacheManager.addCache(cacheRpByEPerson);
                    }

                    cacheByCrisID = cacheManager
                            .getCache("applicationServicecacheByCrisID");
                    if (cacheByCrisID == null)
                    {
                        cacheByCrisID = new Cache(
                                "applicationServicecacheByCrisID",
                                maxInMemoryObjects, overflowToDisk, false,
                                timeToLive, timeToIdle, false,
                                diskExpireThreadInterval);
                        cacheManager.addCache(cacheByCrisID);
                    }

                    cacheBySource = cacheManager
                            .getCache("applicationServiceCacheBySource");
                    if (cacheBySource == null)
                    {
                        cacheBySource = new Cache(
                                "applicationServiceCacheBySource",
                                maxInMemoryObjects, overflowToDisk, false,
                                timeToLive, timeToIdle, false,
                                diskExpireThreadInterval);
                        cacheManager.addCache(cacheBySource);
                    }

                    cacheByUUID = cacheManager
                            .getCache("applicationServiceCacheBySource");
                    if (cacheByUUID == null)
                    {
                        cacheByUUID = new Cache("applicationServiceCacheByUUID",
                                maxInMemoryObjects, overflowToDisk, false,
                                timeToLive, timeToIdle, false,
                                diskExpireThreadInterval);
                        cacheManager.addCache(cacheByUUID);
                    }
                }
            }
            catch (Exception ex)
            {
                log.error("init", ex);
            }
        }
    }

    public void destroy()
    {
        disableCacheManager();
    }

    /**
     * Evict a persistent object from the HibernateSession
     * 
     * @see Session#evict(Object)
     */
    public void evict(Identifiable identifiable)
    {
        applicationDao.evict(identifiable);
    }

    public ResearchObject getBySourceID(String sourceRef, String sourceId)
    {
    	return researchDao.uniqueBySourceID(sourceRef, sourceId);
    }
    public long countSubscriptionsByUUID(String uuid)
    {
        return crisSubscriptionDao.countByUUID(uuid);
    }

    public List<String> getCrisSubscriptionsByEPersonID(int eid)
    {
        return crisSubscriptionDao.findUUIDByEpersonID(eid);
    }

    public CrisSubscription getCrisStatSubscriptionByEPersonIDAndUUID(int eid, String uuid)
    {
        return crisSubscriptionDao.uniqueByEpersonIDandUUID(eid, uuid);
    }
    

    public boolean isSubscribed(int epersonID, String uuid)
    {
        return crisSubscriptionDao.uniqueByEpersonIDandUUID(epersonID, uuid) != null;
    }

    public CrisSubscription getSubscription(int epersonID, String uuid)
    {
        return crisSubscriptionDao.uniqueByEpersonIDandUUID(epersonID, uuid);
    }

    public void deleteSubscriptionByEPersonID(int id)
    {
        crisSubscriptionDao.deleteByEpersonID(id);
    }

    public long countByEpersonIDandUUID(int epersonID, String uuid,
            Class<CrisSubscription> className)
    {
        CrisSubscriptionDao dao = (CrisSubscriptionDao) getDaoByModel(className);
        return dao.countByEpersonIDandUUID(epersonID, uuid);
    }

    public void setConfigurationService(
			ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}
    
    /**
     * Get all researcher in a specific status. If status is null all the
     * ResearcherPage will be returned
     * 
     * @param status
     *            the status to look up or null if any
     * @return the list of ResearcherPage in the supplied status or all
     */
    public List<ResearcherPage> getAllResearcherPageByStatus(Boolean status)
    {
        if (status == null)
        {
            return getList(ResearcherPage.class);
        }
        return researcherPageDao.findAllResearcherPageByStatus(status);
    }

    /**
     * Return a list of all the ResearcherPage that have at least one
     * "name form" that match exactly (as string) the supplied name.
     * 
     * @param name
     *            the name to look up
     * @return the list of ResearcherPage with a name form that match exactly
     *         the lookup text
     */
    public List<ResearcherPage> getResearcherPageByName(String name)
    {
        return researcherPageDao.findAllResearcherByName(name);
    }

    /**
     * The number of all the ResearcherPage.
     * 
     * @return the number of all the ResearcherPage
     */
    public long count()
    {
        return researcherPageDao.count();
    }

    /**
     * The number of ResearcherPage that have at least one "name form" that
     * match exactly (as string) the supplied name.
     * 
     * @param name
     *            the name to look up
     * @return the number of ResearcherPage with a name form that match exactly
     *         the lookup text
     */
    public long countNamesMatching(String name)
    {
        return researcherPageDao.countAllResearcherByName(name);
    }

    /**
     * Utility method, like the {@link #countNamesMatching(String)} the counting
     * is performed excluding a specific ResearcherPage.
     * 
     * @param name
     *            the name to look up
     * @param id
     *            the id (internal db primary key) of the ResearcherPage to
     *            exclude from the match
     * 
     * @return the number of ResearcherPage with a name form that match exactly
     *         the lookup text without take care of the ResearchePage with the
     *         provided id
     */
    public long countNamesMatchingExceptResearcher(String name, Integer id)
    {
        return researcherPageDao.countAllResearcherByNameExceptResearcher(name,
                id);
    }

    /**
     * Return the ReseacherPage related to the supplied autority key.
     * 
     * @param authorityKey
     *            the rp authority key (i.e. rp00024)
     * @return the ResearcherPage; null if the key is invalid
     */
    public ResearcherPage getResearcherByAuthorityKey(String authorityKey)
    {
        ResearcherPage rp = null;
        try
        {
            rp = get(ResearcherPage.class,
                    ResearcherPageUtils.getRealPersistentIdentifier(
                            authorityKey, ResearcherPage.class));
        }
        catch (Exception e)
        {
            // do nothing
        }

        return rp;
    }

    /**
     * Return the list of ResearcherPage with name fields modified after the
     * supplied date
     * 
     * @param nameTimestampLastModified
     *            the starting date for the lookup
     * @return the list of ResearcherPage with name fields modified after the
     *         supplied date
     */
    public List<ResearcherPage> getResearchersPageByNamesTimestampLastModified(
            Date nameTimestampLastModified)
    {
        return researcherPageDao
                .findAllResearcherByNamesTimestampLastModified(nameTimestampLastModified);
    }

    /**
     * Retrieve all the ResearcherPage created in the specified timestamp range.
     * Start and end date can't be both null.
     * 
     * @param start
     *            the start date, no lower limit if null
     * @param end
     *            the end date, no upper limit if null
     * @return the list of ResearcherPage created in the specified timestamp
     *         range
     */
    public List<ResearcherPage> getRPByCriteriaOnDateCreation(Date start,
            Date end)
    {
        if (start != null && end != null)
        {
            return getRPinDateRange(start, end);
        }
        else
        {
            if (start == null && end != null)
            {
                return getRPBeforeDateCreation(end);
            }
            if (end == null && start != null)
            {
                return getRPAfterDateCreation(start);
            }
        }
        return null;
    }

    /* Private utility methods for the retrieve of RP created in a date range */
    private List<ResearcherPage> getRPinDateRange(Date start, Date end)
    {
        return researcherPageDao.findAllResearcherInDateRange(start, end);
    }

    private List<ResearcherPage> getRPBeforeDateCreation(Date end)
    {
        return researcherPageDao.findAllResearcherByCreationDateBefore(end);
    }

    private List<ResearcherPage> getRPAfterDateCreation(Date start)
    {
        return researcherPageDao.findAllResearcherByCreationDateAfter(start);
    }

    /**
     * Retrieve all the ResearcherPage with the rp identifier in the specified
     * range. Start and end identifier can't be both null. The comparation
     * algorithm is alphanumeric
     * 
     * @param start
     *            the start rp identifier, no lower limit if null
     * @param end
     *            the end rp identifier, no upper limit if null
     * @return the list of ResearcherPage with the rp identifier in the
     *         specified range
     */
    public List<ResearcherPage> getAllResearcherByCriteriaOnRPID(String start,
            String end)
    {
        if (start != null && end != null)
        {
            return getRPinRPIDRange(start, end);
        }
        else
        {
            if (start == null && end != null)
            {
                return getRPPrevRPID(end);
            }
            if (end == null && start != null)
            {
                return getRPNextRPID(start);
            }
        }
        return null;
    }

    /*
     * Private utility methods for the retrieve of RP with rp identifier in a
     * specified range
     */
    private List<ResearcherPage> getRPNextRPID(String start)
    {
        Integer s = null;
        if (start != null && !start.isEmpty())
        {
            s = ResearcherPageUtils.getRealPersistentIdentifier(start,
                    ResearcherPage.class);
            return researcherPageDao.findAllNextResearcherByIDStart(s);
        }
        return null;
    }

    private List<ResearcherPage> getRPPrevRPID(String end)
    {
        Integer e = null;
        if (end != null && !end.isEmpty())
        {
            e = ResearcherPageUtils.getRealPersistentIdentifier(end,
                    ResearcherPage.class);
            return researcherPageDao.findAllPrevResearcherByIDEnd(e);
        }
        return null;
    }

    private List<ResearcherPage> getRPinRPIDRange(String start, String end)
    {
        Integer s = null;
        Integer e = null;
        if (start != null && !start.isEmpty() && end != null && !end.isEmpty())
        {
            e = ResearcherPageUtils.getRealPersistentIdentifier(end,
                    ResearcherPage.class);
            s = ResearcherPageUtils.getRealPersistentIdentifier(start,
                    ResearcherPage.class);
            return researcherPageDao.findAllResearcherInIDRange(s, e);
        }
        return null;
    }

    /**
     * Retrieve all the ResearcherPage with the staffNo in the specified range.
     * Start and end staffNo can't be both null. The comparation algorithm is
     * alphanumeric
     * 
     * @param start
     *            the start staffNo, no lower limit if null
     * @param end
     *            the end staffNo, no upper limit if null
     * @return the list of ResearcherPage with the staffNo in the specified
     *         range
     */
    public List<ResearcherPage> getAllResearcherByCriteriaOnStaffNo(
            String start, String end)
    {
        if (start != null && end != null)
        {
            return getRPinStaffNoRange(start, end);
        }
        else
        {
            if (start == null && end != null)
            {
                return getRPPrevStaffNo(end);
            }
            if (end == null && start != null)
            {
                return getRPNextStaffNo(start);
            }
        }
        return null;
    }

    /*
     * Private utility methods for the retrieve of RP with staffNo in a
     * specified range
     */
    private List<ResearcherPage> getRPNextStaffNo(String start)
    {
        return researcherPageDao.findAllNextResearcherBySourceIDStart(start);
    }

    private List<ResearcherPage> getRPPrevStaffNo(String end)
    {
        return researcherPageDao.findAllPrevResearcherBySourceIDEnd(end);
    }

    private List<ResearcherPage> getRPinStaffNoRange(String start, String end)
    {
        return researcherPageDao.findAllResearcherInSourceIDRange(start, end);
    }

    public List<StatSubscription> getAllStatSubscriptionByFreq(int freq)
    {
        return statSubscriptionDao.findByFreq(freq);
    }

    public List<StatSubscription> getAllStatSubscriptionByEPersonID(int eid)
    {
        return statSubscriptionDao.findByEPersonID(eid);
    }

    public List<StatSubscription> getStatSubscriptionByEPersonIDAndUID(int id,
            String uid)
    {
        return statSubscriptionDao.findByEPersonIDandUID(id, uid);
    }

    public List<StatSubscription> getStatSubscriptionByFreqAndType(int freq, int type)
    {
        return statSubscriptionDao.findByFreqAndType(freq, type);
    }

    public void deleteStatSubscriptionsByEPersonID(int id)
    {
        statSubscriptionDao.deleteByEPersonID(id);
    }

    public <T, PK extends Serializable> List<T> getList(Class<T> model,
            List<PK> ids)
    {
        return applicationDao.getList(model, ids);
    }

    public ResearcherPage getResearcherPageByEPersonId(Integer id)
    {
		if (cacheRpByEPerson != null) {
			Element element = cacheRpByEPerson.getQuiet(id);
			if (element != null) {
				ResearcherPage rp = (ResearcherPage) element.getValue();
				if (!isExpiredCache(ResearcherPage.class, element, rp.getId(), rp)) {
					return rp;
				}
				else if (rp != null) {
					return get(ResearcherPage.class, rp.getId(), false);
				}
			}
		}    	
        return researcherPageDao.uniqueByEPersonId(id);
    }

    @Deprecated
    public ResearcherPage getResearcherPageByStaffNo(String code)
    {
        //return (ResearcherPage) getEntityBySourceID(code);
        return researcherPageDao.uniqueBySourceID(null, code);
    }

    @Deprecated
    public Project getResearcherGrantByCode(String code)
    {
        //return (Project) getEntityBySourceID(code);
        return projectDao.uniqueBySourceID(null, code);
    }

    @Deprecated
    public OrganizationUnit getOrganizationUnitByCode(String code)
    {
        //return (OrganizationUnit) getEntityBySourceID(code);
        return organizationUnitDao.uniqueBySourceID(null, code);
    }

    public <T extends ACrisObject> T getEntityByCrisId(String crisID,
            Class<T> className)
    {        
		if (cacheByCrisID != null) {
			Element element = cacheByCrisID.getQuiet(crisID);
			if (element != null) {
				T crisObject = (T) element.getValue();
                //the element retrieved by cache is consistent with the className passed as parameter? (add safety check)
                if(className.isAssignableFrom(crisObject.getClass())) {
    				if (!isExpiredCache(className, element, crisObject.getId(), crisObject)) {
    					return crisObject;
    				}
    				else if (crisObject != null) {
    					return get(className, crisObject.getId(), false);
    				}
                }
                else {
                    //return null because the caller method working on different class object e.g. I searching for a Journal but the primary logic in the caller method see first in ResearcherPage table  
                    return null;
                }
			}
		}

        CrisObjectDao<T> dao = (CrisObjectDao<T>) getDaoByModel(className);
		T object = dao.uniqueByCrisID(crisID);
		if (object != null) {
			putToCache(className, object, object.getId());
		}
		return object;
    }
  
    public <T extends ACrisObject> T getEntityByCrisId(String crisID)
    {        
        T dso = (T)getEntityByCrisId(crisID, ResearcherPage.class);
        if (dso == null) {
            dso = (T)getEntityByCrisId(crisID, OrganizationUnit.class);
            if (dso == null) {
                dso = (T)getEntityByCrisId(crisID, Project.class);
                if (dso == null) {
                    dso = (T)getEntityByCrisId(crisID, ResearchObject.class);
                }
            }
        }
        return dso;
    }
    
    public <T extends ACrisObject> T getEntityBySourceId(String sourceRef, String sourceID,
            Class<T> className)
    {
		if (cacheBySource != null) {
			Element element = cacheBySource.getQuiet(sourceRef + "-" + sourceID);
			if (element != null) {
				T crisObject = (T) element.getValue();
				//the element retrieved by cache is consistent with the className passed as parameter? (add safety check)
				if(className.isAssignableFrom(crisObject.getClass())) {
                    if (!isExpiredCache(className, element, crisObject.getId(),
                            crisObject))
                    {
                        return crisObject;
                    }
                    else if (crisObject != null)
                    {
                        return get(className, crisObject.getId(), false);
                    }
				}
				else {
				    //return null because the caller method working on different class object e.g. I searching for a Journal but the primary logic in the caller method see first in ResearcherPage table  
				    return null;
				}
			}
		}
        CrisObjectDao<T> dao = (CrisObjectDao<T>) getDaoByModel(className);
		T object = dao.uniqueBySourceID(sourceRef, sourceID);
		if (object != null) {
			putToCache(className, object, object.getId());
		}
		return object;
    }

    public ACrisObject getEntityByUUID(String uuid)
    {
		if (cacheByUUID != null) {
			Element element = cacheByUUID.getQuiet(uuid);
			if (element != null) {
				ACrisObject crisObject = (ACrisObject) element.getValue();
				if (!isExpiredCache(crisObject.getClass(), element, crisObject.getId(), crisObject)) {
					return crisObject;
				}
			}
		}    	
        // return ((ApplicationDao) getApplicationDao()).uniqueByUUID(uuid);
        // HIBERNATE 4 seems not support polymorphic query on mappedsuperclass
        ACrisObject obj = researcherPageDao.uniqueByUUID(uuid);
        if (obj == null)
        {
            obj = organizationUnitDao.uniqueByUUID(uuid);
            if (obj == null)
            {
                obj = projectDao.uniqueByUUID(uuid);
                if (obj == null)
                {
                    obj = researchDao.uniqueByUUID(uuid);
                }
            }
        }
		if (obj != null) {
			putToCache((Class) obj.getClass(), obj, obj.getId());
		}        
        return obj;
    }

    public User getUserWSByUsernameAndPassword(String username,
            String password)
    {
        return userWSDao.uniqueByUsernameAndPassword(username, password);
    }

    public User getUserWSByToken(String token)
    {
        return userWSDao.uniqueByToken(token);
    }

	public <T extends ACrisObject> Date uniqueLastModifiedTimeStamp(Class<T> model, int id)
    {
		CrisObjectDao<T> dao = (CrisObjectDao<T>) getDaoByModel(model);
		return dao.uniqueLastModifiedTimeStamp(id);
    }
    
    public ResearcherPage uniqueByCrisID(String crisID)
    {
        return researcherPageDao.uniqueByCrisID(crisID);
    }

    public List<RPProperty> findAnagraficaByRPID(int id)
    {
        return researcherPageDao.findAnagraficaByRPID(id);
    }

    public List<Integer> findAllResearcherPageID()
    {
        return researcherPageDao.findAllResearcherPageID();
    }

    public List<RelationPreference> findRelationsPreferencesForItemID(int itemID)
    {
        return relationPreferenceDao.findByTargetItemID(itemID);
    }

    public List<RelationPreference> findRelationsPreferencesOfUUID(
            String sourceUUID, String relationType)
    {
        return relationPreferenceDao.findBySourceUUIDAndRelationType(
                sourceUUID, relationType);
    }

    public List<RelationPreference> findSelectedRelationsPreferencesOfUUID(
            String uuid, String relationType)
    {
        return relationPreferenceDao.findBySourceUUIDAndRelationTypeAndStatus(
                uuid, relationType, RelationPreference.SELECTED);
    }

    public List<RelationPreference> findRelationsPreferencesByUUIDByRelTypeAndStatus(
            String uuid, String relationType, String status)
    {
        return relationPreferenceDao.findBySourceUUIDAndRelationTypeAndStatus(
                uuid, relationType, status);
    }

    public List<RelationPreference> findRelationsPreferencesForUUID(
            String targetUUID)
    {
        return relationPreferenceDao.findByTargetUUID(targetUUID);
    }

    public RelationPreference getRelationPreferenceForUUIDItemID(String UUID,
            int itemID, String relationType)
    {
        return relationPreferenceDao.uniqueByUUIDItemID(UUID, itemID,
                relationType);
    }

    public RelationPreference getRelationPreferenceForUUIDs(String UUID,
            String targetUUID, String relationType)
    {
        return relationPreferenceDao.uniqueByUUIDs(UUID, targetUUID,
                relationType);
    }

    public <T extends ACrisObject> T get(
            Class<T> model, Integer objectId, boolean forceDetach)
    {
		Element element = getFromCache(model, objectId);
		T rp = element != null ? (T) element.getValue() : null;
		if (isExpiredCache(model, element, objectId, rp))
        {
            rp = super.get(model, objectId);
            if (rp != null)
            {
				putToCache(model, rp, objectId);
                if (forceDetach)
                {
                    rp.getAnagrafica();
                    evict(rp);
                }
            }
        }
        return rp;
    }


	private <T extends ACrisObject> boolean isExpiredCache(Class<T> model, Element element, Integer objectId,
			ACrisObject rp) {
		Date now = new Date();
		if (rp == null) {
			return true;
		}		
		long lastModCache = element.getLastUpdateTime();
		
		if ( now.getTime() - element.getLastAccessTime() > 1000) {
			Date uniqueLastModifiedTimeStamp = uniqueLastModifiedTimeStamp(model, objectId);
			long lastModDb = uniqueLastModifiedTimeStamp != null? uniqueLastModifiedTimeStamp.getTime():Long.MAX_VALUE;
			if (lastModCache >= lastModDb) {
				element.updateAccessStatistics();
				return false;
			}
			else {
				return true;
			}
		}
		return false;
	}

	@Override
	public <T, PK extends Serializable> T get(Class<T> modelClass, PK pkey) {
		if (ACrisObject.class.isAssignableFrom(modelClass) && pkey instanceof Integer) {
			return (T) get((Class<? extends ACrisObject>) modelClass, (Integer) pkey, false);
		} else {
			return super.get(modelClass, pkey);
		}
	}

	public <T extends Serializable, PK extends Serializable> Element getFromCache(
            Class<T> model, PK objectId)
    {
        if (cache != null)
        {
            try
            {
				Element element = cache.getQuiet(model.getName() + "#" + objectId);
				return element;
            }
            catch (Exception ex)
            {
                log.error("getFromCache", ex);
            }
        }
        return null;
    }

	public void clearCache()
    {
        try
        {
        	cache.removeAll();
			cacheRpByEPerson.removeAll();
			cacheBySource.removeAll();
			cacheByCrisID.removeAll();
			cacheByUUID.removeAll();
        }
        catch (Exception ex)
        {
            log.error("clearCache", ex);
        }	
    }
	
	public <T extends Serializable, PK extends Serializable> void putToCache(Class<T> model,
            T object, PK objectId)
    {
		if (object == null) {
			return;
		}
        if (cache != null)
        {
            try
            {
				cache.put(new Element(model.getName() + "#" + objectId, object));
            }
            catch (Exception ex)
            {
                log.error("putToCache", ex);
            }
        }
		if (cacheRpByEPerson != null && object instanceof ResearcherPage) {
			Integer eid = ((ResearcherPage) object).getEpersonID();
			if (eid != null) {
				cacheRpByEPerson.put(new Element(eid, object));
			}
		}
		if (object instanceof ACrisObject) {
			if (cacheByCrisID != null) {
				String key = ((ACrisObject) object).getCrisID();
				if (key != null) {
					cacheByCrisID.put(new Element(key, object));
				}
			}
			if (cacheBySource != null) {
				String sourceRef = ((ACrisObject) object).getSourceRef();
				String sourceID = ((ACrisObject) object).getSourceID();
				if (sourceID != null) {
					String key = sourceRef + "-" + sourceID;
					cacheBySource.put(new Element(key, object));
				}
			}
			if (cacheByUUID != null) {
				String key = ((ACrisObject) object).getUuid();
				if (key != null) {
					cacheByUUID.put(new Element(key, object));
				}
			}
		}        
    }

    public Integer getRPidFindMax()
    {
        return researcherPageDao.idFindMax();
    }

    public List<ACrisObject> getListByUUIDs(List<String> uuidS)
    {
        List<ACrisObject> result = new ArrayList<ACrisObject>();
        for (String uuid : uuidS)
        {
            ACrisObject object = getEntityByUUID(uuid);
            result.add(object);
        }
        return result;
    }

    public long countResearchObjectByType(DynamicObjectType typo)
    {
        return researchDao.countByType(typo);
    }

    public List<ResearchObject> getResearchObjectPaginateListByType(DynamicObjectType typo,
            String sort, boolean inverse,
            int page, Integer maxResults)
    {    
        return researchDao.paginateByType(typo, sort, inverse, (page - 1)
                * maxResults, maxResults);
    }
  
    public List<ResearchObject> getResearchObjectByShortNameType(String shortName) {
    	return researchDao.findByShortNameType(shortName);
    }
    
    public List<ResearchObject> getResearchObjectByIDType(Integer id) {
        return researchDao.findByIDType(id);
    }
    
    @Override
    public <P, PK extends Serializable> void delete(Class<P> model, PK pkey) {    	
    	super.delete(model, pkey);
    	clearCache();
    }

	public List<OrcidQueue> findOrcidQueueByResearcherId(String crisId) {
		return orcidQueueDao.findOrcidQueueByOwner(crisId);
	}
	public OrcidQueue uniqueOrcidQueueByEntityIdAndTypeIdAndOwnerId(Integer entityID, Integer typeId, String ownerId) {
		return orcidQueueDao.uniqueOrcidQueueByEntityIdAndTypeIdAndOwner(entityID, typeId, ownerId);
	}
	public List<OrcidHistory> findOrcidHistoryByOrcidAndTypeId(String orcid, Integer typeId) {	
		return orcidHistoryDao.findOrcidHistoryByOrcidAndTypeId(orcid, typeId);
	}
    public List<OrcidHistory> findOrcidHistoryByOrcidAndEntityUUIDAndTypeId(String orcid, String entityUUID, Integer typeId) {  
        return orcidHistoryDao.findOrcidHistoryByOrcidAndEntityUUIDAndTypeId(orcid, entityUUID, typeId);
    }
	
	public void deleteOrcidQueueByOwnerAndTypeId(String crisID, int typeId) {
		orcidQueueDao.deleteByOwnerAndTypeId(crisID, typeId);
	}
	
	public void deleteOrcidQueueByOwnerAndUuid(String crisID, String uuId) {
		orcidQueueDao.deleteByOwnerAndUuid(crisID, uuId);
	}

	public List<OrcidHistory> findOrcidHistoryByOwnerAndSuccess(String crisID) {
		return orcidHistoryDao.findOrcidHistoryInSuccessByOwner(crisID);
	}

	public List<OrcidHistory> findOrcidHistoryInSuccessByOwnerAndType(String crisID, int type) {
		return orcidHistoryDao.findOrcidHistoryInSuccessByOwnerAndTypeId(crisID, type);
	}
	
	public OrcidHistory uniqueOrcidHistoryInSuccessByOwnerAndEntityUUIDAndTypeId(String crisID, String uuid, int typeID) {
		return orcidHistoryDao.uniqueOrcidHistoryInSuccessByOwnerAndEntityUUIDAndTypeId(crisID, uuid, typeID);
	}
	
	public OrcidHistory uniqueOrcidHistoryByOwnerAndOrcidAndTypeId(String crisID, String orcid, int typeID) {
		return orcidHistoryDao.uniqueOrcidHistoryByOwnerAndOrcidAndTypeId(crisID, orcid, typeID);
	}

    public OrcidHistory uniqueOrcidHistoryByOwnerAndEntityUUIDAndTypeId(
            String crisID, String entityUUID, int typeID)
    {
        return orcidHistoryDao.uniqueOrcidHistoryByOwnerAndEntityUUIDAndTypeId(
                crisID, entityUUID, typeID);
    }

    public List<DynamicTypeNestedObject> findNestedMaskById(Class<DynamicObjectType> clazz, Integer id)
    {
        DynamicObjectTypeDao dao = (DynamicObjectTypeDao)getDaoByModel(clazz);
        return dao.findNestedMaskById(id);
    }
    
    public static synchronized void checkRebuildCrisConfiguration()
    {
        // We only do something if the reindexDiscovery flag has been triggered
        if(DatabaseUtils.getRebuildCrisConfiguration())
        {
            // Kick off a custom thread to perform the reindexing in Discovery
            // (See ReindexerThread nested class below)
            ConfigurationThread go = new ConfigurationThread();
            go.start();
        }
    }
    
	private static class ConfigurationThread extends Thread {

		/**
		 * Actually perform Rebuild Cris Configuration.
		 */
		@Override
		public void run() {
			if (DatabaseUtils.getRebuildCrisConfiguration()) {
				try {
					log.info("Post database migration, rebuild cris configuration");
					String sourceVersion = Util.getSourceVersion();
                    log.info("DSpace version: " + sourceVersion);
					String file = ConfigurationManager.getProperty("dspace.dir") + File.separator + "etc"
					        + File.separator + "upgrade" + File.separator + sourceVersion+"__DSpaceCRIS-Upgrade.xls";
					String[] args = new String[] { "-f", file };
					ImportCRISDataModelConfiguration.main(args);
					log.info("Rebuild CRIS Configuration is complete");
				} catch (SQLException | IOException | BiffException | InstantiationException | IllegalAccessException
						| ParseException e) {
					log.error("Error attempting to Rebuild CRIS Configuration", e);
				} finally {
					// Reset our flag. Job is done or it threw an error,
					// Either way, we shouldn't try again.
					DatabaseUtils.setRebuildCrisConfiguration(false);

				}
			}
		}
	}

    public void disableCacheManager()
    {
        if (cacheManager != null)
        {
            cache = null;
            cacheRpByEPerson = null;
            cacheBySource = null;
            cacheByCrisID = null;
            cacheByUUID = null;            
            cacheManager.shutdown();
        }   
    }

	
} 