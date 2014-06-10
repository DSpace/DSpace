/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.dto;

import java.util.LinkedList;
import java.util.List;

import it.cilea.osd.jdyna.dto.AnagraficaObjectAreaDTO;

import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.list.LazyList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.RestrictedField;

public class ProjectAnagraficaObjectDTO extends CrisAnagraficaObjectDTO
{

    /**
     * The log4j category
     */
    protected final Log log = LogFactory.getLog(getClass());

    private String investigator;

    private List<String> coInvestigators;

    public ProjectAnagraficaObjectDTO(Project grant)
    {
        super(grant);
        this.setTimeStampCreated(grant.getTimeStampInfo().getCreationTime());
        this.setTimeStampModified(grant.getTimeStampInfo()
                .getLastModificationTime());
    }
 
    public void setInvestigator(String investigator)
    {
        this.investigator = investigator;
    }

    public String getInvestigator()
    {
        return investigator;
    }

    /**
     * Decorate list for dynamic binding with spring mvc
     * 
     * @param list
     * @return lazy list temporary
     */
    private List getLazyList(List<String> list)
    {
        log.debug("Decorate list for dynamic binding with spring mvc");
        List lazyList = LazyList.decorate(list,
                FactoryUtils.instantiateFactory(String.class));

        return lazyList;
    }

    public void setCoInvestigators(List<String> coInvestigators)
    {
        this.coInvestigators = coInvestigators;
    }

    public List<String> getCoInvestigators()
    {
        if (this.coInvestigators == null)
        {
            this.coInvestigators = new LinkedList<String>();
        }
        setCoInvestigators(getLazyList(coInvestigators));
        return coInvestigators;
    }
}
