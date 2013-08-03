/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.pmc.services;

import it.cilea.osd.common.dao.IApplicationDao;
import it.cilea.osd.common.model.Identifiable;
import it.cilea.osd.common.service.PersistenceService;

import org.dspace.app.cris.pmc.dao.PMCCitationDao;
import org.dspace.app.cris.pmc.model.PMCCitation;
import org.dspace.app.cris.pmc.model.PMCRecord;
import org.hibernate.Session;

public class PMCPersistenceService extends PersistenceService
{
    private PMCEntrezServices entrez;

    protected IApplicationDao applicationDao;

    private PMCCitationDao citationDao;

    public void init()
    {
        citationDao = (PMCCitationDao) getDaoByModel(PMCCitation.class);
    }

    /**
     * Setter for the applicationDao
     * 
     * @param applicationDao
     *            the dao to use for generic query
     */
    public void setApplicationDao(IApplicationDao applicationDao)
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

    public PMCCitation getCitationByItemID(Integer itemID)
    {
        return citationDao.uniqueCitationByItemID(itemID);
    }
}
