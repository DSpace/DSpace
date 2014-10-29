/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.dao;

import java.util.Date;
import java.util.List;

import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.RPProperty;

/**
 * This interface define the methods available to retrieve ResearcherPage
 * 
 * @author cilea
 * 
 */
public interface ResearcherPageDao extends CrisObjectDao<ResearcherPage>        
{
    public ResearcherPage uniqueBySourceID(String sourceRef, String sourceId);

    public List<ResearcherPage> findAllResearcherPageByStatus(Boolean status);

    public List<ResearcherPage> findAllResearcherByName(String name);

    public long count();

    public long countAllResearcherByName(String name);

    public long countAllResearcherByNameExceptResearcher(String name, Integer id);

    public List<ResearcherPage> findAllResearcherByField(Integer id);

    public List<ResearcherPage> findAllResearcherByNamesTimestampLastModified(
            Date nameTimestampLastModified);

    public List<ResearcherPage> findAllResearcherInDateRange(Date start,
            Date end);

    public List<ResearcherPage> findAllResearcherByCreationDateBefore(Date end);

    public List<ResearcherPage> findAllResearcherByCreationDateAfter(Date start);

    public List<ResearcherPage> findAllNextResearcherByIDStart(Integer start);

    public List<ResearcherPage> findAllPrevResearcherByIDEnd(Integer end);

    public List<ResearcherPage> findAllResearcherInIDRange(Integer start,
            Integer end);

    public List<ResearcherPage> findAllNextResearcherBySourceIDStart(String start);

    public List<ResearcherPage> findAllPrevResearcherBySourceIDEnd(String end);

    public List<ResearcherPage> findAllResearcherInSourceIDRange(String start,
            String end);
    


    public List<RPProperty> findAnagraficaByRPID(Integer id);

    public List<Integer> findAllResearcherPageID();

    public ResearcherPage uniqueByEPersonId(Integer id);

    public Integer idFindMax();
}
