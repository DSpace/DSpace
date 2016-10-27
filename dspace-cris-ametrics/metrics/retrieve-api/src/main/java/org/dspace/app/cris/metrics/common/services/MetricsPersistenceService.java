/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.metrics.common.services;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.cris.metrics.common.dao.CrisMetricsDao;
import org.dspace.app.cris.metrics.common.dao.MetricsApplicationDao;
import org.dspace.app.cris.metrics.common.model.CrisMetrics;
import org.dspace.app.cris.metrics.pmc.dao.PMCCitationDao;
import org.dspace.app.cris.metrics.pmc.model.PMCCitation;
import org.dspace.app.cris.metrics.pmc.model.PMCRecord;
import org.dspace.app.cris.metrics.pmc.services.PMCEntrezException;
import org.dspace.app.cris.metrics.pmc.services.PMCEntrezServices;
import org.dspace.core.Context;
import org.hibernate.Session;

import it.cilea.osd.common.dao.IApplicationDao;
import it.cilea.osd.common.model.Identifiable;
import it.cilea.osd.common.service.PersistenceService;

public class MetricsPersistenceService extends PersistenceService
{
    private PMCEntrezServices entrez;

    protected MetricsApplicationDao applicationDao;

    private PMCCitationDao pmcCitationDao;
    
    private CrisMetricsDao metricsDao;
    
    public void init()
    {
    	pmcCitationDao = (PMCCitationDao) getDaoByModel(PMCCitation.class);
    	metricsDao = (CrisMetricsDao) getDaoByModel(CrisMetrics.class);
    }

    /**
     * Setter for the applicationDao
     * 
     * @param applicationDao
     *            the dao to use for generic query
     */
    public void setApplicationDao(MetricsApplicationDao applicationDao)
    {
        this.applicationDao = applicationDao;
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

    /**
     * Get the PubMedCentral record from the local DB or the remote Entrez
     * service if not locally available.
     * 
     * @param pmcid
     * @return
     * @throws PMCEntrezException
     */
    public PMCRecord getPMCRecord(Integer pmcid) throws PMCEntrezException
    {
        PMCRecord pmcr = get(PMCRecord.class, pmcid);
        if (pmcr == null)
        {
            pmcr = entrez.getPMCRecord(pmcid);
        }
        return pmcr;
    }

    public PMCCitation getPMCCitationByItemID(Integer itemID)
    {
        return pmcCitationDao.uniqueCitationByItemID(itemID);
    }
    
    public CrisMetrics getLastMetricByResourceIDAndResourceTypeAndMetricsType(Integer resourceID, Integer resourceTypeId, String metricsType)
    {
        return metricsDao.uniqueLastMetricByResourceIdAndResourceTypeIdAndMetricsType(resourceID, resourceTypeId, metricsType);
    }
    
    public List<CrisMetrics> getLastMetricByResourceIDAndResourceTypeAndMetricsTypes(Integer resourceID, Integer resourceTypeId, List<String> metricsTypes)
    {
        return metricsDao.findLastMetricByResourceIdAndResourceTypeIdAndMetricsTypes(resourceID, resourceTypeId, metricsTypes);
    }

    @Override
    public <T extends Identifiable> void saveOrUpdate(Class<T> modelClass, T transientObject) {
    	if (modelClass.isAssignableFrom(CrisMetrics.class)) {
    		CrisMetrics metric = (CrisMetrics) transientObject;
    		metric.setLast(true);
    		applicationDao.unsetLastMetric(metric.getResourceTypeId(), metric.getResourceId(), metric.getMetricType());
    	}
    	super.saveOrUpdate(modelClass, transientObject);
    }

	public void buildPeriodMetrics(Context context, String frequency, String type, long rangeLimitSx,
			long rangeLimitDx) {
		try {
			applicationDao.buildPeriodMetrics(context, frequency, type, rangeLimitSx, rangeLimitDx);
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		
	}
}