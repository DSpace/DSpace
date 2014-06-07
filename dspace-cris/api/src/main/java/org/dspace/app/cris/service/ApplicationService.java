/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.service;

import it.cilea.osd.common.model.Identifiable;
import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
import org.dspace.app.cris.dao.ApplicationDao;
import org.dspace.app.cris.dao.CrisObjectDao;
import org.dspace.app.cris.dao.CrisSubscriptionDao;
import org.dspace.app.cris.dao.DynamicObjectDao;
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
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.model.jdyna.DynamicObjectType;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.model.ws.User;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.hibernate.Session;

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

    private CacheManager cacheManager;

    private Cache cache;

    private static Logger log = Logger.getLogger(ApplicationService.class);

    /**
     * Initialization method
     */
    public void init()
    {
        researcherPageDao = (ResearcherPageDao) getDaoByModel(ResearcherPage.class);
        projectDao = (ProjectDao) getDaoByModel(Project.class);
        organizationUnitDao = (OrganizationUnitDao) getDaoByModel(OrganizationUnit.class);
        crisSubscriptionDao = (CrisSubscriptionDao) getDaoByModel(CrisSubscription.class);
        statSubscriptionDao = (StatSubscriptionDao) getDaoByModel(StatSubscription.class);
        userWSDao = (UserWSDao) getDaoByModel(User.class);
        relationPreferenceDao = (RelationPreferenceDao) getDaoByModel(RelationPreference.class);
        researchDao = (DynamicObjectDao) getDaoByModel(ResearchObject.class);
        
        if (cache == null)
        {
            try
            {
                cacheManager = CacheManager.create();
                if (cacheManager != null)
                {
                    cache = cacheManager.getCache("applicationServiceCache");
                    if (cache == null)
                    {
                        cache = new Cache("applicationServiceCache", 100, true,
                                true, 0, 0, false, 600);
                        cacheManager.addCache(cache);
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
        if (cacheManager != null)
        {
            cache = null;
            cacheManager.shutdown();
        }
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
        return researcherPageDao.uniqueByEPersonId(id);
    }

    public ResearcherPage getResearcherPageByStaffNo(String code)
    {
        //return (ResearcherPage) getEntityBySourceID(code);
        return researcherPageDao.uniqueBySourceID(code);
    }

    public Project getResearcherGrantByCode(String code)
    {
        //return (Project) getEntityBySourceID(code);
        return projectDao.uniqueBySourceID(code);
    }

    public OrganizationUnit getOrganizationUnitByCode(String code)
    {
        //return (OrganizationUnit) getEntityBySourceID(code);
        return organizationUnitDao.uniqueBySourceID(code);
    }

    public <T extends ACrisObject> T getEntityByCrisId(String crisID,
            Class<T> className)
    {        
        CrisObjectDao<T> dao = (CrisObjectDao<T>) getDaoByModel(className);
        return dao.uniqueByCrisID(crisID);
    }
  
    public <T extends ACrisObject> T getEntityBySourceId(String sourceID,
            Class<T> className)
    {
        CrisObjectDao<T> dao = (CrisObjectDao<T>) getDaoByModel(className);
        return dao.uniqueBySourceID(sourceID);
    }

    public ACrisObject getEntityByUUID(String uuid)
    {
        return ((ApplicationDao) getApplicationDao()).uniqueByUUID(uuid);
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

    public Date uniqueLastModifiedTimeStamp(int id)
    {
        return researcherPageDao.uniqueLastModifiedTimeStamp(id);
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
        T rp = getFromCache(model, objectId);
        if (rp == null
                || rp.getTimeStampInfo().getTimestampLastModified() == null
                || !rp.getTimeStampInfo().getTimestampLastModified()
                        .getTimestamp()
                        .equals(uniqueLastModifiedTimeStamp(objectId)))
        {
            rp = super.get(model, objectId);
            if (rp != null)
            {
                putToCache(rp, objectId);
                if (forceDetach)
                {
                    rp.getAnagrafica();
                    evict(rp);
                }
            }
        }
        return rp;
    }

    public <T extends Serializable, PK extends Serializable> T getFromCache(
            Class<T> model, PK objectId)
    {
        if (cache != null)
        {
            try
            {
                Element element = cache.get(objectId);
                if (element != null)
                    return (T) element.getValue();
            }
            catch (Exception ex)
            {
                log.error("getFromCache", ex);
            }
        }
        return null;
    }

    public <T extends Serializable, PK extends Serializable> void putToCache(
            T object, PK objectId)
    {
        if (cache != null)
        {
            try
            {
                cache.put(new Element(objectId, object));
            }
            catch (Exception ex)
            {
                log.error("putToCache", ex);
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
   
   
}
