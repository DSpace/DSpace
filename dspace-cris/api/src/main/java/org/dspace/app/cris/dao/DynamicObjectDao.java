/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.dao;

import java.util.List;

import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.jdyna.DynamicObjectType;

/**
 * This interface define the methods available to retrieve ResearchObject
 * 
 * @author cilea
 * 
 */
public interface DynamicObjectDao extends CrisObjectDao<ResearchObject>        
{
    
    public ResearchObject uniqueBySourceID(String sourceRef, String sourceId);

    public long countByType(DynamicObjectType typo);

    public List<ResearchObject> paginateByType(DynamicObjectType typo, String sort, boolean inverse,
            int page, Integer pagesize);
    
    public List<ResearchObject> findByShortNameType(String shortName);
    
    public List<ResearchObject> findByIDType(Integer id);
    
}
